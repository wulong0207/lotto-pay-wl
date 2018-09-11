package com.hhly.paycore.paychannel.huayi.config;

/**
 * @project STC_DEMO
 * 七分钱配置参数
 */
public class SevenPayConfig {
	
	/**
	 * 签名方式
	 */
	public static final String SIGN_TYPE = "RSA";
	
	/**
	 * 字符编码格式 UTF-8
	 */
	public static final String INPUT_CHARSET = "UTF-8";
	
	/**
	 * 接口版本
	 */
	public static final String VERSION = "v2.0";
	
	/**
	 * 商户号
	 * 商户号查询：https://www.qifenqian.com/enterprise/login.do
	 */
	public static final String MERCHANT_CODE = "C2018051700191";//  M9144030035873982X0
	
	/**
	 * 聚合支付网页订单提交地址
	 * 正式环境：https://combinedpay.qifenqian.com/gateway.do
	 * 测试环境：http://zt.qifenmall.com/gateway.do
	 */
//	public static final String AGGREGATEPAY_SUBMIT = "http://zt.qifenmall.com/gateway.do";
	public static final String AGGREGATEPAY_SUBMIT = "https://combinedpay.qifenqian.com/gateway.do";

}
