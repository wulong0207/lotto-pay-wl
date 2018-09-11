package com.hhly.paycore.paychannel.cshpay.util;

import com.hhly.skeleton.base.util.StringUtil;

import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaySignUtil {

	
	/**
	 * 参数加密
	 * @param map
	 * @param keyValue  商户密钥
	 * @return
	 * @throws Exception 
	 */
	public static String getSign(Map<String,String> map,String keyValue) throws Exception{
		StringBuffer buffer = new StringBuffer();
		buffer.append("keyValue=" + keyValue+"&");
		buffer.append(getParamStr(map));
		System.out.println(buffer.toString());
		String sNewString = getSign(buffer.toString().toUpperCase(), "MD5");
		
		return sNewString;
	}
	
	/**
	 * 参数签名串拼接
	 * @param map
	 * @return
	 */
	public static String getParamStr(Map<String,String> map){
		StringBuffer buffer = new StringBuffer();
		List<String> keys = new ArrayList<String>(map.keySet());
		//排序
        Collections.sort(keys);
		//参数值拼接进行加密
        for (int i = 0; i < keys.size(); i++) {
        	String key = keys.get(i);
			if(!"sign".equals(key)&&!"keyValue".equals(key)){
				String value = map.get(key)==null?"":map.get(key);
				if(i==0){
					buffer.append(key + "=" + value);
				}else{
					buffer.append("&"+key + "=" + value);
				}
			}
		}
		return buffer.toString();
	}
	
	/**
	 * 获取加密签名
	 * @param str 字符
	 * @param type 加密类型
	 * @return 
	 * @throws Exception
	 */
	public static String getSign(String str, String type) throws Exception {
		MessageDigest crypt = MessageDigest.getInstance(type);
		crypt.reset();
		crypt.update(str.getBytes("UTF-8"));
		return str = byteToHex(crypt.digest());
	}
	
	/**
	 * 
	* @Title: byteToHex 
	* @Description: 字节转换 16 进制
	* @param @param hash
	* @param @return    设定文件 
	* @return String    返回类型 
	* @throws
	 */
	private static String byteToHex(final byte[] hash) {
		Formatter formatter = new Formatter();
		for (byte b : hash) {
			formatter.format("%02x", b);
		}
		String result = formatter.toString();
		formatter.close();
		return result;
	}
	
	/**
	 * 验签
	 * @param map
	 * @param keyValue  商户密钥
	 * @return
	 * @throws Exception 
	 */
	public static boolean checkSign(Map<String,String> map,String keyValue) throws Exception{
		if(map.get("sign")==null){
			return false;
		}
		//密文
		String sign = map.get("sign");
		map.remove("sign");
		String sNewString = getSign(map, keyValue);
		
		return sNewString.equals(sign);
	}
	
	public static void main(String[] args) throws Exception {
		//**************************参数按照文档传入***********************************
		Map<String,String> map = new HashMap<>();
		//支付测试签名示例


		map.put("appKey", "4c689265d2457db973d469c070da52c8");//测试appKey
		map.put("notifyUrl", "http://183.62.174.53:9017/lotto/v1.0/payCenter/CSHPayWap");
		map.put("payMoney", "0.01");
		map.put("bussOrderNum", "I18071217275417200002");
		map.put("orderName", "2Ncai-300-180712-1");
		map.put("ip", "58.60.230.138");// ip地址
		map.put("appType", "1");
		map.put("payPlatform", "1");
		map.put("remark", "1");
		String sign =getSign(map,"SHWybZhnXT2TgwbVISPrmBsfCHRNeq8pqoSHaQbJ");//测试密钥
		String paramStr = getParamStr(map)+"&sign="+sign;
		String payUrl = "http://128.1.136.18//pay/multifunctionPayment.do?paramStr="+ URLEncoder.encode(paramStr);
		/*//查询订单签名示例
		map.put("appKey", "6413f866b558d3e2b6ccf4f0d865f9df");//测试appKey
		map.put("bussOrderNum", "145800654162564");
		map.put("orderNum", "DD2017536545662646");
		String sign =getSign(map,"u4smNesRMrDAIU62HXNy1eoeP9uD8yaUKCcd103j");//测试密钥
		String paramStr = getParamStr(map)+"&sign="+sign;*/
		
		
		/*//退款签名示例
		map.put("appKey", "6413f866b558d3e2b6ccf4f0d865f9df");//测试appKey
		map.put("bussOrderNum", "145800654162564");
		map.put("orderNum", "DD2017536545662646");
		map.put("refundMoney", "0.01");
		map.put("refundType", "Y");
		String sign =getSign(map,"u4smNesRMrDAIU62HXNy1eoeP9uD8yaUKCcd103j");//测试密钥
		String paramStr = getParamStr(map)+"&sign="+sign;*/
		
		/*//获取对账单签名示例
		map.put("appKey", "6413f866b558d3e2b6ccf4f0d865f9df");//测试appKey
		map.put("billTime", "20170206");
		String sign =getSign(map,"u4smNesRMrDAIU62HXNy1eoeP9uD8yaUKCcd103j");//测试密钥
		String paramStr = getParamStr(map)+"&sign="+sign;*/

		/*//异步通知回调参数验签
		Map<String, Object> param = new HashMap<>();
		param.put("result_code", "200"); // 成功
		param.put("order_num", "DD2017124512345661"); // 订单
		param.put("buss_order_num", "21564561561456"); // 商户订单
		param.put("pay_money", "0.01"); // 支付金额
		param.put("pay_discount_money", "0.01"); // 最终支付金额
		param.put("pay_time", "20170206122558"); // 支付时间
		String sign = PaySignUtil.getSign(param, "u4smNesRMrDAIU62HXNy1eoeP9uD8yaUKCcd103j");
		param.put("sign", sign);
		//验签
		checkSign(map, "u4smNesRMrDAIU62HXNy1eoeP9uD8yaUKCcd103j");*/

		String tc ="I18070417572517200026?buss_order_num=I18070417572517200026";
		if(!StringUtil.isBlank(tc)&&tc.contains("?")){
			tc=tc.substring(0,tc.indexOf("?"));
		}
		System.out.println(payUrl);
	}
}
