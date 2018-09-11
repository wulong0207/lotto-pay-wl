package com.hhly.paycore.paychannel.yeepay.web;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.wechatpay.config.WeChatPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.yeepay.config.YeepayConfig;
import com.hhly.paycore.paychannel.yeepay.utils.PaymobileUtils;
import com.hhly.paycore.paychannel.yeepay.utils.YeepayStandardUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.PayReturnResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;

/**
 * @author YiJian
 * @version 1.0
 * @Desc: 易宝 WEB支付，支持快捷、扫码、支付宝微信支付
 * @date 2017年5月17日
 * @compay 益彩网络科技有限公司
 */
public class YeepayFastService extends PayAbstract {
	Logger logger = Logger.getLogger(YeepayFastService.class);

	@SuppressWarnings("unchecked")
	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.info("易宝支付开始");
		// 1. 构建支付所需参数
		ResultBO<?> paramsResult = buildParamsMap(paymentInfoBO);
		if (paramsResult.isError()) {
			return ResultBO.err();
		}
		TreeMap<String, Object> params = (TreeMap<String, Object>) paramsResult.getData();
		logger.info("请求参数：" + params.toString());
		// 2. 生成AESkey及encryptkey
		String AESKey = PaymobileUtils.buildAESKey();
		String encryptkey = PaymobileUtils.buildEncyptkey(AESKey);
		// 3. 生成data
		String data = PaymobileUtils.buildData(params, AESKey);
		// 4. 获取商户编号及请求地址，并组装支付链接
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("data", data);
		paramMap.put("encryptkey", encryptkey);
		paramMap.put("merchantaccount", YeepayConfig.MERCHANT_ACCOUNT);

