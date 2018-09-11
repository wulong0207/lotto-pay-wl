package com.hhly.paycore.service.impl;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.AgentInfoMapper;
import com.hhly.paycore.dao.AgentTransLogMapper;
import com.hhly.paycore.dao.AgentTransTakenMapper;
import com.hhly.paycore.po.AgentTransLogPO;
import com.hhly.paycore.po.AgentTransTakenPO;
import com.hhly.paycore.service.AgentCoreService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.AgentConstants;
import com.hhly.skeleton.base.constants.AgentConstants.AgentTransTypeEnum;
import com.hhly.skeleton.base.constants.AgentConstants.AgentWalletTransTypeEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransLogBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;
import com.hhly.skeleton.pay.agent.vo.ConfirmTakenResultVO;
import com.hhly.skeleton.pay.agent.vo.WithdrawApplyVO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;

@Service
public class AgentCoreServiceImpl implements AgentCoreService {
	private static final Logger logger = LoggerFactory.getLogger(AgentCoreServiceImpl.class);
	@Resource
	private AgentInfoMapper agentInfoMapper;
	@Resource
	private RedissonClient redissonClient;
	@Resource
	private AgentTransLogMapper agentTransLogMapper;
	@Resource
	private AgentTransTakenMapper agentTransTakenMapper;

	@Override
	public AgentInfoBO findAgentInfo(Integer userId) {
		return agentInfoMapper.selectAgentInfoByUserId(userId);
	}

	@Override
	public ResultBO<?> updateAgentWallet(Integer agentId, Double updateAmount, AgentWalletTransTypeEnum agentWalletTransTypeEnum) {
		String lockKey = createTransKey(agentId);
		RLock lock = redissonClient.getLock(lockKey);
		AgentInfoBO agentInfo = null;
		try {
			// 尝试加锁，最多等待3秒，上锁以后5秒自动解锁
			boolean isLock = lock.tryLock(3, 5, TimeUnit.SECONDS);
			if (isLock) { // 成功
				// 更新代理账户钱包
				Integer result = 0;
				if (agentWalletTransTypeEnum.equals(AgentConstants.AgentWalletTransTypeEnum.IN)) {
					result = agentInfoMapper.updateAgentWalletIncome(agentId, updateAmount);
				} else {
					result = agentInfoMapper.updateAgentWalletOut(agentId, updateAmount);
				}
				logger.info("从代理【" + agentId + "】账户" + agentWalletTransTypeEnum.getValue() + "金额：" + updateAmount + (result > 0 ? "成功" : "失败"));
				if (result > 0) {
					agentInfo = agentInfoMapper.selectAgentInfo(agentId);
					return ResultBO.ok(agentInfo);
				}
			} else {
				logger.info("加锁" + lockKey + "失败");
			}
		} catch (InterruptedException e) {
			logger.error("执行分布式锁：" + lockKey + "异常", e);
		} finally {
			lock.unlock();
		}
		return ResultBO.err(MessageCodeConstants.AGENT_UPDATE_AMOUNT_ERROR);
	}

	@Override
	public int addTransLog(List<AgentTransLogPO> list) throws Exception {
		int num = agentTransLogMapper.insertBatch(list);
		if (num <= 0) {
			throw new RuntimeException("添加代理结算佣金流水异常");
		}
		return num;
	}

	@Override
	public AgentTransLogPO addTransLog(ConfirmTakenResultVO confirmTakenResult) throws Exception {
		AgentTransLogPO agentTransLogPO = new AgentTransLogPO(confirmTakenResult);
		AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.TAKEN_TO_BANK;
		agentTransLogPO.setRemark(agentTransTypeEnum.getValue());
		agentTransLogPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		agentTransLogPO.setTradeCode(confirmTakenResult.getTakenCode());
		agentTransLogPO.setTransInfo(confirmTakenResult.getBankName() + "：" + confirmTakenResult.getCardCode());// 农业银行：***3172
		agentTransLogPO.setTransStatus(AgentConstants.AgentTransStatusEnum.WAIT_AUDIT.getKey());
		agentTransLogPO.setTransType(agentTransTypeEnum.getKey());
		int num = agentTransLogMapper.insert(agentTransLogPO);
		if (num <= 0) {
			throw new RuntimeException("添加代理提款流水异常");
		}
		return agentTransLogPO;
	}

