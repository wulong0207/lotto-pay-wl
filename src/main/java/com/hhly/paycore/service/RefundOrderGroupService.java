package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

public interface RefundOrderGroupService {

	/**  
	* 方法说明: 合买订单退款
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @param baoDiTransUserBO
	* @param groupTransUserList
	* @param orderInfo
	* @param totalOrderGroupAmount 总的参与合买金额
	* @throws Exception
	* @time: 2018年5月4日 下午12:05:10
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyRefundOrder(CancellationRefundBO cancellationRefundBO, List<TransUserBO> groupTransUserList, PayOrderBaseInfoVO orderInfo, double totalOrderGroupAmount) throws Exception;

}
