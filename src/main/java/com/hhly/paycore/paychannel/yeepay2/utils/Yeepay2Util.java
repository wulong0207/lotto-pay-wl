package com.hhly.paycore.paychannel.yeepay2.utils;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.yeepay2.config.Yeepay2Config;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;
import com.hhly.utils.BuildRequestFormUtil;
import com.yeepay.g3.sdk.yop.encrypt.CertTypeEnum;
import com.yeepay.g3.sdk.yop.encrypt.DigitalEnvelopeDTO;
import com.yeepay.g3.sdk.yop.utils.DigitalEnvelopeUtils;
import com.yeepay.g3.sdk.yop.utils.InternalConfig;

public class Yeepay2Util {
	private static Logger logger = LoggerFactory.getLogger(Yeepay2Util.class);
	private static final String VERSION = "1.0";

	private static final Map<String, String> BANK_MAP = new HashMap<>();

	static {
		BANK_MAP.put("ICBC", "ICBC_B2C");// 工商银行
		BANK_MAP.put("CMB", "CMBCHINA_B2C");// 招商银行
		BANK_MAP.put("CCB", "CCB_B2C");// 建设银行
		BANK_MAP.put("BOCOM", "BOCO_B2C");// 交通银行
		BANK_MAP.put("CIB", "CIB_B2C");// 兴业银行
		BANK_MAP.put("CMBC", "CMBC_B2C");// 中国民生银行
		BANK_MAP.put("CEB", "CEB_B2C");// 光大银行
		BANK_MAP.put("BOCSH", "BOC_B2C");// 中国银行
		BANK_MAP.put("PAB", "PINGANBANK_B2C");// 平安银行
		BANK_MAP.put("CNCB", "ECITIC_B2C");// 中信银行
		// BANK_MAP.put("", "SDB_B2C");// 深圳发展银行
		BANK_MAP.put("GDB", "GDB_B2C");// 广发银行
		// BANK_MAP.put("", "SHB_B2C");// 上海银行
		// BANK_MAP.put("", "SPDB_B2C");// 上海浦东发展银行
		// BANK_MAP.put("", "HXB_B2C");// 华夏银行「借」
		// BANK_MAP.put("", "BCCB_B2C");// 北京银行「借」
		// BANK_MAP.put("", "ABC_B2C");// 中国农业银行「借」
		// BANK_MAP.put("", "PSBC_B2C");// 中国邮政储蓄银行「借」
		// BANK_MAP.put("", "BJRCB_B2C");// 北京农商银行
		// BANK_MAP.put("", "SRCB_B2C");// 上海农村商业银行");//B2C
		// BANK_MAP.put("", "HZBANK_B2C");// 杭州银行");//B2C
		// BANK_MAP.put("", "NBCB_B2C");// 宁波银行");//B2C
	}

