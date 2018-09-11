package com.hhly.paycore.service.impl;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.UserInfoMapper;
import com.hhly.paycore.po.UserInfoPO;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 更新最后支付方式
 * @author xiongJinGang
 * @date 2017年6月10日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("infoService")
public class UserInfoServiceImpl implements UserInfoService {
	@Resource
	private RedisUtil redisUtil;

	@Autowired
	private UserInfoMapper userInfoMapper;

	@Override
	public UserInfoBO findUserInfo(Integer userId) throws Exception {
		return userInfoMapper.getUserInfo(userId);
	}

	@Override
	public UserInfoBO findUserByAccountName(String accountName) throws Exception {
		return userInfoMapper.getUserByAccountName(accountName);
	}

	@Override
	public UserInfoBO findUserInfoFromCache(Integer userId) throws Exception {
		String key = CacheConstants.SINGLE_USER_INFO + userId;
		UserInfoBO userInfoBO = redisUtil.getObj(key, new UserInfoBO());
		if (ObjectUtil.isBlank(userInfoBO)) {
			userInfoBO = findUserInfo(userId);
			if (!ObjectUtil.isBlank(userInfoBO)) {
				redisUtil.addObj(key, userInfoBO, CacheConstants.TWO_HOURS);
			}
		}
		return userInfoBO;
	}

	@Override
	public int updateLastUsePayId(UserInfoPO userInfoPO) throws Exception {
		return userInfoMapper.updateUserInfo(userInfoPO);
	}

	@Override
	public void updateLastBankCard(TransRechargeBO transRecharge) throws Exception {
		// 将当前银行ID，更新到用户表中的最后一次使用支付Id
		UserInfoPO userInfoPO = new UserInfoPO();
		userInfoPO.setUserPayId(transRecharge.getRechargeBank());
		userInfoPO.setId(transRecharge.getUserId());
		userInfoPO.setUserPayCardcode(transRecharge.getBankCardNum());
		updateLastUsePayId(userInfoPO);
	}
}
