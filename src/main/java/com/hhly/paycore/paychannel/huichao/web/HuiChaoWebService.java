package com.hhly.paycore.paychannel.huichao.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.huichao.config.HuiChaoConfig;
import com.hhly.paycore.paychannel.huichao.util.HuiChaoUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 汇潮支付
 * @author xiongJinGang
 * @date 2018年1月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HuiChaoWebService extends PayAbstract {
	private static Logger logger = Logger.getLogger(HuiChaoWebService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("汇潮web支付开始，请求参数：" + paymentInfo.toString());
		try {
			// 构建扫码请求参数
			Map<String, String> map = HuiChaoUtil.buildWebMapParam(paymentInfo);
			logger.info("汇潮支付请求参数：" + map.toString());

			String formData = BuildRequestFormUtil.buildRequest(map, HuiChaoConfig.HUICHAO_PAY_URL);
			logger.info("汇潮web支付请求表单：" + formData);
			PayReqResultVO payReqResult = new PayReqResultVO(formData);
			payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.HUICHAO_RECHARGE.getKey());
			return ResultBO.ok(payReqResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		// return HaoDianAUtil.orderRefund(refundParam, PayAbstract.PLATFORM_WEB);
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return HuiChaoUtil.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		// return HaoDianAUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WEB);
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return HuiChaoUtil.payNotify(map);
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
