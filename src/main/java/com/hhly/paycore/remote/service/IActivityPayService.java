package com.hhly.paycore.remote.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.activity.vo.ActivityPayParamVO;

public interface IActivityPayService {

	ResultBO<?> activityPay(ActivityPayParamVO activityPayParam) throws Exception;

}
