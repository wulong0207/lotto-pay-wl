package com.hhly.paycore.paychannel.palmpay.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.palmpay.bean.PalmQueryResultVO;
import com.hhly.paycore.paychannel.palmpay.config.PalmPayConfig;
import com.hhly.paycore.paychannel.palmpay.util.PalmUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 掌宜付Web支付
 * 掌宜付的异步通知地址是配置在掌宜付的商户后台，一个应用ID对应一个，充值的统一通知地址为：域名+/lotto/rechargeCenter/palmWap
 * @author xiongJinGang
 * @date 2017年9月15日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PalmPayWebService extends PayAbstract {
	private static Logger logger = Logger.getLogger(PalmPayWebService.class);

	public static final String QN_RECHARGE = "RECHARGE";
	public static final String QN_PAY = "PAY";

	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.info("掌宜付WEB开始，请求参数：" + paymentInfoBO.toString());
		String key = PalmPayConfig.PALM_RECHARGE_KEY;
		String appId = PalmPayConfig.PALM_RECHARGE_APP_ID;
		String qn = QN_RECHARGE;
		if (paymentInfoBO.getTransType().equals(PayConstants.RechargeTypeEnum.PAY.getKey())) {
			key = PalmPayConfig.PALM_PAY_KEY;
			appId = PalmPayConfig.PALM_PAY_APP_ID;
			qn = QN_PAY;
		}
		// 构建支付所需参数
		Map<String, String> params = PalmUtil.buildWapMapParam(paymentInfoBO, appId);
		params.put("qn", qn);

		// 根据字典排序
		String paramStr = BuildRequestFormUtil.createLinkString(params);
		logger.debug("待签名参数【" + paramStr + "】");
		String sign = BuildRequestFormUtil.addSign(paramStr, key, true);
		logger.debug("加密后的sign：" + sign);
		String url = PalmPayConfig.PALM_PAY_URL + "?" + paramStr + "&sign=" + sign;
		PayReqResultVO payReqResult = new PayReqResultVO(url);
		payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
		// 如果是web的QQ钱包支付，需要以这样的方式去获取支付链接
		if (PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			try {
				String payUrl = PalmUtil.sendGet(url);
				// 方式一：获取到支付链接后，跳转到第三方生成二维码链接
				// payReqResult.setFormLink("https://pay.swiftpass.cn/pay/qrcode?uuid=" + URLEncoder.encode(payUrl, "utf-8"));
				// payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
				// 方式二：直接返回二维码链接
				payReqResult.setFormLink(payUrl);
				payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.PALMPAY_RECHARGE.getKey());
			} catch (Exception e) {
				logger.error("获取掌宜付QQ扫码的支付链接异常", e);
			}
		}
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return super.refund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		String queryParam = PalmUtil.queryPayResult(payQueryParamVO);
		try {
			String requestUrl = PalmPayConfig.PALM_QUERY_URL + "?" + queryParam;
			// String result = HttpClient4Utils.httpGet(requestUrl, null, null, 6000);
			String result = HttpUtil.doGetNotUrlEncode(requestUrl, null);
			logger.info("查询掌宜付【" + payQueryParamVO.getTransCode() + "】支付结果返回：" + result);
			if (!ObjectUtil.isBlank(result)) {
				PayQueryResultVO payQueryResultVO = new PayQueryResultVO();

				PalmQueryResultVO palmQueryResultVO = JSON.parseObject(result, PalmQueryResultVO.class);
				PayStatusEnum payStatusEnum = PalmUtil.getPalmPayStatus(palmQueryResultVO.getCode());

				TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
				payQueryResultVO.setTotalAmount(String.valueOf(transRechargeBO.getRechargeAmount()));
				payQueryResultVO.setTradeNo(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
				payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
				payQueryResultVO.setOrderCode(transRechargeBO.getTransRechargeCode());
				payQueryResultVO.setTradeStatus(payStatusEnum);
				return ResultBO.ok(payQueryResultVO);
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
		} catch (Exception e) {
			logger.error("查询掌宜付支付结果异常：", e);
			return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
		}
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return super.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return PalmUtil.payNotify(map);
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
