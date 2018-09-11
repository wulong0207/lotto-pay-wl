package com.hhly.paycore.paychannel.wechatpay.config;

public class WeChatPayConfig {
	/**
	 * 微信分配的公众账号ID
	 */
	public static final String appid = "wx1f94c3b81bbe6d08";

	/**
	 * 应用密钥
	 */
	public static final String appsecret = "3d6774b15c6d9682e59ddf1cf58a0747";

	/**
	 * APP支付应用密钥
	 */
	public static final String a_appsecret = "824f318198565b4b46f8ddfc07e99db2";
	
	/**
	 * 商户KEY_API密钥
	 */
	public static final String keyapi = "7yGDjwfqaSY4s8oZjXfLtgH8i12a9d7T";

	/**
	 * 微信支付分配的商户号
	 */
	public static final String mchid = "1332467601";

	/**
	 * 微信app支付商户号
	 */
	public static final String app_mchid = "1388952702";
	
	/**
	 * 微信支付成功后通知地址 必须要求80端口并且地址不能带参数
	 */
	public static final String notify_url = "http://www.13322.com";

	/**
	 * 微信支付成功后通知地址 必须要求80端口并且地址不能带参数
	 */
	public static final String notify_m_url = "http://www.13322.com";
	
	/**
	 * 订单生成的机器 IP
	 */
	public static final String spbill_create_ip = "127.0.0.1";

	/**
	 * 微信openId返回接口
	 */
	public static final String wxpayUrl= "http://pay.xxxx.com.cn/wxpayUrl";
}
