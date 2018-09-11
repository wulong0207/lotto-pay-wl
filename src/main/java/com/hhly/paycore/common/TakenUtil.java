package com.hhly.paycore.common;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.po.AgentTransTakenPO;
import com.hhly.paycore.po.TransTakenPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenValidateTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.NumberUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.vo.TakenAmountInfoVO;
import com.hhly.skeleton.pay.vo.TakenBankCardVO;
import com.hhly.skeleton.pay.vo.TakenConfirmVO;
import com.hhly.skeleton.pay.vo.TakenRealAmountVO;
import com.hhly.skeleton.pay.vo.TakenRechargeCountVO;
import com.hhly.skeleton.pay.vo.TakenReqParamVO;
import com.hhly.skeleton.pay.vo.TakenValidateTypeVO;

public class TakenUtil {
	private static final Logger logger = Logger.getLogger(TakenUtil.class);

	/**  
	* 方法说明: 验证提款参数
	* @auth: xiongJinGang
	* @param takenValidateTypeVO
	* @param takenValidateTypeEnum
	* @time: 2017年4月18日 下午4:58:05
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateTakenReq(TakenValidateTypeVO takenValidateTypeVO, TakenValidateTypeEnum takenValidateTypeEnum) {
		if (ObjectUtil.isBlank(takenValidateTypeEnum)) {
			return ResultBO.err(MessageCodeConstants.TAKEN_VALIDATE_TYPE_ERROR_SERVICE);
		}
		String validateStr = takenValidateTypeVO.getValidateStr();
		switch (takenValidateTypeEnum) {
		case EMAIL:
			if (ObjectUtil.isBlank(validateStr)) {
				return ResultBO.err(MessageCodeConstants.TAKEN_MOBILE_VALIDATE_CODE_IS_NULL_FIELD);
			}
		case IDCARD:
			if (ObjectUtil.isBlank(validateStr)) {
				return ResultBO.err(MessageCodeConstants.TAKEN_IDCARD_VALIDATE_CODE_IS_NULL_FIELD);
			}
			// 验证是否是8位
			if (validateStr.length() != Constants.VALIDATE_IDCARD_BANCARD_END_LENGTH) {
				return ResultBO.err(MessageCodeConstants.TAKEN_IDCARD_VALIDATE_CODE_ERROR_SERVICE);
			}
		case BANKCARD:
			if (ObjectUtil.isBlank(validateStr)) {
				return ResultBO.err(MessageCodeConstants.TAKEN_BANKCARD_VALIDATE_CODE_IS_NULL_FIELD);
			}
			// 验证是否是8位
			if (validateStr.length() != Constants.VALIDATE_IDCARD_BANCARD_END_LENGTH) {
				return ResultBO.err(MessageCodeConstants.TAKEN_BANKCARD_VALIDATE_CODE_ERROR_SERVICE);
			}
		case MOBILE:
			if (ObjectUtil.isBlank(validateStr)) {
				return ResultBO.err(MessageCodeConstants.TAKEN_MOBILE_VALIDATE_CODE_IS_NULL_FIELD);
			}
		default:
			break;
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证提款参数
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @time: 2017年4月19日 上午11:06:14
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateTakenParam(TakenReqParamVO takenReqParamVO, boolean validateTakenIds) {
		if (ObjectUtil.isBlank(takenReqParamVO.getBankCardId())) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_ID_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(takenReqParamVO.getTakenAmount())) {
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_IS_NULL_FIELD);
		}
		/*if (ObjectUtil.isBlank(takenReqParamVO.getTakenToken())) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_TIME_OUT_ERROR_SERVICE);
		}*/
		if (validateTakenIds) {
			if (ObjectUtil.isBlank(takenReqParamVO.getTakenIds())) {
				return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_IS_NULL_FIELD);
			}
		} else {
			if (ObjectUtil.isBlank(takenReqParamVO.getBankName())) {
				return ResultBO.err(MessageCodeConstants.BANK_NAME_IS_NULL_FIELD);
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 计算充值金额（按20%计算，充值20%的金额大于钱包中的剩余20%的金额）
	* @auth: xiongJinGang
	* @param list 充值列表
	* @param needSubServiceChargeAmount 需要计算手续的金额
	* @param takenRechargeCountVO 提款列表
	* @param takenRate 提款手续费率
	* @time: 2017年8月22日 下午4:26:28
	* @return: TakenRechargeCountVO 
	*/
	public static TakenRechargeCountVO countRecharge(List<TransRechargeBO> list, Double needSubServiceChargeAmount, TakenRechargeCountVO takenRechargeCountVO, Double takenRate) {
		if (!ObjectUtil.isBlank(list)) {
			List<TransRechargeBO> needBackList = takenRechargeCountVO.getList();
			if (ObjectUtil.isBlank(needBackList)) {
				needBackList = new ArrayList<TransRechargeBO>();
			}
			// 总的20%金额
			Double totalTop20Balance = takenRechargeCountVO.getTotalTop20Balance();
			// 总的充值金额
			Double totalBalance = takenRechargeCountVO.getTotalBalance();
			// 总的服务费
			Double totalServiceBalance = takenRechargeCountVO.getTotalServiceBalance();
			boolean isBiger = false;// 是否大于当前钱包中20%的余额
			for (TransRechargeBO transRechargeBO : list) {
				// 充值到钱包中的金额
				Double arrivalAmount = transRechargeBO.getArrivalAmount();
				// 充值时，如果用了充值红包，那们除掉购彩后的剩余金额就存在这个inWallet中了
				if (!ObjectUtil.isBlank(transRechargeBO.getInWallet())) {
					arrivalAmount = transRechargeBO.getInWallet();
				}
				if (!ObjectUtil.isBlank(arrivalAmount)) {
					// 判断充值金额，低于2元不计算
					if (MathUtil.compareTo(arrivalAmount, Double.parseDouble(Constants.NUM_2 + "")) < 0) {
						continue;
					}
					Double top20Balance = MathUtil.mul(arrivalAmount, Constants.USER_WALLET_TWENTY_PERCENT);// 拆分成20%的金额
					totalTop20Balance = MathUtil.add(totalTop20Balance, top20Balance);
					totalBalance = MathUtil.add(totalBalance, arrivalAmount);// 充值总金额
					totalServiceBalance = MathUtil.add(totalServiceBalance, MathUtil.mul(arrivalAmount, takenRate));// 异常提款手续费
					// 充值、支付状态不为空，并且类型为支付
					if (!ObjectUtil.isBlank(transRechargeBO.getRechargeType()) && PayConstants.RechargeTypeEnum.PAY.getKey().equals(transRechargeBO.getRechargeType())) {
						String orderCodes = transRechargeBO.getOrderCode();// D17081712121916600019,1;D17081809441816600001,1;D17081811171916600003,1;
						if (!RefundUtil.isSinglePay(orderCodes)) {
							transRechargeBO.setBatchPay(Short.parseShort(PayConstants.BatchPayEnum.BATCH.getKey() + ""));
						}
					}

					needBackList.add(transRechargeBO);
					if (MathUtil.compareTo(totalTop20Balance, needSubServiceChargeAmount) >= 0) {
						isBiger = true;
						break;
					}
				}
			}
			takenRechargeCountVO.setTotalBalance(totalBalance);
			takenRechargeCountVO.setTotalTop20Balance(totalTop20Balance);
			takenRechargeCountVO.setBigger(isBiger);
			takenRechargeCountVO.setList(needBackList);
			takenRechargeCountVO.setTotalServiceBalance(totalServiceBalance);
		}
		return takenRechargeCountVO;
	}

	/**  
	* 方法说明: 提款请求
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @time: 2017年4月25日 上午10:13:34
	* @return: String 
	*/
	public static String makeTakenKey(TakenReqParamVO takenReqParamVO) {
		return new StringBuffer(takenReqParamVO.getToken()).append("_").append(takenReqParamVO.getBankCardId()).append("_").append(takenReqParamVO.getTakenAmount()).toString();
	}

	/**  
	* 方法说明: 验证用户的提款次数
	* @auth: xiongJinGang
	* @param takenTimes
	* @time: 2017年4月24日 上午10:18:41
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateTakenTimes(Integer takenTimes) {
		if (!ObjectUtil.isBlank(takenTimes)) {
			if (takenTimes >= Constants.TAKEN_TIMES_FOR_ONE_DAY) {
				return ResultBO.err(MessageCodeConstants.TAKEN_TIMES_ERROR_SERVICE);
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证提款信息
	* @auth: xiongJinGang
	* @param takenIds
	* @time: 2017年4月20日 下午6:20:41
	* @return: ResultBO<?> 
	*/

	private static ResultBO<?> validateTakenIds(String takenIds) {
		takenIds = StringUtil.replaceSign(takenIds);
		Map<Integer, Integer> takenIdMap = new HashMap<Integer, Integer>();
		String[] ids = takenIds.split(",");
		for (String id : ids) {
			id = StringUtil.trimSpace(id);
			if (!NumberUtil.isDigits(id)) {
				return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
			}
			Integer idInt = Integer.parseInt(id);
			takenIdMap.put(idInt, idInt);
		}
		if (ObjectUtil.isBlank(takenIdMap)) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
		}
		return ResultBO.ok(takenIdMap);
	}

	/**  
	* 方法说明: 获取实际的提款信息
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @param takenConfirmVO
	* @time: 2017年4月25日 上午9:52:49
	* @return: ResultBO<?> 
	 * @throws ParseException 
	*/
	@SuppressWarnings({ "unchecked" })
	public static ResultBO<?> getRealTaken(TakenReqParamVO takenReqParamVO, TakenConfirmVO takenConfirmVO) throws ParseException {
		// 获取需要提现的信息
		String takenIds = takenReqParamVO.getTakenIds();
		List<TakenAmountInfoVO> takenList = takenConfirmVO.getList();
		ResultBO<?> resultBO = validateTakenIds(takenIds);
		if (resultBO.isError()) {
			return resultBO;
		}
		List<TakenAmountInfoVO> needTakenList = new ArrayList<TakenAmountInfoVO>();
		Map<Integer, Integer> takenIdMap = (Map<Integer, Integer>) resultBO.getData();
		if (takenIdMap.size() > takenList.size()) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
		}

		for (Map.Entry<Integer, Integer> takenId : takenIdMap.entrySet()) {
			boolean flag = false;
			for (TakenAmountInfoVO takenAmountInfoVO : takenList) {
				if (takenId.getKey().equals(takenAmountInfoVO.getTakenId())) {
					flag = true;
					break;
				}
			}
			if (!flag) {
				// 传过来的takenID不存在，返回错误
				return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
			}
		}
		Double totalTakenAmount = 0.0;
		for (TakenAmountInfoVO takenAmountInfoVO : takenList) {
			// 存在正常提款请求的，不能取消提款
			if (takenAmountInfoVO.getStatus().equals(PayConstants.TakenAmountStatusEnum.NORMAL.getKey())) {
				if (!takenIdMap.containsKey(takenAmountInfoVO.getTakenId())) {
					return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
				}
				totalTakenAmount = MathUtil.add(MathUtil.formatAmountToDouble(takenAmountInfoVO.getTakenAmount()), totalTakenAmount);
				needTakenList.add(takenAmountInfoVO);
			} else {
				if (takenIdMap.containsKey(takenAmountInfoVO.getTakenId())) {
					totalTakenAmount = MathUtil.add(MathUtil.formatAmountToDouble(takenAmountInfoVO.getTakenAmount()), totalTakenAmount);
					needTakenList.add(takenAmountInfoVO);
				}
			}
		}
		if (ObjectUtil.isBlank(needTakenList)) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_ERROR_SERVICE);
		}
		TakenRealAmountVO takenRealAmountVO = new TakenRealAmountVO(totalTakenAmount, needTakenList);
		return ResultBO.ok(takenRealAmountVO);
	}

	/**  
	* 方法说明: 设置提款对象
	* @auth: xiongJinGang
	* @param takenBankCardVO
	* @param payBankBO
	* @param bankcard
	* @time: 2017年4月21日 上午11:21:37
	* @return: TakenBankCardVO 
	*/
	public static TakenBankCardVO setTakenBankCardVO(TakenBankCardVO takenBankCardVO, PayBankBO payBankBO, PayBankcardBO bankcard) {
		takenBankCardVO.setBankCard(StringUtil.hideHeadString(bankcard.getCardcode()));
		takenBankCardVO.setFullBankCard(bankcard.getCardcode());// 先不存到里面
		takenBankCardVO.setBankCardId(bankcard.getId());
		takenBankCardVO.setBankId(bankcard.getBankid());
		takenBankCardVO.setBranchBankName(bankcard.getBankname());// 运行名称
		takenBankCardVO.setBankType(bankcard.getBanktype());// 银行卡类型
		takenBankCardVO.setOpenBank(bankcard.getOpenbank());// 是否开启快捷支付
		takenBankCardVO.setBankName(payBankBO.getName());// 银行名称
		takenBankCardVO.setbLogo(payBankBO.getbLogo());
		takenBankCardVO.setsLogo(payBankBO.getsLogo());
		return takenBankCardVO;
	}

	/**  
	* 方法说明: 免手续费
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @param takenUserWallet
	* @param userId
	* @time: 2017年4月19日 下午3:58:49
	* @return: ResultBO<?> 
	*/
	public static List<TakenAmountInfoVO> freeOfChargeTaken(List<TakenBankCardVO> takenBankList, Double takenAmount) {
		List<TakenAmountInfoVO> list = new ArrayList<TakenAmountInfoVO>();
		TakenAmountInfoVO takenAmountInfo = new TakenAmountInfoVO();
		takenAmountInfo.setArrivalTime(MessageFormat.format(Constants.TAKEN_ARRIVAL_TIME, DateUtil.getBeforeOrAfterDate(1), ""));
		TakenBankCardVO takenBankCard = takenBankList.get(0);
		takenAmountInfo.setBankId(takenBankCard.getBankId());
		takenAmountInfo.setBankCard(takenBankCard.getBankCard());
		takenAmountInfo.setBankName(takenBankCard.getBankName());
		takenAmountInfo.setFullBankCard(takenBankCard.getFullBankCard());
		takenAmountInfo.setExceptionRemark(null);
		takenAmountInfo.setExceptionTips(null);
		takenAmountInfo.setServiceFee("0");// 免手续费
		takenAmountInfo.setStatus(PayConstants.TakenAmountStatusEnum.NORMAL.getKey());
		takenAmountInfo.setTakenAmount(MathUtil.formatAmountToStr(takenAmount));// 提款金额
		takenAmountInfo.setTakenAmountDou(takenAmount);
		takenAmountInfo.setTakenId(0);// 自定义编号
		takenAmountInfo.setTitle(Constants.TAKEN_NORMAL_APPLICATION);
		takenAmountInfo.setServiceFeeTips(Constants.TAKEN_FREE_CHARGE_TIPS);
		list.add(takenAmountInfo);
		return list;
	}

	/**  
	* 方法说明: 账户余额低于10元，提款需要收取1元手续费
	* @auth: xiongJinGang
	* @param bankCardId
	* @param takenAmount
	* @param userId
	* @time: 2017年4月21日 下午6:45:31
	* @return: List<TakenAmountInfoVO> 
	 * @throws Exception 
	*/
	public static List<TakenAmountInfoVO> oneYuanChargeTaken(List<TakenBankCardVO> takenBankList, Double takenAmount) {
		List<TakenAmountInfoVO> list = new ArrayList<TakenAmountInfoVO>();
		TakenAmountInfoVO takenAmountInfo = new TakenAmountInfoVO();
		takenAmountInfo.setArrivalTime(MessageFormat.format(Constants.TAKEN_ARRIVAL_TIME, DateUtil.getBeforeOrAfterDate(1), ""));
		TakenBankCardVO takenBankCard = takenBankList.get(0);
		takenAmountInfo.setBankId(takenBankCard.getBankId());
		takenAmountInfo.setBankCard(takenBankCard.getBankCard());
		takenAmountInfo.setBankName(takenBankCard.getBankName());
		takenAmountInfo.setFullBankCard(takenBankCard.getFullBankCard());
		takenAmountInfo.setExceptionRemark(null);
		takenAmountInfo.setExceptionTips(null);
		takenAmountInfo.setServiceFee(String.valueOf(Constants.TOKEN_LOWEST_TEN_SERVICE_FEE));// 收1元手续费
		takenAmountInfo.setStatus(PayConstants.TakenAmountStatusEnum.NORMAL.getKey());
		takenAmountInfo.setTakenAmount(MathUtil.formatAmountToStr(takenAmount));// 提款金额
		takenAmountInfo.setTakenAmountDou(takenAmount);
		takenAmountInfo.setTakenId(0);// 自定义编号
		takenAmountInfo.setTitle(Constants.TAKEN_NORMAL_APPLICATION);
		takenAmountInfo.setServiceFeeTips(MessageFormat.format(Constants.TAKEN_NEED_CHARGE_TIPS, String.valueOf(Constants.TOKEN_LOWEST_TEN_SERVICE_FEE)));
		list.add(takenAmountInfo);
		return list;
	}

	/**  
	* 方法说明: 正常申请提款，免手续费
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param takenConfirmVO
	* @param needTakenAmount
	* @param takenBankList
	* @param totalServiceFee
	* @time: 2017年8月22日 下午12:06:31
	* @return: Double 
	*/
	public static ResultBO<?> normalTakenApply(TakenConfirmVO takenConfirmVO, Double needTakenAmount, List<TakenBankCardVO> takenBankList) {
		// 20%账户金额消费完成，都可以正常提现。 提款金额小于等于10元，需要收取1元手续费
		try {
			// 提款金额大于10元，正常提取
			List<TakenAmountInfoVO> list = freeOfChargeTaken(takenBankList, needTakenAmount);
			takenConfirmVO.setList(list);
			takenConfirmVO.setTotalServiceFee(0d);
			takenConfirmVO.setConfirmTips(Constants.TAKEN_FREE_SERVICE_AMOUNT_TIPS);
			takenConfirmVO.setActualTakenAmount(needTakenAmount);
		} catch (Exception e) {
			logger.error("用户【" + takenConfirmVO.getUserId() + "】构造正常提款信息异常", e);
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}
		return ResultBO.ok(takenConfirmVO);
	}

	/**  
	* 方法说明: 正常申请，低于10元，收一元手续费
	* @auth: xiongJinGang
	* @param takenConfirmVO
	* @param needTakenAmount
	* @param takenBankList
	* @param totalServiceFee
	* @time: 2017年8月22日 下午2:58:55
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> oneYuanTakenApply(TakenConfirmVO takenConfirmVO, Double needTakenAmount, List<TakenBankCardVO> takenBankList) {
		// 20%账户金额消费完成，都可以正常提现。 提款金额小于等于10元，需要收取1元手续费
		try {
			Double totalServiceFee = Double.parseDouble(Constants.TOKEN_LOWEST_ONE_AMOUNT + "");// 提款金额小于10元，收1元手续费
			List<TakenAmountInfoVO> list = oneYuanChargeTaken(takenBankList, needTakenAmount);
			takenConfirmVO.setList(list);
			takenConfirmVO.setConfirmTips(MessageFormat.format(Constants.TAKEN_SERVICE_AMOUNT_TIPS, list.size(), Constants.TOKEN_LOWEST_TEN_SERVICE_FEE));
			takenConfirmVO.setTotalServiceFee(totalServiceFee);
			takenConfirmVO.setActualTakenAmount(needTakenAmount);
		} catch (Exception e) {
			logger.error("用户【" + takenConfirmVO.getUserId() + "】构造一元提款信息异常", e);
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}
		return ResultBO.ok(takenConfirmVO);
	}

	/**  
	* 方法说明: 生成提款token键
	* @auth: xiongJinGang
	* @param token
	* @param takenToken
	* @time: 2017年4月25日 上午10:58:51
	* @return: String 
	*/
	public static String makeTakenTokenKey(String token) {
		return new StringBuffer(CacheConstants.P_CORE_USER_TAKEN_TOKEN).append(token).toString();
	}

	/**  
	* 方法说明: 提款BO对象转成PO对象（用户CMS后台审核提款记录）
	* @auth: xiongJinGang
	* @param list
	* @param tradeStatus
	* @throws Exception
	* @time: 2017年8月7日 上午11:39:47
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> takenBOConvertTakenPO(List<TransTakenBO> list, Short operateType) throws Exception {
		Map<String, List<TransTakenPO>> map = new HashMap<String, List<TransTakenPO>>();
		List<TransTakenPO> allList = new ArrayList<TransTakenPO>();
		List<TransTakenPO> failList = new ArrayList<TransTakenPO>();
		TransTakenPO transTakenPO = null;
		String batchCode = OrderNoUtil.getOrderNo(NumberCode.SEND_BATCH);// 批次号
		Short tradeStatus = 0;// 交易状态
		int num = 0;
		for (TransTakenBO transTakenBO : list) {
			transTakenPO = new TransTakenPO(transTakenBO);
			// 如果当前操作是审核，那么传过来的状态就只有2种，审核通过、审核不过
			if (operateType.equals(PayConstants.TakenOperateTypeEnum.AUDIT.getKey())) {
				if (num == 0) {
					// 交易状态
					tradeStatus = transTakenBO.getTransStatus();
				}
				// 只有审核通过和审核不过，才能进入以下操作
				if (!tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey()) && !tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是审核通过或者不通过");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				// 审核时，状态都是一致的，如果有一个状态不一致，返回错误
				if (!transTakenBO.getTransStatus().equals(tradeStatus)) {
					logger.info("提款编号【" + transTakenBO.getTransTakenCode() + "】的状态【" + transTakenBO.getTransStatus() + "】与其它记录的状态【" + tradeStatus + "】不同，返回失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				transTakenPO.setReviewTime(DateUtil.getNowDate());// 审核时间
				if (tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey())) {
					transTakenPO.setBatchNum(batchCode);
					// transTakenPO.setBatchStatus(PayConstants.TakenBatchStatusEnum.OPERATE_SUCCESS.getKey());// 批次交易成功
				}
				num++;
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUBMIT.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_PROCESSING.getKey()) && !transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是银行处理中或者银行处理失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				transTakenPO.setRemitTime(DateUtil.getNowDate());// 汇款时间
				if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					failList.add(transTakenPO);
				}
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.BANK_COMPLETE.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey()) && !transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是银行处理成功或者失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				transTakenPO.setDealTime(DateUtil.getNowDate());// 银行处理时间
				if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					failList.add(transTakenPO);
				}
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.CMS_COMPLETE.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.ARRIVAL_ACCOUNT.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是完成状态");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				transTakenPO.setArrivalTime(DateUtil.getNowDate());// 到账时间
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUCCESS_TO_FAIL.getKey())) {
				// 银行处理成功改成银行处理失败，判断传过来的值是不是银行处理失败
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是完成状态");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				transTakenPO.setDealTime(DateUtil.getNowDate());// 银行处理时间
				failList.add(transTakenPO);
			}
			allList.add(transTakenPO);
		}
		map.put("allList", allList);
		map.put("fail", failList);
		return ResultBO.ok(map);
	}

	/**  
	* 方法说明: 代理提款BO转向PO
	* @auth: xiongJinGang
	* @param list
	* @param operateType
	* @throws Exception
	* @time: 2018年3月10日 下午4:49:11
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> takenBOConvertAgentTakenPO(List<AgentTransTakenBO> list, Short operateType) throws Exception {
		Map<String, List<AgentTransTakenPO>> map = new HashMap<String, List<AgentTransTakenPO>>();
		List<AgentTransTakenPO> allList = new ArrayList<AgentTransTakenPO>();
		List<AgentTransTakenPO> failList = new ArrayList<AgentTransTakenPO>();
		AgentTransTakenPO agentTransTakenPO = null;
		String batchCode = OrderNoUtil.getOrderNo(NumberCode.SEND_BATCH);// 批次号
		Short tradeStatus = 0;// 交易状态
		int num = 0;
		for (AgentTransTakenBO transTakenBO : list) {
			agentTransTakenPO = new AgentTransTakenPO(transTakenBO);
			// 如果当前操作是审核，那么传过来的状态就只有2种，审核通过、审核不过
			if (operateType.equals(PayConstants.TakenOperateTypeEnum.AUDIT.getKey())) {
				if (num == 0) {
					// 交易状态
					tradeStatus = transTakenBO.getTransStatus();
				}
				// 只有审核通过和审核不过，才能进入以下操作
				if (!tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey()) && !tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是审核通过或者不通过");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				// 审核时，状态都是一致的，如果有一个状态不一致，返回错误
				if (!transTakenBO.getTransStatus().equals(tradeStatus)) {
					logger.info("提款编号【" + transTakenBO.getTransTakenCode() + "】的状态【" + transTakenBO.getTransStatus() + "】与其它记录的状态【" + tradeStatus + "】不同，返回失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				agentTransTakenPO.setReviewTime(DateUtil.getNowDate());// 审核时间
				if (tradeStatus.equals(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey())) {
					agentTransTakenPO.setBatchNum(batchCode);
					// transTakenPO.setBatchStatus(PayConstants.TakenBatchStatusEnum.OPERATE_SUCCESS.getKey());// 批次交易成功
				}
				num++;
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUBMIT.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_PROCESSING.getKey()) && !transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是银行处理中或者银行处理失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				agentTransTakenPO.setRemitTime(DateUtil.getNowDate());// 汇款时间
				if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					failList.add(agentTransTakenPO);
				}
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.BANK_COMPLETE.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey()) && !transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是银行处理成功或者失败");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				agentTransTakenPO.setDealTime(DateUtil.getNowDate());// 银行处理时间
				if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					failList.add(agentTransTakenPO);
				}
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.CMS_COMPLETE.getKey())) {
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.ARRIVAL_ACCOUNT.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是完成状态");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				agentTransTakenPO.setArrivalTime(DateUtil.getNowDate());// 到账时间
			} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUCCESS_TO_FAIL.getKey())) {
				// 银行处理成功改成银行处理失败，判断传过来的值是不是银行处理失败
				if (!transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					logger.debug("提款记录【" + transTakenBO.getTransTakenCode() + "】不是完成状态");
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
				}
				agentTransTakenPO.setDealTime(DateUtil.getNowDate());// 银行处理时间
				failList.add(agentTransTakenPO);
			}
			allList.add(agentTransTakenPO);
		}
		map.put("allList", allList);
		map.put("fail", failList);
		return ResultBO.ok(map);
	}

}
