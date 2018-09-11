package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.AgentTransTakenPO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;

public interface AgentTransTakenMapper {

	int addTaken(AgentTransTakenPO agentTransTakenPO);

	AgentTransTakenBO findTransTakenDetail(@Param("agentId") Integer agentId, @Param("transTakenCode") String transTakenCode);

	AgentTransTakenBO findTransTakenDetailByCode(@Param("transTakenCode") String transTakenCode);

	AgentTransTakenBO findAgentLastTakenDetail(@Param("agentId") Integer agentId);

	/**  
	* 方法说明: 批量更新提款记录
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年8月7日 上午11:06:12
	* @return: int 
	*/
	int updateTakenByBatch(@Param("list") List<AgentTransTakenPO> list);

	int updateTaken(AgentTransTakenPO agentTransTaken);

}