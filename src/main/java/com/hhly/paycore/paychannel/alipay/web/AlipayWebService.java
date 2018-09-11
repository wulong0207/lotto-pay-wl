package com.hhly.paycore.paychannel.alipay.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayDataDataserviceBillDownloadurlQueryRequest;
import com.alipay.api.request.AlipayTradeFastpayRefundQueryRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayDataDataserviceBillDownloadurlQueryResponse;
import com.alipay.api.response.AlipayTradeFastpayRefundQueryResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.alipay.config.AlipayConfig;
import com.hhly.paycore.paychannel.alipay.web.util.AlipaySubmit;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.RefundStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.UserTransStatusEnum;
import com.hhly.skeleton.base.constants.PayResultConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.AlipayRefundInfoVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.PayReturnResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;

/**
 * @Desc: 支付宝 WEB支付
 * @author YiJian
 * @date 2017年3月15日
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
public class AlipayWebService extends PayAbstract {
	Logger logger = Logger.getLogger(AlipayWebService.class);

	private static AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do", AlipayConfig.pc_app_id, AlipayConfig.seller_private_key, "json", AlipayConfig.input_charset,
			AlipayConfig.alipay_public_key, "RSA");

	/*****************************
	 * 支付宝方定义参数(WEB端)
	 *****************************************/
	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.debug("支付宝支付开始");
		// 构建支付所需参数
		Map<String, String> params = null;
		try {
			params = buildMapParam(paymentInfoBO);
			String sHtmlText = AlipaySubmit.buildRequest(params, "get", "确认");
			PayReqResultVO payReqResult = new PayReqResultVO(sHtmlText);
			payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.ALIPAY_RECHARGE.getKey());
			return ResultBO.ok(payReqResult);
		} catch (Exception e) {
			logger.error("拼装请求参数异常", e);
			return ResultBO.err();
		} finally {
			if (ObjectUtil.isBlank(params)) {
				params.clear();
			}
		}
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		logger.debug("支付宝退款开始");
		AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
		AlipayRefundInfoVO alipayRefundInfoVO = new AlipayRefundInfoVO();
		alipayRefundInfoVO.setOrderCode(refundParam.getOrderCode());
		alipayRefundInfoVO.setRefundAmount(String.valueOf(refundParam.getRefundAmount()));
		alipayRefundInfoVO.setRefundReason(refundParam.getRefundReason());
		alipayRefundInfoVO.setTradeNo(refundParam.getTradeNo());
		alipayRefundInfoVO.setOutRequestNo(refundParam.getRefundCode());
		request.setBizContent(JSON.toJSONString(alipayRefundInfoVO));
		request.setNotifyUrl(refundParam.getNotifyUrl());// 退款通知地址

		logger.debug("支付宝退款请求参数：" + JSON.toJSONString(request));
		try {
			AlipayTradeRefundResponse response = alipayClient.execute(request);
			RefundResultVO refundResultVO = new RefundResultVO();
			if (response.isSuccess()) {
				refundResultVO.setOrderCode(response.getOutTradeNo());
				refundResultVO.setRefundAmount(response.getRefundFee());
				refundResultVO.setRefundCode(response.getOutTradeNo());
				refundResultVO.setRefundRecvAccout(response.getBuyerLogonId());
				refundResultVO.setRefundStatusEnum(RefundStatusEnum.REFUND_PROCESSING);
				refundResultVO.setResultCode(UserTransStatusEnum.TRADE_SUCCESS.getKey());
				refundResultVO.setResultMsg(response.getMsg());
				refundResultVO.setTransactionId(response.getTradeNo());
				return ResultBO.ok(refundResultVO);
			} else {
				logger.error("支付宝退款申请失败：" + response.getBody());
				/*String msg = ObjectUtil.isBlank(response.getSubMsg()) ? response.getMsg() : response.getSubMsg();
				refundResultVO.setResultCode(UserTransStatusEnum.TRADE_FAIL.getKey());
				refundResultVO.setResultMsg(msg);
				refundResultVO.setOrderCode(refundParam.getOrderCode());
				refundResultVO.setRefundCode(refundParam.getRefundCode());
				refundResultVO.setRefundReason(refundParam.getRefundReason());*/
				return ResultBO.err(MessageCodeConstants.REFUND_REQUEST_FAIL_ERROR_SERVICE);
			}
		} catch (Exception e) {
			logger.error("请求支付宝退款接口异常：" + JSON.toJSONString(request), e);
			return ResultBO.err();
		}
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("支付宝详情查询");
		PayQueryResultVO payQueryVO = null;
		String orderNo = payQueryParamVO.getTransCode();
		String tradeNo = payQueryParamVO.getTradeNo();
		AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
		JSONObject json = new JSONObject();
		if (!ObjectUtil.isBlank(orderNo)) {
			json.put("out_trade_no", orderNo);
		}
		if (!ObjectUtil.isBlank(tradeNo)) {
			json.put("trade_no", tradeNo);
		}

		request.setBizContent(json.toJSONString());
		try {
			AlipayTradeQueryResponse response = alipayClient.execute(request);
			if (!ObjectUtil.isBlank(response)) {
				if (response.isSuccess()) {
					payQueryVO = new PayQueryResultVO();
					payQueryVO.setArriveTime(DateUtil.convertDateToStr(response.getSendPayDate()));
					payQueryVO.setBuyerAccount(response.getBuyerLogonId());
					payQueryVO.setOrderCode(response.getOutTradeNo());
					payQueryVO.setPayAmount(response.getBuyerPayAmount());
					payQueryVO.setReceiptAmount(response.getReceiptAmount());
					payQueryVO.setTotalAmount(response.getTotalAmount());
					payQueryVO.setTradeNo(response.getTradeNo());
					// 交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
					payQueryVO.setTradeStatus(getPayStatus(response.getTradeStatus()));
					payQueryVO.setReturnMsg(response.getMsg());
				} else {
					logger.error("查询orderNo：" + orderNo + ",tradeNo：" + tradeNo + "失败，" + response.getBody());
					return ResultBO.err(MessageCodeConstants.QUERY_TRANSACTION_DETAILS_ERROR_SERVICE);
				}
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			logger.debug("查询支付返回的body：" + response.getBody());
		} catch (AlipayApiException e) {
			logger.error("请求支付宝查看详情接口异常", e);
			return ResultBO.err();
		}
		return ResultBO.ok(payQueryVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("支付宝退款查询");
		RefundResultVO refundResultVO = null;
		AlipayTradeFastpayRefundQueryRequest request = new AlipayTradeFastpayRefundQueryRequest();
		JSONObject json = new JSONObject();
		if (!ObjectUtil.isBlank(payQueryParamVO.getTransCode())) {
			json.put("out_trade_no", payQueryParamVO.getTransCode());
		}
		if (!ObjectUtil.isBlank(payQueryParamVO.getTradeNo())) {
			json.put("trade_no", payQueryParamVO.getTradeNo());
		}
		json.put("out_request_no", payQueryParamVO.getRefundRequestNo());
		request.setBizContent(json.toJSONString());
		AlipayTradeFastpayRefundQueryResponse response = null;
		try {
			response = alipayClient.execute(request);
		} catch (AlipayApiException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (response.isSuccess()) {
			refundResultVO = new RefundResultVO();
			refundResultVO.setOrderCode(response.getOutTradeNo());
			refundResultVO.setTransactionId(response.getTradeNo());
			refundResultVO.setRefundCode(response.getOutTradeNo());
			refundResultVO.setRefundReason(response.getRefundReason());
			refundResultVO.setRefundAmount(response.getRefundAmount());
			refundResultVO.setOrderAmount(response.getTotalAmount());
			refundResultVO.setRefundStatusEnum(RefundStatusEnum.REFUND_SUCCESS);
			refundResultVO.setResultCode(UserTransStatusEnum.TRADE_SUCCESS.getKey());
			refundResultVO.setResultMsg(response.getMsg());
		} else {
			logger.error("支付宝退款申请失败：" + response.getBody());
			return ResultBO.err(MessageCodeConstants.REFUND_REQUEST_FAIL_ERROR_SERVICE);
		}
		// 支付宝扫码支付暂未提供退款查询
		return ResultBO.ok(refundResultVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		logger.debug("支付宝异步通知");
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		// 1.验签(2017-05-05 支付回调无法配置，验签不过，先不进行验签)
		/*if (!AlipayNotify.verify(map)) {
			payNotifyResult.setResponse("fail");
			payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			return ResultBO.ok(payNotifyResult);
		}*/
		// 2.封装返回对象
		if (map.containsKey("trade_no")) { // 支付宝交易号
			payNotifyResult.setThirdTradeNo(map.get("trade_no"));
		}
		if (map.containsKey("out_trade_no")) {// 商户订单号
			payNotifyResult.setOrderCode(map.get("out_trade_no"));
		}
		if (map.containsKey("total_fee")) {// 支付金额
			String totalFee = map.get("total_fee");
			if (!ObjectUtil.isBlank(totalFee)) {
				payNotifyResult.setOrderAmt(Double.valueOf(totalFee));
			}
		}
		if (map.containsKey("trade_status")) {// 支付状态
			if (PayResultConstants.ALIPAY_RESULT_TRADE_SUCCESS.equals(map.get("trade_status")) || PayResultConstants.ALIPAY_RESULT_TRADE_FINISHED.equals(map.get("trade_status"))) {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else if (PayResultConstants.ALIPAY_RESULT_WAIT_BUYER_PAY.equals(map.get("trade_status"))) {
				payNotifyResult.setStatus(PayStatusEnum.WAITTING_PAYMENT);
			} else if (PayResultConstants.ALIPAY_RESULT_TRADE_CLOSED.equals(map.get("trade_status"))) {
				payNotifyResult.setStatus(PayStatusEnum.USER_CANCELLED_PAYMENT);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
		}
		if (map.containsKey("gmt_payment")) {// 付款时间
			payNotifyResult.setTradeTime(map.get("gmt_payment"));
		}
		if (map.containsKey("extra_common_param")) {// 附加参数（订单详情）
			payNotifyResult.setAttachData(map.get("extra_common_param"));
		}
		payNotifyResult.setResponse("success");
		return ResultBO.ok(payNotifyResult);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		logger.debug("支付宝同步通知");
		PayReturnResultVO payReturnResultVO = new PayReturnResultVO();
		return ResultBO.ok(payReturnResultVO);
	}

	/**
	 * @Title: buildMapParam
	 * @Description: 把请求参数打包成数组
	 * @param paymentInfoBO
	 * @return Map 返回类型
	 * @throws IOException 
	 * @throws DocumentException 
	 * @throws MalformedURLException 
	 */
	private Map<String, String> buildMapParam(PaymentInfoBO paymentInfoBO) throws Exception {
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("service", "create_direct_pay_by_user");
		sParaTemp.put("partner", AlipayConfig.partner);
		sParaTemp.put("_input_charset", AlipayConfig.input_charset);
		sParaTemp.put("payment_type", AlipayConfig.payment_type);
		sParaTemp.put("notify_url", paymentInfoBO.getNotifyUrl());
		sParaTemp.put("return_url", paymentInfoBO.getUrlReturn());
		sParaTemp.put("seller_email", AlipayConfig.seller_email);
		sParaTemp.put("out_trade_no", paymentInfoBO.getNoOrder());
		if (ObjectUtil.isBlank(paymentInfoBO.getNameGoods())) {
			paymentInfoBO.setNameGoods("购彩");
		}
		sParaTemp.put("subject", paymentInfoBO.getNameGoods());// 商品名称
		// sParaTemp.put("subject", "subject");// 商品名称
		sParaTemp.put("total_fee", paymentInfoBO.getMoneyOrder());
		// sParaTemp.put("body", paymentInfoBO.getAttach());//商品描述
		sParaTemp.put("show_url", AlipayConfig.show_url);
		// 防钓鱼时间戳
		String anti_phishing_key = AlipaySubmit.query_timestamp();
		sParaTemp.put("anti_phishing_key", anti_phishing_key);
		sParaTemp.put("exter_invoke_ip", paymentInfoBO.getUserreqIp());
		sParaTemp.put("it_b_pay", AlipayConfig.it_b_pay);
		sParaTemp.put("qr_pay_mode", AlipayConfig.qr_pay_mode);
		sParaTemp.put("extra_common_param", paymentInfoBO.getAttach());// 公用回传参数
		return sParaTemp;
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		AlipayDataDataserviceBillDownloadurlQueryRequest request = new AlipayDataDataserviceBillDownloadurlQueryRequest();
		request.setBizContent("{" + "    \"bill_type\":\"" + map.get("billType") + "\"," + "    \"bill_date\":\"" + map.get("billDate") + "\"" + "  }");
		String billDownloanUrl = "";
		try {
			AlipayDataDataserviceBillDownloadurlQueryResponse response = alipayClient.execute(request);
			billDownloanUrl = response.getBillDownloadUrl();
		} catch (AlipayApiException e) {
			logger.error("请求支付宝下载对账单接口异常", e);
			return ResultBO.err();
		}
		return ResultBO.ok(billDownloanUrl);
	}

	// 交易状态：WAIT_BUYER_PAY（交易创建，等待买家付款）、TRADE_CLOSED（未付款交易超时关闭，或支付完成后全额退款）、TRADE_SUCCESS（交易支付成功）、TRADE_FINISHED（交易结束，不可退款）
	private PayStatusEnum getPayStatus(String status) {
		PayStatusEnum payStatusEnum = null;
		if (ObjectUtil.isBlank(status)) {
			return payStatusEnum;
		}
		switch (status) {
		case "WAIT_BUYER_PAY":// 交易创建，等待买家付款
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
			break;
		case "TRADE_CLOSED":// 未付款交易超时关闭，或支付完成后全额退款
			payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
			break;
		case "TRADE_SUCCESS":// 交易支付成功
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "TRADE_FINISHED":// 交易结束，不可退款
			payStatusEnum = PayStatusEnum.USER_CANCELLED_PAYMENT;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;// 未知状态
			break;
		}
		return payStatusEnum;
	}
}
