package com.hhly.paycore.paychannel.lianlianpay.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.lianlianpay.enums.LianlianEnum;
import com.hhly.paycore.paychannel.lianlianpay.util.LLPayUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 连连支付WAP请求支付接口
 * @author xiongJinGang
 * @date 2017年9月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class LLPayAppService extends PayAbstract {
	private static Logger logger = Logger.getLogger(LLPayAppService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		TakenPlatformEnum takenPlatformEnum = PayConstants.TakenPlatformEnum.getByKey(paymentInfoBO.getPayPlatform());
		logger.info("连连APP【" + takenPlatformEnum.getValue() + "】支付开始，请求参数：" + paymentInfoBO.toString());
		// 构建支付所需参数
		Map<String, String> params = LLPayUtil.buildAppMapParam(paymentInfoBO);
		String json = JSON.toJSONString(params);
		logger.debug("连连APP支付请求参数：" + json);
		PayReqResultVO payReqResult = new PayReqResultVO(json);
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.LIANLIAN_RECHARGE.getKey());
		payReqResult.setType(PayConstants.PayReqResultEnum.ENCRYPTION.getKey());
		payReqResult.setPayType(PayConstants.AppPayTypeEnum.SDK.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return super.refund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return LLPayUtil.queryPayResult(payQueryParamVO, LianlianEnum.PayTypeEnum.APP);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return super.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return LLPayUtil.payNotify(map, LianlianEnum.PayTypeEnum.APP);
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
