package com.hhly.paycore.remote.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.TransUserMapper;
import com.hhly.paycore.remote.service.ITransUserService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.TransUserVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 用户交易流水记录实现
 * @author xiongjingang
 * @date 2017年3月3日 下午2:42:12
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iTransUserService")
public class TransUserServiceImpl implements ITransUserService {
	public static final Logger logger = LoggerFactory.getLogger(TransTakenConfirmServiceImpl.class);

	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private TransUserMapper transUserMapper;

	@Override
	public ResultBO<?> findUserTransRecordByOrderCode(TransUserVO transUser) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transUser.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		transUser.setUserId(userId);
		// 验证参数有效性
		ResultBO<?> resultBO = TransUtil.validateUserTransRecordByOrderCode(transUser);
		if (resultBO.isError()) {
			logger.info("查询用户{}交易记录失败{}，参数{}", userId, resultBO.getMessage(), transUser.getOrderCode() + "_" + transUser.getTransType());
			return resultBO;
		}
		try {
			List<TransUserBO> userList = transUserMapper.getUserTransRecordByOrderCode(transUser);
			if (ObjectUtil.isBlank(userList)) {
				logger.info("查询用户{}交易记录返回空，参数{}", userId, transUser.getOrderCode() + "_" + transUser.getTransType());
			}
			return ResultBO.ok(userList);
		} catch (Exception e) {
			logger.error("查询用户【" + userId + "】订单{}类型为{}交易流水异常", transUser.getOrderCode(), transUser.getTransType(), e);
			return ResultBO.err(MessageCodeConstants.FIND_DATA_EXCEPTION_SERVICE);
		}
	}

}
