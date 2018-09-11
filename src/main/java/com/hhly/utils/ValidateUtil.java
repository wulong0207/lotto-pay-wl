package com.hhly.utils;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.UserConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.RegularValidateUtil;
import com.hhly.skeleton.base.util.StringUtil;

/**
 * 用户信息格式验证
 * @desc
 * @author zhouyang
 * @date 2017年3月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class ValidateUtil {

	/**
	 * 身份证号验证
	 * @param idCard 身份证号码
	 * @return
	 */
	public static ResultBO<?> validateIdCard(String idCard) throws Exception {
		// 身份证号非空验证
		if (ObjectUtil.isBlank(idCard)) {
			return ResultBO.err(MessageCodeConstants.IDCARD_IS_NULL_FIELD);
		}
		// 身份证号格式验证
		if (!ObjectUtil.isBlank(IDCardUtil.IDCardValidate(StringUtil.getStringLowerCase(idCard)))) {
			return ResultBO.err(MessageCodeConstants.IDCARD_FORMAT_ERROR_FIELD);
		}
		return ResultBO.ok();
	}

	/**
	 * 邮箱验证
	 * @param email 邮箱
	 * @return
	 */
	public static ResultBO<?> validateEmail(String email) {
		// 邮箱地址非空验证
		if (ObjectUtil.isBlank(email)) {
			return ResultBO.err(MessageCodeConstants.EMAIL_IS_NULL_FIELD);
		}
		// 邮箱地址格式验证
		if (!email.matches(RegularValidateUtil.REGULAR_EMAIL)) {
			return ResultBO.err(MessageCodeConstants.EMAIL_FORMAT_ERROR_FIELD);
		}
		return ResultBO.ok();
	}

	/**
	 * 用户名验证
	 * @param account 帐户名
	 * @return
	 */
	public static ResultBO<?> validateAccount(String account) {
		if (ObjectUtil.isBlank(account)) {
			return ResultBO.err(MessageCodeConstants.USERNAME_IS_NULL_FIELD);
		}
		if (account.length() < UserConstants.ACCOUNT_MIN || account.length() > UserConstants.ACCOUNT_MAX) {
			return ResultBO.err(MessageCodeConstants.THE_ACCOUNT_LENGTH_JUST_BETWEEN_FOUR_AND_TWENTY);
		}
		// 帐户名不能为纯符号
		if (account.matches(RegularValidateUtil.REGULAR_ACCOUNT3)) {
			return ResultBO.err(MessageCodeConstants.ACCOUNT_IS_NOT_ALL_SYMBOLS);
		}
		if (account.matches(RegularValidateUtil.REGULAR_ACCOUNT2)) {
			if (account.length() <= 9) {
				return ResultBO.ok();
			} else {
				return ResultBO.err(MessageCodeConstants.ACCOUNT_IS_NUMBER_CON_NOT_OUT_OF_NINE);
			}
		} else {
			if (account.matches(RegularValidateUtil.REGULAR_ACCOUNT)) {
				return ResultBO.ok();
			} else {
				return ResultBO.err(MessageCodeConstants.ACCOUNT_FORMAT_ERROR_FIELD);
			}
		}
	}

	/**
	 * 帐号验证
	 * @param userName 帐号
	 * @return
	 */
	public static ResultBO<?> validateUserName(String userName) {
		// 验证帐号是否为空
		if (ObjectUtil.isBlank(userName)) {
			return ResultBO.err(MessageCodeConstants.ACCOUNT_IS_NULL_FIELD);
		}
		// 验证帐号格式是否合理
		if (!userName.matches(RegularValidateUtil.REGULAR_EMAIL) && !userName.matches(RegularValidateUtil.REGULAR_MOBILE) && !userName.matches(RegularValidateUtil.REGULAR_ACCOUNT)) {
			return ResultBO.err(MessageCodeConstants.ACCOUNT_FORMAT_ERROR_FILED);
		}
		return ResultBO.ok();
	}

	/**
	 * 验证手机号码或邮箱号
	 * @param userName
	 * @return
	 */
	public static ResultBO<?> validateUserName2(String userName) {
		if (ObjectUtil.isBlank(userName)) {
			return ResultBO.err(MessageCodeConstants.ACCOUNT_IS_NULL_FIELD);
		}
		if (!userName.matches(RegularValidateUtil.REGULAR_EMAIL) && !userName.matches(RegularValidateUtil.REGULAR_MOBILE)) {
			return ResultBO.err(MessageCodeConstants.ACCOUNT_FORMAT_ERROR_FILED);
		}
		return ResultBO.ok();
	}

	/**
	 * 密码验证
	 * @param password 密码
	 * @return
	 */
	public static ResultBO<?> validatePassword(String password) {
		// 验证密码是否为空
		if (ObjectUtil.isBlank(password)) {
			return ResultBO.err(MessageCodeConstants.PASSWORD_IS_NULL_FIELD);
		}
		if (!password.matches(RegularValidateUtil.REGULAR_PASSWORD)) {
			return ResultBO.err(MessageCodeConstants.PASSWORD_FORMAT_ERROR_FIELD);
		}
		return ResultBO.ok();
	}

	/**
	 * 验证真实姓名
	 * @param realName 真实姓名
	 * @return
	 */
	public static ResultBO<?> validateRealName(String realName) {
		// 验证真实姓名是否为空
		if (ObjectUtil.isBlank(realName)) {
			return ResultBO.err(MessageCodeConstants.REALNAME_IS_NULL_FIELD);
		}
		if (realName.length() > 6) {
			return ResultBO.err(MessageCodeConstants.YOUR_NAME_OUT_OF_SYSTEM_LENGTH);
		}
		// 验证真实姓名格式
		if (!realName.matches(RegularValidateUtil.REGULAR_REALNAME)) {
			return ResultBO.err(MessageCodeConstants.REALNAME_FORMAT_ERROR_FIELD);
		}
		return ResultBO.ok();
	}

	public static ResultBO<?> validateCardNum(String cardNum) {
		if (ObjectUtil.isBlank(cardNum)) {
			return ResultBO.err(MessageCodeConstants.BANKCARD_IS_NULL);
		}
		if (!cardNum.matches(RegularValidateUtil.REGULAR_BANKCARD)) {
			return ResultBO.err(MessageCodeConstants.BANKCARD_FORMAT_ERROR);
		}
		return ResultBO.ok();
	}

	/**
	 * 验证token
	 * @param token 缓存key
	 * @return
	 */
	public static ResultBO<?> validateToken(String token) {
		// 验证token是否为空
		if (ObjectUtil.isBlank(token)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		return ResultBO.ok();
	}

}
