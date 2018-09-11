package com.hhly.paycore.paychannel.pufa.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.pufa.config.PuFaConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.RefundResultVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 浦发银行工具类
 * @author xiongJinGang
 * @date 2017年11月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PuFaUtil {
	private static final Logger logger = Logger.getLogger(PuFaUtil.class);

	/**  
	* 方法说明: 构建扫码支付参数
	* @auth: xiongJinGang
	* @param paymentInfo
	* @time: 2017年10月12日 下午5:06:38
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWXParam(PaymentInfoBO paymentInfo, String platform) {
		try {
			// 装载订单信息
			Map<String, String> map = new HashMap<String, String>();
			// 根据平台获取商户号和二级商户号
			Map<String, String> configMap = getConfig(platform);
			String merNo = configMap.get("merNo");
			String subMchId = configMap.get("subMchId");
			String productId = "0109";
			String transId = "12";
			if (platform.equals(PayAbstract.PLATFORM_APP)) {
				productId = "0104";
				transId = "11";
			}

			map.put("requestNo", DateUtil.getNow("yyyyMMddHHmmssSSS"));// 请求流水号
			map.put("version", "V1.1");// V1.1
			map.put("productId", productId);// 产品类型 本接口固定传入0109
			map.put("transId", transId);// 交易类型 本接口固定传入12
			map.put("merNo", merNo);// 商户号 平台分配
			map.put("subMchId", subMchId);// 二级商户编号 商户提供资料申请获取
			map.put("orderDate", DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE));// 商品订单支付日期yyyyMMdd
			map.put("orderNo", paymentInfo.getNoOrder());// 商户订单号
			map.put("clientIp", paymentInfo.getUserreqIp());// 真实的客户端IP，否则唤起不了支付。
			map.put("notifyUrl", paymentInfo.getNotifyUrl());// 异步通知地址
			map.put("transAmt", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));// 交易金额
			map.put("commodityName", paymentInfo.getNameGoods());// 商品名称
			// 以下3个参数是选填
			// map.put("agentId", "");//代理商号 平台分配
			// map.put("storeId", "");//门店编号
			// map.put("limitPay", "");//传no_credit限制信用卡，不传不限制
			map.put("signature", signData(map));// 验签字段
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<String, String> buildJsapiParam(PaymentInfoBO paymentInfo, String platform) {
		try {
			// 装载订单信息
			Map<String, String> map = new ConcurrentHashMap<String, String>();
			// 根据平台获取商户号和二级商户号
			Map<String, String> configMap = getConfig(platform);
			String merNo = configMap.get("merNo");
			String subMchId = configMap.get("subMchId");
			String productId = "0109";
			String transId = "12";
			if (platform.equals(PayAbstract.PLATFORM_APP)) {
				productId = "0104";
				transId = "11";
			}

			map.put("requestNo", DateUtil.getNow("yyyyMMddHHmmssSSS"));// 请求流水号
			map.put("version", "V1.1");// V1.1
			map.put("productId", productId);// 产品类型 本接口固定传入0109
			map.put("transId", transId);// 交易类型 本接口固定传入12
			map.put("merNo", merNo);// 商户号 平台分配
			map.put("subMchId", subMchId);// 二级商户编号 商户提供资料申请获取
			map.put("orderDate", DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE));// 商品订单支付日期yyyyMMdd
			map.put("orderNo", paymentInfo.getNoOrder());// 商户订单号
			map.put("clientIp", paymentInfo.getUserreqIp());// 真实的客户端IP，否则唤起不了支付。
			map.put("notifyUrl", paymentInfo.getNotifyUrl());// 异步通知地址
			map.put("transAmt", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));// 交易金额
			map.put("commodityName", paymentInfo.getNameGoods());// 商品名称
			// 以下3个参数是选填
			// map.put("agentId", "");//代理商号 平台分配
			// map.put("storeId", "");//门店编号
			// map.put("limitPay", "");//传no_credit限制信用卡，不传不限制
			map.put("signature", signData(map));// 验签字段
			return map;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/*	private void sendMerchartReport() {
			Map<String, String> configMap = getConfig(platform);
			String merNo = configMap.get("merNo");
			Map<String, String> map = new ConcurrentHashMap<String, String>();
			map.put("requestNo", DateUtil.getNow("yyyyMMddHHmmssSSS"));// 请求流水号
			map.put("version", "V1.1");// V1.1
			map.put("productId", "0112");// 产品类型 固定传入0112
			map.put("transId", "16");// 交易类型 本接口固定传入16
			map.put("payWay", "WX");// 支付渠道;WX-微信报备,ALIPAY-支付宝报备
			map.put("merNo", merNo);// 商户号 平台分配
			map.put("subMchId", subMchId);// 二级商户名称
			map.put("orderDate", DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE));// 商品订单支付日期yyyyMMdd
			map.put("orderNo", paymentInfo.getNoOrder());// 商户订单号
			map.put("clientIp", paymentInfo.getUserreqIp());// 真实的客户端IP，否则唤起不了支付。
			map.put("notifyUrl", paymentInfo.getNotifyUrl());// 异步通知地址
			map.put("transAmt", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));// 交易金额
			map.put("commodityName", paymentInfo.getNameGoods());// 商品名称
		}*/

	/**  
	* 方法说明: 从异步通知中，得到支付结果
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月16日 下午3:01:13
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> analyPayResult(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("浦发支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("signature")) {
			// 查询返回0000时，表示查询成功，其他返回码表示查询失败
			if (map.containsKey("respCode")) {
				payNotifyResult.setOrderCode(map.get("orderNo"));// 商户唯一订单号
				if (map.get("respCode").equals("0000")) {
					payNotifyResult.setThirdTradeNo(map.get("transactionId"));// 平台订单号
					payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("transAmt"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65

					String tradeTime = DateUtil.getNow(DateUtil.DEFAULT_FORMAT);
					if (!ObjectUtil.isBlank(map.get("timeEnd"))) {
						tradeTime = map.get("timeEnd");// 支付完成时间 格式：yyyyMMddHHmmss
						tradeTime = DateUtil.convertStrToTarget(tradeTime, DateUtil.DATE_FORMAT_NUM, DateUtil.DEFAULT_FORMAT);
					}
					payNotifyResult.setTradeTime(tradeTime);// 支付完成时间 格式：yyyyMMddHHmmss
					payNotifyResult.setResponse("SUCCESS");
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				return ResultBO.ok(payNotifyResult);
			}
		} else {
			logger.error("浦发支付异步返回，无签名字段");
			return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 获取支付结果
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:41:00
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO, String platform) {
		try {
			// 封装请求参数
			Map<String, String> resultMap = getPayStatus(payQueryParamVO, platform);
			if (!ObjectUtil.isBlank(resultMap)) {
				// 查询返回0000时，表示查询成功，其他返回码表示查询失败，应该重新发起查询，一直查询到返回0000。原始订单状态参考origRespCode返回码。
				if (resultMap.containsKey("respCode") && resultMap.get("respCode").equals("0000")) {
					if (resultMap.get("origRespCode").equals("0000")) {// 原交易码为0000，表示成功
						if (PuFaUtil.verferSignData(resultMap)) {
							PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
							payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(resultMap.get("transAmt")));
							payQueryResultVO.setTradeNo(resultMap.get("transactionId"));
							payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
							payQueryResultVO.setOrderCode(resultMap.get("orderNo"));
							payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
							return ResultBO.ok(payQueryResultVO);
						} else {
							logger.info("验证签名不通过");
							return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("查询浦发【" + payQueryParamVO.getTransCode() + "】交易结果异常");
		}
		logger.info("【" + payQueryParamVO.getTransCode() + "】查询交易结果失败");
		return ResultBO.err();
	}

	/**  
	* 方法说明: 退款
	* @auth: xiongJinGang
	* @param refundParam
	* @param platform
	* @time: 2017年11月2日 下午6:28:34
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> orderRefund(RefundParamVO refundParam, String platform) {
		Map<String, String> paramMap = new HashMap<String, String>();
		TransRechargeBO transRechargeBO = refundParam.getTransRechargeBO();
		// 根据平台获取商户号和二级商户号
		Map<String, String> configMap = getConfig(platform);
		String merNo = configMap.get("merNo");
		try {
			paramMap.put("requestNo", DateUtil.getNow("yyyyMMddHHmmssSSS"));// 请求流水号
			paramMap.put("version", "V1.1");// 版本号，version默认值是1.1
			paramMap.put("transId", "02");// 交易类型 本接口固定传入02
			// paramMap.put("agentId", "MD5");// 代理商号(平台分配)
			paramMap.put("merNo", merNo);// 商户号
			paramMap.put("orderDate", DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE));// 退货申请日期yyyyMMdd
			paramMap.put("orderNo", refundParam.getRefundCode());// 退货申请订单号
			String orderDate = DateUtil.convertDateToStr(transRechargeBO.getCreateTime(), DateUtil.DATE_FORMAT_NO_LINE);
			paramMap.put("origOrderDate", orderDate);// 原商品订单的日期yyyyMMdd
			paramMap.put("origOrderNo", refundParam.getTransCode());// 原交易商户订单号
			paramMap.put("returnUrl", refundParam.getNotifyUrl());// 页面通知地址
			paramMap.put("notifyUrl", refundParam.getNotifyUrl());// 异步通知地址
			paramMap.put("transAmt", GetWeChatUtil.getMoney(refundParam.getRefundAmount() + ""));// 交易金额
			paramMap.put("refundReson", refundParam.getRefundReason());// 退货原因

			paramMap.put("signature", signData(paramMap));// 验签字段
		} catch (Exception e1) {
			logger.error("交易号【" + refundParam.getTransCode() + "】申请退款，签名异常");
			return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
		}

		try {
			logger.debug("退款请求参数：" + paramMap.toString());
			String result = HttpUtil.doPost(PuFaConfig.PF_PAY_URL, paramMap);
			logger.info("【" + refundParam.getTransCode() + "】申请退款返回结果：" + result);
			Map<String, String> resultMap = transStringToMap(result);

			if (!ObjectUtil.isBlank(resultMap)) {
				if (resultMap.containsKey("respCode")) {
					RefundResultVO refundResultVO = new RefundResultVO();
					refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
					refundResultVO.setResultMsg("退款失败");
					if (resultMap.get("respCode").equals("0000")) {
						if (PuFaUtil.verferSignData(resultMap)) {
							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setOrderCode(resultMap.get("origOrderNo"));// 商户订单号
							refundResultVO.setRefundCode(resultMap.get("orderNo"));// 商户退款单号
							refundResultVO.setTransactionId(resultMap.get("requestNo"));// 平台订单号
							refundResultVO.setRefundId(resultMap.get("requestNo"));// 平台退款单号
							refundResultVO.setResultMsg(resultMap.get("respDesc"));
							refundResultVO.setRefundAmount(GetWeChatUtil.changeF2Y(resultMap.get("transAmt")));// 退款总金额,单位为分,可以做部分退款
							refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_PROCESSING);
							return ResultBO.ok(refundResultVO);
						}
						return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					} else {
						String errorMsg = "退款请求失败";
						if (resultMap.containsKey("respDesc")) {
							errorMsg = resultMap.get("respDesc");
						}
						return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMsg);
					}
				}
			}
		} catch (Exception e) {
			logger.error("浦发退款请求异常", e);
		}
		return ResultBO.err(MessageCodeConstants.REFUND_REQUEST_FAIL_ERROR_SERVICE);
	}

	/**  
	* 方法说明: 退款查询
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月16日 下午3:48:25
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO, String platform) {
		try {
			// 封装请求参数
			Map<String, String> resultMap = getPayStatus(payQueryParamVO, platform);
			RefundResultVO refundResultVO = new RefundResultVO();
			refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());
			refundResultVO.setResultMsg("退款失败");
			if (!ObjectUtil.isBlank(resultMap)) {
				// 查询返回0000时，表示查询成功，其他返回码表示查询失败，应该重新发起查询，一直查询到返回0000。原始订单状态参考origRespCode返回码。
				if (resultMap.containsKey("respCode") && resultMap.get("respCode").equals("0000")) {
					if (resultMap.get("origRespCode").equals("0000")) {// 原交易码为0000，表示成功
						if (PuFaUtil.verferSignData(resultMap)) {
							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setOrderCode(resultMap.get("origOrderNo"));// 商户订单号
							refundResultVO.setTransactionId(resultMap.get("transactionId"));// 平台订单号
							refundResultVO.setResultMsg(resultMap.get("transactionId"));
							if (resultMap.containsKey("refundAmt")) {
								refundResultVO.setRefundAmount(String.valueOf(resultMap.get("refundAmt")));// 退款总金额,单位为分,可以做部分退款
							}
							refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_SUCCESS);
							return ResultBO.ok(refundResultVO);
						} else {
							logger.info("验证签名不通过");
							return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("查询浦发【" + payQueryParamVO.getTransCode() + "】退款交易结果异常");
		}
		logger.info("【" + payQueryParamVO.getTransCode() + "】查询退款交易结果失败");
		return ResultBO.err();

	}

	private static String signData(Map<String, String> map) throws Exception {
		String signatureStr = BuildRequestFormUtil.createLinkString(map);
		String signData = null;
		if (!map.containsKey("agentId")) {
			String path = getLocalPath(PuFaConfig.PF_PRIVATE_KEY_PATH);
			signData = RSAUtil.signByPrivate(signatureStr, RSAUtil.readFile(path, "UTF-8"), "UTF-8");
		} else {
			String path = getLocalPath(PuFaConfig.PF_AGENT_PRIVATE_KEY_PATH);
			signData = RSAUtil.signByPrivate(signatureStr, RSAUtil.readFile(path, "UTF-8"), "UTF-8");
		}
		logger.info("请求数据：" + signatureStr + "&signature=" + signData);
		return signData;
	}

	public static boolean verferSignData(Map<String, String> map) {
		String signature = "";
		if (map.containsKey("signature")) {
			signature = map.get("signature");
		}
		map.remove("signature");
		String signatureStr = BuildRequestFormUtil.createLinkString(map);
		String path = getLocalPath(PuFaConfig.PF_PUBLIC_KEY_PATH);
		return RSAUtil.verifyByKeyPath(signatureStr, signature, path, "UTF-8");
	}

	/**  
	* 方法说明: 交易结果，String 转 Map
	* @auth: xiongJinGang
	* @param result
	* @time: 2017年11月3日 上午10:42:45
	* @return: Map<String,String> 
	*/
	public static Map<String, String> transStringToMap(String result) {
		Map<String, String> resultMap = new HashMap<String, String>();
		if (!ObjectUtil.isBlank(result)) {
			String[] results = StringUtils.split(result, "&");
			for (int i = 0; i < results.length; i++) {
				String[] childResult = StringUtils.split(results[i], "=", 2);
				resultMap.put(childResult[0], childResult[1]);
			}
		}
		return resultMap;
	}

	private static String getLocalPath(String filePath) {
		return PuFaUtil.class.getResource(filePath).getPath();
	}

	/**  
	* 方法说明: 获取配置
	* @auth: xiongJinGang
	* @param platform
	* @time: 2017年11月3日 下午12:21:37
	* @return: Map<String,String> 
	*/
	public static Map<String, String> getConfig(String platform) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("merNo", PuFaConfig.PF_MERCHANT_NO);
		map.put("subMchId", PuFaConfig.PF_WX_SUB_MCH_ID);

		if (platform.equals(PayAbstract.PLATFORM_APP)) {
			map.put("merNo", PuFaConfig.PF_APP_MERCHANT_NO);
			map.put("subMchId", PuFaConfig.PF_APP_WX_SUB_MCH_ID);
		}
		return map;
	}

	/**  
	* 方法说明: 拼装获取交易结果请求参数
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @param platform
	* @time: 2017年11月4日 上午10:47:41
	* @return: void 
	 * @throws  
	 * @throws Exception 
	*/
	private static Map<String, String> getPayStatus(PayQueryParamVO payQueryParamVO, String platform) throws Exception {
		// 封装请求参数
		Map<String, String> paramMap = new HashMap<String, String>();
		TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
		// 根据平台获取商户号和二级商户号
		Map<String, String> configMap = getConfig(platform);
		String merNo = configMap.get("merNo");
		paramMap.put("requestNo", DateUtil.getNow("yyyyMMddHHmmssSSS"));// 交易请求流水号
		paramMap.put("version", "V1.2");// 新接口V1.2
		paramMap.put("transId", "04");// 交易类型 固定传入04
		paramMap.put("merNo", merNo);// 商户号
		String orderDate = DateUtil.convertDateToStr(transRechargeBO.getCreateTime(), DateUtil.DATE_FORMAT_NO_LINE);
		paramMap.put("orderDate", orderDate);// 原商品订单的日期yyyyMMdd
		paramMap.put("orderNo", payQueryParamVO.getTransCode());// 查询的商户订单号
		// paramMap.put("agentId", "");// 代理商号
		paramMap.put("signature", signData(paramMap));// 验签字段
		String result = HttpUtil.doPost(PuFaConfig.PF_PAY_URL, paramMap);
		logger.info("【" + payQueryParamVO.getTransCode() + "】查询交易结果：" + result);
		return transStringToMap(result);
	}
}