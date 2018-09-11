package com.hhly.paycore.paychannel.kjpay.web;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.hongyuepay.util.HongYueUtil;
import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.hhly.paycore.paychannel.huayi.util.HuayiUtil;
import com.hhly.paycore.paychannel.kjpay.config.KjConfig;
import com.hhly.paycore.paychannel.kjpay.util.KjPayUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * @author lgs on
 * @version 1.0
 * @desc 快接Web扫码支付
 * @date 2018/4/18.
 * @company 益彩网络科技有限公司
 */
public class KjPayWebService extends PayAbstract {

    private static Logger logger = Logger.getLogger(KjPayWebService.class);


    public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
        logger.info("快接web支付开始，请求参数：" + paymentInfo.toString());
        try {
            // 构建扫码请求参数
            Map<String, String> map = KjPayUtil.buildWxScanCodeParam(paymentInfo);
            logger.info("快接支付请求参数：" + map.toString());
            String payUrl = KjConfig.KJ_PAY_URL;
            // 如果是微信支付
            if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
                payUrl = payUrl + KjPayUtil.WECHAR + KjPayUtil.SCAN_URL;
            } else if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfo.getPayType())) { //支付宝支付
                payUrl = payUrl + KjPayUtil.ALIPAY + KjPayUtil.DIRECT_CODE;
            }
            String resultJson = HttpUtil.doPost(payUrl, map);
            logger.info("快接web支付返回：" + resultJson);
            if (ObjectUtil.isBlank(resultJson)) {
                return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
            }
            return KjPayUtil.analyResultJson(resultJson, paymentInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ResultBO.ok(null);
    }

    @Override
    public ResultBO<?> refund(RefundParamVO refundParam) {
        return KjPayUtil.orderRefund(refundParam);
    }

    @Override
    public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
        return KjPayUtil.queryResult(payQueryParamVO);
    }

    @Override
    public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
        // return HaoDianAUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WEB);
        return null;
    }

    @Override
    public ResultBO<?> payNotify(Map<String, String> map) {
        return KjPayUtil.payNotify(map);
    }

    @Override
    public ResultBO<?> payReturn(Map<String, String> map) {
        return super.payReturn(map);
    }

    @Override
    public ResultBO<?> queryBill(Map<String, String> map) {
        return null;
    }


}
