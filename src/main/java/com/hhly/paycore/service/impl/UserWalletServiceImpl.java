package com.hhly.paycore.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.dao.UserWalletMapper;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.WalletSplitTypeEnum;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 账户钱包服务层（供内部使用）
 * @author xiongJinGang
 * @date 2017年11月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("userWalletService")
public class UserWalletServiceImpl implements UserWalletService {

	private static final Logger logger = LoggerFactory.getLogger(UserWalletServiceImpl.class);

	@Resource
	private UserWalletMapper userWalletMapper;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private PayCoreService payCoreService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;

	@Override
	public ResultBO<?> findUserWallet(String token) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		UserWalletBO userWalletBO = findUserWalletByUserId(userInfo.getId());
		return ResultBO.ok(userWalletBO);
	}

	@Override
	public UserWalletBO findUserWalletByUserId(Integer userId) {
		UserWalletBO userWalletBO = userWalletMapper.getWalletByUserId(userId);
		if (!ObjectUtil.isBlank(userWalletBO)) {
			// 2017-07-03上午，经过最终确定，账户总金额=中奖金额+20%+80%之和，并且账户中，中奖金额状态不作使用（无效字段）
			// 这里设置totalAmount金额，是为了兼容前端的版本不做修改，继续保留该字段
			userWalletBO.setTotalAmount(userWalletBO.getTotalCashBalance());// 账户总余额。
		}
		return userWalletBO;
	}

	@Override
	public int addUserWallet(UserWalletPO userWalletPO) {
		return userWalletMapper.addUserWallet(userWalletPO);
	}

	@Override
	public int updateUserWallet(UserWalletPO userWalletPO) {
		return userWalletMapper.updateUserWallet(userWalletPO);
	}

	@Override
	public ResultBO<?> addRedColorAmount(List<UserRedAddParamVo> list) throws Exception {
		return addAmountForCms(list, null);
	}

	@Override
	public ResultBO<?> addRedColorAmountByType(List<UserRedAddParamVo> list, Short transType) throws Exception {
		return addAmountForCms(list, transType);
	}

	/**  
	* 方法说明: CMS操作，添加彩金红包
	* @auth: xiongJinGang
	* @param list
	* @param transType
	* @throws Exception
	* @time: 2017年11月8日 下午3:32:48
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> addAmountForCms(List<UserRedAddParamVo> list, Short transType) throws Exception {
		Short splitType = PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey();// 只操作彩金红包账户
		Short operateType = MoneyFlowEnum.IN.getKey();//
		for (UserRedAddParamVo urap : list) {
			logger.info("给用户【" + urap.getUserId() + "】账户添加彩金参数：" + urap.toString());
			// 1、更新账户钱包中彩金红包金额
			ResultBO<?> resultBO = updateUserWalletBySplit(urap.getUserId(), urap.getRedAmount(), operateType, splitType);
			// 彩金红包发送
			Short transStatus = PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();
			if (resultBO.isError()) {
				logger.info("给用户【" + urap.getUserId() + "】账户添加彩金失败：" + resultBO.getMessage());
				throw new RuntimeException("给用户【" + urap.getUserId() + "】账户添加彩金失败");
			}
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			// 2、添加红包交易记录
			urap.setStatus(transStatus);
			urap.setOrderInfo(Constants.ACTIVITY_SEND);// cms发放
			urap.setAfterRedAmount(urap.getRedAmount());// 操作后，红包金额
			urap.setTransAmount(urap.getRedAmount());// 交易金额

			// 这一步就是为了获取用户的渠道ID
			UserInfoBO userInfo = userInfoService.findUserInfoFromCache(urap.getUserId());
			urap.setChannelId(userInfo.getChannelId());

			if (ObjectUtil.isBlank(transType)) {
				transType = TransTypeEnum.RECHARGE.getKey();
			}
			urap.setTransType(transType);
			// 3、添加红包，不需要在交易流水中加，在红包交易表中加即可
			transRedService.addTransRed(urap);
			urap.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
			urap.setTotalCashBalance(userWalletPO.getTotalCashBalance());
			urap.setTransStatus(transStatus);
			urap.setSourceType(PayConstants.SourceTypeEnum.PERSON.getKey());// 人工充值
			// 4、添加交易流水
			TransUserPO transUserPO = transUserService.addTransUser(urap);
			// 添加提供给用户端查看的交易流水
			transUserLogService.addTransLogRecord(transUserPO);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> subRedColorAmount(List<UserRedAddParamVo> list) throws Exception {
		Short splitType = PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey();// 只操作彩金红包账户
		Short operateType = MoneyFlowEnum.OUT.getKey();// 支出
		for (UserRedAddParamVo urap : list) {
			logger.info("扣减用户【" + urap.getUserId() + "】账户彩金参数：" + urap.toString());
			// 1、扣减账户钱包中的钱
			ResultBO<?> resultBO = updateUserWalletBySplit(urap.getUserId(), urap.getRedAmount(), operateType, splitType);
			Short transStatus = PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();
			if (resultBO.isError()) {
				logger.info("扣减用户【" + urap.getUserId() + "】账户彩金失败：" + resultBO.getMessage());
				throw new RuntimeException("扣减用户【" + urap.getUserId() + "】账户彩金失败");
			}

			// 这一步就是为了获取用户的渠道ID
			UserInfoBO userInfo = userInfoService.findUserInfoFromCache(urap.getUserId());
			urap.setChannelId(userInfo.getChannelId());

			// 钱包记录
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			// 2、添加红包作废记录
			urap.setStatus(transStatus);
			urap.setTransStatus(transStatus);
			urap.setOrderInfo(Constants.RED_REMARK_CMS_CANCEL);// cms作废
			urap.setAfterRedAmount(0d);// 操作后，红包金额
			urap.setTransType(TransTypeEnum.DEDUCT.getKey());// 撤单
			urap.setTransAmount(urap.getRedAmount());// 交易金额
			urap.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
			urap.setTotalCashBalance(userWalletPO.getTotalCashBalance());
			transRedService.addTransRed(urap);

			// 3、添加扣款交易流水
			TransUserPO transUserPO = transUserService.addTransUser(urap);
			// 添加提供给用户端查看的交易流水
			transUserLogService.addTransLogRecord(transUserPO);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> addWinAmountForCMS(UserRedAddParamVo userRedAddParam) throws Exception {
		logger.info("给用户【" + userRedAddParam.getUserId() + "】加中奖金额开始，参数：" + userRedAddParam.toString());
		Short splitType = PayConstants.WalletSplitTypeEnum.WINNING.getKey();// 只操作中奖字段
		Short operateType = MoneyFlowEnum.IN.getKey();// 收入
		ResultBO<?> resultBO = updateUserWalletBySplit(userRedAddParam.getUserId(), userRedAddParam.getRedAmount(), operateType, splitType);
		Short transStatus = PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();
		if (resultBO.isError()) {
			logger.info("更新用户【" + userRedAddParam.getUserId() + "】中奖账户失败：" + resultBO.getMessage());
			return ResultBO.err(MessageCodeConstants.UPDATE_USER_WALLET_ERROR);
		}
		// 钱包记录
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		// 这一步就是为了获取用户的渠道ID
		UserInfoBO userInfo = userInfoService.findUserInfoFromCache(userRedAddParam.getUserId());
		userRedAddParam.setChannelId(userInfo.getChannelId());

		// 1、添加交易流水
		userRedAddParam.setAmountWin(userRedAddParam.getRedAmount());
		userRedAddParam.setRedAmount(0d);// 所用的红包消费总金额
		userRedAddParam.setTransAmount(userRedAddParam.getRedAmount());// 交易总金额；现金金额+红包金额+服务费
		userRedAddParam.setStatus(transStatus);
		userRedAddParam.setOrderInfo(Constants.OFFICIAL_AWARD);// cms作废
		userRedAddParam.setAfterRedAmount(0d);// 操作后，红包金额
		userRedAddParam.setTransType(TransTypeEnum.RETURN_AWARD.getKey());// 返奖
		userRedAddParam.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
		userRedAddParam.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 账户总余额
		userRedAddParam.setTransStatus(transStatus);
		TransUserPO transUserPO = transUserService.addTransUser(userRedAddParam);
		// 添加提供给用户端查看的交易流水
		transUserLogService.addTransLogRecord(transUserPO);
		return ResultBO.ok();
	}

	/**
	 * 1、用的是数据库版本号的锁机制来实现分布式锁，用了while循环，一般情况下只会执行一次，只有当版本号不对的情况下，才会出现循环
	 * 2、使用redis的分布式锁
	 * 3、使用zookeeper实现分布式锁
	 */
	@Override
	public ResultBO<?> updateUserWalletCommon(Integer userId, Double updateAmount, Short operateType, Double redAmount, Short redOperateType) throws Exception {
		UserWalletBO userWalletBO = null;
		int loop = 0;
		while (true) {
			// 循环5次还没跳出，自己跳出
			if (loop >= 10) {
				logger.info("循环多次未执行成功，跳出");
				break;
			}
			// 另开启一个事务，否则一直在这个事务中，查询不到最新的记录
			if (loop == 9) {
				Thread.sleep(500);
				userWalletBO = payCoreService.modifyQueryUserWallet(userId);
			} else {
				userWalletBO = userWalletMapper.getWalletByUserId(userId);// 钱包记录
			}
			// 支出
			if (operateType.equals(MoneyFlowEnum.OUT.getKey())) {
				// 无钱包记录，返回账户余额不足
				if (ObjectUtil.isBlank(userWalletBO)) {
					logger.info("用户【" + userId + "】没有钱包账户");
					return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
				}
				// 减账户余额
				ResultBO<?> resultBO = subBalanceByOperateType(userWalletBO, updateAmount, redAmount, redOperateType);
				if (resultBO.isOK()) {
					return resultBO;
				}
			} else {
				// 充值，无钱包记录，添加
				if (ObjectUtil.isBlank(userWalletBO)) {
					logger.info("用户【" + userId + "】没有钱包账户");
					return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
				} else {
					// 有钱包记录，更新账户余额
					ResultBO<?> resultBO = updateUserWalletBalance(userWalletBO, updateAmount, redAmount, redOperateType);
					if (resultBO.isOK()) {
						return resultBO;
					}
				}
			}
			loop++;
		}
		// 虽然配置文件中配置了exception回滚，但始终不起作用
		throw new RuntimeException("操作钱包异常！");
	}

	@Override
	public ResultBO<?> updateUserWalletBySplit(Integer userId, Double updateAmount, Short operateType, Short splitType) throws Exception {
		UserWalletBO userWalletBO = null;
		int loop = 0;
		while (true) {
			// 循环5次还没跳出，自己跳出
			if (loop >= 10) {
				logger.info("循环多次未执行成功，跳出");
				break;
			}
			// 另开启一个事务，否则一直在这个事务中，查询不到最新的记录
			if (loop == 9) {
				Thread.sleep(500);
				userWalletBO = payCoreService.modifyQueryUserWallet(userId);
			} else {
				userWalletBO = userWalletMapper.getWalletByUserId(userId);// 钱包记录
			}
			// 无钱包记录，返回账户余额不足
			if (ObjectUtil.isBlank(userWalletBO)) {
				logger.info("用户【" + userId + "】没有钱包账户");
				return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
			}
			if (ObjectUtil.isBlank(updateAmount)) {
				logger.info("用户【" + userId + "】交易金额为空！");
				return ResultBO.err(MessageCodeConstants.PAY_AMOUNT_ERROR_SERVICE);
			}

			// 支出
			if (operateType.equals(MoneyFlowEnum.OUT.getKey())) {
				// 减账户余额
				ResultBO<?> resultBO = subWalletBalance(userWalletBO, updateAmount, splitType);
				if (resultBO.isOK() || !resultBO.getErrorCode().equals(MessageCodeConstants.ADD_DATA_FAIL_SERVICE)) {
					return resultBO;
				}
			} else {
				// 收入
				ResultBO<?> resultBO = updateUserWalletBalance(userWalletBO, updateAmount, splitType);
				if (resultBO.isOK() || !resultBO.getErrorCode().equals(MessageCodeConstants.ADD_DATA_FAIL_SERVICE)) {
					return resultBO;
				}
			}
			loop++;
		}
		throw new RuntimeException("操作钱包异常！");
	}

	@Override
	public ResultBO<?> updateUserWalletCommon(Integer userId, Double amount80, Double amount20, Double amountWin, Double amountRed, Short operateType) throws Exception {
		UserWalletBO userWalletBO = null;
		int loop = 0;
		while (true) {
			// 循环5次还没跳出，自己跳出
			if (loop >= 10) {
				logger.info("循环多次未执行成功，跳出");
				break;
			}
			// 另开启一个事务，否则一直在这个事务中，查询不到最新的记录
			if (loop == 9) {
				Thread.sleep(500);
				userWalletBO = payCoreService.modifyQueryUserWallet(userId);
			} else {
				userWalletBO = userWalletMapper.getWalletByUserId(userId);// 钱包记录
			}
			// 收入
			if (operateType.equals(MoneyFlowEnum.IN.getKey())) {
				// 无钱包记录，返回账户余额不足
				if (ObjectUtil.isBlank(userWalletBO)) {
					logger.info("用户【" + userId + "】没有钱包账户");
					return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
				}
				// 减账户余额
				ResultBO<?> resultBO = updateSingleWalletBalance(userWalletBO, amount80, amount20, amountWin, amountRed);
				if (resultBO.isOK()) {
					return resultBO;
				}
			}
			loop++;
		}
		// 虽然配置文件中配置了exception回滚，但始终不起作用
		throw new RuntimeException("操作钱包异常！");
	}

	/**  
	* 方法说明: 更新账户钱包余额
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param rechargeMoney
	* @throws Exception
	* @time: 2017年5月24日 下午6:05:49
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> updateUserWalletBalance(UserWalletBO userWalletBO, Double updateAmount, Double redAmount, Short redOperateType) throws Exception {
		int dealResult = 0;
		UserWalletPO userWalletPO = new UserWalletPO(userWalletBO);
		// 有钱包记录
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 现金总余额=中奖金额+20%+80%金额
		Double oldTop20Balance = userWalletBO.getTop20Balance();// 原有20%余额
		Double oldTop80Balance = userWalletBO.getTop80Balance();// 原有80%余额

		logger.info("更新账户余额前，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【" + updateAmount + "】，彩金金额【" + redAmount + "】");
		ResultBO<?> resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			logger.error("注意：用户【" + userWalletBO.getUserId() + "】钱包账户金额错误，请检查");
		}
		// 总现金金额=原有金额+现在充值金额
		Double totalCashAmount = MathUtil.add(totalCashBalance, updateAmount);
		// 20%总余额=原有20%余额+充值金额拆分后的20%金额
		Double totalTop20Amount = MathUtil.add(oldTop20Balance, MathUtil.mul(updateAmount, Constants.USER_WALLET_TWENTY_PERCENT));
		// 80%总余额=原有80%余额+充值金额拆分后的80%金额
		Double totalTop80Amount = MathUtil.add(oldTop80Balance, MathUtil.mul(updateAmount, Constants.USER_WALLET_EIGHTY_PERCENT));
		// 设置最新的金额
		userWalletPO.setTotalCashBalance(totalCashAmount);
		userWalletPO.setTop20Balance(totalTop20Amount);
		userWalletPO.setTop80Balance(totalTop80Amount);
		// 需要添加的红包金额
		if (!ObjectUtil.isBlank(redAmount)) {
			Double redBalance = ObjectUtil.isBlank(userWalletPO.getEffRedBalance()) ? 0d : userWalletPO.getEffRedBalance();
			// 红包是进账
			if (redOperateType.equals(PayConstants.MoneyFlowEnum.IN.getKey())) {
				userWalletPO.setEffRedBalance(MathUtil.add(redBalance, redAmount));
			} else {
				// 红包是出账
				userWalletPO.setEffRedBalance(MathUtil.sub(redBalance, redAmount));
			}
		}
		dealResult = userWalletMapper.updateUserWallet(userWalletPO);
		if (dealResult <= 0) {
			logger.info("更新账户金额失败，参数：" + userWalletPO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		// 账户总金额=总现金金额+中奖余额
		userWalletPO.setTotalAmount(userWalletPO.getTotalCashBalance());
		logger.info("更新账户余额后，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【" + updateAmount + "】，彩金金额【" + redAmount + "】");
		return ResultBO.ok(userWalletPO);
	}

	/**  
	* 方法说明: 更新账户余额，按比例
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param updateAmount
	* @param splitType
	* @throws Exception
	* @time: 2017年5月26日 下午5:29:39
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> updateUserWalletBalance(UserWalletBO userWalletBO, Double updateAmount, Short splitType) throws Exception {
		int dealResult = 0;
		UserWalletPO userWalletPO = new UserWalletPO(userWalletBO);
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 现金总余额
		Double oldTop20Balance = userWalletBO.getTop20Balance();// 原有20%余额
		Double oldTop80Balance = userWalletBO.getTop80Balance();// 原有80%余额
		logger.info("更新账户余额前，账户余额资金情况【" + userWalletBO.toString() + "】，操作金额【" + updateAmount + "】，操作资金【" + WalletSplitTypeEnum.getEnum(splitType).getValue() + "】");

		ResultBO<?> resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			logger.error("注意：用户【" + userWalletBO.getUserId() + "】钱包账户金额错误，请检查");
		}
		// 总现金金额=原有金额+现在充值金额
		Double totalCashAmount = MathUtil.add(totalCashBalance, updateAmount);
		if (splitType.equals(PayConstants.WalletSplitTypeEnum.EIGHTY_PERCENT.getKey())) {// 只操作80%账户
			userWalletPO.setTop80Balance(MathUtil.add(updateAmount, oldTop80Balance));//
			userWalletPO.setTotalCashBalance(totalCashAmount);// 设置最新的金额
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.TWENTY_PERCENT.getKey())) {// 只操作20%账户
			userWalletPO.setTop20Balance(MathUtil.add(updateAmount, oldTop20Balance));
			userWalletPO.setTotalCashBalance(totalCashAmount);// 设置最新的金额
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_PERCENT_RATE.getKey())) {// 将金额按80%，20%比例分配
			// 80%总余额=原有80%余额+充值金额拆分后的80%金额
			Double totalTop80Amount = MathUtil.add(oldTop80Balance, MathUtil.mul(updateAmount, Constants.USER_WALLET_EIGHTY_PERCENT));
			// 20%总余额=原有20%余额+充值金额拆分后的20%金额
			Double totalTop20Amount = MathUtil.add(oldTop20Balance, MathUtil.mul(updateAmount, Constants.USER_WALLET_TWENTY_PERCENT));
			userWalletPO.setTop20Balance(totalTop20Amount);
			userWalletPO.setTop80Balance(totalTop80Amount);
			userWalletPO.setTotalCashBalance(totalCashAmount);// 设置最新的金额
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.WINNING.getKey())) {// 只操作中奖账户
			Double winBalance = ObjectUtil.isBlank(userWalletPO.getWinningBalance()) ? 0d : userWalletPO.getWinningBalance();
			userWalletPO.setWinningBalance(MathUtil.add(winBalance, updateAmount));
			userWalletPO.setTotalCashBalance(totalCashAmount);// 总现金金额 = 中奖金额 + 20% + 80%
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey())) {// 只操作红包账户
			Double redBalance = ObjectUtil.isBlank(userWalletPO.getEffRedBalance()) ? 0d : userWalletPO.getEffRedBalance();
			userWalletPO.setEffRedBalance(MathUtil.add(redBalance, updateAmount));
		} else {
			return ResultBO.err();
		}

		dealResult = userWalletMapper.updateUserWallet(userWalletPO);
		if (dealResult <= 0) {
			logger.info("更新账户金额失败，参数：" + userWalletPO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		// 这里设置totalAmount金额，是为了兼容前端的版本不做修改，继续保留该字段
		userWalletPO.setTotalAmount(userWalletBO.getTotalCashBalance());// 账户总余额。
		logger.info("更新账户余额后，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【" + updateAmount + "】");
		return ResultBO.ok(userWalletPO);
	}

	/**  
	* 方法说明: 更新每个账户的金额
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param amount80 80%账户
	* @param amount20 20%账户
	* @param amountWin 中奖账户
	* @param amountRed 彩金账户
	* @throws Exception
	* @time: 2017年7月12日 上午11:52:35
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> updateSingleWalletBalance(UserWalletBO userWalletBO, Double amount80, Double amount20, Double amountWin, Double amountRed) throws Exception {
		logger.info("更新账户余额前，账户余额资金情况【" + userWalletBO.toString() + "】，操作金额【80%：" + amount80 + "，20%：" + amount20 + "，中奖账户：" + amountWin + "，红包账户：" + amountRed + "】");
		UserWalletPO userWalletPO = new UserWalletPO(userWalletBO);
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 现金总余额
		Double oldTop20Balance = userWalletBO.getTop20Balance();// 原有20%余额
		Double oldTop80Balance = userWalletBO.getTop80Balance();// 原有80%余额
		Double oldTopWinBalance = userWalletBO.getWinningBalance();// 中奖金额
		Double oldRedBalance = userWalletBO.getEffRedBalance();// 彩金红包账户

		userWalletPO.setTop20Balance(MathUtil.add(amount20, oldTop20Balance));
		userWalletPO.setTop80Balance(MathUtil.add(amount80, oldTop80Balance));
		userWalletPO.setWinningBalance(MathUtil.add(amountWin, oldTopWinBalance));
		userWalletPO.setEffRedBalance(MathUtil.add(amountRed, oldRedBalance));
		Double totalAddCashBalance = MathUtil.add(amount80, amount20, amountWin);
		userWalletPO.setTotalCashBalance(MathUtil.add(totalAddCashBalance, totalCashBalance));

		int dealResult = userWalletMapper.updateUserWallet(userWalletPO);
		if (dealResult <= 0) {
			logger.info("更新账户金额失败，参数：" + userWalletPO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		// 这里设置totalAmount金额，是为了兼容前端的版本不做修改，继续保留该字段
		userWalletPO.setTotalAmount(userWalletBO.getTotalCashBalance());// 账户总余额。
		logger.info("更新账户余额后，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【80%：" + amount80 + "，20%：" + amount20 + "，中奖账户：" + amountWin + "，红包账户：" + amountRed + "】");
		return ResultBO.ok(userWalletPO);
	}

	/**  
	* 方法说明: 减账户余额，扣钱优先级（20%>80%>中奖金额）
	* @auth: xiongJinGang
	* @param userWalletBO
	* @param updateAmount
	* @param redAmount
	* @param redOperateType 红包金额是加还是减
	* @throws Exception
	* @time: 2017年5月24日 下午7:27:09
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> subBalanceByOperateType(UserWalletBO userWalletBO, Double updateAmount, Double redAmount, Short redOperateType) throws Exception {
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 现金总余额
		Double oldTop20Balance = userWalletBO.getTop20Balance();// 原有20%余额
		Double oldTop80Balance = userWalletBO.getTop80Balance();// 原有80%余额
		Double oldWinningBalance = userWalletBO.getWinningBalance();// 原有中奖余额
		Double oldEffRedBalance = userWalletBO.getEffRedBalance();// 原有可用红包余额

		// 最新总现金金额=原现金总金额-需要扣除的金额
		Double newTotalCashBalance = 0d;
		Double newTop20Balance = oldTop20Balance;
		Double newTop80Balance = oldTop80Balance;
		Double newWinningBalance = oldWinningBalance;

		logger.info("减账户余额前，账户余额资金情况【" + userWalletBO.toString() + "】，操作金额【" + updateAmount + "】，操作红包【" + redAmount + "】");

		ResultBO<?> resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			throw new RuntimeException("账户总现金金额与子账户钱包金额总和不符，扣减失败！");
		}

		int resultNum = MathUtil.compareTo(totalCashBalance, updateAmount);
		if (resultNum < 0) {
			logger.error("用户【" + userWalletBO.getUserId() + "】账户余额不足，总余额：" + totalCashBalance + "，需要扣除的余额：" + updateAmount);
			throw new RuntimeException("账户余额不足，操作钱包失败！");
		}

		// 更新用户钱包中的金额
		UserWalletPO userWalletPO = new UserWalletPO(userWalletBO);
		// 判断20%中的余额是否大于需要修改的余额，大于直接扣除，否则再扣80%中的
		int compareResult = MathUtil.compareTo(oldTop20Balance, updateAmount);
		// 20%中的余额够扣除
		if (compareResult >= 0) {
			newTop20Balance = MathUtil.sub(oldTop20Balance, updateAmount);
			userWalletPO.setUse20Balance(updateAmount);// 使用20%金额
		} else {
			// 20%扣完
			newTop20Balance = 0d;
			userWalletPO.setUse20Balance(oldTop20Balance);// 使用20%金额
			// 剩余需要减的金额
			Double needSubBalanceAmount = MathUtil.sub(updateAmount, oldTop20Balance);
			// 原有的80%金额不够减
			if (MathUtil.compareTo(oldTop80Balance, needSubBalanceAmount) >= 0) {
				newTop80Balance = MathUtil.sub(oldTop80Balance, needSubBalanceAmount);
				userWalletPO.setUse80Balance(needSubBalanceAmount);// 使用80%金额
			} else {
				newTop80Balance = 0d;
				userWalletPO.setUse80Balance(oldTop80Balance);
				// 需要扣除中奖账户的金额
				Double needSubWinAmount = MathUtil.sub(needSubBalanceAmount, oldTop80Balance);
				newWinningBalance = MathUtil.sub(oldWinningBalance, needSubWinAmount);
				userWalletPO.setUseWinBalance(needSubWinAmount);// 使用中奖金额
			}
		}

		newTotalCashBalance = MathUtil.sub(totalCashBalance, updateAmount);// 剩余总金额

		// 用户原有可用红包余额不为空并且使用红包余额
		if (!ObjectUtil.isBlank(redAmount)) {
			// 红包是进账
			if (redOperateType.equals(PayConstants.MoneyFlowEnum.IN.getKey())) {
				userWalletPO.setEffRedBalance(MathUtil.add(oldEffRedBalance, redAmount));
			} else {
				// 红包是出账
				userWalletPO.setEffRedBalance(MathUtil.sub(oldEffRedBalance, redAmount));
			}
		}

		userWalletPO.setWinningBalance(newWinningBalance);
		userWalletPO.setTotalCashBalance(newTotalCashBalance);
		userWalletPO.setTop20Balance(newTop20Balance);
		userWalletPO.setTop80Balance(newTop80Balance);
		userWalletPO.setId(userWalletBO.getId());
		userWalletPO.setUpdateTime(userWalletBO.getUpdateTime());
		int num = userWalletMapper.updateUserWallet(userWalletPO);
		if (num <= 0) {
			logger.info("扣除账户金额失败，参数：" + userWalletPO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		// 这里设置totalAmount金额，是为了兼容前端的版本不做修改，继续保留该字段
		userWalletBO.setTotalAmount(userWalletBO.getTotalCashBalance());// 账户总余额。
		logger.info("减账户余额后，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【" + updateAmount + "】，操作红包【" + redAmount + "】");
		return ResultBO.ok(userWalletPO);
	}

	private ResultBO<?> subWalletBalance(UserWalletBO userWalletBO, Double updateAmount, Short splitType) throws Exception {
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 现金总余额
		Double oldTop20Balance = userWalletBO.getTop20Balance();// 原有20%余额
		Double oldTop80Balance = userWalletBO.getTop80Balance();// 原有80%余额
		Double oldWinningBalance = userWalletBO.getWinningBalance();// 原有中奖余额
		Double oldErrRedBalance = userWalletBO.getEffRedBalance();// 原有彩金红包金额
		Double oldUpdateAmount = updateAmount;// 原本需要修改的金额
		logger.info("减账户余额前，账户余额资金情况【" + userWalletBO.toString() + "】，操作金额【" + oldUpdateAmount + "】，splitType：" + splitType);

		ResultBO<?> resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			throw new RuntimeException("账户总现金金额与子账户钱包金额总和不符，扣减失败！");
		}

		UserWalletPO userWalletPO = new UserWalletPO(userWalletBO);
		WalletSplitTypeEnum splitTypeEnum = WalletSplitTypeEnum.getEnum(splitType);
		switch (splitTypeEnum) {
		case EIGHTY_PERCENT:
			// 操作金额大于需要操作的账户字段，返回错误
			if (MathUtil.compareTo(updateAmount, oldTop80Balance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			userWalletPO.setTop80Balance(MathUtil.sub(oldTop80Balance, updateAmount));
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, updateAmount));// 设置最新的金额
			userWalletPO.setUse80Balance(updateAmount);
			break;

		default:
			break;
		}
		// 更新用户钱包中的金额
		if (splitType.equals(PayConstants.WalletSplitTypeEnum.EIGHTY_PERCENT.getKey())) {

		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.TWENTY_PERCENT.getKey())) {
			// 操作金额大于需要操作的账户字段，返回错误
			if (MathUtil.compareTo(updateAmount, oldTop20Balance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			userWalletPO.setTop20Balance(MathUtil.sub(oldTop20Balance, updateAmount));
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, updateAmount));// 设置最新的金额
			userWalletPO.setUse20Balance(updateAmount);
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_PERCENT_RATE.getKey())) {// 先扣20%，再扣80%
			if (MathUtil.compareTo(updateAmount, totalCashBalance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			// 判断20%中的余额是否大于需要修改的余额，大于直接扣除，否则再扣80%中的
			int compareResult = MathUtil.compareTo(oldTop20Balance, updateAmount);
			Double newTop20Balance = 0d;
			Double newTop80Balance = 0d;
			// 20%中的余额够扣除
			if (compareResult >= 0) {
				newTop20Balance = MathUtil.sub(oldTop20Balance, updateAmount);
				userWalletPO.setUse20Balance(updateAmount);
			} else {
				// 20%扣完
				newTop20Balance = 0.0;
				Double needSub80BalanceAmount = MathUtil.sub(updateAmount, oldTop20Balance);
				newTop80Balance = MathUtil.sub(oldTop80Balance, needSub80BalanceAmount);
				// 设置使用金额
				userWalletPO.setUse20Balance(oldTop20Balance);
				userWalletPO.setUse80Balance(needSub80BalanceAmount);
			}
			userWalletPO.setTop20Balance(newTop20Balance);
			userWalletPO.setTop80Balance(newTop80Balance);
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.WINNING.getKey())) {// 操作中奖金额
			if (MathUtil.compareTo(updateAmount, oldWinningBalance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			userWalletPO.setWinningBalance(MathUtil.sub(oldWinningBalance, updateAmount));
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, updateAmount));// 设置最新的金额
			userWalletPO.setUseWinBalance(updateAmount);// 使用中奖金额账户
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey())) {// 操作红包金额
			// 操作金额大于需要操作的账户字段，返回错误
			if (MathUtil.compareTo(updateAmount, oldErrRedBalance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			userWalletPO.setEffRedBalance(MathUtil.sub(oldErrRedBalance, updateAmount));
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.WINNING_TWENTY_EIGHTY.getKey())) {// 先奖金>20%>80%
			// 按次序操作，先中奖账户，然后20%账户，再到80%账户
			if (MathUtil.compareTo(updateAmount, oldWinningBalance) > 0) {
				userWalletPO.setWinningBalance(0d);
				userWalletPO.setUseWinBalance(oldWinningBalance);// 使用中奖金额

				// 剩余需要扣减的金额
				updateAmount = MathUtil.sub(updateAmount, oldWinningBalance);

				if (MathUtil.compareTo(oldTop20Balance, updateAmount) >= 0) {
					userWalletPO.setTop20Balance(MathUtil.sub(oldTop20Balance, updateAmount));
					userWalletPO.setUse20Balance(updateAmount);// 使用20%金额
				} else {
					// 减去中奖金额和20余额后，剩余的金额
					updateAmount = MathUtil.sub(updateAmount, oldTop80Balance);
					// 比较剩下的金额与20%中剩余金额
					if (MathUtil.compareTo(updateAmount, oldTop20Balance) > 0) {
						logger.info("账户余额不足，80余额【" + oldTop80Balance + "】，20余额【" + oldTop20Balance + "】，中奖余额【" + oldWinningBalance + "】");
						throw new RuntimeException("账户余额不足，操作钱包失败！");
					}
					userWalletPO.setUse80Balance(updateAmount);// 使用20%金额
					userWalletPO.setTop80Balance(MathUtil.sub(oldTop20Balance, updateAmount));// 使用20%金额
				}
			} else {
				// 中奖账户的金额足够扣
				userWalletPO.setWinningBalance(MathUtil.sub(oldWinningBalance, updateAmount));
				userWalletPO.setUseWinBalance(updateAmount);
			}
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, oldUpdateAmount));// 设置最新的金额
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.WINNING_EIGHTY_TWENTY.getKey())) {// 先奖金>80%>20%
			// 按次序操作，先中奖账户，然后80%账户，再到20%账户
			if (MathUtil.compareTo(updateAmount, oldWinningBalance) > 0) {
				userWalletPO.setWinningBalance(0d);
				userWalletPO.setUseWinBalance(oldWinningBalance);// 使用中奖金额
				// 剩余需要扣减的金额
				updateAmount = MathUtil.sub(updateAmount, oldWinningBalance);

				if (MathUtil.compareTo(oldTop80Balance, updateAmount) >= 0) {
					userWalletPO.setTop80Balance(MathUtil.sub(oldTop80Balance, updateAmount));
					userWalletPO.setUse80Balance(updateAmount);// 使用80%金额
				} else {
					userWalletPO.setTop80Balance(0d);
					userWalletPO.setUse80Balance(oldTop80Balance);// 使用80%金额

					// 减去中奖金额和80余额后，剩余的金额
					updateAmount = MathUtil.sub(updateAmount, oldTop80Balance);
					// 比较剩下的金额与20%中剩余金额
					if (MathUtil.compareTo(updateAmount, oldTop20Balance) > 0) {
						logger.info("账户余额不足，80余额【" + oldTop80Balance + "】，20余额【" + oldTop20Balance + "】，中奖余额【" + oldWinningBalance + "】");
						throw new RuntimeException("账户余额不足，操作钱包失败！");
					}
					userWalletPO.setUse20Balance(updateAmount);// 使用20%金额
					userWalletPO.setTop20Balance(MathUtil.sub(oldTop20Balance, updateAmount));// 使用20%金额
				}
			} else {
				// 中奖金额大于等于需要减的金额，其它账号上的金额就不需要减
				userWalletPO.setWinningBalance(MathUtil.sub(oldWinningBalance, updateAmount));// 剩余中奖金额
				userWalletPO.setUseWinBalance(updateAmount);// 使用中奖金额
			}
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, oldUpdateAmount));// 剩余金额
		} else if (splitType.equals(PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_WINNING.getKey())) {// 先扣20%，再扣80%
			if (MathUtil.compareTo(updateAmount, totalCashBalance) > 0) {
				throw new RuntimeException("账户余额不足，操作钱包失败！");
			}
			// 判断20%中的余额是否大于需要修改的余额，大于直接扣除，否则再扣80%中的
			int compareResult = MathUtil.compareTo(oldTop20Balance, updateAmount);
			// 20%中的余额够扣除
			if (compareResult >= 0) {
				Double newTop20Balance = MathUtil.sub(oldTop20Balance, updateAmount);
				userWalletPO.setUse20Balance(updateAmount);
				userWalletPO.setTop20Balance(newTop20Balance);
			} else {
				// 20%扣完
				Double needSub80BalanceAmount = MathUtil.sub(updateAmount, oldTop20Balance);
				if (MathUtil.compareTo(oldTop80Balance, needSub80BalanceAmount) >= 0) {
					Double newTop80Balance = MathUtil.sub(oldTop80Balance, needSub80BalanceAmount);
					// 设置使用金额
					userWalletPO.setUse20Balance(oldTop20Balance);
					userWalletPO.setUse80Balance(needSub80BalanceAmount);
					userWalletPO.setTop20Balance(0d);
					userWalletPO.setTop80Balance(newTop80Balance);
				} else {
					// 原有80%金额小于需要扣的金额
					Double needSubWinBalanceAmount = MathUtil.sub(needSub80BalanceAmount, oldTop80Balance);
					Double newWinBalance = MathUtil.sub(oldWinningBalance, needSubWinBalanceAmount);
					// 设置使用金额
					userWalletPO.setUse20Balance(oldTop20Balance);
					userWalletPO.setUse80Balance(needSub80BalanceAmount);
					userWalletPO.setUseWinBalance(needSubWinBalanceAmount);

					userWalletPO.setTop20Balance(0d);
					userWalletPO.setTop80Balance(0d);
					userWalletPO.setWinningBalance(newWinBalance);
				}
			}
			userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, oldUpdateAmount));// 剩余金额
		} else {
			throw new RuntimeException("操作类型错误！");
		}
		int num = userWalletMapper.updateUserWallet(userWalletPO);
		if (num <= 0) {
			logger.info("扣除账户金额失败，参数：" + userWalletPO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		// 这里设置totalAmount金额，是为了兼容前端的版本不做修改，继续保留该字段
		userWalletBO.setTotalAmount(userWalletBO.getTotalCashBalance());// 账户总余额。
		logger.info("减账户余额后，账户余额资金情况【" + userWalletPO.toString() + "】，操作金额【" + oldUpdateAmount + "】");
		return ResultBO.ok(userWalletPO);
	}

}
