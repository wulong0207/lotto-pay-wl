package com.hhly.paycore.remote.service.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.common.PayCommon;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.paychannel.UnifiedPayService;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.remote.service.IPayService;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.OrderGroupService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.PayChannelLimitService;
import com.hhly.paycore.service.PayChannelMgrService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderFlowInfoEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.ChannelTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayResultEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.constants.RechargeConstants;
import com.hhly.skeleton.base.mq.msg.MessageModel;
import com.hhly.skeleton.base.mq.msg.OperateNodeMsg;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.vo.ChannelParamVO;
import com.hhly.skeleton.pay.channel.vo.PayTypeResultVO;
import com.hhly.skeleton.pay.vo.BatchPayOrderVO;
import com.hhly.skeleton.pay.vo.PayNotifyMockVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.PayResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.CodeUtil;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】 统一支付实现类
 * @author xiongjingang
 * @date 2017年3月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iPayService")
public class PayServiceImpl extends PayCommon implements IPayService {

	private static final Logger logger = Logger.getLogger(PayServiceImpl.class);
	private static final String PAY_ORDER_LOCK_KEY = "pay_order_lock_key";// 本地支付锁
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private PayBankService payBankService;
	@Resource
	private PayChannelService payChannelService;
	@Resource
	private PayChannelMgrService payChannelMgrService;// 支付渠道管理
	@Resource
	private PayChannelLimitService payChannelLimitService;// 渠道限额
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private RedissonClient redissonClient;// redis锁
	@Resource
	private UserUtil userUtil;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private PayCoreService payCoreService;
	@Resource
	private OrderGroupService buyTogetherService;// 合买

	@Value("${pay.remaining.valid.time}")
	private String validTime;// 支付有效时间
	@Value("${pay.return.url}")
	private String returnUrl;// 支付同步返回URL
	@Value("${pay.notify.url}")
	private String notifyUrl;// 支付异步返回URL
	@Value("${refund.notify.url}")
	private String refundNotifyUrl;// 退款异步返回URL
	@Value("${nowpay.return.url}")
	private String nowPayReturnUrl;// 现在支付同步返回地址
	@Value("${lianpay.return.url}")
	private String lianPayReturnUrl;// 连连支付同步返回地址

