package com.hhly.paycore.paychannel.sandpay.web;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.sandpay.config.SandPayConfig;
import com.hhly.paycore.paychannel.sandpay.util.SandPayUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 六度支付
 * @author xiongJinGang
 * @date 2018年6月26日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SandPayAppService extends PayAbstract {
	private static Logger logger = Logger.getLogger(SandPayAppService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("六度app支付开始，请求参数：" + paymentInfo.toString());
		try {
			// 如果是QQ钱包支付
			if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey())) {
				// 构建QQ扫码请求参数
				Map<String, String> map = SandPayUtil.buildQQParam(paymentInfo);
				logger.info("六度QQ支付请求参数：" + map);
				String result = HttpUtil.doPost(SandPayConfig.SAND_PAY_QQ_PAY_URL, map);
				// {"order_sn":"I18070516220712300001","fee":"1","code_url":"https:\/\/qpay.qq.com\/qr\/5d1819bf","status":"success","signData":"0d8e3b178f47b8c2d7a103b29d751561"}
				logger.info("六度QQ支付返回结果：" + map);
				if (StringUtils.isNotBlank(result)) {
					JSONObject jsonObject = JSON.parseObject(result);
					// 存在状态码和返回地址，并且状态码为success
					if (jsonObject.containsKey("status") && jsonObject.getString("status").equals("success") && jsonObject.containsKey("code_url")) {
						PayReqResultVO payReqResult = new PayReqResultVO(jsonObject.getString("code_url"));
						payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
						payReqResult.setTradeChannel(PayConstants.PayChannelEnum.SANDPAY_RECHARGE.getKey());
						return ResultBO.ok(payReqResult);
					}
				}
			} else {
				// 构建扫码请求参数
				String requestParam = SandPayUtil.buildWapMapParam(paymentInfo);
				logger.info("六度支付请求参数：" + requestParam);

				PayReqResultVO payReqResult = new PayReqResultVO(SandPayConfig.SAND_PAY_PAYMENT_URL + "fastPay.php?" + requestParam);
				payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.SANDPAY_RECHARGE.getKey());
				return ResultBO.ok(payReqResult);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return null;
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return SandPayUtil.queryResult(payQueryParamVO);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return null;
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return SandPayUtil.payNotify(map);
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
