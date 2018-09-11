package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelMgrBO;
import com.hhly.skeleton.pay.channel.vo.ChannelMgrVO;

/**
 * @desc 支付渠道管理
 * @author xiongJinGang
 * @date 2017年12月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayChannelMgrService {

	/**  
	* 方法说明: 获取支付渠道列表
	* @auth: xiongJinGang
	* @time: 2017年12月12日 下午2:25:49
	* @return: List<PayChannelMgrBO> 
	*/
	List<PayChannelMgrBO> findChannelMgrList();

	/**  
	* 方法说明: 从缓存中获取支付渠道列表
	* @auth: xiongJinGang
	* @time: 2017年12月12日 下午2:26:03
	* @return: List<PayChannelMgrBO> 
	*/
	List<PayChannelMgrBO> findChannelMgrListFromCache();

	/**  
	* 方法说明: 获取当前平台正在使用的渠道
	* @auth: xiongJinGang
	* @time: 2017年12月12日 下午2:46:26
	* @param mgrId 渠道管理ID 
	* @return: ChannelMgrVO
	*/
	ChannelMgrVO findInUseChannel(Integer mgrId);

	/**  
	* 方法说明: 验证是否有可用的支付渠道及单笔限额
	* @auth: xiongJinGang
	* @param payAmount
	* @time: 2017年12月12日 下午4:04:31
	* @return: ResultBO<?> 
	*/
	ResultBO<?> validateChannel(Double payAmount, Short transType);

	/**  
	* 方法说明: 根据渠道管理ID获取渠道
	* @auth: xiongJinGang
	* @param mgrId
	* @time: 2017年12月12日 下午6:13:58
	* @return: PayChannelMgrBO 
	*/
	PayChannelMgrBO findChannelMgrById(Integer mgrId);

	/**  
	* 方法说明: 根据渠道编号查找
	* @auth: xiongJinGang
	* @param code
	* @time: 2017年12月15日 下午5:37:08
	* @return: PayChannelMgrBO 
	*/
	PayChannelMgrBO findChannelMgrByCode(String code);

}
