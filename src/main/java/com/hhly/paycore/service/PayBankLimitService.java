package com.hhly.paycore.service;

import com.hhly.skeleton.pay.bo.PayBankLimitBO;

/**
 * @author lgs on
 * @version 1.0
 * @desc
 * @date 2017/3/22.
 * @company 益彩网络科技有限公司
 */
public interface PayBankLimitService {

	/**  
	* 方法说明: 根据银行ID获取银行支付限额
	* @auth: xiongJinGang
	* @param bankId 银行ID
	* @param bankType 银行卡类型1储蓄卡2信用卡
	* @time: 2017年4月7日 上午10:59:33
	* @return: PayBankLimitBO 
	*/
	PayBankLimitBO findPayBankLimitByBankIdFromCache(Integer bankId, Short bankType);
}
