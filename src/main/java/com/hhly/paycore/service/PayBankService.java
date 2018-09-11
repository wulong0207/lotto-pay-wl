package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PayBankBO;

/**
 * @desc 银行service接口
 * @author xiongJinGang
 * @date 2017年4月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface PayBankService {

	/**  
	* 方法说明: 根据银行ID获取银行信息，并且验证
	* @auth: xiongJinGang
	* @param id 银行Id
	* @time: 2017年4月8日 下午5:31:23
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findBankByIdAndValidate(Integer id);

	/**  
	* 方法说明: 从缓存中获取具体的银行信息
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年4月21日 下午2:29:53
	* @return: PayBankBO 
	*/
	PayBankBO findBankFromCache(Integer bankId);

	/**  
	* 方法说明: 获取所有的银行卡信息
	* @auth: xiongJinGang
	* @time: 2017年4月21日 下午2:30:55
	* @return: List<PayBankBO> 
	*/
	List<PayBankBO> findAllBank();

	/**  
	* 方法说明: 根据银行ID查找银行信息
	* @auth: xiongJinGang
	* @param id
	* @time: 2017年5月5日 下午3:25:44
	* @return: PayBankBO 
	*/
	PayBankBO findBankById(Integer id);

	/**  
	* 方法说明: 根据支付类型获取银行
	* @auth: xiongJinGang
	* @param payType
	* @time: 2017年5月19日 下午8:01:10
	* @return: List<PayBankBO> 
	*/
	// List<PayBankBO> findBankByType(Short payType);

	/**  
	* 方法说明: 查找银行列表【根据平台和银行类型排序了的】
	* @auth: xiongJinGang
	* @param platform
	* @param bankType
	* @time: 2017年12月8日 下午7:05:26
	* @return: List<PayBankBO>
	*/
	List<PayBankBO> findBankListByPlatFromCache(String platform, Short bankType);

	/**  
	* 方法说明: 从缓存中获取单个银行信息
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年12月9日 下午12:15:32
	* @return: PayBankBO 
	*/
	PayBankBO findSigleBankFromCache(Integer bankId);
}
