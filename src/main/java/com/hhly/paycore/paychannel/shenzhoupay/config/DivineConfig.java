package com.hhly.paycore.paychannel.shenzhoupay.config;
/**
 * 
 * @ClassName: ShenZhouConfig 
 * @Description: 神州支付
 * @author wuLong
 * @date 2017年8月24日 上午9:10:02 
 *
 */
public class DivineConfig {
	/**wap微信APPID**/
	public static String SHENZHOU_PAY_WX_APPID;
	/**wap微信KEY**/
	public static String SHENZHOU_PAY_WX_KEY;
	/**商户URL**/
	public static String SHENZHOU_PAY_URL;
	/**RSA公钥**/
	public static String SHENZHOU_RSA_PUBLIC_KEY;
	/**商户请求平台查询订单状态接口**/
	public static String SHENZHOU_PAY_QUERY_URL;
	/**app请求支付微信IOS_version**/
	public static String SHENZHOU_PAY_APP_IOS_VERSION;
	/**app请求支付微信ANDROID_version**/
	public static String SHENZHOU_PAY_APP_ANDROID_VERSION;
	/**商户在神州付的唯一身份标识**/
	public static String SHENZHOU_MER_ID;
	/**是md5key，md5加密会用到，跳转接口和直连接口都要用到此参数**/
	public static String SHENZHOU_PRIVATE_KEY;
	/**是直连接口专用的，加密cardInfo时会用到**/
	public static String SHENZHOU_DES_KEY;
	/**充值卡WEB充值地址**/
	public static String SHENZHOU_CARD_WEB_PAY_URL;
	/**充值卡WAP充值地址**/
	public static String SHENZHOU_CARD_WAP_PAY_URL;
}
