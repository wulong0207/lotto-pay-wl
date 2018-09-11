package com.hhly.paycore.paychannel.weifutong.zxsz1.web;

import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.weifutong.zxsz1.util.WFTZhongxin1Util;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc  威富通Wap支付
 * @author xiongJinGang
 * @date 2017年11月23日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class WeifutongAppService extends PayAbstract {

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		ResultBO<Map<String, String>> resultBO = WFTZhongxin1Util.pay(paymentInfo, PLATFORM_APP);
		if (resultBO.isError()) {
			return resultBO;
		}
		Map<String, String> resultMap = resultBO.getData();
		PayReqResultVO payReqResult = new PayReqResultVO(JSON.toJSONString(resultMap));// 唤起QQ钱包支付url地址
		payReqResult.setType(PayConstants.PayReqResultEnum.ENCRYPTION.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.WEIFUTONGZX1_RECHARGE.getKey());
		payReqResult.setPayType(PayConstants.AppPayTypeEnum.SDK.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return WFTZhongxin1Util.orderRefund(refundParam, PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return WFTZhongxin1Util.queryResult(payQueryParamVO, PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return WFTZhongxin1Util.refundQuery(payQueryParamVO, PLATFORM_WAP);
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
