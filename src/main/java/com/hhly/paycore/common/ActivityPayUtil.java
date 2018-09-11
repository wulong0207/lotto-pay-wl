package com.hhly.paycore.common;

import java.util.Date;

import org.apache.log4j.Logger;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.activity.vo.ActivityPayParamVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * @desc 活动支付工具类
 * @author xiongJinGang
 * @date 2018年1月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class ActivityPayUtil {

	private static final Logger logger = Logger.getLogger(ActivityPayUtil.class);

	/**  
	* 方法说明: 验证活动
	* @auth: xiongJinGang
	* @param activityPayParam
	* @time: 2018年1月6日 下午4:55:04
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateParam(ActivityPayParamVO activityPayParam) {
		if (ObjectUtil.isBlank(activityPayParam)) {
			return ResultBO.err(MessageCodeConstants.PARAM_IS_NULL_FIELD);
		}
		// 验证订单号是否为空
		if (ObjectUtil.isBlank(activityPayParam.getOrderCode())) {
			return ResultBO.err(MessageCodeConstants.PAY_ORDER_NO_IS_NULL);
		}
		// 验证活动类型是否为空
		if (ObjectUtil.isBlank(activityPayParam.getActivityType())) {
			return ResultBO.err(MessageCodeConstants.PAY_ACTIVITY_TYPE_IS_NULL);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证订单的订单支付状态及支付时间 
	* @auth: xiongJinGang
	* @param orderInfo
	* @time: 2018年1月6日 下午5:41:22
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateOrderStatus(PayOrderBaseInfoVO orderInfo) {
		/**************判断支付结束时间开始******************/
		if (ObjectUtil.isBlank(orderInfo.getEndSysTime())) {
			logger.info("订单【" + orderInfo.getOrderCode() + "】支付截止时间为空");
			return ResultBO.err(MessageCodeConstants.PAY_END_TIME_IS_NULL_FIELD_SERVICE);
		}
		int days = DateUtil.compare(orderInfo.getEndSysTime(), new Date());
		if (days <= 0) {
			logger.info("订单【" + orderInfo.getOrderCode() + "】已过支付截止时间【" + orderInfo.getEndSysTime() + "】");
			return ResultBO.err(MessageCodeConstants.PAY_DEADLINE_HAS_PASSED);
		}
		/**************判断支付结束时间结束******************/
		if (ObjectUtil.isBlank(orderInfo.getPayStatus())) {
			logger.info("订单【" + orderInfo.getOrderCode() + "】支付状态【" + orderInfo.getPayStatus() + "】为空，不能支付");
			return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
		}
		if (orderInfo.getPayStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey())) {
			logger.info("订单【" + orderInfo.getOrderCode() + "】支付状态已支付成功，不能重复支付");
			return ResultBO.err(MessageCodeConstants.ORDER_HAD_PAY);
		}
		if (ObjectUtil.isBlank(orderInfo.getPayStatus()) || (!orderInfo.getPayStatus().equals(PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey()) && !orderInfo.getPayStatus().equals(PayConstants.PayStatusEnum.BEING_PAID.getKey()))) {
			logger.info("订单【" + orderInfo.getOrderCode() + "】当前支付状态【" + orderInfo.getPayStatus() + "】不能支付");
			return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}
}
