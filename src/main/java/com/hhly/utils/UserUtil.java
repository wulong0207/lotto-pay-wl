package com.hhly.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.exception.Assert;
import com.hhly.skeleton.user.bo.UserInfoBO;

/**
 * 用户信息工具类
 *
 * @author huangchengfang1219
 * @date 2018年3月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Component("userUtil")
public class UserUtil {

	@Autowired
	private RedisUtil redisUtil;

	/**
	 * 从缓存中获取用户ID
	 * 
	 * @param token
	 * @return
	 */
	public Integer getUserIdByToken(String token) {
		return (Integer) redisUtil.getObj(getTokenKey(token));
	}

	/**
	 * 根据Token获取登录平台
	 * 
	 * @param token
	 * @return
	 */
	public Short getLoginPlatform(String token) {
		// 现Token第一个字符标识登录平台
		return Short.parseShort(token.substring(0, 1));
	}

	/**
	 * 从缓存中获取用户信息
	 * 
	 * @param token
	 * @return
	 */
	public UserInfoBO getUserByToken(String token) {
		Integer userId = (Integer) redisUtil.getObj(getTokenKey(token));
		if (userId == null) {
			return null;
		}
		UserInfoBO userInfoBO = (UserInfoBO) redisUtil.getObj(getUserKey(userId));
		if (userInfoBO == null) {
			clearUserToken(token);
			return null;
		}
		userInfoBO.setLoginPlatform(getLoginPlatform(token));
		return userInfoBO;
	}
	
	/**
	 * 根据用户ID获取用户缓存信息
	 * @param userId
	 * @return
	 */
	public UserInfoBO getUserById(Integer userId) {
		return (UserInfoBO) redisUtil.getObj(getUserKey(userId));
	}

	/**
	 * 清除缓存的token
	 * 
	 * @param token
	 */
	public void clearUserToken(String token) {
		redisUtil.delObj(getTokenKey(token));
	}

	/**
	 * 清除用户缓存信息
	 * 
	 * @param userId
	 */
	public void clearUserById(Integer userId) {
		redisUtil.delObj(getUserKey(userId));
	}

	/**
	 * 使用token缓存用户信息
	 * 
	 * @param token
	 * @param userInfoBO
	 */
	public void addUserCacheByToken(String token, UserInfoBO userInfoBO, long seconds) {
		Assert.paramNotNull(userInfoBO.getId(), "id");
		redisUtil.addObj(getTokenKey(token), userInfoBO.getId(), seconds);
		redisUtil.addObj(getUserKey(userInfoBO.getId()), userInfoBO, CacheConstants.ONE_WEEK);
	}

	/**
	 * 使用ID缓存用户信息
	 * 
	 * @param userInfoBO
	 * @param seconds
	 */
	public void addUserCacheById(UserInfoBO userInfoBO) {
		Assert.paramNotNull(userInfoBO.getId(), "id");
		redisUtil.addObj(getUserKey(userInfoBO.getId()), userInfoBO, CacheConstants.ONE_WEEK);
	}

	/**
	 * 更新用户缓存时间
	 * 
	 * @param token
	 */
	public void refreshExpire(String token, long seconds) {
		Integer userId = (Integer) redisUtil.getObj(getTokenKey(token));
		if (userId != null) {
			redisUtil.expire(getTokenKey(token), seconds);
			redisUtil.expire(getUserKey(userId), CacheConstants.ONE_WEEK);
		}
	}

	private String getTokenKey(String token) {
		return CacheConstants.C_CORE_MEMBER_INFO_TOKEN + token;
	}

	private String getUserKey(Integer userId) {
		return CacheConstants.C_CORE_MEMBER_INFO_ID + userId;
	}

}