		TreeMap<String, String> responseMap = null;
		try {
			// responseMap = PaymobileUtils.httpPost(YeepayConfig.PAY_API_URL, merchantaccount, data, encryptkey);
			String result = HttpUtil.doPost(YeepayConfig.PAY_API_URL, paramMap);
			logger.info("请求易宝支付返回：" + result);
			if (ObjectUtil.isBlank(result)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			responseMap = JSON.parseObject(result, new TypeReference<TreeMap<String, String>>() {
			});
		} catch (Exception e) {
			logger.error("访问易宝接口异常，请求参数【" + paymentInfoBO.toString() + "】");
			return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
		}

		// 5. 判断请求是否成功
		if (responseMap.containsKey("error_code")) {
			logger.info("访问易宝接口错误，接口返回:" + responseMap);
			return ResultBO.err();
		}

		// 6. 请求成功，则获取data、encryptkey，并将其解密
		String data_response = responseMap.get("data");
		String encryptkey_response = responseMap.get("encryptkey");
		TreeMap<String, String> responseDataMap = PaymobileUtils.decrypt(data_response, encryptkey_response);

		// 7. sign验签
		if (!PaymobileUtils.checkSign(responseDataMap)) {
			logger.info("sign 验签失败！responseMap:" + responseDataMap);
			return ResultBO.err();
		}

		// 8. 判断请求是否成功
		if (responseDataMap.containsKey("error_code")) {
			logger.info("易宝接口参数检验错误：" + responseDataMap);
			return ResultBO.err();
		}
		/**
		 * String merchantaccount		= formatStr(responseDataMap.get("merchantaccount")); //商户编号
		 * String yborderid			= formatStr(responseDataMap.get("yborderid")); //易宝流水号，易宝中唯一
		 * String orderid       	 	= formatStr(responseDataMap.get("orderid")); //商户订单
		 * 1、移动收银台：返回的链接为需要进行支
		 付的易宝支付收银台地址,商户需要将浏览
		 器跳转到此地址,以完成后续支付流程；
		 * 2、PC 扫码返回的是二维码链接。
		 * String payurl           	= formatStr(responseDataMap.get("payurl"));
		 * String imghexstr         	= formatStr(responseDataMap.get("imghexstr"));// 二维码二进制字符串
		 * String sign             	= formatStr(responseDataMap.get("sign"));
		 */

		// String responseJson = JsonUtil.object2Json(responseDataMap);
		String payURL = responseDataMap.get("payurl");
		PayReqResultVO payReqResult = new PayReqResultVO(payURL);
		if (ObjectUtil.isBlank(payURL)) {
			payReqResult.setCode(false);
			payReqResult.setMsg("易宝未返回请求URL");
		}
		// 如果是PC的支付宝和微信支付，返回二维码url
		if (PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfoBO.getPayPlatform())) {
			if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType()) || PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
				payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
			} else {
				payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
			}
		} else {
			payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
		}
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.YEEPAY_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		logger.debug("易宝退款开始");
		String refundAmount = GetWeChatUtil.getMoney(StringUtil.convertObjToStr(refundParam.getRefundAmount()));

		// 使用TreeMap
		TreeMap<String, Object> treeMap = new TreeMap<String, Object>();
		treeMap.put("orderid", refundParam.getRefundCode()); // 由商户输入，必须保持唯一
		treeMap.put("origyborderid", refundParam.getTradeNo()); // 将被退款订单的易宝流水号
		treeMap.put("amount", Integer.parseInt(refundAmount));
		treeMap.put("currency", 156);
		treeMap.put("cause", refundParam.getRefundReason());

		// 第一步 生成AESkey及encryptkey
		String AESKey = PaymobileUtils.buildAESKey();
		String encryptkey = PaymobileUtils.buildEncyptkey(AESKey);
		// 第二步 生成data
		String data = PaymobileUtils.buildData(treeMap, AESKey);
		// 第三步 http请求，退款接口的请求方式为POST
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("data", data);
		paramMap.put("encryptkey", encryptkey);
		paramMap.put("merchantaccount", YeepayConfig.MERCHANT_ACCOUNT);

		TreeMap<String, String> responseMap = null;
		try {
			// TreeMap<String, String> responseMap = PaymobileUtils.httpPost(YeepayConfig.REFUND_API_URL, merchantaccount, data, encryptkey);
			String result = HttpUtil.doPost(YeepayConfig.REFUND_API_URL, paramMap);
			logger.info("请求易宝退款返回：" + result);
			if (ObjectUtil.isBlank(result)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			responseMap = JSON.parseObject(result, new TypeReference<TreeMap<String, String>>() {
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 第四步 判断请求是否成功
		if (responseMap.containsKey("error_code")) {
			return ResultBO.err();
		}

		// 第五步 请求成功，则获取data、encryptkey，并将其解密
		String data_response = responseMap.get("data");
		String encryptkey_response = responseMap.get("encryptkey");
		TreeMap<String, String> responseDataMap = PaymobileUtils.decrypt(data_response, encryptkey_response);

		logger.debug("请求返回的明文参数：" + responseDataMap);

		// 第六步 sign验签
		if (!PaymobileUtils.checkSign(responseDataMap)) {
			logger.info("sign 验签失败！responseMap:" + responseDataMap);
			return ResultBO.err();
		}

		// 第七步 判断请求是否成功
		if (responseDataMap.containsKey("error_code")) {
			return ResultBO.err();
		}

		// String merchantaccount = responseDataMap.get("merchantaccount"); //商户编号
		String orderid = responseDataMap.get("orderid"); // 退款编号
		String yborderid = responseDataMap.get("yborderid"); // 易宝退款流水号
		String origyborderid = responseDataMap.get("origyborderid"); // 易宝交易订单号
		String amount = responseDataMap.get("amount"); // 已退金额
		// String fee = responseDataMap.get("fee"); // 退款手续费
		// String currency = responseDataMap.get("currency");// 交易币种
		String timestamp = responseDataMap.get("timestamp"); // 时间戳
		// String remain = responseDataMap.get("remain");//单位：分；remain = 订单金额– 退款金额

		RefundResultVO refundResultVO = new RefundResultVO();
		refundResultVO.setTransactionId(origyborderid);
		refundResultVO.setRefundAmount(amount);
		refundResultVO.setRefundSuccessTime(timestamp);
		refundResultVO.setRefundId(yborderid);
		refundResultVO.setRefundCode(orderid);
		refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_PROCESSING);
		refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());

		return ResultBO.ok(refundResultVO);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("易宝详情查询");
		String transCode = payQueryParamVO.getTransCode();
		try {
			TreeMap<String, Object> treeMap = new TreeMap<String, Object>();
			treeMap.put("orderid", transCode);// 商户订单号

			// 第一步 生成AESkey及encryptkey
			String AESKey = PaymobileUtils.buildAESKey();
			String encryptkey = PaymobileUtils.buildEncyptkey(AESKey);

			// 第二步 生成data
			String data = PaymobileUtils.buildData(treeMap, AESKey);
			Map<String, String> paramMap = new HashMap<String, String>();
			paramMap.put("data", data);
			paramMap.put("encryptkey", encryptkey);
			paramMap.put("merchantaccount", YeepayConfig.MERCHANT_ACCOUNT);

			// 第三步 http请求，订单查询接口的请求方式为GET
			// TreeMap<String, String> responseMap = PaymobileUtils.httpGet(YeepayConfig.QueryOrderApi, YeepayConfig.MERCHANT_ACCOUNT, data, encryptkey);

			String result = HttpUtil.doGet(YeepayConfig.QueryOrderApi, paramMap);
			if (ObjectUtil.isBlank(result)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			TreeMap<String, String> responseMap = JSON.parseObject(result, new TypeReference<TreeMap<String, String>>() {
			});

			logger.debug("请求串：" + YeepayConfig.QueryOrderApi + "?merchantaccount=" + YeepayConfig.MERCHANT_ACCOUNT + "&data=" + URLEncoder.encode(data, "utf-8") + "&encryptkey=" + URLEncoder.encode(encryptkey, "utf-8"));

			logger.debug("获取交易号【" + transCode + "】交易明细返回：" + responseMap.toString());
			// 第四步 判断请求是否成功
			if (responseMap.containsKey("error_code")) {
				logger.info("获取交易号【" + transCode + "】失败，" + responseMap.get("error_code"));
				return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
			}

			// 第五步 请求成功，则获取data、encryptkey，并将其解密
			String data_response = responseMap.get("data");
			String encryptkey_response = responseMap.get("encryptkey");
			TreeMap<String, String> responseDataMap = PaymobileUtils.decrypt(data_response, encryptkey_response);
			logger.info("请求交易号【" + transCode + "】返回的明文参数：" + responseDataMap.toString());

			// 第六步 sign验签
			if (!PaymobileUtils.checkSign(responseDataMap)) {
				logger.info("查询交易【" + transCode + "】详情签名错误，参数：" + responseDataMap.toString());
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}

			// 第七步 判断请求是否成功
			if (responseDataMap.containsKey("error_code")) {
				logger.info("获取交易号【" + transCode + "】失败，" + responseDataMap.get("error_code"));
				return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
			}
			// 第八步 进行业务处理
			PayQueryResultVO payQueryResultVO = new PayQueryResultVO(responseDataMap);
			if (payQueryResultVO.getTradeStatus().equals(PayConstants.PayStatusEnum.OVERDUE_PAYMENT)) {
				logger.info("交易号【" + transCode + "】过期未支付！");
				// 过期，直接返回错误，改成过期状态
				return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
			}
			payQueryResultVO.setArriveTime(YeepayStandardUtil.timeStamp2Date(responseDataMap.get("closetime"), null));// 交易时间
			payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(responseDataMap.get("amount")));// 交易的订单金额，单位为元，两位小数
			payQueryResultVO.setReceiptAmount(GetWeChatUtil.changeF2Y(responseDataMap.get("targetamount")));// 实收金额，单位为元，两位小数。该金额为本笔交易，商户账户能够实际收到的金额
			payQueryResultVO.setPayAmount(GetWeChatUtil.changeF2Y(responseDataMap.get("sourceamount")));// 买家实付金额，单位为元，两位小数。该金额代表该笔交易买家实际支付的金额，不包含商户折扣等金额

			return ResultBO.ok(payQueryResultVO);
		} catch (Exception e) {
			logger.error("获取交易号【" + transCode + "】交易明细异常：", e);
			return ResultBO.err();
		}
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("易宝退款查询");
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		logger.debug("易宝异步通知");
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		String data = map.get("data");
		String encryptkey = map.get("encryptkey");
		TreeMap<String, String> dataMap = PaymobileUtils.decrypt(data, encryptkey);
		logger.info("返回的明文参数：" + dataMap);
		if (!PaymobileUtils.checkSign(dataMap)) {
			logger.info("sign 验签失败！dataMap:" + dataMap);
			return ResultBO.err();
		}
		// String merchantaccount = dataMap.get("merchantaccount"); // 商户编号
		String yborderid = dataMap.get("yborderid"); // 易宝订单编号
		String orderid = dataMap.get("orderid");// 商户订单编号
		// String bankcode = dataMap.get("bankcode");// 支付卡所属银行的编码
		// String bank = dataMap.get("bank");// 支付卡所属银行的名称
		// String lastno = dataMap.get("lastno");// 支付卡卡号后4 位
		// String cardtype = dataMap.get("cardtype");// 支付卡的类型，1 为借记卡，2 为信用卡
		String amount = dataMap.get("amount");// 以「分」为单位的整型
		String status = dataMap.get("status");// 状态
		if (!StringUtil.isBlank(yborderid)) {
			payNotifyResult.setThirdTradeNo(yborderid);
		}
		if (!StringUtil.isBlank(orderid)) {
			payNotifyResult.setOrderCode(orderid);
		}
		if (!StringUtil.isBlank(amount)) {
			payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(amount)));
		}
		if (!StringUtil.isBlank(status) && "1".equals(status)) {
			payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
		}
		payNotifyResult.setResponse("SUCCESS");
		return ResultBO.ok(payNotifyResult);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		logger.debug("易宝同步通知");
		PayReturnResultVO payReturnResultVO = new PayReturnResultVO();
		return ResultBO.ok(payReturnResultVO);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {

		return ResultBO.ok();
	}

	private ResultBO<?> buildParamsMap(PaymentInfoBO paymentInfoBO) {
		TreeMap<String, Object> treeMap = new TreeMap<String, Object>();
		if (!StringUtil.isBlank(paymentInfoBO.getNoOrder())) {
			treeMap.put("orderid", paymentInfoBO.getNoOrder());
		}
		treeMap.put("productcatalog", "8");// 商品类别 8：彩票业务
		if (!StringUtil.isBlank(paymentInfoBO.getNameGoods())) {// 商品名称
			treeMap.put("productname", paymentInfoBO.getNameGoods());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getUserId())) {// 用户标识
			treeMap.put("identityid", paymentInfoBO.getUserId());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getUserreqIp())) {// 用户支付时使用的网络终端IP
			treeMap.put("userip", paymentInfoBO.getUserreqIp());
		}
		treeMap.put("terminalid", "44-45-53-54-00-00");// 终端标识ID
		// treeMap.put("userua", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36");//终端设备UA
		if (!StringUtil.isBlank(paymentInfoBO.getDtOrder())) {// 下单时间
			String nowTime = DateUtil.getNow(DateUtil.DATE_FORMAT_NUM);// 拿系统当前时间做为订单时间，因为易宝会判断订单的下单时间，加上我们传过去的订单有效时间
			treeMap.put("transtime", DateUtil.dateTimeStamp(nowTime, DateUtil.DATE_FORMAT_NUM));
			// treeMap.put("transtime", DateUtil.getSecondTimestamp(paymentInfoBO.getDtOrder(), DateUtil.DATE_FORMAT_NUM));
		}
		if (!StringUtil.isBlank(paymentInfoBO.getMoneyOrder())) {// 金额
			treeMap.put("amount", Integer.parseInt(GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder())));
		}
		treeMap.put("identitytype", 2);
		treeMap.put("terminaltype", 1);
		if (!StringUtil.isBlank(paymentInfoBO.getInfoOrder())) {// 商品描述
			treeMap.put("productdesc", paymentInfoBO.getInfoOrder());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getUrlReturn())) {// 页面回调地址
			treeMap.put("fcallbackurl", paymentInfoBO.getUrlReturn());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getNotifyUrl())) {// 后台回调地址
			treeMap.put("callbackurl", paymentInfoBO.getNotifyUrl());
		}
		treeMap.put("currency", 156);// 默认为人民币
		if (!StringUtil.isBlank(paymentInfoBO.getValidOrder())) {// 订单有效时间
			treeMap.put("orderexpdate", Integer.parseInt(paymentInfoBO.getValidOrder()));
		}
		// treeMap.put("paytypes", "");
		if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			treeMap.put("directpaytype", 2);// 直连代码 0：默认 1：微信支付 2：支付宝支付 3：一键支付
		} else if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			treeMap.put("directpaytype", 1);
			treeMap.put("appId", WeChatPayConfig.appid);
		} else {
			treeMap.put("directpaytype", 3);// 直接跳过收银台，到快捷支付页面
		}
		if (PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfoBO.getPayPlatform())) {
			if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
				treeMap.put("paytool", "2");// 支付工具2扫码支付
			}
			treeMap.put("version", 1); // 收银台版本 0wap 1pc
		} else {
			treeMap.put("appId", WeChatPayConfig.appid);
			treeMap.put("version", 0); // 收银台版本 0wap 1pc
		}

		if (!StringUtil.isBlank(paymentInfoBO.getCardNo())) {// 银行卡号
			treeMap.put("cardno", paymentInfoBO.getCardNo());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getIdNo())) {// 身份证
			treeMap.put("idcardtype", "01");
			treeMap.put("idcard", paymentInfoBO.getIdNo());
		}

		if (!StringUtil.isBlank(paymentInfoBO.getAcctName())) {// 姓名
			treeMap.put("owner", paymentInfoBO.getAcctName());
		}
		return ResultBO.ok(treeMap);
	}
}
