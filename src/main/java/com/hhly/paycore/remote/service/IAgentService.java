package com.hhly.paycore.remote.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;
import com.hhly.skeleton.pay.agent.vo.TransferAccountsVO;
import com.hhly.skeleton.pay.agent.vo.WithdrawApplyVO;

/**
 * @desc 代理相关操作
 * @author xiongJinGang
 * @date 2018年3月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface IAgentService {
	/**  
	* 方法说明: 申请提现
	* @auth: xiongJinGang
	* @param withdrawApply
	* @time: 2018年3月2日 下午4:31:21
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> applyWithdraw(WithdrawApplyVO withdrawApply) throws Exception;

	/**  
	* 方法说明: 确认提现
	* @auth: xiongJinGang
	* @param withdrawApply
	* @time: 2018年3月3日 下午4:11:01
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateConfirmWithdraw(WithdrawApplyVO withdrawApply) throws Exception;

	/**  
	* 方法说明: 代理给指定会员转账
	* @auth: xiongJinGang
	* @param transferAccounts
	* @throws Exception
	* @time: 2018年3月5日 上午11:39:43
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateTransAccount(TransferAccountsVO transferAccounts) throws Exception;

	/**  
	* 方法说明: app的交易流水
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @throws Exception
	* @time: 2018年3月5日 下午4:38:46
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findAppTransUserByPage(AgentTransLogParamVO agentTransLogParam) throws Exception;

	/**  
	* 方法说明: 查询交易详情
	* @auth: xiongJinGang
	* @param agentTransLogParam
	* @throws Exception
	* @time: 2018年3月5日 下午5:06:36
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findTransDetail(AgentTransLogParamVO agentTransLogParam) throws Exception;

	/**  
	* 方法说明: 【hession接口】给代理结算佣金
	* @auth: xiongJinGang
	* @param agentId 代理ID
	* @param updateAmount 结算金额
	* @throws Exception
	* @time: 2018年3月7日 上午9:55:50
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateAgentWalletAmount(Integer agentId, Double updateAmount) throws Exception;

	/**  
	* 方法说明: 获取代理用户信息
	* @auth: xiongJinGang
	* @param token
	* @time: 2018年3月7日 下午2:40:24
	* @return: AgentInfoBO 
	*/
	AgentInfoBO findAgentInfo(String token);

	/**  
	* 方法说明: 批量更新提款状态【供CMS后台审核】
	* @auth: xiongJinGang
	* @param list；trans_taken_code、review_by、trans_status、agent_id必填；trans_fail_info为审核不通过时，必填
	* @param operateType；操作类型，1审核、2提交银行、3银行处理结果、4CMS确认完成，参考 TakenOperateTypeEnum
	* @throws Exception
	* @time: 2018年3月10日 下午5:56:39
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateTakenStatusByBatch(List<AgentTransTakenBO> list, Short operateType) throws Exception;
}
