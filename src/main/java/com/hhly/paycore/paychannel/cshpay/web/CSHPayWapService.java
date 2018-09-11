package com.hhly.paycore.paychannel.cshpay.web;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.cshpay.config.CSHConfig;

import com.hhly.paycore.paychannel.cshpay.util.CSHPayUtil;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import org.apache.log4j.Logger;

import java.net.URLEncoder;
import java.util.Map;

/**
 * @author lgs on
 * @version 1.0
 * @desc 爱加密wap支付
 * @date 2018/7/3.
 * @company 益彩网络科技有限公司
 */
public class CSHPayWapService extends PayAbstract {

    private static Logger logger = Logger.getLogger(CSHPayWapService.class);


    public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
        logger.info("爱加密wap支付开始，请求参数：" + paymentInfo.toString());
        try {
            // 构建扫码请求参数
            String param = CSHPayUtil.buildWxScanCodeParam(paymentInfo);

            String payUrl = null;

            if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
                payUrl = CSHConfig.CSH_WX_HEAD_URL + CSHConfig.CSH_WX_PAY_URL + URLEncoder.encode(param);
            } else if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
                payUrl = CSHConfig.CSH_ALIPAY_HEAD_URL + CSHConfig.CSH_ALIPAY_PAY_URL + URLEncoder.encode(param);
            }
            logger.info("爱加密wap支付生成url：" + payUrl);
            PayReqResultVO payReqResult = new PayReqResultVO(payUrl);//
            payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
            payReqResult.setTradeChannel(PayConstants.PayChannelEnum.CSHPAY_RECHARGE.getKey());
            if (paymentInfo.getPayPlatform().equals(PayConstants.TakenPlatformEnum.IOS.getKey())) {
                payReqResult.setIsSafari("1");//如果为IOS手机则打开浏览器
            }

            return ResultBO.ok(payReqResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultBO.ok(null);
    }


    @Override
    public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
        return CSHPayUtil.queryResult(payQueryParamVO);
    }


    @Override
    public ResultBO<?> payNotify(Map<String, String> map) {
        try {
            return CSHPayUtil.payNotify(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultBO.err();
    }


}
