package com.hhly.paycore.dao;

import com.hhly.skeleton.pay.bo.PayBankSegmentBO;

import java.util.List;

/**
 * @version 1.0
 * @auth chenkangning
 * @date 2017/3/13
 * @desc 银行卡号码段信息dao
 * @compay 益彩网络科技有限公司
 */
public interface PayBankSegmentMapper {

	/**
	 * 根据bank_id,top_cut,card_length group
	 * @return
	 */
	List<PayBankSegmentBO> selectGroup();

	/**  
	* 方法说明: 获取银行的所有附加信息
	* @auth: xiongJinGang
	* @time: 2017年4月1日 下午5:04:18
	* @return: List<PayBankSegmentBO> 
	*/
	List<PayBankSegmentBO> getList();
}
