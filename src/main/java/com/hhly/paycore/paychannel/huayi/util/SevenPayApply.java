package com.hhly.paycore.paychannel.huayi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.huayi.config.SevenPayConfig;

/**
 * @project STC_DEMO
 */
public class SevenPayApply {
	private static Logger logger = LoggerFactory.getLogger(SevenPayApply.class);

	/**
	 * 建立请求
	 * @param sParaTemp 请求参数数组
	 * @param strMethod 提交方式。两个值可选：post、get
	 * @param strButtonName 确认按钮显示文字
	 * @return 唤起支付地址
	 * @throws Exception 
	 */
	public static JSONObject buildRequest(Map<String, String> sParaTemp, String strMethod, String sevenPaySubmitUrl) throws Exception {

		// 参数签名
		Map<String, String> sPara = buildRequestPara(sParaTemp);

		// 请求服务
		CloseableHttpClient httpclient = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(sevenPaySubmitUrl);

		// 拼接参数
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		List<String> keys = new ArrayList<String>(sPara.keySet());
		for (String key : keys) {
			nvps.add(new BasicNameValuePair(key, sPara.get(key)));
		}

		httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		CloseableHttpResponse response = httpclient.execute(httpPost);
		try {
			System.out.println(response.getStatusLine());
			HttpEntity entity = response.getEntity();
			// do something useful with the response body
			String resultStr = EntityUtils.toString(entity);
			// logger.info("华移支付请求返回：" + resultStr);

			// 签名验证
			JSONObject result = JSONObject.parseObject(resultStr);
			Map<String, String> resultMap = (Map) result;

			String prestr = SevenPayHelper.createLinkString(SevenPayHelper.paraFilter(resultMap));
			if (!RSAUtils.verify(prestr.getBytes("UTF-8"), RSAUtils.getSevenPayPubKey(), resultMap.get("signMsg"))) {
				throw new Exception("七分钱签名验证不通过！");
			}

			// and ensure it is fully consumed
			// 消耗掉 response
			EntityUtils.consume(entity);
			return result;
		} finally {
			response.close();
		}

	}

	/**
	 * 生成自动跳转的form表单
	 * @param strMethod
	 * @param sevenPaySubmitUrl
	 * @return
	 */
	public static String buildRequestForm(String strMethod, String sevenPaySubmitUrl) {
		StringBuffer sbHtml = new StringBuffer();

		sbHtml.append("<form id=\"sevenpaysubmit\" name=\"sevenpaysubmit\" action=\"" + sevenPaySubmitUrl + "\" method=\"" + strMethod + "\">");
		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"提交\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['sevenpaysubmit'].submit();</script>");

		return sbHtml.toString();
	}

	/**
	 * 生成要请求给七分钱的参数数组
	 * @param sParaTemp 请求前的参数数组
	 * @return 要请求的参数数组
	 */
	public static Map<String, String> buildRequestPara(Map<String, String> sParaTemp) {
		// 除去数组中的空值和签名参数
		Map<String, String> sPara = SevenPayHelper.paraFilter(sParaTemp);

		// 生成签名结果
		String mysign = buildRequestMysign(sPara);

		// 签名结果与签名方式加入请求提交参数组中
		sPara.put("signMsg", mysign);
		sPara.put("signType", SevenPayConfig.SIGN_TYPE);

		return sPara;
	}

	/**
	 * 生成签名结果
	 * @param sPara 要签名的数组
	 * @return 签名结果字符串
	 */
	public static String buildRequestMysign(Map<String, String> sPara) {
		String prestr = SevenPayHelper.createLinkString(sPara); // 把数组所有元素，按照“参数=参数值”的模式用“&”字符拼接成字符串
		System.out.println("代签名字符串：" + prestr);
		String mysign = "";
		if (SevenPayConfig.SIGN_TYPE.equals("RSA")) {
			try {
				mysign = RSAUtils.sign(prestr.getBytes("UTF-8"), null);
				System.out.println("签名：" + mysign);

				// 校验签名
				boolean result = RSAUtils.verify(prestr.getBytes("UTF-8"), null, mysign);
				System.out.println("解签名：" + result);

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("签名过程出现异常");
			}
		}
		return mysign;
	}

}
