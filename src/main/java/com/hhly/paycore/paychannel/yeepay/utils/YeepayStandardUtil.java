package com.hhly.paycore.paychannel.yeepay.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.yeepay.config.YeepayConfig;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;

/**
 * @author YiJian
 * @version 1.0
 * @Desc: 易宝 标准收银台工具类
 * @date 2017年5月17日
 * @compay 益彩网络科技有限公司
 */
public class YeepayStandardUtil {
	private static Logger logger = Logger.getLogger(YeepayStandardUtil.class);

	/**
	 * formatString(String text) : 字符串格式化方法
	 */
	public static String formatString(String text) {
		return (text == null ? "" : text.trim());
	}

	/**
	 * getPayURL() : 生成支付链接
	 */
	public static String getPayURL(Map<String, String> params) {
		String p0_Cmd = formatString(params.get("p0_Cmd"));
		String p1_MerId = YeepayConfig.P1_MERID;
		String p2_Order = formatString(params.get("p2_Order"));
		String p3_Amt = formatString(params.get("p3_Amt"));
		String p4_Cur = formatString(params.get("p4_Cur"));
		String p5_Pid = formatString(params.get("p5_Pid"));
		String p6_Pcat = formatString(params.get("p6_Pcat"));
		String p7_Pdesc = formatString(params.get("p7_Pdesc"));
		String p8_Url = formatString(params.get("p8_Url"));
		String p9_SAF = formatString(params.get("p9_SAF"));
		String pa_MP = formatString(params.get("pa_MP"));
		String pd_FrpId = formatString(params.get("pd_FrpId"));
		String pm_Period = formatString(params.get("pm_Period"));
		String pn_Unit = formatString(params.get("pn_Unit"));
		String pr_NeedResponse = formatString(params.get("pr_NeedResponse"));
		String pt_UserName = formatString(params.get("pt_UserName"));
		String pt_PostalCode = formatString(params.get("pt_PostalCode"));
		String pt_Address = formatString(params.get("pt_Address"));
		String pt_TeleNo = formatString(params.get("pt_TeleNo"));
		String pt_Mobile = formatString(params.get("pt_Mobile"));
		String pt_Email = formatString(params.get("pt_Email"));
		String pt_LeaveMessage = formatString(params.get("pt_LeaveMessage"));
		String keyValue = YeepayConfig.KEY_VALUE;

		String[] strArr = new String[] { p0_Cmd, p1_MerId, p2_Order, p3_Amt, p4_Cur, p5_Pid, p6_Pcat, p7_Pdesc, p8_Url, p9_SAF, pa_MP, pd_FrpId, pm_Period, pn_Unit, pr_NeedResponse, pt_UserName, pt_PostalCode, pt_Address, pt_TeleNo, pt_Mobile,
				pt_Email, pt_LeaveMessage };

		logger.debug("params=" + params);
		String hmac = Digest.getHmac(strArr, keyValue);

		try {
			p0_Cmd = URLEncoder.encode(p0_Cmd, "GBK");
			p1_MerId = URLEncoder.encode(p1_MerId, "GBK");
			p2_Order = URLEncoder.encode(p2_Order, "GBK");
			p3_Amt = URLEncoder.encode(p3_Amt, "GBK");
			p4_Cur = URLEncoder.encode(p4_Cur, "GBK");
			p5_Pid = URLEncoder.encode(p5_Pid, "GBK");
			p6_Pcat = URLEncoder.encode(p6_Pcat, "GBK");
			p7_Pdesc = URLEncoder.encode(p7_Pdesc, "GBK");
			p8_Url = URLEncoder.encode(p8_Url, "GBK");
			p9_SAF = URLEncoder.encode(p9_SAF, "GBK");
			pa_MP = URLEncoder.encode(pa_MP, "GBK");
			pd_FrpId = URLEncoder.encode(pd_FrpId, "GBK");
			pm_Period = URLEncoder.encode(pm_Period, "GBK");
			pn_Unit = URLEncoder.encode(pn_Unit, "GBK");
			pr_NeedResponse = URLEncoder.encode(pr_NeedResponse, "GBK");
			pt_UserName = URLEncoder.encode(pt_UserName, "GBK");
			pt_PostalCode = URLEncoder.encode(pt_PostalCode, "GBK");
			pt_Address = URLEncoder.encode(pt_Address, "GBK");
			pt_TeleNo = URLEncoder.encode(pt_TeleNo, "GBK");
			pt_Mobile = URLEncoder.encode(pt_Mobile, "GBK");
			pt_Email = URLEncoder.encode(pt_Email, "GBK");
			pt_LeaveMessage = URLEncoder.encode(pt_LeaveMessage, "GBK");
			hmac = URLEncoder.encode(hmac, "GBK");
		} catch (Exception e) {
			e.printStackTrace();
		}

		String requestURL = YeepayConfig.REQUEST_URL;
		StringBuffer payURL = new StringBuffer();

		payURL.append(requestURL).append("?");
		payURL.append("p0_Cmd=").append(p0_Cmd);
		payURL.append("&p1_MerId=").append(p1_MerId);
		payURL.append("&p2_Order=").append(p2_Order);
		payURL.append("&p3_Amt=").append(p3_Amt);
		payURL.append("&p4_Cur=").append(p4_Cur);
		payURL.append("&p5_Pid=").append(p5_Pid);
		payURL.append("&p6_Pcat=").append(p6_Pcat);
		payURL.append("&p7_Pdesc=").append(p7_Pdesc);
		payURL.append("&p8_Url=").append(p8_Url);
		payURL.append("&p9_SAF=").append(p9_SAF);
		payURL.append("&pa_MP=").append(pa_MP);
		payURL.append("&pd_FrpId=").append(pd_FrpId);
		payURL.append("&pm_Period=").append(pm_Period);
		payURL.append("&pn_Unit=").append(pn_Unit);
		payURL.append("&pr_NeedResponse=").append(pr_NeedResponse);
		payURL.append("&pt_UserName=").append(pt_UserName);
		payURL.append("&pt_PostalCode=").append(pt_PostalCode);
		payURL.append("&pt_Address=").append(pt_Address);
		payURL.append("&pt_TeleNo=").append(pt_TeleNo);
		payURL.append("&pt_Mobile=").append(pt_Mobile);
		payURL.append("&pt_Email=").append(pt_Email);
		payURL.append("&pt_LeaveMessage=").append(pt_LeaveMessage);
		payURL.append("&hmac=").append(hmac);

		logger.info("payURL : " + payURL);

		return (payURL.toString());
	}

