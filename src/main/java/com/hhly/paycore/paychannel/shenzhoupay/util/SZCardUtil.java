package com.hhly.paycore.paychannel.shenzhoupay.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.paycore.paychannel.shenzhoupay.config.DivineConfig;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.utils.BuildRequestFormUtil;

/**
 * @desc 神州充值卡支付类
 * @author xiongJinGang
 * @date 2017年10月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SZCardUtil {

	private static final Logger logger = LoggerFactory.getLogger(SZCardUtil.class);

	/**  
	* 方法说明: 创建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfo
	* @time: 2017年10月12日 下午12:04:46
	* @return: Map<String,String> 
	*/
	public static ResultBO<?> toPay(PaymentInfoBO paymentInfo, String url) {
		PayReqResultVO payReqResult = new PayReqResultVO();
		Map<String, String> map = new HashMap<String, String>();
		map.put("version", "3");// 版本号值为： 3
		map.put("merId", DivineConfig.SHENZHOU_MER_ID);
		map.put("payMoney", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
		map.put("orderId", paymentInfo.getNoOrder());
		map.put("serverReturnUrl", paymentInfo.getNotifyUrl());// 商户后台通知URL
		map.put("pageReturnUrl", paymentInfo.getUrlReturn());// 商户前台通知URL

		map.put("merUserName", paymentInfo.getAcctName());// 商户用户的用户名
		map.put("itemName", ObjectUtil.isBlank(paymentInfo.getNameGoods()) ? GetWeChatUtil.getRandomStr(32) : paymentInfo.getNameGoods());
		map.put("itemDesc", paymentInfo.getInfoOrder());
		map.put("privateField", paymentInfo.getAttach()); // 商户私有数据
		map.put("verifyType", "1"); // 数据校验方式 固定传 1
		map.put("returnType", "3"); // 默认：页面和服务器返回1：页面返回2：服务器返回3：页面和服务器返回
		map.put("isDebug", "0"); // 是否调试 固定传 0

		// 下面二个参数是移动端用
		// map.put("gatewayId", "0"); //0：充值卡
		// map.put("cardTypeCombine", "0"); //cardTypeCombine=0：移动充值卡1:联通充值卡，2：电信充值卡

		StringBuffer sb = new StringBuffer();
		sb.append(map.get("version")).append("|").append(map.get("merId")).append("|").append(map.get("payMoney")).append("|").append(map.get("orderId")).append("|").append(map.get("pageReturnUrl")).append("|").append(map.get("serverReturnUrl"))
				.append("|").append(map.get("privateField")).append("|").append(DivineConfig.SHENZHOU_PRIVATE_KEY).append("|").append(map.get("verifyType")).append("|").append(map.get("returnType")).append("|").append(map.get("isDebug"));

		// version+|+merId+|+payMoney+|+orderId+|+pageReturnUrl+|+serverReturnUrl+|+privateField+|+privateKey+|+verifyType+|+returnType+|+isDebug
		String md5String = DigestUtils.md5Hex(sb.toString());
		map.put("md5String", md5String); //

		try {
			String formLink = BuildRequestFormUtil.buildRequest(map, url);
			logger.info("神州支付请求返回结果:" + formLink);
			payReqResult.setFormLink(formLink);
			payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
			payReqResult.setTradeChannel(PayConstants.PayChannelEnum.DIVINEPAY_RECHARGE.getKey());
			return ResultBO.ok(payReqResult);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ResultBO.err(MessageCodeConstants.THIRD_PARTY_PAYMENT_RETURN_EMPTY);
	}

	/**  
	* 方法说明: 解析充值卡支付返回结果
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年10月12日 下午2:59:17
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> anaylizePayNotify(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			String version = map.get("version");// 版本号值为： 3
			String merId = map.get("merId");// 商户在神州付的唯一身份标识
			String payMoney = map.get("payMoney");// 订单金额 分
			String orderId = map.get("orderId");// 订单号
			String payResult = map.get("payResult");// 订单结果 1：成功 0：失败
			String privateField = map.get("privateField");// 商户私有数据
			String md5String = map.get("md5String");// MD5 校验串
			// String signString = map.get("signString");// 神州付系统对 md5 加密后的32 位字符串(md5String)进行签名。请联系神州付技术获取神州付公钥，进行校验。
			String payDetails = map.get("payDetails");// 保留字段
			if (ObjectUtil.isBlank(payDetails)) {
				payDetails = "";
			}

			String sign = DigestUtils.md5Hex(version + merId + payMoney + orderId + payResult + privateField + payDetails + DivineConfig.SHENZHOU_PRIVATE_KEY);
			if (!md5String.equals(sign)) {
				logger.error("神州充值卡支付异步通知结果：签名错误，签名结果：" + sign + ",传入签名:" + md5String);
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			if (payResult.equals("1")) {// 支付状态，1支付成功
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
			payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(payMoney)));
			payNotifyResult.setOrderCode(orderId);
			payNotifyResult.setThirdTradeNo(orderId);
			payNotifyResult.setTradeTime(DateUtil.convertDateToStr(new Date(), DateUtil.DATE_FORMAT_NUM));
			payNotifyResult.setResponse(orderId);// 收到服务器返回通知后要输出订单号表示商户已经收到回调
		} catch (Exception e) {
			logger.error("处理充值卡充值异步返回异常", e);
			return ResultBO.err();
		}
		return ResultBO.ok(payNotifyResult);
	}
}
