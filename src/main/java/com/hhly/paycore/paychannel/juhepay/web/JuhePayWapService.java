package com.hhly.paycore.paychannel.juhepay.web;

import java.util.Map;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.juhepay.util.JuhePayUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.JuhePayResponseVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 聚合Wap支付
 * @author xiongJinGang
 * @date 2017年11月24日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class JuhePayWapService extends PayAbstract {

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<?> resultBO = JuhePayUtil.pay(paymentInfo, PLATFORM_WAP);
		if (resultBO.isError()) {
			return resultBO;
		}
		JuhePayResponseVO juhePayResponseVO = (JuhePayResponseVO) resultBO.getData();

		PayReqResultVO payReqResultVO = new PayReqResultVO();
		payReqResultVO.setType(PayConstants.PayReqResultEnum.URL.getKey());
		payReqResultVO.setFormLink(juhePayResponseVO.getPayUrl());
		payReqResultVO.setTradeChannel(PayConstants.PayChannelEnum.JUHEPAY_RECHARGE.getKey());
		return ResultBO.ok(payReqResultVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return JuhePayUtil.payNotify(map, PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return JuhePayUtil.payQuery(payQueryParamVO, PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		// TODO Auto-generated method stub
		return super.refund(refundParam);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// TODO Auto-generated method stub
		return super.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		// TODO Auto-generated method stub
		return super.payReturn(map);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		// TODO Auto-generated method stub
		return super.queryBill(map);
	}

}
