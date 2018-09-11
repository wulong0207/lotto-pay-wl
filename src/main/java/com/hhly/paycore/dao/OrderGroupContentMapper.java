package com.hhly.paycore.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.hhly.skeleton.pay.bo.OrderGroupContentBO;

public interface OrderGroupContentMapper {

	/**  
	* 方法说明: 根据合买订单号和用户Id获取合买订单详情（同一个用户可以参与多次）
	* @auth: xiongJinGang
	* @param orderCode
	* @param userId
	* @time: 2018年5月3日 上午9:25:33
	* @return: List<OrderGroupContentBO> 
	*/
	List<OrderGroupContentBO> getUserOrderGroupContentByOrderCode(@Param("orderCode") String orderCode, @Param("userId") Integer userId);

	/**  
	* 方法说明: 查询用户最后一笔认购记录
	* @auth: xiongJinGang
	* @param orderCode
	* @param userId
	* @time: 2018年5月3日 上午9:38:08
	* @return: OrderGroupContentBO 
	*/
	OrderGroupContentBO getUserLastOrderGroupContentByOrderCode(@Param("orderCode") String orderCode, @Param("userId") Integer userId);

	/**  
	* 方法说明: 根据订单号获取合买订单列表
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2018年5月3日 上午9:26:27
	* @return: List<OrderGroupContentBO> 
	*/
	List<OrderGroupContentBO> getOrderGroupContentByOrderCode(@Param("orderCode") String orderCode);

	/**  
	* 方法说明: 根据buyCode获取合买订单详情
	* @auth: xiongJinGang
	* @param buyCode
	* @time: 2018年5月9日 下午3:00:53
	* @return: OrderGroupContentBO 
	*/
	OrderGroupContentBO getOrderGroupContentByBuyCode(@Param("buyCode") String buyCode);
	
	
	/**  
	* 方法说明: 根据订单号和
	* @auth: xiongJinGang
	* @param orderCode
	* @param buyType
	* @time: 2018年7月19日 下午3:04:32
	* @return: OrderGroupContentBO 
	*/
	OrderGroupContentBO getOrderGroupContentByOrderCodeAndType(@Param("orderCode") String orderCode,@Param("buyType") Short buyType);

	/**  
	* 方法说明: 更新合买订单详情状态
	* @auth: xiongJinGang
	* @param map
	* @time: 2018年5月4日 下午4:20:51
	* @return: int 
	*/
	int updateOrderGroupContentStatus(Map<String, Object> map);
}