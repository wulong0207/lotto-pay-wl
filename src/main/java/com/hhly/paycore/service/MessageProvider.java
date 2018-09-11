package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * 发送消息到队列
 *
 * @author wulong
 * @create 2017/5/11 17:14
 */
public interface MessageProvider {
	/**
	 * 发送消息
	 * @param queueKey 队列名
	 * @param message  消息
	 */
	public void sendMessage(String queueKey, Object message);

	/**  
	* 方法说明: 发送方案进度
	* @auth: xiongJinGang
	* @param orderCode
	* @param status
	* @time: 2017年6月5日 下午2:23:52
	* @return: void 
	*/
	// void sendFlowMessage(String orderCode, String payTime, int status);

	/**  
	* 方法说明: 追号订单生成订单的队列
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年6月16日 下午2:20:36
	* @return: void 
	*/
	void sendOrderAddMessage(List<PayOrderBaseInfoVO> list);

	/**  
	* 方法说明: 发送充值消息
	* @auth: xiongJinGang
	* @param userId
	* @param rechargeCode
	* @time: 2017年8月9日 下午4:00:43
	* @return: void 
	*/
	void sendRechargeMessage(Integer userId, String rechargeCode);

	/**  
	* 方法说明: 发送红包到账信息
	* @auth: xiongJinGang
	* @param userId
	* @param redCode
	* @time: 2017年8月9日 下午4:12:13
	* @return: void 
	*/
	void sendRedMessage(Integer userId, String redCode);

	/**  
	* 方法说明: 发送充值活动消息
	* @auth: xiongJinGang
	* @param activityCode
	* @param transCode
	* @time: 2017年8月14日 下午12:06:52
	* @return: void 
	*/
	void sendRechargeActivityMessage(String activityCode, String transCode, String activityPage);

	/**  
	* 方法说明: 大单预警 
	* @auth: xiongJinGang
	* @param alarmList
	* @time: 2017年8月11日 下午5:49:37
	* @return: void 
	*/
	void sendAlarmMessage(List<PayOrderBaseInfoVO> alarmList);

	/**  
	* 方法说明: 代购、追号订单方案详情
	* @auth: xiongJinGang
	* @param orderCode
	* @param payTime
	* @param status
	* @param buyType
	* @time: 2017年8月18日 上午11:03:13
	* @return: void 
	*/
	void sendOrderFlowMessage(String orderCode, String payTime, Short status, Short buyType);

	/**  
	* 方法说明: 自动审核异常mq
	* @auth: xiongJinGang
	* @param takenCode
	* @time: 2018年3月7日 下午5:15:30
	* @return: void 
	*/
	void sendAutoCheckMessage(String takenCode);
}
