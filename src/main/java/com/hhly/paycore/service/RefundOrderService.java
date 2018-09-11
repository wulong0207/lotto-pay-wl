package com.hhly.paycore.service;

import java.util.List;
import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderAddedIssueBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

public interface RefundOrderService {

	/**  
	* 方法说明: 更新撤单的订单状态
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @param transUserBO
	* @param operateCouponBO
	* @param orderInfo
	* @param orderAddedIssues
	* @throws Exception
	* @time: 2017年8月18日 上午10:25:55
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> modifyRefundOrder(CancellationRefundBO cancellationRefundBO, TransUserBO transUserBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo, List<OrderAddedIssueBO> orderAddedIssues) throws Exception;

	/**  
	* 方法说明: 批量处理退款【CMS专用】
	* @auth: xiongJinGang
	* @param map
	* @param transUserBO
	* @param operateCouponBO
	* @param orderInfo
	* @time: 2017年11月15日 下午2:43:24
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	public ResultBO<?> modifyRefundOrderByBatch(Map<CancellationRefundBO, List<OrderAddedIssueBO>> map, TransUserBO transUserBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception;

	void updateOrderStatus(CancellationRefundBO cancellationRefundBO, List<OrderAddedIssueBO> orderAddedIssues, PayOrderBaseInfoVO cancelOrderInfo) throws Exception;

}
