package com.hhly.paycore.service;

import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.vo.PayChannelVO;

import java.util.List;

/**
 * @desc 支付渠道
 * @author xiongJinGang
 * @date 2017年12月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayChannelService {
	/**
	 * 获取所有支付渠道
	 *
	 * @return
	 */
	List<PayChannelBO> finAllChannel();

	/**
	 * 根据条件查询
	 *
	 * @param payChannelVO 条件对象
	 * @return List<PayChannelPO> 结果集
	 */
	List<PayChannelBO> selectByCondition(PayChannelVO payChannelVO);

	/**  
	* 方法说明: 根据银行ID获取渠道信息（支持缓存）
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年4月10日 上午9:53:03
	* @return: List<PayChannelBO> 
	*/
	List<PayChannelBO> findChannelByBankIdUseCache(Integer bankId);

	/**  
	* 方法说明: 根据主键ID查询
	* @auth: xiongJinGang
	* @param id
	* @time: 2017年12月16日 下午5:30:07
	* @return: PayChannelBO 
	*/
	PayChannelBO findChannelByIdUseCache(Integer id);

}
