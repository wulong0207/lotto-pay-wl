package com.hhly.paycore.paychannel.zhangling;

/**
 * @desc 掌灵支付渠道
 * @author xiongJinGang
 * @date 2017年9月23日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class ZhangLingConfig {
	// 微信、支付宝H5支付请求地址
	public static String ZHANGLING_H5_URL;

	// 微信、支付宝扫码支付请求地址
	public static String ZHANGLING_API_URL;

	// 掌灵支付微信扫码、微信H5、支付宝扫码、支付宝H5是同一个账号
	// 商户号
	public static String ZHANGLING_PAY_PARTNER_CODE;
	// 秘钥
	public static String ZHANGLING_PAY_KEY;
	// 公钥
	public static String ZHANGLING_PUBLIC_KEY;
}
