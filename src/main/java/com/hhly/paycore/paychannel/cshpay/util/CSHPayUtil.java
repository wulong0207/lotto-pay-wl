package com.hhly.paycore.paychannel.cshpay.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.cshpay.config.CSHConfig;
import com.hhly.paycore.paychannel.kjpay.config.KjConfig;
import com.hhly.paycore.paychannel.kjpay.util.KjPayUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.utils.BuildRequestFormUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author lgs on
 * @version 1.0
 * @desc
 * @date 2018/7/3.
 * @company 益彩网络科技有限公司
 */
public class CSHPayUtil {

    private static final String SUCCESS = "200";
    private static Logger logger = Logger.getLogger(CSHPayUtil.class);

    /**
     * 构建请求参数
     *
     * @param paymentInfo
     * @return
     */
    public static String buildWxScanCodeParam(PaymentInfoBO paymentInfo) throws Exception {
        Map<String, String> result = new TreeMap<>();
        result.put("bussOrderNum", paymentInfo.getNoOrder());
        result.put("orderName", paymentInfo.getNameGoods());
        result.put("payMoney", paymentInfo.getMoneyOrder());
        result.put("notifyUrl", paymentInfo.getNotifyUrl());
        if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey())) {
            result.put("appType", "2");
            result.put("returnUrl", paymentInfo.getUrlReturn());
        } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey())) {
            result.put("appType", "2");
        } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey())) {
            result.put("appType", "1");
        }

        result.put("ip", paymentInfo.getUserreqIp());// ip地址
        String keyValue = null;
        String appKey = null;
        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
            result.put("payPlatform", "2");
            result.put("remark", "2");
            keyValue = CSHConfig.CSH_WX_KEYVALYE;
            appKey = CSHConfig.CSH_WX_APPKEY;
        } else if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
            result.put("payPlatform", "1");
            result.put("remark", "1");
            keyValue = CSHConfig.CSH_ALIPAY_KEYVALYE;
            appKey = CSHConfig.CSH_ALIPAY_APPKEY;
        }

        result.put("appKey", appKey);
        String sign = PaySignUtil.getSign(result, keyValue);
        String paramStr = PaySignUtil.getParamStr(result) + "&sign=" + sign;
        return paramStr;
    }


    /**
     * 方法说明: 解析并验证异步通知
     *
     * @param map
     * @auth: xiongJinGang
     * @time: 2018年1月3日 上午10:29:30
     * @return: ResultBO<?>
     */
    public static ResultBO<?> payNotify(Map<String, String> map) throws Exception {
        if (ObjectUtil.isBlank(map)) {
            logger.info("加密狗支付异步通知结果为空");
            return ResultBO.err();
        }
        PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
        if (map.containsKey("sign")) {
            String keyValue = null;
            if (map.get("remark").equals("2")) {
                keyValue = CSHConfig.CSH_WX_KEYVALYE;
            } else if (map.get("remark").equals("1")){
                keyValue = CSHConfig.CSH_ALIPAY_KEYVALYE;
            }

            String needMd5Str = PaySignUtil.getSign(map, keyValue);

            if (!map.get("sign").equals(needMd5Str)) {
                logger.info("加密狗支付异步通知，验证签名不通过");
                return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
            }
            // 状态码1：成功 其它为失败
            // 我们的充值编号都是大写I开头，但快接微信公众号支付只能全部数据，所以在调用接口之前将I全部去掉了，这里需要补上
            String orderNo = map.get("buss_order_num");
            if (!orderNo.startsWith("I")) {
                orderNo = "I" + orderNo;
            }

            payNotifyResult.setOrderCode(orderNo);// 商户唯一订单号
            payNotifyResult.setThirdTradeNo(map.get("order_num"));
            payNotifyResult.setOrderAmt(Double.valueOf(map.get("pay_money")));// 总金额，整数，单位为分
            payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
            payNotifyResult.setResponse("SUCCESS");// success、error
            if (ObjectUtil.isNotNull(map.get("result_code")) && "200".equals(map.get("result_code"))) {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
            } else {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);
            }
            return ResultBO.ok(payNotifyResult);
        }
        logger.error("加密狗支付异步通知，无sign参数");
        return ResultBO.err();
    }


    public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
        try {
            String transCode = payQueryParamVO.getTransCode();

            String appKey = null;
            String keyValue = null;
            String payUrl = null;
            if (payQueryParamVO.getRechargeChannel().equals("2")) {
                keyValue = CSHConfig.CSH_WX_KEYVALYE;
                appKey = CSHConfig.CSH_WX_APPKEY;
                payUrl = CSHConfig.CSH_WX_HEAD_URL + CSHConfig.CSH_QUERY_URL;
            } else if (payQueryParamVO.getRechargeChannel().equals("1")) {
                keyValue = CSHConfig.CSH_ALIPAY_KEYVALYE;
                appKey = CSHConfig.CSH_ALIPAY_APPKEY;
                payUrl = CSHConfig.CSH_ALIPAY_HEAD_URL + CSHConfig.CSH_QUERY_URL;
            }


            SortedMap<String, String> paramMap = new TreeMap<String, String>();
            paramMap.put("appKey", appKey);// 商户号
            paramMap.put("bussOrderNum", transCode);// 订单号
            paramMap.put("orderNum", payQueryParamVO.getTradeNo());
            String sign = PaySignUtil.getSign(paramMap, keyValue);
            paramMap.put("sign", sign);// MD5签名结果


            String paramStr = PaySignUtil.getParamStr(paramMap) + "&sign=" + sign;
            String result = HttpUtil.doPost(payUrl + paramStr);
            if (!ObjectUtil.isBlank(result)) {
                logger.info("全民金服快接支付结果返回：" + result);
                JSONObject jsonObject = JSONObject.parseObject(result);
                String code = jsonObject.getString("resultCode");

                JSONObject detail = jsonObject.getJSONObject("Data");
                Map<String, String> resultMap = new HashMap<String, String>();

                detail.entrySet().forEach(k -> {
                    resultMap.put(k.getKey(), (String) k.getValue());
                });

                String orderStatus = detail.getString("status");
                String countSing = PaySignUtil.getSign(resultMap, keyValue);
                String returnSign = detail.getString("sign");
                if (!countSing.equals(returnSign)) {
                    logger.info("验证签名不通过");
                    return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
                }
                PayConstants.PayStatusEnum payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                if ("2012".equals(code)) {// 等待支付
                    payStatusEnum = PayConstants.PayStatusEnum.WAITTING_PAYMENT;
                } else if ("200".equals(code)) {// 支付成功
                    payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                } else if ("2011".equals(code)) {// 支付失败
                    payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_FAILURE;
                }
                PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
                payQueryResultVO.setTotalAmount(resultMap.get("pay_money"));
                payQueryResultVO.setTradeNo(resultMap.get("order_num"));
                payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
                payQueryResultVO.setOrderCode(resultMap.get("buss_order_num"));
                payQueryResultVO.setTradeStatus(payStatusEnum);
                if (SUCCESS.equals(code)) {
                    return ResultBO.ok(payQueryResultVO);
                } else {
                    return ResultBO.err();
                }
            }
        } catch (Exception e) {
            logger.error("查询快接支付结果异常", e);
        }
        return ResultBO.err();
    }
}
