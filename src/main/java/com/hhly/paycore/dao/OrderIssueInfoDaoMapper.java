package com.hhly.paycore.dao;

import org.apache.ibatis.annotations.Param;

import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderIssueInfoBO;

public interface OrderIssueInfoDaoMapper {
	/**
	 * @Description:根据订单编号找到推单信息
	 * @param issueOrderCode
	 * @return OrderIssueInfoBO
	 * @author wuLong
	 * @date 2017年10月12日 上午10:27:46
	 */
	OrderIssueInfoBO getOrderIssueInfo(@Param("issueOrderCode")String issueOrderCode);
	/**
	 * @Description: 更新发单订单的总佣金
	 * @param id
	 * @param amount
	 * @return
	 * @author wuLong
	 * @date 2017年10月12日 上午10:46:34
	 */
	int update(@Param("id")Integer id,@Param("commissionAmount")double amount);
	/**
	 * 
	 * @Description: 更新推单总佣金为0
	 * @param id
	 * @return
	 * @author wuLong
	 * @date 2017年10月12日 下午6:15:50
	 */
	int updateToZero(@Param("id")Integer id);
}