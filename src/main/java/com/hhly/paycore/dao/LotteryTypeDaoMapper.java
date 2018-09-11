package com.hhly.paycore.dao;

import org.springframework.data.repository.query.Param;

import com.hhly.skeleton.lotto.base.lottery.bo.LotteryBO;

/**
 * @desc 彩种相关的数据接口
 * @author xiongJinGang
 * @date 2017年4月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface LotteryTypeDaoMapper {

	/**  
	* 方法说明: 前端接口：查询单个彩种信息
	* @auth: xiongJinGang
	* @param lotteryCode
	* @time: 2017年4月27日 下午5:44:05
	* @return: LotteryBO 
	*/
	LotteryBO findSingleFront(@Param("lotteryCode") Integer lotteryCode);
}