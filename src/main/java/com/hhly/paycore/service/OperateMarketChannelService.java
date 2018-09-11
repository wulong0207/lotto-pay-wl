package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.pay.channel.bo.OperateMarketChannelBO;

/**
 * @desc 市场渠道接口
 * @author xiongJinGang
 * @date 2018年1月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface OperateMarketChannelService {

	/**  
	* 方法说明: 从缓存中获取市场渠道
	* @auth: xiongJinGang
	* @time: 2018年1月27日 上午11:38:14
	* @return: List<OperateMarketChannelBO> 
	*/
	List<OperateMarketChannelBO> findMarketListFromCache();

	/**  
	* 方法说明: 根据渠道ID获取渠道信息
	* @auth: xiongJinGang
	* @param channelId
	* @time: 2018年1月27日 上午11:38:25
	* @return: OperateMarketChannelBO 
	*/
	OperateMarketChannelBO findSingleMarket(String channelId);

	/**  
	* 方法说明: 验证是否为马甲包
	* @auth: xiongJinGang
	* @param channelId
	* @time: 2018年1月27日 上午11:49:57
	* @return: boolean 
	*/
	boolean isMajia(String channelId);
}
