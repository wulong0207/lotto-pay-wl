package com.hhly.paycore.paychannel.lianlianpay.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.alipay.util.AlipayCore;
import com.hhly.paycore.paychannel.lianlianpay.config.LianPayConfig;
import com.hhly.paycore.paychannel.lianlianpay.enums.LianlianEnum;
import com.hhly.paycore.paychannel.lianlianpay.util.LLPayUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 连连支付 WEB请求支付接口（快捷）
 * @author xiongJinGang
 * @date 2017年9月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class LLPayFastService extends PayAbstract {
	private static Logger logger = Logger.getLogger(LLPayFastService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		Map<String, String> params = LLPayUtil.buildFastMapParam(paymentInfoBO);

		// 根据字典排序
		String paramStr = AlipayCore.createLinkString(params);
		logger.debug("待签名参数【" + paramStr + "】");
		String sign = LLPayUtil.addSign(paramStr, LianPayConfig.FAST_PRI_KEY, LianPayConfig.FAST_MD5_KEY, LianPayConfig.FAST_SIGN_TYPE);
		logger.debug("加密后的sign：" + sign);
		params.put("sign", sign);
		String sHtmlText = LLPayUtil.buildWebRequest(LianPayConfig.PAY_URL, params, "post", "utf-8");
		PayReqResultVO payReqResult = new PayReqResultVO(sHtmlText);
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.LIANLIAN_RECHARGE.getKey());
		payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return super.refund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return LLPayUtil.queryPayResult(payQueryParamVO, LianlianEnum.PayTypeEnum.FAST);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return super.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return LLPayUtil.payNotify(map, LianlianEnum.PayTypeEnum.FAST);
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
