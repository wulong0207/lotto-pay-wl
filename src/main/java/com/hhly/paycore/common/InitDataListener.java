package com.hhly.paycore.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.hhly.paycore.paychannel.yypay.config.YYPayConfig;
import org.apache.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.hhly.paycore.paychannel.cshpay.config.CSHConfig;
import com.hhly.paycore.paychannel.hdapay.config.HaoDianAConfig;
import com.hhly.paycore.paychannel.hongyuepay.config.HongYueConfig;
import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.hhly.paycore.paychannel.huichao.config.HuiChaoConfig;
import com.hhly.paycore.paychannel.juhepay.config.JuhePayConfig;
import com.hhly.paycore.paychannel.kjpay.config.KjConfig;
import com.hhly.paycore.paychannel.lianlianpay.config.LianPayConfig;
import com.hhly.paycore.paychannel.national.config.NationalConfig;
import com.hhly.paycore.paychannel.nowpay.config.NowPayConfig;
import com.hhly.paycore.paychannel.palmpay.config.PalmPayConfig;
import com.hhly.paycore.paychannel.point.config.PointConfig;
import com.hhly.paycore.paychannel.pufa.config.PuFaConfig;
import com.hhly.paycore.paychannel.sandpay.config.SandPayConfig;
import com.hhly.paycore.paychannel.shenzhoupay.config.DivineConfig;
import com.hhly.paycore.paychannel.smartcloud.config.CloudConfig;
import com.hhly.paycore.paychannel.swiftpass.config.SwiftpassPayConfig;
import com.hhly.paycore.paychannel.weifutong.guangda.config.WFTGuangdaConfig;
import com.hhly.paycore.paychannel.weifutong.xingye.config.WFTXingYeConfig;
import com.hhly.paycore.paychannel.weifutong.zxsz1.config.WFTZhongxin1Config;
import com.hhly.paycore.paychannel.weifutong.zxsz2.config.WFTZhongxin2Config;
import com.hhly.paycore.paychannel.xingye.config.XingYeConfig;
import com.hhly.paycore.paychannel.yeepay.config.YeepayConfig;
import com.hhly.paycore.paychannel.yeepay2.config.Yeepay2Config;
import com.hhly.paycore.paychannel.zhangling.ZhangLingConfig;

/**
 * @desc 数据初始化
 * @author xiongJinGang
 * @date 2017年9月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */

@Component
public class InitDataListener implements CommandLineRunner {
	private static Logger logger = Logger.getLogger(InitDataListener.class);

