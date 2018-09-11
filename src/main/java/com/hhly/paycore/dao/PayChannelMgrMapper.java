package com.hhly.paycore.dao;

import java.util.List;

import com.hhly.skeleton.pay.channel.bo.PayChannelMgrBO;

public interface PayChannelMgrMapper {

	PayChannelMgrBO selectById(Integer id);

	/**  
	* 方法说明: 获取所有的支付渠道
	* @auth: xiongJinGang
	* @time: 2017年12月12日 下午2:18:30
	* @return: List<PayChannelMgrBO> 
	*/
	List<PayChannelMgrBO> getAll();
}