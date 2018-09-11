package com.hhly.paycore.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.OrderGroupVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;

/**
 * @desc 合买接口
 * @author xiongJinGang
 * @date 2018年4月28日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface OrderGroupService {

	/**  
	* 方法说明: 根据订单号获取合买订单信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2018年4月28日 上午10:24:40
	* @return: OrderGroupVO 
	*/
	OrderGroupBO findOrderGroupByOrderCode(String orderCode);

	/**  
	* 方法说明: 判断合买进度是否大于90%
	* @auth: xiongJinGang
	* @param orderGroupBO
	* @time: 2018年4月28日 上午11:45:42
	* @return: boolean 
	*/
	boolean isGreaterThan90(OrderGroupBO orderGroupBO);

	/**  
	* 方法说明: 支付成功后，参与合买；合买成功并且达到90%及以上，开始拆票；合买失败，则当作充值
	* @auth: xiongJinGang
	* @param orderCode
	* @param transRecharge
	* @param payNotifyResult
	* @throws Exception
	* @time: 2018年4月28日 下午5:46:43
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateBuyTogetherOrder(String orderCode, TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception;

	/**  
	* 方法说明: 本地支付，如果
	* @auth: xiongJinGang
	* @param orderCode
	* @param payParam
	* @param OrderBaseInfoBO
	* @param toPayEndTimeVO
	* @throws Exception
	* @time: 2018年5月2日 上午11:43:26
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateBuyTogetherOrderByLocal(String orderCode, PayParamVO payParam, OrderBaseInfoBO orderBaseInfo, ToPayEndTimeVO toPayEndTimeVO) throws Exception;

	/**  
	* 方法说明: 合买订单未满员，平台来保底
	* @auth: xiongJinGang
	* @param orderGroupVO
	* @throws Exception
	* @time: 2018年5月4日 下午3:29:43
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateOrderGroupByPlatform(OrderGroupVO orderGroupVO) throws Exception;

	/**  
	* 方法说明: 判断合买金额是否大于90%
	* @auth: xiongJinGang
	* @param orderInfo
	* @param orderGroupBO
	* @time: 2018年8月1日 下午5:02:00
	* @return: boolean 
	*/
	boolean isGreaterThan90(OrderInfoBO orderInfo, OrderGroupBO orderGroupBO);

}
