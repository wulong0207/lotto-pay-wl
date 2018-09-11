package com.hhly.paycore.paychannel.wechatpay.wap;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.wechatpay.config.WeChatPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.RequestHandler;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayResultConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.XmlUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

public class WeChatAppPayService extends PayAbstract {
	Logger logger = Logger.getLogger(WeChatAppPayService.class);

	public ResultBO<?> pay(PaymentInfoBO pay) {
		logger.debug("微信APP支付开始");
		JSONObject json = new JSONObject(); 
		// 1 参数
		// 订单号
		String barcode = pay.getNoOrder();
		// 总金额以分为单位，不带小数点
		String totalFee = GetWeChatUtil.getMoney(pay.getMoneyOrder());
		// 订单生成的机器 IP
		String spbill_create_ip = WeChatPayConfig.spbill_create_ip;
		String trade_type = "APP";
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
		// 3 获取app支付所需参数
		String xml = XmlUtil.map2xmlBody(packageParams, "xml");
		PayReqResultVO payReqResult = null;
		try {
			Map map = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxcreateOrderURL, xml);
			String prepayId = (String) map.get("prepay_id");
			logger.info("获取微信预支付ID----------------" + prepayId);
			json.put("sign", sign);  
	        json.put("appid", WeChatPayConfig.appid);  
	        json.put("noncestr", nonce_str);  
	        json.put("package", "Sign=WXPay");  
	        json.put("partnerid", mch_id);  
	        json.put("prepayid", prepayId);  
	        json.put("timestamp", System.currentTimeMillis());  
	        String jsonStr = json.toJSONString();
	        payReqResult = new PayReqResultVO(jsonStr);
			if (ObjectUtil.isBlank(jsonStr)) {
				payReqResult.setCode(false);
				payReqResult.setMsg("获取预支付Id异常");
			}
			payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.WECHAT_RECHARGE.getKey());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.ok(payReqResult);
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
	
}