	@Override
	public ResultBO<?> findChannelByPlatform(String platform) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultBO<?> pay(PayParamVO payParam) {
		logger.info("支付请求参数：" + payParam.toString());
		// 1、 验证token是否为空，是否有用户信息
		UserInfoBO userInfo = userUtil.getUserByToken(payParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		payParam.setUserId(userInfo.getId());// 设置用户Id
		// 2、验证基本参数有效性
		ResultBO<?> resultBO = PayUtil.validatePayParams(payParam);
		if (resultBO.isError()) {
			logger.info("订单【" + payParam.getOrderCode() + "】支付请求参数验证不过");
			return resultBO;
		}
		// 获取验证后的订单信息
		List<OrderBaseInfoBO> orderList = payParam.getOrderList();
		if (ObjectUtil.isBlank(orderList)) {
			logger.info("获取订单【" + payParam.getOrderCode() + "】详情为空");
			return ResultBO.err(MessageCodeConstants.ORDER_NOT_EXIST_OR_INVALILD);
		}

		// 验证订单的订单、支付、开奖状态是否可以支付
		resultBO = PayUtil.validateOrderStatus(orderList, payParam);
		if (resultBO.isError()) {
			return resultBO;
		}
		// 单个订单对象
		ToPayEndTimeVO toPayEndTimeVO = (ToPayEndTimeVO) resultBO.getData();
		PayOrderBaseInfoVO orderInfoBO = new PayOrderBaseInfoVO(toPayEndTimeVO.getOrderBaseInfo());
		orderInfoBO.setUserId(userInfo.getId());
		orderInfoBO.setOrderAmount(toPayEndTimeVO.getOrderAmount());// 订单总金额
		payParam.setEndSaleTime(orderInfoBO.getEndSysTime());// 支付截止时间

		PayBankcardBO payBankcardBO = null;// 银行卡信息
		PayBankBO payBankBO = null;// 银行信息

		// 银行ID不为空，验证银行信息
		if (!ObjectUtil.isBlank(payParam.getBankId())) {
			resultBO = payBankService.findBankByIdAndValidate(payParam.getBankId());
			if (resultBO.isError()) {
				logger.info("获取银行【" + payParam.getBankId() + "】信息返回：" + resultBO.getMessage());
				return resultBO;
			}
			payBankBO = (PayBankBO) resultBO.getData();
			payParam.setPayBankBO(payBankBO);
		}

		// 账户余额，获取账户钱包金额信息，计算账户总金额与3个子账户金额之和是否相等，不等返回错误
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userInfo.getId());
		resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 3、银行卡不为空，验证银行卡有效性并得到银行卡信息
		if (!ObjectUtil.isBlank(payParam.getBankCardId())) {
			resultBO = bankcardService.findUserBankCardByCardId(userInfo.getId(), payParam.getBankCardId());
			if (resultBO.isError()) {
				logger.info("获取用户【" + userInfo.getId() + "】银行卡【" + payParam.getBankCardId() + "】错误：" + resultBO.getErrorCode());
				return resultBO;
			}
			payBankcardBO = (PayBankcardBO) resultBO.getData();
			// 是否切换不为空，并且为切换时
			if (!ObjectUtil.isBlank(payParam.getChange()) && payParam.getChange().equals(PayConstants.ChangeEnum.YES.getKey())) {
				// 已开通快捷支付，切换成网银
				if (payBankcardBO.getOpenbank().equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey())) {
					payBankcardBO.setOpenbank(PayConstants.BandCardQuickEnum.NOT_OPEN.getKey());
				} else {
					// 未开通快捷支付，切换成快捷
					payBankcardBO.setOpenbank(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey());
				}
			}

			payParam.setPayBankcardBO(payBankcardBO);
			// 银行卡附加信息
			String bankSegmentCode = bankcardSegmentService.findBankSegmentCodeByCard(payBankcardBO.getCardcode());
			if (ObjectUtil.isBlank(bankSegmentCode)) {
				logger.info("获取银行卡附加信息【" + payBankcardBO.getCardcode() + "】为空");
				return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_SEGMENT_NOT_FOUND_ERROR_SERVICE);
			}
			payParam.setBankCode(bankSegmentCode);
		}

		// 4、验证是否使用红包
		OperateCouponBO operateCouponBO = null;
		boolean isActivityOrder = PayUtil.isActivityOrder(orderList);
		if (isActivityOrder) {
			// 是活动订单，需要折扣消费，要预先生成一个红包
			operateCouponBO = new OperateCouponBO();
			operateCouponBO.setRedValue(payParam.getUseRedAmount());// 红包金额
			operateCouponBO.setRedBalance(payParam.getUseRedAmount());// 可用红包余额
			operateCouponBO.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());
		} else {
			if (!ObjectUtil.isBlank(payParam.getRedCode())) {
				resultBO = operateCouponService.findCouponByRedCode(payParam.getRedCode());
				if (resultBO.isError()) {
					logger.info("获取用户【" + userInfo.getId() + "】红包【" + payParam.getRedCode() + "】信息错误：" + resultBO.getMessage());
					return resultBO;
				}
				operateCouponBO = (OperateCouponBO) resultBO.getData();
				// 如果钱包为空，查询钱包信息
				if (ObjectUtil.isBlank(userWalletBO)) {
					userWalletBO = userWalletService.findUserWalletByUserId(userInfo.getId());
				}
				// 验证红包的是否满足使用条件
				resultBO = PayUtil.validateRed(operateCouponBO, payParam, orderInfoBO, userWalletBO);
				if (resultBO.isError()) {
					logger.info("用户【" + userInfo.getId() + "】红包【" + payParam.getRedCode() + "】不满足使用条件：" + resultBO.getMessage());
					return resultBO;
				}
			}
		}

		// 5、 验证账户钱包、彩金红包金额是否够支付
		resultBO = PayUtil.validatePayAmount(payParam, userInfo, operateCouponBO, userWalletBO, orderInfoBO);
		if (resultBO.isError()) {
			logger.info("用户【" + userInfo.getId() + "】账户金额不够支付：" + resultBO.getMessage());
			return resultBO;
		}

		// 判断是否需要调用第三方支付（判断需要支付的金额为0 并且支付银行ID为空）
		if (MathUtil.compareTo(payParam.getPayAmount(), 0.0) == 0) {
			// 不需要调用第三方支付，修改余额，修改彩金红包金额或者记录
			resultBO = localPay(payParam, toPayEndTimeVO);
		} else {
			// 银行ID为空
			if (ObjectUtil.isBlank(payParam.getBankId())) {
				logger.info("用户【" + userInfo.getId() + "】支付金额不为空，但银行ID为空");
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_TYPE_IS_NULL_FIELD);
			}
			/**** 判断是否有可用的支付渠道及支付金额是否超过所有渠道的限额*****/
			// 选择支付渠道的时候会检验
			/*resultBO = payChannelMgrService.validateChannel(payParam.getPayAmount(), PayConstants.RechargeTypeEnum.PAY.getKey());
			if (resultBO.isError()) {
				return resultBO;
			}*/
			// 需要调用第三方支付并添加充值记录
			resultBO = callPay(payParam, userInfo, orderInfoBO);
		}
		return resultBO;
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		TransRechargeBO transRechargeBO = payCoreService.findRechargeRecord(refundParam);
		ResultBO<?> resultBO = PayUtil.checkRefundParam(transRechargeBO, refundParam);
		if (resultBO.isError()) {
			return resultBO;
		}
		ChannelTypeEnum channelType = PayUtil.getChannelType(transRechargeBO.getChannelCode());
		refundParam.setNotifyUrl(PayUtil.getPayMethod(channelType, refundNotifyUrl));// 异步通知地址
		refundParam.setOrderAmount(transRechargeBO.getArrivalAmount());

		// 支付银行类型
		String payType = getPayType(transRechargeBO);
		refundParam.setRechargeChannel(payType);

		UnifiedPayService payService = getServiceImpl(transRechargeBO.getChannelCode());
		if (ObjectUtil.isBlank(refundParam.getOrderCode()) && !ObjectUtil.isBlank(refundParam.getTransCode())) {
			refundParam.setOrderCode(refundParam.getTransCode());
		}
		refundParam.setRefundCode(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
		refundParam.setTransRechargeBO(transRechargeBO);// 有些支付渠道的退款接口，参数要得比较多
		resultBO = payService.refund(refundParam);
		if (resultBO.isError()) {
			return resultBO;
		}
		logger.info("v");
		RefundResultVO refundResultVO = (RefundResultVO) resultBO.getData();
		// 接口请求成功，并且是退款处理中或者是退款成功，修改充值记录状态
		if (refundResultVO.getResultCode().equals(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey())
				&& (refundResultVO.getResultCode().equals(PayConstants.RefundStatusEnum.REFUND_PROCESSING) || refundResultVO.getResultCode().equals(PayConstants.RefundStatusEnum.REFUND_SUCCESS))) {
			// 这里需要修改某些记录的退款状态，暂时还不知道如何操作，先留着
		}
		return resultBO;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(payQueryParamVO.getTransCode());
		if (ObjectUtil.isBlank(transRechargeBO)) {
			return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
		}
		// 判断充值状态
		if (!PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey().equals(transRechargeBO.getTransStatus())) {
			logger.info("充值流水【" + transRechargeBO.getTransRechargeCode() + "】当前交易状态【" + transRechargeBO.getTransStatus() + "】不是进行中，不调用支付接口查询");
			return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE);
		}

		// 根据充值记录中的银行，查找银行编号，目前只需要知道支付宝和微信
		String payType = getPayType(transRechargeBO);
		payQueryParamVO.setRechargeChannel(payType);

		payQueryParamVO.setOrderTime(transRechargeBO.getCreateTime());// 充值记录创建时间
		UnifiedPayService payService = getServiceImpl(transRechargeBO.getChannelCode());
		// 把充值记录做为参数传递

		payQueryParamVO.setTransRechargeBO(transRechargeBO);
		ResultBO<?> resultBO = payService.payQuery(payQueryParamVO);
		if (ObjectUtil.isBlank(resultBO)) {
			logger.info("调用第三方查询接口查询充值流水【" + transRechargeBO.getTransRechargeCode() + "】返回空");
			return ResultBO.err(MessageCodeConstants.DATA_NOT_FOUND_SYS);
		}
		if (resultBO.isError()) {
			return resultBO;
		}
		PayQueryResultVO payQueryResultVO = (PayQueryResultVO) resultBO.getData();
		logger.info("查询充值编号【" + payQueryParamVO.getTransCode() + "】支付结果返回：" + payQueryResultVO.toString() + "，交易状态：" + payQueryResultVO.getTradeStatus());
		if (!ObjectUtil.isBlank(payQueryResultVO)) {
			// 充值交易状态不为进行中，直接返回
			if (!PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey().equals(transRechargeBO.getTransStatus())) {
				logger.info("数据库中，充值编号【" + payQueryParamVO.getTransCode() + "】交易状态为：" + transRechargeBO.getTransStatus() + "，不再执行下面的逻辑");
				return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE);
			}
		} else {
			logger.info("【支付】获取充值流水【" + payQueryParamVO.getTransCode() + "】的充值记录失败，无法更新充值记录和添加交易记录");
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		Double orderPrice = StringUtils.isBlank(payQueryResultVO.getTotalAmount()) ? 0d : Double.valueOf(payQueryResultVO.getTotalAmount());
		payNotifyResult.setOrderAmt(orderPrice);
		payNotifyResult.setThirdTradeNo(payQueryResultVO.getTradeNo());
		payNotifyResult.setTradeTime(payQueryResultVO.getArriveTime());
		payNotifyResult.setOrderCode(payQueryResultVO.getOrderCode());
		payNotifyResult.setStatus(payQueryResultVO.getTradeStatus());
		transRechargeBO.setArrivalAmount(payNotifyResult.getOrderAmt());
		try {
			// 充值的，订单号为空
			if (ObjectUtil.isBlank(transRechargeBO.getOrderCode())) {
				if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
					logger.info("查询充值编号【" + payQueryParamVO.getTransCode() + "】支付结果返回成功，处理成功逻辑");
					// 充值成功，更新充值、钱包、彩金红包、交易流水等记录
					ResultBO<?> resultBO1 = payCoreService.modifyRechargeSuccessTransRecord(transRechargeBO, payNotifyResult);

					if (resultBO1.isOK()) {
						PayResultVO payResultVO = (PayResultVO) resultBO1.getData();

						// 发送充值消息
						MessageModel messageModel = new MessageModel();
						messageModel.setKey(Constants.MSG_NODE_RESEND);
						messageModel.setMessageSource("lotto-pay");
						OperateNodeMsg operateNodeMsg = new OperateNodeMsg();
						operateNodeMsg.setNodeId(4);
						operateNodeMsg.setNodeData(transRechargeBO.getUserId() + ";" + transRechargeBO.getTransRechargeCode());// 用户ID;充值交易号
						messageModel.setMessage(operateNodeMsg);
						messageProvider.sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, messageModel);
						// 使用了红包，才发送红包消息
						if (!ObjectUtil.isBlank(payResultVO.getRedCode())) {
							// 发送得到彩金红包消息
							OperateNodeMsg colorRedMsg = new OperateNodeMsg();
							colorRedMsg.setNodeId(2);
							colorRedMsg.setNodeData(transRechargeBO.getUserId() + ";" + payResultVO.getRedCode());// 用户ID;红包编号
							messageModel.setMessage(colorRedMsg);
							messageProvider.sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, messageModel);
						}
					}
				} else {
					// 实际返回充值失败，才更新
					if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_FAILURE)) {
						// 充值失败，更新充值、交易流水等记录
						logger.info("充值编号【" + transRechargeBO.getTransRechargeCode() + "】交易失败，更新充值状态");
						payCoreService.modifyFailTransRecord(transRechargeBO, payNotifyResult);
					}
				}
			} else {// 支付的才有订单号
				logger.info("处理查询订单【" + transRechargeBO.getOrderCode() + "】支付结果开始");
				batchNotify(transRechargeBO, payNotifyResult);
			}
		} catch (Exception e) {
			logger.error("查询交易号【" + payQueryParamVO.getTransCode() + "】结果异常", e);
		}
		return resultBO;
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(payQueryParamVO.getTransCode());
		if (ObjectUtil.isBlank(transRechargeBO)) {
			return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
		}
		UnifiedPayService payService = getServiceImpl(transRechargeBO.getChannelCode());
		payQueryParamVO.setTransRechargeBO(transRechargeBO);

		String rechargeChannel = RechargeConstants.getThirdTypeByPayType(transRechargeBO.getPayType());
		payQueryParamVO.setRechargeChannel(rechargeChannel);
		return payService.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> params) {
		logger.info("支付同步返回参数：" + params.toString());
		String rechargeCode = "";// 主要存的是充值流水号
		if (params.containsKey("attach")) {
			// 支付宝、微信等通用
			rechargeCode = params.get("attach");
		} else {
			// 连连支付
			rechargeCode = params.get("info_order");
		}
		TransRechargeBO transRecharge = transRechargeService.findRechargeByTransCode(rechargeCode);
		return ResultBO.ok(transRecharge);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> params) {
		logger.info("支付异步通知参数：" + params.toString());
		try {
			String channelTypeName = params.get(Constants.PAY_CHANNEL_TYPE_NAME);
			if (ObjectUtil.isBlank(channelTypeName)) {
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_NULL_FIELD);
			}

			// 调用具体的支付，获取支付返回的具体信息
			UnifiedPayService payService = realPayServices.get(channelTypeName);
			params.remove(Constants.PAY_CHANNEL_TYPE_NAME);// 去掉这个参数，签名验证才能通过
			ResultBO<?> resultBO = payService.payNotify(params);
			if (resultBO.isError()) {
				return resultBO;
			}
			PayNotifyResultVO payNotifyResult = (PayNotifyResultVO) resultBO.getData();
			logger.info("支付回调参数验证后结果：" + payNotifyResult.toString());

			// 验证充值状态是否已更新
			ResultBO<?> rechargeResult = payCoreService.checkTransRechargeStatus(payNotifyResult.getOrderCode());
			if (rechargeResult.isError()) {
				return rechargeResult;
			}
			TransRechargeBO transRecharge = (TransRechargeBO) rechargeResult.getData();

			// 验证充值金额与到账户金额是否一致，不一致返回错误
			ResultBO<?> resultBO1 = PayUtil.validateRechargeAmount(payNotifyResult.getOrderAmt(), transRecharge.getRechargeAmount());
			if (resultBO1.isError()) {
				logger.error("充值金额【" + transRecharge.getRechargeAmount() + "】与到账金额【" + payNotifyResult.getOrderAmt() + "】不匹配");
				return resultBO1;
			}
			// 判断充值记录中的订单是不是合买订单，如果是则返回订单号；不是合买支付，返回空
			String orderCode = PayUtil.checkOrderType(transRecharge.getOrderCode());
			if (ObjectUtil.isBlank(orderCode)) {
				// 非合买订单批量处理
				batchNotify(transRecharge, payNotifyResult);
			} else {
				// 合买订单，需要做合买的流程
				buyTogetherService.updateBuyTogetherOrder(orderCode, transRecharge, payNotifyResult);
			}
			return resultBO;
		} catch (Exception e) {
			logger.error("【支付】处理支付异步通知异常，通知参数：" + params.toString() + "。", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public ResultBO<?> payResult(PayParamVO payParam) {
		// 1、 验证token是否为空，是否有用户信息
		UserInfoBO userInfo = userUtil.getUserByToken(payParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			logger.info("获取支付结果失败，登录token失效");
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}

		String key = CacheConstants.P_CORE_PAY_STATUS_RESULT + userInfo.getId() + "_" + payParam.getTransCode();
		String payResult = redisUtil.getString(key);
		// 余额支付成功的
		if (!ObjectUtil.isBlank(payResult) && payResult.equals(PayResultEnum.BALANCE_SUCCESS.getKey())) {
			logger.debug("获取支付结果key【" + key + "】，支付结果【" + payResult + "】");
			PayResultVO payResultVO = redisUtil.getObj(CacheConstants.P_CORE_PAY_STATUS_OBJ_RESULT + userInfo.getId() + "_" + payParam.getTransCode(), new PayResultVO());
			return ResultBO.ok(payResultVO);
		} else {
			try {
				ResultBO<?> resultBO = transRechargeService.findRechargeByCode(payParam.getToken(), payParam.getTransCode());
				if (resultBO.isOK()) {
					TransRechargeBO transRechargeBO = (TransRechargeBO) resultBO.getData();
					PayResultVO payResultVO = new PayResultVO(transRechargeBO);
					// 有使用红包
					if (!ObjectUtil.isBlank(transRechargeBO.getRedCode())) {
						resultBO = operateCouponService.findCouponByRedCode(transRechargeBO.getRedCode());
						if (resultBO.isOK()) {
							OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();
							payResultVO.setRedName(operateCouponBO.getRedName());// 设置红包名称
						}
					}

					if (!ObjectUtil.isBlank(transRechargeBO.getOrderCode())) {
						BatchPayOrderVO batchPayOrderVO = PayUtil.getOrderInfo(transRechargeBO.getOrderCode());// 订单编号和订单购买类型数组
						resultBO = payOrderUpdateService.findOrderAndValidate(batchPayOrderVO.getList());
						if (resultBO.isOK()) {
							List<PayOrderBaseInfoVO> orderTotalList = (List<PayOrderBaseInfoVO>) resultBO.getData();
							Map<String, Object> map = PayUtil.countOrderTotalAmount(orderTotalList);
							Double totalAmount = (Double) map.get("totalAmount");
							Integer lotteryCode = (Integer) map.get("lotteryCode");
							Double payAmount = MathUtil.sub(totalAmount, transRechargeBO.getRedAmount());
							if (batchPayOrderVO.getBuyTypes().equals(String.valueOf(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey()))) {
								payAmount = transRechargeBO.getRechargeAmount();// 合买实际支付金额等于充值金额
							}
							payResultVO.setOrderAmount(totalAmount);
							payResultVO.setPayAmount(payAmount);
							payResultVO.setLotteryCode(String.valueOf(lotteryCode));
							payResultVO.setRechargePlatform(transRechargeBO.getRechargePlatform());// 充值平台
							payResultVO.setActivityCode(transRechargeBO.getActivityCode());// 充值活动编号
						}
						payResultVO.setOrderCode(batchPayOrderVO.getOrderCodes());
						payResultVO.setBuyType(batchPayOrderVO.getBuyTypes());
					}
					logger.info("获取交易号【" + payParam.getTransCode() + "】支付结果状态为【" + PayConstants.PayStatusEnum.getEnum(payResultVO.getPayStatus()).getValue() + "】");
					return ResultBO.ok(payResultVO);
				}
				logger.info("未获取到交易号【" + payParam.getTransCode() + "】支付结果");
				return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
			} catch (Exception e) {
				logger.error("获取交易号【" + payParam.getTransCode() + "】支付结果系统异常", e);
				return ResultBO.err(MessageCodeConstants.ORDER_IS_BEING_PAID);
			}
		}
	}

	@Override
	public ResultBO<?> payNotifyMock(PayNotifyMockVO payNotifyMockVO) {
		logger.info("支付异步通知参数：" + payNotifyMockVO.toString());
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			// 验证充值状态是否已更新
			ResultBO<?> resultBO = payCoreService.checkTransRechargeStatus(payNotifyMockVO.getTransCode());
			if (resultBO.isError()) {
				return resultBO;
			}
			TransRechargeBO transRecharge = (TransRechargeBO) resultBO.getData();

			payNotifyResult.setPayId(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
			payNotifyResult.setOrderAmt(Double.parseDouble(payNotifyMockVO.getTransAmount()));
			payNotifyResult.setThirdTradeNo(ObjectUtil.isBlank(payNotifyMockVO.getThirdTradeNo()) ? DateUtil.getNow(DateUtil.DATE_FORMAT_NUM) : payNotifyMockVO.getThirdTradeNo());
			payNotifyResult.setTradeTime(ObjectUtil.isBlank(payNotifyMockVO.getThirdTradeNo()) ? DateUtil.getNow() : payNotifyMockVO.getTradeTime());
			PayStatusEnum payStatusEnum = null;
			if (StringUtil.isBlank(payNotifyMockVO.getPayStatus())) {
				payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			} else {
				if (payNotifyMockVO.getPayStatus().equals("1")) {
					payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
				} else {
					payStatusEnum = PayStatusEnum.PAYMENT_FAILURE;
				}
			}
			payNotifyResult.setStatus(payStatusEnum);// 支付状态
			payNotifyResult.setOrderCode(payNotifyMockVO.getTransCode());

			// 验证充值金额与到账户金额是否一致，不一致返回错误
			resultBO = PayUtil.validateRechargeAmount(payNotifyResult.getOrderAmt(), transRecharge.getRechargeAmount());
			if (resultBO.isError()) {
				logger.error("充值金额【" + transRecharge.getRechargeAmount() + "】与到账金额【" + payNotifyResult.getOrderAmt() + "】不匹配");
				return resultBO;
			}

			if (!ObjectUtil.isBlank(transRecharge)) {
				// 充值交易状态为交易成功，直接返回
				if (PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey().equals(transRecharge.getTransStatus())) {
					return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE);
				}
			} else {
				logger.info("【支付】获取充值流水【" + payNotifyResult.getOrderCode() + "】的充值记录失败，无法更新充值记录和添加交易记录");
				return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
			}
			logger.info("交易号【" + payNotifyMockVO.getTransCode() + "】模拟支付回调开始");
			// 判断充值记录中的订单是不是合买订单，如果是则返回订单号；不是合买支付，返回空
			String orderCode = PayUtil.checkOrderType(transRecharge.getOrderCode());
			if (ObjectUtil.isBlank(orderCode)) {
				// 非合买订单批量处理
				batchNotify(transRecharge, payNotifyResult);
			} else {
				// 合买订单，需要做合买的流程
				buyTogetherService.updateBuyTogetherOrder(orderCode, transRecharge, payNotifyResult);
			}
			return resultBO;
		} catch (Exception e) {
			logger.error("【支付】处理支付异步通知异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}

	}

	@Override
	public ResultBO<?> refundNotify(Map<String, String> params) {
		logger.info("退款异步通知参数：" + params.toString());
		try {
			String channelTypeName = params.get(Constants.PAY_CHANNEL_TYPE_NAME);
			if (ObjectUtil.isBlank(channelTypeName)) {
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_NULL_FIELD);
			}

			// 调用具体的支付，获取支付返回的具体信息
			UnifiedPayService payService = realPayServices.get(channelTypeName);
			params.remove(Constants.PAY_CHANNEL_TYPE_NAME);// 去掉这个参数，签名验证才能通过
			ResultBO<?> resultBO = payService.payNotify(params);
			if (resultBO.isError()) {
				return resultBO;
			}
			PayNotifyResultVO payNotifyResult = (PayNotifyResultVO) resultBO.getData();
			logger.info("支付回调参数验证后结果：" + payNotifyResult.toString());
			return resultBO;
		} catch (Exception e) {
			logger.error("【支付】处理支付异步通知异常，通知参数：" + params.toString() + "。", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	/**  
	* 方法说明: 本地支付，不需要调用第三方
	* @auth: xiongJinGang
	* @param payParam
	* @param userInfo
	* @param userWalletBO
	* @param operateCouponBO
	* @param orderInfoBO
	* @time: 2017年4月6日 上午10:49:03
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> localPay(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO) {
		logger.info("订单【" + payParam.getOrderCode() + "】账户余额或红包支付开始！");
		ResultBO<?> resultBO = ResultBO.ok();
		payParam.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));// 交易码
		try {
			List<PayOrderBaseInfoVO> orderBaseList = payCoreService.transOrder(payParam.getOrderList(), payParam.getUserId());
			// 判断订单是否已过支付截止时间
			PayResultVO payResultVO = Constants.validateOrderPayEndTime(orderBaseList);
			if (!ObjectUtil.isBlank(payResultVO.getOrderPassList())) {// 已过支付截止时间
				logger.info("用户【" + payParam.getUserId() + "】订单【" + PayUtil.getOrderCode(payResultVO.getOrderPassList()) + "】已过支付截止时间");
				return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
			}

			if (!ObjectUtil.isBlank(payResultVO.getOrderFailList())) {// 已支付订单
				logger.info("用户【" + payParam.getUserId() + "】订单【" + PayUtil.getOrderCode(payResultVO.getOrderFailList()) + "】已经支付");
				return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
			}

			// 是否合买
			Boolean isBuyTogether = false;
			OrderBaseInfoBO orderBaseInfo = null;
			// 先判断是不是单个支付，再判断订单的购买类型
			if (payParam.getIsBatchPay().equals(PayConstants.BatchPayEnum.SINGLE.getKey())) {
				orderBaseInfo = payParam.getOrderList().get(0);
				if (orderBaseInfo.getBuyType().equals(PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey())) {
					isBuyTogether = true;
				}
			}
			// 合买支付
			if (isBuyTogether) {
				logger.info("订单【" + payParam.getOrderCode() + "】合买，余额支付！");
				resultBO = buyTogetherService.updateBuyTogetherOrderByLocal(payParam.getOrderCode(), payParam, orderBaseInfo, toPayEndTimeVO);
			} else {
				// 本地正常支付，没加锁之前
				// resultBO = updateLocalPayInfo(payParam, toPayEndTimeVO, orderBaseList, payResultVO);

				// 本地正常支付，加锁之后
				String lockKey = PayUtil.getPayOrderLockKey(PAY_ORDER_LOCK_KEY, payParam.getOrderCode(), payParam.getUserId());
				RLock lock = redissonClient.getLock(lockKey);
				try {
					// 尝试加锁，最多等待2秒，上锁以后3秒自动解锁
					boolean isLock = lock.tryLock(2, 3, TimeUnit.SECONDS);
					if (isLock) { // 成功
						logger.info("订单【" + payParam.getOrderCode() + "】获取到锁");
						// 根据订单号再查询一次订单，得到订单支付状态
						resultBO = payOrderUpdateService.findOrderAndValidate(orderBaseList);
						if (resultBO.isError()) {
							logger.info("获取订单【" + PayUtil.getOrderCode(orderBaseList) + "】信息异常：" + resultBO.getMessage());
							return resultBO;
						}
						List<PayOrderBaseInfoVO> orderTotalList = (List<PayOrderBaseInfoVO>) resultBO.getData();
						// 验证支付状态
						for (PayOrderBaseInfoVO payOrderBaseInfoVO : orderTotalList) {
							// 不是等待支付或者支付进行中，返回错误
							if (!payOrderBaseInfoVO.getPayStatus().equals(PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey()) && !payOrderBaseInfoVO.getPayStatus().equals(PayConstants.PayStatusEnum.BEING_PAID.getKey())) {
								logger.info("订单【" + payOrderBaseInfoVO.getOrderCode() + "】当前支付状态为：" + payOrderBaseInfoVO.getPayStatus() + "，不能再支付");
								return ResultBO.err(MessageCodeConstants.PAY_ORDER_STATUS_ERROR_SERVICE);
							}
						}
						resultBO = updateLocalPayInfo(payParam, toPayEndTimeVO, orderTotalList, payResultVO);
					} else {
						logger.info("订单【" + payParam.getOrderCode() + "】没有获取到锁");
					}
				} catch (InterruptedException e) {
					logger.error("执行分布式锁：" + lockKey + "异常", e);
				} finally {
					lock.unlock();
				}
			}
		} catch (Exception e) {
			logger.error("订单【" + payParam.getOrderCode() + "】用现金或余额支付异常。返回：" + JSON.toJSONString(resultBO), e);
			return ResultBO.ok(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
		}
		return resultBO;
	}

	/**  
	* 方法说明: 本地支付
	* @auth: xiongJinGang
	* @param payParam
	* @param toPayEndTimeVO
	* @param orderBaseList
	* @param payResultVO
	* @throws RuntimeException
	* @throws Exception
	* @throws NumberFormatException
	* @time: 2018年5月2日 上午9:37:02
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateLocalPayInfo(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO, List<PayOrderBaseInfoVO> orderBaseList, PayResultVO payResultVO) throws RuntimeException, Exception, NumberFormatException {
		ResultBO<?> resultBO = payCoreService.modifyBalanceAndStatusForLocal(payParam, toPayEndTimeVO, orderBaseList);
		if (resultBO.isOK()) {
			// 1、处理成功后，发送异步消息，通知拆票，减掉待支付订单数量
			logger.info("订单【" + payParam.getOrderCode() + "】，充值交易号【" + payParam.getTransCode() + "】完成支付，开始发送拆单消息");
			messageProvider.sendMessage(Constants.QUEUE_NAME_FOR_ORDER, StringUtil.interceptEndSymbol(payParam.getOrderCode(), ",") + "#1");

			// 2、修改未支付订单数量
			payOrderUpdateService.subNoPayOrderNum(orderBaseList);
			// 3、提交方案2:支付成功3：支付失败4:未支付过期 代购专有：（等待出票）5：出票中 6 出票失败7.已撤单8：等待开奖9：已中奖10：未中奖11：已派奖 追号专有：12：追号中13：追号结束14：中奖追停15：追号撤单

			StringBuffer daigou = new StringBuffer();
			StringBuffer add = new StringBuffer();
			for (PayOrderBaseInfoVO payOrderBaseInfoVO : orderBaseList) {
				// 追号计划
				if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
					add.append(payOrderBaseInfoVO.getOrderCode()).append(",");
				} else {
					daigou.append(payOrderBaseInfoVO.getOrderCode()).append(",");
				}
			}

			if (!ObjectUtil.isBlank(add.toString())) {
				messageProvider.sendOrderFlowMessage(add.toString(), null, Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_SUCCESS.getKey() + ""), CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey());
			}
			if (!ObjectUtil.isBlank(daigou.toString())) {
				messageProvider.sendOrderFlowMessage(daigou.toString(), null, Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_SUCCESS.getKey() + ""), CancellationConstants.OrderTypeEnum.INDENTORDER.getKey());
			}
			// 4、追号订单生成消息
			messageProvider.sendOrderAddMessage(orderBaseList);

			PayReqResultVO payReqResult = new PayReqResultVO();
			payReqResult.setType(PayConstants.PayReqResultEnum.SHOW.getKey());
			payReqResult.setTransCode(payParam.getTransCode());// 交易码
			// 5、添加订单支付结果信息到缓存，余额支付成功
			redisUtil.addString(CacheConstants.P_CORE_PAY_STATUS_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), PayResultEnum.BALANCE_SUCCESS.getKey(), CacheConstants.ONE_HOURS);
			redisUtil.addObj(CacheConstants.P_CORE_PAY_STATUS_OBJ_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), resultBO.getData(), CacheConstants.ONE_HOURS);
			// 大单预警订单，超过2万的订单
			if (!ObjectUtil.isBlank(payResultVO.getAlarmList())) {
				messageProvider.sendAlarmMessage(payResultVO.getAlarmList());
			}
			return ResultBO.ok(payReqResult);
		} else {
			logger.info("订单【" + payParam.getOrderCode() + "】添加流水、修改账户余额返回失败，请求参数：" + payParam.toString());
		}
		return resultBO;
	}

	/**  
	* 方法说明: 发起支付请求
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param payParam
	* @param userInfo
	* @time: 2017年3月30日 下午6:48:40
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> callPay(PayParamVO payParam, UserInfoBO userInfo, PayOrderBaseInfoVO orderInfo) {
		PayBankBO payBankBO = payParam.getPayBankBO();// 银行
		PayBankcardBO payBankcardBO = payParam.getPayBankcardBO();// 银行卡

		/*************根据银行ID获取银行可以使用的支付渠道***************/
		List<PayChannelBO> payChannelList = payChannelService.findChannelByBankIdUseCache(payParam.getPayBankBO().getId());
		// 未获取到可用支付渠道
		if (ObjectUtil.isBlank(payChannelList)) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}

		/******************获取具体的支付渠道*******************/
		ChannelParamVO channelParam = new ChannelParamVO(payParam.getPlatform(), payParam.getPayAmount(), payBankBO, payBankcardBO, payChannelList, PayConstants.RechargeTypeEnum.PAY.getKey(), payParam.getChannelId(), payParam.getAppId());
		if (!ObjectUtil.isBlank(payParam.getAppId()) && !ObjectUtil.isBlank(payParam.getOpenId()) && (payParam.getPlatform().equals(TakenPlatformEnum.WAP.getKey()) || payParam.getPlatform().equals(TakenPlatformEnum.JSAPI.getKey()))) {
			channelParam.setPlatform(PayConstants.TakenPlatformEnum.JSAPI.getKey());
		}
		ResultBO<?> resultBO = payCoreService.getPayChannel(channelParam);
		if (resultBO.isError()) {
			return resultBO;
		}
		PayTypeResultVO payTypeResultVO = (PayTypeResultVO) resultBO.getData();
		String channelType = payTypeResultVO.getPayTypeName();
		// 支付渠道为空，返回错误
		if (ObjectUtil.isBlank(channelType)) {
			if (!ObjectUtil.isBlank(payBankcardBO)) {
				logger.info("根据用户【" + userInfo.getId() + "，银行" + payBankBO.getcName() + "，卡号：" + payBankcardBO.getCardcode() + "】未匹配上具体的支付渠道");
			}
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}
		String transCode = OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN);
		payParam.setTransCode(transCode);

		/***************拼装统一的api支付请求参数***************/
		UnifiedPayService payService = null;
		try {
			payService = getServiceImpl(channelType);
			if (ObjectUtil.isBlank(payService)) {
				logger.info("筛选给用户【" + userInfo.getId() + "】的支付渠道【" + channelType + "】没有对接");
				return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
			}
		} catch (Exception e) {
			logger.error("筛选给用户【" + userInfo.getId() + "】的支付渠道【" + channelType + "】没有对接", e);
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}

		PaymentInfoBO paymentInfo = getPayParam(payParam, userInfo, transCode, channelType, orderInfo);
		// 支付方式 1:网银支付(借记卡) 2:快捷支付(借记卡) 3:快捷支付(信用卡)8:网银支付(信用卡)9:B2B企业网银支付 10：支付宝 11：微信
		paymentInfo.setPayType(payTypeResultVO.getPayType());
		// 渠道类型转换
		PayUtil.platformChange(channelType, paymentInfo);
		resultBO = payService.pay(paymentInfo);

		/***************获取支付请求返回***************/
		if (resultBO.isOK()) {
			PayReqResultVO payReqResultVO = (PayReqResultVO) resultBO.getData();
			payReqResultVO.setTransCode(transCode);
			payReqResultVO.setChannel(payTypeResultVO.getPayType());// 微信、支付宝、QQ、京东等第三方支付渠道
			if (PayConstants.PayReqResultEnum.LINK.getKey().equals(payReqResultVO.getType()) && !StringUtil.isBlank(payReqResultVO.getFormLink())) {
				// 以字节数组流返回，直接后台生成的，前端用来生成二维码图片
				payReqResultVO.setQrStream(CodeUtil.getQrCode(payReqResultVO.getFormLink()));
			}
			resultBO = ResultBO.ok(payReqResultVO);
			// 记录充值记录
			ResultBO<?> resultBO1 = transRechargeService.addRechargeTransList(userInfo, payParam, payTypeResultVO);
			if (resultBO1.isOK()) {
				if (payParam.getIsBatchPay().intValue() == PayConstants.BatchPayEnum.SINGLE.getKey().intValue() && orderInfo.getBuyType().intValue() == PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey().intValue()) {
					// 如果是单个支付的合买，不修改为支付中（合买只有单个支付）
				} else {
					// 更新订单的支付状态为支付中
					try {
						payOrderUpdateService.updateOrderPayingBatch(payParam.getOrderList());
					} catch (Exception e) {
						logger.error("更新订单到支付中异常", e);
						return ResultBO.err(MessageCodeConstants.PAY_UPDATE_ORDER_PAYSTATUS_ERROR_SERVICE);
					}
				}
			}
			return resultBO1.isOK() ? resultBO : resultBO1;
		}
		return resultBO;
	}

	/**  
	* 方法说明: 组装支付对象
	* @auth: xiongJinGang
	* @param payParam
	* @param userInfo
	* @param transCode
	* @param channelType
	* @time: 2017年4月10日 下午4:23:42
	* @return: PaymentInfoBO 
	*/
	private PaymentInfoBO getPayParam(PayParamVO payParam, UserInfoBO userInfo, String transCode, String channelType, PayOrderBaseInfoVO orderInfo) {
		PaymentInfoBO paymentInfo = new PaymentInfoBO(payParam, userInfo, transCode);
		// 拼装第三方接口支付需要的参数
		paymentInfo.setValidOrder(validTime);
		// 2Ncai-100-2017090-1
		// 商品名称 格式说明：2Ncai（默认名称）-100（彩种ID）-2017090（彩期）-1（购彩方式）
		String name = "2Ncai-";
		if (PayConstants.BatchPayEnum.SINGLE.getKey().equals(payParam.getIsBatchPay())) {
			name += orderInfo.getLotteryCode() + "-" + orderInfo.getLotteryIssue() + "-" + orderInfo.getBuyType();
		} else {
			name += transCode;
		}
		paymentInfo.setNameGoods(name);
		paymentInfo.setRegisterTime(userInfo.getRegisterTime());
		paymentInfo.setInfoOrder(orderInfo.getOrderCode());// 这里商品名称，不使用彩种名，用订单号
		paymentInfo.setNotifyUrl(PayUtil.getPayMethod(channelType, notifyUrl));
		if (!ObjectUtil.isBlank(payParam.getReturnUrl())) {
			String return_url = PayUtil.getReturnUrl(payParam.getReturnUrl(), transCode);
			return_url = return_url.replace("=", "_");
			paymentInfo.setAttach(return_url);// 充值，在回调的时候，根据这个参数判断是调用充值还是支付
			paymentInfo.setUrlReturn(PayUtil.getReturnUrl(payParam.getReturnUrl(), transCode));// 默认拼装一个交易码到同步地址
		}
		// 易宝的网银支付跳转
		if (channelType.equals(PayConstants.ChannelTypeEnum.YEEPAY_FAST.name())) {
			paymentInfo.setUrlReturn(returnUrl);
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.NOWPAY_WAP.name())) {
			paymentInfo.setUrlReturn(PayUtil.getPayMethod(transCode, "{noOrder}", nowPayReturnUrl));
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.PALMPAY_WAP.name())) {
			// 掌易付支付，带token过去
			String url = PayUtil.getReturnUrl(payParam.getReturnUrl(), transCode);
			paymentInfo.setUrlReturn(url + "&token=" + payParam.getToken());
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_FAST.name()) || channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_WEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_WAP.name())) {
			// 1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
			String urlr = PayUtil.getPayMethod(transCode, "{noOrder}", lianPayReturnUrl);
			urlr = PayUtil.getPayMethod(paymentInfo.getPayPlatform().toString(), "{platform}", urlr);
			paymentInfo.setUrlReturn(urlr);
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.SANDPAY_WEB.name())) {// 六度支付
			paymentInfo.setUrlReturn(payParam.getReturnUrl());
		} else if (payParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())
				|| payParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey()) && (channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWAP.name()))
				|| channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_WEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_APP.name()) || channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_WAP.name())) {
			// 1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
			String urlr = PayUtil.getPayMethod(transCode, "{noOrder}", lianPayReturnUrl);
			urlr = PayUtil.getPayMethod(paymentInfo.getPayPlatform().toString(), "{platform}", urlr);
			paymentInfo.setUrlReturn(urlr);
		}

		return paymentInfo;
	}

	/**  
	* 方法说明: 根据支付渠道获取具体的实现类
	* @param channelType 支付渠道【ChannelTypeEnum 的枚举名称：LIANLIAN_WEB、LIANLIAN_APP、LIANLIAN_WAP等】
	* @time: 2017年3月6日 下午6:29:05
	* @return: IPayService 
	*/
	private UnifiedPayService getServiceImpl(String channelType) {
		ChannelTypeEnum channel = PayUtil.getChannelType(channelType);
		return realPayServices.get(channel.getChannel());
	}

	/**  
	* 方法说明: 批量处理异步通知（在同一个事务中）
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param payAttachVO
	* @throws Exception
	* @time: 2017年5月16日 下午2:57:10
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> batchNotify(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception {
		transRecharge.setArrivalAmount(payNotifyResult.getOrderAmt());// 到账金额
		BatchPayOrderVO batchPayOrderVO = PayUtil.getOrderInfo(transRecharge.getOrderCode());// 订单编号和订单购买类型数组
		logger.info("充值编号【" + transRecharge.getTransRechargeCode() + "】支付结果状态：" + payNotifyResult.getStatus() + "[1等待支付；2支付成功；3未支付过期；4支付失败；5用户取消]");
		if (PayConstants.PayStatusEnum.PAYMENT_SUCCESS.equals(payNotifyResult.getStatus())) {
			// 1、 更新充值、钱包、彩金红包、交易流水、订单、方案记录等信息
			ResultBO<?> resultBO = payCoreService.modifyPaySuccessTransRecordForBatch(transRecharge, payNotifyResult, batchPayOrderVO.getList());
			if (resultBO.isOK()) {
				// 2、 在支付时间内支付成功后，通知拆票，减掉待支付订单数量
				PayResultVO payResult = (PayResultVO) resultBO.getData();
				if (!ObjectUtil.isBlank(payResult.getOrderSuccessList())) {
					dealPaySuccessResult(payResult.getOrderSuccessList(), transRecharge, payNotifyResult);
				}
				// 已过支付截止时间
				if (!ObjectUtil.isBlank(payResult.getOrderPassList())) {
					logger.info("存在已过支付截止时间订单");
					dealPayFailResult(payNotifyResult, payResult.getOrderPassList(), false);
				}
				// 订单支付状态错误
				if (!ObjectUtil.isBlank(payResult.getOrderFailList())) {
					logger.info("存在支付状态错误订单");
					dealPayFailResult(payNotifyResult, payResult.getOrderFailList(), false);
				}
				// 存在2万及以上的大单，需要预警
				if (!ObjectUtil.isBlank(payResult.getAlarmList())) {
					messageProvider.sendAlarmMessage(payResult.getAlarmList());
				}
				// 往当日渠道限额中添加金额
				payCoreService.addDayLimitAmount(transRecharge);
			}
			return resultBO;
		} else {
			logger.info("充值编号【" + transRecharge.getTransRechargeCode() + "】支付失败，处理失败逻辑");
			// 支付失败，更新充值、交易流水、订单支付状态记录
			ResultBO<?> resultBO = payCoreService.modifyFailTransRecord(transRecharge, payNotifyResult, batchPayOrderVO.getList());
			if (resultBO.isOK()) {
				PayResultVO payResult = (PayResultVO) resultBO.getData();
				if (!ObjectUtil.isBlank(payResult.getOrderFailList())) {
					dealPayFailResult(payNotifyResult, payResult.getOrderFailList(), true);
				}
			}
			return resultBO;
		}
	}

	/**  
	* 方法说明: 支付成功的操作
	* @auth: xiongJinGang
	* @param payResult
	* @param transRecharge
	* @param batchPayOrderVO
	* @param payNotifyResult
	* @time: 2017年6月8日 下午5:26:38
	* @return: void 
	*/
	private void dealPaySuccessResult(List<PayOrderBaseInfoVO> successList, TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) {
		StringBuffer sbBuffer = new StringBuffer();
		StringBuffer daigou = new StringBuffer();
		StringBuffer add = new StringBuffer();
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : successList) {
			sbBuffer.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			// 追号计划
			if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				add.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			} else {
				daigou.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			}
		}
		String orderCodes = sbBuffer.toString();
		logger.info("充值交易号【" + transRecharge.getTransRechargeCode() + "】完成支付，开始发送拆单消息");
		// 1 表示支付后拆单
		messageProvider.sendMessage(Constants.QUEUE_NAME_FOR_ORDER, StringUtil.interceptEndSymbol(orderCodes, ",") + "#1");
		// 2、减去缓存中未支付订单的数量
		payOrderUpdateService.subNoPayOrderNum(successList);
		// 发送方案详情消息 1：提交方案2:支付成功3：支付失败4:未支付过期 代购专有：（等待出票）5：出票中 6 出票失败7.已撤单8：等待开奖9：已中奖10：未中奖11：已派奖 追号专有：12：追号中13：追号结束14：中奖追停15：追号撤单
		logger.info("支付成功，开始发送方案详情消息");

		if (!ObjectUtil.isBlank(add.toString())) {
			messageProvider.sendOrderFlowMessage(add.toString(), payNotifyResult.getTradeTime(), Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_SUCCESS.getKey() + ""), CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey());
		}
		if (!ObjectUtil.isBlank(daigou.toString())) {
			messageProvider.sendOrderFlowMessage(daigou.toString(), payNotifyResult.getTradeTime(), Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_SUCCESS.getKey() + ""), CancellationConstants.OrderTypeEnum.INDENTORDER.getKey());
		}

		// 追号订单生成消息
		messageProvider.sendOrderAddMessage(successList);
	}

	/**  
	* 方法说明: 支付失败的操作
	* @auth: xiongJinGang
	* @param transRecharge
	* @param batchPayOrderVO
	* @param payNotifyResult
	* @time: 2017年6月8日 下午5:26:46
	* @return: void 
	*/
	private void dealPayFailResult(PayNotifyResultVO payNotifyResult, List<PayOrderBaseInfoVO> failList, boolean isSend) {
		StringBuffer sbBuffer = new StringBuffer();
		StringBuffer daigou = new StringBuffer();
		StringBuffer add = new StringBuffer();
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : failList) {
			sbBuffer.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			// 追号计划
			if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				add.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			} else {
				daigou.append(payOrderBaseInfoVO.getOrderCode()).append(",");
			}
		}
		String orderCodes = sbBuffer.toString();
		logger.info("订单【" + orderCodes + "】支付状态错误，支付失败。减去缓存中未支付订单的数量，发送支付失败消息");
		// 1、减去缓存中未支付订单的数量
		payOrderUpdateService.subNoPayOrderNum(failList);
		// 2：提交方案2:支付成功3：支付失败4:未支付过期 代购专有：（等待出票）5：出票中 6 出票失败7.已撤单8：等待开奖9：已中奖10：未中奖11：已派奖 追号专有：12：追号中13：追号结束14：中奖追停15：追号撤单
		if (isSend) {
			logger.info("支付失败，开始发送支付失败方案详情消息");
			if (!ObjectUtil.isBlank(add.toString())) {
				messageProvider.sendOrderFlowMessage(add.toString(), payNotifyResult.getTradeTime(), Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_FAIL.getKey() + ""), CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey());
			}
			if (!ObjectUtil.isBlank(daigou.toString())) {
				messageProvider.sendOrderFlowMessage(daigou.toString(), payNotifyResult.getTradeTime(), Short.parseShort(OrderFlowInfoEnum.StatusEnum.PAY_FAIL.getKey() + ""), CancellationConstants.OrderTypeEnum.INDENTORDER.getKey());
			}
		}
	}

	/**  
	* 方法说明: 根据银行ID获取支付类型
	* @auth: xiongJinGang
	* @param transRechargeBO
	* @time: 2017年10月13日 下午2:48:28
	* @return: void 
	*/
	private String getPayType(TransRechargeBO transRechargeBO) {
		String payType = null;
		// 根据充值记录中的银行，查找银行编号，目前只需要知道支付宝和微信
		PayBankBO payBankBO = payBankService.findBankById(transRechargeBO.getRechargeBank());
		if (!ObjectUtil.isBlank(payBankBO) && !ObjectUtil.isBlank(payBankBO.getCode())) {
			if (payBankBO.getCode().equals(PayConstants.PayBankCodeEnum.ALIPAY.getKey())) {
				payType = PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey();
			} else if (payBankBO.getCode().equals(PayConstants.PayBankCodeEnum.WECHAT.getKey())) {
				payType = PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey();
			} else if (payBankBO.getCode().equals(PayConstants.PayBankCodeEnum.QQ.getKey())) {
				payType = PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey();
			} else {
				payType = PayConstants.PayTypeThirdEnum.BANK_DEBIT_CARD_PAYMENT.getKey();
			}
		}
		return payType;
	}

	// 关闭充值记录
	@Override
	public void modifyCloseOrder(TransRechargeBO transRecharge) {
		String rechargeCode = transRecharge.getTransRechargeCode();
		try {
			PayQueryParamVO payQueryParamVO = new PayQueryParamVO();
			payQueryParamVO.setTransCode(rechargeCode);
			ResultBO<?> resultBO = payQuery(payQueryParamVO);
			logger.info("查询充值编号【" + rechargeCode + "】支付结果返回：" + JSON.toJSONString(resultBO));
			// 未获取到结果或者失败，改为已过期
			if (resultBO.isError()) {
				if (!resultBO.getErrorCode().equals(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE)) {
					try {
						TransRechargePO transRechargePO = new TransRechargePO();
						transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_CLOSED.getKey());// 设置成已关闭
						transRechargePO.setModifyBy(Constants.SYSTEM_OPERATE);// 系统操作
						transRechargePO.setId(transRecharge.getId());
						int num = transRechargeService.updateRecharge(transRechargePO);
						logger.info("系统将交易号【" + rechargeCode + "】状态设置成已关闭返回：" + (num > 0 ? "成功" : "失败"));
						if (num > 0) {
							redisUtil.delObj(CacheConstants.P_CORE_RECHARGE_ORDER + rechargeCode);
						}
					} catch (Exception e) {
						logger.error("系统将交易号【" + rechargeCode + "】状态设置成已关闭异常", e);
					}
				}
			} else {
				// 支付成功，判断支付结果是否为未支付或者支付过期
				PayQueryResultVO payQueryResultVO = (PayQueryResultVO) resultBO.getData();
				if (!ObjectUtil.isBlank(payQueryResultVO)) {
					boolean condition = false;
					PayStatusEnum payStatusEnum = payQueryResultVO.getTradeStatus();
					// 现在支付、聚合支付超时等待支付也关闭
					if (transRecharge.getRechargeChannel().equals(PayConstants.PayChannelEnum.NOWPAY_RECHARGE.getKey()) || transRecharge.getRechargeChannel().equals(PayConstants.PayChannelEnum.JUHEPAY_RECHARGE.getKey())) {
						// 超时、用户取消、等待支付
						if (payStatusEnum.equals(PayConstants.PayStatusEnum.OVERDUE_PAYMENT) || payStatusEnum.equals(PayConstants.PayStatusEnum.USER_CANCELLED_PAYMENT) || payStatusEnum.equals(PayConstants.PayStatusEnum.WAITTING_PAYMENT)) {
							condition = true;
						}
					} else {
						// 超时、用户取消
						if (payStatusEnum.equals(PayConstants.PayStatusEnum.OVERDUE_PAYMENT) || payStatusEnum.equals(PayConstants.PayStatusEnum.USER_CANCELLED_PAYMENT)) {
							condition = true;
						}
					}
					// 超时和用户取消才更新
					if (condition) {
						try {
							TransRechargePO transRechargePO = new TransRechargePO();
							transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_CLOSED.getKey());// 设置成已关闭
							transRechargePO.setModifyBy(Constants.SYSTEM_OPERATE);// 系统操作
							transRechargePO.setId(transRecharge.getId());
							int num = transRechargeService.updateRecharge(transRechargePO);
							logger.info("系统将交易号【" + rechargeCode + "】状态设置成已关闭返回：" + (num > 0 ? "成功" : "失败"));
							if (num > 0) {
								redisUtil.delObj(CacheConstants.P_CORE_RECHARGE_ORDER + rechargeCode);
							}
						} catch (Exception e) {
							logger.error("系统将交易号【" + rechargeCode + "】状态设置成已关闭异常", e);
						}
					}

				}
			}
		} catch (Exception e) {
			logger.error("关闭交易号【" + rechargeCode + "】异常", e);
		}

	}
}
