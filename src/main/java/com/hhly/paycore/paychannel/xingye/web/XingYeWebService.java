package com.hhly.paycore.paychannel.xingye.web;

import java.util.Map;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.xingye.util.XingYeUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 兴业Web支付
 * @author xiongJinGang
 * @date 2017年10月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class XingYeWebService extends PayAbstract {

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<Map<String, String>> resultBO = XingYeUtil.pay(paymentInfo, PLATFORM_WEB);
		if (resultBO.isError()) {
			return resultBO;
		}
		Map<String, String> resultMap = resultBO.getData();
		PayReqResultVO payReqResult = new PayReqResultVO(resultMap.get("code_url"));// 唤起QQ钱包支付url地址
		payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.XINGYE_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return XingYeUtil.orderRefund(refundParam, PayAbstract.PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return XingYeUtil.queryResult(payQueryParamVO, PayAbstract.PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return XingYeUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WEB);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return XingYeUtil.analyPayResult(map);
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
