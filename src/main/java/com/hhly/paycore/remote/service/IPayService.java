package com.hhly.paycore.remote.service;

import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyMockVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 【对外暴露hession接口】 支付相关统一api
 * @author xiongjingang
 * @date 2017年3月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface IPayService {

	/**  
	* 方法说明: 根据平台获取支付信息
	* @param platform 各个平台（PC、H5、ANDROID、IOS）
	* @time: 2017年3月6日 下午2:34:58
	* @return: ResultBO<?> 
	*/
	@Deprecated
	public ResultBO<?> findChannelByPlatform(String platform);

	/**  
	* 方法说明: 统一支付
	* @param payParam
	* @time: 2017年3月6日 下午3:37:17
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> pay(PayParamVO payParam);

	/**  
	* 方法说明: 根据token和订单号获取订单支付结果
	* @auth: xiongJinGang
	* @param payParam
	* @time: 2017年5月23日 下午8:18:44
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payResult(PayParamVO payParam);

	/**  
	* 方法说明: 退款申请
	* @param refundParam
	* @time: 2017年3月6日 下午4:07:19
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> refund(RefundParamVO refundParam);

	/**  
	* 方法说明: 查询交易记录
	* @param payQueryParamVO 查询对象
	* @time: 2017年3月6日 下午4:39:00
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO);

	/**  
	* 方法说明: 查询退款交易记录
	* @param transCode 退款交易流水号
	* @param token 用户登录token
	* @time: 2017年3月6日 下午4:40:28
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO);

	/**  
	 * 方法说明: 支付同步回调
	 * @param params 支付接口返回的参数
	 * @time: 2017年3月6日 下午4:47:18
	 * @return: ResultBO<?> 
	 */
	@Deprecated
	public ResultBO<?> payReturn(Map<String, String> params);

	/**  
	 * 方法说明: 支付异常回调
	 * @param params 支付接口返回的参数
	 * @time: 2017年3月6日 下午4:47:18
	 * @return: ResultBO<?> 
	 */
	public ResultBO<?> payNotify(Map<String, String> params);

	/**  
	* 方法说明: 退款异步回调
	* @param params 支付接口返回的参数
	* @time: 2017年3月6日 下午4:47:11
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> refundNotify(Map<String, String> params);

	/**  
	* 方法说明: 模拟回调，供测试使用
	* @auth: xiongJinGang
	* @param payNotifyMockVO
	* @time: 2017年5月12日 下午12:12:16
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payNotifyMock(PayNotifyMockVO payNotifyMockVO);

	/**  
	* 方法说明: 关闭未充值的记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2018年7月27日 下午12:03:54
	* @return: void 
	*/
	void modifyCloseOrder(TransRechargeBO transRecharge);
}
