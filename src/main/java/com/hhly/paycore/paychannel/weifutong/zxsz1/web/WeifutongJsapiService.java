package com.hhly.paycore.paychannel.weifutong.zxsz1.web;

import java.util.Map;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.weifutong.zxsz1.util.WFTZhongxin1Util;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 威富通公众号支付
 * @author xiongJinGang
 * @date 2018年1月11日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class WeifutongJsapiService extends PayAbstract {

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<Map<String, String>> resultBO = WFTZhongxin1Util.pay(paymentInfo, PLATFORM_JSAPI);
		if (resultBO.isError()) {
			return resultBO;
		}
		Map<String, String> resultMap = resultBO.getData();
		PayReqResultVO payReqResult = new PayReqResultVO(resultMap.get("pay_info"));// 唤起QQ钱包支付url地址
		payReqResult.setType(PayConstants.PayReqResultEnum.ENCRYPTION.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.WEIFUTONGZX1_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return WFTZhongxin1Util.orderRefund(refundParam, PLATFORM_JSAPI);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return WFTZhongxin1Util.queryResult(payQueryParamVO, PLATFORM_JSAPI);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return WFTZhongxin1Util.refundQuery(payQueryParamVO, PLATFORM_JSAPI);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return WFTZhongxin1Util.analyPayResult(map);
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
