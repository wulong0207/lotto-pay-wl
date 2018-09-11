package com.hhly.paycore.paychannel.yeepay.web;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.yeepay.config.YeepayConfig;
import com.hhly.paycore.paychannel.yeepay.utils.Digest;
import com.hhly.paycore.paychannel.yeepay.utils.YeepayStandardUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.PayReturnResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;

/**
 * @author YiJian
 * @version 1.0
 * @Desc: 易宝 WEB支付，只支持PC端的储蓄卡网银支付
 * @date 2017年5月17日
 * @compay 益彩网络科技有限公司
 */
public class YeepayWebService extends PayAbstract {
	static Logger logger = Logger.getLogger(YeepayWebService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		logger.debug("易宝网银支付开始");

		Map<String, String> params = new HashMap<String, String>();
		params.put("p0_Cmd", "Buy");
		// params.put("p1_MerId",YeepayStandardUtil.getP1_MerId());
		if (!StringUtil.isBlank(paymentInfoBO.getNoOrder())) {
			params.put("p2_Order", paymentInfoBO.getNoOrder());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getMoneyOrder())) {
			params.put("p3_Amt", paymentInfoBO.getMoneyOrder());
		}
		params.put("p4_Cur", "CNY");
		if (!StringUtil.isBlank(paymentInfoBO.getNameGoods())) {
			params.put("p5_Pid", paymentInfoBO.getNameGoods());
		}
		// params.put("p6_Pcat", p6_Pcat); //商品种类
		if (!StringUtil.isBlank(paymentInfoBO.getInfoOrder())) {
			params.put("p7_Pdesc", paymentInfoBO.getInfoOrder());
		}
		// if (!StringUtil.isBlank(paymentInfoBO.getNotifyUrl())) {
		// params.put("p8_Url", paymentInfoBO.getNotifyUrl());
		// }
		// 传递同步地址
		if (!StringUtil.isBlank(paymentInfoBO.getUrlReturn())) {
			params.put("p8_Url", paymentInfoBO.getUrlReturn());
		}
		// params.put("p9_SAF", p9_SAF); //送货地址
		if (!StringUtil.isBlank(paymentInfoBO.getAttach())) {
			params.put("pa_MP", paymentInfoBO.getAttach());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getBankCode())) {
			params.put("pd_FrpId", paymentInfoBO.getBankCode());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getValidOrder())) {
			params.put("pm_Period", paymentInfoBO.getValidOrder());
			params.put("pn_Unit", "minute");
		}
		params.put("pr_NeedResponse", "1");
		/*
		if (!StringUtil.isBlank(paymentInfoBO.getAcctName())) {
			params.put("pt_UserName", paymentInfoBO.getAcctName());
		}
		if (!StringUtil.isBlank(paymentInfoBO.getIdNo())) {
			params.put("pt_PostalCode", paymentInfoBO.getIdNo());
		}
		// params.put("pt_Address",pt_Address);
		if (!StringUtil.isBlank(paymentInfoBO.getCardNo())) {
			params.put("pt_TeleNo", paymentInfoBO.getCardNo());
		}
		// params.put("pt_Mobile",paymentInfoBO.get);
		// params.put("pt_Email",pt_Email);
		*/
		if (!StringUtil.isBlank(paymentInfoBO.getUserId())) {
			params.put("pt_LeaveMessage", paymentInfoBO.getUserId());
		}

		String payURL = YeepayStandardUtil.getPayURL(params);
		PayReqResultVO payReqResult = new PayReqResultVO(payURL);
		payReqResult.setType(PayConstants.PayReqResultEnum.URL.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.YEEPAY_RECHARGE.getKey());
		if (ObjectUtil.isBlank(payURL)) {
			payReqResult.setCode(false);
			payReqResult.setMsg("易宝未返回请求URL");
		}
		return ResultBO.ok(payReqResult);
		/*
		String hmac = YeepayStandardUtil.getHmac(params);
		params.put("hmac",hmac);
		String reqUrl = PaymobileUtils.getRequestUrl("requestURL");
		String sHtmlText = AlipaySubmit.buildRequest(reqUrl, params, "post", "utf-8");
		PayReqResultVO payReqResult = new PayReqResultVO(sHtmlText);
		payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
		return ResultBO.ok(payReqResult);
		*/
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		logger.debug("易宝退款开始");

		Map<String, String> params = new HashMap<String, String>();
		params.put("p0_Cmd", "RefundOrd"); // 固定值：RefundOrd
		if (!StringUtil.isBlank(refundParam.getRefundCode())) {
			params.put("p2_Order", refundParam.getRefundCode()); // 退款流水号,若填写则与响应参数中的r4_order相同，用于退款查询
		}
		params.put("pb_TrxId", refundParam.getOrderCode()); // 易宝交易流水号（退款时，交易号存在订单号中传值过来）
		params.put("p3_Amt", StringUtil.convertObjToStr(refundParam.getRefundAmount())); // 退款金额
		params.put("p4_Cur", "CNY");
		params.put("p5_Desc", refundParam.getRefundReason()); // 退款描述
		try {
			RefundResultVO refundResultVO = new RefundResultVO();

			// 发送退款请求
			Map<String, String> refundResult = YeepayStandardUtil.refundByTrxId(params);
			String r1_Code = StringUtil.trimSpace(refundResult.get("r1_Code")); // 退款结果
			String r2_TrxId = StringUtil.trimSpace(refundResult.get("r2_TrxId")); // 易宝交易流水号
			String r3_Amt = StringUtil.trimSpace(refundResult.get("r3_Amt")); // 退款金额
			String r4_Order = StringUtil.trimSpace(refundResult.get("r4_Order")); // 退款请求号
			// String rf_fee = StringUtil.trimSpace(refundResult.get("rf_fee")); // 已退手续费
			String hmacError = StringUtil.trimSpace(refundResult.get("hmacError"));
			String errorMsg = StringUtil.trimSpace(refundResult.get("errorMsg"));

			String resultMsg = "";
			if (StringUtil.isBlank(errorMsg)) {
				if (StringUtil.isBlank(hmacError)) {
					if (r1_Code.equals("2")) {
						resultMsg = "账户状态无效";
					} else if (r1_Code.equals("7")) {
						resultMsg = "该订单不支持退款";
					} else if (r1_Code.equals("10")) {
						resultMsg = "退款金额超限";
					} else if (r1_Code.equals("18")) {
						resultMsg = "余额不足";
					} else if (r1_Code.equals("50")) {
						resultMsg = "订单不存在";
					} else if (r1_Code.equals("55")) {
						resultMsg = "历史退款未开通";
					} else if (r1_Code.equals("6801")) {
						resultMsg = "IP限制";
					}
				} else {
					logger.info("Hmac error !" + hmacError);
					return ResultBO.err();
				}

				if ("1".equals(r1_Code)) {
					refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
				} else {
					refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
				}

				refundResultVO.setTransactionId(r2_TrxId);
				refundResultVO.setRefundAmount(r3_Amt);
				refundResultVO.setRefundId(r4_Order);
				refundResultVO.setResultMsg(resultMsg);
				refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_PROCESSING);

				return ResultBO.ok(refundResultVO);
			} else {
				logger.info("Refund Failed!" + errorMsg);
				return ResultBO.err();
			}
		} catch (Exception e) {
			return ResultBO.err();
		}
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("易宝详情查询");

		try {
			String p0_Cmd = "QueryOrdDetail";
			String p1_MerId = YeepayConfig.P1_MERID;
			String p2_Order = payQueryParamVO.getTransCode();
			String keyValue = YeepayConfig.KEY_VALUE;
			String pv_Ver = "3.0";// 固定值为 3.0
			String p3_ServiceType = "2";// 查询类型，默认值为 2

			String[] strArr = { p0_Cmd, p1_MerId, p2_Order, pv_Ver, p3_ServiceType };

			String hmac = Digest.getHmac(strArr, keyValue);

			Map<String, String> queryParams = new HashMap<String, String>();
			queryParams.put("p0_Cmd", p0_Cmd);
			queryParams.put("p1_MerId", p1_MerId);
			queryParams.put("p2_Order", p2_Order);
			queryParams.put("pv_Ver", pv_Ver);
			queryParams.put("p3_ServiceType", p3_ServiceType);
			queryParams.put("hmac", hmac);

			String queryURL = YeepayConfig.QUERY_URL;
			logger.debug("查询URL【" + queryURL + "】，请求参数【" + queryParams.toString() + "】");

			String r0_Cmd = "";
			String r1_Code = "";
			String r2_TrxId = "";
			String r3_Amt = "";
			String r4_Cur = "";
			String r5_Pid = "";
			String r6_Order = "";
			String r8_MP = "";
			String rw_RefundRequestID = "";
			String rx_CreateTime = "";
			String ry_FinshTime = "";
			String rz_RefundAmount = "";
			String rb_PayStatus = "";
			String rc_RefundCount = "";
			String rd_RefundAmt = "";
			String hmacFromYeepay = "";
			String hmac_safeFromYeepay = "";

			List responseList = null;
			try {
				String result = HttpUtil.doGet(queryURL, queryParams);
				if (ObjectUtil.isBlank(result)) {
					return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
				}
				responseList = JSON.parseObject(result, new TypeReference<List<?>>() {
				});
			} catch (Exception e) {
				logger.error("查询交易号【" + p2_Order + "】详情异常", e);
				logger.error("查询URL【" + queryURL + "】，请求参数【" + queryParams.toString() + "】");
				return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
			}

			if (responseList == null) {
				return ResultBO.err(MessageCodeConstants.THIRD_API_READ_TIME_OUT);
			} else {
				Iterator iter = responseList.iterator();
				while (iter.hasNext()) {
					String temp = YeepayStandardUtil.formatString((String) iter.next());
					if (temp.equals("")) {
						continue;
					}
					int i = temp.indexOf("=");
					int j = temp.length();
					if (i >= 0) {
						String tempKey = temp.substring(0, i);
						String tempValue = URLDecoder.decode(temp.substring(i + 1, j), "GBK");
						if ("r0_Cmd".equals(tempKey)) {
							r0_Cmd = tempValue;
						} else if ("r1_Code".equals(tempKey)) {
							r1_Code = tempValue;
						} else if ("r2_TrxId".equals(tempKey)) {
							r2_TrxId = tempValue;
						} else if ("r3_Amt".equals(tempKey)) {
							r3_Amt = tempValue;
						} else if ("r4_Cur".equals(tempKey)) {
							r4_Cur = tempValue;
						} else if ("r5_Pid".equals(tempKey)) {
							r5_Pid = tempValue;
						} else if ("r6_Order".equals(tempKey)) {
							r6_Order = tempValue;
						} else if ("r8_MP".equals(tempKey)) {
							r8_MP = tempValue;
						} else if ("rw_RefundRequestID".equals(tempKey)) {
							rw_RefundRequestID = tempValue;
						} else if ("rx_CreateTime".equals(tempKey)) {
							rx_CreateTime = tempValue;
						} else if ("ry_FinshTime".equals(tempKey)) {
							ry_FinshTime = tempValue;
						} else if ("rz_RefundAmount".equals(tempKey)) {
							rz_RefundAmount = tempValue;
						} else if ("rb_PayStatus".equals(tempKey)) {
							rb_PayStatus = tempValue;
						} else if ("rc_RefundCount".equals(tempKey)) {
							rc_RefundCount = tempValue;
						} else if ("rd_RefundAmt".equals(tempKey)) {
							rd_RefundAmt = tempValue;
						} else if ("hmac".equals(tempKey)) {
							hmacFromYeepay = tempValue;
						} else if ("hmac_safe".equals(tempKey)) {
							hmac_safeFromYeepay = tempValue;
						}
					}
				}

				String[] stringArr = { r0_Cmd, r1_Code, r2_TrxId, r3_Amt, r4_Cur, r5_Pid, r6_Order, r8_MP, rw_RefundRequestID, rx_CreateTime, ry_FinshTime, rz_RefundAmount, rb_PayStatus, rc_RefundCount, rd_RefundAmt };
				String localHmac = Digest.getHmac(stringArr, keyValue);
				boolean ishmac_safe = YeepayStandardUtil.verifyCallbackHmac_safe(stringArr, hmac_safeFromYeepay);

				if (!localHmac.equals(hmacFromYeepay) || !ishmac_safe) {
					StringBuffer temp = new StringBuffer();
					for (int i = 0; i < stringArr.length; i++) {
						temp.append(stringArr[i]);
					}
				}
			}

			if (ObjectUtil.isBlank(r1_Code)) {
				return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
			}
			if (r1_Code.equals("50")) {// 1：查询正常；50：订单不存在
				return ResultBO.err(MessageCodeConstants.DATA_NOT_FOUND_SYS);
			}

			PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
			if (rb_PayStatus.equals("SUCCESS")) {// 支付成功
				payQueryResultVO.setTradeStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
			} else if (rb_PayStatus.equals("INIT") || rb_PayStatus.equals("CANCELED")) {// 未支付、已取消
				// payQueryResultVO.setTradeStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);
				// 直接改状态
				return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
			} else {
				payQueryResultVO.setTradeStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 没匹配上，都失败
			}
			payQueryResultVO.setTradeNo(r2_TrxId);// 易宝交易流水号
			payQueryResultVO.setTotalAmount(r3_Amt);// 交易的订单金额
			payQueryResultVO.setReceiptAmount(r3_Amt);// 实收金额
			payQueryResultVO.setPayAmount(r3_Amt);// 买家实付金额
			payQueryResultVO.setArriveTime(null);// 交易时间先设置成空
			payQueryResultVO.setOrderCode(r6_Order);// 益彩订单号
			// payQueryResultVO.setArriveTime(YeepayStandardUtil.timeStamp2Date(ry_FinshTime, null));
			// 时间格式为："20170712192704"
			payQueryResultVO.setArriveTime(DateUtil.convertStrToTarget(ry_FinshTime, DateUtil.DATE_FORMAT_NUM, DateUtil.DEFAULT_FORMAT));
			return ResultBO.ok(payQueryResultVO);
		} catch (Exception e) {
			logger.error("获取【" + payQueryParamVO.getTransCode() + "】网银支付结果异常", e);
			return ResultBO.err(MessageCodeConstants.THIRD_API_QUERY_ERROR);
		}
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		logger.debug("易宝退款查询");
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		logger.debug("易宝异步通知");
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();

		String p1_MerId = map.get("p1_MerId");// 商户编号
		String r0_Cmd = map.get("r0_Cmd");// 业务类型
		String r1_Code = map.get("r1_Code");// 交易结果
		String r2_TrxId = map.get("r2_TrxId");// 易宝支付流水号
		String r3_Amt = map.get("r3_Amt");// 金额
		String r4_Cur = map.get("r4_Cur");// 币种
		String r5_Pid = map.get("r5_Pid");// 商品名称
		String r6_Order = map.get("r6_Order");// 商户订单号
		String r7_Uid = map.get("r7_Uid");// 易宝支付会员ID
		String r8_MP = map.get("r8_MP");// 商户拓展信息
		String r9_BType = map.get("r9_BType"); // 通知类型
		String rp_PayDate = map.get("rp_PayDate");// 支付成功时间
		String hmac = map.get("hmac");// 签名数据
		String hmac_safe = map.get("hmac_safe");// 安全签名数据
		// String rb_BankId = map.get("rb_BankId");// 支付通道编码
		// String ro_BankOrderId = map.get("ro_BankOrderId");// 银行订单号
		// String rq_CardNo = map.get("rq_CardNo");// 神州行充值卡号
		// String ru_Trxtime = map.get("ru_Trxtime");// 通知时间
		// String rq_SourceFee = map.get("rq_SourceFee");// 用户手续费
		// String rq_TargetFee = map.get("rq_TargetFee");// 商户手续费

		r0_Cmd = ObjectUtil.isBlank(r0_Cmd) ? "" : r0_Cmd;//
		r7_Uid = ObjectUtil.isBlank(r7_Uid) ? "" : r7_Uid;// 如果为null，签名验证不通过
		r8_MP = ObjectUtil.isBlank(r8_MP) ? "" : r8_MP;//

		// 加密参数
		String[] strArr = { p1_MerId, r0_Cmd, r1_Code, r2_TrxId, r3_Amt, r4_Cur, r5_Pid, r6_Order, r7_Uid, r8_MP, r9_BType };

		try {
			boolean hmacIsCorrect = YeepayStandardUtil.verifyCallbackHmac(strArr, hmac);
			boolean hmacsafeIsCorrect = YeepayStandardUtil.verifyCallbackHmac_safe(strArr, hmac_safe);

			if (hmacIsCorrect && hmacsafeIsCorrect) {
				// 2服务器点对点通讯,方便走流程暂时先改为1页面浏览器重定向
				if (r9_BType.equals("2")) {
					if (!StringUtil.isBlank(r2_TrxId)) {
						payNotifyResult.setThirdTradeNo(r2_TrxId);
					}
					if (!StringUtil.isBlank(r6_Order)) {
						payNotifyResult.setOrderCode(r6_Order);
					}
					if (!StringUtil.isBlank(r3_Amt)) {
						payNotifyResult.setOrderAmt(Double.parseDouble(r3_Amt));
					}
					if (!StringUtil.isBlank(r8_MP)) {
						payNotifyResult.setAttachData(r8_MP);
					}
					if (!StringUtil.isBlank(rp_PayDate)) {
						payNotifyResult.setTradeTime(rp_PayDate);
					}
					if (!StringUtil.isBlank(r1_Code) && "1".equals(r1_Code)) {
						payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
					}
					payNotifyResult.setResponse("SUCCESS");
				} else {
					logger.info("通知类型为1：1 - 浏览器重定向 2 - 服务器点对点通讯");
				}
			} else {
				logger.error("易宝参数验签失败");
				return ResultBO.err();
			}
		} catch (Exception e) {
			logger.error("易宝标准收银台支付回调出错，" + e);
		}
		return ResultBO.ok(payNotifyResult);
	}

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		logger.debug("易宝同步通知");
		PayReturnResultVO payReturnResultVO = new PayReturnResultVO();
		return ResultBO.ok(payReturnResultVO);
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {

		return ResultBO.ok();
	}
}
