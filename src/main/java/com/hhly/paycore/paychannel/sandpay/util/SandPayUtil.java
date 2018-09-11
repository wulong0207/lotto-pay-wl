package com.hhly.paycore.paychannel.sandpay.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.sandpay.config.SandPayConfig;
import com.hhly.paycore.paychannel.sandpay.rsp.SandPayBody;
import com.hhly.paycore.paychannel.sandpay.rsp.SandPayHead;
import com.hhly.paycore.paychannel.sandpay.rsp.SandPayRsp;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.paycore.paychannel.yeepay.utils.HttpClient4Utils;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 六度支付工具类
 * @author xiongJinGang
 * @date 2018年6月26日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SandPayUtil {
	private static Logger logger = Logger.getLogger(SandPayUtil.class);
	public static final String SUCCESS = "0";// 成功标志

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年6月26日 下午4:00:31
	* @return: Map<String,String> 
	*/
	public static String buildWapMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("mid", SandPayConfig.SAND_PAY_MERCHANT_CODE);// 商户号
		map.put("userId", paymentInfoBO.getUserIdInt() + "");// 用户id
		map.put("orderCode", paymentInfoBO.getNoOrder());// 订单号

		String money = GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder());
		map.put("totalAmount", StringUtil.autoGenericCode(money, 12));// 金额
		map.put("subject", paymentInfoBO.getNameGoods());// 订单标题
		map.put("body", paymentInfoBO.getNameGoods());// 订单内容

		map.put("frontUrl", paymentInfoBO.getUrlReturn());// 同步地址
		map.put("notifyUrl", paymentInfoBO.getNotifyUrl());// 异步地址

		Map<String, String> sortMap = sortMapByKey(map);
		String needSign = JSON.toJSONString(sortMap);
		needSign = needSign.substring(0, needSign.length() - 1);
		needSign += ",\"key\":\"" + SandPayConfig.SAND_PAY_PAYMENT_KEY + "\"}";
		String signAfter = DigestUtils.md5Hex(needSign).toUpperCase();
		logger.info("六度支付待签名字符串：" + needSign + "，签名后结果：" + signAfter);
		map.put("sign", signAfter);// MD5签名结果

		return BuildRequestFormUtil.createLinkString(map);
	}

	/**  
	* 方法说明: 构建QQ支付请参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年7月5日 下午3:52:40
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildQQParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("fee", String.valueOf(GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder())));// 金额分
		map.put("order_sn", paymentInfoBO.getNoOrder());// 订单号
		map.put("create_ip", paymentInfoBO.getUserreqIp());// ip地址
		map.put("mchid", SandPayConfig.SAND_PAY_QQ_MERCHANT_CODE);// 商户号
		map.put("notify_url", paymentInfoBO.getNotifyUrl());// 异步地址

		String needSign = BuildRequestFormUtil.sortMapAndCreateStr(map);
		needSign += "&" + SandPayConfig.SAND_PAY_QQ_PAYMENT_KEY;
		String signAfter = DigestUtils.md5Hex(needSign);
		logger.info("六度QQ支付待签名字符串：" + needSign + "，签名后结果：" + signAfter);
		map.put("sign", signAfter);// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 解析并验证异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2018年1月3日 上午10:29:30
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("六度支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			String sign = map.get("sign");
			map.remove("sign");

			// 回调参数包含这二个，表示是QQ支付回调的
			if (map.containsKey("out_trade_no") && map.containsKey("result_code")) {
				String needSign = BuildRequestFormUtil.sortMapAndCreateStr(map);
				needSign += "&" + SandPayConfig.SAND_PAY_QQ_PAYMENT_KEY;
				String signAfter = DigestUtils.md5Hex(needSign);
				if (!signAfter.equals(sign)) {
					logger.info("六度QQ支付回调待签名串：" + needSign + "，签名后结果：" + signAfter);
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}
				payNotifyResult.setOrderCode(map.get("out_trade_no"));// 商户唯一订单号
				payNotifyResult.setThirdTradeNo(map.get("transaction_id"));// 平台订单号
				payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(map.get("fee"))));// 资费（分）
				payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
				payNotifyResult.setResponse("success");
				if (map.get("result_code").equals("0")) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				return ResultBO.ok(payNotifyResult);
			} else {
				Map<String, String> sortMap = sortMapByKey(map);
				String needSign = JSON.toJSONString(sortMap);
				needSign = needSign.substring(0, needSign.length() - 1);
				needSign += ",\"key\":\"" + SandPayConfig.SAND_PAY_PAYMENT_KEY + "\"}";
				String signAfter = DigestUtils.md5Hex(needSign).toUpperCase();
				if (!sign.equals(signAfter)) {
					logger.info("六度支付异步通知，验证签名不通过，待签名串：" + signAfter);
					return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
				}

				payNotifyResult.setOrderCode(map.get("orderCode"));// 商户唯一订单号
				payNotifyResult.setThirdTradeNo(map.get("tradeNo"));// 平台订单号
				payNotifyResult.setOrderAmt(MathUtil.div(Integer.parseInt(map.get("totalAmount")), 100));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
				payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
				payNotifyResult.setResponse("SUCCESS");
				if (map.get("orderStatus").equals("1")) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}
				return ResultBO.ok(payNotifyResult);
			}
		}
		logger.error("六度支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		try {

			TransRechargeBO transRechargeBO = payQueryParamVO.getTransRechargeBO();
			// QQ支付的查询
			if (transRechargeBO.getPayType().equals(PayConstants.PayTypeEnum.QQ_PAYMENT.getKey())) {
				// 不可理喻，查个结果，加一大堆参数，参数要与下单参数一致，目前没有存储异步通知地址和用户的IP，要改结构，先不处理这个查询，直接返回超时
				/*Map<String, String> map = new ConcurrentHashMap<String, String>();
				map.put("fee", String.valueOf(GetWeChatUtil.getMoney(transRechargeBO.getMoneyOrder())));// 金额分
				map.put("order_sn", transRechargeBO.getTransRechargeCode());// 订单号
				map.put("create_ip", paymentInfoBO.getUserreqIp());// ip地址
				map.put("mchid", SandPayConfig.SAND_PAY_QQ_MERCHANT_CODE);// 商户号
				map.put("notify_url", paymentInfoBO.getNotifyUrl());// 异步地址
				
				String needSign = BuildRequestFormUtil.sortMapAndCreateStr(map);
				needSign += "&" + SandPayConfig.SAND_PAY_QQ_PAYMENT_KEY;
				String signAfter = DigestUtils.md5Hex(needSign);
				logger.info("六度QQ支付待签名字符串：" + needSign + "，签名后结果：" + signAfter);
				map.put("sign", signAfter);// MD5签名结果
				*/
				logger.info("六度QQ支付充值编号【" + transRechargeBO.getTransRechargeCode() + "】不查询支付结果，直接设置支付超时关闭");
				return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
			} else {
				String needSign = "{\"mid\":\"" + SandPayConfig.SAND_PAY_MERCHANT_CODE + "\",\"orderCode\":\"" + payQueryParamVO.getTransCode() + "\",\"key\":\"" + SandPayConfig.SAND_PAY_PAYMENT_KEY + "\"}";
				String signAfter = DigestUtils.md5Hex(needSign).toUpperCase();
				String url = SandPayConfig.SAND_PAY_PAYMENT_URL + "query.php?mid=" + SandPayConfig.SAND_PAY_MERCHANT_CODE + "&orderCode=" + payQueryParamVO.getTransCode() + "&sign=" + signAfter;
				String result = HttpClient4Utils.httpGet(url, null, "UTF-8", 20000);
				logger.info("获取订单【" + payQueryParamVO.getTransCode() + "】详情返回：" + result);
				if (StringUtil.isBlank(result)) {
					logger.info("获取【" + payQueryParamVO.getTransCode() + "】的支付结果返回空");
					return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
				}
				List<SandPayRsp> list = JSON.parseArray(result, SandPayRsp.class);
				SandPayRsp sandPayRsp = list.get(0);
				SandPayHead head = sandPayRsp.getHead();
				SandPayBody body = sandPayRsp.getBody();
				if (!ObjectUtil.isBlank(head) && head.getRespCode().equals("0")) {
					if (!ObjectUtil.isBlank(body)) {
						if (body.getOrderStatus().equals("1")) {
							PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
							payQueryResultVO.setTotalAmount(MathUtil.div(Integer.parseInt(body.getTotalAmount()), 100) + "");
							payQueryResultVO.setTradeNo(body.getOriTradeNo());
							payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
							payQueryResultVO.setOrderCode(body.getOriOrderCode());
							if (body.getOrderStatus().equals("1")) {
								payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
							} else {
								payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_FAILURE);
							}
							return ResultBO.ok(payQueryResultVO);
						} else {
							return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
	}

	/**
	 * 使用 Map按key进行排序
	 * @param map
	 * @return
	 */
	public static Map<String, String> sortMapByKey(Map<String, String> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}

		Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());

		sortMap.putAll(map);

		return sortMap;
	}
}

class MapKeyComparator implements Comparator<String> {

	@Override
	public int compare(String str1, String str2) {

		return str1.compareTo(str2);
	}
}
