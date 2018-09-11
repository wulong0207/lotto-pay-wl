package com.hhly.paycore.service;

import java.util.List;

import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * @desc 订单支付完后，更新支付状态
 * @author xiongJinGang
 * @date 2017年3月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayOrderUpdateService {
	/**  
	* 方法说明: 处理订单的支付状态
	* @auth: xiongJinGang
	* @param payOrderUpdatePO
	* @param payOrderBaseInfoVO
	* @time: 2017年3月27日 上午10:48:59
	* @return: int 
	* @throws Exception 
	*/
	public int dealOrderPayStatus(PayOrderUpdatePO payOrderUpdatePO, PayOrderBaseInfoVO payOrderBaseInfoVO) throws Exception;

	/**  
	* 方法说明: 获取订单信息并且验证订单是否支付
	* @auth: xiongJinGang
	* @param orderCode
	* @param buyType
	* @time: 2017年4月8日 下午5:25:45
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	ResultBO<?> findOrderAndValidate(String orderCode, Integer buyType) throws Exception;

	/**  
	* 方法说明: 批量查询订单信息
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月11日 下午12:01:37
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	ResultBO<?> findOrderAndValidate(List<PayOrderBaseInfoVO> list) throws Exception;

	/**  
	* 方法说明: 批量更新订单状态
	* @auth: xiongJinGang
	* @param orderTotalList
	* @param payNotifyResult
	* @param transRecharge
	* @throws Exception
	* @time: 2017年5月11日 下午5:29:28
	* @return: int 
	*/
	int updateOrderBatch(List<PayOrderBaseInfoVO> orderTotalList, PayNotifyResultVO payNotifyResult, TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明: 根据订单号获取订单信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年4月26日 上午10:34:50
	* @return: PayOrderBaseInfoVO 
	*/
	PayOrderBaseInfoVO findOrderInfo(String orderCode);

	PayOrderBaseInfoVO findOrderAdded(String orderCode) throws Exception;

	/**  
	* 方法说明: 根据订单号查询不同的库
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年5月26日 下午7:27:36
	* @return: PayOrderBaseInfoVO 
	*/
	PayOrderBaseInfoVO findOrderBoth(String orderCode);

	/**  
	* 方法说明: 修改未支付订单数
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月31日 下午4:49:38
	* @return: void 
	*/
	void subNoPayOrderNum(List<PayOrderBaseInfoVO> list);

	/**  
	* 方法说明: 更新订单支付状态为支付中
	* @auth: xiongJinGang
	* @param orderTotalList
	* @throws Exception
	* @time: 2017年6月19日 下午6:21:32
	* @return: int 
	*/
	int updateOrderPayingBatch(List<OrderBaseInfoBO> orderList) throws Exception;

}
