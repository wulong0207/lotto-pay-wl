package com.hhly.paycore.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;

import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderFollowedInfoBO;

public interface OrderFollowedInfoDaoMapper {
	/**
	 * @Description: 根据订单编号，更新跟单订单的佣金
	 * @param commissionAmount
	 * @param orderCode
	 * @return
	 * @author wuLong
	 * @date 2017年10月12日 上午10:52:32
	 */
	int update(@Param("commissionAmount")double commissionAmount,@Param("orderCode") String orderCode);
	/**
	 * @Description: 根据推单Id找到所有的跟单订单编号
	 * @param orderIssueId
	 * @return
	 * @author wuLong
	 * @date 2017年10月12日 下午5:33:20
	 */
	List<String> getFollowOrderCode(@Param("orderIssueId") Integer orderIssueId);
	
	@SelectProvider(type = OrderFollowedInfoProvider.class , method = "selectOrderFollow" )
	@MapKey("orderCode")
	Map<String, OrderFollowedInfoBO> findOrderFollowMap(Integer orderIssueId);
}