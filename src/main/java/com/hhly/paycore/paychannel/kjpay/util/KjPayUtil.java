package com.hhly.paycore.paychannel.kjpay.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.hhly.paycore.paychannel.kjpay.config.KjConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.sign.Base64;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.*;
import com.hhly.utils.BuildRequestFormUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author lgs on
 * @version 1.0
 * @desc 快接支付工具类
 * @date 2018/4/18.
 * @company 益彩网络科技有限公司
 */
public class KjPayUtil {
    private static Logger logger = Logger.getLogger(KjPayUtil.class);
    /**
     * 微信支付
     **/
    public static String WECHAR = "/wechar";
    /**
     * 支付宝支付
     **/
    public static String ALIPAY = "/alipay";
    /**
     * 支付宝线上线下扫码
     **/
    public static String DIRECT_CODE = "/direct_code";

    /**
     * 线上线下扫码
     **/
    public static String SCAN_URL = "/scan_pay";

    /**
     * H5支付
     **/
    public static String WAP_URL = "/wap_pay";
    /**
     * APP支付
     **/
    public static String APP_URL = "/app_pay";
    /**
     * 公众号支付
     **/
    public static String PUBLIC_URL = "/public_pay";
    /**
     * 查询订单
     **/
    public static String QUERY_PAY_URL = "/query_pay";
    /**
     * 退款
     **/
    public static String TRADE_REFUND = "/trade_refund";

    public static final String SUCCESS = "1";// 成功标志

    public static final String KJPAY_JSAPI_CODE = "KJPAY_JSAPI";// 快接微信公众号支付


    /**
     * 构建请求参数
     *
     * @param paymentInfo
     * @return
     */
    public static Map<String, String> buildWxScanCodeParam(PaymentInfoBO paymentInfo) {
        Map<String, String> result = new TreeMap<>();
        result.put("merchant_no", KjConfig.KJ_PAY_PARTNER_CODE);
        result.put("merchant_order_no", paymentInfo.getNoOrder());
        result.put("start_time", DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
        result.put("trade_amount", paymentInfo.getMoneyOrder());
        result.put("notify_url", paymentInfo.getNotifyUrl());
        result.put("goods_name", paymentInfo.getNameGoods());
        result.put("goods_desc", paymentInfo.getInfoOrder());
        result.put("sign_type", "1");

        //wap 或者公众号支付 支付就增加同步回调地址
        if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.JSAPI.getKey())) {
            result.put("return_url", paymentInfo.getUrlReturn());// 订单支付同步回调地址
//            result.put("pay_mode","2"); //1-同步不跳转 2-同步跳转

        }

        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey()) || paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey()))) {
            result.put("user_ip", paymentInfo.getUserreqIp());// ip地址
        }


//        // 支付方式为微信支付并且为公众号支付
//        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey()) && paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.JSAPI.getKey())) {
////            result.put("openID", paymentInfo.getOpenId());// 微信openID
//            result.put("return_url", paymentInfo.getUrlReturn());// 订单支付同步回调地址
////            result.put("orderNo", paymentInfo.getNoOrder().substring(1, paymentInfo.getNoOrder().length()));
//        }
        // 支付方式为微信支付并且为WAP支付
        if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
//            result.put("openID", paymentInfo.getOpenId());// 微信openID
//            result.put("return_url", paymentInfo.getUrlReturn());// 订单支付同步回调地址

            result.put("pay_mode", "2"); //1-同步不跳转 2-同步跳转
            result.put("return_url", paymentInfo.getUrlReturn()); //1-同步不跳转 2-同步跳转
            //快接支付微信需要传递支付场景
            Map<String, String> map = new HashMap<>();
            if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.ANDROID.getKey())) {
                map.put("type", "Android");
                map.put("app_name", "2N彩票");
                map.put("package_name", "com.hhly.welfarelottery");
            } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey())) {
                map.put("type", "IOS");
                map.put("app_name", "2N彩票");
                map.put("bundle_id", "cp.yc.com");
            } else if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey())) {
                map.put("type", "Wap");
                map.put("wap_name", "2N彩票");
                map.put("wap_url", "http://cp.2ncai.com/");
            }
            result.put("pay_sence", JSON.toJSONString(map));
