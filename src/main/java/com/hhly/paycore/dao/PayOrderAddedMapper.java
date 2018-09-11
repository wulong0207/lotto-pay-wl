package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.skeleton.pay.bo.PayOrderAddBO;

/**
 * @desc 订单追号计划
 * @author xiongJinGang
 * @date 2017年4月26日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayOrderAddedMapper {

	/**  
	* 方法说明:  更新追号订单的支付信息
	* @auth: xiongJinGang
	* @param payOrderUpdatePO
	* @time: 2017年4月26日 上午10:02:03
	* @return: int 
	*/
	public int updateOrderPayStatus(PayOrderUpdatePO payOrderUpdatePO);

	/**  
	* 方法说明: 批量更新追号计划状态
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月12日 上午10:46:29
	* @return: int 
	*/
	public int updateOrderBatch(@Param("list") List<PayOrderUpdatePO> list);

	/**  
	* 方法说明: 根据订单号获取订单信息 
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年4月26日 上午10:02:33
	* @return: OrderInfoBO 
	*/
	public PayOrderAddBO getOrderInfo(@Param("orderCode") String orderCode);

	/**  
	* 方法说明: 查询多个订单
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年5月11日 上午11:05:16
	* @return: List<OrderInfoBO> 
	*/
	List<PayOrderAddBO> getOrderList(@Param("orderCodes") List<String> orderCode);
}
