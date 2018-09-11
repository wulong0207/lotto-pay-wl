package com.hhly.paycore.paychannel.nowpay.wap;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.nowpay.config.NowPayConfig;
import com.hhly.paycore.paychannel.nowpay.util.FormDateReportConvertor;
import com.hhly.paycore.paychannel.nowpay.util.HttpNowPayUtils;
import com.hhly.paycore.paychannel.nowpay.util.MD5Facade;
import com.hhly.paycore.paychannel.wechatpay.web.util.GetWeChatUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

import net.sf.json.JSONObject; 
/**
 * 
 * @ClassName: NowWapPayService 
 * @Description: 现在支付wap
 * @author wuLong
 * @date 2017年8月8日 上午10:32:33 
 *
 */
public class NowWapPayService extends PayAbstract {
	private Logger logger = LoggerFactory.getLogger(NowWapPayService.class);
	
	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		logger.info("NowWapPayService.pay接手的对象信息:"+JSON.toJSONString(paymentInfo));
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		PayReqResultVO payReqResult = new PayReqResultVO();
		try {
			packageParams.put("mhtOrderNo", paymentInfo.getNoOrder());
			packageParams.put("mhtOrderName", paymentInfo.getNameGoods());
			packageParams.put("mhtOrderType", "01");
			packageParams.put("version", "1.0.0");
			packageParams.put("funcode", "WP001");
			packageParams.put("mhtCurrencyType", "156");
			packageParams.put("notifyUrl", paymentInfo.getNotifyUrl());//商户后台通知URL
			packageParams.put("frontNotifyUrl", paymentInfo.getUrlReturn());//商户前台通知URL
			String md5key = null;
			String payType = paymentInfo.getPayType();
			String payChannelType = null;
			switch (payType) {
				case "10":
					packageParams.put("appId", NowPayConfig.NOW_PAY_WAP_ALI_APPID);
					md5key = NowPayConfig.NOW_PAY_WAP_ALI_MD5KEY;
					payChannelType = "12";
					break;
				case "11":
					packageParams.put("appId", NowPayConfig.NOW_PAY_WAP_WX_APPID);
					md5key = NowPayConfig.NOW_PAY_WAP_WX_MD5KEY;
					payChannelType = "13";
					break;
				default:
					break;
			}
			packageParams.put("mhtOrderAmt", GetWeChatUtil.getMoney(paymentInfo.getMoneyOrder()));
			packageParams.put("mhtCharset", "UTF-8");
			packageParams.put("mhtOrderDetail", paymentInfo.getInfoOrder());
			packageParams.put("mhtOrderStartTime", paymentInfo.getDtOrder());//商户订单开始时间
			packageParams.put("deviceType", "0601");//设备类型
			packageParams.put("payChannelType", payChannelType);//用户所选渠道类型,银联：11,支付宝：12,微信：13,手Q：25
			Date dtorder = DateUtil.convertStrToDate(paymentInfo.getDtOrder(), DateUtil.DATE_FORMAT_NUM);
			Calendar cd = Calendar.getInstance();
			cd.setTime(dtorder);
			cd.add(Calendar.MINUTE, Integer.valueOf(paymentInfo.getValidOrder()));
			packageParams.put("mhtOrderTimeOut", String.valueOf(Integer.valueOf(paymentInfo.getValidOrder())*60));//商户订单超时时间 60~3600秒，默认3600
			packageParams.put("outputType", "0");//输出格式(outputType=0(默认取值),outputType=1,outputType=2(注：微信deeplink)，outputType=3)
			packageParams.put("mhtSignType", "MD5");//商户签名方法(定值：MD5)
			String mhtSignature = MD5Facade.getFormDataParamMD5(packageParams, md5key, "UTF-8");
			packageParams.put("mhtSignature", mhtSignature);//商户数据签名(除mhtSignature字段外，所有参数都参与MD5签名。)
			logger.info("现在支付请求参数："+net.sf.json.JSONObject.fromObject(packageParams));
			logger.debug("现在支付请求参数："+net.sf.json.JSONObject.fromObject(packageParams));
			
			// 建立请求
			String reposeText = HttpNowPayUtils.http(NowPayConfig.NOW_PAY_URL, packageParams);
			logger.info("现在支付返回结果:"+reposeText);
			logger.debug("现在支付返回结果:"+reposeText);
			if(reposeText.indexOf("form")>-1){
				String form = reposeText.substring(reposeText.indexOf("name=")+"name=".length()+1,reposeText.indexOf("method")-2).trim();
				String formLink = reposeText.substring(reposeText.indexOf("<form"), reposeText.indexOf("</form>")+"</form>".length());
				formLink +="<script>document.forms."+form+".submit();</script>";
				payReqResult.setFormLink(formLink);
				payReqResult.setTradeChannel(PayConstants.PayChannelEnum.NOWPAY_RECHARGE.getKey());
				payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
			}else{
				Map<String,String> map = FormDateReportConvertor.parseFormDataPatternReportWithDecode(reposeText, "UTF-8", "UTF-8");
				logger.info("现在支付转码返回结果:"+JSONObject.fromObject(map));
				String responseCode = map.get("responseCode");
				//不成功
				if(!"A001".equals(responseCode)){
					logger.info("现在支付返回的错误信息:"+map.get("responseMsg"));
					return ResultBO.err();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.ok(payReqResult);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
		SortedMap<String, String> packageParams = new TreeMap<String, String>();
		try {
			packageParams.put("funcode", "MQ002");
			packageParams.put("version", "1.0.0");
			packageParams.put("deviceType", "0601");
			packageParams.put("mhtOrderNo", payQueryParamVO.getTransCode());
			packageParams.put("mhtCharset", "UTF-8");
			packageParams.put("mhtSignType", "MD5");
			String rechargeChannel = payQueryParamVO.getRechargeChannel();
			String md5key = null;
			switch (rechargeChannel) {
				case "10":
					packageParams.put("appId", NowPayConfig.NOW_PAY_WAP_ALI_APPID);
					md5key = NowPayConfig.NOW_PAY_WAP_ALI_MD5KEY;
					break;
				case "11":
					packageParams.put("appId", NowPayConfig.NOW_PAY_WAP_WX_APPID);
					md5key = NowPayConfig.NOW_PAY_WAP_WX_MD5KEY;
					break;
				default:
					break;
			}
			String sign = MD5Facade.getFormDataParamMD5(packageParams, md5key, "UTF-8");
			packageParams.put("mhtSignature", sign);
			logger.info("现在支付商户主动查询订单状态请求参数:"+JSONObject.fromObject(packageParams));
//			String reposeText = HttpNowPayUtils.http(NowPayConfig.NOW_PAY_URL, packageParams);
			String reposeText = HttpUtil.doPost(NowPayConfig.NOW_PAY_URL, packageParams);
			
			logger.info("现在支付商户主动查询订单状态返回结果:"+reposeText);
			Map<String,String> map = FormDateReportConvertor.parseFormDataPatternReportWithDecode(reposeText, "UTF-8", "UTF-8");
			logger.info("现在支付商户主动查询订单状态转码返回结果:"+JSONObject.fromObject(map));
			String responseCode = map.get("responseCode");
			if("A001".equals(responseCode)){
				String signature = map.get("signature");
				map.remove("signature");
				String signReturn = MD5Facade.getFormDataParamMD5(map, md5key, "UTF-8");
				if(!signReturn.equals(signature)){
					logger.error("现在支付查询订单状态返回结果：签名错误,现在支付我方签名结果："+signReturn+",第三方签名:"+signature);
					return ResultBO.err();
				}
				// 2.封装返回对象
				if (map.containsKey("nowPayOrderNo")) { // 支付宝交易号
					payQueryResultVO.setTradeNo(map.get("nowPayOrderNo"));
				}
				if (map.containsKey("mhtOrderNo")) {// 商户订单号
					payQueryResultVO.setOrderCode(map.get("mhtOrderNo"));
				}
				if (map.containsKey("mhtOrderAmt")) {// 支付金额
					String totalFee = map.get("mhtOrderAmt");
					if (!ObjectUtil.isBlank(totalFee)) {
						double tf = Double.valueOf(totalFee)/100;
						payQueryResultVO.setTotalAmount(String.valueOf(tf));
					}
				}
				if (map.containsKey("transStatus")) {// 支付状态
					String status = map.get("transStatus");
					//0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
					if ("A001".equals(status)){
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
					} else if("A004".equals(status)){
						payQueryResultVO.setTradeStatus(PayStatusEnum.WAITTING_PAYMENT);
					} else if("A003".equals(status)){
						payQueryResultVO.setTradeStatus(PayStatusEnum.BEING_PAID);
					} else if("A002".equals(status)){
						payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_FAILURE);
					} else if("A005".equals(status)){
						payQueryResultVO.setTradeStatus(PayStatusEnum.OVERDUE_PAYMENT);
					}
				}
				if (map.containsKey("payTime")) {// 付款时间
					payQueryResultVO.setArriveTime(DateUtil.convertDateToStr(new Date(), DateUtil.DATE_FORMAT_NUM));
				}
			}else{
				if("A002".equals(responseCode)){
					logger.info("现在支付商户主动查询订单状态响应失败");
					return ResultBO.err();
				}else if("A002".equals(responseCode)){
					logger.info("现在支付商户主动查询订单状态响应未知");
					return ResultBO.err();
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return ResultBO.err();
		} finally {
			if (!ObjectUtil.isBlank(packageParams)) {
				packageParams.clear();
			}
		}
		return ResultBO.ok(payQueryResultVO);
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			String payChannelType = map.get("payChannelType");
			String md5key = null;
			if("12".equals(payChannelType)){
				md5key = NowPayConfig.NOW_PAY_WAP_ALI_MD5KEY;
			}else if("13".equals(payChannelType)){
				md5key = NowPayConfig.NOW_PAY_WAP_WX_MD5KEY;
			}
			String signatrue = map.get("signature");
			map.remove("signature");
			String sign = MD5Facade.getFormDataParamMD5(map, md5key, "UTF-8");
			if(!sign.equals(signatrue)){
				logger.error("现在支付异步通知结果：签名错误,现在支付我方签名结果："+sign+",第三方签名:"+map.get("signature"));
				payNotifyResult.setResponse("SUCCESS");
				payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE);// 支付失败
				return ResultBO.ok(payNotifyResult);
			}
			// 2.封装返回对象
			if (map.containsKey("nowPayOrderNo")) { // 支付宝交易号
				payNotifyResult.setThirdTradeNo(map.get("nowPayOrderNo"));
			}
			if (map.containsKey("mhtOrderNo")) {// 商户订单号
				payNotifyResult.setOrderCode(map.get("mhtOrderNo"));
			}
			if (map.containsKey("mhtOrderAmt")) {// 支付金额
				String totalFee = map.get("mhtOrderAmt");
				if (!ObjectUtil.isBlank(totalFee)) {
					double tf = Double.valueOf(totalFee)/100;
					payNotifyResult.setOrderAmt(tf);
				}
			}
			if (map.containsKey("transStatus")) {// 支付状态
				String status = map.get("transStatus");
				//0-成功 1-失败 2-待支付 3-已关闭 4-转入退款
				if ("A001".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
				} else if ("A004".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.WAITTING_PAYMENT);
				} else if ("A003".equals(status)) {
					payNotifyResult.setStatus(PayStatusEnum.BEING_PAID);
				} else if("A002".equals(status)){
					payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
				}  else if("A005".equals(status)){
					payNotifyResult.setStatus(PayStatusEnum.OVERDUE_PAYMENT);
				}
			}
			payNotifyResult.setTradeTime(DateUtil.convertDateToStr(new Date(), DateUtil.DATE_FORMAT_NUM));
			payNotifyResult.setResponse("SUCCESS=Y");
		} catch (Exception e) {
			logger.error(e.getMessage(),e);
			return ResultBO.err();
		}
		return ResultBO.ok(payNotifyResult);
	}
}
