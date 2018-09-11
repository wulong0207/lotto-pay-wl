package com.hhly.paycore.remote.service;

import java.util.List;

import com.hhly.skeleton.pay.bo.UserPayTypeBO;
import com.hhly.skeleton.pay.vo.ToRechargeParamVO;

/**
 * @desc 【对外暴露hession接口】支付渠道管理
 * @author xiongJinGang
 * @date 2017年12月13日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface IPayChannelService {

	/**  
	* 方法说明: 获取用户的支付渠道
	* @auth: xiongJinGang
	* @param toRechargeParamVO
	* @time: 2018年2月27日 下午6:26:05
	* @return: List<UserPayTypeBO> 
	*/
	List<UserPayTypeBO> findUserPayTypes(ToRechargeParamVO toRechargeParamVO);

}