	/**  
	* 方法说明: 统一调用支付接口
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年5月18日 下午3:22:57
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> callYeepay(PaymentInfoBO paymentInfoBO) {
		Map<String, String> params = Yeepay2Util.getToken(paymentInfoBO);
		PayReqResultVO payReqResult = null;
		try {
			// 第一步，调用接口获取token
			Map<String, String> result = YeepayTools.requestYOP(params, Yeepay2Config.NEW_YEEPAY_TRADE_ORDER_URI, YeepayTools.TRADEORDER);
			logger.info("订单" + paymentInfoBO.getNoOrder() + "获取token返回：" + result.toString());
			if (ObjectUtil.isBlank(result)) {
				return ResultBO.err(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
			}
			if (!result.get("code").equals("OPR00000")) {
				return ResultBO.err(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
			}
			// 第二步，调用支付接口
			String url = Yeepay2Util.createOrderByCashier(result.get("token"), paymentInfoBO);
			logger.info("拼装新易宝支付请求URL：" + url);
			if (ObjectUtil.isBlank(url)) {
				return ResultBO.err(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
			}
			payReqResult = new PayReqResultVO(url);
			payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.YEEPAY2_RECHARGE.getKey());
		} catch (IOException e) {
			logger.error("订单" + paymentInfoBO.getNoOrder() + "调用易宝支付异常", e);
			return ResultBO.err(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
		}
		return ResultBO.ok(payReqResult);
	}

	/**  
	* 方法说明: 第一步，构建获取token的请求参数
	* @auth: xiongJinGang
	* @param paymentInfo
	* @time: 2018年5月17日 下午5:36:58
	* @return: Map<String,String> 
	*/
	public static Map<String, String> getToken(PaymentInfoBO paymentInfo) {
		Map<String, String> param = new HashMap<String, String>();
		param.put("parentMerchantNo", Yeepay2Config.NEW_YEEPAY_PARENT_MERCHANT_NO);// 系统商或者平台商商编，如果是单商户，和收单商户商编保持一致
		param.put("merchantNo", Yeepay2Config.NEW_YEEPAY_PARENT_MERCHANT_NO);// 收单商户商编
		param.put("orderId", paymentInfo.getNoOrder());// 商户订单号
		param.put("orderAmount", paymentInfo.getMoneyOrder());// 单位：元，两位小数，最低 0.01
		param.put("timeoutExpress", paymentInfo.getValidOrder());// 单位：分钟，默认 24小时，最小1分钟，1 最大180天
		param.put("redirectUrl", paymentInfo.getUrlReturn());// 支付成功页面回调地址
		param.put("notifyUrl", paymentInfo.getNotifyUrl());// 支付成功服务器回调地址
		param.put("goodsParamExt", createJson("goodsName", paymentInfo.getInfoOrder()));// 商品信息Json格式{“goodsName”: “abc商品名称”,”goodsDesc”: ”商品描述”}
		// param.put("requestDate","" );//请求时间，用于计算订单有效期，格式 yyyy-MM-dd HH:mm:ss，不传默认为易宝接收到请求的时间
		// param.put("paymentParamExt", );//Json支付扩展参数 具体组合请参照附录【下单支付场景和支付扩展参数详细说明】
		// param.put("industryParamExt", );//Json 行业扩展参数 {"bizSource":"业务来源","bizEntity":"经营主体 "}
		// param.put("memo", "");//商户可以自定义自身业务需要使用的字段：如对账时定义该订单应属的会计科目
		// param.put("riskParamExt", "");//风控参数，请参照附录【风控参数】（由分控部门提供）
		// param.put("csUrl", "");//清算成功服务器回调地址
		// param.put("fundProcessType", "");//资金处理类型，可选值：DELAY_SETTLE("延迟结算"),REAL_TIME("实时订单");REAL_TIME_DIVIDE（”实时分账”）REAL_TIME_SPLIT_ACCOUNT_IN("实时拆分入账");
		// param.put("divideDetail", "");//拆分入账/实时分账，分账详情；资金处理类型为REAL_TIME_DIVIDE("实时分账"),REAL_TIME_SPLIT_ACCOUNT_IN("实时拆分入账")时，必填
		// param.put("divideNotifyUrl", "");//实时分账回告商户地址资金处理类型为 REAL_TIME_DIVIDE("实时分账")时，必填
		return param;
	}

