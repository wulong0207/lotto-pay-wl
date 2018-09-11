package com.hhly.paycore.remote.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.vo.PayParamVO;

public interface IPayPushOrderService {
	public ResultBO<?> pushPay(PayParamVO payParam);

}
