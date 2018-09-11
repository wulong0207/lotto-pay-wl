package com.hhly.paycore.dao;

import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.vo.PayChannelVO;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface PayChannelMapper {

	List<PayChannelBO> selectAll();

	/**
	 * 根据条件查询
	 *
	 * @param payChannelVO 条件对象
	 * @return list 结果集
	 */
	List<PayChannelBO> selectByCondition(PayChannelVO payChannelVO);

	/**  
	* 方法说明: 根据银行ID获取支付渠道内容
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年4月10日 上午9:46:20
	* @return: List<PayChannelBO> 
	*/
	List<PayChannelBO> getChannelByBankId(@Param("bankId") Integer bankId);

	/**  
	* 方法说明: 根据主键获取支付渠道
	* @auth: xiongJinGang
	* @param id
	* @time: 2017年12月16日 下午5:27:19
	* @return: PayChannelBO 
	*/
	PayChannelBO getChannelById(@Param("id") Integer id);

}