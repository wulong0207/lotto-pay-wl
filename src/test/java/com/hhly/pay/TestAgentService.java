package com.hhly.pay;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;

import com.hhly.paycore.remote.service.IAgentService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;

public class TestAgentService extends BaseTest {

	@Resource
	private IAgentService agentService;

	/**  
	* 方法说明: 退款
	* @auth: xiongJinGang
	* @time: 2017年6月7日 上午10:46:32
	* @return: void 
	 * @throws Exception 
	*/
	@Test
	public void refundTest() throws Exception {
		List<AgentTransTakenBO> list = new ArrayList<AgentTransTakenBO>();
		AgentTransTakenBO agentTransTakenBO = new AgentTransTakenBO();
		// trans_taken_code、review_by、trans_status、agent_id
		agentTransTakenBO.setTransTakenCode("O18031319160516600007");
		agentTransTakenBO.setReviewBy("系统");
		// 提现交易状态；1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
		agentTransTakenBO.setTransStatus((short) 2);
		list.add(agentTransTakenBO);

		/*AgentTransTakenBO agentTransTakenBO1 = new AgentTransTakenBO();
		// trans_taken_code、review_by、trans_status、agent_id
		agentTransTakenBO1.setAgentId(null);
		agentTransTakenBO1.setTransTakenCode("O18031411272016600021");
		agentTransTakenBO1.setReviewBy("系统");
		// 提现交易状态；1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
		agentTransTakenBO1.setTransStatus((short) 4);
		list.add(agentTransTakenBO1);*/

		// 操作类型，1审核、2提交银行、3银行处理结果、4CMS确认完成，参考 TakenOperateTypeEnum
		ResultBO<?> resultBO = agentService.updateTakenStatusByBatch(list, (short) 1);
		System.out.println("审核返回：" + resultBO.getMessage());
	}

	@Test
	public void tokenTest() {
		// 支付结果
		// redisUtil.addString("PAY_STATUS_RESULT_619_O1706031555411000010", "success", CacheConstants.FIFTEEN_MINUTES);
	}

}
