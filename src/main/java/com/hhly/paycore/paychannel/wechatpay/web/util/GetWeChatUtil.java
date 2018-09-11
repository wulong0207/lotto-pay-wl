package com.hhly.paycore.paychannel.wechatpay.web.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.ssl.SSLContext;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.hhly.paycore.paychannel.wechatpay.config.WeChatPayConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.http.HttpClientConnectionManager;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.JsonUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.URLUtil;

public class GetWeChatUtil {
	
	/**
	 * 微信统一下单接口
	 */
	public static String wxcreateOrderURL = "https://api.mch.weixin.qq.com/pay/unifiedorder";
	/**
	 * 微信申请退款
	 */
	public static String wxrefundOrderURL = "https://api.mch.weixin.qq.com/secapi/pay/refund";
	/**
	 * 微信退款查询
	 */
	public static String wxrefundQueryURL = "https://api.mch.weixin.qq.com/pay/refundquery";
	/**
	 * 微信下单查询
	 */
	public static String wxorderQueryURL = "https://api.mch.weixin.qq.com/pay/orderquery";
	/**
	 * 微信对账单查询
	 */
	public static String wxbillQueryURL = "https://api.mch.weixin.qq.com/pay/downloadbill";
	
	public static String ACCEE_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

	/**
	 * description:获取微信返回信息
	 * 
	 * @param url
	 * @param xmlParam
	 * @return
	 */
	public static Map getWxInfo(String url, String xmlParam) throws Exception {
		DefaultHttpClient httpclient = new DefaultHttpClient();
		httpclient = (DefaultHttpClient) HttpClientConnectionManager.getSSLInstance(httpclient);
		DefaultHttpClient client = new DefaultHttpClient();
		client.getParams().setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
		HttpPost httpost = HttpClientConnectionManager.getPostMethod(url);
		Map map = new HashMap();
		try {
			httpost.setEntity(new StringEntity(xmlParam, "UTF-8"));
			HttpResponse response = httpclient.execute(httpost);

			String jsonStr = EntityUtils.toString(response.getEntity(), "UTF-8");
			/*if (jsonStr.indexOf("FAIL") != -1) {
				return map;
			}*/
			map = doXMLParse(jsonStr);
			//prepay_id = (String) map.get("prepay_id");
			//code_url = (String) map.get("code_url");
			//returnCode = (String) map.get("return_code");
		} finally {

		}
		return map;
	}
	
	/** 
	* @Title: wxRefund 
	* @Description: 微信退款请求，需要双向证书
	*  @param xmlParam
	*  @return
	* @time 2017年4月1日 上午11:41:30
	*/
	public static Map wxRefund(String xmlParam) throws Exception {
        FileInputStream instream = null;
        KeyStore keyStore = null;
        SSLContext sslcontext = null;
		try {
			keyStore = KeyStore.getInstance("PKCS12");  
			instream = new FileInputStream(new File("D:/10016225.p12"));
            keyStore.load(instream,"见邮件".toCharArray());  
            sslcontext = SSLContexts.custom().loadKeyMaterial(keyStore,  
			        "见邮件".toCharArray()).build();
        } finally {  
            try {
            	if(instream!=null)
            		instream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}  
        }  
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(  
                sslcontext, new String[] { "TLSv1" }, null,  
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);  
        CloseableHttpClient httpclient = HttpClients.custom()  
                .setSSLSocketFactory(sslsf).build();  
        HttpPost httppost = new HttpPost(wxrefundOrderURL);
		Map map = new HashMap();
		try {
			httppost.setEntity(new StringEntity(xmlParam, "UTF-8"));
			CloseableHttpResponse responseEntry = httpclient.execute(httppost);  

			String jsonStr = EntityUtils.toString(responseEntry.getEntity(), "UTF-8");
			System.out.println(jsonStr);
			if (jsonStr.indexOf("FAIL") != -1) {
				return map;
			}
			map = doXMLParse(jsonStr);
		} finally {

		}
		return map;
	}

	/**
	 * @Title getOpenId
	 * @Description TODO
	 * @return
	 * @return String
	 */
	public String getOpenId(String code) {
		if (ObjectUtil.isBlank(code) || ObjectUtil.isBlank(WeChatPayConfig.appid) || ObjectUtil.isBlank(WeChatPayConfig.appsecret)) {
			return null;
		}
		final String grantType = "authorization_code";
		Map<String, String> params = new HashMap<String, String>(4);
		params.put("appid", WeChatPayConfig.appid);
		params.put("secret", WeChatPayConfig.appsecret);
		params.put("code", code);
		params.put("grant_type", grantType);
		String accessTokenUrl = URLUtil.appendOrReplaceParam(ACCEE_TOKEN_URL, params);
		HttpClient client = new HttpClient();
		GetMethod get = new GetMethod(accessTokenUrl);
		try {
			client.executeMethod(get);
			if (get.getStatusCode() == 200) {
				String response = get.getResponseBodyAsString();//{"errcode":40029,"errmsg":"invalid code, hints: [ req_id: jrKMza0816th10 ]"}
				String openid = JsonUtil.getValue(response, "openid");
				if (ObjectUtil.isBlank(openid)) {
					return null;
				}
				return openid;
			}
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			get.releaseConnection();
		}
		return null;
	}
	
