package com.hhly.paycore.paychannel.alipay.wap;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.alipay.config.AlipayConfig;
import com.hhly.paycore.paychannel.alipay.wap.util.AlipaySubmit;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

/** 
* @Desc:  支付宝 手机网页支付
* @author YiJian 
* @date 2017年3月15日
* @compay 益彩网络科技有限公司
* @version 1.0
*/
public class AlipayWapService extends PayAbstract {

	Logger logger = Logger.getLogger(AlipayWapService.class);

	public ResultBO<?> pay(PaymentInfoBO paymentInfoBO) {
		// 移动支付 授权
		String req_dataToken = "<direct_trade_create_req><notify_url>" + paymentInfoBO.getNotifyUrl() + "</notify_url><call_back_url>" + paymentInfoBO.getRequestUrl() + "</call_back_url><seller_account_name>" + AlipayConfig.seller_email
				+ "</seller_account_name><out_trade_no>" + paymentInfoBO.getNoOrder() + "</out_trade_no><subject>" + paymentInfoBO.getNameGoods() + "</subject><total_fee>" + paymentInfoBO.getMoneyOrder() + "</total_fee><merchant_url>"
				+ AlipayConfig.merchant_url + "</merchant_url></direct_trade_create_req>";
		// 把请求参数打包成数组
		Map<String, String> sParaTempToken = new HashMap<String, String>();
		sParaTempToken.put("service", "alipay.wap.trade.create.direct");
		sParaTempToken.put("partner", AlipayConfig.partner);
		sParaTempToken.put("_input_charset", AlipayConfig.input_charset);
		sParaTempToken.put("sec_id", AlipayConfig.sign_type);
		sParaTempToken.put("format", AlipayConfig.format);
		sParaTempToken.put("v", AlipayConfig.v);
		sParaTempToken.put("req_id", paymentInfoBO.getNoOrder());
		sParaTempToken.put("req_data", req_dataToken);

		String sHtmlTextToken = null;
		String request_token = null;
		try {
			sHtmlTextToken = AlipaySubmit.buildRequest("", "", sParaTempToken);
			// URLDECODE返回的信息
			sHtmlTextToken = URLDecoder.decode(sHtmlTextToken, AlipayConfig.input_charset);
			// 获取token
			request_token = AlipaySubmit.getRequestToken(sHtmlTextToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// //////////////////////////////////根据授权码token调用交易接口alipay.wap.auth.authAndExecute//////////////////////////////////////
		// 业务详细
		String req_data = "<auth_and_execute_req><request_token>" + request_token + "</request_token></auth_and_execute_req>";
		// 必填
		// 把请求参数打包成数组
		Map<String, String> sParaTemp = new HashMap<String, String>();
		sParaTemp.put("service", "alipay.wap.auth.authAndExecute");
		sParaTemp.put("partner", AlipayConfig.partner);
		sParaTemp.put("_input_charset", AlipayConfig.input_charset);
		sParaTemp.put("sec_id", AlipayConfig.sign_type);
		sParaTemp.put("format", AlipayConfig.format);
		sParaTemp.put("v", AlipayConfig.v);
		sParaTemp.put("req_data", req_data);
		// 建立请求
		String sHtmlText = AlipaySubmit.buildRequest(sParaTemp, "get", "确认");
		PayReqResultVO payReqResult = new PayReqResultVO(sHtmlText);
		payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.ALIPAY_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
		// return ResultBO.ok(sHtmlText);
	}
}
