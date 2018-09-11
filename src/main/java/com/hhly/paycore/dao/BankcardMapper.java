package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.PayBankcardPO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.vo.PayBankcardVO;

public interface BankcardMapper {

	/**
	 * 查询用户银行卡信息
	 * @param payBankcardVO 数据对象
	 * @return
	 */
	List<PayBankcardBO> selectBankCard(PayBankcardVO payBankcardVO);

	/**  
	* 方法说明: 获取用户可用银行卡信息
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年12月8日 上午11:54:57
	* @return: List<PayBankcardBO> 
	*/
	List<PayBankcardBO> getUserBankList(@Param("userId") Integer userId);

	/**  
	* 方法说明: 根据银行卡ID获取用户银行卡信息
	* @auth: xiongJinGang
	* @param userId
	* @param bankCardId
	* @time: 2017年4月8日 下午4:39:42
	* @return: PayBankcardBO 
	*/
	PayBankcardBO getUserBankById(@Param("userId") Integer userId, @Param("id") Integer bankCardId);

	/**  
	* 方法说明: 根据银行卡号获取用户的银行信息
	* @auth: xiongJinGang
	* @param userId
	* @param cardcode
	* @time: 2017年7月26日 下午4:02:30
	* @return: PayBankcardBO 
	*/
	PayBankcardBO getUserBankByCode(@Param("userId") Integer userId, @Param("cardCode") String cardcode);

	/**
	 * 更新银行卡信息
	 * @param payBankcardPO
	 * @return
	 */
	int updateByBankCardId(@Param("record") PayBankcardPO payBankcardPO);

	/**  
	* 方法说明: 更新银行名称
	* @auth: xiongJinGang
	* @param payBankcardPO
	* @time: 2017年5月4日 下午6:39:01
	* @return: int 
	*/
	int updateBankName(PayBankcardPO payBankcardPO);

	List<PayBankcardBO> selectAll();

	List<PayBankcardBO> findPayBankCardByUserId(@Param("userId") Integer userId, @Param("orderBy") String orderBy);

	/**
	* 设置默认银行卡
	* @param payBankcardPO 参数
	* @return 影响行数
	*/
	int updateDefault(PayBankcardPO payBankcardPO);

	/**
	 * 取消其它银行卡为默认卡
	 * @param payBankcardPO 参数
	 * @return 影响行数
	 */
	int updateDisableDefault(PayBankcardPO payBankcardPO);
}
