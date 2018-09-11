package com.hhly.paycore.paychannel.transfer.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 转账汇款
 * @author xiongJinGang
 * @date 2018年8月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class TransferWapService extends PayAbstract {
	private static Logger logger = Logger.getLogger(TransferWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("转账汇款开始，请求参数：" + paymentInfo.toString());
		PayReqResultVO payReqResult = new PayReqResultVO();
		payReqResult.setType(PayConstants.PayReqResultEnum.FRONT.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.TRANSFER_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return null;
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
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
