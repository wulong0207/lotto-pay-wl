package com.hhly.paycore.paychannel.shenzhoupay.util;

import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.paycore.paychannel.nowpay.util.FormDateReportConvertor;
import com.hhly.paycore.paychannel.shenzhoupay.config.DivineConfig;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;

import net.sf.json.JSONObject;

public class DivineUtil {
	private static Logger logger = LoggerFactory.getLogger(DivineUtil.class);

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @return
	* @time: 2017年10月24日 上午11:54:58
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> queryOrder(PayQueryParamVO payQueryParamVO) {
		PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		try {
			String url = String.format(DivineConfig.SHENZHOU_PAY_QUERY_URL, payQueryParamVO.getTransCode(), DivineConfig.SHENZHOU_PAY_WX_APPID);
			logger.info("神州支付商户主动查询订单状态请求url:" + url);
			// String reposeText = HttpNowPayUtils.http(url, packageParams);
			String reposeText = HttpUtil.doPost(url, packageParams);
			if (ObjectUtil.isBlank(reposeText)) {
				return null;
			}
			logger.info("神州支付商户主动查询订单状态返回结果:" + reposeText);
			Map<String, String> map = FormDateReportConvertor.parseFormDataPatternReportWithDecode(reposeText, "UTF-8", "UTF-8");
			logger.info("神州支付商户主动查询订单状态转码返回结果:" + JSONObject.fromObject(map));// {"returnCode":"720","errorMsg":"商户订单号或应用id不正确"}
			String responseCode = map.get("returnCode");
			if ("200".equals(responseCode)) {
				String signature = map.get("signature");
				map.remove("signature");
				String mhtSignature = RSAUtil.doEncrypt(map, DivineConfig.SHENZHOU_PAY_WX_KEY);
				if (!mhtSignature.equals(signature)) {
					logger.error("神州支付异步通知结果：签名错误,现在支付我方签名结果：" + mhtSignature + ",第三方签名:" + signature);
					return ResultBO.err();
				}
				// 2.封装返回对象
				if (map.containsKey("orderNo")) { // 第三方交易号
					payQueryResultVO.setTradeNo(map.get("orderNo"));
				}
				if (map.containsKey("mchntOrderNo")) {// 商户订单号
					payQueryResultVO.setOrderCode(map.get("mchntOrderNo"));
				}
				if (map.containsKey("amount")) {// 支付金额
					String totalFee = map.get("amount");
					if (!ObjectUtil.isBlank(totalFee)) {
						double tf = Double.valueOf(totalFee) / 100;
						payQueryResultVO.setTotalAmount(String.valueOf(tf));
					}
				}
				if (map.containsKey("paySt")) {// 支付状态
					String status = map.get("paySt");
					// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
					if ("2".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
					} else if ("0".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.WAITTING_PAYMENT);
					} else if ("1".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.BEING_PAID);
					} else if ("3".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_FAILURE);
					} else if ("4".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.REFUND);
					} else if ("6".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.USER_CANCELLED_PAYMENT);
					} else if ("5".equals(status)) {
						payQueryResultVO.setTradeStatus(PayStatusEnum.OVERDUE_PAYMENT);
					}
				}
				payQueryResultVO.setArriveTime(DateUtil.convertDateToStr(new Date(), DateUtil.DATE_FORMAT_NUM));
			} else if ("500".equals(responseCode)) {
				logger.info("神州支付商户主动查询订单状态响应:系统异常");
				return ResultBO.err();
			} else if ("720".equals(responseCode)) {
				logger.info("神州支付商户主动查询订单状态响应:商户订单号或应用id填写不正确");
				return ResultBO.err();
			} else {
				return ResultBO.err();
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.ok(payQueryResultVO);
	}

	/**  
	* 方法说明: 支付异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月24日 下午12:21:29
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			String signatrue = map.get("signature");
			map.remove("signature");
			String mhtSignature = RSAUtil.doEncrypt(map, DivineConfig.SHENZHOU_PAY_WX_KEY);
			if (!mhtSignature.equals(signatrue)) {
				logger.error("神州支付异步通知结果：签名错误,现在支付我方签名结果：" + mhtSignature + ",第三方签名:" + signatrue);
				payNotifyResult.setResponse("SUCCESS");
				payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 支付失败
				return ResultBO.ok(payNotifyResult);
			}
			// 2.封装返回对象
			if (map.containsKey("orderNo")) { // 支付平台订单号
				payNotifyResult.setThirdTradeNo(map.get("orderNo"));
			}
			if (map.containsKey("mchntOrderNo")) {// 商户订单号
				payNotifyResult.setOrderCode(map.get("mchntOrderNo"));
			}
			if (map.containsKey("amount")) {// 支付金额
				String totalFee = String.valueOf(map.get("amount"));
				if (!ObjectUtil.isBlank(totalFee)) {
					double tf = Double.valueOf(totalFee) / 100;
					payNotifyResult.setOrderAmt(tf);
				}
			}
			if (map.containsKey("paySt")) {// 支付状态
				String status = String.valueOf(map.get("paySt"));
				// 0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
				if ("2".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else if ("0".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.WAITTING_PAYMENT);
				} else if ("1".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.BEING_PAID);
				} else if ("3".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				} else if ("4".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.REFUND);
				} else if ("6".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.USER_CANCELLED_PAYMENT);
				} else if ("5".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.OVERDUE_PAYMENT);
				}
			}
			payNotifyResult.setTradeTime(DateUtil.convertDateToStr(new Date(), DateUtil.DATE_FORMAT_NUM));
			payNotifyResult.setResponse("SUCCESS=true");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return ResultBO.err();
		}
		return ResultBO.ok(payNotifyResult);
	}
}
