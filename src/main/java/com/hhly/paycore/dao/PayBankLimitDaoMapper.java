package com.hhly.paycore.dao;

import com.hhly.skeleton.pay.bo.PayBankLimitBO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PayBankLimitDaoMapper {

	List<PayBankLimitBO> selectAll();

	/**  
	* 方法说明: 根据银行ID和银行卡类型获取限额
	* @auth: xiongJinGang
	* @param bankId 银行ID
	* @param bankType 银行卡类型。1储蓄卡 2信用卡
	* @time: 2017年4月7日 上午11:03:19
	* @return: PayBankLimitBO 
	*/
	PayBankLimitBO getByBankIdAndType(@Param("bankId") Integer bankId, @Param("bankType") Short bankType);

	/**  
	* 方法说明: 根据银行ID获取限额
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年6月21日 上午11:09:04
	* @return: PayBankLimitBO 
	*/
	List<PayBankLimitBO> getPayBankLimitByBankId(@Param("bankId") Integer bankId);

}