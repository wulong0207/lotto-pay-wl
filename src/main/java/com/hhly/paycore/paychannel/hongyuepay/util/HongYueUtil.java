package com.hhly.paycore.paychannel.hongyuepay.util;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.hongyuepay.config.HongYueConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.xingye.config.XingYeConfig;
import com.hhly.paycore.sign.SignUtils;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeThirdEnum;
import com.hhly.skeleton.base.constants.PayConstants.XingYeTradeTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.*;
import com.hhly.utils.BuildRequestFormUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * @desc 鸿粤工具类
 * @author xiongJinGang
 * @date 2018年1月4日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HongYueUtil {
	private static final Logger logger = Logger.getLogger(HongYueUtil.class);

	/**  
	* 方法说明: 支付请求
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param platform
	* @time: 2017年11月23日 下午5:49:44
	* @return: ResultBO<Map<String,String>> 
	*/
	@SuppressWarnings("unchecked")
	public static ResultBO<Map<String, String>> pay(PaymentInfoBO paymentInfo, String platform) {
		PayTypeThirdEnum payType = PayTypeThirdEnum.getEnum(paymentInfo.getPayType());
		logger.info("鸿粤【" + payType.getValue() + "】支付请求参数：" + paymentInfo.toString());
		String[] mchInfo = HongYueUtil.getMchInfo(paymentInfo.getPayType(), platform);
		String key = mchInfo[0];
		String mchId = mchInfo[1];
		String interfaceType = mchInfo[2];

		Map<String, String> paramMap = buildPayParam(paymentInfo, mchId, key, interfaceType, platform);
		// 调用接口
		Map<String, String> resultMap = HttpUtil.doPostXml(paramMap, HongYueConfig.HONGYUE_TRADE_URL);
		logger.info("鸿粤请求支付返回：" + resultMap.toString());
		ResultBO<?> resultBO = checkPayResult(paymentInfo, payType, key, resultMap, platform);
		if (resultBO.isError()) {
			return ((ResultBO<Map<String, String>>) resultBO);
		}
		return ResultBO.ok(resultMap);
	}

	/**
	* 方法说明: 鸿粤【支付宝扫码】支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param mchId
	* @param key
	* @param interfaceType
	* @time: 2017年11月23日 下午2:43:36
	* @return: Map<String,String>
	*/
	public static Map<String, String> buildPayParam(PaymentInfoBO paymentInfo, String mchId, String key, String interfaceType, String platform) {
		try {
			// 装载订单信息
			Map<String, String> map = new HashMap<String, String>();
			map.put("service", interfaceType);// 接口类型
			map.put("version", "2.0");// 版本号，version默认值是2.0
			map.put("charset", "UTF-8");// 可选值 UTF-8 ，默认为 UTF-8
			map.put("sign_type", "MD5");// 签名类型，取值：MD5默认：MD5
			map.put("mch_id", mchId);// 商户号
			map.put("out_trade_no", paymentInfo.getNoOrder());// 商户订单号
			map.put("body", paymentInfo.getInfoOrder());// 商品描述
			map.put("attach", paymentInfo.getAttach());// 商户附加信息，可做扩展参数
			map.put("total_fee", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));// 总金额，以分为单位，不允许包含任何字、符号
			map.put("mch_create_ip", paymentInfo.getUserreqIp());// 订单生成的机器 IP
			map.put("notify_url", paymentInfo.getNotifyUrl());// 通知地址

			// 订单生成时间，格式为yyyymmddhhmmss，如2009年12月25日9点10分10秒表示为20091225091010。时区为GMT+8 beijing。该时间取自商户服务器。注：订单生成时间与超时时间需要同时传入才会生效。
			map.put("time_start", DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
			// 订单失效时间，格式为yyyymmddhhmmss，如2009年12月27日9点10分10秒表示为20091227091010。时区为GMT+8 beijing。该时间取自商户服务器。注：订单生成时间与超时时间需要同时传入才会生效。
			Date thirdAfter = DateUtil.addMinute(new Date(), Integer.parseInt(paymentInfo.getValidOrder()));
			map.put("time_expire", DateUtil.convertDateToStr(thirdAfter, DateUtil.DATE_FORMAT_NUM));
			map.put("nonce_str", GetWeChatUtil.getRandomStr(32));// 随机字符串，不长于 32 位

			// 微信支付的wap支付，需要这些参数
			if (paymentInfo.getPayType().equals(PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && (platform.equals(PayAbstract.PLATFORM_WAP) || platform.equals(PayAbstract.PLATFORM_APP) || platform.equals(PayAbstract.PLATFORM_JSAPI))) {
				if (platform.equals(PayAbstract.PLATFORM_JSAPI)) {
					map.put("is_raw", "1");// 原生JS
					// if (!paymentInfo.isTest()) {
					map.put("sub_openid", paymentInfo.getOpenId());// 微信用户关注商家公众号的openid
					map.put("sub_appid", paymentInfo.getAppId());// 微信公众平台基本配置中的AppID(应用ID)
					// }
				} else {
					map.put("callback_url", paymentInfo.getUrlReturn());// 页面通知地址
					String deviceInfo = "iOS_WAP";
					if (platform.equals(PayAbstract.PLATFORM_WAP)) {
						// 如果是用于苹果app应用里值为iOS_SDK；如果是用于安卓app应用里值为AND_SDK；如果是用于手机网站，值为iOS_WAP或AND_WAP均可
						if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey())) {
							deviceInfo = "AND_WAP";
						}
					} else {
						deviceInfo = "iOS_SDK";
						if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey())) {
							deviceInfo = "AND_SDK";
						}
					}
					map.put("device_info", deviceInfo);// 应用类型

					// 如果是用于苹果或安卓app应用中，传分别对应在AppStore和安桌分发市场中的应用名（如：王者荣耀）如果是用于手机网站，传对应的网站名(如：京东官网)
					map.put("mch_app_name", "2ncai");// 应用名
					// 如果是用于苹果或安卓app应用中，苹果传IOS 应用唯一标识(如：com.tencent.wzryIOS)安卓传包名(如：com.tencent.tmgp.sgame)如果是用于手机网站，传网站首页URL地址,必须保证公网能正常访问(如：https://m.jd.com)
					map.put("mch_app_id", "https://m.2ncai.com");// 应用标识
				}
			}
			String sign = BuildRequestFormUtil.createLinkString(map, key);
			map.put("sign", sign);// MD5签名结果
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	* 方法说明: 获取支付结果
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:41:00
	* @return: ResultBO<?>
	*/
	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO, String platform) {
		String[] mchInfo = getMchInfo(payQueryParamVO.getRechargeChannel(), platform);
		String key = mchInfo[0];
		String mchId = mchInfo[1];
		// 封装请求参数
		SortedMap<String, String> paramMap = new TreeMap<String, String>();
		paramMap.put("service", XingYeTradeTypeEnum.TRADE_QUERY.getValue());
		paramMap.put("version", "2.0");
		paramMap.put("charset", "UTF-8");
		paramMap.put("sign_type", "MD5");
		paramMap.put("mch_id", mchId);
		paramMap.put("out_trade_no", payQueryParamVO.getTransCode());
		paramMap.put("transaction_id", payQueryParamVO.getTradeNo());// 平台交易号, out_trade_no和transaction_id至少一个必填，同时存在时transaction_id优先。
		paramMap.put("nonce_str", GetWeChatUtil.getNonceStr());

		String sign = BuildRequestFormUtil.createLinkString(paramMap, key);
		paramMap.put("sign", sign);
		Map<String, String> resultMap = HttpUtil.doPostXml(paramMap, XingYeConfig.XY_PAY_URL);
		if (!ObjectUtil.isBlank(resultMap)) {
			logger.info("查询鸿粤支付结果返回：" + resultMap.toString());
			if (resultMap.containsKey("sign")) {
				if (!SignUtils.checkParam(resultMap, key)) {
					logger.info("验证签名不通过");
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}
				// 以下字段在 status 和 result_code 都为 0的时候有返回
				if (resultMap.get("status").equals("0") && resultMap.get("result_code").equals("0")) {
					// SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭 REVERSE—已冲正 REVOK—已撤销
					String tradeState = resultMap.get("trade_state");
					PayStatusEnum payStatusEnum = BuildRequestFormUtil.getPayStatus(tradeState);
					if (tradeState.equals("SUCCESS")) {
						PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
						payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("total_fee")));
						payQueryResultVO.setTradeNo(resultMap.get("out_transaction_id"));
						payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
						payQueryResultVO.setOrderCode(resultMap.get("out_trade_no"));
						payQueryResultVO.setTradeStatus(payStatusEnum);
						return ResultBO.ok(payQueryResultVO);
					} else {
						return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
					}
				}
			}
		}
		return ResultBO.err();
	}

	/**
	* 方法说明: 从异步通知中，得到支付结果
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月16日 下午3:01:13
	* @return: ResultBO<?>
	*/
	public static ResultBO<?> analyPayResult(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("鸿粤支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			// 交易类型，区分支付宝还是微信扫码
			if (!map.containsKey("trade_type")) {
				logger.info("鸿粤支付异步通知trade_type为空");
				return ResultBO.err();
			}
			String key = null;
			XingYeTradeTypeEnum XingYeTradeTypeEnum = PayConstants.XingYeTradeTypeEnum.getEnum(map.get("trade_type"));
			switch (XingYeTradeTypeEnum) {
			case QQ_SCANCODE:// QQ扫码，支付请求用的是这个
			case QQ_SCANCODE2:// QQ扫码，支付完成，异步通知用的是这个
				key = HongYueConfig.HONGYUE_QQ_PAY_KEY;
				break;
			case QQ_WAP:
				key = HongYueConfig.HONGYUE_QQ_WALLET_PAY_KEY;
				break;
			case ALIPAY_SCANCODE:
				key = HongYueConfig.HONGYUE_ALI_PAY_KEY;
				break;
			case ALIPAY_WAP:
				// key = HongYueConfig.XY_ALI_PAY_KEY;
				break;
			case WX_WAP:
				key = HongYueConfig.HONGYUE_WX_WAP_PAY_KEY;
				break;
			case WX_APP:
				key = HongYueConfig.HONGYUE_WX_APP_PAY_KEY;
				break;
			case WX_SCANCODE:
				key = HongYueConfig.HONGYUE_WX_PAY_KEY;
				break;
			case WX_JSAPI:
				key = HongYueConfig.HONGYUE_WX_JSAPI_PAY_KEY;
				break;
			default:
				break;
			}

			if (!SignUtils.checkParam(map, key)) {
				logger.info("鸿粤验证签名不通过");
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}

			String status = map.get("status");
			// 0表示成功，非0表示失败此字段是通信标识，非交易标识，交易是否成功需要查看 result_code 来判断
			if (StringUtils.isNotBlank(status) && "0".equals(status)) {
				String result_code = map.get("result_code");
				// 0表示成功，非0表示失败
				if (result_code != null && "0".equals(result_code)) {
					payNotifyResult.setOrderCode(map.get("out_trade_no"));// 商户唯一订单号
					// 支付结果：0-成功：其他-失败
					String payResult = map.get("pay_result");
					if (payResult != null && "0".equals(payResult)) {
						payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
					} else {
						payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
					}
					payNotifyResult.setThirdTradeNo(map.get("transaction_id"));// 平台订单号
					payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("total_fee"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
					payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
					payNotifyResult.setResponse("success");
					// map.get("bank_type");// 银行类型
					// map.get("bank_billno");//银行订单号，若为手Q支付则为空
					return ResultBO.ok(payNotifyResult);
				}
			}
			logger.info("鸿粤支付异步通知状态失败，不进行处理");
		}
		return ResultBO.err();
	}

	/**
	* 方法说明: 退款
	* @auth: xiongJinGang
	* @param refundParam
	* @time: 2017年10月16日 下午3:44:34
	* @return: ResultBO<?>
	*/
	public static ResultBO<?> orderRefund(RefundParamVO refundParam, String platform) {
		// 第三方支付，才需要判断是支付宝还是手Q
		String[] mchInfo = getMchInfo(refundParam.getRechargeChannel(), platform);
		String key = mchInfo[0];
		String mchId = mchInfo[1];
		String refundUrl = mchInfo[3];

		// 封装请求参数
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("service", XingYeTradeTypeEnum.REFUND.getValue());// 接口类型：unified.trade.refund
		paramMap.put("version", "2.0");// 版本号，version默认值是2.0
		paramMap.put("charset", "UTF-8");// 可选值 UTF-8 ，默认为 UTF-8
		paramMap.put("sign_type", "MD5");// 签名类型，取值：MD5默认：MD5
		paramMap.put("mch_id", mchId);
		paramMap.put("out_trade_no", refundParam.getTransCode());// 商户订单号
		if (!ObjectUtil.isBlank(refundParam.getTradeNo())) {
			paramMap.put("transaction_id", refundParam.getTradeNo());// 平台交易号, out_trade_no和transaction_id至少一个必填，同时存在时transaction_id优先。
		}
		paramMap.put("out_refund_no", PayAbstract.REFUND_HEADER + DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
		paramMap.put("total_fee", GetWeChatUtil.getMoney(String.valueOf(refundParam.getOrderAmount())));// 订单总金额，单位为分
		paramMap.put("refund_fee", GetWeChatUtil.getMoney(String.valueOf(refundParam.getRefundAmount())));// 退款总金额,单位为分,可以做部分退款
		paramMap.put("op_user_id", mchId);// 操作员帐号,默认为商户号
		paramMap.put("refund_channel", "ORIGINAL");// ORIGINAL-原路退款，默认
		paramMap.put("nonce_str", GetWeChatUtil.getNonceStr());

		String sign = BuildRequestFormUtil.createLinkString(paramMap, key);
		paramMap.put("sign", sign);// MD5签名结果

		try {
			logger.debug("退款请求参数：" + paramMap.toString());
			Map<String, String> resultMap = HttpUtil.doPostXml(paramMap, refundUrl);
			if (!ObjectUtil.isBlank(resultMap)) {
				logger.info("退款返回结果：" + resultMap.toString());
				if (resultMap.containsKey("sign")) {
					if (!SignUtils.checkParam(resultMap, key)) {
						logger.info("验证签名不通过");
						return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					}
					if (resultMap.get("status").equals("0")) {
						RefundResultVO refundResultVO = new RefundResultVO();
						refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
						refundResultVO.setResultMsg("退款失败");
						// 以下字段在 status 和 result_code 都为 0的时候有返回
						if (resultMap.get("result_code").equals("0")) {
							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setOrderCode(resultMap.get("out_trade_no"));// 商户订单号
							refundResultVO.setRefundCode(resultMap.get("out_refund_no"));// 商户退款单号
							refundResultVO.setTransactionId(resultMap.get("transaction_id"));// 平台订单号
							refundResultVO.setRefundId(resultMap.get("refund_id"));// 平台退款单号
							refundResultVO.setRefundChannel(resultMap.get("refund_channel"));// 退款渠道 ORIGINAL—原路退款，默认
							refundResultVO.setResultMsg("退款成功");
							refundResultVO.setRefundAmount(GetWeChatUtil.changeF2Y(resultMap.get("refund_fee")));// 退款总金额,单位为分,可以做部分退款
							refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_PROCESSING);
							return ResultBO.ok(refundResultVO);
						} else {
							String errorMessage = "退款失败";
							if (resultMap.containsKey("err_msg")) {
								errorMessage = resultMap.get("err_msg");
							}
							return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
						}
					} else {
						// {message=您的请求过于频繁, status=400, charset=UTF-8, version=2.0}
						String errorMessage = "退款请求失败";
						if (resultMap.containsKey("message")) {
							errorMessage = resultMap.get("message");
						}
						return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
					}
				}
			}
		} catch (Exception e) {
			logger.error("兴业退款请求异常", e);
		}
		return ResultBO.err();
	}

	/**
	* 方法说明: 退款查询
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:48:25
	* @return: ResultBO<?>
	*/
	public static ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO, String platform) {
		// 第三方支付，才需要判断是支付宝还是手Q
		String[] mchInfo = getMchInfo(payQueryParamVO.getRechargeChannel(), platform);
		String key = mchInfo[0];
		String mchId = mchInfo[1];

		// 封装请求参数
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("service", XingYeTradeTypeEnum.REFUND_QUERY.getValue());// 接口类型：unified.trade.refundquery
		paramMap.put("version", "2.0");// 版本号，version默认值是2.0
		paramMap.put("charset", "UTF-8");// 可选值 UTF-8 ，默认为 UTF-8
		paramMap.put("sign_type", "MD5");// 签名类型，取值：MD5默认：MD5
		paramMap.put("mch_id", mchId);
		paramMap.put("out_trade_no", payQueryParamVO.getTransCode());// 商户订单号
		paramMap.put("transaction_id", payQueryParamVO.getTradeNo());// 平台交易号, out_trade_no和transaction_id至少一个必填，同时存在时transaction_id优先。

		// 平台退款单号refund_id、out_refund_no、out_trade_no 、transaction_id 四个参数必填一个， 如果同时存在优先级为：refund_id>out_refund_no>transaction_id>out_trade_no
		// paramMap.put("out_refund_no", "");//商户退款单号
		// paramMap.put("refund_id", "");//平台退款单号

		paramMap.put("nonce_str", GetWeChatUtil.getNonceStr());

		String sign = BuildRequestFormUtil.createLinkString(paramMap, key);
		paramMap.put("sign", sign);// MD5签名结果

		Map<String, String> resultMap = HttpUtil.doPostXml(paramMap, XingYeConfig.XY_PAY_URL);
		if (!ObjectUtil.isBlank(resultMap)) {
			logger.info("请求结果：" + resultMap.toString());
			if (resultMap.containsKey("sign")) {
				if (!SignUtils.checkParam(resultMap, key)) {
					logger.info("验证签名不通过");
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}
				RefundResultVO refundResultVO = new RefundResultVO();
				refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
				refundResultVO.setResultMsg("退款失败");

				// 以下字段在 status 和 result_code 都为 0的时候有返回
				if (resultMap.get("status").equals("0") && resultMap.get("result_code").equals("0")) {
					String refundCount = resultMap.get("refund_count");// 退款笔数
					if (ObjectUtil.isBlank(refundCount)) {
						logger.info("查询【" + payQueryParamVO.getTransCode() + "】记录失败，退款笔数" + refundCount + "错误：");
						return ResultBO.err();
					}

					Double totalRefundAmount = 0d;
					Integer refundCountInt = Integer.parseInt(refundCount);
					for (int i = 0; i < refundCountInt; i++) {
						String orderRefundNo = resultMap.get("out_refund_no_" + i);
						String refundId = resultMap.get("refund_id_" + i);
						String refundChannel = resultMap.get("refund_channel_" + i);
						String refundFee = resultMap.get("refund_fee_" + i);
						String couponRefund_fee = resultMap.get("coupon_refund_fee_" + i);
						String refundStatus = resultMap.get("refund_status_" + i);
						logger.info("【" + payQueryParamVO.getTransCode() + "】第" + (i + 1) + "条退款记录：orderRefundNo=" + orderRefundNo + "，refundChannel=" + refundChannel + "，refundId=" + refundId + "，refundFee=" + refundFee + "，couponRefund_fee="
								+ couponRefund_fee + "，refundStatus=" + refundStatus);
						Double refundFeeDou = Double.valueOf(GetWeChatUtil.changeF2Y(refundFee));
						totalRefundAmount = MathUtil.add(totalRefundAmount, refundFeeDou);
					}

					refundResultVO.setOrderCode(resultMap.get("out_trade_no"));// 商户订单号
					refundResultVO.setTransactionId(resultMap.get("transaction_id"));// 平台订单号
					refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
					refundResultVO.setResultMsg("退款成功");
					refundResultVO.setRefundAmount(String.valueOf(totalRefundAmount));// 退款总金额,单位为分,可以做部分退款
					refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_SUCCESS);
					return ResultBO.ok(refundResultVO);
				}
			}
		}
		return ResultBO.err();
	}

	/**
	* 方法说明: SUCCESS—支付成功REFUND—转入退款NOTPAY—未支付CLOSED—已关闭REVERSE—已冲正REVOK—已撤销
	* @auth: xiongJinGang
	* @param status
	* @time: 2017年9月23日 下午4:57:47
	* @return: PayStatusEnum
	*/
	public static PayStatusEnum getPayStatus(String status) {
		PayStatusEnum payStatusEnum = null;
		switch (status) {
		case "SUCCESS":// 支付成功
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "REFUND":// 转入退款
			payStatusEnum = PayStatusEnum.REFUND;
			break;
		case "NOTPAY":// 未支付
		case "CLOSED":// 已关闭
			payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
			break;
		case "REVOK":// 已撤销
			payStatusEnum = PayStatusEnum.USER_CANCELLED_PAYMENT;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;// 未知状态
			break;
		}
		return payStatusEnum;
	}

	public static String[] getMchInfo(String rechargeChannel, String platform) {
		String[] str = new String[4];
		PayTypeThirdEnum payType = PayTypeThirdEnum.getEnum(rechargeChannel);
		switch (payType) {
		case ALIPAY_PAYMENT:// 支付宝支付
			str[0] = HongYueConfig.HONGYUE_ALI_PAY_KEY;
			str[1] = HongYueConfig.HONGYUE_ALI_PAY_PARTNER_CODE;
			if (platform.equals(PayAbstract.PLATFORM_WEB)) {
				str[2] = XingYeTradeTypeEnum.ALIPAY_SCANCODE.getValue();
			} else {
				str[2] = XingYeTradeTypeEnum.ALIPAY_WAP.getValue();
			}
			str[3] = HongYueConfig.HONGYUE_TRADE_URL;// 退款地址
			break;
		case WEIXIN_PAYMENT:// 微信支付
			if (platform.equals(PayAbstract.PLATFORM_WAP)) {
				str[0] = HongYueConfig.HONGYUE_WX_WAP_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_WX_WAP_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.WX_WAP.getValue();
			} else if (platform.equals(PayAbstract.PLATFORM_APP)) {
				str[0] = HongYueConfig.HONGYUE_WX_APP_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_WX_APP_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.WX_APP.getValue();
			} else if (platform.equals(PayAbstract.PLATFORM_JSAPI)) {
				str[0] = HongYueConfig.HONGYUE_WX_JSAPI_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_WX_JSAPI_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.WX_JSAPI.getValue();
			} else {
				str[0] = HongYueConfig.HONGYUE_WX_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_WX_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.WX_SCANCODE.getValue();
			}
			str[3] = HongYueConfig.HONGYUE_TRADE_URL;// 退款地址
			break;
		case QQ_PAYMENT:// QQ钱包支付
			if (platform.equals(PayAbstract.PLATFORM_WEB)) {
				str[0] = HongYueConfig.HONGYUE_QQ_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_QQ_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.QQ_SCANCODE.getValue();
			} else if (platform.equals(PayAbstract.PLATFORM_WAP)) {
				str[0] = HongYueConfig.HONGYUE_QQ_WALLET_PAY_KEY;
				str[1] = HongYueConfig.HONGYUE_QQ_WALLET_PAY_PARTNER_CODE;
				str[2] = XingYeTradeTypeEnum.QQ_WAP.getValue();
			}
			str[3] = HongYueConfig.HONGYUE_TRADE_URL;// 退款地址
			break;
		default:
			break;
		}
		return str;
	}

	/**
	* 方法说明: 检查支付请求返回
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param payType
	* @param key
	* @param resultMap
	* @time: 2017年11月23日 下午5:39:14
	* @return: ResultBO<?>
	*/
	private static ResultBO<?> checkPayResult(PaymentInfoBO paymentInfo, PayTypeThirdEnum payType, String key, Map<String, String> resultMap, String platform) {
		if (!ObjectUtil.isBlank(resultMap)) {
			boolean isCheckCode = true;// 是否验证有没有result_code，微信app支付请求返回没有这个参数
			if (paymentInfo.getPayType().equals(PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && platform.equals(PayAbstract.PLATFORM_APP)) {
				isCheckCode = false;
			}

			// 0表示成功，非0表示失败此字段是通信标识
			if (PayAbstract.TRADE_SUCCESS.equals(resultMap.get("status"))) {
				if ((PayAbstract.TRADE_SUCCESS.equals(resultMap.get("result_code")) && isCheckCode) || !isCheckCode) {
					if (resultMap.containsKey("sign")) {
						if (!SignUtils.checkParam(resultMap, key)) {
							logger.error("订单【" + paymentInfo.getNoOrder() + "】【" + payType.getValue() + "】请求返回签名验证不过");
							return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
						}
						return ResultBO.ok();
					}
				} else {
					String errorMsg = "支付请求失败";
					if (resultMap.containsKey("err_msg")) {
						errorMsg = resultMap.get("err_msg");
					}
					return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMsg);
				}
			}
			String errorMsg = "支付请求失败";
			if (resultMap.containsKey("message")) {
				errorMsg = resultMap.get("message");
			}
			return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMsg);
		} else {
			return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
		}
	}

}