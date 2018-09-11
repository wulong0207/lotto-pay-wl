package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PayBankSegmentBO;

/**
 * @desc 银行卡号码段信息
 * @author xiongJinGang
 * @date 2017年4月1日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface BankcardSegmentService {
	/**  
	* 方法说明: 获取所有银行卡号段信息
	* @auth: xiongJinGang
	* @time: 2017年4月1日 下午5:10:42
	* @return: List<PayBankSegmentBO> 
	*/
	List<PayBankSegmentBO> findList();

	/**  
	* 方法说明: 获取指定银行卡的附加信息
	* @auth: xiongJinGang
	* @param bankCard
	* @time: 2017年4月1日 下午5:24:16
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findPayBankSegmentByCard(String bankCard);

	/**  
	* 方法说明: 获取银行的唯一编码
	* @auth: xiongJinGang
	* @param bankCard
	* @time: 2017年4月10日 下午3:55:18
	* @return: String 
	*/
	String findBankSegmentCodeByCard(String bankCard);
}
