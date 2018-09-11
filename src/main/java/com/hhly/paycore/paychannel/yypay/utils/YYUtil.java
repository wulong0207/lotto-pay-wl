package com.hhly.paycore.paychannel.yypay.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.cshpay.config.CSHConfig;
import com.hhly.paycore.paychannel.cshpay.util.CSHPayUtil;
import com.hhly.paycore.paychannel.cshpay.util.PaySignUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.yypay.config.YYPayConfig;
import com.hhly.paycore.sign.MD5;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.exception.ServiceRuntimeException;
import com.hhly.skeleton.base.util.*;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.utils.BuildRequestFormUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author lgs on
 * @version 1.0
 * @desc YY支付工具类
 * @date 2018/8/7.
 * @company 益彩网络科技有限公司
 */
public class YYUtil {
    private static final String SUCCESS = "200";
    private static Logger logger = Logger.getLogger(YYUtil.class);

    /**
     * 公共请求
     *
     * @param paymentInfo
     * @return
     */
    public static JSONObject buildRequest(PaymentInfoBO paymentInfo) throws Exception {
        Map<String, String> result = new HashMap<>();
        result.put("merId", YYPayConfig.YYPAY_UID);
        result.put("outTradeNo", paymentInfo.getNoOrder());
        result.put("body", paymentInfo.getNameGoods());
        result.put("totalFee", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
        result.put("notifyUrl", paymentInfo.getNotifyUrl());
        if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey())) {
            result.put("payType", "H5");
            result.put("callBackUrl", paymentInfo.getUrlReturn());
        } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey())) {
            result.put("payType", "APP");
        } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())) {
            result.put("payType", "PC");
        }
        result.put("nonceStr", DateUtil.getNow());// ip地址
        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
            result.put("payChannel", "WXPAY");
        } else if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
            result.put("payChannel", "ALIPAY");
        }

        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey()))) {
            throw new ServiceRuntimeException("微信支付不支持H5与APP");
        }

        String pararmStr = BuildRequestFormUtil.createLinkString(result, false) + "&key=" + YYPayConfig.YYPAY_SECRET;
        String sign = DigestUtils.md5Hex(pararmStr).toUpperCase();
        result.put("sign", sign);
        logger.info("YY支付请求参数::::::::" + pararmStr);
        String json = HttpUtil.doPost(YYPayConfig.YYPAY_URL, result);
        logger.info("YY支付请求返回::::::::" + json);
        if (StringUtil.isBlank(json)) {
            throw new ServiceRuntimeException("YY支付请求返回空值");
        }
        return JsonUtil.jsonToObject(json, JSONObject.class);
    }


    /**
     * 方法说明: 解析支付请求返回
     *
     * @param result
     * @param paymentInfo
     * @time: 2018年3月27日 上午11:55:41
     * @return: ResultBO<?>
     */
    public static ResultBO<?> analyResultJson(JSONObject result, PaymentInfoBO paymentInfo) throws IOException {
        if (result.getString("code").equals("0")) {

            Map<String, String> map = result.getJSONObject("data").toJavaObject(Map.class);
            String mapSign = map.get("sign");
            map.remove("sign");
            String pararmStr = BuildRequestFormUtil.createLinkString(map, false) + "&key=" + YYPayConfig.YYPAY_SECRET;

            String sign = DigestUtils.md5Hex(pararmStr).toUpperCase();
            if (mapSign.equals(sign)) {
                String payInfo = map.get("payUrl");
                Short type = PayConstants.PayReqResultEnum.LINK.getKey();

                if (!PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfo.getPayPlatform())) {
                    type = PayConstants.PayReqResultEnum.URL.getKey();
                }

                PayReqResultVO payReqResult = new PayReqResultVO(payInfo);//
                payReqResult.setType(type);
                payReqResult.setTradeChannel(PayConstants.PayChannelEnum.YY_RECHARGE.getKey());
                payReqResult.setTransCode(map.get("orderNo"));
                payReqResult.setFormLink(payInfo);
                return ResultBO.ok(payReqResult);
            } else {
                return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
            }
        } else {
            if (paymentInfo.isTest()) {// 测试环境，显示第三方返回的信息
                String msg = result.getString("message");
                return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, msg);
            } else {
                return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
            }
        }

    }

    /**
     * 方法说明: 解析并验证异步通知
     *
     * @param map
     * @auth: xiongJinGang
     * @time: 2018年1月3日 上午10:29:30
     * @return: ResultBO<?>
     */
    public static ResultBO<?> payNotify(Map<String, String> map) {
        if (ObjectUtil.isBlank(map)) {
            logger.info("YY支付异步通知结果为空");
            return ResultBO.err();
        }
        PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
        if (map.containsKey("sign")) {
            String mapSign = map.get("sign");
            map.remove("sign");
            String pararmStr = BuildRequestFormUtil.createLinkString(map) + "&key=" + YYPayConfig.YYPAY_SECRET;
            String sign = DigestUtils.md5Hex(pararmStr).toUpperCase();

            if (!mapSign.equals(sign)) {
                logger.info("YY支付异步通知，验证签名不通过");
                return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
            }
            // 状态码1：成功 其它为失败
            // 我们的充值编号都是大写I开头，但快接微信公众号支付只能全部数据，所以在调用接口之前将I全部去掉了，这里需要补上
            String orderNo = map.get("outTradeNo");
            if (!orderNo.startsWith("I")) {
                orderNo = "I" + orderNo;
            }

            payNotifyResult.setOrderCode(orderNo);// 商户唯一订单号
            payNotifyResult.setThirdTradeNo(map.get("orderNo"));
            payNotifyResult.setOrderAmt(Double.valueOf(GetWeChatUtil.changeF2Y(map.get("totalFee"))));// 总金额，整数，单位为分
            payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
            payNotifyResult.setResponse("SUCCESS");// success、error
            if (ObjectUtil.isNotNull(map.get("status")) && "paid".equals(map.get("status"))) {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
            } else {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);
            }
            return ResultBO.ok(payNotifyResult);
        }
        logger.error("YY支付异步通知，无sign参数");
        return ResultBO.err();
    }


    public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
        try {
            String transCode = payQueryParamVO.getTransCode();

            SortedMap<String, String> paramMap = new TreeMap<String, String>();
            paramMap.put("merId", YYPayConfig.YYPAY_UID);// 商户号
            paramMap.put("outTradeNo", transCode);// 订单号
            paramMap.put("orderNo", payQueryParamVO.getTradeNo());
            paramMap.put("payTime", DateUtil.convertDateToStr(payQueryParamVO.getOrderTime(), DateUtil.DATE_FORMAT_NUM));
            paramMap.put("nonceStr", DateUtil.getNow());// ip地址
            String pararmStr = BuildRequestFormUtil.createLinkString(paramMap) + "&key=" + YYPayConfig.YYPAY_SECRET;
            String sign = DigestUtils.md5Hex(pararmStr).toUpperCase();
            paramMap.put("sign", sign);// MD5签名结果
            String result = HttpUtil.doPost(YYPayConfig.YYPAY_QUERY_URL, paramMap);
            if (!ObjectUtil.isBlank(result)) {
                logger.info("YY支付结果返回：" + result);
                Map<String, String> resultMap = JsonUtil.jsonToObject(result, Map.class);
                String code = resultMap.get("status");
                String returnPararmStr = BuildRequestFormUtil.sortMapAndCreateStr(resultMap) + "&key=" + YYPayConfig.YYPAY_SECRET;
                String returnSign = DigestUtils.md5Hex(returnPararmStr).toUpperCase();
                if (!returnSign.equals(resultMap.get("sign"))) {
                    logger.info("验证签名不通过");
                    return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
                }
                PayConstants.PayStatusEnum payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                if ("needpay".equals(code)) {// 等待支付
                    payStatusEnum = PayConstants.PayStatusEnum.WAITTING_PAYMENT;
                } else if ("paid".equals(code)) {// 支付成功
                    payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                } else if ("error_no_pay".equals(code)) {// 支付失败
                    payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_FAILURE;
                }
                PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
                payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("totalFee")));
                payQueryResultVO.setTradeNo(resultMap.get("orderNo"));
                payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
                payQueryResultVO.setOrderCode(resultMap.get("outTradeNo"));
                payQueryResultVO.setTradeStatus(payStatusEnum);
                if (SUCCESS.equals(code)) {
                    return ResultBO.ok(payQueryResultVO);
                } else {
                    return ResultBO.err();
                }
            }
        } catch (Exception e) {
            logger.error("查询YY支付结果异常", e);
        }
        return ResultBO.err();
    }
}
