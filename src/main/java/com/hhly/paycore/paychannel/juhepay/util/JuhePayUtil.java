package com.hhly.paycore.paychannel.juhepay.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.juhepay.config.JuhePayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeThirdEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.Md5Util;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.JuhePayResponseVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;

public class JuhePayUtil {
	private static Logger logger = LoggerFactory.getLogger(JuhePayUtil.class);

	/**  
	* 方法说明: 支付请求
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param platform
	* @time: 2017年11月24日 下午3:26:20
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("static-access")
	public static ResultBO<?> pay(PaymentInfoBO paymentInfo, String platform) {
		PayTypeThirdEnum payType = PayConstants.PayTypeThirdEnum.getEnum(paymentInfo.getPayType());
		logger.info("聚合支付" + payType.getValue() + " " + platform + "请求原始参数：" + paymentInfo.toString());
		SortedMap<String, Object> packageParams = new TreeMap<String, Object>();
		packageParams.put("cpId", JuhePayConfig.JUHEPAY_CPID);
		packageParams.put("cpOrderId", paymentInfo.getNoOrder());
		packageParams.put("mchCreateIp", paymentInfo.getUserreqIp());
		packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());
		packageParams.put("serviceName", getServiceName(paymentInfo, platform));
		packageParams.put("totalFee", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
		packageParams.put("nonceStr", GetWeChatUtil.getRandomStr(32));
		packageParams.put("inputCharset", "UTF-8");
		packageParams.put("signType", "MD5");
		packageParams.put("currency", "CNY");
		packageParams.put("attach", paymentInfo.getAttach());
		packageParams.put("body", paymentInfo.getInfoOrder());
		packageParams.put("timeStart", paymentInfo.getDtOrder());
		// 京东支付需要加orderType参数
		if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.JD_PAYMENT.getKey())) {
			if (platform.equals(PayAbstract.PLATFORM_WAP)) {
				packageParams.put("callbackUrl", paymentInfo.getUrlReturn());// 前台地址
			} else {
				packageParams.put("orderType", "1");// 订单类型： 0-实物，1-虚拟
			}
		}

		Date dtorder = DateUtil.convertStrToDate(paymentInfo.getDtOrder(), DateUtil.DATE_FORMAT_NUM);
		Calendar cd = Calendar.getInstance();
		cd.setTime(dtorder);
		cd.add(Calendar.MINUTE, Integer.valueOf(paymentInfo.getValidOrder()));
		packageParams.put("timeExpire", DateUtil.convertDateToStr(cd.getTime(), DateUtil.DATE_FORMAT_NUM));
		String sign = createSign(packageParams, JuhePayConfig.JUHEPAY_MD5_KEY);
		packageParams.put("sign", sign);
		logger.info("聚合支付" + platform + "调用参数：" + JSON.toJSONString(packageParams));
		try {
			String responseStr = HttpUtil.doPost(JuhePayConfig.JUHEPAY_URL, packageParams);
			logger.info("聚合支付返回：" + responseStr);
			JSONObject jsonObject = JSONObject.parseObject(responseStr);
			if (jsonObject.containsKey("errorMsg")) {
				logger.error("调用聚合支付" + platform + "请求失败：" + jsonObject.get("errorMsg"));
				return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, jsonObject.get("errorMsg"));
			}
			JuhePayResponseVO juhePayResponseVO = (JuhePayResponseVO) jsonObject.toJavaObject(jsonObject, JuhePayResponseVO.class);
			return ResultBO.ok(juhePayResponseVO);
		} catch (Exception e) {
			logger.error("聚合支付" + platform + "请求支付异常：", e);
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 异步回调
	* @auth: xiongJinGang
	* @param map
	* @param platform
	* @time: 2017年11月24日 下午3:26:35
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map, String platform) {
		logger.info("聚合支付" + platform + "异步通知结果：" + map.toString());
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		SortedMap<String, Object> sortedMap = new TreeMap<String, Object>();
		for (String key : map.keySet()) {
			if (key.equals("sign")) {
				continue;
			}
			sortedMap.put(key, map.get(key));
		}
		String reponseSing = map.get("sign");
		String sign = createSign(sortedMap, JuhePayConfig.JUHEPAY_MD5_KEY);
		if (!sign.equals(reponseSing)) {
			logger.error("聚合支付异步通知结果：签名错误,聚合支付我方签名结果：" + sign + ",第三方签名:" + reponseSing);
			payNotifyResult.setResponse("SUCCESS");
			payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 支付失败
			return ResultBO.ok(payNotifyResult);
		}
		// 2.封装返回对象
		if (map.containsKey("transId")) { // 支付宝交易号
			payNotifyResult.setThirdTradeNo(map.get("transId"));
		}
		if (map.containsKey("cpOrderId")) {// 商户订单号
			payNotifyResult.setOrderCode(map.get("cpOrderId"));
		}
		if (map.containsKey("totalFee")) {// 支付金额
			String totalFee = map.get("totalFee");
			if (!ObjectUtil.isBlank(totalFee)) {
				double tf = Double.valueOf(totalFee) / 100;
				payNotifyResult.setOrderAmt(tf);
			}
		}
		if (map.containsKey("status")) {// 支付状态
			payNotifyResult.setStatus(getPayStatus(map.get("status")));
		}
		if (map.containsKey("transTime")) {// 付款时间
			payNotifyResult.setTradeTime(DateUtil.convertDateToStr(DateUtil.convertStrToDate(map.get("transTime"), DateUtil.DATE_FORMAT_NUM)));
		}
		if (map.containsKey("attach")) {// 附加参数（订单详情）
			payNotifyResult.setAttachData(map.get("attach"));
		}
		payNotifyResult.setResponse("SUCCESS");
		return ResultBO.ok(payNotifyResult);
	}

	/**  
	* 方法说明: 支付结果查询
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @param platform
	* @time: 2017年11月24日 下午3:26:46
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO, String platform) {
		logger.info("聚合支付" + platform + "查询订单交易状态:" + payQueryParamVO.toString());
		SortedMap<String, Object> sortedMap = new TreeMap<>();
		PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
		sortedMap.put("serviceName", PayConstants.JuhePlatformEnum.TRADE_QUERY.getValue());
		sortedMap.put("cpId", JuhePayConfig.JUHEPAY_CPID);
		sortedMap.put("inputCharset", "UTF-8");
		sortedMap.put("signType", "MD5");
		sortedMap.put("cpOrderId", payQueryParamVO.getTransCode());
		sortedMap.put("transId", payQueryParamVO.getTradeNo());
		sortedMap.put("nonceStr", GetWeChatUtil.getRandomStr(32));
		String sign = createSign(sortedMap, JuhePayConfig.JUHEPAY_MD5_KEY);
		sortedMap.put("sign", sign);
		try {
			String responseStr = HttpUtil.doPost(JuhePayConfig.JUHEPAY_URL, sortedMap);
			JSONObject jsonObject = JSONObject.parseObject(responseStr);
			if (jsonObject.get("errorMsg") != null) {
				throw new Exception(jsonObject.get("errorMsg").toString());
			}
			payQueryResultVO.setAttach(jsonObject.getString("attach"));
			payQueryResultVO.setArriveTime(DateUtil.convertDateToStr(DateUtil.convertStrToDate(jsonObject.getString("transTime"), DateUtil.DATE_FORMAT_NUM)));
			payQueryResultVO.setOrderCode(jsonObject.getString("cpOrderId"));
			payQueryResultVO.setTradeNo(jsonObject.getString("transId"));
			// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
			payQueryResultVO.setTradeStatus(getPayStatus(jsonObject.getString("status")));
			String totalFee = jsonObject.getString("totalFee");
			double tf = Double.valueOf(totalFee) / 100;
			payQueryResultVO.setTotalAmount(String.valueOf(tf));
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(sortedMap)) {
				sortedMap.clear();
			}
		}
		return ResultBO.ok(payQueryResultVO);
	}

	/**  
	* 方法说明: 获取具体服务接口
	* @auth: xiongJinGang
	* @param paymentInfo
	* @param packageParams
	* @param takenPlatformEnum
	* @time: 2017年11月24日 下午2:30:10
	* @return: void 
	*/
	private static String getServiceName(PaymentInfoBO paymentInfo, String platform) {
		PayTypeThirdEnum payType = PayConstants.PayTypeThirdEnum.getEnum(paymentInfo.getPayType());
		switch (platform) {
		case PayAbstract.PLATFORM_WEB:
			switch (payType) {
			case ALIPAY_PAYMENT:// 支付宝支付
				return PayConstants.JuhePlatformEnum.ALIPAY_NATIVE.getValue();
			case WEIXIN_PAYMENT:// 微信支付
				return PayConstants.JuhePlatformEnum.WEIXIN_NATIVE.getValue();
			case QQ_PAYMENT:// QQ钱包支付
				return PayConstants.JuhePlatformEnum.QQ_PAY.getValue();
			case JD_PAYMENT:// 京东支付
				return PayConstants.JuhePlatformEnum.JD_PAY.getValue();
			default:
				return null;
			}
		case PayAbstract.PLATFORM_WAP:
			switch (payType) {
			case WEIXIN_PAYMENT:// 微信支付
				return PayConstants.JuhePlatformEnum.WEIXIN_WAP.getValue();
			case JD_PAYMENT:// 京东支付
				return PayConstants.JuhePlatformEnum.JD_WAP.getValue();
			default:
				return null;
			}
		case PayAbstract.PLATFORM_APP:
			switch (payType) {
			case WEIXIN_PAYMENT:// 微信支付
				return PayConstants.JuhePlatformEnum.WEIXIN_WAP.getValue();
			case JD_PAYMENT:// 京东支付
				return PayConstants.JuhePlatformEnum.JD_APP.getValue();
			default:
				return null;
			}
		}
		return null;
	}

