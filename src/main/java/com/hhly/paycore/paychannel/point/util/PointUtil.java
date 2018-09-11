package com.hhly.paycore.paychannel.point.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.point.config.PointConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;

/**
 * @desc 指点支付工具类
 * @author xiongJinGang
 * @date 2018年6月13日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PointUtil {
	private static Logger logger = Logger.getLogger(PointUtil.class);
	public static final String SUCCESS = "0";// 成功标志

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2018年6月13日 上午10:19:14
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWapMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("amount", GetWeChatUtil.getMoney(paymentInfoBO.getMoneyOrder()));// 金额 分
		map.put("backurl", paymentInfoBO.getUrlReturn());// 同步地址
		map.put("notifyurl", paymentInfoBO.getNotifyUrl());// 异步地址
		map.put("out_trade_no", paymentInfoBO.getNoOrder());// 订单号
		map.put("mchid", PointConfig.POINT_PAYMENT_MERCHANT_CODE);// 商户号
		map.put("good", paymentInfoBO.getNameGoods());// 商户名
		map.put("ip", paymentInfoBO.getUserreqIp());//
		map.put("type", "aliwap");// 支付场景代码/在WAP页面发起支付宝支付
		map.put("msg", "2ncai");//

		StringBuffer params = new StringBuffer();
		params.append("amount=").append(map.get("amount"));
		params.append("&backurl=").append(map.get("backurl"));
		params.append("&msg=").append("2ncai");
		params.append("&out_trade_no=").append(map.get("out_trade_no"));
		params.append("&mchid=").append(map.get("mchid"));
		params.append("&notifyurl=").append(map.get("notifyurl"));
		params.append("&good=").append(map.get("good"));
		params.append("&type=").append(map.get("type"));
		params.append("&key=").append(PointConfig.POINT_PAYMENT_KEY);
		logger.info("指点支付待签名参数：" + params.toString());

		StringBuffer sb = new StringBuffer();
		sb.append("amount=").append(map.get("amount"));
		sb.append("&mchid=").append(map.get("mchid"));
		sb.append("&type=").append(map.get("type"));
		String sign = DigestUtils.md5Hex(sb.toString() + "&key=" + PointConfig.POINT_PAYMENT_KEY);

		map.put("sign", sign);// MD5签名结果
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
			logger.info("指点支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("sign")) {
			// md5(amount=&out_trade_no=&order=&time=&trade=&key=key)
			String needMd5Str = new StringBuffer("amount=").append(map.get("amount")).append("&out_trade_no=").append(map.get("out_trade_no")).append("&order=").append(map.get("order")).append("&time=").append(map.get("time")).append("&trade=")
					.append(map.get("trade")).append("&").append("key=").append(PointConfig.POINT_PAYMENT_KEY).toString();

			String md5Sign = DigestUtils.md5Hex(needMd5Str);
			if (!map.get("sign").equals(md5Sign)) {
				logger.info("指点支付异步通知，验证签名不通过，待签名串：" + needMd5Str);
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			// 状态码88：成功 其它为失败
			payNotifyResult.setOrderCode(map.get("out_trade_no"));// 商户唯一订单号
			payNotifyResult.setThirdTradeNo(map.get("order"));// 平台订单号
			payNotifyResult.setOrderAmt(Double.valueOf(GetWeChatUtil.changeF2Y(map.get("amount"))));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
			payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
			payNotifyResult.setResponse("SUCCESS");
			payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			return ResultBO.ok(payNotifyResult);
		}
		logger.error("指点支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		logger.info(payQueryParamVO.getTransCode() + "超时关闭（指点支付没有查询接口，默认都返回支付超时）");
		return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
	}
}
