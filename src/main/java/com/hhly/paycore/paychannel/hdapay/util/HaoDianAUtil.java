package com.hhly.paycore.paychannel.hdapay.util;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.hdapay.config.HaoDianAConfig;
import com.hhly.paycore.sign.SignUtils;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeThirdEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.MathUtil;
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
 * @desc 好店啊工具类
 * @author xiongJinGang
 * @date 2017年10月17日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HaoDianAUtil {
	private static Logger logger = Logger.getLogger(HaoDianAUtil.class);
	public static final String SUCCESS = "0";// 成功标志

	private static Map<String, String> commonParam(String[] mchInfo) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("ds_id", mchInfo[0]);// 渠道商ID
		// map.put("mch_id", mchInfo[1]);// 商户ID
		long timeStampSec = System.currentTimeMillis() / 1000;
		String timestamp = String.format("%010d", timeStampSec);
		map.put("timestamp", timestamp);// 时间戳
		map.put("sign_type", "MD5");// 目前支持MD5和RSA2
		map.put("version", "1.0");// 固定值1.0
		return map;
	}

	/**  
	* 方法说明: 根据支付渠道获取加密信息【0：渠道商ID 1：商户ID 2：md5key】
	* @auth: xiongJinGang
	* @param rechargeChannel
	* @time: 2017年10月18日 下午5:19:49
	* @return: String[] 
	*/
	public static String[] getMchInfo(String rechargeChannel, String platForm) {
		String[] str = new String[3];
		str[0] = HaoDianAConfig.HDA_NEW_DS_ID;
		PayTypeThirdEnum payType = PayConstants.PayTypeThirdEnum.getEnum(rechargeChannel);
		switch (payType) {
		case ALIPAY_PAYMENT:// 支付宝支付
			if (platForm.equals(PayAbstract.PLATFORM_WEB)) {
				str[1] = HaoDianAConfig.HDA_ALIPAY_SCAN_MCH_ID;
			} else {
				str[1] = HaoDianAConfig.HDA_ALIPAY_WAP_MCH_ID;
			}
			break;
		case WEIXIN_PAYMENT:// 微信支付
			if (platForm.equals(PayAbstract.PLATFORM_WEB)) {
				str[1] = HaoDianAConfig.HDA_WECHAT_SCAN_MCH_ID;
			} else {
				str[1] = HaoDianAConfig.HDA_WX_WAP_MCH_ID;
			}
			break;
		case QQ_PAYMENT:// QQ钱包支付
			if (platForm.equals(PayAbstract.PLATFORM_WEB)) {
				str[1] = HaoDianAConfig.HDA_QQ_SCAN_MCH_ID;
			} else {
				str[1] = HaoDianAConfig.HDA_QQ_WAP_MCH_ID;
			}
			break;
		default:
			break;
		}
		str[2] = HaoDianAConfig.HDA_NEW_DS_SECRET;
		return str;
	}

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月16日 上午9:27:33
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWebMapParam(PaymentInfoBO paymentInfoBO, String[] mchInfo) {
		Map<String, String> map = commonParam(mchInfo);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("mch_id", mchInfo[1]);
		jsonObject.put("ds_trade_no", paymentInfoBO.getNoOrder());// 渠道商系统付款交易号
		jsonObject.put("pay_fee", paymentInfoBO.getMoneyOrder());// 支付金额，2位小数，如果是整数金额，用.00来格式化。举例：100.00，90.10【注意必须提供2位小数格式的金额，例如 100.00】
		String tradeType = getTradeType(paymentInfoBO);
		jsonObject.put("trade_type", tradeType);// 交易类型
		jsonObject.put("expire_time", paymentInfoBO.getValidOrder());// 支付有效期，单位分钟
		jsonObject.put("notify_url", paymentInfoBO.getNotifyUrl());// 支付后返回地址
		// jsonObject.put("trade_type", tradeType);// 交易商品描述 ,50个字符内 【选填】
		// jsonObject.put("trade_memo", tradeType);//交易备注

		map.put("biz_content", jsonObject.toJSONString());// 业务参数的集合，最大长度不限，除全局参数外所有请求数据都必须放在这个参数中传递，具体参照具体接口文档
		String sign = BuildRequestFormUtil.createLinkString(map, mchInfo[2], false);
		map.put("sign", sign);// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 构建wap支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年10月18日 上午11:32:11
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO, String[] mchInfo) {
		Map<String, String> map = commonParam(mchInfo);
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("mch_id", mchInfo[1]);
		jsonObject.put("ds_trade_no", paymentInfoBO.getNoOrder());// 渠道商系统付款交易号
		jsonObject.put("pay_fee", paymentInfoBO.getMoneyOrder());// 支付金额，精确到小数2位 元

		String tradeType = getTradeType(paymentInfoBO);
		jsonObject.put("trade_type", tradeType);// 交易类型
		jsonObject.put("expire_time", paymentInfoBO.getValidOrder());// 支付有效期，单位分钟
		jsonObject.put("user_ip", paymentInfoBO.getUserreqIp());// 请求IP
		jsonObject.put("notify_url", paymentInfoBO.getNotifyUrl());// 异步通知地址
		jsonObject.put("callback_url", paymentInfoBO.getUrlReturn());// 同步通知地址
		map.put("biz_content", jsonObject.toJSONString());// 业务参数的集合，最大长度不限，除全局参数外所有请求数据都必须放在这个参数中传递，具体参照具体接口文档

		String sign = BuildRequestFormUtil.createLinkString(map, HaoDianAConfig.HDA_NEW_DS_SECRET, false);
		map.put("sign", sign);// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 获取请求方式
	* WX-微信支付；AP-支付宝支付；QQ-QQ钱包支付；【注意大写WX/AP/QQ】
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月15日 下午5:44:24
	* @return: String 
	*/
	private static String getTradeType(PaymentInfoBO paymentInfoBO) {
		String tradeType = "";// WX-微信支付；AP-支付宝支付；QQ-QQ钱包支付；【注意大写WX/AP/QQ】
		if (PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			tradeType = "AP";
		} else if (PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			tradeType = "WX";
		} else if (PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey().equals(paymentInfoBO.getPayType())) {
			tradeType = "QQ";
		}
		return tradeType;
	}

	/**  
	* 方法说明: 构建支付请求结果map
	* @auth: xiongJinGang
	* @param jsonObject
	* @time: 2017年10月17日 下午6:04:34
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildResultMapParam(JSONObject jsonObject) {
		Map<String, String> map = new HashMap<String, String>();
		if (jsonObject.containsKey("status")) {
			map.put("status", jsonObject.getString("status"));// 0 成功；其它有错误发生，
		}
		if (jsonObject.containsKey("message")) {
			map.put("message", jsonObject.getString("message"));// 状态描述
		}
		if (jsonObject.containsKey("trade_no")) {
			map.put("trade_no", jsonObject.getString("trade_no"));// 好店啊系统付款交易号
		}
		if (jsonObject.containsKey("ds_trade_no")) {
			map.put("ds_trade_no", jsonObject.getString("ds_trade_no"));// 渠道商系统付款交易号
		}
		if (jsonObject.containsKey("trade_type")) {
			map.put("trade_type", jsonObject.getString("trade_type"));// 交易类型 WX WX-微信支付；AP-支付宝支付；QQ-QQ钱包支付；
		}
		if (jsonObject.containsKey("prepay_res")) {
			map.put("prepay_res", jsonObject.getString("prepay_res"));// 预支付URI，不能直接展示给客人扫码，需要通过该URI生成二维码图片后展示给客人扫码完成支付（有效期最长2小时，过期后扫码不能再发起支付）
		}
		if (jsonObject.containsKey("prepay_url")) {
			map.put("prepay_url", jsonObject.getString("prepay_url"));// 微信代理预支付URL【以代理方式转发该链接完成支付】
		}
		if (jsonObject.containsKey("prepay_qrcode")) {
			map.put("prepay_qrcode", jsonObject.getString("prepay_qrcode"));// 预支付URI对应的二维码图片，可以直接展示给客人扫码完成支付（有效期最长2小时，过期后扫码不能再发起支付）
		}
		if (jsonObject.containsKey("sign")) {
			map.put("sign", jsonObject.getString("sign"));// 服务端返回结果签名
		}
		return map;
	}

	private static Map<String, String> buildQueryResultMapParam(JSONObject jsonObject) {
		Map<String, String> map = new HashMap<String, String>();
		if (jsonObject.containsKey("status")) {
			map.put("status", jsonObject.getString("status"));// 0 成功；其它有错误发生，
		}
		if (jsonObject.containsKey("message")) {
			map.put("message", jsonObject.getString("message"));// 状态描述
		}
		// 在status=0时才有下面的内容
		if (map.get("status").equals(SUCCESS)) {
			if (jsonObject.containsKey("trade_status")) {
				// "SUCCESS—支付成功 REFUND—已操作全额退款（非实时到账，退款到账时间取决于具体银行清算规则和操作流程）PART_REFUND—已操作部分退款（非实时到账，退款到账时间取决于具体银行清算规则和操作流程） NOTPAY—未支付 CLOSED—已关闭 REVOKED—已冲正 USERPAYING—用户支付中 PAYERROR—支付失败(其他原因，如银行返回失败) "
				map.put("trade_status", jsonObject.getString("trade_status"));// 交易状态
			}
			// 在trade_status=SUCCESS【支付成功】时返回
			if (map.get("trade_status").equals("SUCCESS")) {
				if (jsonObject.containsKey("trade_no")) {
					map.put("trade_no", jsonObject.getString("trade_no"));// 好店啊系统付款交易号
				}
				if (jsonObject.containsKey("ds_trade_no")) {
					map.put("ds_trade_no", jsonObject.getString("ds_trade_no"));// 渠道商系统付款交易号
				}
				if (jsonObject.containsKey("pa_trade_no")) {
					map.put("pa_trade_no", jsonObject.getString("pa_trade_no"));// 合作方系统付款交易号
				}
				if (jsonObject.containsKey("tp_trade_no")) {
					map.put("tp_trade_no", jsonObject.getString("tp_trade_no"));// 第三方(微信/支付宝)系统付款交易号
				}
				if (jsonObject.containsKey("total_fee")) {
					map.put("total_fee", jsonObject.getString("total_fee"));// 应付金额（交易总金额），精确到小数2位
				}
				if (jsonObject.containsKey("tp_discount_fee")) {
					map.put("tp_discount_fee", jsonObject.getString("tp_discount_fee"));// 第三方(微信/支付宝)系统优惠/折扣金额，精确到小数2位
				}
				if (jsonObject.containsKey("pay_fee")) {
					map.put("pay_fee", jsonObject.getString("pay_fee"));// 实付金额（商户实际收到金额），精确到小数2位
				}
				if (jsonObject.containsKey("pay_time")) {
					map.put("pay_time", jsonObject.getString("pay_time"));// 付款完成时间
				}
				if (jsonObject.containsKey("pay_type")) {
					map.put("pay_type", jsonObject.getString("pay_type"));// 支付方式
				}
				if (jsonObject.containsKey("trade_type")) {
					map.put("trade_type", jsonObject.getString("trade_type"));// 交易类型 WX WX-微信支付；AP-支付宝支付；QQ-QQ钱包支付；
				}
				if (jsonObject.containsKey("buyer_id")) {
					map.put("buyer_id", jsonObject.getString("buyer_id"));// 付款顾客在商户下的唯一标识
				}
				if (jsonObject.containsKey("buyer_logon_id")) {
					map.put("buyer_logon_id", jsonObject.getString("buyer_logon_id"));// 付款顾客支付登录账号
				}
				if (jsonObject.containsKey("sign")) {
					map.put("sign", jsonObject.getString("sign"));// 服务端返回结果签名
				}
			}
		}
		return map;
	}

	/**  
	* 方法说明: 退款结果转成map
	* @auth: xiongJinGang
	* @param jsonObject
	* @time: 2017年10月18日 下午6:03:21
	* @return: Map<String,String> 
	*/
	private static Map<String, String> buildRefundResultMapParam(JSONObject jsonObject) {
		Map<String, String> map = new HashMap<String, String>();
		if (jsonObject.containsKey("status")) {
			map.put("status", jsonObject.getString("status"));// 0 成功；其它有错误发生，
		}
		if (jsonObject.containsKey("message")) {
			map.put("message", jsonObject.getString("message"));// 状态描述
		}
		// 在status=0时才有下面的内容
		if (map.get("status").equals(SUCCESS)) {
			if (jsonObject.containsKey("trade_no")) {
				map.put("trade_no", jsonObject.getString("trade_no"));// 好店啊系统付款交易号
			}
			if (jsonObject.containsKey("refund_no")) {
				map.put("refund_no", jsonObject.getString("refund_no"));// 好店啊系统退款交易号
			}
			if (jsonObject.containsKey("pa_refund_no")) {
				map.put("pa_refund_no", jsonObject.getString("pa_refund_no"));// 合作方系统退款交易号
			}
			if (jsonObject.containsKey("refund_channel")) {
				map.put("refund_channel", jsonObject.getString("refund_channel"));// 退款渠道
			}
			if (jsonObject.containsKey("refund_fee")) {
				map.put("refund_fee", jsonObject.getString("refund_fee"));// 退款金额，精确到小数2位
			}
			if (jsonObject.containsKey("sign")) {
				map.put("sign", jsonObject.getString("sign"));// 服务端返回结果签名
			}
		}
		return map;
	}

	/**  
	* 方法说明: 处理异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月18日 下午3:08:32
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map, String platForm) {
		try {
			if (ObjectUtil.isBlank(map)) {
				logger.info("好店啊支付异步返回结果为空");
				return ResultBO.err();
			} else {
				PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
				String status = map.get("status");
				// 在status=0【支付成功】时有效
				if (status.equals(SUCCESS)) {
					// 接口中返回的签名
					// String paramSign = map.get("sign");
					// map.remove("sign");
					// String paramStr = BuildRequestFormUtil.createLinkString(map, true);

					String rechargeChannel = null;
					String tradeType = map.get("trade_type");
					if (tradeType.equals("WX")) {
						rechargeChannel = PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey();
					} else if (tradeType.equals("AP")) {
						rechargeChannel = PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey();
					} else {
						rechargeChannel = PayConstants.PayTypeThirdEnum.QQ_PAYMENT.getKey();
					}
					String[] mchInfo = getMchInfo(rechargeChannel, platForm);
					if (!SignUtils.checkParam(map, mchInfo[2], false)) {
						logger.info("好店啊异步回调签名验证不过");
						// return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					}
					// String sign = BuildRequestFormUtil.addSign(paramStr, mchInfo[2], false);
					// if (!paramSign.equalsIgnoreCase(sign)) {
					// logger.info("好店啊异步回调签名验证不过，原始签名：" + paramSign + "，生成签名：" + sign);
					// return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					// }

					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
					payNotifyResult.setResponse("success");// 商户系统接收并处理理回调通知后，直接返回0值表示处理理成功
					payNotifyResult.setOrderCode(map.get("ds_trade_no"));// 商户唯一订单号
					payNotifyResult.setThirdTradeNo(map.get("trade_no"));// 交易号
					payNotifyResult.setOrderAmt(Double.parseDouble(map.get("pay_fee")));// 应付金额（交易总金额），精确到小数2位
					String tradeTime = map.get("pay_time");// 付款完成时间 YYYY-MM-DD HH:MM:SS
					if (ObjectUtil.isBlank(tradeTime)) {
						tradeTime = DateUtil.getNow(DateUtil.DEFAULT_FORMAT);
					}
					payNotifyResult.setTradeTime(tradeTime);// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
					if (map.containsKey("buyer_logon_id")) {
						payNotifyResult.setPayAccount(map.get("buyer_logon_id"));// 支付宝买家账号
					}
					if (map.containsKey("pay_type")) {
						payNotifyResult.setPayType(map.get("pay_type"));// BARCODE_PAY：条码支付；JS_PAY：公众号支付；QRCODE_PAY：扫码支付；WAP_PAY：WAP(H5)支付；APP_PAY：APP支付；
					}
					if (map.containsKey("trade_type")) {
						payNotifyResult.setTradeType(map.get("trade_type"));// 交易类型 WX-微信支付；AP-支付宝支付；QQ-QQ钱包支付；
					}
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				return ResultBO.ok(payNotifyResult);
			}
		} catch (Exception e) {
			logger.error("处理好店啊支付异步返回结果异常！", e);
			return ResultBO.err();
		}
	}

	/**  
	* 方法说明: 构建查询请求参数
	* @auth: xiongJinGang
	* @param payQueryParamVO
	* @time: 2017年10月18日 下午3:13:21
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildQueryMapParam(PayQueryParamVO payQueryParamVO, String[] mchInfo) {
		Map<String, String> map = commonParam(mchInfo);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put("mch_id", mchInfo[1]);
		if (!ObjectUtil.isBlank(payQueryParamVO.getTransCode())) {
			jsonObject.put("ds_trade_no", payQueryParamVO.getTransCode());// 渠道商系统付款交易号
		}
		if (!ObjectUtil.isBlank(payQueryParamVO.getTradeNo())) {
			jsonObject.put("trade_no", payQueryParamVO.getTradeNo());// 好店啊系统付款交易号
		}
		map.put("biz_content", jsonObject.toJSONString());// 业务参数的集合，最大长度不限，除全局参数外所有请求数据都必须放在这个参数中传递，具体参照具体接口文档
		String sign = BuildRequestFormUtil.createLinkString(map, mchInfo[2], false);
		map.put("sign", sign);// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 构建退款请求参数
	* @auth: xiongJinGang
	* @param refundParam
	* @time: 2017年10月18日 下午5:47:32
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildRefundMapParam(RefundParamVO refundParam, String[] mchInfo) {
		Map<String, String> map = commonParam(mchInfo);
		if (!ObjectUtil.isBlank(refundParam.getTransCode())) {
			map.put("ds_trade_no", refundParam.getTransCode());// 渠道商系统付款交易号
		}
		if (!ObjectUtil.isBlank(refundParam.getTradeNo())) {
			map.put("trade_no", refundParam.getTradeNo());// 好店啊系统付款交易号
		}
		map.put("refund_fee", refundParam.getRefundAmount() + "");// 退款金额
		String sign = BuildRequestFormUtil.createLinkString(map, mchInfo[2], false);
		map.put("sign", sign);// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 查询支付结果
	* @auth: xiongJinGang
	* @param payQueryParam
	* @time: 2017年10月18日 下午5:43:26
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParam, String platForm) {
		logger.debug("好店啊支付查询，请求参数：" + payQueryParam.toString());
		try {
			// 构建扫码请求参数
			String[] mchInfo = getMchInfo(payQueryParam.getRechargeChannel(), platForm);
			Map<String, String> map = HaoDianAUtil.buildQueryMapParam(payQueryParam, mchInfo);
			logger.info("好店啊查询请求参数：" + map.toString());
			// 调用接口
			String resultJson = HttpUtil.doPost(HaoDianAConfig.HDA_NEW_PAY_URL + "pay/tradequery", map);
			logger.info("查询好店啊【" + payQueryParam.getTransCode() + "】交易结果返回：" + resultJson);
			PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
			if (!ObjectUtil.isBlank(resultJson)) {
				JSONObject jsonObject = JSON.parseObject(resultJson);
				// status=0 并且trade_status为success。表示查询成功，支付成功
				if (jsonObject.containsKey("status") && jsonObject.getString("status").equals(HaoDianAUtil.SUCCESS)) {
					String tradeStatus = jsonObject.getString("trade_status");
					PayStatusEnum payStatusEnum = BuildRequestFormUtil.getPayStatus(tradeStatus);
					payQueryResultVO.setTradeStatus(payStatusEnum);
					// 支付成功的
					if (tradeStatus.equals("SUCCESS")) {
						if (jsonObject.containsKey("sign")) {
							// 查询结果拼装成map
							Map<String, String> resultMap = HaoDianAUtil.buildQueryResultMapParam(jsonObject);
							if (!SignUtils.checkParam(resultMap, mchInfo[2], false)) {
								logger.error("订单【" + payQueryParam.getTransCode() + "】请求返回签名验证不过");
								return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
							} else {
								payQueryResultVO.setTotalAmount(resultMap.get("pay_fee"));
								payQueryResultVO.setTradeNo(resultMap.get("trade_no"));
								String tradeTime = map.get("pay_time");// 付款完成时间 YYYY-MM-DD HH:MM:SS
								if (ObjectUtil.isBlank(tradeTime)) {
									tradeTime = DateUtil.getNow(DateUtil.DEFAULT_FORMAT);
								}
								payQueryResultVO.setArriveTime(tradeTime);
								payQueryResultVO.setOrderCode(resultMap.get("ds_trade_no"));
								return ResultBO.ok(payQueryResultVO);
							}
						}
						return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
					} else {
						TransRechargeBO transRechargeBO = payQueryParam.getTransRechargeBO();
						payQueryResultVO.setTotalAmount(String.valueOf(transRechargeBO.getRechargeAmount()));
						payQueryResultVO.setOrderCode(transRechargeBO.getTransRechargeCode());
						return ResultBO.ok(payQueryResultVO);
					}
				}
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	/**  
	* 方法说明: 订单退款
	* @auth: xiongJinGang
	* @param refundParam
	* @time: 2017年10月18日 下午6:09:15
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> orderRefund(RefundParamVO refundParam, String platForm) {
		logger.debug("好店啊退款，请求参数：" + refundParam.toString());
		try {
			// 构建退款请求参数
			String[] mchInfo = getMchInfo(refundParam.getRechargeChannel(), platForm);
			Map<String, String> map = HaoDianAUtil.buildRefundMapParam(refundParam, mchInfo);
			logger.info("好店啊退款请求参数：" + map.toString());
			// 调用接口
			String resultJson = HttpUtil.doPost(HaoDianAConfig.HDA_NEW_PAY_URL + "refund", map);
			logger.info("好店啊【" + refundParam.getTransCode() + "】退款返回：" + resultJson);

			RefundResultVO refundResultVO = new RefundResultVO();
			if (!ObjectUtil.isBlank(resultJson)) {
				JSONObject jsonObject = JSON.parseObject(resultJson);
				// status=0 退款成功
				if (jsonObject.containsKey("status") && jsonObject.getString("status").equals(SUCCESS)) {
					if (jsonObject.containsKey("sign")) {
						// 退款结果拼装成map
						Map<String, String> resultMap = HaoDianAUtil.buildRefundResultMapParam(jsonObject);
						if (!SignUtils.checkParam(resultMap, mchInfo[2], false)) {
							logger.error("订单【" + refundParam.getTransCode() + "】请求返回签名验证不过");
							return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
						} else {
							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setResultMsg(jsonObject.getString("message"));
							refundResultVO.setOrderCode(resultMap.get("pa_refund_no"));// 商户订单号
							refundResultVO.setRefundCode(resultMap.get("refund_no"));// 商户退款单号
							refundResultVO.setTransactionId(resultMap.get("trade_no"));// 平台订单号
							// refundResultVO.setRefundId(resultMap.get("refund_id"));// 平台退款单号
							refundResultVO.setRefundChannel(resultMap.get("refund_channel"));// 退款渠道 ORIGINAL—原路退款，默认
							refundResultVO.setRefundAmount(resultMap.get("refund_fee"));// 退款总金额,单位为分,可以做部分退款
							refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_SUCCESS);
							return ResultBO.ok(refundResultVO);
						}
					}
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				} else {
					String errorMessage = "退款失败";
					if (jsonObject.containsKey("message")) {
						errorMessage = jsonObject.getString("message");
					}
					return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
				}
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("好店啊退款异常", e);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	public static ResultBO<?> refundQuery(PayQueryParamVO payQueryParam, String platForm) {
		logger.debug("好店啊退款查询，请求参数：" + payQueryParam.toString());
		try {
			// 构建退款查询请求参数
			String[] mchInfo = getMchInfo(payQueryParam.getRechargeChannel(), platForm);
			Map<String, String> map = HaoDianAUtil.buildQueryMapParam(payQueryParam, mchInfo);
			logger.info("好店啊退款查询请求参数：" + map.toString());
			// 调用接口
			String resultJson = HttpUtil.doPost(HaoDianAConfig.HDA_NEW_PAY_URL + "refundquery", map);
			logger.info("好店啊【" + payQueryParam.getTransCode() + "】退款查询返回：" + resultJson);

			RefundResultVO refundResultVO = new RefundResultVO();
			if (!ObjectUtil.isBlank(resultJson)) {
				JSONObject jsonObject = JSON.parseObject(resultJson);
				// status=0 退款查询成功
				if (jsonObject.containsKey("status") && jsonObject.getString("status").equals(SUCCESS)) {
					if (jsonObject.containsKey("sign")) {
						// 退款查询结果拼装成map
						Map<String, String> resultMap = HaoDianAUtil.buildRefundResultMapParam(jsonObject);
						if (!SignUtils.checkParam(resultMap, mchInfo[2], false)) {
							logger.error("订单【" + payQueryParam.getTransCode() + "】请求返回签名验证不过");
							return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
						} else {
							String refundCount = resultMap.get("refund_count");// 退款笔数
							if (ObjectUtil.isBlank(refundCount)) {
								logger.info("查询【" + payQueryParam.getTransCode() + "】记录失败，退款笔数" + refundCount + "错误：");
								return ResultBO.err();
							}

							Double totalRefundAmount = 0d;
							Integer refundCountInt = Integer.parseInt(refundCount);
							for (int i = 0; i < refundCountInt; i++) {
								String orderRefundNo = resultMap.get("refund_no_" + i);// 好店啊系统退款交易号
								String refundId = resultMap.get("pa_refund_no_" + i);// 合作方系统退款交易号
								String refundChannel = resultMap.get("refund_channel_" + i);// 退款渠道
								String refundFee = resultMap.get("refund_fee_" + i);// 退款金额，精确到小数2位
								// "SUCCESS—退款成功FAIL—退款失败PROCESSING—退款处理中NOTSURE—未确定， 需要商户原退款单号重新发起CHANGE—转入代发，退款到银行发现用户的卡作废或者冻结了，导致原路退款银行卡失败，资金回流到商户的现金帐号，需要商户人工干预，通过线下或者银行转账的方式进行退款。"
								String refundStatus = resultMap.get("refund_status_" + i);// 退款过程状态
								// String refundTime = resultMap.get("refund_time_" + i);//退款时间
								logger.info("【" + payQueryParam.getTransCode() + "】第" + (i + 1) + "条退款记录：orderRefundNo=" + orderRefundNo + "，refundChannel=" + refundChannel + "，refundId=" + refundId + "，refundFee=" + refundFee + "，refundStatus="
										+ refundStatus);
								Double refundFeeDou = Double.valueOf(refundFee);
								totalRefundAmount = MathUtil.add(totalRefundAmount, refundFeeDou);
							}

							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setOrderCode(resultMap.get("ds_trade_no"));// 商户订单号
							refundResultVO.setTransactionId(resultMap.get("trade_no"));// 平台订单号
							refundResultVO.setResultCode(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
							refundResultVO.setResultMsg("退款成功");
							refundResultVO.setRefundAmount(String.valueOf(totalRefundAmount));// 退款总金额,单位为分,可以做部分退款
							refundResultVO.setRefundStatusEnum(PayConstants.RefundStatusEnum.REFUND_SUCCESS);
							return ResultBO.ok(refundResultVO);
						}
					}
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				} else {
					String errorMessage = "退款失败";
					if (jsonObject.containsKey("message")) {
						errorMessage = jsonObject.getString("message");
					}
					return ResultBO.err(MessageCodeConstants.VALIDATE_PARAM_ERROR_FIELD, errorMessage);
				}
			} else {
				return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
			}
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("好店啊退款异常", e);
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}
}
