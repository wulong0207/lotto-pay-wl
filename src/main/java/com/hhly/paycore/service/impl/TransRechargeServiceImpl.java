package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.OperateCouponMapper;
import com.hhly.paycore.dao.TransRechargeDetailMapper;
import com.hhly.paycore.dao.TransRechargeMapper;
import com.hhly.paycore.po.TransRechargeDetailPO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayChannelEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.RedTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.TransStatusEnum;
import com.hhly.skeleton.base.constants.RechargeConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PageBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.channel.vo.PayTypeResultVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

@Service("transRechargeService")
public class TransRechargeServiceImpl implements TransRechargeService {

	public static final Logger logger = Logger.getLogger(TransRechargeServiceImpl.class);

	@Resource
	private TransRechargeMapper transRechargeMapper;
	@Resource
	private TransRechargeDetailMapper transRechargeDetailMapper;
	@Resource
	private PayBankService payBankService;
	@Resource
	private OperateCouponMapper operateCouponMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;

	@Override
	public ResultBO<?> findRechargeByCode(String token, String rechargeCode) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		if (ObjectUtil.isBlank(rechargeCode)) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_IS_NULL_FIELD);
		}
		TransRechargeBO transRechargeBO = null;
		try {
			transRechargeBO = transRechargeMapper.getUserRechargeByCode(userId, rechargeCode);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(ResultBO.getMsg(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE, e));
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		if (null == transRechargeBO) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		if (!ObjectUtil.isBlank(transRechargeBO.getBankCardNum())) {
			transRechargeBO.setBankCardNum(StringUtil.hideHeadString(transRechargeBO.getBankCardNum()));
		}

		// 渠道名称
		PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByKey(transRechargeBO.getRechargeChannel());
		if (null != payChannelEnum) {
			transRechargeBO.setRechargeChannelName(payChannelEnum.getValue());
			transRechargeBO.setRechargeBankName(payChannelEnum.getValue());
		}
		// 充值银行名称
		PayBankBO payBankBO = payBankService.findBankFromCache(transRechargeBO.getRechargeBank());
		if (!ObjectUtil.isBlank(transRechargeBO.getRechargeBank())) {
			transRechargeBO.setRechargeBankName(payBankBO.getcName());
		}
		// 交易状态
		if (!ObjectUtil.isBlank(transRechargeBO.getTransStatus())) {
			TransStatusEnum transStatusEnum = PayConstants.TransStatusEnum.getEnum(transRechargeBO.getTransStatus());
			if (null != transStatusEnum) {
				transRechargeBO.setTransStatusName(transStatusEnum.getValue());
			}
		}
		// 红包不为空，查询红包
		if (!ObjectUtil.isBlank(transRechargeBO.getRedCode())) {
			OperateCouponBO operateCouponBO = operateCouponMapper.getUserCouponeByRedCode(userId, transRechargeBO.getRedCode());
			Double showAmount = transRechargeBO.getRechargeAmount();// 显示金额
			if (!ObjectUtil.isBlank(operateCouponBO)) {
				RedTypeEnum redTypeEnum = PayConstants.RedTypeEnum.getEnum(operateCouponBO.getRedType());
				switch (redTypeEnum) {
				case RECHARGE_DISCOUNT:// 充值红包
					Double leaveAmount = MathUtil.sub(transRechargeBO.getRechargeAmount(), operateCouponBO.getMinSpendAmount());// 剩余金额=充值金额-使用红包金额
					Double couponAmount = MathUtil.add(operateCouponBO.getMinSpendAmount(), operateCouponBO.getRedValue());// 充值100送20，送的总金额=100+20=120
					showAmount = MathUtil.add(leaveAmount, couponAmount);
					showAmount = MathUtil.sub(showAmount, transRechargeBO.getServiceCharge());// 展示给前端的金额
					break;
				case CONSUMPTION_DISCOUNT:// 满减红包
					break;
				case RED_COLOR:// 彩金红包
					break;
				case BONUS_RED:// 加奖红包
					break;
				case BIG_PACKAGE: // 大礼包
				case RANDOM_RED:// 随机红包
					break;
				default:
					break;
				}
			}
			// 添加符号的金额
			transRechargeBO.setShowAmount(StringUtil.formatAmount(showAmount, "+"));
		}
		return ResultBO.ok(transRechargeBO);
	}

	@Override
	public TransRechargeBO findRechargeByTransCode(String rechargeCode) {
		TransRechargeBO rechargeBO = null;
		try {
			rechargeBO = transRechargeMapper.getRechargeByCode(rechargeCode);
		} catch (Exception e) {
			logger.error("获取充值【" + rechargeCode + "】交易记录异常", e);
		}
		return rechargeBO;
	}

	@Override
	public TransRechargeBO findByTransRecharge(TransRechargeVO transRecharge) {
		return transRechargeMapper.getByTransRecharge(transRecharge);
	}

	@Override
	public List<TransRechargeBO> findRechargeByBatchCode(String batchNum) {
		return transRechargeMapper.getRechargeByBatchCode(batchNum);
	}

	@Override
	public List<TransRechargeBO> findRechargeRecordForTaken(Integer userId, Integer currentPage) {
		TransParamVO transParamVO = getTransParam(userId);
		transParamVO.setCurrentPage(currentPage);
		return transRechargeMapper.getRechargeListByPage(transParamVO);
	}

	@Override
	public Integer findRechargeRecordCountForTaken(Integer userId) {
		TransParamVO transParamVO = getTransParam(userId);
		return transRechargeMapper.getRechargeListCount(transParamVO);
	}

	/**  
	* 方法说明: 拼装获取交易参数
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年8月23日 下午4:19:02
	* @return: AgentTransLogParamVO 
	*/
	private TransParamVO getTransParam(Integer userId) {
		TransParamVO transParamVO = new TransParamVO();
		transParamVO.setUserId(userId);
		transParamVO.setTakenStatus(PayConstants.RechargeTakenStatusEnum.ALLOW.getKey());// 获取可提的充值记录
		// 需要排除人工充值及代理充值的充值记录
		// transParamVO.setExcludeChannels(new Object[] { PayConstants.PayChannelEnum.ARTIFICIAL_RECHARGE.getKey(), PayConstants.PayChannelEnum.AGENT_RECHARGE.getKey() });
		// 2018-07-13 为了配合一比分充值，代理的充值也允许提现
		transParamVO.setExcludeChannels(new Object[] { PayConstants.PayChannelEnum.ARTIFICIAL_RECHARGE.getKey() });
		transParamVO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 只查询出交易成功的
		return transParamVO;
	}

	@Override
	public ResultBO<?> findRechargeListByPage(TransParamVO transParamVO) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transParamVO.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		transParamVO.setUserId(userId);
		ResultBO<?> resultBO = TransUtil.validateCommonParam(transParamVO);
		if (resultBO.isError()) {
			return resultBO;
		}
		transParamVO = (TransParamVO) resultBO.getData();
		int count = transRechargeMapper.getRechargeListCount(transParamVO);
		if (count > 0) {
			PageBO pageBO = new PageBO(transParamVO.getShowCount(), count, transParamVO.getCurrentPage());
			List<TransRechargeBO> list = transRechargeMapper.getRechargeListByPage(transParamVO);

			if (!ObjectUtil.isBlank(list)) {
				for (TransRechargeBO transRecharge : list) {
					// 渠道名称
					if (!ObjectUtil.isBlank(transRecharge.getRechargeBank())) {
						PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByKey(transRecharge.getRechargeChannel());
						if (null != payChannelEnum) {
							transRecharge.setRechargeChannelName(payChannelEnum.getValue());
						}
					}
					// 充值银行名称
					if (!ObjectUtil.isBlank(transRecharge.getRechargeBank())) {
						PayBankBO payBankBO = payBankService.findBankFromCache(transRecharge.getRechargeBank());
						if (!ObjectUtil.isBlank(transRecharge.getRechargeBank())) {
							transRecharge.setRechargeBankName(payBankBO.getcName());
						}
					}
					// 交易状态
					if (!ObjectUtil.isBlank(transRecharge.getTransStatus())) {
						TransStatusEnum transStatusEnum = PayConstants.TransStatusEnum.getEnum(transRecharge.getTransStatus());
						if (null != transStatusEnum) {
							transRecharge.setTransStatusName(transStatusEnum.getValue());
						}
					}
				}
			}
			pageBO.setDataList(list);
			return ResultBO.ok(pageBO);
		}
		return ResultBO.ok();
	}

	@Override
	public List<TransRechargeBO> findRechargeByParam(Map<String, Object> map) {
		return transRechargeMapper.getRechargeByParam(map);
	}

	@Override
	public ResultBO<?> addRechargeTrans(TransRechargeVO transRechargeVO, UserInfoBO userInfo) {
		try {
			// 验证参数是否为空
			ResultBO<?> rechargeBO = TransUtil.validateAddRecharge(transRechargeVO);
			if (rechargeBO.isError()) {
				logger.error("添加交易记录失败：" + rechargeBO.getMessage() + "，请求参数：" + transRechargeVO.toString());
				return rechargeBO;
			}
			TransRechargePO transRechargePO = new TransRechargePO(userInfo);
			BeanUtils.copyProperties(transRechargePO, transRechargeVO);
			transRechargePO.setTransRechargeCode(transRechargeVO.getTransCode());// 交易号
			transRechargePO.setServiceCharge(0.0);// 服务费
			transRechargePO.setChannelId(transRechargeVO.getChannelId());// 渠道ID
			transRechargePO.setSwitchStatus(transRechargeVO.getSwitchStatus());// 是否切换，0不切换，1切换
			// 当前时间加上30分钟，为充值截止时间
			Date transEndTimeLast = DateUtil.addMinute(DateUtil.getNowDate(), Constants.RECHARGE_EFFECTIVE_TIME);
			transRechargePO.setTransEndTime(transEndTimeLast);// 交易截止时间
			transRechargePO.setActivityCode(transRechargeVO.getHdCode());// 充值活动编号
			int num = transRechargeMapper.addRechargeTrans(transRechargePO);
			if (num <= 0) {
				logger.info("添加充值记录失败：" + transRechargeVO.toString());
				return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("添加充值记录异常：" + transRechargeVO.toString() + "。" + e.getMessage());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> addRechargeTrans(TransRechargePO transRecharge) {
		int num = transRechargeMapper.addRechargeTrans(transRecharge);
		if (num <= 0) {
			logger.info("添加充值记录失败：" + transRecharge.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> addRechargeTransList(UserInfoBO userInfo, PayParamVO payParam, PayTypeResultVO payTypeResultVO) {
		TransRechargePO transRechargePO = null;
		try {
			transRechargePO = new TransRechargePO(userInfo);
			PayBankcardBO payBankcardBO = payParam.getPayBankcardBO();
			Short payType = PayConstants.PayTypeEnum.THIRD_PAYMENT.getKey();// 默认第三方支付
			transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 默认其它类型
			transRechargePO.setRechargeBank(payParam.getBankId());// 银行Id
			// 银行卡号不为空
			if (!ObjectUtil.isBlank(payBankcardBO)) {
				// 是否开启快捷支付 0：否，1：是
				Short openBank = payBankcardBO.getOpenbank();
				if (PayConstants.BandCardQuickEnum.HAD_OPEN.getKey().equals(openBank)) {
					payType = PayConstants.PayTypeEnum.QUICK_PAYMENT.getKey();// 快捷支付
				} else {
					payType = PayConstants.PayTypeEnum.BANK_PAYMENT.getKey();// 网银支付
				}
			} else {
				// 获取第三方支付方式
				Short payTypeInfo = RechargeConstants.getThirdTradeType(payParam.getPayBankBO());
				if (!ObjectUtil.isBlank(payType)) {
					payType = payTypeInfo;
				}
			}
			transRechargePO.setPayType(payType);
			transRechargePO.setRechargeRemark(payParam.getRemark());
			transRechargePO.setRechargePlatform(payParam.getPlatform());
			transRechargePO.setRemark(payParam.getRemark());
			Double redAmount = ObjectUtil.isBlank(payParam.getUseRedAmount()) ? 0.0 : payParam.getUseRedAmount();
			transRechargePO.setRedAmount(redAmount);// 批量支付没有红包
			transRechargePO.setRedCode(payParam.getRedCode());// 红包编号
			transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey());// 交易进行中
			transRechargePO.setTransRechargeCode(payParam.getTransCode());
			transRechargePO.setServiceCharge(0.0);// 服务费
			transRechargePO.setChannelId(payParam.getChannelId());// 渠道ID，前端传递的渠道ID
			PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByType(payTypeResultVO.getChannelCode());
			transRechargePO.setChannelCode(payTypeResultVO.getPayTypeName());
			transRechargePO.setRechargeChannel(payChannelEnum.getKey());// 充值渠道
			// 计算充值金额（减去相应的手续费，目前手续费为0），实际充值的现金金额
			transRechargePO.setRechargeAmount(MathUtil.calCounterFee(payParam.getPayAmount(), 0.0));// 每个订单需要支付的金额
			transRechargePO.setArrivalAmount(0d);// 充值到账金额
			StringBuffer orderInfo = new StringBuffer();
			List<OrderBaseInfoBO> orderList = payParam.getOrderList();

			List<TransRechargeDetailPO> detailList = new ArrayList<TransRechargeDetailPO>();
			TransRechargeDetailPO transRechargeDetailPO = null;
			for (OrderBaseInfoBO orderBaseInfoBO : orderList) {
				// 如果是单个支付，并且是合买支付
				if (payParam.getIsBatchPay().equals(PayConstants.BatchPayEnum.SINGLE.getKey()) && orderBaseInfoBO.getBuyType().equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey())) {
					transRechargePO.setGroupAmount(payParam.getUseBalance());// 使用余额金额
				}
				orderInfo.append(orderBaseInfoBO.getOrderCode()).append(",").append(orderBaseInfoBO.getBuyType()).append(";");
				transRechargeDetailPO = new TransRechargeDetailPO(payParam.getTransCode(), orderBaseInfoBO.getOrderCode(), orderBaseInfoBO.getBuyType(), orderBaseInfoBO.getOrderAmount());
				detailList.add(transRechargeDetailPO);
			}
			transRechargePO.setOrderCode(orderInfo.toString());
			transRechargePO.setTransEndTime(payParam.getEndSaleTime());// 交易截止时间
			transRechargePO.setSwitchStatus(payParam.getChange());// 是否切换，0不切换，1切换
			transRechargePO.setActivityCode(payParam.getActivityCode());
			transRechargePO.setRechargeType(PayConstants.RechargeTypeEnum.PAY.getKey());// 充值
			transRechargePO.setTakenStatus(PayConstants.RechargeTakenStatusEnum.NOT_ALLOW.getKey());// 即买即付，默认不可提
			int num = transRechargeMapper.addRechargeTrans(transRechargePO);
			if (num <= 0) {
				logger.info("添加充值记录失败");
				return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
			}
			num = transRechargeDetailMapper.addRechargeDetailList(detailList);
			if (num <= 0) {
				logger.info("添加充值记录详情失败");
				return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
			}
		} catch (Exception e) {
			logger.error("添加充值记录异常，参数【" + transRechargePO.toString() + "】", e);
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		return ResultBO.ok(transRechargePO);
	}

	@Override
	public void updateRechargeTrans(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, Short operate) throws Exception {
		try {
			TransRechargePO transRechargePO = new TransRechargePO();
			transRechargePO.setId(transRecharge.getId());
			transRechargePO.setTransRechargeCode(transRecharge.getTransRechargeCode());
			if (payNotifyResult.getStatus().getKey().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey())) {
				transRechargePO.setThirdTransNum(payNotifyResult.getThirdTradeNo());
				transRechargePO.setTransStatus(TransStatusEnum.TRADE_SUCCESS.getKey());
				transRechargePO.setArrivalAmount(transRecharge.getArrivalAmount());
			} else {
				transRechargePO.setTransStatus(TransStatusEnum.TRADE_FAIL.getKey());
				transRechargePO.setArrivalAmount(0d);
			}
			transRechargePO.setServiceCharge(transRecharge.getServiceCharge());
			try {
				String transTime = ObjectUtil.isBlank(payNotifyResult.getTradeTime()) ? DateUtil.getNow() : payNotifyResult.getTradeTime();
				transRechargePO.setThirdTransTime(DateUtil.convertStrToDate(transTime));// 20170811091631
			} catch (Exception e) {
				logger.error("格式化充值流水【" + transRecharge.getTransRechargeCode() + "】交易时间【" + payNotifyResult.getTradeTime() + "】异常");
				transRechargePO.setThirdTransTime(DateUtil.getNowDate());//
			}
			// 交易结束时间是，该笔订单或者充值的最后结束时间
			// transRechargePO.setTransEndTime(DateUtil.convertStrToDate(DateUtil.getNow()));
			transRechargePO.setResponseTime(DateUtil.convertStrToDate(DateUtil.getNow()));// 20170811091631
			transRechargePO.setUpdateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
			// 红包编号不为空，并且操作是充值操作，需要更新这个字段的金额
			if (!ObjectUtil.isBlank(transRecharge.getRedCode()) && operate.equals(RedTypeEnum.RECHARGE_DISCOUNT.getKey()) && payNotifyResult.getStatus().equals(PayStatusEnum.PAYMENT_SUCCESS)) {
				transRechargePO.setInWallet(transRecharge.getArrivalAmount());
			}
			transRechargePO.setRedCode(transRecharge.getRedCode());
			int num = transRechargeMapper.updateRechargeTrans(transRechargePO);
			logger.info("更新充值记录" + transRecharge.getTransRechargeCode() + "返回：" + (num > 0 ? "成功" : "失败"));
			if (num <= 0) {
				logger.info("更新充值记录" + transRecharge.getTransRechargeCode() + "失败：" + transRechargePO.toString());
				throw new Exception("更新充值记录失败");
			}
		} catch (Exception e) {
			logger.error("更新充值记录异常：", e);
			throw new Exception("更新充值记录异常");
		}
	}

	@Override
	public void updateRechargeTakenStatusByBatch(List<TransRechargePO> rechargeList) throws Exception {
		int num = transRechargeMapper.updateRechargeTakenStatusByBatch(rechargeList);
		if (num <= 0) {
			logger.info("批量更新充值提款状态失败");
			throw new RuntimeException("批量更新充值提款状态失败");
		}
	}

	@Override
	public void updateRechargeTransForBatch(List<TransRechargeBO> rechargeList, PayNotifyResultVO payNotifyResult) throws Exception {
		List<TransRechargePO> list = new ArrayList<TransRechargePO>();
		TransRechargePO transRechargePO = null;
		for (TransRechargeBO transRechargeBO : rechargeList) {
			transRechargePO = new TransRechargePO();
			transRechargeBO.setTransStatus(payNotifyResult.getStatus().getKey());
			Date nowDate = DateUtil.convertStrToDate(DateUtil.getNow());
			// 交易结束时间是，该笔订单或者充值的最后结束时间
			// String transTime = ObjectUtil.isBlank(payNotifyResult.getTradeTime()) ? DateUtil.getNow() : payNotifyResult.getTradeTime();
			// transRechargeBO.setTransEndTime(DateUtil.convertStrToDate(transTime));
			transRechargeBO.setResponseTime(nowDate);
			transRechargeBO.setUpdateTime(nowDate);
			// 交易成功，才有交易号和交易金额
			if (payNotifyResult.getStatus().getKey().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey())) {
				transRechargeBO.setThirdTransNum(payNotifyResult.getThirdTradeNo());
				// transRechargeBO.setArrivalAmount(arrivalAmount);
			}
			BeanUtils.copyProperties(transRechargePO, transRechargeBO);
			list.add(transRechargePO);
		}
		int num = transRechargeMapper.updateBatch(list);
		if (num <= 0) {
			logger.info("批量更新充值记录失败");
			throw new RuntimeException("更新充值记录失败");
		}
	}

	@Override
	public int updateRechargeTransRedInfo(TransRechargeBO transRecharge) throws Exception {
		TransRechargePO transRechargePO = new TransRechargePO();
		transRechargePO.setRedAmount(transRecharge.getRedAmount());
		transRechargePO.setRedCode(transRecharge.getRedCode());
		transRechargePO.setTransRechargeCode(transRecharge.getTransRechargeCode());
		transRechargePO.setId(transRecharge.getId());
		return transRechargeMapper.updateRechargeTrans(transRechargePO);
	}

	@Override
	public int updateRecharge(TransRechargePO transRechargePO) throws Exception {
		return transRechargeMapper.update(transRechargePO);
	}

}
