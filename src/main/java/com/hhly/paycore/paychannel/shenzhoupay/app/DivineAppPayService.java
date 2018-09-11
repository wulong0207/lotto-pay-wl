package com.hhly.paycore.paychannel.shenzhoupay.app;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.shenzhoupay.config.DivineConfig;
import com.hhly.paycore.paychannel.shenzhoupay.util.DivineUtil;
import com.hhly.paycore.paychannel.shenzhoupay.util.RSAUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

/**
 * 
 * @ClassName: DivineAppPayService 
 * @Description: 神州App 
 * @author wuLong
 * @date 2017年9月8日 下午2:09:21 
 *
 */
public class DivineAppPayService extends PayAbstract {
	private Logger logger = LoggerFactory.getLogger(DivineAppPayService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		PayReqResultVO payReqResult = new PayReqResultVO();
		try {
			String key = null;
			if (paymentInfo.getPayPlatform() == 4) {// ios
				packageParams.put("appid", DivineConfig.SHENZHOU_PAY_WX_APPID);
				packageParams.put("amount", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
				packageParams.put("mchntOrderNo", paymentInfo.getNoOrder());
				packageParams.put("subject", ObjectUtil.isBlank(paymentInfo.getNameGoods()) ? GetWeChatUtil.getRandomStr(32) : paymentInfo.getNameGoods());
				packageParams.put("currency", "RMB");
				packageParams.put("body", paymentInfo.getInfoOrder());
				packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());// 商户后台通知URL
				packageParams.put("expireMs", String.valueOf(Integer.valueOf(paymentInfo.getValidOrder()) * 60 * 60));// 商户订单超时时间 60~3600秒，默认3600
				packageParams.put("version", DivineConfig.SHENZHOU_PAY_APP_IOS_VERSION);
				packageParams.put("payChannelId", "wechat");
				packageParams.put("appinfo", "2N彩票|yicai.apps.2ncai");
				key = DivineConfig.SHENZHOU_PAY_WX_KEY;
			} else if (paymentInfo.getPayPlatform() == 3) {// android
				packageParams.put("appid", DivineConfig.SHENZHOU_PAY_WX_APPID);
				packageParams.put("amount", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
				packageParams.put("mchntOrderNo", paymentInfo.getNoOrder());
				packageParams.put("subject", ObjectUtil.isBlank(paymentInfo.getNameGoods()) ? GetWeChatUtil.getRandomStr(32) : paymentInfo.getNameGoods());
				packageParams.put("body", paymentInfo.getInfoOrder());
				packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());// 商户后台通知URL
				packageParams.put("version", DivineConfig.SHENZHOU_PAY_APP_ANDROID_VERSION);
				packageParams.put("clientIp", paymentInfo.getUserreqIp());
				packageParams.put("payChannelId", "wechat");
				key = DivineConfig.SHENZHOU_PAY_WX_KEY;
			}
			String encryptBefore = JSON.toJSONString(packageParams);
			logger.info("神州支付加密前的请求参数：" + encryptBefore);
			String mhtSignature = RSAUtil.doEncrypt(packageParams, key);
			if (paymentInfo.getPayPlatform() == 4) {// ios
				packageParams.put("sign", mhtSignature);
				payReqResult.setFormLink(JSON.toJSONString(packageParams));
			} else if (paymentInfo.getPayPlatform() == 3) {
				packageParams.put("appSign", mhtSignature);
				packageParams.put("ip", paymentInfo.getUserreqIp());
				packageParams.put("appkey", DivineConfig.SHENZHOU_PAY_WX_KEY);
				packageParams.remove("clientIp");
				payReqResult.setFormLink(JSON.toJSONString(packageParams));
			}
			payReqResult.setType(PayConstants.PayReqResultEnum.ENCRYPTION.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.DIVINEPAY_RECHARGE.getKey());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return DivineUtil.queryOrder(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return DivineUtil.payNotify(map);
	}

}
