package com.hhly.paycore.paychannel.kjpay.config;

import org.apache.log4j.Logger;

/**
 * @author lgs on
 * @version 1.0
 * @desc 快接支付配置文件
 * @date 2018/4/18.
 * @company 益彩网络科技有限公司
 */
public class KjConfig {
    private static Logger logger = Logger.getLogger(KjConfig.class);

    /**
     * 商户号
     **/
    public static String KJ_PAY_PARTNER_CODE;
    /**
     * 秘钥
     **/
    public static String KJ_PAY_KEY;

    /**
     * 快接支付请求地址 固定不变
     **/
    public static String KJ_PAY_URL;

}
