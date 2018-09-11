package com.hhly.paycore.dao;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.AgentInfoPO;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;

/**
 * @desc 代理相关
 * @author xiongJinGang
 * @date 2018年3月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface AgentInfoMapper {
	AgentInfoBO selectAgentInfo(Integer id);

	AgentInfoBO selectAgentInfoByUserId(Integer userId);

	Integer updateAgent(AgentInfoPO agentInfoPO);

	/**  
	* 方法说明: 代理用户钱包金额累加
	* @auth: xiongJinGang
	* @param agentInfoPO
	* @time: 2018年3月3日 下午12:15:57
	* @return: Integer 
	*/
	Integer updateAgentWalletIncome(@Param("id") Integer id, @Param("subAmount") Double subAmount);

	Integer updateAgentWalletOut(@Param("id") Integer id, @Param("subAmount") Double subAmount);
}