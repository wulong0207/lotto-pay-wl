package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.skeleton.pay.channel.bo.PayChannelLimitBO;

/**
 * @desc 渠道限额
 * @author xiongJinGang
 * @date 2017年12月14日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayChannelLimitMapper {

	List<PayChannelLimitBO> selectById(@Param("payChannelMgrId") Integer id);

	/**  
	* 方法说明: 获取所有的支付限额
	* @auth: xiongJinGang
	* @time: 2017年12月8日 下午4:11:38
	* @return: List<PayChannelLimitBO> 
	*/
	List<PayChannelLimitBO> getAll();
}