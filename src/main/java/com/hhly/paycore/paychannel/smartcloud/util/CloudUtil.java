package com.hhly.paycore.paychannel.smartcloud.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.smartcloud.config.CloudConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;

/**
 * @desc 智能云支付
 * @author xiongJinGang
 * @date 2018年8月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class CloudUtil {
	private static Logger logger = Logger.getLogger(CloudUtil.class);
	public static final String SUCCESS = "0";// 成功标志

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年6月13日 上午10:19:14
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("uid", CloudConfig.CLOUD_UID);// 商户号
		map.put("orderid", paymentInfoBO.getNoOrder());// 订单号

		String wapType = null;// wap支付方式
		if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			wapType = "10001";
		} else if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			wapType = "20001";
		}
		map.put("istype", wapType);// 付款方式编号 10001 支付宝 20001 微信
		String price = paymentInfoBO.getMoneyOrder();
		price = rvZeroAndDot(price);
		map.put("price", price);// 订单金额，单位元，小数位最末位不能是0；
		map.put("goodsname", paymentInfoBO.getNameGoods());// 商户名
		map.put("orderuid", "2ncai");// 商品详情
		map.put("return_url", paymentInfoBO.getUrlReturn());// 同步地址
		map.put("notify_url", paymentInfoBO.getNotifyUrl());// 异步地址

		// 签名的拼接顺序：按这个值顺序拼接：goodsname + istype + notify_url + orderid + orderuid + price + return_url + token + uid，再进行 md5 加密（小写）
		StringBuffer sb = new StringBuffer(map.get("goodsname"));
		sb.append(map.get("istype"));
		sb.append(map.get("notify_url"));
		sb.append(map.get("orderid"));
		sb.append(map.get("orderuid"));
		sb.append(map.get("price"));
		sb.append(map.get("return_url"));
		sb.append(CloudConfig.CLOUD_SECRET);
		sb.append(map.get("uid"));
		String sign = DigestUtils.md5Hex(sb.toString());

		map.put("key", sign);// MD5签名结果
		return map;
	}

	public static String rvZeroAndDot(String s) {
		if (s.isEmpty()) {
			return null;
		}
		if (s.indexOf(".") > 0) {
			s = s.replaceAll("0+?$", "");// 去掉多余的0
			s = s.replaceAll("[.]$", "");// 如最后一位是.则去掉
		}
		return s;
	}

	/**  
	* 方法说明: 解析并验证异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2018年1月3日 上午10:29:30
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("智能云支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			// orderid + orderuid + ordno + price + realprice + token，再进行 md5 加密（小写）
			StringBuffer sb = new StringBuffer(map.get("goodsname"));
			sb.append(map.get("orderid"));
			sb.append(map.get("orderuid"));
			sb.append(map.get("ordno"));
			sb.append(map.get("price"));
			sb.append(map.get("realprice"));
			sb.append(CloudConfig.CLOUD_SECRET);
			String sign = DigestUtils.md5Hex(sb.toString());

			if (!map.get("sign").equals(sign)) {
				logger.info("智能云支付异步通知，验证签名不通过，待签名串：" + sb.toString());
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			// 状态码88：成功 其它为失败
			payNotifyResult.setOrderCode(map.get("orderid"));// 商户唯一订单号
			payNotifyResult.setThirdTradeNo(map.get("ordno"));// 平台订单号
			payNotifyResult.setOrderAmt(Double.valueOf(GetWeChatUtil.changeF2Y(map.get("realprice"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
			payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
			payNotifyResult.setResponse("success");
			payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			return ResultBO.ok(payNotifyResult);
		}
		logger.error("智能云支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("uid", CloudConfig.CLOUD_UID);// 商户号
		map.put("orderid", payQueryParamVO.getTransCode());// 订单号
		map.put("price", transRechargeBO.getRechargeAmount() + "");// 订单金额，单位元，小数位最后一位不能是 0；
		map.put("orderuid", "2ncai");// 商品详情

		// 按这个值顺序拼接：uid + orderid + price + orderuid + token，再进 行 md5 加密（小写）
		StringBuffer sb = new StringBuffer(map.get("uid"));
		sb.append(map.get("orderid"));
		sb.append(map.get("price"));
		sb.append(map.get("orderuid"));
		sb.append(CloudConfig.CLOUD_SECRET);
		String sign = DigestUtils.md5Hex(sb.toString());
		map.put("key", sign);// MD5签名结果
		try {
			String json = HttpUtil.doPost(CloudConfig.CLOUD_SAVE_URL + "select", map);
			logger.info("查询智能云wap支付结果返回：" + json + "，status描述：1 支付成功，回调成功 2 支付成功，通知中 3 待支付 4 订单超时，已关闭");
			if (ObjectUtil.isBlank(json)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			// {"ordno":"D160745028264662","orderid":"I18080311505512300002","price":"0.01","realprice":"0.01","orderuid":"2ncai","status":"3","key":"39c240c646f89a8b73fa7e0ef65ab03f","code":200}
			JSONObject jsonObject = JSON.parseObject(json);// 为 200 表示没有错误
			if (jsonObject.getString("code").equals("200")) {
				// 值顺序拼接：uid + orderid + price + realprice + orderuid + ordno + status + token，再进行 md5 加密（小写
				StringBuffer sb2 = new StringBuffer(CloudConfig.CLOUD_UID);
				sb2.append(jsonObject.getString("orderid"));
				sb2.append(jsonObject.getString("price"));
				sb2.append(jsonObject.getString("realprice"));
				sb2.append(jsonObject.getString("orderuid"));
				sb2.append(jsonObject.getString("ordno"));
				sb2.append(jsonObject.getString("status"));
				sb2.append(CloudConfig.CLOUD_SECRET);
				String sign2 = DigestUtils.md5Hex(sb2.toString());
				if (!sign2.equals(jsonObject.getString("key"))) {
					logger.info("查询智能云wap支付结果签名验证不通过");
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}

				PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
				payQueryResultVO.setTotalAmount(jsonObject.getString("realprice"));
				payQueryResultVO.setTradeNo(jsonObject.getString("ordno"));
				payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
				payQueryResultVO.setOrderCode(jsonObject.getString("orderid"));
				String status = jsonObject.getString("status");
				if (status.equals("1") || status.equals("2")) {
					payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else if (status.equals("3")) {
					payQueryResultVO.setTradeStatus(PayStatusEnum.WAITTING_PAYMENT);
				} else if (status.equals("4")) {
					payQueryResultVO.setTradeStatus(PayStatusEnum.OVERDUE_PAYMENT);
				}
				return ResultBO.ok(payQueryResultVO);
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
	}
}
