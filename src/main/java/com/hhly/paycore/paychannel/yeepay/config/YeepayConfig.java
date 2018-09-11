package com.hhly.paycore.paychannel.yeepay.config;

/**
 * @desc 易宝配置文件
 * @author xiongJinGang
 * @date 2017年6月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class YeepayConfig {
	// 一键支付移动端paymobile，生产环境测试商户编号
	public static String MERCHANT_ACCOUNT;// "10000418926";
	// 商户私钥
	public static String MERCHANT_PRIVATE_KEY;//
	// 易宝公玥
	public static String YEEPAY_PUBLIC_KEY;// "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCKcSa7wS6OMUL4oTzJLCBsE5KTkPz9OTSiOU6356BsR6gzQ9kf/xa+Wi1ZANTeNuTYFyhlCI7ZCLW7QNzwAYSFStKzP3UlUzsfrV7zge8gTgJSwC/avsZPCWMDrniC3HiZ70l1mMBK5pL0H6NbBFJ6XgDIw160aO9AxFZa5pfCcwIDAQAB";
	// 4.1 订单支付接口请求地址
	public static String PAY_API_URL;// "https://ok.yeepay.com/paymobile/payapi/request";
	// 4.2 订单查询接口请求地址
	public static String QueryOrderApi;// "https://ok.yeepay.com/merchant/query_server/pay_single";
	// 4.3 消费对账文件下载接口请求地址
	public static String PayClearDataApi;// "https://ok.yeepay.com/merchant/query_server/pay_clear_data";
	// 4.4 单笔退款接口请求地址
	public static String REFUND_API_URL;// "https://ok.yeepay.com/merchant/query_server/direct_refund";
	// 4.5 退款查询接口请求地址
	public static String QueryRefundApi;// "https://ok.yeepay.com/merchant/query_server/refund_single";
	// 4.6 退款对账文件下载接口
	public static String RefundClearDataApi;// "https://ok.yeepay.com/merchant/query_server/refund_clear_data";
	// 4.7 银行卡信息查询接口请求地址
	public static String CheckBankcardApi;// "https://ok.yeepay.com/payapi/api/bankcard/check";
	// 4.8 查询绑卡信息列表
	public static String QueryBankCardListApi;// "https://ok.yeepay.com/payapi/api/bankcard/bind/list";
	// 4.9 解绑卡
	public static String UnbindCardApi;// "https://ok.yeepay.com/payapi/api/bankcard/unbind";

	// 易宝网银标准版配置
	// 商户编号
	public static String P1_MERID;// "10000457067";
	// p1_MerId;//10012442782

	// 商户密钥
	public static String KEY_VALUE;// "U26po59182dV8d7654bo24o5z369408u4sQ3To9j6QuopAbo3gwj4h33mro4";
	// keyValue;//mP42238826nuW64r7yh26DGK34o2L2m81L25RG32lD7Lo1058A7iJ28at6QS

	// 下单请求地址
	public static String REQUEST_URL;// "https://www.yeepay.com/app-merchant-proxy/node";
	// 订单查询请求地址
	public static String QUERY_URL;// "https://cha.yeepay.com/app-merchant-proxy/command";
	// 退款请求地址
	public static String REFUND_URL;// "https://cha.yeepay.com/app-merchant-proxy/command";
	// 退款查询请求地址
	public static String REFUND_QUERY_URL;// "https://www.yeepay.com/app-merchant-proxy/node";
	// 订单取消请求地址
	public static String CANCEL_ORDER_URL;// "https://cha.yeepay.com/app-merchant-proxy/command";
}
