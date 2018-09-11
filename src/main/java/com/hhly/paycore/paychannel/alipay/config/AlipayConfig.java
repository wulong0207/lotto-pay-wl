package com.hhly.paycore.paychannel.alipay.config;

/**
 * 设置帐户有关信息及返回路径
 * 
 * @Desc: 提示：如何获取安全校验码和合作身份者ID 1.用您的签约支付宝账号登录支付宝网站(www.alipay.com)
 *        2.点击“商家服务”(https://b.alipay.com/order/myOrder.htm)
 *        3.点击“查询合作者身份(PID)”、“查询安全校验码(Key)” 安全校验码查看时，输入支付密码后，页面呈灰色的现象，怎么办？ 解决方法：
 *        1、检查浏览器配置，不让浏览器做弹框屏蔽设置 2、更换浏览器或电脑，重新登录查询。
 * @author YiJian
 * @date 2017年3月13日
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
public class AlipayConfig {
	private static AlipayConfig alconfig = null;

	private AlipayConfig() {
	}

	public static AlipayConfig getInstance() {
		if (alconfig == null) {
			alconfig = new AlipayConfig();
		}
		return alconfig;
	}

	// 如何获取安全校验码和合作身份者ID
	// 1.访问支付宝商户服务中心(b.alipay.com)，然后用您的签约支付宝账号登陆.
	// 2.访问“技术服务”→“下载技术集成文档”（https://b.alipay.com/support/helperApply.htm?action=selfIntegration）
	// 3.在“自助集成帮助”中，点击“合作者身份(Partner ID)查询”、“安全校验码(Key)查询”
	// ↓↓↓↓↓↓↓↓↓↓请在这里配置您的基本信息↓↓↓↓↓↓↓↓↓↓↓↓↓↓↓
	// 合作身份者ID，以2088开头由16位纯数字组成的字符串
	public static String it_b_pay = "1h";
	public static String partner = "2088021239978032";
	public static String service = "create_direct_pay_by_user";
	// 交易安全检验码，由数字和字母组成的32位字符串
	public static String key = "rsa55e8ph1n3zoxtj72j1jmwje9gh470";
	// pc端appid
	public static String pc_app_id = "2016062001534483";
	// app端appid
	public static String app_app_id = "2017031406218327";
	// 签约支付宝账号或卖家收款支付宝帐户
	public static String seller_email = "service@13322.com";
	// 读配置文件
	// notify_url 交易过程中服务器通知的页面 要用 http://格式的完整路径，不允许加?id=123这类自定义参数
	public static String notify_url = "http:www.xxx.com/projectName/alipayTrade.action";
	// 付完款后跳转的页面 要用 http://格式的完整路径，不允许加?id=123这类自定义参数
	// return_url的域名不能写成http://localhost/js_jsp_utf8/return_url.jsp，否则会导致return_url执行无效
	public static String return_url = "http:www.xxx.com/projectName/alipayTrade.action";
	// 网站商品的展示地址，不允许加?id=123这类自定义参数
	public static String show_url = "http://www.alipay.com";
	// 收款方名称，如：公司名称、网站名称、收款人姓名等
	public static String mainname = "深圳益彩网络科技有限公司";
	// ↑↑↑↑↑↑↑↑↑↑请在这里配置您的基本信息↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑
	// 字符编码格式 目前支持 gbk 或 utf-8
	public static String input_charset = "UTF-8";
	// 签名方式 不需修改
	public static String sign_type = "MD5";
	// 访问模式,根据自己的服务器是否支持ssl访问，若支持请选择https；若不支持请选择http
	public static String transport = "http";
	// 商户的私钥
	// RSA 支付宝公钥
	public static String alipay_public_key = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDDI6d306Q8fIfCOaTXyiUeJHkrIvYISRcc73s3vF1ZT7XN8RNPwJxo8pWaJMmvyTn9N4HQ632qJBVHf8sxHi/fEsraprwCtzvzQETrNRwVxLO5jVmRGi60j8Ue1efIlzPXV9je9mkjzOmdssymZkh2QhUrCmZYI/FCEa3/cNMW0QIDAQAB";
	// RSA 商户私钥
	public static String seller_private_key = "MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBALOgv+fA0OQqpfLuGc5M6iMiDLLRzEd2kVr/t6dHVzRdf3j+NskT688477rTkrofg0j/U981n5tYEnlZ9rdrFlFyMs0od5wVEVTkS9I3cMkBK5tYsN/JA5uakcBroUuVQ7pBoXLPBJS+u7lp+R4Ieu8Q0tQWVFxw0UPWglaQsU8NAgMBAAECgYA7r14Uk2zVY5Zgcd0BP21M3zN491z5IdyKcA2F955yN97zRQTPK19fK3v8h/BpB6+Hwc8Mi7TM2SbKS6o+p8ZyGo75whyRzSRvTHGv/YDG1W4o3GzEUMVZWwiWMvF78fJBubDmwcmxa9fjCoccvaRTX+9iU6Q+zqkEi8S0ACa3wQJBAOkc9UyAM+w4A0tErY6ZtC3HibOb//gEML90anD2JNTfeZwlizGWAmYSyGFOdxfzVi5ULyWKirLJ3zpHFhj61IsCQQDFQ4ABUsaoflJW/ohcRpJmzNvpq7goKT515US6D5u1IH6T9clMu6dZHWL+A4laOkcDMO8D5aD9s6bzxqJouiXHAkAiMXI1t3RmSM0K5FcE3LzWHjevlclVCuBPpPbb/O7oHqOK6OLi8hsF9+lWhJjpdXMFtql3uzufAAdzV+wA3AIDAkBaKQnekPjfli0IOfoVQuQzPDrfSG85W/CCbjHFYNDoLlvsQJCrE7HfV0EhnHgw1yYR3VHBVDkK/Au4q/cbKsmNAkEAiDDvaFk0OVN4Pn1kVal8ait1V+jQgP3G2AXEM890Xni5A3SMqLWqS+4Sdw4ghoRbu5gKL3/UGIDRl7motiRrKA==";
	// 支付方式
	public static String payment_type = "1";
	/*
	 * 扫码支付的方式，支持前置模式和跳转模式。
	 * 前置模式是将二维码前置到商户的订单确认页的模式。需要商户在自己的页面中以iframe方式请求支付宝页面。具体分为以下3种：
	 * 0：订单码-简约前置模式，对应iframe宽度不能小于600px，高度不能小于300px；
	 * 1：订单码-前置模式，对应iframe宽度不能小于300px，高度不能小于600px
	 * 2：订单码-跳转模式 跳转模式下，用户的扫码界面是由支付宝生成的，不在商户的域名下。  
	 * 3：订单码-迷你前置模式，对应iframe宽度不能小于75px，高度不能小于75px。
	 */
	public static String qr_pay_mode = "2";

	// 下面为移动支付所需
	// 移动支付操作中断返回地址
	public static String merchant_url = "";
	// 请求参数格式
	public static String format = "xml";
	// 接口版本号
	public static String v = "2.0";

}
