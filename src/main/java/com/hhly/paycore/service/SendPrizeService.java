package com.hhly.paycore.service;

import com.hhly.skeleton.base.bo.ResultBO;

/**
 * @desc 派奖服务接口
 * @author xiongJinGang
 * @date 2017年9月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface SendPrizeService {
	/**  
	* 方法说明: 派奖
	* @auth: xiongJinGang
	* @param orderCode
	* @throws Exception
	* @time: 2017年9月7日 下午5:13:24
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateSendPrize(String orderCode) throws Exception;

	/**  
	* 方法说明: 重置派奖
	* @auth: xiongJinGang
	* @param orderCode
	* @throws Exception
	* @time: 2017年9月7日 下午5:16:12
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateResetSendPrize(String orderCode) throws Exception;
}
