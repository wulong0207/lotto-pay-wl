package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.skeleton.pay.bo.PayBankBO;

public interface PayBankMapper {
	/**  
	* 方法说明:获取所有银行信息 
	* @auth: xiongJinGang
	* @time: 2017年4月8日 下午4:11:05
	* @return: List<PayBankBO> 
	*/
	List<PayBankBO> getAll();

	/**  
	* 方法说明: 根据银行ID获取银行信息
	* @auth: xiongJinGang
	* @param id
	* @time: 2017年4月8日 下午4:10:09
	* @return: PayBankBO 
	*/
	PayBankBO getBankById(@Param("id") Integer id);

	/**  
	* 方法说明: 获取每个端银行的排序结果
	* @auth: xiongJinGang
	* @param orderBy
	* @time: 2017年12月8日 上午11:48:59
	* @return: List<PayBankBO> 
	*/
	List<PayBankBO> getSortBankByPlatform(@Param("orderBy") String orderBy, @Param("payType") Short payType);

}