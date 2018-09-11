package com.hhly.paycore.paychannel.yeepay.utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.yeepay.config.YeepayConfig;

/**
 * @author YiJian
 * @version 1.0
 * @Desc: 易宝 收银台工具类
 * @date 2017年5月17日
 * @compay 益彩网络科技有限公司
 */
public class PaymobileUtils {

	// 编码格式UTF-8
	public static final String CHARSET = "UTF-8";

	// 生成AESKey: 16位的随机串
	public static String buildAESKey() {
		return RandomUtil.getRandom(16);
	}

	// 使用易宝公钥将AESKey加密生成encryptkey
	public static String buildEncryptkey(String AESKey, String publicKey) {
		String encryptkey = "";
		try {
			encryptkey = RSA.encrypt(AESKey, publicKey);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return encryptkey;
	}

	// 使用易宝公钥将AESKey加密生成encryptkey
	public static String buildEncyptkey(String AESKey) {
		return buildEncryptkey(AESKey, YeepayConfig.YEEPAY_PUBLIC_KEY);
	}

	// 生成RSA签名：sign
	public static String buildSign(TreeMap<String, Object> treeMap, String privateKey) {

		String sign = "";
		StringBuffer buffer = new StringBuffer();
		for (Entry<String, Object> entry : treeMap.entrySet()) {
			if (entry.getValue() != null) {
				buffer.append(entry.getValue());
			}
		}
		String signTemp = buffer.toString();

		if (StringUtils.isNotEmpty(privateKey)) {
			sign = RSA.sign(signTemp, privateKey);
		}
		return sign;
	}

	// 使用商户私钥生成RSA签名：sign
	public static String buildSign(TreeMap<String, Object> treeMap) {
		return buildSign(treeMap, YeepayConfig.MERCHANT_PRIVATE_KEY);
	}

	// 生成密文：data
	public static String buildData(TreeMap<String, Object> treeMap, String AESKey) {
		// 将商户编号放入treeMap
		treeMap.put("merchantaccount", YeepayConfig.MERCHANT_ACCOUNT);

		// 生成sign，并将其放入treeMap
		String sign = buildSign(treeMap);
		treeMap.put("sign", sign);
		String jsonStr = JSON.toJSONString(treeMap);
		return AES.encryptToBase64(jsonStr, AESKey);
	}

	// 一键支付post请求
	public static TreeMap<String, String> httpPost(String url, String merchantaccount, String data, String encryptkey) {
		// 请求参数为如下三者：merchantaccount、data、enrcyptkey
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("data", data);
		paramMap.put("encryptkey", encryptkey);
		paramMap.put("merchantaccount", merchantaccount);

		String responseBody = HttpClient4Utils.sendHttpRequest(url, paramMap, CHARSET, true);
		TreeMap<String, String> result = JSON.parseObject(responseBody, new TypeReference<TreeMap<String, String>>() {
		});
		return result;
	}

	// get请求
	public static TreeMap<String, String> httpGet(String url, String merchantaccount, String data, String encryptkey) {

		TreeMap<String, String> result = null;

		// 请求参数为如下三者：merchantaccount、data、enrcyptkey
		Map<String, String> paramMap = new HashMap<String, String>();
		paramMap.put("data", data);
		paramMap.put("encryptkey", encryptkey);
		paramMap.put("merchantaccount", merchantaccount);

		String responseBody = HttpClient4Utils.sendHttpRequest(url, paramMap, CHARSET, false);
		result = JSON.parseObject(responseBody, new TypeReference<TreeMap<String, String>>() {
		});

		return result;
	}

	// 解密data，获得明文参数
	public static TreeMap<String, String> decrypt(String data, String encryptkey, String privateKey) {
		// 1.使用商户密钥解密encryptKey。
		String AESKey = "";
		try {
			AESKey = RSA.decrypt(encryptkey, YeepayConfig.MERCHANT_PRIVATE_KEY);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// 2.使用AESKey解开data，取得明文参数；解密后格式为json
		String jsonStr = AES.decryptFromBase64(data, AESKey);

		// 3.将JSON格式数据转换成TreeMap格式
		TreeMap<String, String> result = JSON.parseObject(jsonStr, new TypeReference<TreeMap<String, String>>() {
		});

		return result;
	}

	// 解密data，获得明文参数
	public static TreeMap<String, String> decrypt(String data, String encryptkey) {
		return decrypt(data, encryptkey, YeepayConfig.MERCHANT_PRIVATE_KEY);
	}

	// sign验签
	public static boolean checkSign(String params, String signYeepay, String publicKey) {
		return RSA.checkSign(params, signYeepay, YeepayConfig.YEEPAY_PUBLIC_KEY);
	}

	// sign验签
	public static boolean checkSign(TreeMap<String, String> dataMap) {
		// 获取明文参数中的sign。
		String signYeepay = StringUtils.trimToEmpty(dataMap.get("sign"));

		// 将明文参数中sign之外的其他参数，拼接成字符串
		StringBuffer buffer = new StringBuffer();
		for (Entry<String, String> entry : dataMap.entrySet()) {
			String key = formatStr(entry.getKey());
			String value = formatStr(entry.getValue());
			if ("sign".equals(key)) {
				continue;
			}
			buffer.append(value);
		}

		// result为true时表明验签通过
		return checkSign(buffer.toString(), signYeepay, YeepayConfig.YEEPAY_PUBLIC_KEY);
	}

	public static InputStream clearDataHttpGet(String url, String merchantaccount, String data, String encryptkey) {
		return ClearDataUtils.httpGet(url, merchantaccount, data, encryptkey);
	}

	// 字符串格式化
	public static String formatStr(String text) {
		return (text == null) ? "" : text.trim();
	}
}
