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
 * @desc 兴业Wap支付
 * @author xiongJinGang
 * @date 2017年9月26日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class XingYeWapService extends PayAbstract {

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<Map<String, String>> resultBO = XingYeUtil.pay(paymentInfo, PLATFORM_WAP);
		if (resultBO.isError()) {
			return resultBO;
		}
		Map<String, String> resultMap = resultBO.getData();
		String pay_url = resultMap.get("pay_url");// 直接使用此链接请求支付宝支付,QQ钱包的没有这个链接
		if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey()) || paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
			pay_url = resultMap.get("pay_info");// 唤起QQ钱包支付url地址，如果是支付宝的，这个字段是【JSON字符串，自行唤起支付宝钱包支付】
		}
		PayReqResultVO payReqResult = new PayReqResultVO(pay_url);// 唤起QQ钱包支付url地址
		payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.XINGYE_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return XingYeUtil.orderRefund(refundParam, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return XingYeUtil.queryResult(payQueryParamVO, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return XingYeUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WAP);
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
