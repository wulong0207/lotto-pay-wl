package com.hhly.paycore.controller;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.remote.service.IAgentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;
import com.hhly.skeleton.pay.agent.vo.TransferAccountsVO;
import com.hhly.skeleton.pay.agent.vo.WithdrawApplyVO;
import com.hhly.skeleton.pay.vo.PayBankcardVO;
import com.hhly.skeleton.pay.vo.TakenBankCardVO;

/**
 * @desc 代理输入输出控制层
 * @author xiongJinGang
 * @date 2018年3月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@RestController
@RequestMapping("/agent")
public class AgentController {

	private static final Logger logger = LoggerFactory.getLogger(AgentController.class);

	@Resource
	private IAgentService agentService;
	@Resource
	private BankcardService bankcardService;

	/**  
	* 方法说明: 获取用户储蓄卡列表
	* @auth: xiongJinGang
	* @param payBankcard
	* @throws Exception
	* @time: 2018年3月2日 下午3:15:27
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("unchecked")
	@RequestMapping(value = "/findBankList", method = RequestMethod.POST)
	public ResultBO<?> findBankList(@RequestBody PayBankcardVO payBankcard) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("获取token为：" + payBankcard.getToken() + " 的银行卡列表");
		}
		// 只获取储蓄卡
		ResultBO<?> resultBO = bankcardService.findBankList(payBankcard.getToken());
		if (resultBO.isError()) {
			return resultBO;
		}
		List<TakenBankCardVO> list = (List<TakenBankCardVO>) resultBO.getData();
		AgentInfoBO agentInfo = agentService.findAgentInfo(payBankcard.getToken());
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("list", list);
		jsonObject.put("maxAmount", agentInfo.getAgentWallet());
		return ResultBO.ok(jsonObject);
	}

	/**  
	* 方法说明: 申请提现
	* @auth: xiongJinGang
	* @param withdrawApply
	* @throws Exception
	* @time: 2018年3月5日 上午9:26:15
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/applyWithdraw", method = RequestMethod.POST)
	public ResultBO<?> applyWithdraw(@RequestBody WithdrawApplyVO withdrawApply) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("代理token为：" + withdrawApply.getToken() + " 申请提现，" + withdrawApply.toString());
		}
		return agentService.applyWithdraw(withdrawApply);
	}

	/**  
	* 方法说明: 确认提现
	* @auth: xiongJinGang
	* @param withdrawApply
	* @throws Exception
	* @time: 2018年3月5日 上午9:26:29
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/confirmWithdraw", method = RequestMethod.POST)
	public ResultBO<?> confirmWithdraw(@RequestBody WithdrawApplyVO withdrawApply) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("代理token为：" + withdrawApply.getToken() + " 确认提现，" + withdrawApply.toString());
		}
		return agentService.updateConfirmWithdraw(withdrawApply);
	}

	/**  
	* 方法说明: 代理转充值
	* @auth: xiongJinGang
	* @param transferAccounts
	* @throws Exception
	* @time: 2018年3月5日 上午11:56:58
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/transAccount", method = RequestMethod.POST)
	public ResultBO<?> transAccount(@RequestBody TransferAccountsVO transferAccounts) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("代理token为：" + transferAccounts.getToken() + " 转账，" + transferAccounts.toString());
		}
		return agentService.updateTransAccount(transferAccounts);
	}

	/**  
	* 方法说明: 交易流水列表
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @throws Exception
	* @time: 2018年3月5日 下午4:47:22
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/transList", method = RequestMethod.POST)
	public ResultBO<?> transList(@RequestBody AgentTransLogParamVO agentTransLogParam) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("获取token为：" + agentTransLogParam.getToken() + " 交易流水，" + agentTransLogParam.toString());
		}
		return agentService.findAppTransUserByPage(agentTransLogParam);
	}

	/**  
	* 方法说明: 获取交易详情
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @throws Exception
	* @time: 2018年3月5日 下午5:07:10
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/transDetail", method = RequestMethod.POST)
	public ResultBO<?> transDetail(@RequestBody AgentTransLogParamVO agentTransLogParam) throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("获取token为：" + agentTransLogParam.getToken() + " 交易流水，" + agentTransLogParam.toString());
		}
		return agentService.findTransDetail(agentTransLogParam);
	}
}
