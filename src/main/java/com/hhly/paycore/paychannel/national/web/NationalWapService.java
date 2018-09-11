package com.hhly.paycore.paychannel.national.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.national.config.NationalConfig;
import com.hhly.paycore.paychannel.national.util.NationalUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 国连支付
 * @author xiongJinGang
 * @date 2018年8月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class NationalWapService extends PayAbstract {
	private static Logger logger = Logger.getLogger(NationalWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("国连wap支付开始，请求参数：" + paymentInfo.toString());
		try {
			// 构建扫码请求参数
			Map<String, String> map = NationalUtil.buildWapMapParam(paymentInfo, PLATFORM_WAP);
			if (ObjectUtil.isBlank(map)) {
				return ResultBO.err(MessageCodeConstants.TRANS_TAKEN_BANK_IS_NULL_FIELD);
			}
			logger.info("国连支付请求参数：" + map.toString());

			String sHtmlText = BuildRequestFormUtil.buildRequest(map, NationalConfig.NATIONAL_URL + "payorders");
			PayReqResultVO payReqResult = new PayReqResultVO(sHtmlText);
			payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.NATIONAL_RECHARGE.getKey());
			return ResultBO.ok(payReqResult);
		} catch (Exception e) {
			logger.error("国连支付请求异常：" + e);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return NationalUtil.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return NationalUtil.payNotify(map);
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
