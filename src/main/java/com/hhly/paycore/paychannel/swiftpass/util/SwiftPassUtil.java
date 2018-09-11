package com.hhly.paycore.paychannel.swiftpass.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.swiftpass.config.SwiftpassPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;

/**
 * @desc 威富通第三方技术公司支付接口调用工具类
 * @author xiongJinGang
 * @date 2017年10月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SwiftPassUtil {
	private static final Logger logger = Logger.getLogger(SwiftPassUtil.class);

	/**
	 * 加载请求参数，以JSON字符串形式返回
	 * @param fee
	 * @param orderSn
	 * @return
	 * @throws Exception
	 */
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO) {
		try {
			// 装载订单信息
			Map<String, String> map = new HashMap<String, String>();
			Long fee = Long.parseLong(GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder()));
			String orderNo = paymentInfoBO.getNoOrder();
			map.put("fee", fee + "");
			map.put("order_sn", orderNo);
			map.put("mchid", SwiftpassPayConfig.SWIFTPASS_PAY_PARTNER_CODE);
			map.put("notify_url", paymentInfoBO.getNotifyUrl());
			// map.put("feepoint", SwiftpassPayConfig.SWIFTPASS_PAY_COUNT_POINT);
			// map.put("callback_url", URLEncoder.encode(paymentInfoBO.getUrlReturn(), "utf-8"));
			// map.put("notify_url", URLEncoder.encode(paymentInfoBO.getNotifyUrl(), "utf-8"));
			// map.put("callback_url", paymentInfoBO.getUrlReturn());

			// 装载签名字符串
			String sign = getSignature(SwiftpassPayConfig.SWIFTPASS_PAY_PARTNER_CODE, fee, orderNo, SwiftpassPayConfig.SWIFTPASS_PAY_KEY);
			map.put("sign", sign);

			// 转换为JSON字符串
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 获取签名字符串
	 * 
	 * @param fee 资费
	 * @param tradeNo 订单号
	 * @return 签名字符串
	 * @throws Exception
	 */
	private static String getSignature(String feePoint, Long fee, String tradeNo, String key) {
		// 拼接待签名字符串
		String sign = StringUtils.joinWith("&", feePoint, fee, tradeNo, key);
		if (logger.isDebugEnabled())
			logger.debug("一比分威富通第三方技术公司的微信WAP接口-拼接待签名字符串：" + sign);

		// 进行MD5加密
		return DigestUtils.md5Hex(sign);
	}

	/**
	 * 对支付通知内容进行签名校验
	 * @param map 通知内容
	 * @return boolean
	 * @throws Exception
	 */
	private static boolean checkSignature(Map<String, String> map) throws Exception {
		// 提取参与签名的参数
		String feePoint = MapUtils.getString(map, "feepoint");
		Long fee = MapUtils.getLong(map, "fee");
		String outTradeNo = MapUtils.getString(map, "out_trade_no");

		// 生成签名
		String newSign = getSignature(feePoint, fee, outTradeNo, SwiftpassPayConfig.SWIFTPASS_PAY_KEY);
		// 提取签名字符串
		String sign = MapUtils.getString(map, "sign");

		// 比较通知签名和生成的签名
		return StringUtils.equals(newSign, sign);
	}

	public static ResultBO<?> payNotify(Map<String, String> map) {
		try {
			if (ObjectUtil.isBlank(map)) {
				logger.info("一比分威富通支付异步返回结果为空");
				return ResultBO.err();
			} else {
				boolean checkResult = checkSignature(map);
				if (!checkResult) {
					logger.info("签名认证失败");
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}

				PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
				String resultCode = map.get("result_code");
				if (resultCode.equals("0")) {// 0 表示成功
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
					payNotifyResult.setResponse("success");// 商户系统接收并处理理回调通知后，直接返回0值表示处理理成功
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}

				payNotifyResult.setOrderCode(map.get("out_trade_no"));// 商户唯一订单号
				String tradeNo = map.get("out_transaction_id");// 第三方订单号
				payNotifyResult.setThirdTradeNo(tradeNo);// 支付单号
				logger.info("支付截止时间：" + map.get("time_end"));
				payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("fee"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
				payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
				return ResultBO.ok(payNotifyResult);
			}
		} catch (Exception e) {
			logger.error("处理一比分威富通支付异步返回结果异常！", e);
			return ResultBO.err();
		}
	}

	/**  
	* 方法说明: SUCCESS—支付成功REFUND—转入退款NOTPAY—未支付CLOSED—已关闭REVERSE—已冲正REVOK—已撤销
	* @auth: xiongJinGang
	* @param status
	* @time: 2017年9月23日 下午4:57:47
	* @return: PayStatusEnum 
	*/
	public static PayStatusEnum getPayStatus(String status) {
		PayStatusEnum payStatusEnum = null;
		switch (status) {
		case "SUCCESS":// 支付成功
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "REFUND":// 转入退款
			payStatusEnum = PayStatusEnum.REFUND;
			break;
		case "NOTPAY":// 未支付
		case "CLOSED":// 已关闭
			payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
			break;
		case "REVOK":// 已撤销
			payStatusEnum = PayStatusEnum.USER_CANCELLED_PAYMENT;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;// 未知状态
			break;
		}
		return payStatusEnum;
	}

}