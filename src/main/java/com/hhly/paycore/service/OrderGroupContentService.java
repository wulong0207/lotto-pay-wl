package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.pay.bo.OrderGroupContentBO;

/**
 * @desc 合买订单详情
 * @author xiongJinGang
 * @date 2018年5月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface OrderGroupContentService {

	/**  
	* 方法说明: 获取订单的合买记录详情
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2018年5月3日 上午9:40:40
	* @return: List<OrderGroupContentBO> 
	*/
	List<OrderGroupContentBO> findOrderGroupContentByOrderCode(String orderCode);

	/**  
	* 方法说明: 更新合买订单详情退款状态
	* @auth: xiongJinGang
	* @param orderCode
	* @param status
	* @time: 2018年5月4日 下午4:24:19
	* @return: int 
	*/
	int updateOrderGroupContentStatus(String orderCode, Short status);

	/**  
	* 方法说明: 根据合买订单编号获取合买订单详情信息
	* @auth: xiongJinGang
	* @param buyCode
	* @time: 2018年5月9日 下午3:07:45
	* @return: OrderGroupContentBO 
	*/
	OrderGroupContentBO findOrderGroupContentByBuyCodeFromCache(String buyCode);

}
