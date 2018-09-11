package com.hhly.paycore.paychannel.hdapay.web;

import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.hdapay.config.HaoDianAConfig;
import com.hhly.paycore.paychannel.hdapay.util.HaoDianAUtil;
import com.hhly.paycore.sign.SignUtils;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 好店啊WAP支付
 * @author xiongJinGang
 * @date 2017年10月17日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HaoDianAWapService extends PayAbstract {
	private static Logger logger = Logger.getLogger(HaoDianAWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("好店啊WAP支付开始，请求参数：" + paymentInfo.toString());
		try {
			String[] mchInfo = HaoDianAUtil.getMchInfo(paymentInfo.getPayType(), PayAbstract.PLATFORM_WAP);
			Map<String, String> map = HaoDianAUtil.buildWapMapParam(paymentInfo, mchInfo);
			logger.info("好店啊支付请求参数：" + map.toString());
			// 调用接口
			String resultJson = HttpUtil.doPost(HaoDianAConfig.HDA_NEW_PAY_URL + "pay/wap", map);
			logger.info("订单【" + paymentInfo.getNoOrder() + "】好店啊支付请求返回：" + resultJson);
			if (!ObjectUtil.isBlank(resultJson)) {
				JSONObject jsonObject = JSON.parseObject(resultJson);
				if (jsonObject.containsKey("status") && jsonObject.getString("status").equals(HaoDianAUtil.SUCCESS)) {
					if (jsonObject.containsKey("sign")) {
						Map<String, String> resultMap = HaoDianAUtil.buildResultMapParam(jsonObject);
						if (!SignUtils.checkParam(resultMap, mchInfo[2], false)) {
							logger.error("订单【" + paymentInfo.getNoOrder() + "】请求返回签名验证不过");
							return ResultBO.err();
						} else {
							String payUrl = resultMap.get("prepay_url");
							if (paymentInfo.getPayType().equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
								payUrl = resultMap.get("prepay_url");
							}
							PayReqResultVO payReqResult = new PayReqResultVO(payUrl);
							payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
							payReqResult.setTradeChannel(PayConstants.PayChannelEnum.HAODIANA_RECHARGE.getKey());
							return ResultBO.ok(payReqResult);
						}
					}
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				} else {
					String errorMsg = MessageCodeConstants.PAY_FAIL_ERROR_SERVICE;
					if (jsonObject.containsKey("message")) {
						errorMsg = jsonObject.getString("message");
					}
					return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMsg);
				}
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return HaoDianAUtil.orderRefund(refundParam, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		return HaoDianAUtil.queryResult(payQueryParamVO, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return HaoDianAUtil.refundQuery(payQueryParamVO, PayAbstract.PLATFORM_WAP);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return HaoDianAUtil.payNotify(map, PayAbstract.PLATFORM_WAP);
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
