package com.hhly.paycore.remote.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.common.PayCommon;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.jms.DelayService;
import com.hhly.paycore.paychannel.UnifiedPayService;
import com.hhly.paycore.remote.service.IPayPushOrderService;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.ChannelTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayResultEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderCopyPayInfoBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.vo.ChannelParamVO;
import com.hhly.skeleton.pay.channel.vo.PayTypeResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.CodeUtil;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】推单支付
 * @author xiongJinGang
 * @date 2018年1月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iPayPushOrderService")
public class PayPushOrderServiceImpl extends PayCommon implements IPayPushOrderService {
	private static final Logger logger = Logger.getLogger(PayPushOrderServiceImpl.class);
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private PayBankService payBankService;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private PayChannelService payChannelService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private PayCoreService payCoreService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private DelayService delayService;// 延时处理
	@Value("${delayQueue.isopen}")
	private String openDelayQueue;// true 打开delayQueue延时关闭，false关闭
	@Value("${recharge.remaining.valid.time}")
	private String validTime;// 支付有效时间
	@Value("${recharge.return.url}")
	private String returnUrl;// 支付同步返回URL
	@Value("${recharge.notify.url}")
	private String notifyUrl;// 支付异步返回URL
	@Value("${nowpay.return.url}")
	private String nowPayReturnUrl;// 现在支付同步返回地址
	@Value("${lianpay.return.url}")
	private String lianPayReturnUrl;// 连连支付同步返回地址

