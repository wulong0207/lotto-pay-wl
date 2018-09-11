package com.hhly.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.ObjectUtil;

/**
 * @desc 构建请求支付form表单
 * @author xiongJinGang
 * @date 2017年10月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class BuildRequestFormUtil {
	private static final Logger logger = LogManager.getLogger(BuildRequestFormUtil.class);

	/**
	 * 建立请求，以表单HTML形式构造（默认）
	 * @param param 请求参数数组
	 * @param method 提交方式。两个值可选：post、get
	 * @param buttonName 确认按钮显示文字
	 * @return 提交表单HTML文本
	 */
	public static String buildRequest(Map<String, String> param, String method, String buttonName, String action, String charset) {
		if (ObjectUtil.isBlank(charset)) {
			charset = "UTF-8";
		}
		StringBuffer sbHtml = new StringBuffer();
		sbHtml.append("<form id=\"payForm\" name=\"paySubmit\" action=\"" + action + "_input_charset=" + charset + "\" method=\"" + method + "\">");
		for (String key : param.keySet()) {
			String value = param.get(key);
			sbHtml.append("<input type=\"hidden\" name=\"" + key + "\" value=\"" + value + "\"/>");
		}

		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"" + buttonName + "\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['payForm'].submit();</script>");

		return sbHtml.toString();
	}

	/**  
	* 方法说明: 构建支付请求form
	* @auth: xiongJinGang
	* @param param 请求参数map 【必填】
	* @param method 请求方式 get\post 【可空】默认post
	* @param buttonName 按钮名称 【可空】
	* @param action 请求URL 【必填】
	* @time: 2017年10月12日 上午10:03:02
	* @return: String 
	*/
	public static String buildRequest(Map<String, String> param, String method, String buttonName, String action) {
		if (ObjectUtil.isBlank(buttonName)) {
			buttonName = "支付";
		}

		if (ObjectUtil.isBlank(method)) {
			method = "post";
		}

		StringBuffer sbHtml = new StringBuffer();
		sbHtml.append("<form id=\"payForm\" name=\"paySubmit\" action=\"" + action + "\" method=\"" + method + "\">");
		for (String key : param.keySet()) {
			String value = param.get(key);
			sbHtml.append("<input type=\"hidden\" name=\"" + key + "\" value=\"" + value + "\"/>");
		}
		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"" + buttonName + "\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['payForm'].submit();</script>");
		return sbHtml.toString();
	}

	/**  
	* 方法说明: 构建支付请求form，默认post的方式
	* @auth: xiongJinGang
	* @param param
	* @param action
	* @time: 2017年10月12日 上午10:06:11
	* @return: String 
	*/
	public static String buildRequest(Map<String, String> param, String action) {
		return buildRequest(param, null, null, action);
	}

	/**  
	* 方法说明: 拼装请求串
	* @auth: xiongJinGang
	* @param params
	* @time: 2017年9月15日 下午5:43:41
	* @return: String 
	*/
	public static String createLinkString(Map<String, String> params) {
		return sortMapToStr(params, false);
	}

	/**  
	* 方法说明: 拼装加密串
	* @auth: xiongJinGang
	* @param params
	* @param isAll 是否所有的参与签名，true是所有的参数签名
	* @time: 2017年10月19日 下午3:07:47
	* @return: String 
	*/
	public static String createLinkString(Map<String, String> params, boolean isAll) {
		return sortMapToStr(params, isAll);
	}

	/**  
	* 方法说明: map排序并且输出字符串
	* @auth: xiongJinGang
	* @param params
	* @param isAll 是否所有的参与签名，true是所有的参数签名
	* @time: 2017年10月19日 下午3:05:46
	* @return: String 
	*/
	private static String sortMapToStr(Map<String, String> params, boolean isAll) {
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		String prestr = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = String.valueOf(params.get(key));
			// 为空的不参与签名
			if (!isAll && StringUtils.isBlank(value)) {
				continue;
			}
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr = prestr + key + "=" + value;
			} else {
				prestr = prestr + key + "=" + value + "&";
			}
		}
		logger.debug("待加密串：" + prestr);
		return prestr;
	}

	/**  
	* 方法说明: 六度支付，参数排序
	* @auth: xiongJinGang
	* @param params
	* @param signKey
	* @time: 2018年7月5日 下午4:11:16
	* @return: String 
	*/
	public static String sortMapAndCreateStr(Map<String, String> params) {
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		String prestr = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = String.valueOf(params.get(key));
			// 为空的不参与签名
			if (StringUtils.isBlank(value)) {
				continue;
			}
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr += value;
			} else {
				prestr += value + "&";
			}
		}
		return prestr;
	}

	/**  
	* 方法说明: 带连接符的参数排序
	* @auth: xiongJinGang
	* @param params
	* @param needConnectSymbol true 需要连接符,false不需要连接符
	* @time: 2018年3月27日 上午10:41:29
	* @return: String 
	*/
	public static String sortMapToStrConnSymbol(Map<String, String> params, boolean needConnectSymbol) {
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		String prestr = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = params.get(key);
			// 为空的不参与签名
			/*if (StringUtils.isBlank(value)) {
				continue;
			}*/
			// sign不参与签名
			if (key.equals("sign")) {
				continue;
			}
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr = prestr + key + "=" + value;
			} else {
				String symbol = "&";
				if (!needConnectSymbol) {
					symbol = "";
				}
				prestr = prestr + key + "=" + value + symbol;
			}
		}
		logger.debug("待加密串：" + prestr);
		return prestr;
	}

	public static String sortMapToStrConnSymbol(Map<String, String> params, String symbol) {
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		String prestr = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = params.get(key);
			// 为空的不参与签名
			/*if (StringUtils.isBlank(value)) {
				continue;
			}*/
			// sign不参与签名
			if (key.equals("sign")) {
				continue;
			}
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr = prestr + key + ":" + value;
			} else {
				if (StringUtils.isBlank(symbol)) {
					symbol = "";
				}
				prestr = prestr + key + ":" + value + symbol;
			}
		}
		logger.debug("待加密串：" + prestr);
		return prestr;
	}

	/**  
	* 方法说明: 将map转成字符串，并且MD5签名
	* @auth: xiongJinGang
	* @param params 请求map
	* @param addKey 加密原串后面是否需要加 &key=。 true是需求加，false是不需要加
	* @param key 加密key
	* @time: 2017年10月12日 下午4:50:35
	* @return: String 
	*/
	public static String createLinkString(Map<String, String> params, String key, boolean addKey) {
		return addSign(createLinkString(params), key, addKey);
	}

	public static String createLinkString(Map<String, String> params, String key) {
		return addSign(createLinkString(params), key, true);
	}

	/**  
	* 方法说明: MD5签名
	* @auth: xiongJinGang
	* @param paramStr
	* @param key
	* @time: 2017年9月15日 下午5:43:30
	* @return: String 
	*/
	public static String addSign(String paramStr, String key, boolean addKey) {
		if (addKey) {
			paramStr += "&key=" + key;
		} else {
			paramStr += key;
		}
		logger.debug("MD5原串：" + paramStr);
		return DigestUtils.md5Hex(paramStr).toUpperCase();
	}

	// 微信返回订单状态 SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭 REVOKED—已撤销（刷卡支付） USERPAYING--用户支付中 PAYERROR--支付失败(其他原因，如银行返回失败)
	public static PayStatusEnum getPayStatus(String status) {
		PayStatusEnum payStatusEnum = null;
		switch (status) {
		case "SUCCESS":// 支付成功
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "REFUND":// 转入退款
			payStatusEnum = PayStatusEnum.REFUND;
			break;
		case "PART_REFUND":// 部分退款
			payStatusEnum = PayStatusEnum.REFUND;
			break;
		case "NOTPAY":// 未支付
		case "CLOSED":// 已关闭
		case "TIME_OUT":// 订单超时（终态）
		case "REPEALED":// 订单撤销（分账订单退款后查询）
			payStatusEnum = PayStatusEnum.OVERDUE_PAYMENT;
			break;
		case "REVOK":// 已撤销
		case "REVOKED":// 已冲正
		case "REJECT":// 订单拒绝（终态）
			payStatusEnum = PayStatusEnum.USER_CANCELLED_PAYMENT;
			break;
		case "USERPAYING":// 用户支付中
		case "PROCESSING":// 处理中（非终态）
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
			break;
		case "PAYERROR":// 支付失败(其他原因，如银行返回失败)
			payStatusEnum = PayStatusEnum.PAYMENT_FAILURE;
			break;
		case "REVERSE":// 已冲正。 没成功，当银行转账交易出现如通讯超时等异常情况时，交易发起方自动或人工发起银行转账冲正交易，取消原转账交易
		case "REVERSAL":// 冲正
			payStatusEnum = PayStatusEnum.PAYMENT_FAILURE;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;// 未知状态
			break;
		}
		return payStatusEnum;
	}
}
