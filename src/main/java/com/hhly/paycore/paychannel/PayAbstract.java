package com.hhly.paycore.paychannel;

import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

public abstract class PayAbstract implements UnifiedPayService {
	public static final String TRADE_SUCCESS = "0";// 交易成功，很多接口返回0表示交易成功，根据接口不同判断
	public static final String TRADE_SUCCESS_PUFA = "0000";// 浦发银行交易成功
	public static final String PLATFORM_WEB = "WEB";
	public static final String PLATFORM_WAP = "WAP";
	public static final String PLATFORM_APP = "APP";
	public static final String PLATFORM_JSAPI = "JSAPI";
	public static final String REFUND_HEADER = "TK";// 商户退款单号头部

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return null;
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return null;
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		return null;
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		return null;
	}

}
