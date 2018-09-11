package com.hhly.paycore.dao;

import com.hhly.paycore.po.OrderResetExceptionPO;

public interface OrderResetExceptionMapper {
	/**  
	* 方法说明: 添加重置开奖异常订单信息
	* @auth: xiongJinGang
	* @time: 2017年9月8日 上午10:45:21
	* @return: OrderResetExceptionPO 
	*/
	int addOrderReset(OrderResetExceptionPO orderResetExceptionPO);

}
