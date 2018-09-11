package com.hhly.paycore.paychannel.alipay.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.httpclient.methods.multipart.FilePartSource;
import org.apache.commons.httpclient.methods.multipart.PartSource;

import com.hhly.skeleton.base.util.ObjectUtil;

/**
 * 支付宝接口公用函数类
 * 
 * @Desc: 该类是请求、通知返回两个文件所调用的公用函数核心处理文件，不需要修改
 * @author YiJian
 * @date 2017年3月10日
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
public class AlipayCore {

	/**
	 * 除去数组中的空值和签名参数
	 * 
	 * @param sArray
	 *            签名参数组
	 * @return 去掉空值与签名参数后的新签名参数组
	 */
	public static Map<String, String> paraFilter(Map<String, String> sArray) {

		Map<String, String> result = new HashMap<String, String>();

		if (sArray == null || sArray.size() <= 0) {
			return result;
		}

		for (String key : sArray.keySet()) {
			String value = sArray.get(key);
			if (value == null || value.equals("") || key.equalsIgnoreCase("sign") || key.equalsIgnoreCase("sign_type")) {
				continue;
			}
			result.put(key, value);
		}

		return result;
	}

	/**
	 * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
	 * 
	 * @param params
	 *            需要排序并参与字符拼接的参数组
	 * @return 拼接后字符串
	 */
	public static String createLinkString(Map<String, String> params) {
		List<String> keys = new ArrayList<String>(params.keySet());
		Collections.sort(keys);
		String prestr = "";
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			String value = params.get(key);
			if (i == keys.size() - 1) {// 拼接时，不包括最后一个&字符
				prestr = prestr + key + "=" + value;
			} else {
				prestr = prestr + key + "=" + value + "&";
			}
		}
		return prestr;
	}

	/** 
	* @Title: createLinkStringByEntity 
	* @Description: 把对象所有非空属性排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
	* @param object
	* @return String    返回类型 
	*/
	public static String createLinkStringByEntity(Object object) {
		Field[] fields = object.getClass().getDeclaredFields();
		Map map = new HashMap();
		for (Field field : fields) {
			String value = (String) getFieldValueByName(field.getName(), object);
			if (!ObjectUtil.isBlank(value)) {
				map.put(field.getName(), value);
			}
		}
		return createLinkString(map);
	}

	/**
	 * 把数组所有元素按照固定参数排序，以“参数=参数值”的模式用“&”字符拼接成字符串
	 * 
	 * @param params
	 *            需要参与字符拼接的参数组
	 * @return 拼接后字符串
	 */
	public static String createLinkStringNoSort(Map<String, String> params) {

		// 手机网站支付MD5签名固定参数排序，顺序参照文档说明
		StringBuilder gotoSign_params = new StringBuilder();
		gotoSign_params.append("service=" + params.get("service"));
		gotoSign_params.append("&v=" + params.get("v"));
		gotoSign_params.append("&sec_id=" + params.get("sec_id"));
		gotoSign_params.append("&notify_data=" + params.get("notify_data"));

		return gotoSign_params.toString();
	}

	/** 
	 * 写日志，方便测试（看网站需求，也可以改成把记录存入数据库）
	 * @param sWord 要写入日志里的文本内容
	
	public static void logResult(String sWord) {
	    FileWriter writer = null;
	    try {
	        writer = new FileWriter(AlipayConfig.log_path + "alipay_log_" + System.currentTimeMillis()+".txt");
	        writer.write(sWord);
	    } catch (Exception e) {
	        e.printStackTrace();
	    } finally {
	        if (writer != null) {
	            try {
	                writer.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	}
	     */

	/**
	 * 生成文件摘要
	 * 
	 * @param strFilePath
	 *            文件路径
	 * @param file_digest_type
	 *            摘要算法
	 * @return 文件摘要结果
	 */
	public static String getAbstract(String strFilePath, String file_digest_type) throws IOException {
		PartSource file = new FilePartSource(new File(strFilePath));
		if (file_digest_type.equals("MD5")) {
			return DigestUtils.md5Hex(file.createInputStream());
		} else if (file_digest_type.equals("SHA")) {
			return DigestUtils.sha256Hex(file.createInputStream());
		} else {
			return "";
		}
	}

	/**
	 * @Title: getFieldValueByName
	 * @Description: 根据属性名获取属性值
	 * @param fieldName
	 * @param obj
	 * @return Object 返回类型
	 */
	private static Object getFieldValueByName(String fieldName, Object obj) {
		try {
			String firstLetter = fieldName.substring(0, 1).toUpperCase();
			String getter = "get" + firstLetter + fieldName.substring(1);
			Method method = obj.getClass().getMethod(getter, new Class[] {});
			Object value = method.invoke(obj, new Object[] {});
			return value;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