	@Override
	public ResultBO<?> pushPay(PayParamVO payParam) {
		logger.info("支付请求参数：" + payParam.toString());
		// 1、 验证token是否为空，是否有用户信息
		UserInfoBO userInfo = userUtil.getUserByToken(payParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		payParam.setUserId(userInfo.getId());// 设置用户Id
		// 2、验证基本参数有效性
		ResultBO<?> resultBO = PayUtil.validatePushPayParams(payParam);
		if (resultBO.isError()) {
			logger.info("方案详情【" + payParam.getIssueId() + "】支付请求参数验证不过");
			return resultBO;
		}
		// 获取验证后的方案信息
		OrderCopyPayInfoBO orderCopyPayInfoBO = payParam.getOrderCopyPayInfoBO();
		if (ObjectUtil.isBlank(orderCopyPayInfoBO)) {
			logger.info("获取方案【" + payParam.getOrderCopyPayInfoBO() + "】详情为空");
			return ResultBO.err(MessageCodeConstants.ORDER_NOT_EXIST_OR_INVALILD);
		}

		// 验证用户对该方案详情是否已支付，去查询third_trans_detail
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
		PayOrderBaseInfoVO orderInfoBO = new PayOrderBaseInfoVO();
		orderInfoBO.setOrderCode(orderCopyPayInfoBO.getIssueCode());
		orderInfoBO.setOrderAmount(orderCopyPayInfoBO.getPrice());
		// 5、 验证账户钱包、彩金红包金额是否够支付
		resultBO = PayUtil.validatePayAmount(payParam, userInfo, null, userWalletBO, orderInfoBO);
		if (resultBO.isError()) {
			logger.info("用户【" + userInfo.getId() + "】账户金额不够支付：" + resultBO.getMessage());
			return resultBO;
		}

		// 判断是否需要调用第三方支付（判断需要支付的金额为0 并且支付银行ID为空）
		if (MathUtil.compareTo(payParam.getPayAmount(), 0.0) == 0) {
			// 不需要调用第三方支付，修改余额，修改彩金红包金额或者记录
			resultBO = localPay(payParam, orderCopyPayInfoBO);
		} else {
			// 银行ID为空
			if (ObjectUtil.isBlank(payParam.getBankId())) {
				logger.info("用户【" + userInfo.getId() + "】支付金额不为空，但银行ID为空");
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_TYPE_IS_NULL_FIELD);
			}
			// 需要调用第三方支付并添加充值记录
			resultBO = callPay(payParam, userInfo, orderInfoBO);
		}
		return resultBO;
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
	private ResultBO<?> localPay(PayParamVO payParam, OrderCopyPayInfoBO orderCopyPayInfoBO) {
		logger.info("方案详情【" + payParam.getIssueId() + "】账户余额或红包支付开始！");
		payParam.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));// 交易码
		ResultBO<?> resultBO = null;
		try {
			// 判断订单是否已过支付截止时间
			boolean outTime = Constants.validateTime(orderCopyPayInfoBO.getLotteryCode(), orderCopyPayInfoBO.getSaleEndDate());
			if (!outTime) {// 已过支付截止时间
				logger.info("用户【" + payParam.getUserId() + "】支付方案详情【" + payParam.getIssueId() + "】已过支付截止时间");
				return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
			}

			resultBO = payCoreService.modifyPushStatusForLocal(payParam);
			if (resultBO.isOK()) {
				// 1、处理成功
				logger.info("方案详情【" + payParam.getOrderCode() + "】，充值交易号【" + payParam.getTransCode() + "】完成支付");

				PayReqResultVO payReqResult = new PayReqResultVO();
				payReqResult.setType(PayConstants.PayReqResultEnum.SHOW.getKey());
				payReqResult.setTransCode(payParam.getTransCode());// 交易码
				// 5、添加订单支付结果信息到缓存，余额支付成功
				redisUtil.addString(CacheConstants.P_CORE_PAY_STATUS_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), PayResultEnum.BALANCE_SUCCESS.getKey(), CacheConstants.ONE_HOURS);
				redisUtil.addObj(CacheConstants.P_CORE_PAY_STATUS_OBJ_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), resultBO.getData(), CacheConstants.ONE_HOURS);
				return ResultBO.ok(payReqResult);
			} else {
				logger.info("订单【" + payParam.getOrderCode() + "】添加流水、修改账户余额返回失败，请求参数：" + payParam.toString());
			}
			return resultBO;
		} catch (Exception e) {
			logger.error("订单【" + payParam.getOrderCode() + "】用现金或余额支付异常。返回：" + JSON.toJSONString(resultBO), e);
			return ResultBO.ok(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
		}
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
		Short platform = payParam.getPlatform();// 平台
		PayBankBO payBankBO = payParam.getPayBankBO();// 银行
		PayBankcardBO payBankcardBO = payParam.getPayBankcardBO();// 银行卡
		Double payAmount = payParam.getPayAmount();// 支付金额

		/*************根据银行ID获取银行可以使用的支付渠道***************/
		List<PayChannelBO> payChannelList = payChannelService.findChannelByBankIdUseCache(payParam.getPayBankBO().getId());
		// 未获取到可用支付渠道
		if (ObjectUtil.isBlank(payChannelList)) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}

		/******************获取具体的支付渠道*******************/
		ChannelParamVO channelParam = new ChannelParamVO(platform, payAmount, payBankBO, payBankcardBO, payChannelList, PayConstants.RechargeTypeEnum.PAY.getKey(), payParam.getChannelId(), payParam.getAppId());
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
		resultBO = payService.pay(paymentInfo);

		/***************获取支付请求返回***************/
		if (resultBO.isOK()) {
			PayReqResultVO payReqResultVO = (PayReqResultVO) resultBO.getData();
			payReqResultVO.setTransCode(transCode);
			payReqResultVO.setChannel(payTypeResultVO.getPayType());// 微信、支付宝、QQ、京东等第三方支付渠道
			if (PayConstants.PayReqResultEnum.LINK.getKey().equals(payReqResultVO.getType())) {
				// 以字节数组流返回，直接后台生成的，前端用来生成二维码图片
				payReqResultVO.setQrStream(CodeUtil.getQrCode(payReqResultVO.getFormLink()));
			}
			resultBO = ResultBO.ok(payReqResultVO);
			// 记录充值记录
			ResultBO<?> resultBO1 = transRechargeService.addRechargeTransList(userInfo, payParam, payTypeResultVO);
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
		} else if (payParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey()) || payParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey())
				&& (channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWAP.name()))) {
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

}