	/**
	 * queryByOrder() : 订单查询方法
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> queryByOrder1(Map<String, String> params) throws UnsupportedEncodingException {

		logger.info("##### queryByOrder() #####");

		String p0_Cmd = "QueryOrdDetail";
		String p1_MerId = YeepayConfig.P1_MERID;
		String p2_Order = params.get("p2_Order");
		String keyValue = YeepayConfig.KEY_VALUE;
		String pv_Ver = "3.0";
		String p3_ServiceType = params.get("p3_ServiceType");

		String[] strArr = { p0_Cmd, p1_MerId, p2_Order, pv_Ver, p3_ServiceType };

		String hmac = Digest.getHmac(strArr, keyValue);

		Map<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("p0_Cmd", p0_Cmd);
		queryParams.put("p1_MerId", p1_MerId);
		queryParams.put("p2_Order", p2_Order);
		queryParams.put("pv_Ver", pv_Ver);
		queryParams.put("p3_ServiceType", p3_ServiceType);
		queryParams.put("hmac", hmac);

		String queryURL = YeepayConfig.QUERY_URL;

		logger.info("queryParams : " + queryParams);
		logger.info("queryURL : " + queryURL);

		Map<String, String> queryResult = new HashMap<String, String>();
		String r0_Cmd = "";
		String r1_Code = "";
		String r2_TrxId = "";
		String r3_Amt = "";
		String r4_Cur = "";
		String r5_Pid = "";
		String r6_Order = "";
		String r8_MP = "";
		String rw_RefundRequestID = "";
		String rx_CreateTime = "";
		String ry_FinshTime = "";
		String rz_RefundAmount = "";
		String rb_PayStatus = "";
		String rc_RefundCount = "";
		String rd_RefundAmt = "";
		String hmacFromYeepay = "";
		String hmac_safeFromYeepay = "";
		String hmacError = ""; // 自定义，非接口返回。
		String errorMsg = ""; // 自定义，非接口返回。

		List responseList = null;

		try {
			responseList = HttpUtils.URLGet(queryURL, queryParams);
			logger.info("responseList : " + responseList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (responseList == null) {
			errorMsg = "No data returned!";
		} else {
			Iterator iter = responseList.iterator();
			while (iter.hasNext()) {
				String temp = formatString((String) iter.next());
				if (temp.equals("")) {
					continue;
				}
				int i = temp.indexOf("=");
				int j = temp.length();
				if (i >= 0) {
					String tempKey = temp.substring(0, i);
					String tempValue = URLDecoder.decode(temp.substring(i + 1, j), "GBK");
					if ("r0_Cmd".equals(tempKey)) {
						r0_Cmd = tempValue;
					} else if ("r1_Code".equals(tempKey)) {
						r1_Code = tempValue;
					} else if ("r2_TrxId".equals(tempKey)) {
						r2_TrxId = tempValue;
					} else if ("r3_Amt".equals(tempKey)) {
						r3_Amt = tempValue;
					} else if ("r4_Cur".equals(tempKey)) {
						r4_Cur = tempValue;
					} else if ("r5_Pid".equals(tempKey)) {
						r5_Pid = tempValue;
					} else if ("r6_Order".equals(tempKey)) {
						r6_Order = tempValue;
					} else if ("r8_MP".equals(tempKey)) {
						r8_MP = tempValue;
					} else if ("rw_RefundRequestID".equals(tempKey)) {
						rw_RefundRequestID = tempValue;
					} else if ("rx_CreateTime".equals(tempKey)) {
						rx_CreateTime = tempValue;
					} else if ("ry_FinshTime".equals(tempKey)) {
						ry_FinshTime = tempValue;
					} else if ("rz_RefundAmount".equals(tempKey)) {
						rz_RefundAmount = tempValue;
					} else if ("rb_PayStatus".equals(tempKey)) {
						rb_PayStatus = tempValue;
					} else if ("rc_RefundCount".equals(tempKey)) {
						rc_RefundCount = tempValue;
					} else if ("rd_RefundAmt".equals(tempKey)) {
						rd_RefundAmt = tempValue;
					} else if ("hmac".equals(tempKey)) {
						hmacFromYeepay = tempValue;
					} else if ("hmac_safe".equals(tempKey)) {
						hmac_safeFromYeepay = tempValue;
					}
				}
			}

			String[] stringArr = { r0_Cmd, r1_Code, r2_TrxId, r3_Amt, r4_Cur, r5_Pid, r6_Order, r8_MP, rw_RefundRequestID, rx_CreateTime, ry_FinshTime, rz_RefundAmount, rb_PayStatus, rc_RefundCount, rd_RefundAmt };
			String localHmac = Digest.getHmac(stringArr, keyValue);
			boolean ishmac_safe = verifyCallbackHmac_safe(stringArr, hmac_safeFromYeepay);

			if (!localHmac.equals(hmacFromYeepay) || !ishmac_safe) {
				StringBuffer temp = new StringBuffer();
				for (int i = 0; i < stringArr.length; i++) {
					temp.append(stringArr[i]);
				}
			}
		}

		queryResult.put("r0_Cmd", r0_Cmd);
		queryResult.put("r1_Code", r1_Code);
		queryResult.put("r2_TrxId", r2_TrxId);
		queryResult.put("r3_Amt", r3_Amt);
		queryResult.put("r4_Cur", r4_Cur);
		queryResult.put("r5_Pid", r5_Pid);
		queryResult.put("r6_Order", r6_Order);
		queryResult.put("r8_MP", r8_MP);
		queryResult.put("rw_RefundRequestID", rw_RefundRequestID);
		queryResult.put("rx_CreateTime", rx_CreateTime);
		queryResult.put("ry_FinshTime", ry_FinshTime);
		queryResult.put("rz_RefundAmount", rz_RefundAmount);
		queryResult.put("rb_PayStatus", rb_PayStatus);
		queryResult.put("rc_RefundCount", rc_RefundCount);
		queryResult.put("rd_RefundAmt", rd_RefundAmt);
		queryResult.put("hamcError", hmacError);
		queryResult.put("errorMsg", errorMsg);
		logger.info("queryResult : " + queryResult);
		return (queryResult);
	}

	/**
	 * refundByTrxId() : 单笔订单退款
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> refundByTrxId(Map<String, String> params) throws UnsupportedEncodingException {

		logger.info("##### refundByTrxId() #####");
		logger.info("params : " + params);

		String p0_Cmd = params.get("p0_Cmd");
		String p1_MerId = YeepayConfig.P1_MERID;
		String p2_Order = params.get("p2_Order");
		String pb_TrxId = params.get("pb_TrxId");
		String p3_Amt = params.get("p3_Amt");
		String p4_Cur = params.get("p4_Cur");
		String p5_Desc = params.get("p5_Desc");
		String keyValue = YeepayConfig.KEY_VALUE;

		String[] strArr = { p0_Cmd, p1_MerId, p2_Order, pb_TrxId, p3_Amt, p4_Cur, p5_Desc };
		String hmac = Digest.getHmac(strArr, keyValue);

		Map<String, String> refundParams = new HashMap<String, String>();
		refundParams.put("p0_Cmd", p0_Cmd);
		refundParams.put("p1_MerId", p1_MerId);
		refundParams.put("p2_Order", p2_Order);
		refundParams.put("pb_TrxId", pb_TrxId);
		refundParams.put("p3_Amt", p3_Amt);
		refundParams.put("p4_Cur", p4_Cur);
		refundParams.put("p5_Desc", p5_Desc);
		refundParams.put("hmac", hmac);

		logger.info("refundParams :" + refundParams);

		Map<String, String> refundResult = new HashMap<String, String>();
		String r0_Cmd = "";
		String r1_Code = "";
		String r2_TrxId = "";
		String r3_Amt = "";
		String r4_Cur = "";
		String r4_Order = "";
		String rf_fee = "";
		String hmacFromYeepay = "";
		String hmac_safeFromYeepay = "";
		String hmacError = "";
		String errorMsg = "";

		String refundURL = YeepayConfig.REFUND_URL;
		List responseList = null;

		try {
			// responseList = HttpUtils.URLGet(refundURL, refundParams);
			String result = HttpUtil.doGet(refundURL, refundParams);
			if (ObjectUtil.isBlank(result)) {
				return null;
			}
			responseList = JSON.parseObject(result, new TypeReference<List<?>>() {
			});
			logger.info("responseList : " + responseList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		StringBuffer payURL = new StringBuffer();

		payURL.append(refundURL).append("?");
		payURL.append("p0_Cmd=").append(p0_Cmd);
		payURL.append("&p1_MerId=").append(p1_MerId);
		payURL.append("&p2_Order=").append(p2_Order);
		payURL.append("&pb_TrxId=").append(pb_TrxId);
		payURL.append("&p3_Amt=").append(p3_Amt);
		payURL.append("&p4_Cur=").append(p4_Cur);
		payURL.append("&p5_Desc=").append(p5_Desc);
		payURL.append("&hmac=").append(hmac);

		logger.info("payURL=" + payURL);

		if (ObjectUtil.isBlank(responseList)) {
			errorMsg = "No Data Returned!";
		} else {
			Iterator iter = responseList.iterator();
			while (iter.hasNext()) {
				String temp = formatString((String) iter.next());
				if (temp.equals("")) {
					continue;
				}
				int i = temp.indexOf("=");
				int j = temp.length();
				if (i >= 0) {
					String tempKey = temp.substring(0, i);
					String tempValue = temp.substring(i + 1, j);
					if ("r0_Cmd".equals(tempKey)) {
						r0_Cmd = tempValue;
					} else if ("r1_Code".equals(tempKey)) {
						r1_Code = tempValue;
					} else if ("r2_TrxId".equals(tempKey)) {
						r2_TrxId = tempValue;
					} else if ("r3_Amt".equals(tempKey)) {
						r3_Amt = tempValue;
					} else if ("r4_Cur".equals(tempKey)) {
						r4_Cur = tempValue;
					} else if ("r4_Order".equals(tempKey)) {
						r4_Order = tempValue;
					} else if ("rf_fee".equals(tempKey)) {
						rf_fee = tempValue;
					} else if ("hmac".equals(tempKey)) {
						hmacFromYeepay = tempValue;
					} else if ("hmac_safe".equals(tempKey)) {
						hmac_safeFromYeepay = tempValue;
					}
				}
			}
			String[] stringArr = { r0_Cmd, r1_Code, r2_TrxId, r3_Amt, r4_Cur };
			String localHmac = Digest.getHmac(stringArr, keyValue);
			boolean ishmac_safe = verifyCallbackHmac_safe(stringArr, hmac_safeFromYeepay);

			if (!localHmac.equals(hmacFromYeepay) || !ishmac_safe) {
				hmacError = "Hmac 不匹配！ hmacFromYeepay : " + hmacFromYeepay + "; localHmac : " + localHmac;
				StringBuffer temp = new StringBuffer();
				for (int i = 0; i < stringArr.length; i++) {
					temp.append(stringArr[i]);
				}
			}

		}

		refundResult.put("r0_Cmd", r0_Cmd);
		refundResult.put("r1_Code", r1_Code);
		refundResult.put("r2_TrxId", r2_TrxId);
		refundResult.put("r3_Amt", r3_Amt);
		refundResult.put("r4_Cur", r4_Cur);
		refundResult.put("r4_Order", r4_Order);
		refundResult.put("rf_fee", rf_fee);
		refundResult.put("hmac", hmacFromYeepay);
		refundResult.put("hmac_safe", hmac_safeFromYeepay);
		refundResult.put("hmacError", hmacError);
		refundResult.put("errorMsg", errorMsg);

		logger.info("refundResult : " + refundResult);

		return (refundResult);
	}

	/**
	 * refundQuery() : 退款订单查询方法
	 * <p>
	 * 请求参数说明
	 * p0_Cmd  			- 业务类型，固定值"RefundResults"
	 * p1_MerId			- 商户编号
	 * p2_Order			- 退款请求号
	 * pb_TrxId			- 易宝流水号
	 * keyValue			- 商户密钥
	 * <p>
	 * 返回结果result说明
	 * r0_Cmd			- 业务类型，固定值”RefundResults"
	 * r1_Code          - 查询结果
	 * r2_TrxId         - 易宝流水号
	 * r4_Order         - 退款请求号
	 * refundStatus     - 退款申请结果
	 * refundFrpStatus  - 退至银行状态
	 * hmac				- 签名数据
	 * hmacError		- 自定义，非接口返回
	 * errorMsg			- 自定义，非接口返回
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> refundQuery(Map<String, String> params) throws UnsupportedEncodingException {

		logger.info("##### refundQuery() #####");

		String p0_Cmd = "RefundResults";
		String p1_MerId = YeepayConfig.P1_MERID;
		String p2_Order = formatString(params.get("p2_Order"));
		String pb_TrxId = formatString(params.get("pb_TrxId"));
		String keyValue = YeepayConfig.KEY_VALUE;
		String hmac = Digest.getHmac(new String[] { p0_Cmd, p1_MerId, p2_Order, pb_TrxId }, keyValue);

		Map<String, String> refundQueryParams = new HashMap<String, String>();
		refundQueryParams.put("p0_Cmd", p0_Cmd);
		refundQueryParams.put("p1_MerId", p1_MerId);
		refundQueryParams.put("p2_Order", p2_Order);
		refundQueryParams.put("pb_TrxId", pb_TrxId);
		refundQueryParams.put("hmac", hmac);
		String refundQueryURL = YeepayConfig.REFUND_QUERY_URL;

		Map<String, String> refundQueryResult = new HashMap<String, String>();
		String r0_Cmd = "";
		String r1_Code = "";
		String r2_TrxId = "";
		String r4_Order = "";
		String refundStatus = "";
		String refundFrpStatus = "";
		String hmacFromYeepay = "";
		String hmac_safeFromYeepay = "";
		String hmacError = ""; // 自定义，非接口返回。
		String errorMsg = ""; // 自定义，非接口返回。

		List responseList = null;

		try {
			// responseList = HttpUtils.URLGet(refundQueryURL, refundQueryParams);
			String result = HttpUtil.doGet(refundQueryURL, refundQueryParams);
			if (ObjectUtil.isBlank(result)) {
				return null;
			}
			responseList = JSON.parseObject(result, new TypeReference<List<?>>() {
			});
			logger.info("responseList : " + responseList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (responseList.size() == 0) {
			errorMsg = "No Data Returned!";
		} else {
			Iterator iter = responseList.iterator();
			while (iter.hasNext()) {
				String temp = formatString((String) iter.next());
				if (temp.equals("")) {
					continue;
				}
				int i = temp.indexOf("=");
				int j = temp.length();
				if (i >= 0) {
					String tempKey = temp.substring(0, i);
					String tempValue = temp.substring(i + 1, j);
					if ("r0_Cmd".equals(tempKey)) {
						r0_Cmd = tempValue;
					} else if ("r1_Code".equals(tempKey)) {
						r1_Code = tempValue;
					} else if ("r2_TrxId".equals(tempKey)) {
						r2_TrxId = tempValue;
					} else if ("r4_Order".equals(tempKey)) {
						r4_Order = tempValue;
					} else if ("refundStatus".equals(tempKey)) {
						refundStatus = tempValue;
					} else if ("refundFrpStatus".equals(tempKey)) {
						refundFrpStatus = tempValue;
					} else if ("hmac".equals(tempKey)) {
						hmacFromYeepay = tempValue;
					} else if ("hmac_safe".equals(tempKey)) {
						hmac_safeFromYeepay = tempValue;
					}
				}
			}
			String[] stringArr = { r0_Cmd, r1_Code, r2_TrxId, r4_Order, refundStatus, refundFrpStatus };
			String localHmac = Digest.getHmac(stringArr, keyValue);
			boolean ishmac_safe = verifyCallbackHmac_safe(stringArr, hmac_safeFromYeepay);

			if (!localHmac.equals(hmacFromYeepay) || !ishmac_safe) {
				hmacError = "Hmac 不匹配！ hmacFromYeepay : " + hmacFromYeepay + "; localHmac : " + localHmac;
				StringBuffer temp = new StringBuffer();
				for (int i = 0; i < stringArr.length; i++) {
					temp.append(stringArr[i]);
				}
			}

		}

		refundQueryResult.put("r0_Cmd", r0_Cmd);
		refundQueryResult.put("r1_Code", r1_Code);
		refundQueryResult.put("r2_TrxId", r2_TrxId);
		refundQueryResult.put("r4_Order", r4_Order);
		refundQueryResult.put("refundStatus", refundStatus);
		refundQueryResult.put("refundFrpStatus", refundFrpStatus);
		refundQueryResult.put("hmac", hmacFromYeepay);
		refundQueryResult.put("hmac_safe", hmac_safeFromYeepay);
		refundQueryResult.put("hmacError", hmacError);
		refundQueryResult.put("errorMsg", errorMsg);

		logger.info("refundQueryResult : " + refundQueryResult);

		return (refundQueryResult);
	}

	/**
	 * verifyCallbackHmac() : 验证回调参数是否有效
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static boolean verifyCallbackHmac(String[] stringValue, String hmacFromYeepay) throws UnsupportedEncodingException {

		logger.info("##### verifyCallbackHmac() #####");

		String keyValue = YeepayConfig.KEY_VALUE;

		StringBuffer sourceData = new StringBuffer();
		for (int i = 0; i < stringValue.length; i++) {

			// stringValue[i] = URLDecoder.decode(stringValue[i],"GBK");
			// stringValue[i] = new String(stringValue[i].getBytes("8859_1"),"GB2312");
			if (ObjectUtil.isBlank(stringValue[i])) {
				continue;
			}
			sourceData.append(stringValue[i]);
			logger.info("stringValue ～～～～: " + stringValue[i]);

		}
		logger.info("sourceData ～～～～: " + sourceData);

		String localHmac = Digest.getHmac(stringValue, keyValue);

		StringBuffer hmacSource = new StringBuffer();
		for (int i = 0; i < stringValue.length; i++) {
			hmacSource.append(stringValue[i]);
		}

		return (localHmac.equals(hmacFromYeepay) ? true : false);
	}

	/**
	 * verifyCallbackHmac_safe() : 验证回调安全签名数据是否有效
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static boolean verifyCallbackHmac_safe(String[] stringValue, String hmac_safeFromYeepay) throws UnsupportedEncodingException {
		String keyValue = YeepayConfig.KEY_VALUE;
		StringBuffer sourceData = new StringBuffer();
		for (int i = 0; i < stringValue.length; i++) {
			if (ObjectUtil.isBlank(stringValue[i])) {
				continue;
			}
			sourceData.append(stringValue[i] + "#");
		}

		sourceData = sourceData.deleteCharAt(sourceData.length() - 1);
		logger.info("sourceData ～～～～: " + sourceData.toString());

		String localHmac_safe = Digest.hmacSign(sourceData.toString(), keyValue);
		logger.info("localHmac_safe:" + localHmac_safe);
		StringBuffer hmacSource = new StringBuffer();
		for (int i = 0; i < stringValue.length; i++) {
			hmacSource.append(stringValue[i]);
		}
		return (localHmac_safe.equals(hmac_safeFromYeepay) ? true : false);
	}

	/**
	 * cancelOrder() : 订单撤销方法
	 *
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> cancelOrder(Map<String, String> params) throws UnsupportedEncodingException {

		logger.info("##### cancelOrder() #####");

		String p0_Cmd = formatString(params.get("p0_Cmd"));
		String p1_MerId = YeepayConfig.P1_MERID;
		String pb_TrxId = formatString(params.get("pb_TrxId"));
		String keyValue = YeepayConfig.KEY_VALUE;

		String[] strArr = { p0_Cmd, p1_MerId, pb_TrxId };
		String hmac = Digest.getHmac(strArr, keyValue);

		Map<String, String> cancelParams = new HashMap<String, String>();
		cancelParams.put("p0_Cmd", p0_Cmd);
		cancelParams.put("p1_MerId", p1_MerId);
		cancelParams.put("pb_TrxId", pb_TrxId);
		cancelParams.put("hmac", hmac);

		String cancelOrderURL = YeepayConfig.CANCEL_ORDER_URL;

		logger.info("cancelParams : " + cancelParams);
		logger.info("cancelOrderURL : " + cancelOrderURL);

		Map<String, String> cancelResult = new HashMap<String, String>();
		String r0_Cmd = "";
		String r1_Code = "";
		String hmacFromYeepay = "";
		String hmac_safeFromYeepay = "";
		String errorMsg = "";
		String hmacError = "";

		List responseList = null;

		try {
			// responseList = HttpUtils.URLGet(cancelOrderURL, cancelParams);
			String result = HttpUtil.doGet(cancelOrderURL, cancelParams);
			if (ObjectUtil.isBlank(result)) {
				return null;
			}
			responseList = JSON.parseObject(result, new TypeReference<List<?>>() {
			});
			logger.info("responseList : " + responseList);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (responseList.size() == 0) {
			errorMsg = "No Data Returned!";
		} else {
			Iterator iter = responseList.iterator();
			while (iter.hasNext()) {
				String temp = formatString((String) iter.next());
				if (temp.equals("")) {
					continue;
				}
				int i = temp.indexOf("=");
				int j = temp.length();
				if (i >= 0) {
					String tempKey = temp.substring(0, i);
					String tempValue = temp.substring(i + 1, j);
					if ("r0_Cmd".equals(tempKey)) {
						r0_Cmd = tempValue;
					} else if ("r1_Code".equals(tempKey)) {
						r1_Code = tempValue;
					} else if ("hmac".equals(tempKey)) {
						hmacFromYeepay = tempValue;
					} else if ("hmac_safe".equals(tempKey)) {
						hmac_safeFromYeepay = tempValue;
					}
				}
			}
			String[] stringArr = { r0_Cmd, r1_Code };
			String localHmac = Digest.getHmac(stringArr, keyValue);
			boolean ishmac_safe = verifyCallbackHmac_safe(stringArr, hmac_safeFromYeepay);

			if (!localHmac.equals(hmacFromYeepay) || !ishmac_safe) {
				hmacError = "Hmac 不匹配！ hmacFromYeepay : " + hmacFromYeepay + "; localHmac : " + localHmac;
				StringBuffer temp = new StringBuffer();
				for (int i = 0; i < stringArr.length; i++) {
					temp.append(stringArr[i]);
				}
			}
		}

		cancelResult.put("r0_Cmd", r0_Cmd);
		cancelResult.put("r1_Code", r1_Code);
		cancelResult.put("errorMsg", errorMsg);
		cancelResult.put("hmacError", hmacError);

		logger.info("cancelResult : " + cancelResult);

		return (cancelResult);
	}

	/**  
	* 方法说明: 时间戳转换成时间类型
	* @auth: xiongJinGang
	* @param seconds
	* @param format
	* @time: 2017年7月28日 上午11:07:02
	* @return: String 
	*/
	public static String timeStamp2Date(String seconds, String format) {
		try {
			if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
				return "";
			}
			if (format == null || format.isEmpty()) {
				format = "yyyy-MM-dd HH:mm:ss";
			}
			SimpleDateFormat sdf = new SimpleDateFormat(format);
			return sdf.format(new Date(Long.valueOf(seconds + "000")));
		} catch (NumberFormatException e) {
			return DateUtil.getNow();
		}
	}

	/*
	加签
	 */
	public static String getHmac(Map<String, String> params) {
		String p0_Cmd = formatString(params.get("p0_Cmd"));
		String p1_MerId = YeepayConfig.P1_MERID;
		String p2_Order = formatString(params.get("p2_Order"));
		String p3_Amt = formatString(params.get("p3_Amt"));
		String p4_Cur = formatString(params.get("p4_Cur"));
		String p5_Pid = formatString(params.get("p5_Pid"));
		String p6_Pcat = formatString(params.get("p6_Pcat"));
		String p7_Pdesc = formatString(params.get("p7_Pdesc"));
		String p8_Url = formatString(params.get("p8_Url"));
		String p9_SAF = formatString(params.get("p9_SAF"));
		String pa_MP = formatString(params.get("pa_MP"));
		String pd_FrpId = formatString(params.get("pd_FrpId"));
		String pm_Period = formatString(params.get("pm_Period"));
		String pn_Unit = formatString(params.get("pn_Unit"));
		String pr_NeedResponse = formatString(params.get("pr_NeedResponse"));
		String pt_UserName = formatString(params.get("pt_UserName"));
		String pt_PostalCode = formatString(params.get("pt_PostalCode"));
		String pt_Address = formatString(params.get("pt_Address"));
		String pt_TeleNo = formatString(params.get("pt_TeleNo"));
		String pt_Mobile = formatString(params.get("pt_Mobile"));
		String pt_Email = formatString(params.get("pt_Email"));
		String pt_LeaveMessage = formatString(params.get("pt_LeaveMessage"));
		String keyValue = YeepayConfig.KEY_VALUE;

		String[] strArr = new String[] { p0_Cmd, p1_MerId, p2_Order, p3_Amt, p4_Cur, p5_Pid, p6_Pcat, p7_Pdesc, p8_Url, p9_SAF, pa_MP, pd_FrpId, pm_Period, pn_Unit, pr_NeedResponse, pt_UserName, pt_PostalCode, pt_Address, pt_TeleNo, pt_Mobile,
				pt_Email, pt_LeaveMessage };

		logger.info("params=" + params);
		for (int i = 0; i < strArr.length; i++) {
			System.out.print(strArr[i]);
		}

		return Digest.getHmac(strArr, keyValue);
	}

}
