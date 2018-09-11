package com.hhly.paycore.paychannel.palmpay.util;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.palmpay.config.PalmPayConfig;
import com.hhly.paycore.paychannel.palmpay.web.PalmPayWapService;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.utils.BuildRequestFormUtil;

public class PalmUtil {
	private static Logger logger = Logger.getLogger(PalmPayWapService.class);

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月16日 上午9:27:33
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO, String appId) {
		Map<String, String> map = new HashMap<String, String>();
		try {
			map.put("partner_id", PalmPayConfig.PALM_PARTNER_ID);// 商户ID
			map.put("app_id", appId);// 应⽤用ID

			String wapType = getWapType(paymentInfoBO);
			map.put("wap_type", wapType);// 支付方式 1 微信H5 2 支付宝H5 3 银联H5 4 微信扫码 5 微信公众号 6 QQ钱包 7 QQ钱包公众号 8 支付宝扫码
			if (wapType.equals(WapTypeEnum.BANK_H5.getKey())) {
				map.put("bank_code", paymentInfoBO.getBankCode());// 银行代码 所用银行对应的代码，wap_type为3时使用
			}

			map.put("money", String.valueOf(GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder())));// 总⾦金金额 正整数，以分为单位
			map.put("out_trade_no", paymentInfoBO.getNoOrder());// 商户订单编号 需保证同一app_id下唯一，交宜易结果回调通知将传递此参数，⽀支付结果查询也使⽤用此参数
			map.put("subject", URLEncoder.encode(paymentInfoBO.getNameGoods(), "UTF-8"));// 商品名称 UTF-8编码，需进⾏行行URL encode
			map.put("imei", paymentInfoBO.getUserId());// 用户唯一标识 可以使用手机设备识别码或⽤用户ID等能唯一标识用户的数值
			map.put("return_url", URLEncoder.encode(paymentInfoBO.getUrlReturn(), "UTF-8"));// 支付后返回地址，需进⾏行行URL encode
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return map;
	}

	/**  
	* 方法说明: 查询支付结果
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @param payTypeEnum
	* @time: 2017年9月16日 上午9:28:15
	* @return: ResultBO<?> 
	*/
	public static String queryPayResult(PayQueryParamVO payQueryParamVO) {
		logger.debug("掌宜付查询支付结果开始，请求参数：" + payQueryParamVO.toString());
		TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
		String key = PalmPayConfig.PALM_RECHARGE_KEY;
		String appId = PalmPayConfig.PALM_RECHARGE_APP_ID;
		if (transRechargeBO.getRechargeType().equals(PayConstants.RechargeTypeEnum.PAY.getKey())) {
			key = PalmPayConfig.PALM_PAY_KEY;
			appId = PalmPayConfig.PALM_PAY_APP_ID;
		}

		Map<String, String> map = new HashMap<String, String>();
		map.put("partner_id", PalmPayConfig.PALM_PARTNER_ID);// 商户ID
		map.put("app_id", appId);// 应⽤用ID
		map.put("out_trade_no", payQueryParamVO.getTransCode());// 商户订单编号 需保证同一app_id下唯一，交宜易结果回调通知将传递此参数，⽀支付结果查询也使⽤用此参数

		// 根据字典排序
		String paramStr = BuildRequestFormUtil.createLinkString(map);
		String sign = BuildRequestFormUtil.addSign(paramStr, key, true);
		return paramStr + "&sign=" + sign;
	}

	/**  
	* 方法说明: 支付异步回调
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年9月16日 上午11:31:38
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		try {
			if (ObjectUtil.isBlank(map)) {
				logger.info("掌宜付支付异步返回结果为空");
				return ResultBO.err();
			} else {
				String palmSign = map.get("sign");
				map.remove("sign");
				String paramStr = BuildRequestFormUtil.createLinkString(map);

				String key = PalmPayConfig.PALM_RECHARGE_KEY;
				String qn = map.get("qn");
				if (qn.equals(PalmPayWapService.QN_PAY)) {
					key = PalmPayConfig.PALM_PAY_KEY;
				}

				String sign = BuildRequestFormUtil.addSign(paramStr, key, true);
				if (!palmSign.equalsIgnoreCase(sign)) {
					logger.info("掌宜付异步回调签名验证不过，原始签名：" + palmSign + "，生成签名：" + sign);
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}

				PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
				PayStatusEnum payStatusEnum = PalmUtil.getPalmPayStatus(map.get("code"));
				// 支付结果 SUCCESS 成功支付结果以此为准，商户按此进行后续是否发货操作
				if (PayConstants.PayStatusEnum.PAYMENT_SUCCESS.equals(payStatusEnum)) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
					payNotifyResult.setResponse("0");// 商户系统接收并处理理回调通知后，直接返回0值表示处理理成功
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				payNotifyResult.setOrderCode(map.get("out_trade_no"));// 商户唯一订单号
				String invoiceNo = map.get("invoice_no");// 平台订单编号 平台⾃自动生成，全局唯一
				String tradeNo = map.get("up_invoice_no");// 银⾏行行或微信支付流水号，不不是所有通道或支付方式都提供
				String thirdTradeNo = ObjectUtil.isBlank(tradeNo) ? invoiceNo : tradeNo;
				payNotifyResult.setThirdTradeNo(thirdTradeNo);// 连连支付支付单号

				payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("money"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
				payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
				return ResultBO.ok(payNotifyResult);
			}
		} catch (Exception e) {
			logger.error("处理掌宜付支付异步返回结果异常！", e);
			return ResultBO.err();
		}
	}

	/**  
	* 方法说明: 获取请求方式
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月15日 下午5:44:24
	* @return: String 
	*/
	private static String getWapType(PaymentInfoBO paymentInfoBO) {
		// 支付方式
		String wapType = WapTypeEnum.WEIXIN_H5.getKey();
		if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			if (paymentInfoBO.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())) {
				wapType = WapTypeEnum.ALIPAY_QRCODE.getKey();
			} else {
				wapType = WapTypeEnum.ALIPAY_H5.getKey();
			}
		} else if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			if (paymentInfoBO.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())) {
				wapType = WapTypeEnum.WEIXIN_QRCODE.getKey();
			} else {
				wapType = WapTypeEnum.WEIXIN_H5.getKey();
			}
		} else if (PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			if (paymentInfoBO.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())) {
				wapType = WapTypeEnum.QQ_WALLET.getKey();
			} else {
				wapType = WapTypeEnum.QQ_WALLET.getKey();
			}
		} else {
			wapType = WapTypeEnum.BANK_H5.getKey();
		}
		return wapType;
	}

	/**
	 * @desc 支付方式枚举
	 * 1 微信H5 2 支付宝H5 3 银联H5 4 微信扫码 5 微信公众号 6 QQ钱包 7 QQ钱包公众号 8 支付宝扫码
	 * @author xiongJinGang
	 * @date 2017年9月15日
	 * @company 益彩网络科技公司
	 * @version 1.0
	 */
	public enum WapTypeEnum {
		WEIXIN_H5("1", "微信H5"), // 微信H5
		ALIPAY_H5("2", "支付宝H5"), // 支付宝H5
		BANK_H5("3", "银联H5"), // 银联H5
		WEIXIN_QRCODE("4", "微信扫码"), // 微信扫码
		WEIXIN_PUBLIC("5", "微信公众号"), // 微信公众号
		QQ_WALLET("6", "QQ钱包"), // QQ钱包扫码
		QQ_WALLET_PUBLIC("7", "QQ钱包公众号"), // QQ钱包公众号
		ALIPAY_QRCODE("8", "支付宝扫码"); // 支付宝扫码

		private String key;
		private String value;

		WapTypeEnum(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}
	}

	/**  
	* 方法说明: 掌宜付支付结果状态 
	* @auth: xiongJinGang
	* @param status
	* @time: 2017年9月16日 上午10:11:13
	* @return: PayStatusEnum 
	*/
	public static PayStatusEnum getPalmPayStatus(String status) {
		PayStatusEnum payStatusEnum = null;
		switch (status) {
		case "0":// 支付成功
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "1":// 交易处理理中
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
			break;
		case "2":// 交易未完成
			payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
			break;
		case "3":// 无此订单号
			payStatusEnum = PayStatusEnum.USER_CANCELLED_PAYMENT;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;// 未知状态
			break;
		}
		return payStatusEnum;
	}

	/**  
	* 方法说明: QQ扫码专用，获取header中的Location
	* QQ钱包(wap_type=6) 通道如果需要以扫码⽅方式使⽤用，可以使⽤用以下⽅方法⽣生成⼆二维码图⽚片：
	1. 从接⼝口响应的Header中的Location字段，获取跳转地址
	2. 将跳转地址进⾏行行Url encode处理理后，以uuid参数传给 https://pay.swiftpass.cn/pay/qrcode，如:
	https://pay.swiftpass.cn/pay/qrcode?
	uuid=https%3A%2F%2Fmyun.tenpay.com%2Fmqq%2Fpay%2Fqrcode.html%3F_wv%3D1027%2
	6_bid%3D2183%26t%3D5V29085826a026f7a5afdf88b474f674
	* @auth: xiongJinGang
	* @param url
	* @time: 2017年10月14日 下午4:15:07
	* @return: String 
	*/
	public static String sendGet(String url) {
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 打开和URL之间的连接
			URLConnection connection = realUrl.openConnection();
			// 设置通用的请求属性
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 建立实际的连接
			connection.connect();
			// 获取所有响应头字段
			Map<String, List<String>> map = connection.getHeaderFields();
			// 遍历所有的响应头字段
			for (String key : map.keySet()) {
				logger.info(key + "--->" + map.get(key));
				if (!ObjectUtil.isBlank(key) && key.equals("Location")) {
					result = map.get(key).get(0);
					break;
				}
			}
			// 定义 BufferedReader输入流来读取URL的响应
			/*	in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					result += line;
				}*/
		} catch (Exception e) {
			logger.error("发送GET请求出现异常！", e);
		} finally {
		}
		return result;
	}
}
