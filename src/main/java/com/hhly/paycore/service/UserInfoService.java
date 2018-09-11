package com.hhly.paycore.service;

import com.hhly.paycore.po.UserInfoPO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.user.bo.UserInfoBO;

/**
 * 用户个人信息接口 （远程服务）
 * @desc
 * @author zhouyang
 * @date 2017年3月4日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface UserInfoService {

	/**  
	* 方法说明: 更新用户的最后一次支付ID
	* @auth: xiongJinGang
	* @param userInfoPO
	* @throws Exception
	* @time: 2017年5月15日 下午8:30:28
	* @return: int 
	*/
	int updateLastUsePayId(UserInfoPO userInfoPO) throws Exception;

	/**  
	* 方法说明: 更新最后使用银行ID和银行卡号
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年11月9日 下午5:13:34
	* @return: void 
	*/
	void updateLastBankCard(TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明: 根据用户ID获取用户信息
	* @auth: xiongJinGang
	* @param userId
	* @throws Exception
	* @time: 2017年11月15日 下午4:44:31
	* @return: UserInfoBO 
	*/
	UserInfoBO findUserInfo(Integer userId) throws Exception;

	/**  
	* 方法说明: 从缓存中获取用户基本信息
	* @auth: xiongJinGang
	* @param userId
	* @throws Exception
	* @time: 2017年11月15日 下午4:54:42
	* @return: UserInfoBO 
	*/
	UserInfoBO findUserInfoFromCache(Integer userId) throws Exception;

	/**  
	* 方法说明: 根据用户账户获取用户基本信息
	* @auth: xiongJinGang
	* @param accountName
	* @throws Exception
	* @time: 2018年3月5日 上午9:53:17
	* @return: UserInfoBO 
	*/
	UserInfoBO findUserByAccountName(String accountName) throws Exception;

}
