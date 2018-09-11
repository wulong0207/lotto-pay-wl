package com.hhly.paycore.paychannel.shenzhoupay.web;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.paycore.paychannel.PayAbstract;
import com.hhly.paycore.paychannel.shenzhoupay.config.DivineConfig;
import com.hhly.paycore.paychannel.shenzhoupay.util.SZCardUtil;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;

/**
 * @desc 神州充值卡支付
 * @author xiongJinGang
 * @date 2017年10月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SZCardWebService extends PayAbstract {
	private Logger logger = LoggerFactory.getLogger(SZCardWebService.class);

	@Override
	public ResultBO<?> pay(PaymentInfoBO paymentInfo) {
		return SZCardUtil.toPay(paymentInfo, DivineConfig.SHENZHOU_CARD_WEB_PAY_URL);
	}

	@Override
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO) {
		logger.info(payQueryParamVO.getTransCode() + "为神州充值卡充值，过时间未支付，设置成订单关闭！");
		return ResultBO.err();
	}

	@Override
	public ResultBO<?> payNotify(Map<String, String> map) {
		return SZCardUtil.anaylizePayNotify(map);
	}

}