	@Override
	public AgentTransLogPO addTransLog(AgentInfoBO agentInfo, Double updateAmount, String account, String memberTransUserCode) throws Exception {
		AgentTransLogPO agentTransLogPO = new AgentTransLogPO();
		agentTransLogPO.setAgentId(agentInfo.getId());
		agentTransLogPO.setTransAmount(updateAmount);
		agentTransLogPO.setRealAmount(updateAmount);
		agentTransLogPO.setServiceCharge(0d);
		agentTransLogPO.setTaxCharge(0d);
		agentTransLogPO.setTradeCode(memberTransUserCode);
		agentTransLogPO.setTotalCashBalance(agentInfo.getAgentWallet());
		agentTransLogPO.setCreateBy(agentInfo.getAgentCode());
		AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.RECHARGE_TO_ACCOUNT;
		agentTransLogPO.setRemark(agentTransTypeEnum.getValue());
		agentTransLogPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		agentTransLogPO.setTransInfo(agentTransTypeEnum.getValue() + "：" + account);// 充值到账号：***3172
		agentTransLogPO.setTransStatus(AgentConstants.AgentTransStatusEnum.TRADE_SUCCESS.getKey());
		agentTransLogPO.setTransType(agentTransTypeEnum.getKey());
		int num = agentTransLogMapper.insert(agentTransLogPO);
		if (num <= 0) {
			throw new RuntimeException("添加代理充值流水异常");
		}
		return agentTransLogPO;
	}

	@Override
	public AgentTransLogPO addTransLog(AgentInfoBO agentInfo, Double updateAmount) throws Exception {
		AgentTransLogPO agentTransLogPO = new AgentTransLogPO();
		agentTransLogPO.setAgentId(agentInfo.getId());
		agentTransLogPO.setTransAmount(updateAmount);
		agentTransLogPO.setServiceCharge(0d);
		agentTransLogPO.setTaxCharge(0d);
		agentTransLogPO.setRealAmount(updateAmount);
		agentTransLogPO.setTotalCashBalance(agentInfo.getAgentWallet());
		agentTransLogPO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
		AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.SETTLE_ACCOUNTS;
		agentTransLogPO.setRemark(agentTransTypeEnum.getValue());
		agentTransLogPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		agentTransLogPO.setTransInfo(agentTransTypeEnum.getValue());// 充值到账号：***3172
		agentTransLogPO.setTransStatus(AgentConstants.AgentTransStatusEnum.TRADE_SUCCESS.getKey());
		agentTransLogPO.setTransType(agentTransTypeEnum.getKey());
		int num = agentTransLogMapper.insert(agentTransLogPO);
		if (num <= 0) {
			throw new RuntimeException("添加代理结算佣金流水异常");
		}
		return agentTransLogPO;
	}

