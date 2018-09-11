package com.hhly.paycore.paychannel.yeepay2.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.yeepay2.utils.Yeepay2Util;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReturnResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

public class Yeepay2AppService extends PayAbstract {
	static Logger logger = LoggerFactory.getLogger(Yeepay2AppService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.debug("新易宝APP支付开始");
		return Yeepay2Util.callYeepay(paymentInfoBO);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return Yeepay2Util.orderRefund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return Yeepay2Util.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return Yeepay2Util.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return Yeepay2Util.analyPayResult(map);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		logger.debug("易宝同步通知");
		PayReturnResultVO payReturnResultVO = new PayReturnResultVO();
		return ResultBO.ok(payReturnResultVO);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		// return Yeepay2Util.refundQuery(payQueryParamVO);
		return ResultBO.ok();
	}
}
