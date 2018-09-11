package com.hhly.paycore.paychannel.national.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.national.config.NationalConfig;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeThirdEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 国连支付工具类
 * @author xiongJinGang
 * @date 2018年8月4日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class NationalUtil {
	private static Logger logger = Logger.getLogger(NationalUtil.class);
	public static final String SUCCESS = "1";// 成功标志1成功 0失败

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年6月13日 上午10:19:14
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO, String platform) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		try {
			map.put("partner", NationalConfig.NATIONAL_UID);// 商户号
			map.put("sdorderno", paymentInfoBO.getNoOrder());// 订单号
			map.put("paymoney", paymentInfoBO.getMoneyOrder());// 订单金额，单位元，小数位最末位不能是0；
			map.put("returnurl", paymentInfoBO.getUrlReturn());// 同步地址
			map.put("notifyurl", paymentInfoBO.getNotifyUrl());// 异步地址

			PayTypeThirdEnum payType = PayConstants.PayTypeThirdEnum.getEnum(paymentInfoBO.getPayType());
			map.put("paytype", getPayType(payType, platform));// 支付类型
			if (!checkBankCanPay(payType, paymentInfoBO.getBankSimpleCode())) {
				logger.info("国连网银直连支付参数错误，银行编码为空，不能发起支付请求");
				return null;
			}
			map.put("bankcode", paymentInfoBO.getBankSimpleCode());// 网银直连不可为空，其他支付方式可为空

			// String needSign = BuildRequestFormUtil.createLinkString(map);
			// notifyurl={value}&partner={value}&paymoney={value}&returnurl={value}&sdorderno={value}&&apikey
			StringBuffer sb = new StringBuffer("notifyurl=" + map.get("notifyurl"));
			sb.append("&partner=");
			sb.append(map.get("partner"));
			sb.append("&paymoney=");
			sb.append(map.get("paymoney"));
			sb.append("&returnurl=");
			sb.append(map.get("returnurl"));
			sb.append("&sdorderno=");
			sb.append(map.get("sdorderno"));
			String md5Secret = sb.toString() + "&" + NationalConfig.NATIONAL_SECRET;
			String sign = DigestUtils.md5Hex(md5Secret);
			map.put("sign", sign);// MD5签名结果
		} catch (Exception e) {
			logger.error("拼装国连支付参数异常：", e);
		}
		return map;
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
			logger.info("国连支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			String sign = map.get("sign");
			map.remove("sign");
			map.remove("remark");
			String link = BuildRequestFormUtil.createLinkString(map);
			String newSign = DigestUtils.md5Hex(link + "&" + NationalConfig.NATIONAL_SECRET);
			if (!sign.equals(newSign)) {
				logger.info("国连支付异步通知，验证签名不通过，待签名串：" + map);
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			// 状态码88：成功 其它为失败
			payNotifyResult.setOrderCode(map.get("sdorderno"));// 商户唯一订单号
			payNotifyResult.setThirdTradeNo(map.get("sdpayno"));// 平台订单号
			payNotifyResult.setOrderAmt(Double.valueOf(map.get("paymoney")));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
			payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
			payNotifyResult.setResponse("success");
			if (map.get("status").equals(SUCCESS)) {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
			return ResultBO.ok(payNotifyResult);
		}
		logger.error("国连支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("partner", NationalConfig.NATIONAL_UID);// 商户号
		map.put("sdorderno", payQueryParamVO.getTransCode());// 订单号
		map.put("reqtime", DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));// 订单金额，单位元，小数位最后一位不能是 0；
		StringBuffer sb = new StringBuffer("partner=" + map.get("partner"));
		sb.append("&sdorderno=");
		sb.append(map.get("sdorderno"));
		sb.append("&reqtime=");
		sb.append(map.get("reqtime"));
		sb.append("&" + NationalConfig.NATIONAL_SECRET);
		String sign = DigestUtils.md5Hex(sb.toString());
		map.put("sign", sign);// MD5签名结果
		try {
			logger.info("查询国连支付请求参数：" + map);
			String json = HttpUtil.doPost(NationalConfig.NATIONAL_URL + "apiorderquery", map);
			if (ObjectUtil.isBlank(json)) {
				logger.info("查询国连支付结果返回空");
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			logger.info("查询国连支付结果返回：" + decode(json) + "，status描述：1 支付成功");
			// {"status":1,"msg":"成功订单","sdorderno":"商户订单号","paymoney":"订单金额","sdpayno":"平台订单号"}
			JSONObject jsonObject = JSON.parseObject(json);
			String status = jsonObject.getString("status");
			if (status.equals("1")) {
				PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
				payQueryResultVO.setTotalAmount(jsonObject.getString("paymoney"));
				payQueryResultVO.setTradeNo(jsonObject.getString("sdpayno"));
				payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
				payQueryResultVO.setOrderCode(jsonObject.getString("sdorderno"));
				payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
				return ResultBO.ok(payQueryResultVO);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
	}

	/**  
	* 方法说明: 获取支付方式
	* @auth: xiongJinGang
	* @param rechargeChannel
	* @param platform
	* @time: 2018年8月4日 下午4:23:41
	* @return: String 
	*/
	public static String getPayType(PayTypeThirdEnum payType, String platform) {
		String str = null;
		switch (payType) {
		case ALIPAY_PAYMENT:// 支付宝支付
			if (platform.equals(PayAbstract.PLATFORM_WEB)) {
				str = "alipay";
			} else {
				str = "alipaywap";
			}
			break;
		case WEIXIN_PAYMENT:// 微信支付
			if (platform.equals(PayAbstract.PLATFORM_WEB)) {
				str = "weixin";
			} else {
				str = "wxh5";
			}
			break;
		case BANK_DEBIT_CARD_PAYMENT:// 网银
		case BANK_CREDIT_CARD_PAYMENT://
			str = "bank";
			break;
		case QUICK_DEBIT_CARD_PAYMENT:// 网银
		case QUICK_CREDIT_CARD_PAYMENT://
			str = "quickbank";
			break;
		default:
			break;
		}
		return str;
	}

	/**  
	* 方法说明:网银直连一定需要银行编码 
	* @auth: xiongJinGang
	* @param payType
	* @param bankCode
	* @time: 2018年8月11日 上午10:07:28
	* @return: boolean 
	*/
	public static boolean checkBankCanPay(PayTypeThirdEnum payType, String bankCode) {
		boolean flag = true;
		switch (payType) {
		case BANK_DEBIT_CARD_PAYMENT:// 网银
		case BANK_CREDIT_CARD_PAYMENT://
		case QUICK_DEBIT_CARD_PAYMENT:// 网银
		case QUICK_CREDIT_CARD_PAYMENT://
			if (ObjectUtil.isBlank(bankCode)) {
				flag = false;
			}
			break;
		default:
			break;
		}
		return flag;
	}

	/**  
	* 方法说明: unicode转中文
	* @auth: xiongJinGang
	* @param str
	* @time: 2018年8月8日 下午5:50:21
	* @return: String 
	*/
	public static String decode(String str) {
		Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
		Matcher matcher = pattern.matcher(str);
		char ch;
		while (matcher.find()) {
			// group 6728
			String group = matcher.group(2);
			// ch:'木' 26408
			ch = (char) Integer.parseInt(group, 16);
			// group1 \u6728
			String group1 = matcher.group(1);
			str = str.replace(group1, ch + "");
		}
		return str;
	}
}
