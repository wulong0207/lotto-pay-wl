package com.hhly.paycore.paychannel.yypay.web;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.cshpay.config.CSHConfig;
import com.hhly.paycore.paychannel.cshpay.util.CSHPayUtil;
import com.hhly.paycore.paychannel.yypay.utils.YYUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import org.apache.log4j.Logger;

import java.net.URLEncoder;
import java.util.Map;

/**
 * @author lgs on
 * @version 1.0
 * @desc YYPay支付服务
 * @date 2018/8/7.
 * @company 益彩网络科技有限公司
 */
public class YYPayService extends PayAbstract {

    private static final Logger logger = Logger.getLogger(YYPayService.class);

    @Override
    public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
        logger.info("YY支付请求参数::::::::" + paymentInfo.toString());
        try {
            JSONObject result = YYUtil.buildRequest(paymentInfo);
            return YYUtil.analyResultJson(result, paymentInfo);
        } catch (Exception e) {
            logger.info("YY支付失败::::::::" + e.getMessage());
            e.printStackTrace();
        }
        return ResultBO.ok(null);
    }

    @Override
    public ResultBO<?> refund(RefundParamVO refundParam) {

        return null;
    }

    @Override
    public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
        return null;
    }

    @Override
    public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {

        return null;
    }

    @Override
    public ResultBO<?> payNotify(Map<String, String> map) {
        return YYUtil.payNotify(map);
    }


}
