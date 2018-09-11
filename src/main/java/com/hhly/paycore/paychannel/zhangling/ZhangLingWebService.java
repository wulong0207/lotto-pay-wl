package com.hhly.paycore.paychannel.zhangling;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.exception.ServiceRuntimeException;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * @desc 掌灵Web支付
 * @author xiongJinGang
 * @date 2017年11月23日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class ZhangLingWebService extends ZhangLingAbstractService {

	private static final Logger logger = Logger.getLogger(ZhangLingWebService.class);

	@Override
	protected ResultBO<?> invokePay(Map<String, String> paramMap) {
		String zhangLingRetStr = null;
		try {
			zhangLingRetStr = HttpUtil.doPost(getPayUrl(), paramMap);
		} catch (Exception e) {
			logger.error(e);
		}
		logger.info("掌灵请求支付返回：" + zhangLingRetStr);

		HashMap<String,Object> map = (HashMap) JSON.parseObject(zhangLingRetStr, HashMap.class);
		// extra值格式为：code_url=https://xxx&code_img_url=http://trans.palmf.cn/xxxxxx,项目只需二维码url，不需要图片
		String rawUrl = map.get("extra").toString();
		String codeUrl = rawUrl.substring(0,rawUrl.indexOf("&code_img_url")).replace("code_url=","");
		// 唤起支付的url地址
		PayReqResultVO payReqResult = new PayReqResultVO(codeUrl);
		payReqResult.setType(PayConstants.PayReqResultEnum.LINK.getKey());
		payReqResult.setTradeChannel(PayConstants.PayChannelEnum.ZHANGLING_RECHARGE.getKey());
		return ResultBO.ok(payReqResult);
	}

	@Override
	protected String getPayUrl() {
		// PC扫码的请求地址
		return ZhangLingConfig.ZHANGLING_API_URL;
	}

	@Override
	protected String getPlatform() {
		return PayAbstract.PLATFORM_WEB;
	}

	@Override
	protected String getVersion(Short payPlatform) {
		return "h5_NoEncrypt";
	}

	@Override
	protected String getZhangLingPayChannelId(String payType) {
		if (payType.equals(PayConstants.PayTypeThirdEnum.WEIXIN_PAYMENT.getKey())) {
			// 微信扫码支付
			return "2100000001";
		} else if(payType.equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
			// 支付宝扫码支付
			return "0000000002";
		} else{
			throw new ServiceRuntimeException("掌灵Web支付未支持的支付方式payType："+payType);
		}
	}
}
