package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.AgentTransLogPO;
import com.hhly.skeleton.pay.agent.bo.AgentTransLogBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;

public interface AgentTransLogMapper {

	int insert(AgentTransLogPO agentTransLog);

	int insertBatch(List<AgentTransLogPO> list);

	AgentTransLogPO selectById(Integer agentId);

	AgentTransLogBO findTransDetail(@Param("agentId") Integer agentId, @Param("transCode") String transCode);

	AgentTransLogBO findAgentTransLogByTakenCode(@Param("tradeCode") String tradeCode);

	List<AgentTransLogBO> getAgentTransListByPage(AgentTransLogParamVO agentTransLogParamVO);

	Integer getAgentTransListCount(AgentTransLogParamVO agentTransLogParamVO);

	AgentTransLogBO findAgentLastTransDetail(@Param("agentId") Integer agentId);

	int updateAgentTransByBatch(List<AgentTransLogPO> list);

	int updateAgentTrans(AgentTransLogPO agentTransLogPO);
}