//            result.put("orderNo", paymentInfo.getNoOrder().substring(1, paymentInfo.getNoOrder().length()));
        }

        String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(result, true);
        result.put("sign", DigestUtils.md5Hex(needMd5Str + "&key=" + KjConfig.KJ_PAY_KEY));
        return result;
    }


    /**
     * 方法说明: 解析支付请求返回
     *
     * @param json
     * @param paymentInfo
     * @time: 2018年3月27日 上午11:55:41
     * @return: ResultBO<?>
     */
    public static ResultBO<?> analyResultJson(String json, PaymentInfoBO paymentInfo) throws IOException {
        JSONObject object = JSON.parseObject(json);
        if (object.getString("status").equals(SUCCESS)) {
            JSONObject result = object.getJSONObject("data");
            String payInfo = result.getString("pay_url");
            Short type = PayConstants.PayReqResultEnum.LINK.getKey();
            if (PayConstants.TakenPlatformEnum.JSAPI.getKey().equals(paymentInfo.getPayPlatform())) {
                type = PayConstants.PayReqResultEnum.ENCRYPTION.getKey();
                payInfo = result.getString("pay_url");// 唤起原生公众号参数
                // payInfo = result.getString("payInfo");// 使用平台起调微信公众号支付，支付成功或者失败都会回调同步地址returnUrl。需要使用时必须向平台申请配置支付授权目录为:https://pay.cnmobi.cn/pay/wxgzh/
            }
            if (!PayConstants.TakenPlatformEnum.WEB.getKey().equals(paymentInfo.getPayPlatform())) {
                type = PayConstants.PayReqResultEnum.URL.getKey();
            }

            PayReqResultVO payReqResult = new PayReqResultVO(payInfo);//
            payReqResult.setType(type);
            payReqResult.setTradeChannel(PayConstants.PayChannelEnum.KJ_RECHARGE.getKey());
            payReqResult.setTransCode(result.getString("trade_no"));
            if (type.shortValue() == PayConstants.PayReqResultEnum.LINK.getKey().shortValue() && StringUtil.isBlank(payInfo)) {
                BASE64Decoder decoder = new BASE64Decoder();
                //解码前需要 去掉data:image/png;base64,字符串 截取前面22位
                String image = result.getString("image").substring(22);
                byte[] img = decoder.decodeBuffer(image);
                payReqResult.setQrStream(img);
            }
            return ResultBO.ok(payReqResult);
        } else {
            if (paymentInfo.isTest()) {// 测试环境，显示第三方返回的信息
                String msg = object.getString("info");
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
            logger.info("快接支付异步通知结果为空");
            return ResultBO.err();
        }
        PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
        if (map.containsKey("sign")) {
            String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(map, true);

            String md5Sign = DigestUtils.md5Hex(needMd5Str + "&key=" + KjConfig.KJ_PAY_KEY);
            if (!map.get("sign").equals(md5Sign)) {
                logger.info("快接支付异步通知，验证签名不通过");
                return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
            }
            // 状态码1：成功 其它为失败
            // 我们的充值编号都是大写I开头，但快接微信公众号支付只能全部数据，所以在调用接口之前将I全部去掉了，这里需要补上
            String orderNo = map.get("merchant_order_no");
            if (!orderNo.startsWith("I")) {
                orderNo = "I" + orderNo;
            }

            payNotifyResult.setOrderCode(orderNo);// 商户唯一订单号
            payNotifyResult.setThirdTradeNo(map.get("trade_no"));// 平台订单号
            payNotifyResult.setOrderAmt(Double.valueOf(map.get("amount")));// 总金额，整数，单位为分
            payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
            payNotifyResult.setResponse("success");// success、error
            if (StringUtils.isNotBlank(map.get("status")) && "Success".equals(map.get("status"))) {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
            } else {
                payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);
            }
            return ResultBO.ok(payNotifyResult);
        }
        logger.error("快接支付异步通知，无sign参数");
        return ResultBO.err();
    }

    /**
     * 方法说明: 订单退款
     *
     * @param refundParam
     * @auth: xiongJinGang
     * @time: 2018年3月27日 下午4:35:12
     * @return: ResultBO<?>
     */
    public static ResultBO<?> orderRefund(RefundParamVO refundParam) {
        // 检查是否为公众号支付
        boolean isJsapiPay = checkPayType(refundParam.getTransRechargeBO());
        String transCode = refundParam.getTransCode();
        if (isJsapiPay) {
            transCode = transCode.substring(1, transCode.length());
        }
        // 封装请求参数
        Map<String, String> paramMap = new HashMap<String, String>();
        paramMap.put("trade_no", transCode);// 商户订单号
        paramMap.put("merchant_no", KjConfig.KJ_PAY_PARTNER_CODE);// 商户号
        paramMap.put("sign_type", "1");
        paramMap.put("refundReson", refundParam.getRefundReason());// 退款原因
//        paramMap.put("timestamp", String.valueOf(DateUtil.getNowTimeStamp()));// 请求时间戳，毫秒数

        String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(paramMap, true);
        paramMap.put("sign", DigestUtils.md5Hex(needMd5Str + "&key=" + KjConfig.KJ_PAY_KEY));// MD5签名结果

        try {
            logger.info("退款请求参数：" + paramMap.toString());

            String payUrl = KjConfig.KJ_PAY_URL;
            // 如果是微信支付
            if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(refundParam.getRechargeChannel())) {
                payUrl = payUrl + KjPayUtil.WECHAR;
            } else if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(refundParam.getRechargeChannel())) { //支付宝支付
                payUrl = payUrl + KjPayUtil.ALIPAY;
            }

            String result = HttpUtil.doPost(payUrl + TRADE_REFUND, paramMap);
            if (!ObjectUtil.isBlank(result)) {
                logger.info("退款返回结果：" + result);
                JSONObject jsonObject = JSONObject.parseObject(result);
                String code = jsonObject.getString("status");
                RefundResultVO refundResultVO = new RefundResultVO();
                if (SUCCESS.equals(code)) {
                    refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
                    refundResultVO.setResultMsg(jsonObject.getString("info"));
                    refundResultVO.setOrderCode(refundParam.getTransCode());// 商户订单号
                    refundResultVO.setRefundAmount(String.valueOf(refundParam.getOrderAmount()));// 退款总金额
                    return ResultBO.ok(refundResultVO);
                } else {
                    return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, jsonObject.getString("msg"));
                }
            }
        } catch (Exception e) {
            logger.error("快接退款请求异常", e);
        }
        return ResultBO.err();
    }

    public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
        try {
            // 检查是否为公众号支付
//            boolean isJsapiPay = checkPayType(payQueryParamVO.getTransRechargeBO());
            String tradeNo = payQueryParamVO.getTradeNo();
//            if (isJsapiPay) {
//                transCode = transCode.substring(1, transCode.length());
//            }
            // 封装请求参数
            SortedMap<String, String> paramMap = new TreeMap<String, String>();
            paramMap.put("merchant_no", KjConfig.KJ_PAY_PARTNER_CODE);// 商户号
            paramMap.put("trade_no", tradeNo);// 订单号
            paramMap.put("sign_type", "1");// 签名类型(1md5)

            String needMd5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(paramMap, true);
            paramMap.put("sign", DigestUtils.md5Hex(needMd5Str + "&key=" + KjConfig.KJ_PAY_KEY));// MD5签名结果

            String payUrl = KjConfig.KJ_PAY_URL;
            if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(payQueryParamVO.getRechargeChannel())) {
                payUrl = payUrl + KjPayUtil.WECHAR;
            } else if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(payQueryParamVO.getRechargeChannel())) { //支付宝支付
                payUrl = payUrl + KjPayUtil.ALIPAY;
            }
            String result = HttpUtil.doPost(payUrl + QUERY_PAY_URL, paramMap);
            if (!ObjectUtil.isBlank(result)) {
                logger.info("查询快接支付结果返回：" + result);
                JSONObject jsonObject = JSONObject.parseObject(result);
                String code = jsonObject.getString("status");
                if (SUCCESS.equals(code)) {
                    JSONObject detail = jsonObject.getJSONObject("data");
                    Map<String, String> resultMap = new HashMap<String, String>();

                    detail.entrySet().forEach(k -> {
                        resultMap.put(k.getKey(), (String) k.getValue());
                    });

                    String orderStatus = detail.getString("status");
                    String md5Str = BuildRequestFormUtil.sortMapToStrConnSymbol(resultMap, true);
                    String countSign = DigestUtils.md5Hex(md5Str + "&key=" + KjConfig.KJ_PAY_KEY);// MD5签名结果
                    String returnSign = detail.getString("sign");
                    if (!countSign.equals(returnSign)) {
                        logger.info("验证签名不通过");
                        return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
                    }
                    PayConstants.PayStatusEnum payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                    if ("Wait".equals(orderStatus)) {// 等待支付
                        payStatusEnum = PayConstants.PayStatusEnum.WAITTING_PAYMENT;
                    } else if ("Success".equals(orderStatus)) {// 支付成功
                        payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
                    } else if ("Fail".equals(orderStatus)) {// 支付失败
                        payStatusEnum = PayConstants.PayStatusEnum.PAYMENT_FAILURE;
                    } else if ("Init".equals(orderStatus)) {// 订单已撤销
                        payStatusEnum = PayConstants.PayStatusEnum.OVERDUE_PAYMENT;
                    } else if ("RefundSuccess".equals(orderStatus)) {// 订单已退款
                        payStatusEnum = PayConstants.PayStatusEnum.REFUND;
                    }
                    PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
                    payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("amount")));
                    payQueryResultVO.setTradeNo(resultMap.get("trade_no"));
                    payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
                    payQueryResultVO.setOrderCode(payQueryParamVO.getTransCode());
                    payQueryResultVO.setTradeStatus(payStatusEnum);
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

    /**
     * 方法说明: 检验支付方式是否为公众号支付
     *
     * @param transRechargeBO
     * @auth: xiongJinGang
     * @time: 2018年3月28日 下午12:09:57
     * @return: void
     */
    private static boolean checkPayType(TransRechargeBO transRechargeBO) {
        boolean flag = Boolean.FALSE;
        // 如果是快接公众号支付，要将交易编号前的I去掉，他们只允许纯数字的交易编号
        if (!ObjectUtil.isBlank(transRechargeBO.getChannelCode()) && transRechargeBO.getChannelCode().equals(KJPAY_JSAPI_CODE)) {
            flag = Boolean.TRUE;
        }
        return flag;
    }

}
