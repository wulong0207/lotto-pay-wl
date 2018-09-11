package com.hhly.paycore.paychannel.zhangling;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.exception.ServiceRuntimeException;
import com.hhly.skeleton.pay.vo.PayReqResultVO;

import java.util.Map;
import java.util.Objects;

/**
 * @desc  掌灵Wap支付
 * @author xiongJinGang
 * @date 2017年11月23日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class ZhangLingWapService extends ZhangLingAbstractService {

	@Override
	protected ResultBO<?> invokePay(Map<String, String> paramMap) {
		// 拼表单
		StringBuilder sbHtml = new StringBuilder();
		sbHtml.append("<form id=\"lianliansubmit\" name=\"lianliansubmit\" action=\"" + getPayUrl() + "\"" + "\" method=\"post\">");
		sbHtml.append("<input type='hidden' name='orderInfo' value='" + paramMap.get("orderInfo") + "'/>");
		// submit按钮控件请不要含有name属性
		sbHtml.append("<input type=\"submit\" value=\"确认\" style=\"display:none;\"></form>");
		sbHtml.append("<script>document.forms['lianliansubmit'].submit();</script>");
		PayReqResultVO payReqResult = new PayReqResultVO(sbHtml.toString());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.ZHANGLING_RECHARGE.getKey());
		payReqResult.setType(PayConstants.PayReqResultEnum.FORM.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	protected String getPayUrl() {
		return ZhangLingConfig.ZHANGLING_H5_URL;
	}

	@Override
	protected String getPlatform() {
		return PayAbstract.PLATFORM_WAP;
	}

	@Override
	protected String getVersion(Short payPlatform) {
		// 支付平台 1PC、2WAP、3ANDROID、4IOS、5未知、6H5，参见：PayConstants.TakenPlatformEnum枚举
		return Objects.equals(PayConstants.TakenPlatformEnum.ANDROID.getKey(),payPlatform) || Objects.equals(PayConstants.TakenPlatformEnum.IOS.getKey(),payPlatform) ? "h5_NoEncrypt_js":"h5_NoEncrypt";
	}

	@Override
	protected String getZhangLingPayChannelId(String payType) {
		if (payType.equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
			// 微信wap支付
			return "0000000007";
		} else if(payType.equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
			// 支付宝wap支付
			return "0000000006";
		} else{
			throw new ServiceRuntimeException("掌灵Wap支付未支持的支付方式payType："+payType);
		}
	}
}
