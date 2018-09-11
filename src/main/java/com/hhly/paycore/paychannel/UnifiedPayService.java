/**    
* @Title: UnifiedPayService.java  
* @Package com.hhly.paycore.api  
* @Description: TODO
* @author xiongjingang 
* @date 2017年3月6日 下午6:54:17  
* @version V1.0    
*/
package com.hhly.paycore.paychannel;

import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;

/**
 * @desc 统一支付接口
 * @author xiongjingang
 * @date 2017年3月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface UnifiedPayService {

	/**  
	* 方法说明: 统一支付
	* @param paymentInfo 支付请求参数
	* @time: 2017年3月6日 下午3:37:17
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> pay(PaymentInfoBO paymentInfo);

	/**  
	* 方法说明: 退款申请
	* @param refundParam
	* @time: 2017年3月6日 下午4:07:19
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> refund(RefundParamVO refundParam);

	/**  
	* 方法说明: 查询交易记录
	* @param payQueryParamVO 交易查询参数
	* @time: 2017年3月6日 下午4:39:00
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payQuery(PayQueryParamVO payQueryParamVO);

	/**  
	* 方法说明: 查询退款交易记录
	* @param transCode 退款交易流水号
	* @time: 2017年3月6日 下午4:40:28
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> refundQuery(PayQueryParamVO payQueryParamVO);

	/**  
	* 方法说明: 异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年3月23日 下午2:16:13
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payNotify(Map<String, String> map);

	/**  
	* 方法说明: 同步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年3月23日 下午2:16:13
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> payReturn(Map<String, String> map);
	
	/** 
	* @Title: queryBill 
	* @Description: 查询对账单
	*  @param map
	*  @return ResultBO<?>
	* @time 2017年4月10日 上午9:56:45
	*/
	public ResultBO<?> queryBill(Map<String, String> map);
}
