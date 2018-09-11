package com.hhly.paycore.paychannel.lianlianpay.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.alipay.util.AlipayCore;
import com.hhly.paycore.paychannel.lianlianpay.bean.LianlianNotiryParamVO;
import com.hhly.paycore.paychannel.lianlianpay.bean.LianlianQueryResultVO;
import com.hhly.paycore.paychannel.lianlianpay.config.LianPayConfig;
import com.hhly.paycore.paychannel.lianlianpay.enums.LianlianEnum;
import com.hhly.paycore.paychannel.lianlianpay.enums.LianlianEnum.PayTypeEnum;
import com.hhly.paycore.paychannel.lianlianpay.security.Md5Algorithm;
import com.hhly.paycore.paychannel.lianlianpay.security.TraderRSAUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.constants.PayResultConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;

/**
* 常用工具函数
* @author guoyx e-mail:guoyx@lianlian.com
* @date:2013-6-25 下午05:23:05
* @version :1.0
*
*/
public class LLPayUtil {
	private static final Logger logger = Logger.getLogger(LLPayUtil.class);

	private static final String SUCCESS_CODE = "0000";
	private static final String FAIL_CODE = "9999";
	private static final String QUERY_NO_DATA = "8901";// 无记录

	private static final String SUCCESS_MSG = "交易成功";
	private static final String FAIL_MSG = "交易失败";

	/**
	 * str空判断
	 * @param str
	 * @return
	 * @author guoyx
	 */
	public static boolean isnull(String str) {
		if (null == str || str.equalsIgnoreCase("null") || str.equals("")) {
			return true;
		} else
			return false;
	}

	/**
	 * 获取当前时间str，格式yyyyMMddHHmmss
	 * @return
	 * @author guoyx
	 */
	public static String getCurrentDateTimeStr() {
		SimpleDateFormat dataFormat = new SimpleDateFormat("yyyyMMddHHmmss");
		Date date = new Date();
		String timeString = dataFormat.format(date);
		return timeString;
	}

	/**
	 * 
	 * 功能描述：获取真实的IP地址
	 * @param request
	 * @return
	 * @author guoyx
	 */
	public static String getIpAddr(HttpServletRequest request) {
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		if (!isnull(ip) && ip.contains(",")) {
			String[] ips = ip.split(",");
			ip = ips[ips.length - 1];
		}
		// 转换IP 格式
		if (!isnull(ip)) {
			ip = ip.replace(".", "_");
		}
		return ip;
	}

