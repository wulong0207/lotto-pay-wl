package com.hhly.paycore.remote.service;

import com.hhly.skeleton.pay.bo.PayBankLimitBO;

import java.util.List;

/**
 * @author lgs on
 * @version 1.0
 * @desc 【对外暴露hession接口】
 * @date 2017/3/22.
 * @company 益彩网络科技有限公司
 */
public interface IPayBankLimitService {
	/**  
	* 方法说明: 获取所有的银行支付限额【对外暴露接口】
	* @auth: xiongJinGang
	* @time: 2017年11月28日 上午10:25:16
	* @return: List<PayBankLimitBO> 
	*/
	List<PayBankLimitBO> selectAll();


}