	/**
	 * @Title getCode
	 * @Description TODO
	 * @return
	 * @return String
	 */
	public String getCode(String orderNo) {
		// ------微信公众号登录-------------------
		String url = "https://open.weixin.qq.com/connect/oauth2/authorize";
		String redirecturl = "http://exchange.genebook.com.cn/weichatCallback";
		String targetUrl = WeChatPayConfig.wxpayUrl + "?orderNo=" + orderNo;
		String appid = WeChatPayConfig.appid;
		String response_type = "code";
		//String scope = "snsapi_base";
		String scope = "snsapi_userinfo";
		String state = "aaabbb";
		redirecturl = URLUtil.appendOrReplaceParam(redirecturl, "targetUrl", URLUtil.encodeURL(targetUrl));
		Map<String, String> params = new LinkedHashMap<String, String>(5);
		params.put("appid", appid);
		params.put("redirect_uri", URLUtil.encodeURL(redirecturl));
		params.put("response_type", response_type);
		params.put("scope", scope);
		params.put("state", state);
		String result = URLUtil.appendOrReplaceParam(url, params);
		result += "#wechat_redirect";
		return result;
	}

	/**
	 * 解析xml,返回第一级元素键值对。如果第一级元素有子节点，则此节点的值是子节点的xml数据。
	 * 
	 * @param strxml
	 * @return
	 * @throws JDOMException
	 * @throws IOException
	 */
	public static Map doXMLParse(String strxml) throws Exception {
		if (null == strxml || "".equals(strxml)) {
			return null;
		}

		Map m = new HashMap();
		InputStream in = String2Inputstream(strxml);
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build(in);
		Element root = doc.getRootElement();
		List list = root.getChildren();
		Iterator it = list.iterator();
		while (it.hasNext()) {
			Element e = (Element) it.next();
			String k = e.getName();
			String v = "";
			List children = e.getChildren();
			if (children.isEmpty()) {
				v = e.getTextNormalize();
			} else {
				v = getChildrenText(children);
			}

			m.put(k, v);
		}

		// 关闭流
		in.close();

		return m;
	}

	/**
	 * 获取子结点的xml
	 * 
	 * @param children
	 * @return String
	 */
	public static String getChildrenText(List children) {
		StringBuffer sb = new StringBuffer();
		if (!children.isEmpty()) {
			Iterator it = children.iterator();
			while (it.hasNext()) {
				Element e = (Element) it.next();
				String name = e.getName();
				String value = e.getTextNormalize();
				List list = e.getChildren();
				sb.append("<" + name + ">");
				if (!list.isEmpty()) {
					sb.append(getChildrenText(list));
				}
				sb.append(value);
				sb.append("</" + name + ">");
			}
		}

		return sb.toString();
	}

	public static InputStream String2Inputstream(String str) {
		return new ByteArrayInputStream(str.getBytes());
	}
	

	/**
	 * @Title getNonceStr
	 * @Description 获取随机字符串
	 * @return
	 */
	public static String getNonceStr() {
		// 随机数
		String currTime = DateUtil.getNow(DateUtil.DATE_FORMAT_NUM);
		// 8位日期
		String strTime = currTime.substring(8, currTime.length());
		// 四位随机数
		String strRandom = buildRandom(4) + "";
		// 10位序列号,可以自行调整。
		return strTime + strRandom;
	}
	/**
	 * 
	 * @Description: 获取指定长度的随机字符串
	 * @param len 最长度 65位
	 * @return
	 * @author wuLong
	 * @date 2017年7月22日 下午12:16:56
	 */
	public static String getRandomStr(Integer len){
		String strTime = System.currentTimeMillis()+"";
		char[] st = strTime.toCharArray();
		char[] arr = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};
		char[] sum = Arrays.copyOf(arr, st.length+arr.length);
		System.arraycopy(st, 0, sum, arr.length, st.length);
		StringBuffer sb = new StringBuffer();
		int arrlen = sum.length;
		Random rand = new Random();
		for(int i = 0;i<len;i++){
			sb.append(sum[rand.nextInt(arrlen)]+"");
		}
		String str = sb.toString();
		return str;
	}
	
	public static void main(String[] args) {
		System.out.println(getRandomStr(32));
	}

	/**
	 * @Title getMoney
	 * @Description 元转换成分
	 * @param amount
	 * @return String
	 */
	public static String getMoney(String amount) {
		if (amount == null) {
			return "";
		}
		// 金额转化为分为单位
		String currency = amount.replaceAll("\\$|\\￥|\\,", ""); // 处理包含, ￥
																// 或者$的金额
		int index = currency.indexOf(".");
		int length = currency.length();
		Long amLong = 0l;
		if (index == -1) {
			amLong = Long.valueOf(currency + "00");
		} else if (length - index >= 3) {
			amLong = Long.valueOf((currency.substring(0, index + 3)).replace(".", ""));
		} else if (length - index == 2) {
			amLong = Long.valueOf((currency.substring(0, index + 2)).replace(".", "") + 0);
		} else {
			amLong = Long.valueOf((currency.substring(0, index + 1)).replace(".", "") + "00");
		}
		return amLong.toString();
	}
	
	/**
	 * 取出一个指定长度大小的随机正整数.
	 * 
	 * @param length
	 *            int 设定所取出随机数的长度。length小于11
	 * @return int 返回生成的随机数。
	 */
	public static int buildRandom(int length) {
		int num = 1;
		double random = Math.random();
		if (random < 0.1) {
			random = random + 0.1;
		}
		for (int i = 0; i < length; i++) {
			num = num * 10;
		}
		return (int) ((random * num));
	}
	
	   /**  
     * 将分为单位的转换为元 （除100）  
     *   
     * @param amount  
     * @return  
     * @throws Exception   
     */    
    public static String changeF2Y(String amount){    
        if(!amount.matches("\\-?[0-9]+")) {    
            return "";
        }    
        return BigDecimal.valueOf(Long.valueOf(amount)).divide(new BigDecimal(100)).toString();    
    }  
}