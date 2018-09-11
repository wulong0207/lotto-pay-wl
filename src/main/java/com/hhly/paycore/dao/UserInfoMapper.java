package com.hhly.paycore.dao;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.UserInfoPO;
import com.hhly.skeleton.user.bo.UserInfoBO;

/**
 * 用户基本信息mapper接口
 * @desc
 * @author zhouyang
 * @date 2017年3月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface UserInfoMapper {

	/**
	 * 修改用户信息
	 * @param userInfoPO
	 * @return
	 */
	int updateUserInfo(UserInfoPO userInfoPO);

	/**  
	* 方法说明: 获取用户基本信息
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年11月15日 下午4:19:29
	* @return: UserInfoBO 
	*/
	UserInfoBO getUserInfo(@Param("id") Integer userId);

	/**  
	* 方法说明: 根据账户获取用户信息
	* @auth: xiongJinGang
	* @param accountName
	* @time: 2018年3月5日 上午9:50:28
	* @return: UserInfoBO 
	*/
	UserInfoBO getUserByAccountName(@Param("accountName") String accountName);

}
