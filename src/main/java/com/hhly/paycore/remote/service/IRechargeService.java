package com.hhly.paycore.remote.service;

import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.vo.AgentPayVO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.PayNotifyMockVO;
import com.hhly.skeleton.pay.vo.RechargeParamVO;

/**
 * @desc 【对外暴露hession接口】 充值统一接口
 * @author xiongJinGang
 * @date 2017年4月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface IRechargeService {

	/**  
	* 方法说明: 充值
	* @auth: xiongJinGang
	* @param rechargeParam
	* @time: 2017年4月8日 下午3:13:48
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> recharge(RechargeParamVO rechargeParam);

	/**  
	* 方法说明: 充值结果查询
	* @auth: xiongJinGang
	* @param rechargeParam
	* @time: 2017年5月23日 下午9:14:33
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> rechargeResult(RechargeParamVO rechargeParam);

	/**  
	* 方法说明: 充值同步回调
	* @auth: xiongJinGang
	* @param params
	* @time: 2017年4月8日 下午3:14:06
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> rechargeReturn(Map<String, String> params);

	/**  
	* 方法说明: 充值异步回调
	* @auth: xiongJinGang
	* @param params
	* @time: 2017年4月8日 下午3:14:08
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	public ResultBO<?> rechargeNotify(Map<String, String> params) throws Exception;

	/**  
	* 方法说明: 模拟充值回调
	* @auth: xiongJinGang
	* @param payNotifyMockVO
	* @throws Exception
	* @time: 2017年5月25日 下午5:42:06
	* @return: ResultBO<?> 
	*/
	ResultBO<?> rechargeNotifyMock(PayNotifyMockVO payNotifyMockVO) throws Exception;

	/**  
	* 方法说明: 提供给CMS人工充值（充值现金、充值红包）
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @time: 2017年7月6日 下午5:26:47
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateRecharge(CmsRechargeVO cmsRecharge);

	/**  
	* 方法说明: 代理系统充值
	* @auth: xiongJinGang
	* @param agentPayVO
	* @throws Exception
	* @time: 2017年11月3日 下午4:17:12
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> agentRecharge(AgentPayVO agentPayVO) throws Exception;

	/**  
	* 方法说明: 充值活动：CMS赠送红包后，调用该接口扣除现金，生成彩金红包及交易流水
	* @auth: xiongJinGang
	* @param cmsRecharge用以下4个参数：userId、rechargeAmount、rechargeCode、activityCode
	* @time: 2017年8月21日 下午5:41:24
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	ResultBO<?> updateWalletToBuyRedColorForCms(CmsRechargeVO cmsRecharge) throws Exception;

	/**  
	* 方法说明: 充值活动：供MS调用，首充活动【将活动编号更新到充值记录，将充值金额转成彩金】
	* @auth: xiongJinGang
	* @param cmsRecharge 对象中必填参数：userId, rechargeAmount, rechargeCode, activityCode
	* @throws Exception
	* @time: 2018年3月6日 上午10:34:08
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateWalletFirstRechargeForCms(CmsRechargeVO cmsRecharge) throws Exception;

	/**  
	* 方法说明: 代理现金充值
	* @auth: xiongJinGang
	* @param agentPayVO
	* @throws Exception
	* @time: 2018年4月8日 下午12:15:43
	* @return: ResultBO<?> 
	*/
	ResultBO<?> agentRechargeCash(AgentPayVO agentPayVO) throws Exception;

}
