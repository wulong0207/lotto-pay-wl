package com.hhly.paycore.paychannel.hongyuepay.web;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.hongyuepay.util.HongYueUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

import java.util.Map;

/**
 * @desc 鸿粤Web支付
 * @author xiongJinGang
 * @date 2017年11月23日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HongYueWebService extends PayAbstract {

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<Map<String, String>> resultBO = HongYueUtil.pay(paymentInfo, PLATFORM_WEB);
		if (resultBO.isError()) {
			return resultBO;
		}
		Map<String, String> resultMap = resultBO.getData();
		PayReqResultVO payReqResult = new PayReqResultVO(resultMap.get("code_url"));
		payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.HONGYUE_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return HongYueUtil.orderRefund(refundParam, PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return HongYueUtil.queryResult(payQueryParamVO, PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return HongYueUtil.refundQuery(payQueryParamVO, PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return HongYueUtil.analyPayResult(map);
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