	@Override
	public AgentTransTakenPO addAgentTaken(WithdrawApplyVO withdrawApply, ConfirmTakenResultVO confirmTakenResult, AgentInfoBO agentInfo, PayBankcardBO bankCard) throws Exception {
		AgentTransTakenPO agentTransTakenPO = new AgentTransTakenPO();
		agentTransTakenPO.setTransTakenCode(confirmTakenResult.getTakenCode());// 固定规则的提现记录编号
		agentTransTakenPO.setAgentId(agentInfo.getId());// 代理id
		agentTransTakenPO.setPayChannel((short) 5);// 提现的付款渠道；1：支付宝充值；2：微信支付；3：练练支付；4：百度支付；5：人工充值
		agentTransTakenPO.setTakenBank(bankCard.getBankid());// 银行id
		agentTransTakenPO.setBankCardNum(bankCard.getCardcode());// 提现的银行卡号
		agentTransTakenPO.setBankInfo(confirmTakenResult.getBankName() + "：" + StringUtil.hideHeadString(bankCard.getCardcode()));// 分支行信息
		agentTransTakenPO.setExtractAmount(confirmTakenResult.getTakenAmount());// 提款金额
		agentTransTakenPO.setServiceCharge(confirmTakenResult.getFee());// 服务费
		agentTransTakenPO.setTaxCharge(confirmTakenResult.getTax());// 劳务所得税费
		// agentTransTakenBO.setReviewBy(reviewBy);// 审核人
		// agentTransTakenBO.setTransCert(transCert);// 银行交易凭证
		// agentTransTakenBO.setTransFailInfo(transFailInfo);// 交易失败原因
		agentTransTakenPO.setTransStatus(PayConstants.TakenStatusEnum.PENDING_AUDIT.getKey());// 提现交易状态；1审核通过 2审核不通过 3银行处理成功 4银行处理失败 5已到帐6待审核7银行处理中
		agentTransTakenPO.setTakenPlatform(withdrawApply.getPaltform());// 提现平台1：本站web；2：本站wap；3：android客户端；4：ios客户端；5：未知；
		agentTransTakenPO.setChannelId(withdrawApply.getChannelId());// 提现渠道
		// agentTransTakenBO.setThirdTransNum(thirdTransNum);// 第三方流水号
		// agentTransTakenBO.setBatchNum(batchNum);// 批次号
		// agentTransTakenBO.setBatchStatus(batchStatus);// 批次状态； 0：处理失败；1：处理成功
		// agentTransTakenBO.setBankReturnInfo(bankReturnInfo);// 银行返回信息描述
		agentTransTakenPO.setRealAmount(confirmTakenResult.getArrivalAmount());// 用户实际到帐金额
		agentTransTakenPO.setCreateBy(agentInfo.getAgentCode());// 创建人
		// agentTransTakenBO.setUpdateBy(updateBy);// 修改人
		agentTransTakenPO.setRemark(Constants.ACCOUNT_TAKEN);// 备注说明
		// 添加提款记录
		int num = agentTransTakenMapper.addTaken(agentTransTakenPO);
		if (num <= 0) {
			throw new RuntimeException("添加代理结算佣金流水异常");
		}
		return agentTransTakenPO;
	}

	@Override
	public AgentTransLogBO findAgentTransLogDetail(Integer agentId, String transCode) {
		return agentTransLogMapper.findTransDetail(agentId, transCode);
	}

	@Override
	public AgentTransLogBO findAgentTransLogByTakenCode(String tradeCode) {
		return agentTransLogMapper.findAgentTransLogByTakenCode(tradeCode);
	}

	@Override
	public AgentTransTakenBO findAgentTakenDetail(Integer agentId, String takenCode) {
		return agentTransTakenMapper.findTransTakenDetail(agentId, takenCode);
	}

	@Override
	public AgentTransTakenBO findAgentTakenDetailByCode(String takenCode) {
		return agentTransTakenMapper.findTransTakenDetailByCode(takenCode);
	}

	@Override
	public AgentTransTakenBO findAgentLastTakenDetail(Integer agentId) {
		return agentTransTakenMapper.findAgentLastTakenDetail(agentId);
	}

	@Override
	public AgentTransLogBO findAgentLastTransDetail(Integer agentId) {
		return agentTransLogMapper.findAgentLastTransDetail(agentId);
	}

	@Override
	public Integer findAgentTransListCount(AgentTransLogParamVO agentTransLogParam) {
		return agentTransLogMapper.getAgentTransListCount(agentTransLogParam);
	}

	@Override
	public List<AgentTransLogBO> findAgentTransListByPage(AgentTransLogParamVO agentTransLogParam) {
		return agentTransLogMapper.getAgentTransListByPage(agentTransLogParam);
	}

	@Override
	public int updateTakenByBatch(List<AgentTransTakenPO> list) {
		return agentTransTakenMapper.updateTakenByBatch(list);
	}

	@Override
	public int updateTransUserByBatch(List<AgentTransLogPO> list) {
		return agentTransLogMapper.updateAgentTransByBatch(list);
	}

	/**  
	* 方法说明: 创建转账的key
	* @auth: xiongJinGang
	* @param agentId
	* @param transType
	* @time: 2018年3月5日 上午10:37:52
	* @return: String 
	*/
	private String createTransKey(Integer agentId) {
		return new StringBuffer(CacheConstants.P_CORE_AGENT_WALLET_LOCK).append(agentId).toString();
	}
}
