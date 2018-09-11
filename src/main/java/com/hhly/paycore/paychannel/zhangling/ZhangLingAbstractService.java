package com.hhly.paycore.paychannel.zhangling;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.shenzhoupay.util.RSAUtil;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.exception.ServiceRuntimeException;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @desc  掌灵抽象支付
 * @author TonyOne
 * @date 2018年4月24日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public abstract class ZhangLingAbstractService extends PayAbstract {

	private static final Logger logger = Logger.getLogger(ZhangLingAbstractService.class);

	private static final String SUCCESS_CODE = "200";

	public ResultBO<?> pay(PaymentInfoBO paymentInfo)  {
		PayConstants.PayTypeThirdEnum payType = PayConstants.PayTypeThirdEnum.getEnum(paymentInfo.getPayType());
		logger.info("掌灵【" + payType.getValue() + "】支付请求参数：" + paymentInfo);
		// 调用接口
		return invokePay(buildPayParam(paymentInfo));
	}

	protected abstract ResultBO<?> invokePay(Map<String, String> paramMap);

	protected abstract String getPayUrl();

	protected abstract String getPlatform();

	/**
	 * 方法说明: 掌灵支付请求参数
	 * @auth: xiongJinGang
	 * @param paymentInfo
	 * @time: 2017年11月23日 下午2:43:36
	 * @return: Map<String,String>
	 */
	private Map<String, String> buildPayParam(PaymentInfoBO paymentInfo){
		Map<String,Object> map=new HashMap<String, Object>();
		// 订单总金额
		map.put("amount", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
		map.put("appid",ZhangLingConfig.ZHANGLING_PAY_PARTNER_CODE);
		// 订单描述
		map.put("body",paymentInfo.getInfoOrder());
		map.put("clientIp",paymentInfo.getUserreqIp());
		// 商户订单号
		map.put("mchntOrderNo", paymentInfo.getNoOrder());
		map.put("notifyUrl",paymentInfo.getNotifyUrl());
		// 选择网关支付，该字段必填
		map.put("returnUrl",paymentInfo.getUrlReturn());
		// 商品名称,商户自定义订单标题
		map.put("subject",paymentInfo.getNameGoods());
		// 接口版本号
		map.put("version",getVersion(paymentInfo.getPayPlatform()));
		// 附加数据
		map.put("extra", paymentInfo.getAttach());
		// 订单有效时长,默认 24 小时过期；单位：毫秒数
		map.put("expireMs", Long.parseLong(paymentInfo.getValidOrder())*60*1000);
		/**
		 * 微信扫码支付 2100000001
		 * 微信wap  2000000001
		 * 网关支付 2000000002
		 * 微信wap原生支付  2000000006
		 */
		/**
		 * 微信wap支付  0000000007
		 */
		map.put("payChannelId",getZhangLingPayChannelId(paymentInfo.getPayType()));
		String sign=doEncrypt(map, ZhangLingConfig.ZHANGLING_PAY_KEY);
		map.put("signature", sign);
		String  jsonObject= JSON.toJSONString(map);
		logger.info("加密前reqParams="+jsonObject);
		//加密
		String reqParams=null;
		try {
			reqParams = Base64.encodeBase64String(RSAUtil.encryptByPublicKeyByPKCS1Padding(jsonObject.getBytes("utf-8"), ZhangLingConfig.ZHANGLING_PUBLIC_KEY));
			logger.info("加密后reqParams="+reqParams);
		} catch (Exception e) {
			logger.error(e);
			throw new ServiceRuntimeException(e.getMessage());
		}
		// 掌灵支付要求把得到的密文值赋给orderInfo参数，通过页面form表单post请求，完成预下单
		Map<String,String> retMap = new HashMap<>();
		retMap.put("orderInfo",reqParams);
		return retMap;
	}

	/**
	 *  如果由服务端接入H5支付，客户端只负责显示服务端页面，version字段为"h5_NoEncrypt"
	 *  如果由客户端(android,iOS)接入H5支付，则version字段应为"h5_NoEncrypt_js"
	 * @param payPlatform
	 * @return
	 */
	protected abstract String getVersion(Short payPlatform);

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO vo) {
		//请求接口地址：http://trans.palmf.cn/sdk/v1.0/payOrderResult/参数1/参数2
		String url = String.format("http://trans.palmf.cn/sdk/v1.0/payOrderResult/%s/%s", vo.getTransCode(),ZhangLingConfig.ZHANGLING_PAY_PARTNER_CODE);
		String result = null;
		try {
			result = HttpUtil.doPost(url);
		} catch (Exception e) {
			logger.error(e);
		}
		logger.info("掌灵查询支付结果返回："+result);
		HashMap<String,Object> map = (HashMap) JSON.parseObject(result, HashMap.class);
		String returnCode = map.get("returnCode").toString();
		if(SUCCESS_CODE.equals(returnCode)) {
			String upSignature=(String)map.get("signature");
			String downSignature=doEncrypt(map, ZhangLingConfig.ZHANGLING_PAY_KEY);
			if(!downSignature.equals(upSignature)) {
				logger.error("掌灵支付查询结果校验不通过："+map);
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
			payQueryResultVO.setTotalAmount(GetWeChatUtil.changeF2Y(map.get("amount").toString()));
			payQueryResultVO.setTradeNo(map.get("orderNo").toString());
			payQueryResultVO.setArriveTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));
			payQueryResultVO.setOrderCode(map.get("mchntOrderNo").toString());
			payQueryResultVO.setTradeStatus(getPayStatus(map.get("paySt").toString()));
			return ResultBO.ok(payQueryResultVO);
		} else {
			logger.info("掌灵查询支付结果返回的状态码异常");
			return ResultBO.err();
		}
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		// 参数map是由com.hhly.lotto.api.data.pay.v1_0.PayCommon.readReqStream()返回的
		String zhangLingNotify = map.get(Constants.PAY_RESPONSE_KEY_NAME);
		HashMap<String,Object> zhangLingNotifyMap = (HashMap)JSON.parseObject(zhangLingNotify, HashMap.class);
		String upSignature=(String)zhangLingNotifyMap.get("signature");
		String downSignature=doEncrypt(zhangLingNotifyMap, ZhangLingConfig.ZHANGLING_PAY_KEY);
		if(!downSignature.equals(upSignature)) {
			logger.error("掌灵支付异步通知结果校验不通过："+zhangLingNotifyMap);
			return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		//paySt 0:待支付;1:支付中;2:支付成功;3:支付失败;4:已关闭
		String paySt = zhangLingNotifyMap.get("paySt").toString();
		payNotifyResult.setStatus(getPayStatus(paySt));
		payNotifyResult.setOrderCode(zhangLingNotifyMap.get("mchntOrderNo").toString());// 商户唯一订单号
		payNotifyResult.setThirdTradeNo(zhangLingNotifyMap.get("orderNo").toString());// 平台订单号
		payNotifyResult.setOrderAmt(Double.parseDouble(GetWeChatUtil.changeF2Y(zhangLingNotifyMap.get("amount").toString())));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
		payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
		payNotifyResult.setResponse("{\"success\":\"true\"}");
		return ResultBO.ok(payNotifyResult);
	}

	private PayConstants.PayStatusEnum getPayStatus(String paySt) {
		PayConstants.PayStatusEnum payStatus;
		switch (paySt) {
			case "0":
				payStatus = PayConstants.PayStatusEnum.WAITTING_PAYMENT;
				break;
			case "1":
				payStatus = PayConstants.PayStatusEnum.BEING_PAID;
				break;
			case "2":
				payStatus = PayConstants.PayStatusEnum.PAYMENT_SUCCESS;
				break;
			case "3":
				payStatus = PayConstants.PayStatusEnum.PAYMENT_FAILURE;
				break;
			case "4":
				payStatus = PayConstants.PayStatusEnum.OVERDUE_PAYMENT;
				break;
			default:
				throw new ServiceRuntimeException("未知的支付结果paySt:"+paySt);
		}
		return payStatus;
	}

	protected abstract String getZhangLingPayChannelId(String payType);

	@Override
	public ResultBO<?> payReturn(Map<String, String> map) {
		return super.payReturn(map);
	}

	@Override
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO) {
		throw new ServiceRuntimeException("掌灵不支持查询退款");
	}

	@Override
	public ResultBO<?> refund(RefundParamVO refundParam) {
		throw new ServiceRuntimeException("掌灵不支持退款");
	}

	@Override
	public ResultBO<?> queryBill(Map<String, String> map) {
		throw new ServiceRuntimeException("掌灵不支持queryBill");
	}

	private String doEncrypt(Map<String, Object> map,String mchntKey) {
		Object[] keys =  map.keySet().toArray();
		Arrays.sort(keys);
		StringBuilder originStr = new StringBuilder();
		for(Object key:keys){
			if(null!=map.get(key)&&!map.get(key).toString().equals("")&&!"signature".equals(key))
				originStr.append(key).append("=").append(map.get(key)).append("&");
		}
		originStr.append("key=").append(mchntKey);
		String sign = null;
		try {
			sign = DigestUtils.md5Hex(originStr.toString().getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			logger.error(e);
			throw new ServiceRuntimeException(e.getMessage());
		}
		return sign;
	}

}