	/**
	 * 生成待签名串
	 * @param paramMap
	 * @return
	 * @author guoyx
	 */
	public static String genSignData(JSONObject jsonObject) {
		StringBuffer content = new StringBuffer();

		// 按照key做首字母升序排列
		List<String> keys = new ArrayList<String>(jsonObject.keySet());
		Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);
		for (int i = 0; i < keys.size(); i++) {
			String key = (String) keys.get(i);
			if ("sign".equals(key)) {
				continue;
			}
			String value = jsonObject.getString(key);
			// 空串不参与签名
			if (isnull(value)) {
				continue;
			}
			content.append((i == 0 ? "" : "&") + key + "=" + value);

		}
		String signSrc = content.toString();
		if (signSrc.startsWith("&")) {
			signSrc = signSrc.replaceFirst("&", "");
		}
		return signSrc;
	}

	/**
	 * 加签
	 * 
	 * @param reqObj
	 * @param rsa_private
	 * @param md5_key
	 * @return
	 * @author guoyx
	 */
	public static String addSign(JSONObject reqObj, String rsa_private, String md5_key) {
		if (reqObj == null) {
			return "";
		}
		String sign_type = reqObj.getString("sign_type");
		if (LianlianEnum.SignTypeEnum.MD5.getCode().equals(sign_type)) {
			return addSignMD5(reqObj, md5_key);
		} else {
			return addSignRSA(reqObj, rsa_private);
		}
	}

	public static String addSign(String paramStr, String rsa_private, String md5_key, String signType) {
		if (LianlianEnum.SignTypeEnum.MD5.getCode().equals(signType)) {
			return addSignMD5(paramStr, md5_key);
		} else {
			return addSignRSA(paramStr, rsa_private);
		}
	}

	/**
	 * 签名验证
	 * 
	 * @param reqStr
	 * @return
	 */
	public static boolean checkSign(String reqStr, String rsa_public, String md5_key) {
		JSONObject reqObj = JSON.parseObject(reqStr);
		if (reqObj == null) {
			return false;
		}
		String sign_type = reqObj.getString("sign_type");
		if (LianlianEnum.SignTypeEnum.MD5.getCode().equals(sign_type)) {
			return checkSignMD5(reqObj, md5_key);
		} else {
			return checkSignRSA(reqObj, rsa_public);
		}
	}

	/**
	 * RSA签名验证
	 * 
	 * @param reqObj
	 * @return
	 * @author guoyx
	 */
	private static boolean checkSignRSA(JSONObject reqObj, String rsa_public) {

		logger.info("进入商户[" + reqObj.getString("oid_partner") + "]RSA签名验证");
		String sign = reqObj.getString("sign");
		// 生成待签名串
		String sign_src = genSignData(reqObj);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]待签名原串" + sign_src);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]签名串" + sign);
		try {
			if (TraderRSAUtil.checksign(rsa_public, sign_src, sign)) {
				logger.info("商户[" + reqObj.getString("oid_partner") + "]RSA签名验证通过");
				return true;
			} else {
				logger.info("商户[" + reqObj.getString("oid_partner") + "]RSA签名验证未通过");
				return false;
			}
		} catch (Exception e) {
			logger.info("商户[" + reqObj.getString("oid_partner") + "]RSA签名验证异常" + e.getMessage());
			return false;
		}
	}

	/**
	 * MD5签名验证
	 * 
	 * @param signSrc
	 * @param sign
	 * @return
	 * @author guoyx
	 */
	private static boolean checkSignMD5(JSONObject reqObj, String md5_key) {
		logger.info("进入商户[" + reqObj.getString("oid_partner") + "]MD5签名验证");
		String sign = reqObj.getString("sign");
		// 生成待签名串
		String sign_src = genSignData(reqObj);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]待签名原串" + sign_src);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]签名串" + sign);
		sign_src += "&key=" + md5_key;
		try {
			if (sign.equals(Md5Algorithm.getInstance().md5Digest(sign_src.getBytes("utf-8")))) {
				logger.info("商户[" + reqObj.getString("oid_partner") + "]MD5签名验证通过");
				return true;
			} else {
				logger.info("商户[" + reqObj.getString("oid_partner") + "]MD5签名验证未通过");
				return false;
			}
		} catch (UnsupportedEncodingException e) {
			logger.info("商户[" + reqObj.getString("oid_partner") + "]MD5签名验证异常" + e.getMessage());
			return false;
		}
	}

	/**
	 * RSA加签名
	 * 
	 * @param reqObj
	 * @param rsa_private
	 * @return
	 * @author guoyx
	 */
	private static String addSignRSA(JSONObject reqObj, String rsa_private) {
		logger.info("进入商户[" + reqObj.getString("oid_partner") + "]RSA加签名");
		// 生成待签名串
		String sign_src = genSignData(reqObj);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]加签原串" + sign_src);
		try {
			return TraderRSAUtil.sign(rsa_private, sign_src);
		} catch (Exception e) {
			logger.info("商户[" + reqObj.getString("oid_partner") + "]RSA加签名异常" + e.getMessage());
			return "";
		}
	}

	private static String addSignRSA(String needSignStr, String rsa_private) {
		try {
			return TraderRSAUtil.sign(rsa_private, needSignStr);
		} catch (Exception e) {
			logger.info("RSA加签名异常，参数【" + needSignStr + "】" + e.getMessage());
			return "";
		}
	}

	/**
	 * MD5加签名
	 * 
	 * @param reqObj
	 * @param md5_key
	 * @return
	 * @author guoyx
	 */
	private static String addSignMD5(JSONObject reqObj, String md5_key) {
		logger.info("进入商户[" + reqObj.getString("oid_partner") + "]MD5加签名");
		// 生成待签名串
		String sign_src = genSignData(reqObj);
		logger.info("商户[" + reqObj.getString("oid_partner") + "]加签原串" + sign_src);
		sign_src += "&key=" + md5_key;
		try {
			return Md5Algorithm.getInstance().md5Digest(sign_src.getBytes("utf-8"));
		} catch (Exception e) {
			logger.info("商户[" + reqObj.getString("oid_partner") + "]MD5加签名异常" + e.getMessage());
			return "";
		}
	}

	/**  
	* 方法说明: md5加密
	* @auth: xiongJinGang
	* @param needSignStr
	* @param md5_key
	* @time: 2017年9月9日 上午10:31:56
	* @return: String 
	*/
	private static String addSignMD5(String needSignStr, String md5_key) {
		needSignStr += "&key=" + md5_key;
		try {
			return Md5Algorithm.getInstance().md5Digest(needSignStr.getBytes("utf-8"));
		} catch (Exception e) {
			logger.info("MD5加签名异常，参数【" + needSignStr + "】" + e.getMessage());
			return "";
		}
	}

	/**  
	* 方法说明: 构建请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月8日 下午5:35:34
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("oid_partner", LianPayConfig.OID_PARTNER);// 支付交易商户编号
		params.put("sign_type", LianPayConfig.SIGN_TYPE);//

		params.put("busi_partner", LianPayConfig.BUSI_PARTNER);// 商户业务类型
		params.put("version", LianPayConfig.VERSION);// 版本号:1.0
		params.put("user_id", paymentInfoBO.getUserId());// 该用户在商户系统中的唯一编号，要求是该编号在商户系统中唯一标识该用户
		params.put("timestamp", DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));// 时间戳格式：YYYYMMDDH24MISS 14 位数
		params.put("bank_code", paymentInfoBO.getBankCode());// 指定银行网银编号 8 位数字具体对应的银行编号见附录，如果此字段不为空则会直接跳转到相应的银行网银页面。此功能提供给商户的用户直接跳转到指定的银行网银页面，更方便用户进行支付。
		params.put("dt_order", paymentInfoBO.getDtOrder());// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
		params.put("info_order", paymentInfoBO.getInfoOrder());// 订单描述
		params.put("money_order", paymentInfoBO.getMoneyOrder());// 交易金额
		params.put("name_goods", paymentInfoBO.getNameGoods());// 商品名称
		params.put("no_order", paymentInfoBO.getNoOrder());// 商户唯一订单号
		params.put("notify_url", paymentInfoBO.getNotifyUrl());// 服务器异步通知地址
		params.put("url_return", paymentInfoBO.getUrlReturn());// 支付结束回显url
		// 加了下面的风控参数
		params.put("risk_item", LLPayUtil.getRiskItem(paymentInfoBO));// 【快捷支付必填】风险控制参数
		if (!ObjectUtil.isBlank(paymentInfoBO.getUserreqIp())) {
			String userreqIp = paymentInfoBO.getUserreqIp().replace(".", "_");// 用户端申请 IP 请将 IP 中的“.”替换为“_”，例如：IP 是122.11.37.211 的,请转换为 122_11_37_211
			params.put("userreq_ip", userreqIp);
		}
		params.put("valid_order", paymentInfoBO.getValidOrder());// 订单有效时间 分钟为单位，默认为 10080 分钟（7 天），从创建时间开始，过了此订单有效时间此笔订单就会被设置为失败状态不能再重新进行支付。
		params.put("pay_type", paymentInfoBO.getPayType());// 支付方式 1：网银支付（借记卡）8：网银支付（信用卡）9：B2B 企业网银支付
		// params.put("url_order", "");// 【选填】订单地址 合作系统中订单的详情链接地址
		// params.put("risk_item", "");// 【选填】风险控制参数
		return params;
	}

	/**  
	* 方法说明: Wap支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月12日 下午5:28:49
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("oid_partner", LianPayConfig.FAST_OID_PARTNER);// 支付交易商户编号
		params.put("sign_type", LianPayConfig.FAST_SIGN_TYPE);// 采用RSA

		params.put("busi_partner", LianPayConfig.BUSI_PARTNER);// 商户业务类型
		params.put("user_id", paymentInfoBO.getUserId());// 该用户在商户系统中的唯一编号，要求是该编号在商户系统中唯一标识该用户
		params.put("dt_order", paymentInfoBO.getDtOrder());// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
		params.put("info_order", paymentInfoBO.getInfoOrder());// 订单描述
		params.put("money_order", paymentInfoBO.getMoneyOrder());// 交易金额
		params.put("name_goods", paymentInfoBO.getNameGoods());// 商品名称
		params.put("no_order", paymentInfoBO.getNoOrder());// 商户唯一订单号
		params.put("notify_url", paymentInfoBO.getNotifyUrl());// 服务器异步通知地址
		params.put("url_return", paymentInfoBO.getUrlReturn());// 支付结束回显url
		params.put("version", LianPayConfig.WAP_VERSION);// 版本号:1.0
		params.put("app_request", getAppRequest(paymentInfoBO.getPayPlatform()));// 请求应用标识 1-Android 2-ios 3-WAP
		// 下面二个参数一定要有值，""会去签名，导致签名不过
		// params.put("bg_color", "");// 支付页面背景颜色，000000~ffffff 默认值为 ff5001
		// params.put("font_color", "");// 支付页面字体颜色
		params.put("syschnotify_flag", "1");// 0-点击通知 1-主动通知 默认为 0
		params.put("card_no", paymentInfoBO.getCardNo());// 银行卡号前置，卡号可以在商户的页面输入

		params.put("id_type", paymentInfoBO.getIdType());// 默认为 00：身份证或企业经营证件,1: 户口簿，2：护照,3.军官证,4.士兵证，5. 港澳居民来往内地通行证,6. 台湾同胞来往内地通行证,7. 临时身份证,8. 外国人居留证,9. 警官证, X.其他证件，10.组织结构代码
		params.put("id_no", paymentInfoBO.getIdNo());// 证件号码
		params.put("acct_name", paymentInfoBO.getAcctName());// 银行账号姓名
		params.put("flag_modify", "1");// 修改标记 非必须0-可以修改，默认为 01-不允许修改与 id_type,id_no,acct_name 配合使用，如果该用户在商户系统已经实名认证过了，则在绑定银行卡的输入信息不能修改，否则可以修改

		// 加了下面的风控参数
		params.put("risk_item", LLPayUtil.getRiskItem(paymentInfoBO));// 【快捷支付必填】风险控制参数
		params.put("valid_order", paymentInfoBO.getValidOrder());// 订单有效时间 分钟为单位，默认为 10080 分钟（7 天），从创建时间开始，过了此订单有效时间此笔订单就会被设置为失败状态不能再重新进行支付。
		params.put("pay_type", paymentInfoBO.getPayType());// 支付方式 1：网银支付（借记卡）8：网银支付（信用卡）9：B2B 企业网银支付
		return params;
	}

	/**  
	* 方法说明: 构建快捷支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年10月16日 下午5:34:40
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildFastMapParam(PaymentInfoBO paymentInfoBO) {
		logger.info("连连快捷支付开始，请求参数：" + paymentInfoBO.toString());
		// 构建支付所需参数
		Map<String, String> params = LLPayUtil.buildMapParam(paymentInfoBO);
		params.put("oid_partner", LianPayConfig.FAST_OID_PARTNER);// 支付交易商户编号
		params.put("sign_type", LianPayConfig.FAST_SIGN_TYPE);//
		// 加了下面的风控参数，RSA签名验证通不过，先暂时去掉
		params.put("risk_item", LLPayUtil.getRiskItem(paymentInfoBO));// 【快捷支付必填】风险控制参数
		params.put("card_no", paymentInfoBO.getCardNo());// 银行卡号前置，卡号可以在商户的页面输入
		params.put("id_type", paymentInfoBO.getIdType());// 证件类型 0:身份证
		params.put("id_no", paymentInfoBO.getIdNo());// 证件号码
		params.put("acct_name", paymentInfoBO.getAcctName());// 银行账号姓名
		params.put("flag_modify", "1");// 修改标记 非必须0-可以修改，默认为 01-不允许修改与 id_type,id_no,acct_name 配合使用，如果该用户在商户系统已经实名认证过了，则在绑定银行卡的输入信息不能修改，否则可以修改
		return params;
	}

	/**  
	* 方法说明: ios、android支付参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月12日 下午5:32:54
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildAppMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> params = new HashMap<String, String>();
		/************* 参与签名的参数开始 *****************/
		params.put("oid_partner", LianPayConfig.FAST_OID_PARTNER);// 支付交易商户编号
		params.put("sign_type", LianPayConfig.FAST_SIGN_TYPE);// 采用RSA

		params.put("busi_partner", LianPayConfig.BUSI_PARTNER);// 商户业务类型
		params.put("no_order", paymentInfoBO.getNoOrder());// 商户唯一订单号
		params.put("dt_order", paymentInfoBO.getDtOrder());// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
		params.put("name_goods", paymentInfoBO.getNameGoods());// 商品名称
		params.put("info_order", paymentInfoBO.getInfoOrder());// 订单描述
		params.put("money_order", paymentInfoBO.getMoneyOrder());// 交易金额
		params.put("notify_url", paymentInfoBO.getNotifyUrl());// 服务器异步通知地址
		params.put("valid_order", paymentInfoBO.getValidOrder());// 订单有效时间 分钟为单位，默认为 10080 分钟（7 天），从创建时间开始，过了此订单有效时间此笔订单就会被设置为失败状态不能再重新进行支付。
		// 加了下面的风控参数，RSA签名验证通不过，先暂时去掉
		params.put("risk_item", LLPayUtil.getRiskItem(paymentInfoBO));// 【快捷支付必填】风险控制参数
		/************* 参与签名的参数结束 *****************/
		// 根据字典排序
		String paramStr = AlipayCore.createLinkString(params);
		logger.debug("待签名参数【" + paramStr + "】");
		String sign = LLPayUtil.addSign(paramStr, LianPayConfig.FAST_PRI_KEY, LianPayConfig.FAST_MD5_KEY, LianPayConfig.FAST_SIGN_TYPE);
		logger.debug("加密后的sign：" + sign);
		params.put("sign", sign);

		params.put("user_id", paymentInfoBO.getUserId());// 该用户在商户系统中的唯一编号，要求是该编号在商户系统中唯一标识该用户
		params.put("force_bank", paymentInfoBO.getUserId());// 0— 不强制用户使用该银行的银行卡支付 用户可以选择其他银行的银行卡进行支付1— 强制该用户使用该银行的银行卡进行支付（此标志与银行编号配合使用）
		params.put("id_type", paymentInfoBO.getIdType());// 默认为 00：身份证或企业经营证件,1: 户口簿，2：护照,3.军官证,4.士兵证，5. 港澳居民来往内地通行证,6. 台湾同胞来往内地通行证,7. 临时身份证,8. 外国人居留证,9. 警官证, X.其他证件，10.组织结构代码
		params.put("id_no", paymentInfoBO.getIdNo());// 证件号码
		params.put("acct_name", paymentInfoBO.getAcctName());// 银行账号姓名
		params.put("flag_modify", "1");// 修改标记 非必须0-可以修改，默认为 01-不允许修改与 id_type,id_no,acct_name 配合使用，如果该用户在商户系统已经实名认证过了，则在绑定银行卡的输入信息不能修改，否则可以修改
		params.put("card_no", paymentInfoBO.getCardNo());// 银行卡号前置，卡号可以在商户的页面输入

		// 以下3个参数先不填
		// params.put("platform", "");// 平台来源标示。该参数可实现多个商户号之间用户数据共享，该标识填写主商户号即可
		// params.put("no_agree", "");//已经记录快捷银行卡的用户，商户在调用的时候可以与 pay_type 一块配合使用
		// params.put("shareing_data", "");// 分帐信息数据
		params.put("pay_type", paymentInfoBO.getPayType());// 支付方式 1：网银支付（借记卡）8：网银支付（信用卡）9：B2B 企业网银支付
		return params;
	}

	/**  
	* 方法说明: 获取请求平台
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月12日 下午4:19:40
	* @return: void 
	*/
	private static String getAppRequest(Short platformCode) {
		String platform = "3";
		TakenPlatformEnum takenPlatformEnum = PayConstants.TakenPlatformEnum.getByKey(platformCode);
		switch (takenPlatformEnum) {
		case WAP:
			platform = "3";
			break;
		case ANDROID:
			platform = "1";
			break;
		case IOS:
			platform = "2";
			break;
		default:
			break;
		}
		return platform;
	}

	/**  
	* 方法说明: 读取request流
	* @auth: xiongJinGang
	* @param request
	* @time: 2017年9月12日 上午11:10:57
	* @return: String 
	*/
	public static String readReqStr(HttpServletRequest request) {
		BufferedReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "utf-8"));
			String line = null;

			while ((line = reader.readLine()) != null) {
				sb.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != reader) {
					reader.close();
				}
			} catch (IOException e) {

			}
		}
		return sb.toString();
	}

	/**  
	* 方法说明: 连连支付异步通知结果
	* @auth: xiongJinGang
	* @param map
	* @param typeName
	* @time: 2017年9月12日 上午11:03:21
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map, PayTypeEnum payTypeEnum) {
		try {
			String response = map.get(Constants.PAY_RESPONSE_KEY_NAME);
			if (ObjectUtil.isBlank(response)) {
				logger.info("连连" + payTypeEnum.getMsg() + "支付异步返回参数为空");
				return ResultBO.err();
			} else {
				if (payTypeEnum.getCode().equals(LianlianEnum.PayTypeEnum.BANK.getCode())) {
					// 网银支付签名验证
					if (!LLPayUtil.checkSign(response, LianPayConfig.YT_PUB_KEY, LianPayConfig.MD5_KEY)) {
						logger.info("连连" + payTypeEnum.getMsg() + "支付异步返回参数签名验证不过");
						return ResultBO.err();
					}
				} else {
					// 快捷支付、Wap、app签名验证
					if (!LLPayUtil.checkSign(response, LianPayConfig.FAST_PUB_KEY, LianPayConfig.FAST_MD5_KEY)) {
						logger.info("连连" + payTypeEnum.getMsg() + "支付异步返回参数签名验证不过");
						return ResultBO.err();
					}
				}
				PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
				// 解析异步通知对象
				LianlianNotiryParamVO payDataBean = JSON.parseObject(response, LianlianNotiryParamVO.class);
				// 支付结果 SUCCESS 成功支付结果以此为准，商户按此进行后续是否发货操作
				if (PayResultConstants.COMMON_RESULT_SUCCESS.equals(payDataBean.getResult_pay())) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
					payNotifyResult.setResponse(LLPayUtil.responseStr(true));
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				payNotifyResult.setOrderCode(payDataBean.getNo_order());// 商户唯一订单号
				payNotifyResult.setThirdTradeNo(payDataBean.getOid_paybill());// 连连支付支付单号
				payNotifyResult.setOrderAmt(Double.parseDouble(payDataBean.getMoney_order()));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
				String tradeTime = DateUtil.convertStrToTarget(payDataBean.getDt_order(), DateUtil.DATE_FORMAT_NUM, DateUtil.DEFAULT_FORMAT);
				payNotifyResult.setTradeTime(tradeTime);// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
				return ResultBO.ok(payNotifyResult);
			}
		} catch (Exception e) {
			logger.error("处理连连" + payTypeEnum.getMsg() + "支付异步返回结果异常！", e);
			return ResultBO.err();
		}
	}

	/**  
	* 方法说明: 查询支付结果返回（没做签名校验）
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @param typeName
	* @time: 2017年9月12日 上午11:08:19
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> queryPayResult(PayQueryParamVO payQueryParamVO, PayTypeEnum payTypeEnum) {
		logger.debug("连连" + payTypeEnum.getMsg() + "支付结果查询开始，请求参数：" + payQueryParamVO.toString());
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("oid_partner", LianPayConfig.OID_PARTNER);// 商户编号
		jsonObject.put("sign_type", LianPayConfig.SIGN_TYPE);// 签名方式
		jsonObject.put("no_order", payQueryParamVO.getTransCode());// 商户唯一订单号
		jsonObject.put("dt_orde", DateUtil.convertDateToStr(payQueryParamVO.getOrderTime(), DateUtil.DATE_FORMAT_NUM));// 商户订单时间 格式：YYYYMMDDH24MISS
		// jsonObject.put("oid_paybill", "");//【选填】连连支付支付单号
		jsonObject.put("query_version", "1.1");// 查询版本号 不传就是老版本 默认 1.01.1 版本 查询新增 memo 字段、银行名称bank_name 字段
		String sign = LLPayUtil.addSign(jsonObject, LianPayConfig.TRADER_PRI_KEY, LianPayConfig.MD5_KEY);
		jsonObject.put("sign", sign);// 签名

		String queryParam = jsonObject.toJSONString();
		logger.debug("查询连连" + payTypeEnum.getMsg() + "支付结果请求参数：" + queryParam);
		// HttpRequestSimple httpClient = HttpRequestSimple.getInstance();
		// String queryResult = httpClient.postSendHttp(LianPayConfig.QUERY_URL, queryParam);
		String queryResult = null;
		try {
			queryResult = HttpUtil.doPost(LianPayConfig.QUERY_URL, queryParam);
		} catch (Exception e) {
			logger.error("查询连连支付结果异常：", e);
			return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
		}

		logger.info("查询【" + payQueryParamVO.getTransCode() + "】支付结果返回：" + queryResult);
		LianlianQueryResultVO llResult = JSON.parseObject(queryResult, LianlianQueryResultVO.class);

		PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
		// 查询成功
		if (llResult.getRet_code().equals(SUCCESS_CODE)) {
			// 支付结果
			payQueryResultVO.setTradeNo(llResult.getOid_paybill());
			payQueryResultVO.setOrderCode(llResult.getNo_order());
			payQueryResultVO.setBuyerAccount("");// 买家账号
			payQueryResultVO.setTradeStatus(LLPayUtil.getTransResult(llResult.getResult_pay()));
			payQueryResultVO.setTotalAmount(llResult.getMoney_order());// 交易的订单金额，单位为元，两位小数
			payQueryResultVO.setReceiptAmount(llResult.getMoney_order());
			payQueryResultVO.setPayAmount(llResult.getMoney_order());
			String targetDate = DateUtil.convertStrToTarget(llResult.getDt_order(), DateUtil.DATE_FORMAT_NUM, DateUtil.DEFAULT_FORMAT);
			payQueryResultVO.setArriveTime(targetDate);
			payQueryResultVO.setReturnMsg(llResult.getRet_msg());
			// payQueryResultVO.setTradeType("");
			payQueryResultVO.setBankType(llResult.getPay_type());
			payQueryResultVO.setBankName(llResult.getBank_name());
			// payQueryResultVO.setAttach("");
			return ResultBO.ok(payQueryResultVO);
		} else {
			if (llResult.getRet_code().equals(QUERY_NO_DATA)) {
				return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
			}
			return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
		}
	}

	/**  
	* 方法说明: 返回第三方支付结果
	* @auth: xiongJinGang
	* @param result
	* @time: 2017年9月9日 上午11:50:24
	* @return: String 
	*/
	public static String responseStr(boolean result) {
		JSONObject responseObj = new JSONObject();
		responseObj.put("ret_code", result ? SUCCESS_CODE : FAIL_CODE);
		responseObj.put("ret_msg", result ? SUCCESS_MSG : FAIL_MSG);
		return responseObj.toJSONString();
	}

	/** 
	* @Title: getRiskItem 
	* @Description: 连连快捷 风险控制参数
	* @time 2017年4月13日 下午3:22:12
	*/
	public static String getRiskItem(PaymentInfoBO paymentInfoBO) {
		JSONObject riskItemObj = new JSONObject();
		// 基本风控参数
		riskItemObj.put("frms_ware_category", "1007");// 商品类目 彩票
		String registerTime = "";
		if (ObjectUtil.isBlank(paymentInfoBO.getRegisterTime())) {
			registerTime = DateUtil.getNow(DateUtil.DATE_FORMAT_NUM);
		} else {
			registerTime = DateUtil.convertDateToStr(paymentInfoBO.getRegisterTime(), DateUtil.DATE_FORMAT_NUM);
		}
		riskItemObj.put("user_info_dt_register", registerTime);// 注册时间 YYYYMMDDH24MISS
		riskItemObj.put("user_info_mercht_userno", paymentInfoBO.getUserId());// 商户用户唯一标识
		if (ObjectUtil.isBlank(paymentInfoBO.getMobilePhone())) {
			riskItemObj.put("user_info_bind_phone", paymentInfoBO.getMobilePhone());// 绑定手机号 如有，需要传送
		}
		// 行业 商户风控参数
		riskItemObj.put("user_info_full_name", paymentInfoBO.getAcctName());// 用户注册姓名
		// 0：身份证或企业经营证件1：户口簿，2：护照3：军官证,4：士兵证5： 港澳居民来往内地通行证6：台湾同胞来往内地通行证7： 临时身份证8： 外国人居留证9： 警官证X：其他证件默认为：0
		riskItemObj.put("user_info_id_type", "0");// 用户注册证件类型
		riskItemObj.put("user_info_id_no", paymentInfoBO.getIdNo());// 用户注册证件号码
		// 1：:是 0：无认证商户自身是否对用户信息进行实名认证。默认：0
		riskItemObj.put("user_info_identify_state", "1");// 是否实名认证
		// 是实名认证时，必填1：银行卡认证2：现场认证3：身份证远程认证4：其它认证
		riskItemObj.put("user_info_identify_type", "1");// 实名认证方式

		return riskItemObj.toString();
	}

	/**  
	* 方法说明: 获取交易结果
	* SUCCESS 成功WAITING 等待支付PROCESSING 银行支付处理中REFUND 退款FAILURE 失败支付结果以此为准，商户按此进行后续是否发货操作
	* @auth: xiongJinGang
	* @param resultCode
	* @time: 2017年9月9日 下午3:40:23
	* @return: void 
	*/
	public static PayStatusEnum getTransResult(String resultCode) {
		PayStatusEnum payStatusEnum = null;
		switch (resultCode) {
		case "SUCCESS":
			payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			break;
		case "WAITING":
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
			break;
		case "PROCESSING":
			payStatusEnum = PayStatusEnum.BEING_PAID;
			break;
		case "REFUND":
			payStatusEnum = PayStatusEnum.REFUND;
			break;
		case "FAILURE":
			payStatusEnum = PayStatusEnum.PAYMENT_FAILURE;
			break;
		default:
			payStatusEnum = PayStatusEnum.WAITTING_PAYMENT;
			break;
		}
		return payStatusEnum;
	}

	public static String buildWebRequest(String payGateway, Map<String, String> sParaTemp, String strMethod, String charset) {
		List<String> keys = new ArrayList<String>(sParaTemp.keySet());
		StringBuffer sbHtml = new StringBuffer();
		sbHtml.append("<form id=\"lianliansubmit\" name=\"lianliansubmit\" action=\"" + payGateway + "\"" + "_input_charset=" + charset + "\" method=\"" + strMethod + "\">");

		for (int i = 0; i < keys.size(); i++) {
			String name = (String) keys.get(i);
			String value = (String) sParaTemp.get(name);

			sbHtml.append("<input type='hidden' name='" + name + "' value='" + value + "'/>");
		}

		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"确认\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['lianliansubmit'].submit();</script>");

		return sbHtml.toString();
	}

	public static String buildWapRequest(String payGateway, Map<String, String> sParaTemp, String strMethod, String charset) {
		StringBuffer sbHtml = new StringBuffer();
		sbHtml.append("<form id=\"lianliansubmit\" name=\"lianliansubmit\" action=\"" + payGateway + "\" method=\"" + strMethod + "\">");
		String reqJson = JSON.toJSONString(sParaTemp);
		sbHtml.append("<input type=\"hidden\" name=\"req_data\" value='" + reqJson + "'/>");

		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"确认\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['lianliansubmit'].submit();</script>");
		return sbHtml.toString();
	}
}
