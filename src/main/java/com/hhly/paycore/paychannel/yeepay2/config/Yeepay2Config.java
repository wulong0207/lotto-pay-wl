package com.hhly.paycore.paychannel.yeepay2.config;

/**
 * @desc 易宝配置文件
 * @author xiongJinGang
 * @date 2017年6月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class Yeepay2Config {
	// 请求地址
	public static String NEW_YEEPAY_URL;
	// 系统商或者平台商商编，如果是单商户，和收单商户商编保持一致
	public static String NEW_YEEPAY_PARENT_MERCHANT_NO;//
	// 收单商户商编号
	public static String NEW_YEEPAY_MERCHANT_NO;
	// 订单创建URI
	public static String NEW_YEEPAY_TRADE_ORDER_URI;
	// 单笔订单查询URI
	public static String NEW_YEEPAY_ORDER_QUERY_URI;
	// 单笔退款URI
	public static String NEW_YEEPAY_REFUNDU_RI;
	// 单笔退款查询URI
	public static String NEW_YEEPAY_REFUND_QUERY_URI;
	// API收银台支付
	public static String NEW_YEEPAY_API_CASHIER;
	// 标准收银台支付
	public static String NEW_YEEPAY_CASHIER;
}
