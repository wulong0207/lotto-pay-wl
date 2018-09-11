package com.hhly.paycore.dao;

import java.util.List;

import com.hhly.paycore.po.TransRechargeDetailPO;
import com.hhly.skeleton.pay.bo.TransRechargeDetailBO;

/**
 * @desc 充值详情
 * @author xiongJinGang
 * @date 2017年8月21日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransRechargeDetailMapper {

	/**  
	* 方法说明: 根据充值编号获取充值详情
	* @auth: xiongJinGang
	* @param rechargeCode
	* @time: 2017年8月21日 下午3:21:02
	* @return: List<TransRechargeDetailBO> 
	*/
	List<TransRechargeDetailBO> getRechargeDetailByCode(String rechargeCode);

	/**  
	* 方法说明: 根据订单号获取充值详情信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年8月21日 下午3:21:21
	* @return: TransRechargeDetailBO 
	*/
	TransRechargeDetailBO getRechargeDetailByOrderCode(String orderCode);

	/**  
	* 方法说明: 添加充值详情
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年8月21日 下午3:20:49
	* @return: int 
	*/
	int addRechargeDetailList(List<TransRechargeDetailPO> list);

}
