package com.hhly.paycore.paychannel.wechatpay.web;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.wechatpay.config.WeChatPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.RequestHandler;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayResultConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.base.util.XmlUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;

/**
 * @Desc: 微信支付Web 相关操作
 * @author YiJian
 * @date 2017年3月30日
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
@Service("weChatWebPayService")
public class WeChatWebPayService extends PayAbstract {
	Logger logger = Logger.getLogger(WeChatWebPayService.class);

	public ResultBO<?> pay(PaymentInfoBO pay) {
		logger.debug("微信扫码支付开始");
		// 1 参数
		// 订单号
		String barcode = pay.getNoOrder();
		// 总金额以分为单位，不带小数点
		String totalFee = GetWeChatUtil.getMoney(pay.getMoneyOrder());
		// 订单生成的机器 IP
		String spbill_create_ip = WeChatPayConfig.spbill_create_ip;
		String trade_type = "NATIVE";
		// 商户号
		String mch_id = WeChatPayConfig.mchid;
		// 随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();
		// 商户订单号
		String out_trade_no = barcode;
		// 附加参数
		String attach = pay.getAttach();
		// 商品描述
		String body = pay.getInfoOrder();
		// 2 封装参数 生成Sign签名
		SortedMap<String, Object> packageParams = new TreeMap<String, Object>();
		packageParams.put("appid", WeChatPayConfig.appid);
		packageParams.put("mch_id", mch_id);
		packageParams.put("nonce_str", nonce_str);
		packageParams.put("body", body);
		packageParams.put("attach", attach);
		packageParams.put("out_trade_no", out_trade_no);
		packageParams.put("total_fee", totalFee);
		packageParams.put("spbill_create_ip", spbill_create_ip);
		packageParams.put("notify_url", pay.getNotifyUrl());
		packageParams.put("trade_type", trade_type);

		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		String sign = reqHandler.createSign(packageParams);
		packageParams.put("sign", sign);
		// 3 获取codeurl
		String xml = XmlUtil.map2xmlBody(packageParams, "xml");
		PayReqResultVO payReqResult = null;
		try {
			Map map = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxcreateOrderURL, xml);
			String code_url = (String) map.get("code_url");

			logger.info("获取微信扫一扫 URL----------------" + code_url);
			payReqResult = new PayReqResultVO(code_url);
			if (ObjectUtil.isBlank(code_url)) {
				payReqResult.setCode(false);
				payReqResult.setMsg((String) map.get("err_code_des"));
			}
			payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.WECHAT_RECHARGE.getKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		// 微信金额单位为分，这里把元换算为分
		String orderAmount = GetWeChatUtil.getMoney(StringUtil.convertObjToStr(refundParam.getOrderAmount()));
		String refundAmount = GetWeChatUtil.getMoney(StringUtil.convertObjToStr(refundParam.getRefundAmount()));
		// 获取随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();
		// 封装请求参数
		SortedMap<String, Object> paramMap = new TreeMap<String, Object>();
		paramMap.put("appid", WeChatPayConfig.appid);
		paramMap.put("mch_id", WeChatPayConfig.mchid);
		paramMap.put("nonce_str", nonce_str);
		paramMap.put("op_user_id", WeChatPayConfig.mchid);// 操作员
		paramMap.put("out_refund_no", refundParam.getRefundCode());// 商户退款单号
		paramMap.put("out_trade_no", refundParam.getOrderCode());// 商户订单号
		paramMap.put("refund_fee", refundAmount);// 退款金额
		paramMap.put("total_fee", orderAmount);// 订单金额
		// paramMap.put("transaction_id", refundParam.getThirdTransId());//微信订单号，与商户订单号二选一
		// 初始化微信请求
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		// 生成Sign签名
		String sign = reqHandler.createSign(paramMap);
		paramMap.put("sign", sign);
		String requestXML = XmlUtil.map2xmlBody(paramMap, "xml");
		// 发送退款请求 return_code：SUCCESS/FAIL
		RefundResultVO refundResultVO = null;
		try {
			Map map = new GetWeChatUtil().wxRefund(requestXML);
			// 将map中的参数转成RefundResultVO中的对象字段
			refundResultVO = new RefundResultVO(map);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.ok(refundResultVO);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		// 获取随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();
		String tradeNo = payQueryParamVO.getTradeNo();
		// 封装请求参数
		SortedMap<String, Object> paramMap = new TreeMap<String, Object>();
		paramMap.put("appid", WeChatPayConfig.appid);
		paramMap.put("mch_id", WeChatPayConfig.mchid);
		paramMap.put("nonce_str", nonce_str);
		paramMap.put("out_trade_no", payQueryParamVO.getTransCode());// 益彩交易流水号
		paramMap.put("transaction_id", tradeNo);// 微信订单号
		// 初始化微信请求
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		// 生成Sign签名
		String sign = reqHandler.createSign(paramMap);
		paramMap.put("sign", sign);
		// 发送请求，获取订单详情
		String requestXML = XmlUtil.map2xmlBody(paramMap, "xml");
		PayQueryResultVO payQueryVO = null;
		Map map = null;
		try {
			map = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxorderQueryURL, requestXML);
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.debug("查询【" + tradeNo + "】交易结果返回：" + map.toString());
		if (!ObjectUtil.isBlank(map)) {
			String returnCode = (String) map.get("return_code");// 返回状态码SUCCESS
			String returnMsg = (String) map.get("return_msg");// 返回结果
			if (returnCode.equals("SUCCESS")) {
				payQueryVO = new PayQueryResultVO(map);
			} else {
				logger.info("查询【" + tradeNo + "】交易结果返回：returnCode:" + returnCode + "，returnMsg:" + returnMsg);
			}
		}
		return ResultBO.ok(payQueryVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// 获取随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();
		// 封装请求参数
		SortedMap<String, Object> paramMap = new TreeMap<String, Object>();
		paramMap.put("appid", WeChatPayConfig.appid);
		paramMap.put("mch_id", WeChatPayConfig.mchid);
		paramMap.put("nonce_str", nonce_str);
		paramMap.put("out_trade_no", payQueryParamVO.getTransCode());
		// 初始化微信请求
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		// 生成Sign签名
		String sign = reqHandler.createSign(paramMap);
		paramMap.put("sign", sign);
		// 发送请求，获取退款详情
		String requestXML = XmlUtil.map2xmlBody(paramMap, "xml");
		Map map = new HashMap();
		RefundResultVO refundResultVO = null;
		try {
			map = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxrefundQueryURL, requestXML);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.ok(map);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		// 验签
		String notifySign = map.get("sign");
		// 在通知返回参数列表中，除去sign、sign_type两个参数外，凡是通知返回回来的参数皆是待验签的参数。
		map.remove("sign");
		map.remove("sign_type");
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		// 生成Sign签名
		String sign = reqHandler.getSortParams(map);

		if (!notifySign.equals(sign)) {
			payNotifyResult.setResponse("<xml>" + "<return_code><![CDATA[FAIL]]></return_code>" + "<return_msg><![CDATA[验签失败]]></return_msg>" + "</xml> ");
			payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 支付失败
			return ResultBO.ok(payNotifyResult);
		}
		// 验签成功，封装参数返回
		if (map.containsKey("transaction_id")) { // wx交易号
			payNotifyResult.setThirdTradeNo(map.get("transaction_id"));
		}
		if (map.containsKey("out_trade_no")) {// 商户订单号
			payNotifyResult.setOrderCode(map.get("out_trade_no"));
		}
		if (map.containsKey("total_fee")) {// 支付金额
			payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("total_fee"))));
		}
		if (map.containsKey("result_code")) {// 支付状态
			if (PayResultConstants.COMMON_RESULT_SUCCESS.equals(map.get("result_code"))) {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
		}
		if (map.containsKey("time_end")) {// 付款时间
			payNotifyResult.setTradeTime(map.get("time_end"));
		}
		if (map.containsKey("attach")) {// 附加参数（订单详情）
			payNotifyResult.setAttachData(map.get("attach"));
		}
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("return_code", "SUCCESS");
		returnMap.put("return_msg", "OK");
		payNotifyResult.setResponse(XmlUtil.map2xmlBody(returnMap, "xml"));
		return ResultBO.ok(payNotifyResult);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		// 获取随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();
		// 封装请求参数
		SortedMap<String, Object> paramMap = new TreeMap<String, Object>();
		paramMap.put("appid", WeChatPayConfig.appid);
		paramMap.put("mch_id", WeChatPayConfig.mchid);
		paramMap.put("nonce_str", nonce_str);
		paramMap.put("bill_date", map.get("billDate"));
		paramMap.put("bill_type", map.get("billType"));
		// 初始化微信请求
		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);
		// 生成Sign签名
		String sign = reqHandler.createSign(paramMap);
		paramMap.put("sign", sign);
		// 发送请求，获取退款详情
		String requestXML = XmlUtil.map2xmlBody(paramMap, "xml");
		Map repmap = new HashMap();
		try {
			repmap = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxbillQueryURL, requestXML);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.ok(repmap);
	}

}
