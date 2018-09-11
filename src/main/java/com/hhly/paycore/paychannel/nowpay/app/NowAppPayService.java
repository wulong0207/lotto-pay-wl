package com.hhly.paycore.paychannel.nowpay.app;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.nowpay.config.NowPayConfig;
import com.hhly.paycore.paychannel.nowpay.util.FormDateReportConvertor;
import com.hhly.paycore.paychannel.nowpay.util.HttpNowPayUtils;
import com.hhly.paycore.paychannel.nowpay.util.MD5Facade;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

import net.sf.json.JSONObject;

/**
 * 
 * @ClassName: NowAppPayService 
 * @Description: 现在支付app
 * @author wuLong
 * @date 2017年8月8日 上午10:39:11 
 *
 */
public class NowAppPayService extends PayAbstract {
	private Logger logger = LoggerFactory.getLogger(NowAppPayService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("NowAppPayService.pay接手的对象信息:"+JSON.toJSONString(paymentInfo));
		PayReqResultVO payReqResult = new PayReqResultVO();
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		String payType = paymentInfo.getPayType();
		String payChannelType = null;
		String md5key = null;
		switch (payType) {
		case "10":
			packageParams.put("appId", NowPayConfig.NOW_PAY_APP_ALI_QQ_APPID);
			md5key = NowPayConfig.NOW_PAY_APP_ALI_QQ_MD5KEY;
			payChannelType = "12";
			break;
		case "11":
			packageParams.put("appId", NowPayConfig.NOW_PAY_APP_WX_APPID);
			md5key = NowPayConfig.NOW_PAY_APP_WX_MD5KEY;
			payChannelType = "13";
			break;
		default:
			break;
		}
		packageParams.put("consumerName", "IPaynow");
		packageParams.put("mhtCharset", "UTF-8");
		packageParams.put("mhtCurrencyType", "156");
		packageParams.put("mhtOrderAmt", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
		packageParams.put("mhtOrderDetail", paymentInfo.getInfoOrder());
		packageParams.put("mhtOrderName", paymentInfo.getNameGoods());
		packageParams.put("mhtOrderNo", paymentInfo.getNoOrder());
		packageParams.put("mhtOrderStartTime", paymentInfo.getDtOrder());
		packageParams.put("mhtOrderTimeOut", String.valueOf(Integer.valueOf(paymentInfo.getValidOrder()) * 60));
		packageParams.put("mhtOrderType", "01");
		packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());
		packageParams.put("payChannelType", payChannelType);
		if(paymentInfo.getPayPlatform() == 4){//ios
			packageParams.put("mhtReserved", "yicai-ios-pay");
		}else if(paymentInfo.getPayPlatform() == 3){//android
			packageParams.put("mhtLimitPay", "1");
			packageParams.put("mhtReserved", "yicai-android-pay");
			packageParams.put("version", "1.0.0");
			packageParams.put("deviceType", "01");
			packageParams.put("funcode", "WP001");
			packageParams.put("mhtSignType", "MD5");
		}
		String mhtSignature = MD5Facade.getFormDataParamMD5(packageParams, md5key, "UTF-8");
		packageParams.put("mhtSignature", mhtSignature);
		if(paymentInfo.getPayPlatform() == 4){
			packageParams.put("mhtSignType", "MD5");
		}
		payReqResult.setFormLink(HttpNowPayUtils.getParseMapToFormString(packageParams));
		payReqResult.setType(PayConstants.PayReqResultEnum.ENCRYPTION.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.NOWPAY_RECHARGE.getKey());
		payReqResult.setPayType(PayConstants.AppPayTypeEnum.SDK.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		try {
			packageParams.put("funcode", "MQ002");
			packageParams.put("mhtOrderNo", payQueryParamVO.getTransCode());
			packageParams.put("mhtCharset", "UTF-8");
			packageParams.put("mhtSignType", "MD5");
			String rechargeChannel = payQueryParamVO.getRechargeChannel();
			String md5key = null;
			switch (rechargeChannel) {
			case "10":
				packageParams.put("appId", NowPayConfig.NOW_PAY_APP_ALI_QQ_APPID);
				md5key = NowPayConfig.NOW_PAY_APP_ALI_QQ_MD5KEY;
				break;
			case "11":
				packageParams.put("appId", NowPayConfig.NOW_PAY_APP_WX_APPID);
				md5key = NowPayConfig.NOW_PAY_APP_WX_MD5KEY;
				break;
			default:
				break;
			}
			String sign = MD5Facade.getFormDataParamMD5(packageParams, md5key, "UTF-8");
			packageParams.put("mhtSignature", sign);
			logger.info("现在支付商户主动查询订单状态请求参数:" + JSONObject.fromObject(packageParams));
//			String reposeText = HttpNowPayUtils.http(NowPayConfig.NOW_PAY_URL, packageParams);
			String reposeText = HttpUtil.doPost(NowPayConfig.NOW_PAY_URL, packageParams);
			
			logger.info("现在支付商户主动查询订单状态返回结果:" + reposeText);
			Map<String, String> map = FormDateReportConvertor.parseFormDataPatternReportWithDecode(reposeText, "UTF-8", "UTF-8");
			logger.info("现在支付商户主动查询订单状态转码返回结果:" + JSONObject.fromObject(map));
			String responseCode = map.get("responseCode");
			if ("A001".equals(responseCode)) {
				String signature = map.get("signature");
				map.remove("signature");
				String mhtReserved = map.get("mhtReserved");
				if(mhtReserved.indexOf("ios")!=-1){
					map.remove("signType");
				}
				String signReturn = MD5Facade.getFormDataParamMD5(map, md5key, "UTF-8");
				if (!signReturn.equals(signature)) {
					logger.error("现在支付查询订单状态返回结果：签名错误,现在支付我方签名结果：" + signReturn + ",第三方签名:" + signature);
					return ResultBO.err();
				}
				// 2.封装返回对象
				if (map.containsKey("nowPayOrderNo")) { // 支付宝交易号
					payQueryResultVO.setTradeNo(map.get("nowPayOrderNo"));
				}
				if (map.containsKey("mhtOrderNo")) {// 商户订单号
					payQueryResultVO.setOrderCode(map.get("mhtOrderNo"));
				}
				if (map.containsKey("mhtOrderAmt")) {// 支付金额
					String totalFee = map.get("mhtOrderAmt");
					if (!ObjectUtil.isBlank(totalFee)) {
						double tf = Double.valueOf(totalFee) / 100;
						payQueryResultVO.setTotalAmount(String.valueOf(tf));
					}
				}
				if (map.containsKey("transStatus")) {// 支付状态
					String status = map.get("transStatus");
					// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
					if ("A001".equals(status)) {// 成功
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
					} else if ("A004".equals(status) || "A003".equals(status) || "A00I".equals(status)) {// 处理中||支付结果未知||订单未处理
						payQueryResultVO.setTradeStatus(PayStatusEnum.BEING_PAID);
					} else if ("A002".equals(status)) {// 失败
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_FAILURE);
					} else if ("A005".equals(status)) {// 订单受理失败
						payQueryResultVO.setTradeStatus(PayStatusEnum.OVERDUE_PAYMENT);
					} else if ("A006".equals(status)) {// 交易关闭
						payQueryResultVO.setTradeStatus(PayStatusEnum.USER_CANCELLED_PAYMENT);
					}
				}
				if (map.containsKey("payTime")) {// 付款时间
					payQueryResultVO.setArriveTime(DateUtil.convertDateToStr(DateUtil.convertStrToDate(map.get("payTime"), DateUtil.DATE_FORMAT_NUM)));
				}
			} else {
				if ("A002".equals(responseCode)) {
					logger.info("现在支付商户主动查询订单状态响应失败");
					return ResultBO.err();
				} else if ("A002".equals(responseCode)) {
					logger.info("现在支付商户主动查询订单状态响应未知");
					return ResultBO.err();
				} else {
					return ResultBO.err();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.ok(payQueryResultVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			String payChannelType = map.get("payChannelType");
			String md5key = null;
			if ("12".equals(payChannelType)) {
				md5key = NowPayConfig.NOW_PAY_APP_ALI_QQ_MD5KEY;
			} else if ("13".equals(payChannelType)) {
				md5key = NowPayConfig.NOW_PAY_APP_WX_MD5KEY;
			}
			String signatrue = map.get("signature");
			map.remove("signature");
			String mhtReserved = map.get("mhtReserved");
			if(mhtReserved.indexOf("ios")!=-1){
				map.remove("signType");
			}
			String sign = MD5Facade.getFormDataParamMD5(map, md5key, "UTF-8");
			if (!sign.equals(signatrue)) {
				logger.error("现在支付异步通知结果：签名错误,现在支付我方签名结果：" + sign + ",第三方签名:" + signatrue);
				payNotifyResult.setResponse("SUCCESS");
				payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 支付失败
				return ResultBO.ok(payNotifyResult);
			}
			
			/*
			 {mhtCharset=UTF-8, tradeStatus=A001, payChannelType=12, appId=1408709961320306, 
			 mhtOrderStartTime=20170908153941, mhtOrderNo=I17090815394105100001, signType=MD5,
			  mhtReserved=yicai-ios-pay, funcode=N001, mhtOrderAmt=1, mhtOrderTimeOut=1800, 
			  mhtOrderName=2Ncai-300-170826-1, channelOrderNo=2017090821001004440227317004, deviceType=01, 
			  payConsumerId=2088802874192449, mhtOrderType=05, nowPayOrderNo=201001201709081539557585327,
			   mhtCurrencyType=156}

			 */
			// 2.封装返回对象
			if (map.containsKey("nowPayOrderNo")) { // 支付宝交易号
				payNotifyResult.setThirdTradeNo(map.get("nowPayOrderNo"));
			}
			if (map.containsKey("mhtOrderNo")) {// 商户订单号
				payNotifyResult.setOrderCode(map.get("mhtOrderNo"));
			}
			if (map.containsKey("mhtOrderAmt")) {// 支付金额
				String totalFee = map.get("mhtOrderAmt");
				if (!ObjectUtil.isBlank(totalFee)) {
					double tf = Double.valueOf(totalFee) / 100;
					payNotifyResult.setOrderAmt(tf);
				}
			}
			if (map.containsKey("tradeStatus")) {// 支付状态
				String status = map.get("tradeStatus");
				// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
				if ("A001".equals(status)) {// 成功
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else if ("A004".equals(status) || "A003".equals(status) || "A00I".equals(status)) {// 处理中||支付结果未知||订单未处理
					payNotifyResult.setStatus(PayStatusEnum.BEING_PAID);
				} else if ("A002".equals(status)) {// 失败
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				} else if ("A005".equals(status)) {// 订单受理失败
					payNotifyResult.setStatus(PayStatusEnum.OVERDUE_PAYMENT);
				} else if ("A006".equals(status)) {// 交易关闭
					payNotifyResult.setStatus(PayStatusEnum.USER_CANCELLED_PAYMENT);
				}
			}
			if (map.containsKey("payTime")) {// 付款时间
				payNotifyResult.setTradeTime(DateUtil.convertDateToStr(DateUtil.convertStrToDate(map.get("payTime"), DateUtil.DATE_FORMAT_NUM)));
			}
			payNotifyResult.setResponse("SUCCESS=Y");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		}
		return ResultBO.ok(payNotifyResult);
	}

}
