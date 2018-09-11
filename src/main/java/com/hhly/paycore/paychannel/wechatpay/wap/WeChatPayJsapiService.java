package com.hhly.paycore.paychannel.wechatpay.wap;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.wechatpay.config.WeChatPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.RequestHandler;
import com.hhly.paycore.paychannel.wechatpay.web.util.Sha1Util;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.XmlUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

public class WeChatPayJsapiService extends PayAbstract {

	Logger logger = Logger.getLogger(WeChatPayJsapiService.class);

	public ResultBO<?> pay(PaymentInfoBO pay) {
		logger.debug("微信Jsapi支付开始");

		// 获取code
		String code = new GetWeChatUtil().getCode(pay.getNoOrder());
		// 获取openid
		String openid = new GetWeChatUtil().getOpenId(code);
		// 1 参数
		// 总金额以分为单位，不带小数点
		String totalFee = GetWeChatUtil.getMoney(pay.getMoneyOrder());
		// 订单生成的机器 IP
		String spbill_create_ip = WeChatPayConfig.spbill_create_ip;
		String trade_type = "JSAPI";
		// 商户号
		String mch_id = WeChatPayConfig.mchid;
		// 随机字符串
		String nonce_str = GetWeChatUtil.getNonceStr();

		SortedMap<String, Object> packageParams = new TreeMap<String, Object>();
		packageParams.put("appid", WeChatPayConfig.appid);
		packageParams.put("mch_id", mch_id);
		packageParams.put("nonce_str", nonce_str);
		packageParams.put("body", pay.getInfoOrder());
		packageParams.put("attach", pay.getAttach());
		packageParams.put("out_trade_no", pay.getNoOrder());
		packageParams.put("total_fee", totalFee);
		packageParams.put("spbill_create_ip", spbill_create_ip);
		packageParams.put("notify_url", pay.getNotifyUrl());
		packageParams.put("trade_type", trade_type);
		packageParams.put("openid", openid);

		RequestHandler reqHandler = new RequestHandler(null, null);
		reqHandler.init(WeChatPayConfig.appid, WeChatPayConfig.appsecret, WeChatPayConfig.keyapi);

		String sign = reqHandler.createSign(packageParams);
		packageParams.put("sign", sign);
		String xml = XmlUtil.map2xmlBody(packageParams, "xml");
		PayReqResultVO payReqResult = null;
		try {
			Map map = new GetWeChatUtil().getWxInfo(GetWeChatUtil.wxcreateOrderURL, xml);
			String prepayId = (String) map.get("prepay_id");
			logger.info("获取到的预支付ID：" + prepayId);

			// 获取prepay_id后，拼接最后请求支付所需要的package
			SortedMap<String, Object> finalpackage = new TreeMap<String, Object>();
			String timestamp = Sha1Util.getTimeStamp();
			String packages = "prepay_id=" + prepayId;
			finalpackage.put("appId", WeChatPayConfig.appid);
			finalpackage.put("timeStamp", timestamp);
			finalpackage.put("nonceStr", nonce_str);
			finalpackage.put("package", packages);
			finalpackage.put("signType", "MD5");
			// 要签名
			String finalsign = reqHandler.createSign(finalpackage);
			String finaPackage = "\"appId\":\"" + WeChatPayConfig.appid + "\",\"timeStamp\":\"" + timestamp + "\",\"nonceStr\":\"" + nonce_str + "\",\"package\":\"" + packages + "\",\"signType\" : \"MD5" + "\",\"paySign\":\"" + finalsign + "\"";
			payReqResult = new PayReqResultVO(finaPackage);
			if (ObjectUtil.isBlank(finaPackage)) {
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
}
