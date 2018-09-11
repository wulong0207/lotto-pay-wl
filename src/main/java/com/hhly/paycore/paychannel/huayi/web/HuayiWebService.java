package com.hhly.paycore.paychannel.huayi.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.hhly.paycore.paychannel.huayi.util.HuayiUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 华移web支付
 * @author xiongJinGang
 * @date 2018年3月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HuayiWebService extends PayAbstract {
	private static Logger logger = Logger.getLogger(HuayiWebService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("华移web支付开始，请求参数：" + paymentInfo.toString());
		try {
			// 构建扫码请求参数
			Map<String, String> map = HuayiUtil.buildWxScanCodeParam(paymentInfo);
			logger.info("华移支付请求参数：" + map.toString());
			String payUrl = HuayiUtil.ALIPAY_SCAN_URL;
			// 如果是微信支付
			if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfo.getPayType())) {
				payUrl = HuayiUtil.WECHART_SCAN_URL;
			}
			String resultJson = HttpUtil.doPost(HuayiConfig.HUAYI_REQUEST_URL + payUrl, map);
			logger.info("华移web支付返回：" + resultJson);
			if (ObjectUtil.isBlank(resultJson)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			return HuayiUtil.analyResultJson(resultJson, paymentInfo);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return HuayiUtil.orderRefund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return HuayiUtil.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// return HaoDianAUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WEB);
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return HuayiUtil.payNotify(map);
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
