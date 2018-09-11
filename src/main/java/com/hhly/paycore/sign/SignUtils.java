/**
 * Project Name:payment
 * File Name:SignUtils.java
 * Package Name:cn.swiftpass.utils.payment.sign
 * Date:2014-6-27下午3:22:33
 *
*/

package com.hhly.paycore.sign;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

/**
 * ClassName:SignUtils
 * Function: 签名用的工具箱
 * Date:     2014-6-27 下午3:22:33 
 * @author    
 */
public class SignUtils {

	/**
	 * <功能详细描述>验证返回参数
	 * @param params
	 * @param key
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	public static boolean checkParam(Map<String, String> params, String key) {
		return checkSign(params, key, true);
	}

	/**  
	* 方法说明: 验证返回结果签名是否一致
	* @auth: xiongJinGang
	* @param params
	* @param key
	* @param addKey 是否需要在待签名后面添加：&key= 。true是添加，false是不添加
	* @time: 2017年10月18日 下午12:04:36
	* @return: boolean 
	*/
	public static boolean checkParam(Map<String, String> params, String key, boolean addKey) {
		return checkSign(params, key, addKey);
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param params
	* @param key
	* @return
	* @time: 2017年10月18日 下午12:03:19
	* @return: boolean 
	*/
	private static boolean checkSign(Map<String, String> params, String key, boolean addKey) {
		boolean result = false;
		if (params.containsKey("sign")) {
			String sign = params.get("sign");
			params.remove("sign");
			StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
			SignUtils.buildPayParams(buf, params, false);
			String preStr = buf.toString();
			String signRecieve = null;
			if (addKey) {
				signRecieve = MD5.sign(preStr, "&key=" + key, "utf-8");
			} else {
				signRecieve = MD5.sign(preStr, key, "utf-8");
			}
			result = sign.equalsIgnoreCase(signRecieve);
		}
		return result;
	}

	/**
	 * 过滤参数
	 * @author  
	 * @param sArray
	 * @return
	 */
	public static Map<String, String> paraFilter(Map<String, String> sArray) {
		Map<String, String> result = new HashMap<String, String>(sArray.size());
		if (sArray == null || sArray.size() <= 0) {
			return result;
		}
		for (String key : sArray.keySet()) {
			String value = sArray.get(key);
			if (value == null || value.equals("") || key.equalsIgnoreCase("sign")) {
				continue;
			}
			result.put(key, value);
		}
		return result;
	}

	/** <一句话功能简述>
	 * <功能详细描述>将map转成String
	 * @param payParams
	 * @return
	 * @see [类、类#方法、类#成员]
	 */
	public static String payParamsToString(Map<String, String> payParams) {
		return payParamsToString(payParams, false);
	}

	public static String payParamsToString(Map<String, String> payParams, boolean encoding) {
		return payParamsToString(new StringBuilder(), payParams, encoding);
	}

	/**
	 * @author 
	 * @param payParams
	 * @return
	 */
	public static String payParamsToString(StringBuilder sb, Map<String, String> payParams, boolean encoding) {
		buildPayParams(sb, payParams, encoding);
		return sb.toString();
	}

	/**
	 * @author 
	 * @param payParams
	 * @return
	 */
	public static void buildPayParams(StringBuilder sb, Map<String, String> payParams, boolean encoding) {
		List<String> keys = new ArrayList<String>(payParams.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			sb.append(key).append("=");
			if (encoding) {
				sb.append(urlEncode(payParams.get(key)));
			} else {
				sb.append(payParams.get(key));
			}
			sb.append("&");
		}
		sb.setLength(sb.length() - 1);
	}

	public static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (Throwable e) {
			return str;
		}
	}

	public static Element readerXml(String body, String encode) throws DocumentException {
		SAXReader reader = new SAXReader(false);
		InputSource source = new InputSource(new StringReader(body));
		source.setEncoding(encode);
		Document doc = reader.read(source);
		Element element = doc.getRootElement();
		return element;
	}

}
