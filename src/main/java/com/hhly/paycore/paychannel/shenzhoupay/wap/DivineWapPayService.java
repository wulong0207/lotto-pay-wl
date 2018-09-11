package com.hhly.paycore.paychannel.shenzhoupay.wap;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
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
import com.hhly.utils.BuildRequestFormUtil;

/**
 * 
 * @ClassName: ShenZhouWapPayService 
 * @Description: 神州支付wap
 * @author wuLong
 * @date 2017年8月8日 上午10:32:33 
 *
 */
public class DivineWapPayService extends PayAbstract {
	private Logger logger = LoggerFactory.getLogger(DivineWapPayService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		PayReqResultVO payReqResult = new PayReqResultVO();
		try {
			packageParams.put("appid", DivineConfig.SHENZHOU_PAY_WX_APPID);
			packageParams.put("amount", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
			packageParams.put("mchntOrderNo", paymentInfo.getNoOrder());
			packageParams.put("subject", ObjectUtil.isBlank(paymentInfo.getNameGoods()) ? GetWeChatUtil.getRandomStr(32) : paymentInfo.getNameGoods());
			packageParams.put("currency", "RMB");
			packageParams.put("body", paymentInfo.getInfoOrder());
			packageParams.put("clientIp", paymentInfo.getUserreqIp());
			packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());// 商户后台通知URL
			packageParams.put("returnUrl", paymentInfo.getUrlReturn());// 商户前台通知URL
			packageParams.put("description", paymentInfo.getAttach());
			packageParams.put("expireMs", String.valueOf(Integer.valueOf(paymentInfo.getValidOrder()) * 60 * 60));// 商户订单超时时间 60~3600秒，默认3600
			packageParams.put("version", "api_NoEncrypt");
			packageParams.put("payChannelId", "0000000007");
			String mhtSignature = RSAUtil.doEncrypt(packageParams, DivineConfig.SHENZHOU_PAY_WX_KEY);
			packageParams.put("signature", mhtSignature);// 商户数据签名(除mhtSignature字段外，所有参数都参与MD5签名。)HttpDivinePayUtils.parseMapToString(packageParams)
			String encryptBefore = JSON.toJSONString(packageParams);
			logger.info("神州支付加密前的请求参数：" + encryptBefore);
			// 加密
			String reqParams = null;
			try {
				reqParams = Base64.encodeBase64String(RSAUtil.encryptByPublicKeyByPKCS1Padding(encryptBefore.getBytes("utf-8"), DivineConfig.SHENZHOU_RSA_PUBLIC_KEY));
				logger.info("神州支付加密后的请求参数" + reqParams);
				Map<String, String> map = new HashMap<String, String>();
				map.put("orderInfo", reqParams);
				String formLink = BuildRequestFormUtil.buildRequest(map, DivineConfig.SHENZHOU_PAY_URL);
				logger.info("神州支付请求返回结果:" + formLink);
				if (formLink.indexOf("form") < 0) {
					return ResultBO.err();
				}
				payReqResult.setFormLink(formLink);
				payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.DIVINEPAY_RECHARGE.getKey());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (reqParams == null) {
				return ResultBO.err();
			}
			// 建立请求
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
