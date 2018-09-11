package com.hhly.paycore.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;

/**
 * @desc 合买订单退款
 * @author xiongJinGang
 * @date 2018年5月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface CancellationRefundOrderGroupService {
	/**  
	* 方法说明: 合买订单退款
	* @auth: xiongJinGang
	* @param orderCancelMsgModel
	* @throws Exception
	* @time: 2018年5月2日 下午5:48:25
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> doCancellation(OrderCancelMsgModel orderCancelMsgModel) throws Exception;
}
