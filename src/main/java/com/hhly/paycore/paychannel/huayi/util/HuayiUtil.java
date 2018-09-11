package com.hhly.paycore.paychannel.huayi.util;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;
import com.hhly.utils.BuildRequestFormUtil;
import com.ibm.icu.math.BigDecimal;

/**
 * @desc 华移支付请求
 * @author xiongJinGang
 * @date 2018年3月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HuayiUtil {
	private static Logger logger = Logger.getLogger(HuayiUtil.class);

	public static final String WECHART_SCAN_URL = "scancode/wx";// 微信支付扫码地址
	public static final String ALIPAY_SCAN_URL = "scancode/alipay";// 支付宝扫码地址
	public static final String WECHART_JSAPI_URL = "wxgzh/api";// 微信公众号支付
	public static final String ORDER_REFUND_URL = "order/refundOrder";// 订单退款
	public static final String ORDER_QUERY_URL = "order/orderStatus";// 订单查询接口

	public static final String SUCCESS = "1";// 成功标志
	public static final String FAIL = "0";// 失败标志

	public static final String REFUND_HEAD = "TK";//
	public static final String WECHAT_JSAPI_CODE = "HUAYI_JSAPI";// 华移微信公众号支付
	public static final String HUAYI_WAP_CODE = "HUAYI_WAP";// 华移wap支付

	/**  
	* 方法说明: 构造微信扫码支付参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年3月27日 上午10:44:37
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWxScanCodeParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("merchantNo", HuayiConfig.HUAYI_MERCHANT_NO);// 商户号
		map.put("name", paymentInfoBO.getNameGoods());// 商品名称
		map.put("total", GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder()));// 总金额，整数，单位为分
		map.put("orderNo", paymentInfoBO.getNoOrder());// 唯一订单号，每次请求必须不同，请全部使用数字组合
		map.put("nofityUrl", paymentInfoBO.getNotifyUrl());// 异步地址订单支付异步回调地址。具体参见章节4.2.3。通知订单执行状态，地址不能携带任何参数
		map.put("timestamp", String.valueOf(DateUtil.getNowTimeStamp()));// 请求时间戳，毫秒数

		// 支付方式为微信支付并且为公众号支付
		if (paymentInfoBO.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && paymentInfoBO.getPayPlatform().equals(PayConstants.TakenPlatformEnum.JSAPI.getKey())) {
			map.put("openID", paymentInfoBO.getOpenId());// 微信openID
			map.put("returnUrl", paymentInfoBO.getUrlReturn());// 订单支付同步回调地址
			map.put("orderNo", paymentInfoBO.getNoOrder().substring(1, paymentInfoBO.getNoOrder().length()));
		}

		// map.put("storeId", paymentInfoBO.getNameGoods());// 【选填】渠道号（门店编号），需提交后台审核
		String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(map, false);
		map.put("sign", DigestUtils.md5Hex(needMd5Str + HuayiConfig.HUAYI_SECRET));// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 构建微信H5支付请参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年7月4日 下午5:10:42
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWxH5Param(PaymentInfoBO paymentInfoBO) {
		HashMap<String, String> postParams = new HashMap<String, String>();
		postParams.put("mchOrderId", paymentInfoBO.getNoOrder());// 订单号
		postParams.put("mchId", HuayiConfig.HUAYI_WXH5_MERCHANT_NO);// 商户号
		postParams.put("version", "v2.0");// 版本号
		postParams.put("inputCharset", "UTF-8");
		postParams.put("pageLanguage", "1");// 固定值1，不用更改
		postParams.put("pgUrl", paymentInfoBO.getUrlReturn());// 订单完成后返回的商户页面地址
		postParams.put("bgUrl", paymentInfoBO.getNotifyUrl());// 订单完成后回调商户后台通知地址
		postParams.put("orderAmt", paymentInfoBO.getMoneyOrder());
		String orderTimestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
		; // 订单时间戳
		postParams.put("orderTimestamp", orderTimestamp);// 订单时间戳
		postParams.put("prodName", paymentInfoBO.getNameGoods());// 商品名称
		postParams.put("prodDesc", paymentInfoBO.getNameGoods());// 商品描述
		Integer orderTimeOut = Integer.parseInt(paymentInfoBO.getValidOrder()) * 60;
		postParams.put("orderTimeOut", orderTimeOut.toString());// 单位为秒。 此处表示60分钟。60 * 60 = 3600秒
		postParams.put("payType", "1");// 支付方式。固定值1，不用更改
		postParams.put("service", ServiceType.H5T_GATEWAY_PAY.getType());
		postParams.put("channel", "wx");
		return postParams;
	}

	/**  
	* 方法说明: 解析支付请求返回
	* @auth: xiongJinGang
	* @param json
	* @param paymentInfo
	* @time: 2018年3月27日 上午11:55:41
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> analyResultJson(String json, PaymentInfoBO paymentInfo) {
		JSONObject jsonObject = JSON.parseObject(json);
		String code = jsonObject.getString("code");
		if (SUCCESS.equals(code)) {
			JSONObject result = JSON.parseObject(jsonObject.getString("result"));
			String payInfo = result.getString("codeUrl");
			Short type = PayConstants.PayReqResultEnum.LINK.getKey();
			if (PayConstants.TakenPlatformEnum.JSAPI.getKey().equals(paymentInfo.getPayPlatform())) {
				type = PayConstants.PayReqResultEnum.ENCRYPTION.getKey();
				payInfo = result.getString("formfield");// 唤起原生公众号参数
				// payInfo = result.getString("payInfo");// 使用平台起调微信公众号支付，支付成功或者失败都会回调同步地址returnUrl。需要使用时必须向平台申请配置支付授权目录为:https://pay.cnmobi.cn/pay/wxgzh/
			} else if (!PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfo.getPayPlatform())) {
				type = PayConstants.PayReqResultEnum.URL.getKey();
			}

			PayReqResultVO payReqResult = new PayReqResultVO(payInfo);//
			payReqResult.setType(type);
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.HUAYI_RECHARGE.getKey());
			if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey())) {
				payReqResult.setIsSafari("1");//如果为IOS手机则打开浏览器
			}
			return ResultBO.ok(payReqResult);
		} else {
			if (paymentInfo.isTest()) {// 测试环境，显示第三方返回的信息
				String msg = jsonObject.getString("msg");
				return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, msg);
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
			}
		}
	}

	/**  
	* 方法说明: 解析并验证异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2018年1月3日 上午10:29:30
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("华移支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(map, false);

			String md5Sign = DigestUtils.md5Hex(needMd5Str + HuayiConfig.HUAYI_SECRET);
			if (!map.get("sign").equals(md5Sign)) {
				logger.info("华移支付异步通知，验证签名不通过");
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			// 状态码88：成功 其它为失败
			// 我们的充值编号都是大写I开头，但华移微信公众号支付只能全部数据，所以在调用接口之前将I全部去掉了，这里需要补上
			String orderNo = map.get("orderNo");
			if (!orderNo.startsWith("I")) {
				orderNo = "I" + orderNo;
			}

			payNotifyResult.setOrderCode(orderNo);// 商户唯一订单号
			payNotifyResult.setThirdTradeNo(orderNo);// 平台订单号
			payNotifyResult.setOrderAmt(Double.valueOf(GetWeChatUtil.changeF2Y(map.get("total"))));// 总金额，整数，单位为分
			payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
			payNotifyResult.setResponse("success");// success、error
			if (StringUtils.isNotBlank(map.get("code")) && SUCCESS.equals(map.get("code"))) {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
			return ResultBO.ok(payNotifyResult);
		} else {
			// {channelOrderId=4200000145201807040399993638, mchId=C2018051700191, orderAmt=0.01, gmtCreate=2018-07-04 17:20:59, mchOrderId=I18070417205512300001, gmtPayment=2018-07-04 17:21:32, tradeStatus=TRADE_SUCCESS,
			// prodName=2Ncai-I18070417205512300001, signType=RSA, payChannel=wx, inOrderId=SP20180704172059686QnZyoCXSRY,
			// signMsg=0B48F367F06822D029DF4174B481AEC14342EDA1A8096FEB4E495717935ECFCCE14EA04EED0C923869EAC675C7E5716AD982E8F71DB2E5B20B235531414EC1F07B81AB4A9B4C24589308E8168675F4BE7B87BFA4D6A606D9E1DBC0D7837E4F605C8AB86A1709D3E25EF44C8928622EB27F9287D0EDCD4F032E487D49C00EC0B5,
			// prodDesc=2Ncai-I18070417205512300001}
			// 新的微信支付返回
			if (map.containsKey("payChannel") && map.containsKey("signType") && map.containsKey("signMsg")) {
				try {
					String oldSignMsg = map.get("signMsg");
					String oldSignType = map.get("signType");
					// 移除signType和signMsg，这两个参数不参与签名
					map.remove("signType");
					map.remove("signMsg");

					// 对移除signType和signMsg后的剩余参数进行排序、组装，获得待签名字符串。
					String signMsg = SevenPayHelper.dealSignParam(map);
					logger.info("待签名串：" + signMsg);
					byte[] data = signMsg.getBytes("UTF-8");

					// 七分钱使用RSA方式进行签名
					if ("RSA".equals(oldSignType)) {

						// 校验签名
						boolean result = RSAUtils.verify(data, RSAUtils.getSevenPayPubKey(), oldSignMsg);
						if (!result) {
							logger.error("华移支付签名验证不通过");
							return ResultBO.err();
						} else {
							// 校验通过后，处理订单支付结果返回信息
							payNotifyResult.setOrderCode(map.get("mchOrderId"));// 商户唯一订单号
							payNotifyResult.setThirdTradeNo(map.get("inOrderId"));// 平台订单号
							payNotifyResult.setOrderAmt(new BigDecimal(map.get("orderAmt")).doubleValue());// 总金额，整数，单位为分
							payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒

							if (map.get("tradeStatus").equals("TRADE_SUCCESS") || map.get("tradeStatus").equals("TRADE_FINISHED")) {
								payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
								payNotifyResult.setResponse("success");// success、failure
							} else {
								payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
								payNotifyResult.setResponse("failure");// success、failure
							}
							return ResultBO.ok(payNotifyResult);
						}
					} else {
						logger.error("华移支付签名方式错误，当前版本只支持RSA签名");
					}
				} catch (Exception e) {
					logger.error("解析华移支付异步通知异常", e);
				}
			}
		}
		logger.error("华移支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	/**  
	* 方法说明: 订单退款
	* @auth: xiongJinGang
	* @param refundParam
	* @time: 2018年3月27日 下午4:35:12
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> orderRefund(RefundParamVO refundParam) {

		TransRechargeBO transRechargeBO = refundParam.getTransRechargeBO();
		// 如果是微信wap支付，不是公众号支付
		if (transRechargeBO.getPayType().equals(PayConstants.PayTypeEnum.WECHART_PAYMENT.getKey()) && transRechargeBO.getChannelCode().equals(HUAYI_WAP_CODE)) {
			try {
				HashMap<String, String> postParams = new HashMap<String, String>();
				postParams.put("mchOrderId", refundParam.getTransCode());
				postParams.put("mchId", HuayiConfig.HUAYI_WXH5_MERCHANT_NO);
				postParams.put("version", "v2.0");
				postParams.put("mchRefundId", REFUND_HEAD + refundParam.getTransCode());
				postParams.put("refundAmt", new BigDecimal(refundParam.getOrderAmount()).toString());
				postParams.put("refundDesc", refundParam.getRefundReason());
				postParams.put("service", ServiceType.MCH_REFUND.getType());
				postParams.put("signType", "RSA");

				// 创建七分钱订单请求
				JSONObject json = SevenPayApply.buildRequest(postParams, "post", HuayiConfig.HUAYI_WXH5_REQUEST_URL);
				// {"returnCode":"SUCCESS","returnMsg":"退款成功","mchId":"C2018051700191","inRefundId":"RF20180704200656191DdrKTZsiFA","mchRefundId":"TKI18070419061012300001","signType":"RSA","signMsg":"A077A987DE4DC002DFDF047F2974E070E6C15616862D25BB33436FA754843CCD9299629A14CED7105A8F405869C0DDB399A4D6575F0D0432EBD35A2032F8DB1D82FEF44400E8312E6C8638D3A817C6089DA83114AE65A332EB22C2BE2CB313B114D3081A141A315648878C4D06FDBF0BC1895A9825403F7BBD50C5FCFA35167A","mchOrderId":"I18070419061012300001","refundAmt":"0.01000000000000000020816681711721685132943093776702880859375"}
				logger.info("订单【" + refundParam.getTransCode() + "】退款返回：" + json);
				ResultBO<?> resultBO = checkResult(json);
				if (resultBO.isError()) {
					return resultBO;
				}
				Map<String, String> map = (Map<String, String>) resultBO.getData();

				RefundResultVO refundResultVO = new RefundResultVO();
				String returnCode = map.get("returnCode");
				if (returnCode.equals("SUCCESS")) {
					refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
					refundResultVO.setResultMsg(map.get("returnMsg"));
					refundResultVO.setOrderCode(refundParam.getTransCode());// 商户订单号
					refundResultVO.setRefundAmount(String.valueOf(map.get("refundAmt")));// 退款总金额
					return ResultBO.ok(refundResultVO);
				} else {
					return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, map.get("returnMsg"));
				}
			} catch (Exception e) {
				logger.error("华移订单【" + refundParam.getTransCode() + "】退款异常", e);
			}
		} else {
			// 检查是否为公众号支付
			boolean isJsapiPay = checkPayType(refundParam.getTransRechargeBO());
			String transCode = refundParam.getTransCode();
			if (isJsapiPay) {
				transCode = transCode.substring(1, transCode.length());
			}
			// 封装请求参数
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("orderNo", transCode);// 商户订单号
			paramMap.put("merchantNo", HuayiConfig.HUAYI_MERCHANT_NO);// 商户号
			paramMap.put("refundFee", GetWeChatUtil.getMoney(String.valueOf(refundParam.getOrderAmount())));// 退款金额必须小于等于订单总金额，可以分多批次退款
			paramMap.put("refundReson", refundParam.getRefundReason());// 退款原因
			paramMap.put("timestamp", String.valueOf(DateUtil.getNowTimeStamp()));// 请求时间戳，毫秒数

			String sign = BuildRequestFormUtil.sortMapToStrConnSymbol(paramMap, false);
			paramMap.put("sign", DigestUtils.md5Hex(sign + HuayiConfig.HUAYI_SECRET));// MD5签名结果

			try {
				logger.debug("退款请求参数：" + paramMap.toString());
				String result = HttpUtil.doPost(HuayiConfig.HUAYI_REQUEST_URL + ORDER_REFUND_URL, paramMap);
				if (!ObjectUtil.isBlank(result)) {
					logger.info("退款返回结果：" + result);
					JSONObject jsonObject = JSONObject.parseObject(result);
					String code = jsonObject.getString("code");
					RefundResultVO refundResultVO = new RefundResultVO();
					if (SUCCESS.equals(code)) {
						refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
						refundResultVO.setResultMsg(jsonObject.getString("msg"));
						refundResultVO.setOrderCode(refundParam.getTransCode());// 商户订单号
						refundResultVO.setRefundAmount(String.valueOf(refundParam.getOrderAmount()));// 退款总金额
						return ResultBO.ok(refundResultVO);
					} else {
						return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, jsonObject.getString("msg"));
					}
				}
			} catch (Exception e) {
				logger.error("华移退款请求异常", e);
			}
		}
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {

		TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
		// 如果是微信wap支付，不是公众号支付
		if (transRechargeBO.getPayType().equals(PayConstants.PayTypeEnum.WECHART_PAYMENT.getKey()) && transRechargeBO.getChannelCode().equals(HUAYI_WAP_CODE)) {
			HashMap<String, String> postParams = new HashMap<String, String>();
			postParams.put("mchOrderId", payQueryParamVO.getTransCode());
			postParams.put("mchId", HuayiConfig.HUAYI_WXH5_MERCHANT_NO);
			postParams.put("version", "v2.0");
			postParams.put("service", ServiceType.QUERY_ORDER_STATUS.getType());

			// 创建七分钱订单请求
			try {
				JSONObject json = SevenPayApply.buildRequest(postParams, "post", HuayiConfig.HUAYI_WXH5_REQUEST_URL);
				// {"returnCode":"FAILURE","returnMsg":"订单不存在","signType":"RSA","signMsg":"6E923C5B2A6E14F35EE0BA5DE14B9C2EE6BC37905FAC7522C6FEB249691DF8F585A5F37E630D22EB470C79863668696A19B0565F834AFB6FC3B5ADE2305515454F1DEEE34E82AD3552DB080D2F6C3B84E213317EDE114EBB55609BCF6E335C1FB9CCD2AD56CA149BF00944303FAFFB5618FFB010E2CAD616012B8CDA41426513"}
				logger.info("获取订单【" + payQueryParamVO.getTransCode() + "】支付结果返回：" + json.toString());
				ResultBO<?> resultBO = checkResult(json);
				if (resultBO.isError()) {
					return resultBO;
				}
				Map<String, String> map = (Map<String, String>) resultBO.getData();
				String payStatus = map.get("tradeStatus");
				PayStatusEnum payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
				if (payStatus.equals("TRADE_SUCCESS") || payStatus.equals("TRADE_FINISHED")) {
					// 交易成功，且可对该交易做操作，如：多级分润、退款等。交易成功且结束，即不可再做任何操作。
					payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
				} else if (payStatus.equals("BUYER_PAYING") || payStatus.equals("WAIT_BUYER_PAY")) {
					// 买家正在支付。交易创建，等待买家付款。
					payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
				} else if (payStatus.equals("TRADE_CLOSED")) {
					// 在指定时间段内未支付时关闭的交易； 在交易完成全额退款成功时关闭的交易。
					payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
				}

				PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
				payQueryResultVO.setTotalAmount(map.get("orderAmt"));
				payQueryResultVO.setTradeNo(payQueryParamVO.getTransCode());
				payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
				payQueryResultVO.setOrderCode(payQueryParamVO.getTransCode());
				payQueryResultVO.setTradeStatus(payStatusEnum);
				return ResultBO.ok(payQueryResultVO);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return queryCommonResult(payQueryParamVO);
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 验证返回结果
	* @auth: xiongJinGang
	* @param json
	* @throws Exception
	* @throws UnsupportedEncodingException
	* @time: 2018年7月4日 下午7:57:43
	* @return: void 
	*/
	@SuppressWarnings("unchecked")
	private static ResultBO<?> checkResult(JSONObject json) throws Exception, UnsupportedEncodingException {
		// 移除signType和signMsg，这两个参数不参与签名
		Map<String, String> map = JSON.parseObject(json.toJSONString(), Map.class);
		String oldSignType = map.remove("signType");
		String oldSignMsg = map.remove("signMsg");
		map.remove("signType");
		map.remove("signMsg");

		// 对移除signType和signMsg后的剩余参数进行排序、组装，获得待签名字符串。
		String signMsg = SevenPayHelper.dealSignParam(map);
		logger.info("待签名串：" + signMsg);
		byte[] data = signMsg.getBytes("UTF-8");
		// 七分钱使用RSA方式进行签名
		if ("RSA".equals(oldSignType)) {
			// 校验签名
			boolean result = RSAUtils.verify(data, RSAUtils.getSevenPayPubKey(), oldSignMsg);
			if (!result) {
				logger.error("华移支付签名验证不通过");
				return ResultBO.err();
			} else {
				if (map.containsKey("tradeStatus")) {
					return ResultBO.ok(map);
				}
			}
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 普通查询支付结果
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @param isJsapiPay
	* @time: 2018年7月4日 下午6:29:57
	* @return: void 
	*/
	public static ResultBO<?> queryCommonResult(PayQueryParamVO payQueryParamVO) {
		try {
			String transCode = payQueryParamVO.getTransCode();
			// 检查是否为公众号支付
			boolean isJsapiPay = checkPayType(payQueryParamVO.getTransRechargeBO());
			if (isJsapiPay) {
				transCode = transCode.substring(1, transCode.length());
			}
			// 封装请求参数
			SortedMap<String, String> paramMap = new TreeMap<String, String>();
			paramMap.put("merchantNo", HuayiConfig.HUAYI_MERCHANT_NO);// 商户号
			paramMap.put("orderNo", transCode);// 订单号
			paramMap.put("timestamp", String.valueOf(DateUtil.getNowTimeStamp()));// 请求时间戳，毫秒数

			String sign = BuildRequestFormUtil.sortMapToStrConnSymbol(paramMap, false);
			paramMap.put("sign", DigestUtils.md5Hex(sign + HuayiConfig.HUAYI_SECRET));// MD5签名结果

			String result = HttpUtil.doPost(HuayiConfig.HUAYI_REQUEST_URL + ORDER_QUERY_URL, paramMap);
			if (!ObjectUtil.isBlank(result)) {
				logger.info("查询华移支付结果返回：" + result);
				JSONObject jsonObject = JSONObject.parseObject(result);
				String code = jsonObject.getString("code");
				if (SUCCESS.equals(code)) {
					JSONObject detail = JSONObject.parseObject(jsonObject.getString("result"));
					Map<String, String> resultMap = new HashMap<String, String>();
					resultMap.put("timestamp", detail.getString("timestamp"));
					resultMap.put("total", detail.getString("total"));
					resultMap.put("orderNo", detail.getString("orderNo"));
					String orderStatus = detail.getString("code");
					resultMap.put("code", orderStatus);
					resultMap.put("merchantNo", detail.getString("merchantNo"));

					String countSign = DigestUtils.md5Hex(BuildRequestFormUtil.sortMapToStrConnSymbol(resultMap, false) + HuayiConfig.HUAYI_SECRET);
					String returnSign = detail.getString("sign");
					if (!countSign.equals(returnSign)) {
						logger.info("验证签名不通过");
						return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					}
					PayStatusEnum payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
					if ("0".equals(orderStatus)) {// 等待支付
						payStatusEnum = PayConstants.PayStatusEnum.WAITTING_PAYMENT;
					} else if ("1".equals(orderStatus)) {// 支付成功
						payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
					} else if ("2".equals(orderStatus)) {// 支付失败
						payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_FAILURE;
					} else if ("3".equals(orderStatus)) {// 订单已撤销
						payStatusEnum = PayConstants.PayStatusEnum.OVERDUE_PAYMENT;
					} else if ("4".equals(orderStatus)) {// 订单已退款
						payStatusEnum = PayConstants.PayStatusEnum.REFUND;
					}
					PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
					payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("total")));
					payQueryResultVO.setTradeNo(resultMap.get("orderNo"));
					payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
					payQueryResultVO.setOrderCode(payQueryParamVO.getTransCode());
					payQueryResultVO.setTradeStatus(payStatusEnum);
					return ResultBO.ok(payQueryResultVO);
				} else {
					return ResultBO.err();
				}
			}
		} catch (Exception e) {
			logger.error("查询华移支付结果异常", e);
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 检验支付方式是否为公众号支付
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2018年3月28日 下午12:09:57
	* @return: void 
	*/
	private static boolean checkPayType(TransRechargeBO transRechargeBO) {
		boolean flag = Boolean.FALSE;
		// 如果是华移公众号支付，要将交易编号前的I去掉，他们只允许纯数字的交易编号
		if (!ObjectUtil.isBlank(transRechargeBO.getChannelCode()) && transRechargeBO.getChannelCode().equals(WECHAT_JSAPI_CODE)) {
			flag = Boolean.TRUE;
		}
		return flag;
	}

}