	@Override
	public void run(String... args) throws Exception {
		try {
			Properties p1 = new Properties();
			InputStream in1 = getClass().getResourceAsStream("/application.properties");
			p1.load(in1);
			in1.close();

			String active = p1.getProperty("spring.profiles.active").trim();
			Properties p = new Properties();
			InputStream in = getClass().getResourceAsStream("/application-" + active + ".properties");
			p.load(in);
			in.close();
			// 一键支付移动端paymobile，生产环境测试商户编号
			YeepayConfig.MERCHANT_ACCOUNT = p.getProperty("YEEPAY_MERCHANT_ACCOUNT").trim();
			// 商户私钥
			YeepayConfig.MERCHANT_PRIVATE_KEY = p.getProperty("YEEPAY_MERCHANT_PRIVATE_KEY").trim();
			// 易宝公玥
			YeepayConfig.YEEPAY_PUBLIC_KEY = p.getProperty("YEEPAY_PUBLIC_KEY").trim();
			// 4.1 订单支付接口请求地址
			YeepayConfig.PAY_API_URL = p.getProperty("YEEPAY_PAY_API_URL").trim();
			// 4.2 订单查询接口请求地址
			YeepayConfig.QueryOrderApi = p.getProperty("YEEPAY_QUERYORDERAPI").trim();
			// 4.3 消费对账文件下载接口请求地址
			YeepayConfig.PayClearDataApi = p.getProperty("YEEPAY_PAYCLEARDATAAPI").trim();
			// 4.4 单笔退款接口请求地址
			YeepayConfig.REFUND_API_URL = p.getProperty("YEEPAY_REFUND_API_URL").trim();
			// 4.5 退款查询接口请求地址
			YeepayConfig.QueryRefundApi = p.getProperty("YEEPAY_QUERYREFUNDAPI").trim();
			// 4.6 退款对账文件下载接口
			YeepayConfig.RefundClearDataApi = p.getProperty("YEEPAY_REFUNDCLEARDATAAPI").trim();
			// 4.7 银行卡信息查询接口请求地址
			YeepayConfig.CheckBankcardApi = p.getProperty("YEEPAY_CHECKBANKCARDAPI").trim();
			// 4.8 查询绑卡信息列表
			YeepayConfig.QueryBankCardListApi = p.getProperty("YEEPAY_QUERYBANKCARDLISTAPI").trim();
			// 4.9 解绑卡
			YeepayConfig.UnbindCardApi = p.getProperty("YEEPAY_UNBINDCARDAPI").trim();

			// 易宝网银标准版配置
			// 商户编号
			YeepayConfig.P1_MERID = p.getProperty("YEEPAY_P1_MERID").trim();
			// p1_MerId= p.getProperty("YEEPAY_MERCHANT_ACCOUNT").trim();//10012442782

			// 商户密钥
			YeepayConfig.KEY_VALUE = p.getProperty("YEEPAY_KEY_VALUE").trim();
			// keyValue= p.getProperty("YEEPAY_MERCHANT_ACCOUNT").trim();//mP42238826nuW64r7yh26DGK34o2L2m81L25RG32lD7Lo1058A7iJ28at6QS

			// 下单请求地址
			YeepayConfig.REQUEST_URL = p.getProperty("YEEPAY_REQUEST_URL").trim();
			// 订单查询请求地址
			YeepayConfig.QUERY_URL = p.getProperty("YEEPAY_QUERY_URL").trim();
			// 退款请求地址
			YeepayConfig.REFUND_URL = p.getProperty("YEEPAY_REFUND_URL").trim();
			// 退款查询请求地址
			YeepayConfig.REFUND_QUERY_URL = p.getProperty("YEEPAY_REFUND_QUERY_URL").trim();
			// 订单取消请求地址
			YeepayConfig.CANCEL_ORDER_URL = p.getProperty("YEEPAY_CANCEL_ORDER_URL").trim();

			/**************聚合支付配置文件加载****************/
			JuhePayConfig.JUHEPAY_CPID = p.getProperty("JUHEPAY_CPID").trim();
			JuhePayConfig.JUHEPAY_MD5_KEY = p.getProperty("JUHEPAY_MD5_KEY").trim();
			JuhePayConfig.JUHEPAY_NOTIFY_URL = p.getProperty("recharge.notify.url").trim();
			JuhePayConfig.JUHEPAY_URL = p.getProperty("JUHEPAY_URL").trim();

			/**************现在支付配置文件加载****************/
			/** app端 支付宝+QQ钱包 appid  **/
			NowPayConfig.NOW_PAY_APP_ALI_QQ_APPID = p.getProperty("NOW_PAY_APP_ALI_QQ_APPID").trim();
			/** app端  支付宝+QQ钱包 md5key **/
			NowPayConfig.NOW_PAY_APP_ALI_QQ_MD5KEY = p.getProperty("NOW_PAY_APP_ALI_QQ_MD5KEY").trim();
			/** app端微信 appid  **/
			NowPayConfig.NOW_PAY_APP_WX_APPID = p.getProperty("NOW_PAY_APP_WX_APPID").trim();
			/** app端微信 md5key **/
			NowPayConfig.NOW_PAY_APP_WX_MD5KEY = p.getProperty("NOW_PAY_APP_WX_MD5KEY").trim();
			/** wap支付宝H5 appid  **/
			NowPayConfig.NOW_PAY_WAP_ALI_APPID = p.getProperty("NOW_PAY_WAP_ALI_APPID").trim();
			/** wap支付宝H5 md5key **/
			NowPayConfig.NOW_PAY_WAP_ALI_MD5KEY = p.getProperty("NOW_PAY_WAP_ALI_MD5KEY").trim();
			/** wap手Q appid  **/
			NowPayConfig.NOW_PAY_WAP_QQ_APPID = p.getProperty("NOW_PAY_WAP_QQ_APPID").trim();
			/** wap手Q md5key **/
			NowPayConfig.NOW_PAY_WAP_QQ_MD5KEY = p.getProperty("NOW_PAY_WAP_QQ_MD5KEY").trim();
			/** wap微信 appid **/
			NowPayConfig.NOW_PAY_WAP_WX_APPID = p.getProperty("NOW_PAY_WAP_WX_APPID").trim();
			/** wap微信 md5key **/
			NowPayConfig.NOW_PAY_WAP_WX_MD5KEY = p.getProperty("NOW_PAY_WAP_WX_MD5KEY").trim();
			/** 现在支付请求路径 **/
			NowPayConfig.NOW_PAY_URL = p.getProperty("NOW_PAY_URL").trim();

			/**************神州支付配置文件加载****************/
			DivineConfig.SHENZHOU_PAY_URL = p.getProperty("SHENZHOU_PAY_URL").trim();
			DivineConfig.SHENZHOU_PAY_WX_APPID = p.getProperty("SHENZHOU_PAY_WX_APPID").trim();
			DivineConfig.SHENZHOU_PAY_WX_KEY = p.getProperty("SHENZHOU_PAY_WX_KEY").trim();
			DivineConfig.SHENZHOU_RSA_PUBLIC_KEY = p.getProperty("SHENZHOU_RSA_PUBLIC_KEY").trim();
			DivineConfig.SHENZHOU_PAY_QUERY_URL = p.getProperty("SHENZHOU_PAY_QUERY_URL").trim();
			DivineConfig.SHENZHOU_PAY_APP_IOS_VERSION = p.getProperty("SHENZHOU_PAY_APP_IOS_VERSION").trim();
			DivineConfig.SHENZHOU_PAY_APP_ANDROID_VERSION = p.getProperty("SHENZHOU_PAY_APP_ANDROID_VERSION").trim();
			DivineConfig.SHENZHOU_MER_ID = p.getProperty("SHENZHOU_MER_ID").trim();
			DivineConfig.SHENZHOU_PRIVATE_KEY = p.getProperty("SHENZHOU_PRIVATE_KEY").trim();
			DivineConfig.SHENZHOU_DES_KEY = p.getProperty("SHENZHOU_DES_KEY").trim();
			DivineConfig.SHENZHOU_CARD_WEB_PAY_URL = p.getProperty("SHENZHOU_CARD_WEB_PAY_URL").trim();
			DivineConfig.SHENZHOU_CARD_WAP_PAY_URL = p.getProperty("SHENZHOU_CARD_WAP_PAY_URL").trim();

			/**************连连支付配置文件加载****************/
			LianPayConfig.YT_PUB_KEY = p.getProperty("LL_YT_PUB_KEY").trim();
			LianPayConfig.TRADER_PRI_KEY = p.getProperty("LL_TRADER_PRI_KEY").trim();
			LianPayConfig.MD5_KEY = p.getProperty("LL_MD5_KEY").trim();
			LianPayConfig.OID_PARTNER = p.getProperty("LL_OID_PARTNER").trim();
			LianPayConfig.SIGN_TYPE = p.getProperty("LL_SIGN_TYPE").trim();
			LianPayConfig.VERSION = p.getProperty("LL_VERSION").trim();
			LianPayConfig.BUSI_PARTNER = p.getProperty("LL_BUSI_PARTNER").trim();
			LianPayConfig.PAY_URL = p.getProperty("LL_PAY_URL").trim();
			LianPayConfig.QUERY_URL = p.getProperty("LL_QUERY_URL").trim();
			LianPayConfig.QUERY_USER_BANKCARD_URL = p.getProperty("LL_QUERY_USER_BANKCARD_URL").trim();
			LianPayConfig.QUERY_BANKCARD_URL = p.getProperty("LL_QUERY_BANKCARD_URL").trim();

			// 连连快捷支付配置
			LianPayConfig.FAST_PUB_KEY = p.getProperty("LL_FAST_PUB_KEY").trim();
			LianPayConfig.FAST_PRI_KEY = p.getProperty("LL_FAST_PRI_KEY").trim();
			LianPayConfig.FAST_SIGN_TYPE = p.getProperty("LL_FAST_SIGN_TYPE").trim();
			LianPayConfig.FAST_OID_PARTNER = p.getProperty("LL_FAST_OID_PARTNER").trim();
			LianPayConfig.FAST_MD5_KEY = p.getProperty("LL_FAST_MD5_KEY").trim();

			// 连连WAP支付配置
			LianPayConfig.WAP_PAY_URL = p.getProperty("LL_WAP_PAY_URL").trim();
			LianPayConfig.WAP_VERSION = p.getProperty("LL_WAP_VERSION").trim();

			/**************掌宜付支付配置文件加载****************/
			PalmPayConfig.PALM_PARTNER_ID = p.getProperty("PALM_PARTNER_ID").trim();
			PalmPayConfig.PALM_PAY_APP_ID = p.getProperty("PALM_PAY_APP_ID").trim();
			PalmPayConfig.PALM_PAY_KEY = p.getProperty("PALM_PAY_KEY").trim();
			PalmPayConfig.PALM_RECHARGE_APP_ID = p.getProperty("PALM_RECHARGE_APP_ID").trim();
			PalmPayConfig.PALM_RECHARGE_KEY = p.getProperty("PALM_RECHARGE_KEY").trim();
			PalmPayConfig.PALM_PAY_URL = p.getProperty("PALM_PAY_URL").trim();
			PalmPayConfig.PALM_QUERY_URL = p.getProperty("PALM_QUERY_URL").trim();

			/**************威富通支付配置文件加载****************/
			// 微信公众号
			SwiftpassPayConfig.SWIFTPASS_PAY_PARTNER_CODE = p.getProperty("SWIFTPASS_PAY_PARTNER_CODE").trim();
			SwiftpassPayConfig.SWIFTPASS_PAY_KEY = p.getProperty("SWIFTPASS_PAY_KEY").trim();
			SwiftpassPayConfig.SWIFTPASS_PAY_COUNT_POINT = p.getProperty("SWIFTPASS_PAY_COUNT_POINT").trim();
			SwiftpassPayConfig.SWIFTPASS_PAY_URL = p.getProperty("SWIFTPASS_PAY_URL").trim();
			SwiftpassPayConfig.SWIFTPASS_QUERY_PARTNER_CODE = p.getProperty("SWIFTPASS_QUERY_PARTNER_CODE").trim();
			SwiftpassPayConfig.SWIFTPASS_QUERY_KEY = p.getProperty("SWIFTPASS_QUERY_KEY").trim();
			SwiftpassPayConfig.SWIFTPASS_QUERY_URL = p.getProperty("SWIFTPASS_QUERY_URL").trim();

			/**************威富通支付配置文件加载****************/
			WFTXingYeConfig.WEIFUTONG_TRADE_URL = p.getProperty("WEIFUTONG_TRADE_URL").trim();
			// 支付宝扫码
			WFTXingYeConfig.WEIFUTONG_ALI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ALI_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_ALI_PAY_KEY = p.getProperty("WEIFUTONG_ALI_PAY_KEY").trim();
			// 微信扫码
			WFTXingYeConfig.WEIFUTONG_WX_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_WX_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_WX_PAY_KEY = p.getProperty("WEIFUTONG_WX_PAY_KEY").trim();
			// 微信WAP
			WFTXingYeConfig.WEIFUTONG_WX_WAP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_WX_WAP_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_WX_WAP_PAY_KEY = p.getProperty("WEIFUTONG_WX_WAP_PAY_KEY").trim();
			// 微信APP
			WFTXingYeConfig.WEIFUTONG_WX_APP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_WX_APP_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_WX_APP_PAY_KEY = p.getProperty("WEIFUTONG_WX_APP_PAY_KEY").trim();
			// QQ扫码
			WFTXingYeConfig.WEIFUTONG_QQ_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_QQ_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_QQ_PAY_KEY = p.getProperty("WEIFUTONG_QQ_PAY_KEY").trim();
			// 微信公众号
			WFTXingYeConfig.WEIFUTONG_WX_JSAPI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_WX_JSAPI_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_WX_JSAPI_PAY_KEY = p.getProperty("WEIFUTONG_WX_JSAPI_PAY_KEY").trim();
			// QQ钱包支付
			WFTXingYeConfig.WEIFUTONG_QQ_WALLET_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_QQ_WALLET_PAY_PARTNER_CODE").trim();
			WFTXingYeConfig.WEIFUTONG_QQ_WALLET_PAY_KEY = p.getProperty("WEIFUTONG_QQ_WALLET_PAY_KEY").trim();

			/**************威富通光大支付配置文件加载****************/
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_TRADE_URL = p.getProperty("WEIFUTONG_GUANGDA_TRADE_URL").trim();
			// 支付宝扫码
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_ALI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_ALI_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_ALI_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_ALI_PAY_KEY").trim();
			// 微信扫码
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_WX_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_WX_PAY_KEY").trim();
			// 微信WAP
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_WAP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_WX_WAP_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_WAP_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_WX_WAP_PAY_KEY").trim();
			// 微信APP
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_APP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_WX_APP_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_APP_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_WX_APP_PAY_KEY").trim();
			// 微信公众号
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_JSAPI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_WX_JSAPI_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_WX_JSAPI_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_WX_JSAPI_PAY_KEY").trim();
			// QQ扫码
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_QQ_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_QQ_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_QQ_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_QQ_PAY_KEY").trim();
			// QQ钱包支付
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_QQ_WALLET_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_GUANGDA_QQ_WALLET_PAY_PARTNER_CODE").trim();
			WFTGuangdaConfig.WEIFUTONG_GUANGDA_QQ_WALLET_PAY_KEY = p.getProperty("WEIFUTONG_GUANGDA_QQ_WALLET_PAY_KEY").trim();

			/**************威富通中信深圳一支付配置文件加载****************/
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_TRADE_URL = p.getProperty("WEIFUTONG_ZXSZ1_TRADE_URL").trim();
			// 支付宝扫码
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_ALI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_ALI_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_ALI_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_ALI_PAY_KEY").trim();
			// 微信扫码
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_WX_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_WX_PAY_KEY").trim();
			// 微信WAP
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_WAP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_WX_WAP_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_WAP_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_WX_WAP_PAY_KEY").trim();
			// 微信APP
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_APP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_WX_APP_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_APP_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_WX_APP_PAY_KEY").trim();
			// 微信公众号
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_JSAPI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_WX_JSAPI_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_WX_JSAPI_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_WX_JSAPI_PAY_KEY").trim();
			// QQ扫码
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_QQ_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_QQ_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_QQ_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_QQ_PAY_KEY").trim();
			// QQ钱包支付
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_QQ_WALLET_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ1_QQ_WALLET_PAY_PARTNER_CODE").trim();
			WFTZhongxin1Config.WEIFUTONG_ZXSZ1_QQ_WALLET_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ1_QQ_WALLET_PAY_KEY").trim();

			/**************威富通中信深圳二支付配置文件加载****************/
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_TRADE_URL = p.getProperty("WEIFUTONG_ZXSZ2_TRADE_URL").trim();
			// 支付宝扫码
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_ALI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_ALI_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_ALI_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_ALI_PAY_KEY").trim();
			// 微信扫码
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_WX_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_WX_PAY_KEY").trim();
			// 微信WAP
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_WAP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_WX_WAP_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_WAP_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_WX_WAP_PAY_KEY").trim();
			// 微信APP
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_APP_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_WX_APP_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_APP_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_WX_APP_PAY_KEY").trim();
			// 微信公众号
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_JSAPI_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_WX_JSAPI_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_WX_JSAPI_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_WX_JSAPI_PAY_KEY").trim();
			// QQ扫码
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_QQ_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_QQ_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_QQ_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_QQ_PAY_KEY").trim();
			// QQ钱包支付
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_QQ_WALLET_PAY_PARTNER_CODE = p.getProperty("WEIFUTONG_ZXSZ2_QQ_WALLET_PAY_PARTNER_CODE").trim();
			WFTZhongxin2Config.WEIFUTONG_ZXSZ2_QQ_WALLET_PAY_KEY = p.getProperty("WEIFUTONG_ZXSZ2_QQ_WALLET_PAY_KEY").trim();

			/**************兴业支付配置文件加载****************/
			// 支付宝扫码
			XingYeConfig.XY_ALI_PAY_MCH_ID = p.getProperty("XY_ALI_PAY_MCH_ID").trim();
			XingYeConfig.XY_ALI_PAY_KEY = p.getProperty("XY_ALI_PAY_KEY").trim();
			XingYeConfig.XY_PAY_URL = p.getProperty("XY_PAY_URL").trim();
			XingYeConfig.XY_REFUND_URL = p.getProperty("XY_REFUND_URL").trim();
			// QQ扫码
			XingYeConfig.XY_QQ_PAY_MCH_ID = p.getProperty("XY_QQ_PAY_MCH_ID").trim();
			XingYeConfig.XY_QQ_PAY_KEY = p.getProperty("XY_QQ_PAY_KEY").trim();
			XingYeConfig.XY_QQ_REFUND_URL = p.getProperty("XY_QQ_REFUND_URL").trim();

			// QQ钱包
			XingYeConfig.XY_QQ_WALLET_PAY_MCH_ID = p.getProperty("XY_QQ_WALLET_PAY_MCH_ID").trim();
			XingYeConfig.XY_QQ_WALLET_PAY_KEY = p.getProperty("XY_QQ_WALLET_PAY_KEY").trim();

			// 微信APP支付
			XingYeConfig.XY_WX_PAY_MCH_ID = p.getProperty("XY_WX_PAY_MCH_ID").trim();
			XingYeConfig.XY_WX_PAY_KEY = p.getProperty("XY_WX_PAY_KEY").trim();

			/**************好店啊支付配置文件加载****************/
			// 支付宝、微信扫码
			HaoDianAConfig.HDA_NEW_DS_ID = p.getProperty("HDA_NEW_DS_ID").trim();
			HaoDianAConfig.HDA_NEW_DS_SECRET = p.getProperty("HDA_NEW_DS_SECRET").trim();

			HaoDianAConfig.HDA_ALIPAY_SCAN_MCH_ID = p.getProperty("HDA_ALIPAY_SCAN_MCH_ID").trim();
			HaoDianAConfig.HDA_WECHAT_SCAN_MCH_ID = p.getProperty("HDA_WECHAT_SCAN_MCH_ID").trim();
			HaoDianAConfig.HDA_QQ_SCAN_MCH_ID = p.getProperty("HDA_QQ_SCAN_MCH_ID").trim();
			HaoDianAConfig.HDA_ALIPAY_WAP_MCH_ID = p.getProperty("HDA_ALIPAY_WAP_MCH_ID").trim();
			HaoDianAConfig.HDA_WX_WAP_MCH_ID = p.getProperty("HDA_WX_WAP_MCH_ID").trim();
			HaoDianAConfig.HDA_QQ_WAP_MCH_ID = p.getProperty("HDA_QQ_WAP_MCH_ID").trim();

			HaoDianAConfig.HDA_NEW_PAY_URL = p.getProperty("HDA_NEW_PAY_URL").trim();

			/**************浦发银行支付配置文件加载****************/
			PuFaConfig.PF_PAY_URL = p.getProperty("PF_PAY_URL").trim();
			PuFaConfig.PF_MERCHANT_NO = p.getProperty("PF_MERCHANT_NO").trim();
			PuFaConfig.PF_WX_SUB_MCH_ID = p.getProperty("PF_WX_SUB_MCH_ID").trim();

			PuFaConfig.PF_APP_MERCHANT_NO = p.getProperty("PF_APP_MERCHANT_NO").trim();
			PuFaConfig.PF_APP_WX_SUB_MCH_ID = p.getProperty("PF_APP_WX_SUB_MCH_ID").trim();
			PuFaConfig.PF_APP_WX_APP_ID = p.getProperty("PF_APP_WX_APP_ID").trim();

			PuFaConfig.PF_PUBLIC_KEY_PATH = p.getProperty("PF_PUBLIC_KEY_PATH").trim();
			PuFaConfig.PF_PRIVATE_KEY_PATH = p.getProperty("PF_PRIVATE_KEY_PATH").trim();
			PuFaConfig.PF_PRIVATE_KEY_PFX_PATH = p.getProperty("PF_PRIVATE_KEY_PFX_PATH").trim();
			PuFaConfig.PF_PRIVATE_KEY_PWD = p.getProperty("PF_PRIVATE_KEY_PWD").trim();
			PuFaConfig.PF_AGENT_PRIVATE_KEY_PATH = p.getProperty("PF_AGENT_PRIVATE_KEY_PATH").trim();

			/**************汇潮支付配置文件加载****************/
			HuiChaoConfig.HUICHAO_PAY_URL = p.getProperty("HUICHAO_PAY_URL").trim();
			HuiChaoConfig.HUICHAO_QUERY_URL = p.getProperty("HUICHAO_QUERY_URL").trim();
			HuiChaoConfig.HUICHAO_MER_NO = p.getProperty("HUICHAO_MER_NO").trim();
			HuiChaoConfig.HUICHAO_KEY = p.getProperty("HUICHAO_KEY").trim();

			/**************华移支付配置文件加载****************/
			HuayiConfig.HUAYI_REQUEST_URL = p.getProperty("HUAYI_REQUEST_URL").trim();
			HuayiConfig.HUAYI_MERCHANT_NO = p.getProperty("HUAYI_MERCHANT_NO").trim();
			HuayiConfig.HUAYI_SECRET = p.getProperty("HUAYI_SECRET").trim();
			HuayiConfig.HUAYI_WXH5_MERCHANT_NO = p.getProperty("HUAYI_WXH5_MERCHANT_NO").trim();
			HuayiConfig.HUAYI_WXH5_REQUEST_URL = p.getProperty("HUAYI_WXH5_REQUEST_URL").trim();
			HuayiConfig.HUAYI_PRIVATE_KEY = p.getProperty("HUAYI_PRIVATE_KEY").trim();
			HuayiConfig.HUAYI_PUBLIC_KEY = p.getProperty("HUAYI_PUBLIC_KEY").trim();
			HuayiConfig.HUAYI_SEVEN_PUBLIC_KEY = p.getProperty("HUAYI_SEVEN_PUBLIC_KEY").trim();

			/**************鸿粤支付配置文件加载****************/
			HongYueConfig.HONGYUE_TRADE_URL = p.getProperty("HONGYUE_TRADE_URL").trim();
			// 微信扫码
			HongYueConfig.HONGYUE_WX_PAY_PARTNER_CODE = p.getProperty("HONGYUE_WX_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_WX_PAY_KEY = p.getProperty("HONGYUE_WX_PAY_KEY").trim();
			// 微信WAP
			HongYueConfig.HONGYUE_WX_WAP_PAY_PARTNER_CODE = p.getProperty("HONGYUE_WX_WAP_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_WX_WAP_PAY_KEY = p.getProperty("HONGYUE_WX_WAP_PAY_KEY").trim();
			/*
			// 支付宝扫码
			HongYueConfig.HONGYUE_ALI_PAY_PARTNER_CODE = p.getProperty("HONGYUE_ALI_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_ALI_PAY_KEY = p.getProperty("HONGYUE_ALI_PAY_KEY").trim();
			
			// 微信APP
			HongYueConfig.HONGYUE_WX_APP_PAY_PARTNER_CODE = p.getProperty("HONGYUE_WX_APP_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_WX_APP_PAY_KEY = p.getProperty("HONGYUE_WX_APP_PAY_KEY").trim();
			// 微信公众号
			HongYueConfig.HONGYUE_WX_JSAPI_PAY_PARTNER_CODE = p.getProperty("HONGYUE_WX_JSAPI_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_WX_JSAPI_PAY_KEY = p.getProperty("HONGYUE_WX_JSAPI_PAY_KEY").trim();
			// QQ扫码
			HongYueConfig.HONGYUE_QQ_PAY_PARTNER_CODE = p.getProperty("HONGYUE_QQ_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_QQ_PAY_KEY = p.getProperty("HONGYUE_QQ_PAY_KEY").trim();
			// QQ钱包支付
			HongYueConfig.HONGYUE_QQ_WALLET_PAY_PARTNER_CODE = p.getProperty("HONGYUE_QQ_WALLET_PAY_PARTNER_CODE").trim();
			HongYueConfig.HONGYUE_QQ_WALLET_PAY_KEY = p.getProperty("HONGYUE_QQ_WALLET_PAY_KEY").trim();
			*/

			/**************快接支付配置文件加载****************/
			// 商户号
			KjConfig.KJ_PAY_PARTNER_CODE = p.getProperty("KJ_PAY_PARTNER_CODE").trim();
			// 秘钥
			KjConfig.KJ_PAY_KEY = p.getProperty("KJ_PAY_KEY").trim();

			// 支付请求地址
			KjConfig.KJ_PAY_URL = p.getProperty("KJ_PAY_URL").trim();

			/**************掌灵支付配置文件加载****************/
			ZhangLingConfig.ZHANGLING_H5_URL = p.getProperty("ZHANGLING_H5_URL").trim();
			ZhangLingConfig.ZHANGLING_API_URL = p.getProperty("ZHANGLING_API_URL").trim();
			// 掌灵支付微信扫码、微信H5、支付宝扫码、支付宝H5是同一个账号
			ZhangLingConfig.ZHANGLING_PAY_PARTNER_CODE = p.getProperty("ZHANGLING_PAY_PARTNER_CODE").trim();
			ZhangLingConfig.ZHANGLING_PAY_KEY = p.getProperty("ZHANGLING_PAY_KEY").trim();
			ZhangLingConfig.ZHANGLING_PUBLIC_KEY = p.getProperty("ZHANGLING_PUBLIC_KEY").trim();

			/**************新易宝支付配置文件加载****************/
			Yeepay2Config.NEW_YEEPAY_URL = p.getProperty("NEW_YEEPAY_URL").trim();
			Yeepay2Config.NEW_YEEPAY_PARENT_MERCHANT_NO = p.getProperty("NEW_YEEPAY_PARENT_MERCHANT_NO").trim();
			Yeepay2Config.NEW_YEEPAY_MERCHANT_NO = p.getProperty("NEW_YEEPAY_MERCHANT_NO").trim();
			Yeepay2Config.NEW_YEEPAY_TRADE_ORDER_URI = p.getProperty("NEW_YEEPAY_TRADE_ORDER_URI").trim();
			Yeepay2Config.NEW_YEEPAY_ORDER_QUERY_URI = p.getProperty("NEW_YEEPAY_ORDER_QUERY_URI").trim();
			Yeepay2Config.NEW_YEEPAY_REFUNDU_RI = p.getProperty("NEW_YEEPAY_REFUNDU_RI").trim();
			Yeepay2Config.NEW_YEEPAY_REFUND_QUERY_URI = p.getProperty("NEW_YEEPAY_REFUND_QUERY_URI").trim();
			Yeepay2Config.NEW_YEEPAY_API_CASHIER = p.getProperty("NEW_YEEPAY_API_CASHIER").trim();
			Yeepay2Config.NEW_YEEPAY_CASHIER = p.getProperty("NEW_YEEPAY_CASHIER").trim();

			/**************指点支付配置文件加载****************/
			PointConfig.POINT_PAYMENT_MERCHANT_CODE = p.getProperty("POINT_PAYMENT_MERCHANT_CODE").trim();
			PointConfig.POINT_PAYMENT_KEY = p.getProperty("POINT_PAYMENT_KEY").trim();
			PointConfig.POINT_PAYMENT_URL = p.getProperty("POINT_PAYMENT_URL").trim();

			/**************六点支付配置文件加载****************/
			SandPayConfig.SAND_PAY_MERCHANT_CODE = p.getProperty("SAND_PAY_MERCHANT_CODE").trim();
			SandPayConfig.SAND_PAY_PAYMENT_KEY = p.getProperty("SAND_PAY_PAYMENT_KEY").trim();
			SandPayConfig.SAND_PAY_PAYMENT_URL = p.getProperty("SAND_PAY_PAYMENT_URL").trim();
			SandPayConfig.SAND_PAY_QQ_MERCHANT_CODE = p.getProperty("SAND_PAY_QQ_MERCHANT_CODE").trim();
			SandPayConfig.SAND_PAY_QQ_PAYMENT_KEY = p.getProperty("SAND_PAY_QQ_PAYMENT_KEY").trim();
			SandPayConfig.SAND_PAY_QQ_PAY_URL = p.getProperty("SAND_PAY_QQ_PAY_URL").trim();
			SandPayConfig.SAND_PAY_QQ_QUERY_URL = p.getProperty("SAND_PAY_QQ_QUERY_URL").trim();

			/**************CSH支付配置文件加载****************/
			CSHConfig.CSH_WX_APPKEY = p.getProperty("CSH_WX_APPKEY").trim();
			CSHConfig.CSH_WX_PAY_URL = p.getProperty("CSH_WX_PAY_URL").trim();
			CSHConfig.CSH_WX_KEYVALYE = p.getProperty("CSH_WX_KEYVALYE").trim();
			CSHConfig.CSH_WX_HEAD_URL = p.getProperty("CSH_WX_HEAD_URL").trim();

			CSHConfig.CSH_ALIPAY_APPKEY = p.getProperty("CSH_ALIPAY_APPKEY").trim();
			CSHConfig.CSH_ALIPAY_KEYVALYE = p.getProperty("CSH_ALIPAY_KEYVALYE").trim();
			CSHConfig.CSH_ALIPAY_PAY_URL = p.getProperty("CSH_ALIPAY_PAY_URL").trim();
			CSHConfig.CSH_ALIPAY_HEAD_URL = p.getProperty("CSH_ALIPAY_HEAD_URL").trim();
			CSHConfig.CSH_QUERY_URL = p.getProperty("CSH_QUERY_URL").trim();

			/**************智能云支付配置文件加载****************/
			CloudConfig.CLOUD_UID = p.getProperty("CLOUD_UID").trim();
			CloudConfig.CLOUD_SECRET = p.getProperty("CLOUD_SECRET").trim();
			CloudConfig.CLOUD_URL = p.getProperty("CLOUD_URL").trim();
			CloudConfig.CLOUD_SAVE_URL = p.getProperty("CLOUD_SAVE_URL").trim();

			/**************YY支付配置文件加载****************/
			YYPayConfig.YYPAY_UID = p.getProperty("YYPAY_UID").trim();
			YYPayConfig.YYPAY_SECRET = p.getProperty("YYPAY_SECRET").trim();
			YYPayConfig.YYPAY_URL = p.getProperty("YYPAY_URL").trim();
			YYPayConfig.YYPAY_QUERY_URL = p.getProperty("YYPAY_QUERY_URL").trim();

			/**************国连支付配置文件加载****************/
			NationalConfig.NATIONAL_UID = p.getProperty("NATIONAL_UID").trim();
			NationalConfig.NATIONAL_SECRET = p.getProperty("NATIONAL_SECRET").trim();
			NationalConfig.NATIONAL_URL = p.getProperty("NATIONAL_URL").trim();

			logger.info("支付配置文件加载完成");
		} catch (IOException e) {
			logger.error("支付配置文件加载异常：", e);
		}
	}

}
