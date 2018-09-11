package com.hhly.paycore.service;

import java.util.List;
import java.util.Map;

import com.hhly.skeleton.pay.channel.bo.PayChannelLimitBO;

/**
 * @desc 渠道支付限额
 * @author xiongJinGang
 * @date 2017年12月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayChannelLimitService {

	/**  
	* 方法说明: 所有渠道支付限额列表 
	* @auth: xiongJinGang
	* @time: 2017年12月8日 下午4:16:33
	* @return: List<PayChannelLimitBO> 
	*/
	List<PayChannelLimitBO> findChannelList();

	/**  
	* 方法说明: 从缓存中获取支付限额
	* @auth: xiongJinGang
	* @param payChannelMgrId
	* @time: 2017年12月8日 下午5:38:44
	* @return: Map<String, PayChannelLimitBO> 
	*/
	Map<String, PayChannelLimitBO> findSingleChannelLimit(Integer payChannelMgrId);

}
