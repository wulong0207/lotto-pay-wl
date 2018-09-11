package com.hhly.paycore.paychannel.lianlianpay.config;

/**
 * @Desc: 连连支付-商户配置信息
 * @author YiJian
 * @date 2017年3月6日
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
public class LianPayConfig {
	/***************************连连支付网银配置************************************/
	// 银通公钥
	public static String YT_PUB_KEY;
	// 商户私钥
	public static String TRADER_PRI_KEY;
	// MD5 KEY
	public static String MD5_KEY;
	// 商户编号
	public static String OID_PARTNER;
	// 签名方式 RSA或MD5
	public static String SIGN_TYPE;
	// 接口版本号，固定1.0
	public static String VERSION;
	// 业务类型，连连支付根据商户业务为商户开设的业务类型； （101001：虚拟商品销售、109001：实物商品销售、108001：外部账户充值）
	public static String BUSI_PARTNER;
	// 连连网银、快捷支付服务地址
	public static String PAY_URL;
	// 支付结果查询地址
	public static String QUERY_URL;
	// 用户已绑定银行卡列表查询
	public static String QUERY_USER_BANKCARD_URL;
	// 银行卡卡bin信息查询
	public static String QUERY_BANKCARD_URL;

	/***************************连连快捷支付配置************************************/
	// 银通公钥
	public static String FAST_PUB_KEY;
	// 商户私钥
	public static String FAST_PRI_KEY;
	// MD5 KEY
	public static String FAST_SIGN_TYPE;
	public static String FAST_OID_PARTNER;
	public static String FAST_MD5_KEY;

	/***************************连连WAP支付配置************************************/
	// wap支付地址
	public static String WAP_PAY_URL;
	// wap版本号
	public static String WAP_VERSION;
}
