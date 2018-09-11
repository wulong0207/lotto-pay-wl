package com.hhly.paycore.service;

import java.util.List;

import com.hhly.paycore.po.AgentTransLogPO;
import com.hhly.paycore.po.AgentTransTakenPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.AgentConstants.AgentWalletTransTypeEnum;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransLogBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;
import com.hhly.skeleton.pay.agent.vo.ConfirmTakenResultVO;
import com.hhly.skeleton.pay.agent.vo.WithdrawApplyVO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;

/**
 * @desc 代理核心业务
 * @author xiongJinGang
 * @date 2018年3月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface AgentCoreService {

	/**  
	* 方法说明: 用户ID
	* @auth: xiongJinGang
	* @param userId
	* @time: 2018年3月7日 下午2:39:27
	* @return: AgentInfoBO 
	*/
	AgentInfoBO findAgentInfo(Integer userId);

	/**  
	* 方法说明: 更新代理账户的钱包（加了分布式锁）
	* @auth: xiongJinGang
	* @param agentId
	* @param updateAmount
	* @time: 2018年3月5日 上午10:21:20
	* @return: Integer 
	*/
	ResultBO<?> updateAgentWallet(Integer agentId, Double updateAmount, AgentWalletTransTypeEnum agentWalletTransTypeEnum);

	/**  
	* 方法说明: 添加代理提款交易流水
	* @auth: xiongJinGang
	* @param confirmTakenResult
	* @throws Exception
	* @time: 2018年3月5日 下午5:34:48
	* @return: AgentTransLogPO 
	*/
	AgentTransLogPO addTransLog(ConfirmTakenResultVO confirmTakenResult) throws Exception;

	/**  
	* 方法说明: 添加代理转账交易流水
	* @auth: xiongJinGang
	* @param agentInfo
	* @param updateAmount
	* @param account
	* @throws Exception
	* @time: 2018年3月5日 下午5:35:03
	* @return: AgentTransLogPO 
	*/
	AgentTransLogPO addTransLog(AgentInfoBO agentInfo, Double updateAmount, String account, String memberTransUserCode) throws Exception;

	/**  
	* 方法说明: 添加代理结算佣金交易流水
	* @auth: xiongJinGang
	* @param agentInfo
	* @param updateAmount
	* @throws Exception
	* @time: 2018年3月7日 上午9:52:53
	* @return: AgentTransLogPO 
	*/
	AgentTransLogPO addTransLog(AgentInfoBO agentInfo, Double updateAmount) throws Exception;

	/**  
	* 方法说明: 获取代理交易详情
	* @auth: xiongJinGang
	* @param agentId 代理ID
	* @param transCode 交易编号
	* @time: 2018年3月8日 上午9:43:15
	* @return: AgentTransLogBO 
	*/
	AgentTransLogBO findAgentTransLogDetail(Integer agentId, String transCode);

	/**  
	* 方法说明: 获取代理提款详情
	* @auth: xiongJinGang
	* @param agentId 代理ID 
	* @param takenCode 提款编号
	* @time: 2018年3月8日 上午9:46:01
	* @return: AgentTransTakenBO 
	*/
	AgentTransTakenBO findAgentTakenDetail(Integer agentId, String takenCode);

	AgentTransTakenBO findAgentTakenDetailByCode(String takenCode);

	/**  
	* 方法说明: 添加代理提款流水
	* @auth: xiongJinGang
	* @param withdrawApply 申请提款请求参数
	* @param confirmTakenResult 确认提款结果
	* @param agentInfo 代理用户信息
	* @param bankCard 银行卡信息
	* @param transTakenCode
	* @time: 2018年3月5日 下午5:31:15
	* @return: void 
	*/
	AgentTransTakenPO addAgentTaken(WithdrawApplyVO withdrawApply, ConfirmTakenResultVO confirmTakenResult, AgentInfoBO agentInfo, PayBankcardBO bankCard) throws Exception;

	/**  
	* 方法说明: 获取交易列表数量
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @time: 2018年3月8日 上午10:00:05
	* @return: Integer 
	*/
	Integer findAgentTransListCount(AgentTransLogParamVO agentTransLogParam);

	/**  
	* 方法说明: 分页获取交易记录
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @time: 2018年3月8日 上午10:00:18
	* @return: List<AgentTransLogBO> 
	*/
	List<AgentTransLogBO> findAgentTransListByPage(AgentTransLogParamVO agentTransLogParam);

	/**  
	* 方法说明: 查找代理最后一次提现记录
	* @auth: xiongJinGang
	* @param agentId
	* @time: 2018年3月10日 下午4:04:04
	* @return: AgentTransTakenBO 
	*/
	AgentTransTakenBO findAgentLastTakenDetail(Integer agentId);

	/**  
	* 方法说明: 批量更新提款状态
	* @auth: xiongJinGang
	* @param list
	* @time: 2018年3月10日 下午4:37:03
	* @return: int 
	*/
	int updateTakenByBatch(List<AgentTransTakenPO> list);

	/**  
	* 方法说明: 根据提款编号获取代理交易记录
	* @auth: xiongJinGang
	* @param tradeCode
	* @time: 2018年3月10日 下午5:01:14
	* @return: AgentTransLogBO 
	*/
	AgentTransLogBO findAgentTransLogByTakenCode(String tradeCode);

	/**  
	* 方法说明: 批量添加流水记录
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2018年3月10日 下午5:39:23
	* @return: int 
	*/
	int addTransLog(List<AgentTransLogPO> list) throws Exception;

	/**  
	* 方法说明: 批量更新代理交易流水状态 
	* @auth: xiongJinGang
	* @param list
	* @time: 2018年3月10日 下午5:55:30
	* @return: int 
	*/
	int updateTransUserByBatch(List<AgentTransLogPO> list);

	/**  
	* 方法说明: 查找指定代理最后一条返佣流水
	* @auth: xiongJinGang
	* @param agentId
	* @time: 2018年3月13日 下午3:50:14
	* @return: AgentTransLogBO 
	*/
	AgentTransLogBO findAgentLastTransDetail(Integer agentId);

}
