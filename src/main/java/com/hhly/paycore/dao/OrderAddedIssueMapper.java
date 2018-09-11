package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.OrderAddedIssuePO;
import com.hhly.skeleton.pay.bo.OrderAddedIssueBO;

/**
 * @author YiJian
 * @version 1.0
 * @desc
 * @date 2017/5/24.
 * @company 益彩网络科技有限公司
 */
public interface OrderAddedIssueMapper {

	/**
	 * 获取追号期数详情
	 * @param orderAddedIssueBO
	 * @return
	 */
	List<OrderAddedIssueBO> getOrderAddedIssues(OrderAddedIssueBO orderAddedIssueBO);

	/**
	 * 获取追号期数详情List
	 * @param orderAddedIssueBO
	 * @return
	 */
	List<OrderAddedIssueBO> getCancelOrderAddedIssues(OrderAddedIssueBO orderAddedIssueBO);

	/**  
	* 方法说明: 根据订单号获取订单信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年8月3日 下午5:39:14
	* @return: OrderAddedIssueBO 
	*/
	OrderAddedIssueBO getOrderInfo(@Param("orderCode") String orderCode);

	/**
	 * 更新追号期数详情
	 * @param orderAddedIssuePO
	 * @return
	 */
	int updateAddedIssue(OrderAddedIssuePO orderAddedIssuePO);
}