	/**  
	* 方法说明: 用易宝收银台支付，返回一个支付链接
	* @auth: xiongJinGang
	* @param token
	* @param paymentInfo
	* @param platform
	* @time: 2018年5月18日 上午10:58:29
	* @return: String 
	*/
	public static String createOrderByCashier(String token, PaymentInfoBO paymentInfo) {
		String url = null;
		try {
			Map<String, String> params = new HashMap<String, String>();
			params.put("merchantNo", Yeepay2Config.NEW_YEEPAY_MERCHANT_NO);// 收款商户编号
			params.put("token", token);//
			params.put("timestamp", DateUtil.getNowTimeStamp() + "");// 以请求的发起时间转换为时间戳，用来控制一次收银台操作的最大时间
			String cardType = "";
			String directpaytype = "";
			// 设置该参数后，直接调用支付工具，不显示易宝移动收银台页面。枚举值：WECHAT： WX支付ALIPAY： ZFB 支付YJZF： 易宝一键支付CFL: 分期支付DBFQ: 担保分期网银直联编码请参考 5.1、5.2部分值可不传，但参数参与排序生成 sign
			// 易宝没有对接支付宝和微信，所以这里不需要
			// if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
			// directpaytype = "ALIPAY";//
			// } else if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
			// directpaytype = "WECHAT";
			// } else {
			directpaytype = "YJZF";// 默认易宝的一键支付，但是如果存在银行编码，就用直联的银行编码
			// 只有在PC端时，才能用银行编码进行直联
			if (PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfo.getPayPlatform()) && !ObjectUtil.isBlank(paymentInfo.getBankSimpleCode()) && BANK_MAP.containsKey(paymentInfo.getBankSimpleCode())) {
				directpaytype = BANK_MAP.get(paymentInfo.getBankSimpleCode());
			}

			// 限制交易的卡类型，当前只能限制一键支付卡类型，网银请联系运营配置，枚举值：DEBIT：借记卡CREDIT：信用卡该项目不传时，不限制支付卡种值可不传，但参数参与排序生成 sign
			if (PayConstants.PayTypeThirdEnum.BANK_DEBIT_CARD_PAYMENT.getKey().equals(paymentInfo.getPayType()) || PayConstants.PayTypeThirdEnum.QUICK_DEBIT_CARD_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
				cardType = "DEBIT";
			}
			if (PayConstants.PayTypeThirdEnum.QUICK_CREDIT_CARD_PAYMENT.getKey().equals(paymentInfo.getPayType()) || PayConstants.PayTypeThirdEnum.BANK_CREDIT_CARD_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
				cardType = "CREDIT";
			}
			// }
			params.put("directPayType", directpaytype);//
			params.put("cardType", cardType);//
			params.put("userNo", paymentInfo.getUserId());//
			params.put("userType", "USER_ID");// IMEI ：IMEIMAC：MAC 地USER_ID ：用户 IDEMAIL ：用户 EmailPHONE：用户手机号ID_CARD ：用户身份证号
			// 格式：{“appId”:”wx9e13bd68a8f1921e”,”openId”:”zml_wechat”,”clientId”:”*****”}
			if (!ObjectUtil.isBlank(paymentInfo.getAppId()) && !ObjectUtil.isBlank(paymentInfo.getOpenId())) {
				String ext = "{\"appId\":\"" + paymentInfo.getAppId() + "\",\"openId\":\"" + paymentInfo.getOpenId() + "\",\"clientId\":\"\"}";
				params.put("ext", ext);// 微信公众号支付实现方式：此 json 串中，d appid 与openid 需传递，在微信平台打开时可调起公众号支付
			}
			url = YeepayTools.getUrl(params);
		} catch (Exception e) {
			logger.error("拼装新易宝支付请求链接异常", e);
		}
		return url;

	}

	public static Map<String, String> createOrderByApi(String token, PaymentInfoBO paymentInfo, String platform) {
		Map<String, String> params = new HashMap<>();
		params.put("token", token);//
		// web支付一般是扫码支付。SCCANPAY（用户扫码支付）MSCANPAY（商户扫码支付）WECHAT_OPENID（公众号支付）ZFB_SHH（支付宝生活号)
		// 1、payTool 为 SCCANPAY 时可选:WECHAT、 ALIPAY、JD 2、payTool 为 MSCANPAY 时可选:WECHAT、 ALIPAY、JD 3、payTool 为 WECHAT_OPENID 时可选: WECHAT 4、payTool 为 ZFB_SHH 时可选: ALIPAY

		if (platform.equals(PayAbstract.PLATFORM_WEB)) {// 扫码支付
			params.put("payTool", "SCCANPAY");//
			if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
				params.put("payType", "WECHAT");//
			} else if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
				params.put("payType", "ALIPAY");//
			} else if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.JD_PAYMENT.getKey())) {
				params.put("payType", "JD");//
			}
		} else if (platform.equals(PayAbstract.PLATFORM_JSAPI)) {// 公众号支付
			params.put("payTool", "WECHAT_OPENID");//
			params.put("payType", "WECHAT");//
			params.put("appId", paymentInfo.getAppId());// 商家公众号ID。公众号支付、支付宝生活号支付时必填
			params.put("openId", paymentInfo.getOpenId());// 用户OPENID。公众号支付、支付宝生活号支付时必填
		} else if (platform.equals(PayAbstract.PLATFORM_APP)) {// 移动端支付宝支付
			if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
				params.put("payTool", "ALIPAY");//
				params.put("appId", paymentInfo.getAppId());// 商家公众号ID。公众号支付、支付宝生活号支付时必填
				params.put("openId", paymentInfo.getOpenId());// 用户OPENID。公众号支付、支付宝生活号支付时必填
			} else {
				logger.info("新易宝支付未匹配上支付方式，platform=" + platform + "，payType=" + paymentInfo.getPayType());
				return null;
			}
		} else {
			logger.info("新易宝支付未匹配上支付方式，platform=" + platform + "，payType=" + paymentInfo.getPayType());
			return null;
		}

		params.put("userNo", paymentInfo.getUserId());//
		params.put("userType", "USER_ID");// IMEI (IMEI )MAC (MAC 地址 )USER_ID (用户 ID )EMAIL (用户 Email )PHONE (用户手机号 )ID_CARD (用户身份证号)
		// params.put("payEmpowerNo", payEmpowerNo);// 授权码。用户被扫时必填
		// params.put("merchantTerminalId", merchantTerminalId);// 。用户被扫时必填
		// params.put("merchantStoreNo", "");// 。用户被扫时必填
		params.put("userIp", paymentInfo.getUserreqIp());// 用户发起支付的IP
		params.put("version", VERSION);// 接口版本。固定1.0
		return params;
	}

	/**  
	* 方法说明: 构建json数据
	* @auth: xiongJinGang
	* @param key
	* @param value
	* @time: 2018年5月17日 下午5:35:17
	* @return: String 
	*/
	private static String createJson(String key, String value) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(key, value);
		return jsonObject.toJSONString();
	}

	/**  
	* 方法说明: 从异步通知中，得到支付结果
	* 收到易宝回调通知需回写大写“SUCCESS”,如没有回写则每 5 分钟通知一次，总共 3 次，3 次后没有拿到回写则停止通知。
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月16日 下午3:01:13
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> analyPayResult(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("新易宝异步通知结果为空");
			return ResultBO.err();
		}
		logger.info("新易宝支付异步返回：" + map);
		if (!map.containsKey("response")) {
			logger.info("新易宝异步通知结果中没有response");
			return ResultBO.err();
		}
		String response = map.get("response");
		DigitalEnvelopeDTO dto = new DigitalEnvelopeDTO();
		dto.setCipherText(response);
		PrivateKey privateKey = InternalConfig.getISVPrivateKey(CertTypeEnum.RSA2048);
		PublicKey publicKey = InternalConfig.getYopPublicKey(CertTypeEnum.RSA2048);

		dto = DigitalEnvelopeUtils.decrypt(dto, privateKey, publicKey);
		logger.info("解密结果:" + dto.getPlainText());
		Map<String, String> jsonMap = JSON.parseObject(dto.getPlainText(), new TypeReference<TreeMap<String, String>>() {
		});

		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();

		String status = jsonMap.get("status");
		// 0表示成功，非0表示失败此字段是通信标识，非交易标识，交易是否成功需要查看 result_code 来判断
		if (StringUtils.isNotBlank(status) && "SUCCESS".equals(status)) {
			payNotifyResult.setOrderCode(jsonMap.get("orderId"));// 商户唯一订单号
			payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			payNotifyResult.setThirdTradeNo(jsonMap.get("uniqueOrderNo"));// 易宝流水号
			payNotifyResult.setOrderAmt(Double.parseDouble(jsonMap.get("payAmount")));// 实付金额
			payNotifyResult.setTradeTime(jsonMap.get("paySuccessDate"));// 支付完成时间 2017-12-12 13:23:45
			payNotifyResult.setResponse("SUCCESS");
			// map.get("bank_type");// 银行类型
			// map.get("bank_billno");//银行订单号，若为手Q支付则为空
			return ResultBO.ok(payNotifyResult);
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 检查结果
	* @auth: xiongJinGang
	* @param result
	* @time: 2018年5月18日 下午5:53:10
	* @return: boolean 
	*/
	private static boolean checkResult(Map<String, String> result) {
		logger.info("新易宝接口返回：" + result);
		boolean flag = false;
		// 结果码，OPR00000表示成功
		if (!ObjectUtil.isBlank(result) && result.containsKey("code") && result.get("code").equals("OPR00000")) {
			flag = true;
		}
		return flag;
	}

	/**  
	* 方法说明: 获取支付结果
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:41:00
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		try {
			Map<String, String> params = new HashMap<>();
			params.put("orderId", payQueryParamVO.getTransCode());
			params.put("uniqueOrderNo", payQueryParamVO.getTradeNo());
			Map<String, String> result = YeepayTools.requestYOP(params, Yeepay2Config.NEW_YEEPAY_ORDER_QUERY_URI, YeepayTools.ORDERQUERY);
			// 结果码，OPR00000表示成功
			if (checkResult(result)) {
				PayStatusEnum payStatusEnum = BuildRequestFormUtil.getPayStatus(result.get("status"));
				PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
				payQueryResultVO.setTotalAmount(result.get("payAmount"));
				payQueryResultVO.setTradeNo(result.get("uniqueOrderNo"));
				payQueryResultVO.setArriveTime(result.get("paySuccessDate"));
				payQueryResultVO.setOrderCode(result.get("orderId"));
				payQueryResultVO.setTradeStatus(payStatusEnum);
				return ResultBO.ok(payQueryResultVO);
			} else {
				return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
			}
		} catch (IOException e) {
			logger.error("获取订单【" + payQueryParamVO.getTransCode() + "】支付结果异常", e);
			return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, "查询支付结果异常");
		}
	}

	/**  
	* 方法说明:退款 
	* @auth: xiongJinGang
	* @param refundParam
	* @time: 2018年5月18日 下午5:58:58
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> orderRefund(RefundParamVO refundParam) {
		Map<String, String> params = new HashMap<>();
		params.put("orderId", refundParam.getTransCode());// 商户订单号
		String uniqueOrderNo = ObjectUtil.isBlank(refundParam.getTradeNo()) ? refundParam.getTransRechargeBO().getThirdTransNum() : refundParam.getTradeNo();
		params.put("uniqueOrderNo", uniqueOrderNo);// 平台交易号
		params.put("refundRequestId", PayAbstract.REFUND_HEADER + refundParam.getTransCode());
		params.put("refundAmount", refundParam.getRefundAmount().toString());
		params.put("description", refundParam.getRefundReason());
		// params.put("memo", memo);
		// params.put("notifyUrl", notifyUrl);
		// params.put("accountDivided", accountDivided);

		try {
			logger.info("订单【" + refundParam.getOrderCode() + "】退款请求参数：" + params.toString());
			Map<String, String> resultMap = YeepayTools.requestYOP(params, Yeepay2Config.NEW_YEEPAY_REFUNDU_RI, YeepayTools.REFUND);
			logger.info("订单【" + refundParam.getOrderCode() + "】退款返回：" + resultMap.toString());

			RefundResultVO refundResultVO = new RefundResultVO();
			refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
			refundResultVO.setResultMsg("退款失败");
			if (checkResult(resultMap)) {
				refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
				refundResultVO.setOrderCode(resultMap.get("orderId"));// 原商户订单号
				refundResultVO.setRefundCode(resultMap.get("refundRequestId"));// 商户退款单号
				refundResultVO.setTransactionId(resultMap.get("uniqueRefundNo"));// 平台订单号
				refundResultVO.setResultMsg("退款成功");
				refundResultVO.setRefundAmount(resultMap.get("refundAmount"));// 退款金额
				refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_PROCESSING);
				return ResultBO.ok(refundResultVO);
			} else {
				String errorMessage = "退款请求失败";
				if (resultMap.containsKey("message")) {
					errorMessage = resultMap.get("message");
				}
				return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
			}
		} catch (IOException e) {
			logger.error("订单【" + refundParam.getTransCode() + "】退款异常", e);
			return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, "退款异常");
		}
	}

	/**  
	* 方法说明: 退款查询
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:48:25
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		Map<String, String> params = new HashMap<>();
		params.put("refundRequestId", PayAbstract.REFUND_HEADER + payQueryParamVO.getTransCode());
		params.put("orderId", payQueryParamVO.getTransCode());
		params.put("uniqueRefundNo", payQueryParamVO.getRefundRequestNo());

		try {
			Map<String, String> result = YeepayTools.requestYOP(params, Yeepay2Config.NEW_YEEPAY_REFUND_QUERY_URI, YeepayTools.REFUNDQUERY);
			logger.info("订单【" + payQueryParamVO.getTransCode() + "】退款结果：" + result.toString());
			if (checkResult(result)) {
				RefundResultVO refundResultVO = new RefundResultVO();

				refundResultVO.setOrderCode(result.get("orderId"));// 商户订单号
				refundResultVO.setTransactionId(result.get("uniqueRefundNo"));// 易宝退款流水号
				refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
				refundResultVO.setResultMsg(result.get("message"));
				refundResultVO.setRefundAmount(result.get("refundAmount"));// 退款总金额,单位为分,可以做部分退款
				refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_SUCCESS);
				return ResultBO.ok(refundResultVO);
			} else {
				String errorMessage = "退款请求失败";
				if (result.containsKey("message")) {
					errorMessage = result.get("message");
				}
				return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
			}
		} catch (IOException e) {
			logger.error("查询订单【" + payQueryParamVO.getTransCode() + "】退款结果异常", e);
			return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, "查询退款结果异常");
		}
	}

}