	/**  
	* 方法说明: 获取支付状态
	* @auth: xiongJinGang
	* @param payNotifyResult
	* @param status
	* @time: 2017年11月24日 下午3:20:03
	* @return: void 
	*/
	private static PayStatusEnum getPayStatus(String status) {
		// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
		if ("0".equals(status)) {
			return PayStatusEnum.PAYMENT_SUCCESS;
		} else if ("2".equals(status)) {
			return PayStatusEnum.WAITTING_PAYMENT;
		} else if ("3".equals(status)) {
			return PayStatusEnum.USER_CANCELLED_PAYMENT;
		} else if ("1".equals(status)) {
			return PayStatusEnum.PAYMENT_FAILURE;
		} else if ("4".equals(status)) {
			return PayStatusEnum.REFUND;
		}
		return null;
	}

	/**  
	* 方法说明: 生成签名
	* @auth: xiongJinGang
	* @param packageParams
	* @param key
	* @time: 2017年11月24日 下午3:28:33
	* @return: String 
	*/
	public static String createSign(SortedMap<String, Object> packageParams, String key) {
		StringBuffer sb = new StringBuffer();
		Set<Entry<String, Object>> es = packageParams.entrySet();
		Iterator<Entry<String, Object>> it = es.iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> entry = it.next();
			String k = (String) entry.getKey();
			String v = (String) entry.getValue();
			if (null != v && !"".equals(v) && !"sign".equals(k) && !"key".equals(k)) {
				sb.append(k + "=" + v + "&");
			}
		}
		sb.append("key=" + key);
		String toSing = sb.toString();
		String sign = Md5Util.md5_32(toSing).toUpperCase();
		return sign;

	}
}
