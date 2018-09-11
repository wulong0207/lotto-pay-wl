package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;

/**
 * @desc 订单支付后更新订单的信息
 * @author xiongJinGang
 * @date 2017年3月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayOrderUpdateMapper {

	/**  
	* 方法说明: 更新订单的支付信息
	* @auth: xiongJinGang
	* @param payOrderUpdatePO
	* @time: 2017年3月27日 上午10:18:22
	* @return: int 
	*/
	int updateOrderPayStatus(PayOrderUpdatePO payOrderUpdatePO);

	/**  
	* 方法说明: 更新订单的中奖状态
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @time: 2017年9月7日 下午7:30:08
	* @return: int 
	*/
	int updateOrderWinningStatus(OrderInfoBO orderInfoBO);

	/**  
	* 方法说明: 批量更新订单状态
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月11日 下午5:34:50
	* @return: int 
	*/
	int updateOrderBatch(@Param("list") List<PayOrderUpdatePO> list);

	/**  
	* 方法说明: 根据订单号获取订单信息 
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年3月27日 下午2:58:40
	* @return: OrderInfoBO 
	*/
	OrderInfoBO getOrderInfo(String orderCode);

	/**  
	* 方法说明: 查询多个订单
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年5月11日 上午11:05:16
	* @return: List<OrderInfoBO> 
	*/
	List<OrderInfoBO> getOrderList(@Param("orderCodes") List<String> orderCode);
}
