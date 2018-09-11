package com.hhly.paycore.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.ChannelTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.CmsRechargeTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.RedStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.constants.PayConstants.UserTransMoneyFlowEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.NumberUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankLimitBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.AgentPayVO;
import com.hhly.skeleton.pay.vo.BatchPayOrderVO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.PayAttachVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.RechargeParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;

/**
 * @desc 支付参数的验证
 * @author xiongjingang
 * @date 2017年3月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PayUtil {

	private static Logger logger = Logger.getLogger(PayUtil.class);

	/**  
	* 方法说明: 传递给第三方支付的额外字段
	* @auth: xiongJinGang
	* @param transCode
	* @param buyType
	* @time: 2017年4月25日 下午6:45:59
	* @return: String 
	*/
	public static String getAttachStr(String transCode, Integer isBatchPay, Integer userId) {
		PayAttachVO payAttachVO = new PayAttachVO(transCode, isBatchPay, userId);
		return payAttachVO.toString();
	}

	/**  
	* 方法说明: 根据返回额外字符串得到对象
	* @auth: xiongJinGang
	* @param attachInfo
	* @time: 2017年4月25日 下午6:50:54
	* @return: PayAttachVO 
	*/
	public static PayAttachVO getAttachObj(String attachInfo) {
		if (ObjectUtil.isBlank(attachInfo)) {
			return null;
		}
		String[] attachInfos = attachInfo.split(";");
		PayAttachVO payAttachVO = new PayAttachVO();
		for (int i = 0; i < attachInfos.length; i++) {
			switch (i) {
			case 0:// 交易号
				payAttachVO.setTransCode(attachInfos[i]);
				continue;
			case 1:// 是否批量支付
				payAttachVO.setIsBatchPay(Integer.parseInt(attachInfos[i]));
				continue;
			case 2:// 用户Id
				payAttachVO.setUserId(Integer.parseInt(attachInfos[i]));
				continue;
			default:
				break;
			}
		}
		return payAttachVO;
	}

	/**  
	* 方法说明: 验证支付参数的有效性
	* @auth: xiongjingang
	* @param paramVO
	* @time: 2017年3月16日 上午11:21:34
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validatePayParams(PayParamVO payParam) {
		if (ObjectUtil.isBlank(payParam.getOrderCode())) {
			return ResultBO.err(MessageCodeConstants.ORDER_CODE_IS_NULL_FIELD);
		}
		// 余额不为空，判断传入的金额是否为负数
		if (!ObjectUtil.isBlank(payParam.getBalance())) {
			if (Double.compare(payParam.getBalance(), 0) < 0) {
				return ResultBO.err(MessageCodeConstants.PAY_BALANCE_ERROR_SERVICE);
			}
		}
		if (ObjectUtil.isBlank(payParam.getToken())) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		if (ObjectUtil.isBlank(payParam.getClientIp())) {
			return ResultBO.err(MessageCodeConstants.CLIENT_IP_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(payParam.getPlatform())) {
			return ResultBO.err(MessageCodeConstants.TRANS_PLATFORM_IS_NULL_FIELD);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证充值参数
	* @auth: xiongJinGang
	* @param rechargeParam
	* @time: 2017年4月10日 下午3:34:29
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateRechargeParams(RechargeParamVO rechargeParam) {
		if (ObjectUtil.isBlank(rechargeParam.getToken())) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}

		if (ObjectUtil.isBlank(rechargeParam.getBankId())) {
			return ResultBO.err(MessageCodeConstants.TRANS_PAY_TYPE_IS_NULL_FIELD);
		}

		if (ObjectUtil.isBlank(rechargeParam.getRechargeAmount())) {
			return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_BALANCE_IS_NULL_FIELD);
		} else {
			// 测试环境先不验证
			/*if (isTest.equals("false")) {
				// 判断充值金额是否低于最低充值金额(生产环境验证)
				if (MathUtil.compareTo(rechargeParam.getRechargeAmount(), Constants.RECHARGE_LOWEST_AMOUNT) < 0) {
					return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_AMOUNT_ERROR_SERVICE);
				}
			}*/
		}
		if (ObjectUtil.isBlank(rechargeParam.getClientIp())) {
			return ResultBO.err(MessageCodeConstants.CLIENT_IP_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(rechargeParam.getPlatform())) {
			return ResultBO.err(MessageCodeConstants.PAY_PLATFORM_IS_NULL_FIELD);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证推单请求参数
	* @auth: xiongJinGang
	* @param payParamVO
	* @time: 2018年1月11日 下午4:39:44
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validatePushPayParams(PayParamVO payParam) {
		if (ObjectUtil.isBlank(payParam.getIssueId())) {
			return ResultBO.err(MessageCodeConstants.FIND_ISSUE_INFO_FAIL);
		}
		// 余额不为空，判断传入的金额是否为负数
		if (!ObjectUtil.isBlank(payParam.getBalance())) {
			if (Double.compare(payParam.getBalance(), 0) < 0) {
				return ResultBO.err(MessageCodeConstants.PAY_BALANCE_ERROR_SERVICE);
			}
		}
		if (ObjectUtil.isBlank(payParam.getToken())) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		if (ObjectUtil.isBlank(payParam.getClientIp())) {
			return ResultBO.err(MessageCodeConstants.CLIENT_IP_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(payParam.getPlatform())) {
			return ResultBO.err(MessageCodeConstants.TRANS_PLATFORM_IS_NULL_FIELD);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证红包使用条件
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @param payParamVO
	* @time: 2017年4月6日 下午4:27:02
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateRed(OperateCouponBO operateCouponBO, PayParamVO payParamVO, PayOrderBaseInfoVO orderInfoBO, UserWalletBO userWalletBO) {
		try {
			String limitPlagform = operateCouponBO.getLimitPlatform();
			Short currentPlatform = payParamVO.getPlatform();// 当前平台
			String limitLottery = operateCouponBO.getLimitLottery();// 限制具体彩种使用
			Double totalAmount = orderInfoBO.getOrderAmount();// 总订单金额
			if (ObjectUtil.isBlank(operateCouponBO) || (!ObjectUtil.isBlank(operateCouponBO) && operateCouponBO.getUserId().intValue() != payParamVO.getUserId().intValue())) {
				return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
			}
			// 判断非彩金红包是否过期（因为彩金红包可能没有过期时间）
			if (!operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
				int num = DateUtil.compare(operateCouponBO.getRedOverdueTime(), DateUtil.getNowDate());
				if (num <= 0) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_EXPIRED_ERROR_SERVICE);// 红包已过期
				}
			}
			// 判断红包状态是否可用
			ResultBO<?> resultBO = validateRedStatus(operateCouponBO.getRedStatus());
			if (resultBO.isError()) {
				return resultBO;
			}

			// 限制平台不为空，进行检验
			if (!ObjectUtil.isBlank(limitPlagform)) {
				// 红包的使用平台与当前平台不匹配，返回错误
				boolean validateFlag = OperateCouponUtil.validateLimitPlatform(limitPlagform, currentPlatform);
				if (!validateFlag) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_LIMIT_PLATFORM_ERROR_SERVICE);
				}
			}

			// 如果限制彩种编号不为空则进行判断
			if (!ObjectUtil.isBlank(limitLottery)) {
				boolean flag = OperateCouponUtil.validateLotteryCode(limitLottery, orderInfoBO.getLotteryCode());
				if (!flag) {
					logger.info("用户【" + operateCouponBO.getUserId() + "】彩种编号【" + orderInfoBO.getLotteryCode() + "】，红包可用彩种编号【" + limitLottery + "】");
					return ResultBO.err(MessageCodeConstants.PAY_RED_LIMIT_LOTTORY_ERROR_SERVICE);
				}
			}
			// 彩金红包判断
			if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
				// 彩金红包的剩余金额大于等于订单的支付金额，并且支付方式不为空，返回错误 || 彩金红包足够支付，但还是传了余额金额，返回错误
				if (MathUtil.compareTo(operateCouponBO.getRedBalance(), totalAmount) >= 0 && (!ObjectUtil.isBlank(payParamVO.getBankId()) || !ObjectUtil.isBlank(payParamVO.getBalance()))) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_COLOR_ENOUTH_ERROR_SERVICE);
				}
				// 用户账户中的可用红包余额小于当前使用的彩金红包余额，返回错误
				if (MathUtil.compareTo(userWalletBO.getEffRedBalance(), operateCouponBO.getRedBalance()) < 0) {
					logger.info("用户【" + operateCouponBO.getUserId() + "】账户红包金额小于彩金红包【" + operateCouponBO.getRedCode() + "】可用金额【" + operateCouponBO.getRedBalance() + "】");
					return ResultBO.err(MessageCodeConstants.PAY_RED_AMOUNT_ERROR_SERVICE);
				}
			} else if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.CONSUMPTION_DISCOUNT.getKey())) {
				// 满减红包判断
				// Double redValue = operateCouponBO.getRedValue();// 面额（满100减20，存的是20）
				Integer minSpendAmount = operateCouponBO.getMinSpendAmount();// 最低消费金额（满100减20，存的是100）
				// 订单金额不满足最低消费金额
				if (MathUtil.compareTo(minSpendAmount, totalAmount) > 0) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_NOT_SUIT_ERROR_SERVICE);
				}
			}
		} catch (Exception e) {
			logger.error("验证红包【" + operateCouponBO.getRedCode() + "】使用条件异常：", e);
			return ResultBO.err(MessageCodeConstants.PAY_RED_NOT_SUIT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证充值红包，（红包类型，使用平台 ）
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @param rechargeParam
	* @time: 2017年4月10日 下午5:49:03
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateRechargeRed(OperateCouponBO operateCouponBO, RechargeParamVO rechargeParam) {
		String limitPlagform = operateCouponBO.getLimitPlatform();
		Short currentPlatform = rechargeParam.getPlatform();// 当前平台
		// 1：充值优惠；2：消费折扣；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
		// 验证红包类型
		if (!operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey())) {
			// 查询出来的红包不是充值红包
			return ResultBO.err(MessageCodeConstants.PAY_RED_LIMIT_PLATFORM_ERROR_SERVICE);
		}
		// 验证红包的状态
		ResultBO<?> resultBO = validateRedStatus(operateCouponBO.getRedStatus());
		if (resultBO.isError()) {
			return resultBO;
		}
		// 验证红包是否过期
		int num = DateUtil.compare(operateCouponBO.getRedOverdueTime(), DateUtil.getNowDate());
		if (num <= 0) {
			return ResultBO.err(MessageCodeConstants.PAY_RED_EXPIRED_ERROR_SERVICE);// 红包已过期
		}
		// 验证红包使用平台
		if (!ObjectUtil.isBlank(limitPlagform)) {
			// 红包的使用平台与当前平台不匹配，返回错误
			boolean validateFlag = OperateCouponUtil.validateLimitPlatform(limitPlagform, currentPlatform);
			if (!validateFlag) {
				return ResultBO.err(MessageCodeConstants.PAY_RED_LIMIT_PLATFORM_ERROR_SERVICE);
			}
		}
		// 充值金额小于红包面额，返回错误
		if (MathUtil.compareTo(operateCouponBO.getMinSpendAmount(), rechargeParam.getRechargeAmount()) > 0) {
			return ResultBO.err(MessageCodeConstants.PAY_RED_NOT_SUIT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 检查银行单笔最大限额
	* @auth: xiongJinGang
	* @param payParam
	* @param payBankcard
	* @param payBankLimit
	* @time: 2017年4月7日 下午12:17:46
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> checkPayLimit(PayParamVO payParam, PayBankLimitBO payBankLimit) {
		if (!ObjectUtil.isBlank(payBankLimit)) {
			String limitTime = payBankLimit.getLimitTime();// 单笔限额
			if (!ObjectUtil.isBlank(limitTime)) {
				if (NumberUtil.isDouble(limitTime)) {
					Double payAmount = ObjectUtil.isBlank(payParam.getPayAmount()) ? 0.0 : payParam.getPayAmount();// 支付金额
					int result = MathUtil.compareTo(Double.valueOf(limitTime), payAmount);
					if (result < 0) {
						return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_LIMIT_ERROR_SERVICE);
					}
				}
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 获取订单号
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年6月27日 上午10:31:58
	* @return: String 
	*/
	public static String getOrderCode(List<PayOrderBaseInfoVO> list) {
		String orderCodes = "";
		if (!ObjectUtil.isBlank(list)) {
			for (PayOrderBaseInfoVO payOrderBaseInfo : list) {
				orderCodes += payOrderBaseInfo.getOrderCode() + ",";
			}
		}
		return orderCodes;
	}

	/**  
	* 方法说明: 验证彩金红包状态是否可用，金额是否够付（支付回调后验证）
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年3月27日 下午3:37:32
	* @return: ResultBO 
	*/
	public static ResultBO<?> validateOperateCoupon(TransRechargeBO transRecharge, OperateCouponBO operateCouponBO) throws Exception {
		// 验证当前红包的状态
		ResultBO<?> resultBO = validateRedStatus(operateCouponBO.getRedStatus());
		if (resultBO.isError()) {
			return resultBO;// 红包不是可使用状态
		}

		// 判断红包是否为彩金红包，如果是，判断彩金红包够不够支付前的金额
		if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
			Double redBlance = operateCouponBO.getRedBalance();
			Double redAmount = transRecharge.getRedAmount();// 使用的红包金额
			int redMoney = MathUtil.compareTo(redBlance, redAmount);
			// 当前彩金红包中够扣（避免支付时够扣，回调回来后又不够扣的情况）
			if (redMoney < 0) {
				logger.info("彩金红包【" + operateCouponBO.getRedCode() + "】中的剩余彩金不够订单【" + transRecharge.getOrderCode() + "】本次支付！");
				return ResultBO.err(MessageCodeConstants.PAY_RED_BALANCE_NOT_ENOUGH_ERROR_SERVICE);
			}
		} else {
			// 其它红包
			logger.info("其它红包暂时不能使用");
			return ResultBO.err(MessageCodeConstants.PAY_RED_BAND_USE_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证账户余额是否够支付
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param orderInfoBO
	* @throws Exception
	* @time: 2017年4月11日 上午10:34:50
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateUserWalletBalance(UserWalletBO userWalletBO, Double needSubMoney) throws Exception {
		// 未使用彩金红包信息，判断总现金余额中的金额是否大于当前的订单金额
		int compareResult = MathUtil.compareTo(userWalletBO.getTotalAmount(), needSubMoney);
		if (compareResult < 0) {
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 批量支付时，验证账户中 总现金金额+中奖金额 是否大于订单总金额
	* @auth: xiongJinGang
	* @param totalAmount 总现金金额
	* @param needSubAmount 需要支付的订单金额
	* @throws Exception
	* @time: 2017年5月11日 下午4:40:15
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateUserWalletBalance(Double totalAmount, Double needSubAmount, TransRechargeBO transRecharge) throws Exception {
		Double redMoney = transRecharge.getRedAmount();// 红包金额
		// 红包金额不为空
		if (!ObjectUtil.isBlank(redMoney)) {
			needSubAmount = MathUtil.sub(needSubAmount, redMoney);
		}
		// 判断总现金余额中的金额是否大于当前的订单金额
		int compareResult = MathUtil.compareTo(totalAmount, needSubAmount);
		if (compareResult < 0) {
			logger.info("用户【" + transRecharge.getUserId() + "】账户总金额：" + totalAmount + "，需要扣除的金额：" + needSubAmount + "，不够扣除");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		// 已经减去了使用红包的金额
		return ResultBO.ok(needSubAmount);
	}

	/**  
	* 方法说明: 计算订单总额
	* @auth: xiongJinGang
	* @param orderTotalList
	* @time: 2017年5月25日 下午8:51:35
	* @return: Map<String, Object> 
	*/
	public static Map<String, Object> countOrderTotalAmount(List<PayOrderBaseInfoVO> orderTotalList) {
		Double needSubMoney = 0.0;
		Map<String, Object> map = new HashMap<String, Object>();
		// 计算订单总金额
		int num = 0;
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : orderTotalList) {
			needSubMoney = MathUtil.add(needSubMoney, payOrderBaseInfoVO.getOrderAmount());
			// 跟产品沟通，批量支付只有一个彩种
			if (num == 0) {
				map.put("lotteryCode", payOrderBaseInfoVO.getLotteryCode());
			}
		}
		map.put("totalAmount", needSubMoney);
		return map;
	}

	/**  
	* 方法说明: 获取支付渠道
	* @auth: xiongJinGang
	* @param channelType
	* @time: 2017年3月23日 上午9:51:33
	* @return: ChannelTypeEnum 
	*/
	public static ChannelTypeEnum getChannelType(String channelType) {
		return ChannelTypeEnum.from(channelType);
	}

	/**  
	* 方法说明: 获取支付方法
	* @auth: xiongJinGang
	* @param channelType
	* @time: 2017年3月23日 上午9:51:00
	* @return: String 
	*/
	public static String getPayMethod(String channelType, String url) {
		ChannelTypeEnum channelTypeEnum = getChannelType(channelType);
		String method = channelTypeEnum.getChannel();
		return url.replace("{method}", method);
	}

	/**
	 * @Description: 获取支付方法
	 * @param value
	 * @param replaceKey
	 * @param url
	 * @return String
	 * @author wuLong
	 * @date 2017年8月17日 下午6:22:16
	 */
	public static String getPayMethod(String value, String replaceKey, String url) {
		return url.replace(replaceKey, value);
	}

	/**  
	* 方法说明: 获取支付方法
	* @auth: xiongJinGang
	* @param channelType
	* @param url
	* @time: 2017年5月3日 下午3:53:52
	* @return: String 
	*/
	public static String getPayMethod(ChannelTypeEnum channelTypeEnum, String url) {
		String method = channelTypeEnum.getChannel();
		return url.replace("{method}", method);
	}

	/**  
	* 方法说明: 验证红包状态
	* @auth: xiongJinGang
	* @param redStatus
	* @time: 2017年4月11日 上午10:11:47
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateRedStatus(String redStatus) {
		RedStatusEnum RedStatusEnum = PayConstants.RedStatusEnum.getEnumByKey(redStatus);
		switch (RedStatusEnum) {
		case WAITTING_ACTIVATION:// 待激活
			return ResultBO.err(MessageCodeConstants.PAY_RED_WAITTING_ACTIVATION_ERROR_SERVICE);
		case WAITTING_DISTRIBUTE:// 待派发
			return ResultBO.err(MessageCodeConstants.PAY_RED_WAITTING_ACTIVATION_ERROR_SERVICE);
		case NORMAL:// 正常
			return ResultBO.ok();
		case EXPIRED:// 已过期
			return ResultBO.err(MessageCodeConstants.PAY_RED_EXPIRED_ERROR_SERVICE);
		case INVALID:// 已作废
			return ResultBO.err(MessageCodeConstants.PAY_RED_INVALID_ERROR_SERVICE);
		case ALREADY_USE:// 已使用
			return ResultBO.err(MessageCodeConstants.PAY_RED_ALREADY_USE_ERROR_SERVICE);
		default:
			return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
		}
	}

	/**  
	* 方法说明: 根据交易类型获取交易编号头
	* @param transType
	* @time: 2017年3月10日 上午10:33:23
	* @return: NumberCode 
	*/
	public static NumberCode getNoHeadByTradeType(Short transType) {
		UserTransMoneyFlowEnum flowType = PayConstants.UserTransMoneyFlowEnum.getTransTypeByKey(transType);
		NumberCode numberCode = null;
		// 根据交易类型获取交易编号头，未获取到默认支出头
		if (MoneyFlowEnum.IN.getKey().equals(flowType.getType())) {
			numberCode = OrderEnum.NumberCode.RUNNING_WATER_IN;
		} else {
			numberCode = OrderEnum.NumberCode.RUNNING_WATER_OUT;
		}
		return numberCode;
	}

	/**  
	* 方法说明: 获取支付返回的mock数据
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年3月28日 下午2:40:09
	* @return: PayNotifyResultVO 
	*/
	public static PayNotifyResultVO getPayNotifyResultMock(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setPayId(System.currentTimeMillis() + "");// 支付号
		payNotifyResult.setAttachData(map.get("attach"));// 支付时的附加数据，异步通知时原样返回
		Double orderAmt = ObjectUtil.isBlank(map.get("total_fee")) ? 0.0 : Double.valueOf(map.get("total_fee"));
		payNotifyResult.setOrderAmt(orderAmt);// 订单金额、到账金额（元），微信的需要将分转成元
		payNotifyResult.setChannelType(map.get("channelTypeName"));// 支付渠道类型
		payNotifyResult.setThirdTradeNo(System.currentTimeMillis() + "");// 第三方交易号
		payNotifyResult.setTradeTime(DateUtil.getNow());// 付款时间
		payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);// 支付状态
		payNotifyResult.setResponse("success");// 响应给第三方支付异步回调的数据（SUCCESS、{"ret_code":"0000","ret_msg":"交易成功"}）
		payNotifyResult.setOrderCode(map.get("out_trade_no"));// 益彩订单号
		payNotifyResult.setSettleDate(DateUtil.getNow(DateUtil.DATE_FORMAT));// YYYYMMDD 清算日期
		return payNotifyResult;
	}

	/**  
	* 方法说明: 判断是否为活动订单，true：是，false：否
	* @auth: xiongJinGang
	* @param orderList
	* @time: 2017年8月12日 上午9:24:29
	* @return: boolean 
	*/
	public static boolean isActivityOrder(List<OrderBaseInfoBO> orderList) {
		boolean flag = false;
		// 活动订单只能单个支付，如果数量超过1，肯定不是活动订单
		if (orderList.size() == 1) {
			for (OrderBaseInfoBO orderBaseInfoBO : orderList) {
				if (!ObjectUtil.isBlank(orderBaseInfoBO.getActivityCode())) {
					flag = true;
				}
			}
		}
		return flag;
	}

	/**  
	* 方法说明: 验证订单的订单、支付、开奖状态
	* @auth: xiongJinGang
	* @param orderBaseInfoBO
	* @time: 2017年4月27日 下午12:17:54
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateOrderStatus(List<OrderBaseInfoBO> orderList, PayParamVO payParam) {
		Double totalPayMoney = 0.0;
		ToPayEndTimeVO toPayEndTimeVO = new ToPayEndTimeVO();
		Date endSaleTime = null;
		String orderCode = null;
		OrderBaseInfoBO orderBaseInfo = null;
		int num = 0;
		for (OrderBaseInfoBO orderInfo : orderList) {
			/**************判断支付结束时间开始******************/
			if (ObjectUtil.isBlank(orderInfo.getEndSaleTime())) {
				logger.info("订单【" + payParam.getOrderCode() + "】支付截止时间为空");
				return ResultBO.err(MessageCodeConstants.PAY_END_TIME_IS_NULL_FIELD_SERVICE);
			}
			int days = DateUtil.compare(orderInfo.getEndSaleTime(), new Date());
			if (days <= 0) {
				logger.info("订单【" + payParam.getOrderCode() + "】已过支付截止时间【" + orderInfo.getEndSaleTime() + "】");
				return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
			}

			if (num == 0) {
				endSaleTime = orderInfo.getEndSaleTime();// 支付截止时间
				orderBaseInfo = orderInfo;
			} else {
				// 取一个最短的时间
				int compareResult = DateUtil.compare(endSaleTime, orderInfo.getEndSaleTime());
				if (compareResult > 0) {
					orderCode = payParam.getOrderCode();
					endSaleTime = orderInfo.getEndSaleTime();
				}
			}

			/**************判断支付结束时间结束******************/
			// 合买不判断支付状态
			if (!orderInfo.getBuyType().equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey())) {
				if (ObjectUtil.isBlank(orderInfo.getPayStatus())) {
					logger.info("订单【" + orderInfo.getOrderCode() + "】支付状态【" + orderInfo.getPayStatus() + "】为空，不能支付");
					return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
				}
				if (orderInfo.getPayStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey())) {
					logger.info("订单【" + orderInfo.getOrderCode() + "】支付状态已支付成功，不能重复支付");
					return ResultBO.err(MessageCodeConstants.ORDER_HAD_PAY);
				}
				
				if (!orderInfo.getPayStatus().equals(Integer.valueOf(PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey() + "")) && !orderInfo.getPayStatus().equals(Integer.valueOf(PayConstants.PayStatusEnum.BEING_PAID.getKey() + ""))) {
					logger.info("订单【" + payParam.getOrderCode() + "】当前支付状态【" + orderInfo.getPayStatus() + "】不能支付");
					return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
				}
			}

			// 判断是否为追号计划
			if (orderInfo.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				// 批量支付和单个支付，查询订单的接口不一样
				if (payParam.getIsBatchPay().equals(PayConstants.BatchPayEnum.SINGLE.getKey())) {
					// 支付状态：等待支付；订单状态：追号中；中奖状态：未中奖
					if (ObjectUtil.isBlank(orderInfo.getAddStatus()) || !orderInfo.getAddStatus().equals(Integer.parseInt(OrderEnum.AddStatus.CHASING.getKey() + ""))) {
						logger.info("单个订单【" + payParam.getOrderCode() + "】支付，当前追号状态orderStatus【" + orderInfo.getOrderStatus() + "】addStatus()【" + orderInfo.getAddStatus() + "】不能支付");
						return ResultBO.err(MessageCodeConstants.PAY_ORDER_ADD_STATUS_ERROR_SERVICE);
					}
				} else {
					// 支付状态：等待支付；订单状态：追号中；中奖状态：未中奖
					if (ObjectUtil.isBlank(orderInfo.getOrderStatus()) || !orderInfo.getOrderStatus().equals(Integer.parseInt(OrderEnum.AddStatus.CHASING.getKey() + ""))) {
						logger.info("批量订单【" + payParam.getOrderCode() + "】支付，当前追号状态orderStatus【" + orderInfo.getOrderStatus() + "】addStatus()【" + orderInfo.getAddStatus() + "】不能支付");
						return ResultBO.err(MessageCodeConstants.PAY_ORDER_ADD_STATUS_ERROR_SERVICE);
					}
				}
			} else if (orderInfo.getBuyType().equals(PayConstants.BuyTypeEnum.PURCHASING.getKey()) || orderInfo.getBuyType().equals(PayConstants.BuyTypeEnum.CHASE_NUMBER.getKey())) {
				// 支付状态为：等待支付；订单状态为：待拆票；中奖状态为:等待开奖
				if (ObjectUtil.isBlank(orderInfo.getOrderStatus()) || !orderInfo.getOrderStatus().equals(Integer.parseInt(OrderEnum.OrderStatus.WAITING_SPLIT_TICKET.getValue() + ""))) {
					logger.info("订单【" + orderInfo.getOrderCode() + "】当前订单状态【" + orderInfo.getOrderStatus() + "】不能支付");
					return ResultBO.err(MessageCodeConstants.PAY_ORDER_STATUS_ERROR_SERVICE);
				}
				if (ObjectUtil.isBlank(orderInfo.getWinningStatus()) || !orderInfo.getWinningStatus().equals(Integer.parseInt(OrderEnum.OrderWinningStatus.NOT_DRAW_WINNING.getValue() + ""))) {
					logger.info("订单【" + payParam.getOrderCode() + "】当前中奖状态【" + orderInfo.getWinningStatus() + "】不能支付");
					return ResultBO.err(MessageCodeConstants.PAY_ORDER_WINNING_STATUS_ERROR_SERVICE);
				}
			}
			totalPayMoney = MathUtil.add(totalPayMoney, orderInfo.getOrderAmount());
			num++;
		}

		// 使用了余额+银行支付，判断余额支付是否大于订单金额,如果大于，返回错误
		if (!ObjectUtil.isBlank(payParam.getBalance()) && !ObjectUtil.isBlank(payParam.getBankId())) {
			if (MathUtil.compareTo(payParam.getBalance(), totalPayMoney) >= 0) {
				logger.info("用户【" + payParam.getUserId() + "】余额【" + payParam.getBalance() + "】足够订单金额【" + totalPayMoney + "】，无需再用银行【" + payParam.getBankId() + "】支付");
				return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ENOUTH_ERROR_SERVICE);
			}
		}
		toPayEndTimeVO.setOrderAmount(totalPayMoney);
		orderBaseInfo.setEndSaleTime(endSaleTime);
		toPayEndTimeVO.setOrderBaseInfo(orderBaseInfo);
		// 支付结束时间
		if (!ObjectUtil.isBlank(endSaleTime)) {
			int days = DateUtil.compare(endSaleTime, new Date());
			if (days <= 0) {
				logger.info("订单【" + orderCode + "】已过支付截止时间【" + endSaleTime + "】");
				return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
			}
			Long leavePayTime = DateUtil.compareAndGetSeconds(endSaleTime, new Date());
			toPayEndTimeVO.setLeavePayTime(leavePayTime);
		}
		return ResultBO.ok(toPayEndTimeVO);
	}

	/**  
	* 方法说明: 获取充值描述
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年4月28日 上午9:32:05
	* @return: String 
	*/
	public static String getRechargeRemark(TransRechargeBO transRecharge, PayBankBO payBankBO) {
		String remark = payBankBO.getName();
		if (ObjectUtil.isBlank(transRecharge.getBankCardNum())) {
			remark += "支付";
		} else {
			remark += "：" + StringUtil.hideHeadString(transRecharge.getBankCardNum()) + "支付";
		}
		return remark;
	}

	/**  
	* 方法说明: 购彩描述
	* @auth: xiongJinGang
	* @param payOrderBaseInfoVO
	* @time: 2017年4月28日 上午9:57:38
	* @return: String 
	*/
	public static String getGouCaiRemark(PayOrderBaseInfoVO payOrderBaseInfoVO) {
		StringBuffer stringBuffer = new StringBuffer();
		if (!payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey())) {
			stringBuffer.append(payOrderBaseInfoVO.getLotteryName());
		}
		String buyTypeName = PayConstants.BuyTypeEnum.getEnum(payOrderBaseInfoVO.getBuyType()).getValue();
		stringBuffer.append(buyTypeName).append("|").append(payOrderBaseInfoVO.getLotteryIssue()).append("期");
		return stringBuffer.toString();
	}

	/**  
	* 方法说明: 撤单描述
	* @auth: xiongJinGang
	* @param payOrderBaseInfoVO
	* @time: 2017年12月2日 下午12:18:14
	* @return: String 
	*/
	public static String getRefundRemark(PayOrderBaseInfoVO payOrderBaseInfoVO) {
		StringBuffer stringBuffer = new StringBuffer();
		if (payOrderBaseInfoVO.getBuyType().intValue() != PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey().intValue()) {
			stringBuffer.append(payOrderBaseInfoVO.getLotteryName());
		}
		String buyTypeName = PayConstants.BuyTypeEnum.getEnum(payOrderBaseInfoVO.getBuyType()).getValue();
		stringBuffer.append(buyTypeName);
		return stringBuffer.toString();
	}

	public static String getGouCaiRemark(OrderInfoBO orderInfoBO) {
		StringBuffer stringBuffer = new StringBuffer(orderInfoBO.getLotteryName());
		String buyTypeName = PayConstants.BuyTypeEnum.getEnum(Integer.parseInt(String.valueOf(orderInfoBO.getBuyType()))).getValue();
		stringBuffer.append(buyTypeName).append("|").append(orderInfoBO.getLotteryIssue()).append("期");
		return stringBuffer.toString();
	}

	/**  
	* 方法说明: 合买抽成的描述
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @time: 2018年5月22日 下午12:00:58
	* @return: String 
	*/
	public static String getOrderGroupCommissonRemark(OrderInfoBO orderInfoBO) {
		StringBuffer stringBuffer = new StringBuffer(orderInfoBO.getLotteryName());
		String buyTypeName = PayConstants.BuyTypeEnum.getEnum(Integer.parseInt(String.valueOf(orderInfoBO.getBuyType()))).getValue();
		stringBuffer.append(buyTypeName).append("|").append(orderInfoBO.getLotteryIssue()).append("期");
		stringBuffer.append("|").append(Constants.ORDER_GROUP_COMMISSON_AMOUNT);
		return stringBuffer.toString();
	}

	/**  
	* 方法说明: 检验退款金额
	* @auth: xiongJinGang
	* @param transRechargeBO
	* @param refundParam
	* @time: 2017年5月3日 下午12:25:11
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> checkRefundParam(TransRechargeBO transRechargeBO, RefundParamVO refundParam) {
		if (ObjectUtil.isBlank(transRechargeBO)) {
			return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
		}
		Double arrivalAmount = transRechargeBO.getArrivalAmount();// 到账金额
		Double refundAmount = refundParam.getRefundAmount();// 退款金额
		if (ObjectUtil.isBlank(refundAmount)) {
			return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(arrivalAmount)) {
			return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_IS_NULL_FIELD);
		}
		// 到账金额与申请退款金额对比
		int compareResult = MathUtil.compareTo(arrivalAmount, refundAmount);
		if (compareResult < 0) {
			return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 拆分订单和订单类型
	* @auth: xiongJinGang
	* @param orderInfo
	* @time: 2017年5月11日 下午3:22:46
	* @return: List<String[]> 
	*/
	public static BatchPayOrderVO getOrderInfo(String orderInfo) {
		String[] singleOrders = orderInfo.split(";");// D1705151804120100003,1;D1705151804000100002,1;
		List<PayOrderBaseInfoVO> list = new ArrayList<PayOrderBaseInfoVO>();
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		BatchPayOrderVO batchPayOrderVO = new BatchPayOrderVO();
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer buyTypeSb = new StringBuffer();
		for (String singleOrder : singleOrders) {
			payOrderBaseInfoVO = new PayOrderBaseInfoVO();
			String[] orderCodes = singleOrder.split(",");
			payOrderBaseInfoVO.setOrderCode(orderCodes[0]);
			if (orderCodes.length > 1) {
				buyTypeSb.append(orderCodes[1]).append(",");
				payOrderBaseInfoVO.setBuyType(Integer.parseInt(orderCodes[1]));
			}
			stringBuffer.append(orderCodes[0]).append(",");
			list.add(payOrderBaseInfoVO);
		}
		String orderCodes = stringBuffer.toString();
		if (!ObjectUtil.isBlank(orderCodes) && orderCodes.endsWith(",")) {
			orderCodes = orderCodes.substring(0, orderCodes.length() - 1);
		}
		batchPayOrderVO.setOrderCodes(orderCodes);
		if (!ObjectUtil.isBlank(buyTypeSb)) {
			String buyType = buyTypeSb.toString();
			if (!ObjectUtil.isBlank(buyType) && buyType.endsWith(",")) {
				buyType = buyType.substring(0, buyType.length() - 1);
			}
			batchPayOrderVO.setBuyTypes(buyType);
		}
		batchPayOrderVO.setList(list);
		return batchPayOrderVO;
	}

	/**  
	* 方法说明: 根据订单列表，分开订单号和购买类型
	* @auth: xiongJinGang
	* @param orderTotalList
	* @time: 2017年7月26日 上午11:50:03
	* @return: BatchPayOrderVO 
	*/
	public static BatchPayOrderVO getOrderInfo(List<PayOrderBaseInfoVO> orderTotalList) {
		BatchPayOrderVO batchPayOrderVO = new BatchPayOrderVO();
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer buyTypeSb = new StringBuffer();
		for (PayOrderBaseInfoVO pb : orderTotalList) {
			stringBuffer.append(pb.getOrderCode()).append(",");
			if (!pb.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				buyTypeSb.append("1").append(",");
			} else {
				buyTypeSb.append(pb.getBuyType()).append(",");
			}
		}
		if (!ObjectUtil.isBlank(stringBuffer)) {
			batchPayOrderVO.setOrderCodes(StringUtil.interceptEndSymbol(stringBuffer.toString(), ","));
			batchPayOrderVO.setBuyTypes(StringUtil.interceptEndSymbol(buyTypeSb.toString(), ","));
		}
		return batchPayOrderVO;
	}

	public static BatchPayOrderVO getOrderInfo(PayParamVO payParam) {
		String orderCode = payParam.getOrderCode();
		// String buyType = payParam.getBuyType();
		String[] singleOrders = orderCode.split(",");// D1705151804120100003,D1705151804000100002;
		// String[] singleBuyTypes = buyType.split(",");//
		List<PayOrderBaseInfoVO> list = new ArrayList<PayOrderBaseInfoVO>();
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		BatchPayOrderVO batchPayOrderVO = new BatchPayOrderVO();
		StringBuffer stringBuffer = new StringBuffer();
		StringBuffer buyTypeSb = new StringBuffer();
		for (String singleOrder : singleOrders) {
			payOrderBaseInfoVO = new PayOrderBaseInfoVO();
			String[] orderCodes = singleOrder.split(",");
			payOrderBaseInfoVO.setOrderCode(orderCodes[0]);
			if (orderCodes.length > 1) {
				buyTypeSb.append(orderCodes[1]).append(",");
				payOrderBaseInfoVO.setBuyType(Integer.parseInt(orderCodes[1]));
			}
			stringBuffer.append(orderCodes[0]).append(",");
			list.add(payOrderBaseInfoVO);
		}
		batchPayOrderVO.setOrderCodes(stringBuffer.toString());
		if (!ObjectUtil.isBlank(buyTypeSb)) {
			batchPayOrderVO.setBuyTypes(buyTypeSb.toString());
		}
		batchPayOrderVO.setList(list);
		return batchPayOrderVO;
	}

	/**  
	* 方法说明: 验证支付金额
	* @auth: xiongJinGang
	* @param payParam
	* @param userInfo
	* @param operateCouponBO
	* @time: 2017年3月31日 上午11:04:57
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validatePayAmount(PayParamVO payParam, UserInfoBO userInfo, OperateCouponBO operateCouponBO, UserWalletBO userWalletBO, PayOrderBaseInfoVO orderInfo) {
		if (!orderInfo.getBuyType().equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey())) {
			Double useBalance = ObjectUtil.isBlank(payParam.getBalance()) ? 0.0 : payParam.getBalance();// 页面传过来需要使用的余额
			Double usePayAmount = ObjectUtil.isBlank(payParam.getPayAmount()) ? 0.0 : payParam.getPayAmount();// 页面传过来需要支付的现金金额

			payParam.setUseBalance(useBalance);// 需要使用的余额金额
			Double orderAmount = orderInfo.getOrderAmount();// 订单金额
			if (ObjectUtil.isBlank(orderAmount)) {
				return ResultBO.err(MessageCodeConstants.ORDER_ACCOUNT_IS_NULL_FIELD);
			}
			Double redAmount = 0.0;// 彩金红包中的金额
			// 判断客户端是否使用了红包，以及红包中的金额是否大于等于客户端传进来的红包金额，如果彩金红包中的金额小于传进来的金额，返回红包中可用余额不足
			if (!ObjectUtil.isBlank(operateCouponBO)) {
				// 验证红包类型是否为彩金红包，如果是彩金红包，获取余额，判断余额是否大于0
				if (PayConstants.RedTypeEnum.RED_COLOR.getKey().equals(operateCouponBO.getRedType())) {
					redAmount = operateCouponBO.getRedBalance();// 红包余额
					payParam.setUseRedAmount(redAmount);// 使用红包金额
				} else if (PayConstants.RedTypeEnum.CONSUMPTION_DISCOUNT.getKey().equals(operateCouponBO.getRedType())) {
					// 满减红包
					Double redValue = operateCouponBO.getRedValue();// 具体减多少金额
					Integer minSpendAmount = operateCouponBO.getMinSpendAmount();// 最低使用条件
					// 判断订单是否符合满减红包条件
					if (MathUtil.compareTo(orderAmount, Double.valueOf(String.valueOf(minSpendAmount))) < 0) {
						return ResultBO.err(MessageCodeConstants.PAY_RED_NOT_SUIT_ERROR_SERVICE);
					}
					Double leavePayAmount = MathUtil.sub(orderAmount, redValue);
					// 使用了余额
					if (MathUtil.compareTo(useBalance, 0.0) > 0) {
						// 余额大于剩余需要支付的金额
						if (MathUtil.compareTo(useBalance, leavePayAmount) > 0) {
							// 余额已经足够支付，但是页面还传递了需要现金支付的金额，返回错误
							if (MathUtil.compareTo(usePayAmount, 0.0) > 0) {
								return ResultBO.err(MessageCodeConstants.PAY_PAY_AMOUNT_ERROR_SERVICE);
							}
							payParam.setUseBalance(leavePayAmount);// 订单金额减去红包金额，等于需要使用的余额
						} else {
							Double needPayAmount = MathUtil.sub(leavePayAmount, useBalance);
							// 剩余需要支付的金额与客户端传递的需要支付金额不匹配，返回金额错误
							if (MathUtil.compareTo(needPayAmount, usePayAmount) != 0) {
								return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
							}
						}
					} else {
						// 没有使用余额，判断需要支付的金额与实际传递过来的金额是否一致
						if (MathUtil.compareTo(usePayAmount, leavePayAmount) != 0) {
							return ResultBO.err(MessageCodeConstants.PAY_PAY_AMOUNT_ERROR_SERVICE);
						}
					}
					payParam.setUseRedAmount(redValue);
					return ResultBO.ok(payParam);
				}
			}

			/***************************我是分隔线，下面是使用了彩金红包或者没用彩金红包的操作***********************************/
			// 订单金额减去红包的金额 小于 账户余额+需要支付的现金金额，返回支付金额不对
			if (MathUtil.compareTo(MathUtil.sub(orderAmount, redAmount), MathUtil.add(useBalance, usePayAmount)) > 0) {
				return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
			}

			// 判断客户端是否使用了余额，以及余额中的金额是否大于等于客户端传进来的余额，如果钱包中的可用现金余额小于传进来的使用余额，返回钱包中可用余额不足
			if (!ObjectUtil.isBlank(userWalletBO)) {
				// 账户中的总现金余额低于客户端传过来的金额，返回余额不足，不能支付
				if (MathUtil.compareTo(userWalletBO.getTotalCashBalance(), useBalance) < 0) {
					return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
				}
			}

			// 彩金红包中余额大于订单金额，就不需要用余额和支付金额了
			if (MathUtil.compareTo(redAmount, orderAmount) >= 0) {
				payParam.setUseRedAmount(orderAmount);// 需要使用的红包金额就是订单的金额
				// 判断调页面传的第三方的金额是否大于0，红包金额足够支付了，不需要再调第三方支付了
				if (MathUtil.compareTo(usePayAmount, 0.0) > 0) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_COLOR_ENOUTH_ERROR_SERVICE);
				}
				// 判断是否使用了账户余额，如果也使用了余额，返回错误
				if (MathUtil.compareTo(useBalance, 0.0) > 0) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_COLOR_ENOUTH_ERROR_SERVICE);
				}
				payParam.setUseBalance(useBalance);// 实际使用账户余额的金额
			} else {
				// 红包不够支付
				Double redAddBalanceAmount = MathUtil.add(redAmount, useBalance);
				// 红包+余额的金额大于订单金额，判断（支付金额、支付银行等是否为空）
				if (MathUtil.compareTo(redAddBalanceAmount, orderAmount) > 0) {
					// 账户余额大于订单金额，但页面传递需要第三方支付金额仍然大于0，返回支付金额错误
					if (MathUtil.compareTo(usePayAmount, 0.0) > 0) {
						return ResultBO.err(MessageCodeConstants.PAY_PAY_AMOUNT_ERROR_SERVICE);
					}
					payParam.setUseBalance(MathUtil.sub(orderAmount, redAmount));// 订单金额减去红包金额，等于需要使用的余额
				} else {
					// 红包+余额的金额小于订单金额
					if (ObjectUtil.isBlank(useBalance) && ObjectUtil.isBlank(redAmount) && MathUtil.compareTo(usePayAmount, 0.0) <= 0) {
						return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
					}
					// 验证需要支付的总金额与订单金额是否匹配，不匹配直接返回错误
					Double totalNeedAmount = MathUtil.add(redAddBalanceAmount, usePayAmount);
					if (MathUtil.compareTo(totalNeedAmount, orderInfo.getOrderAmount()) != 0) {
						logger.error("订单【" + orderInfo.getOrderCode() + "】支付金额" + orderInfo.getOrderAmount() + "错误，客户端使用余额金额：" + useBalance + "，需要支付金额：" + usePayAmount);
						return ResultBO.err(MessageCodeConstants.PAY_AMOUNT_ERROR_SERVICE);
					}
				}
			}
		} else {
			if (ObjectUtil.isBlank(payParam.getBuyAmount())) {
				return ResultBO.err(MessageCodeConstants.PAY_PAY_AMOUNT_ERROR_SERVICE);
			}
			Double useBalance = ObjectUtil.isBlank(payParam.getBalance()) ? 0.0 : payParam.getBalance();// 页面传过来需要使用的余额
			// 账户中的总现金余额低于客户端传过来的使用余额金额，返回余额不足，不能支付
			if (MathUtil.compareTo(userWalletBO.getTotalCashBalance(), useBalance) < 0) {
				return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
			}
			payParam.setUseBalance(useBalance);
		}
		return ResultBO.ok(payParam);
	}

	/**  
	* 方法说明: 拼装支付同步回调地址
	* @auth: xiongJinGang
	* @param returnUrl
	* @param transCode
	* @time: 2017年6月12日 上午10:16:59
	* @return: String 
	*/
	public static String getReturnUrl(String returnUrl, String transCode) {
		if (!ObjectUtil.isBlank(returnUrl)) {
			if (returnUrl.contains("?")) {
				returnUrl += "&tc=" + transCode;
			} else {
				returnUrl += "?tc=" + transCode;
			}
		}
		return returnUrl;
	}

	/**  
	* 方法说明: 计算账户总现金金额 是否等于 winning_balance + top_80_balance + top_20_balance 总和
	* @auth: xiongJinGang
	* @param userWalletBO
	* @time: 2017年7月3日 上午11:15:20
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> countTotalAmount(UserWalletBO userWalletBO) {
		if (ObjectUtil.isBlank(userWalletBO)) {
			logger.info("用户钱包为空");
			return ResultBO.err(MessageCodeConstants.USER_WALLET_AMOUNT_ERROR_SERVICE);
		}
		try {
			Double totalCashAmount = userWalletBO.getTotalCashBalance();// 总现金金额（total_cash_balance）=winning_balance + top_80_balance + top_20_balance
			Double totalAmount = MathUtil.add(userWalletBO.getTop20Balance(), userWalletBO.getTop80Balance(), userWalletBO.getWinningBalance());
			// 总现金金额不等于20%+80%+中奖金额的总金额，返回错误
			if (MathUtil.compareTo(totalCashAmount, totalAmount) != 0) {
				logger.info("用户【" + userWalletBO.getUserId() + "】账户总金额匹配不上，账户金额【" + userWalletBO.toString() + "】");
				return ResultBO.err(MessageCodeConstants.USER_WALLET_AMOUNT_ERROR_SERVICE);
			}
		} catch (Exception e) {
			logger.error("订单账户金额总和异常【" + userWalletBO.toString() + "】", e);
			return ResultBO.err(MessageCodeConstants.USER_WALLET_AMOUNT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证cms人工充值参数
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @time: 2017年7月6日 下午4:21:14
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateCmsRecharge(CmsRechargeVO cmsRecharge) {
		if (ObjectUtil.isBlank(cmsRecharge.getUserId())) {
			return ResultBO.err(MessageCodeConstants.USER_INFO_ERROR_SYS);
		}
		if (ObjectUtil.isBlank(cmsRecharge.getRechargeAmount())) {
			return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_BALANCE_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(cmsRecharge.getRechargeType())) {
			return ResultBO.err(MessageCodeConstants.PAY_TRADE_TYPE_ERROR_SERVICE);
		}
		if (!CmsRechargeTypeEnum.CASH.getKey().equals(cmsRecharge.getRechargeType()) && !CmsRechargeTypeEnum.RED.getKey().equals(cmsRecharge.getRechargeType())) {
			return ResultBO.err(MessageCodeConstants.PAY_TRADE_TYPE_ERROR_SERVICE);
		}
		if (ObjectUtil.isBlank(cmsRecharge.getOperator())) {
			return ResultBO.err(MessageCodeConstants.OPERATE_IS_NULL_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**
	 * 验证代理系统充值参数
	 * @param agentPayVO
	 * @return
	 * @author YiJian
	 * @date 2017年7月21日 下午3:51:24
	 */
	public static ResultBO<?> validateAgentRecharge(AgentPayVO agentPayVO) {
		if (ObjectUtil.isBlank(agentPayVO.getUserId())) {
			return ResultBO.err(MessageCodeConstants.USER_ID_IS_NULL_HTTP_ERROR_CODE);
		}
		if (ObjectUtil.isBlank(agentPayVO.getAmount())) {
			return ResultBO.err(MessageCodeConstants.RECHARGE_AMOUNT_IS_NULL_HTTP_ERROR_CODE);
		}
		// if (ObjectUtil.isBlank(agentPayVO.getAgentCode())) {
		// return ResultBO.err(MessageCodeConstants.AGENT_CODE_IS_NULL_HTTP_ERROR_CODE);
		// }
		if (ObjectUtil.isBlank(agentPayVO.getAgentTradeNo())) {
			return ResultBO.err(MessageCodeConstants.AGENT_TRADE_NO_IS_NULL_HTTP_ERROR_CODE);
		}

		return ResultBO.ok();
	}

	/**  
	* 方法说明: 充值金额与到账金额对比
	* @auth: xiongJinGang
	* @param arrivaleAmount
	* @param rechargeAmount
	* @time: 2017年8月2日 上午10:34:28
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateRechargeAmount(Double arrivaleAmount, Double rechargeAmount) {
		if (MathUtil.compareTo(arrivaleAmount, rechargeAmount) != 0) {
			return ResultBO.err(MessageCodeConstants.PAY_AMOUNT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 拼装当日限额的key
	* @auth: xiongJinGang
	* @param channelId
	* @param payType
	* @param cardType
	* @time: 2017年12月14日 下午4:16:51
	* @return: String 
	*/
	public static String getDayLimitKey(String keyHead, Integer channelId, Short payType, Short cardType) {
		String today = DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE);// 当天日期
		return new StringBuffer(keyHead).append(today).append("_").append(channelId).append("_").append(payType).append("_").append(cardType).toString();
	}

	/**  
	* 方法说明: 本地支付加锁
	* @auth: xiongJinGang
	* @param keyHead
	* @param orderNo
	* @param userId
	* @time: 2018年7月7日 下午4:24:21
	* @return: String 
	*/
	public static String getPayOrderLockKey(String keyHead, String orderNo, Integer userId) {
		return new StringBuffer(keyHead).append(orderNo).append("_").append(userId).toString();
	}

	/**  
	* 方法说明: 验证订单是不是合买订单，如果是，则返回订单号（合买订单目前只支持单个支付）
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2018年5月3日 下午2:52:44
	* @return: String 
	*/
	public static String checkOrderType(String orderCode) {
		// 订单编号不为空并且是合买
		if (!StringUtil.isBlank(orderCode)) {
			String[] singleOrders = orderCode.split(";");
			if (singleOrders.length == 1) {
				for (String singleOrder : singleOrders) {
					String[] orderCodes = singleOrder.split(",");
					// 判断是否为合买订单
					if (orderCodes[1].equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey().toString())) {
						return orderCodes[0];
					}
				}
			}
		}
		return null;
	}
	
	/**  
	* 方法说明: 渠道类型转换
	* @auth: xiongJinGang
	* @param channelType
	* @param paymentInfo
	* @time: 2018年8月7日 下午3:00:41
	* @return: void 
	*/
	public static void platformChange(String channelType, PaymentInfoBO paymentInfo) {
		// 如果h5、ios、和android中的任意一平台
		if (paymentInfo.getPayPlatform().equals(TakenPlatformEnum.WAP.getKey()) || paymentInfo.getPayPlatform().equals(TakenPlatformEnum.ANDROID.getKey()) || paymentInfo.getPayPlatform().equals(TakenPlatformEnum.IOS.getKey())) {
			if (channelType.endsWith("WEB")) {
				paymentInfo.setPayPlatform(TakenPlatformEnum.WEB.getKey());
			}
		}
	}
}
