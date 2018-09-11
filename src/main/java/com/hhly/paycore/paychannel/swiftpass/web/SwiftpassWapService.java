package com.hhly.paycore.paychannel.swiftpass.web;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.palmpay.util.PalmUtil;
import com.hhly.paycore.paychannel.swiftpass.config.SwiftpassPayConfig;
import com.hhly.paycore.paychannel.swiftpass.util.SwiftPassUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.sign.MD5;
import com.hhly.paycore.sign.SignUtils;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 威富通Wap支付
 * @author xiongJinGang
 * @date 2017年9月26日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SwiftpassWapService extends PayAbstract {
	private static Logger logger = Logger.getLogger(SwiftpassWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.info("一比分威富通WAP开始，请求参数：" + paymentInfoBO.toString());
		// 构建支付所需参数
		Map<String, String> params = SwiftPassUtil.buildWapMapParam(paymentInfoBO);
		String payResult = null;
		try {
			payResult = HttpUtil.doPost(SwiftpassPayConfig.SWIFTPASS_PAY_URL, params);
			logger.info("一比分威富通支付请求返回：" + payResult);
			// {"callback_order":"I17101215292608600008","code":0,"info":"http://qq.ludstudio.com/api/jumptoweixin?wid=157\u0026qid=59df19dd3fe72c8524f7fcdf","message":""}
			if (ObjectUtil.isBlank(payResult)) {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
			JSONObject jsonObject = JSON.parseObject(payResult);
			if (jsonObject.containsKey("info")) {
				String payUrl = jsonObject.getString("info");
				PayReqResultVO payReqResult = new PayReqResultVO(payUrl);
				payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.SWIFTPASS_RECHARGE.getKey());
				return ResultBO.ok(payReqResult);
			}
		} catch (Exception e) {
			logger.error("订单【" + paymentInfoBO.getNoOrder() + "】请求支付异常", e);
			return ResultBO.err(MessageCodeConstants.PAY_FAIL_ERROR_SERVICE);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		return super.refund(refundParam);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		// 封装请求参数
		SortedMap<String, String> paramMap = new TreeMap<String, String>();
		paramMap.put("service", "unified.trade.query");
		paramMap.put("version", "2.0");
		paramMap.put("charset", "UTF-8");
		paramMap.put("sign_type", "MD5");
		paramMap.put("mch_id", SwiftpassPayConfig.SWIFTPASS_QUERY_PARTNER_CODE);
		paramMap.put("out_trade_no", payQueryParamVO.getTransCode());
		paramMap.put("transaction_id", payQueryParamVO.getTradeNo());// 平台交易号, out_trade_no和transaction_id至少一个必填，同时存在时transaction_id优先。
		paramMap.put("nonce_str", GetWeChatUtil.getNonceStr());

		Map<String, String> params = SignUtils.paraFilter(paramMap);
		StringBuilder buf = new StringBuilder((params.size() + 1) * 10);
		SignUtils.buildPayParams(buf, params, false);
		String preStr = buf.toString();
		String sign = MD5.sign(preStr, "&key=" + SwiftpassPayConfig.SWIFTPASS_QUERY_KEY, "utf-8");
		paramMap.put("sign", sign);
		Map<String, String> resultMap = HttpUtil.doPostXml(paramMap, SwiftpassPayConfig.SWIFTPASS_PAY_URL);
		if (!ObjectUtil.isBlank(resultMap)) {
			logger.info("请求结果：" + resultMap.toString());
			if (resultMap.containsKey("sign")) {
				if (!SignUtils.checkParam(resultMap, SwiftpassPayConfig.SWIFTPASS_QUERY_KEY)) {
					logger.info("验证签名不通过");
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}
				// 以下字段在 status 和 result_code 都为 0的时候有返回
				if (resultMap.get("status").equals("0") && resultMap.get("result_code").equals("0")) {
					// SUCCESS—支付成功 REFUND—转入退款 NOTPAY—未支付 CLOSED—已关闭 REVERSE—已冲正 REVOK—已撤销
					String tradeState = resultMap.get("trade_state");
					PayStatusEnum payStatusEnum = PalmUtil.getPalmPayStatus(tradeState);
					if (tradeState.equals("SUCCESS")) {
						PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
						payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("total_fee")));
						payQueryResultVO.setTradeNo(resultMap.get("out_transaction_id"));
						payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
						payQueryResultVO.setOrderCode(resultMap.get("out_trade_no"));
						payQueryResultVO.setTradeStatus(payStatusEnum);
						return ResultBO.ok(payQueryResultVO);
					} else {
						return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
					}
				}
			}
		}
		return ResultBO.err();
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		return super.refundQuery(payQueryParamVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return SwiftPassUtil.payNotify(map);
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
