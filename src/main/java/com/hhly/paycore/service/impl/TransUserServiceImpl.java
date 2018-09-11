package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.TransUserMapper;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderCopyPayInfoBO;
import com.hhly.skeleton.pay.agent.vo.TransferAccountsVO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.channel.bo.ChannelRechargeBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.TransUserVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.task.order.vo.OrderChannelVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 用户交易流水记录实现
 * @author xiongjingang
 * @date 2017年3月3日 下午2:42:12
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("transUserService")
public class TransUserServiceImpl implements TransUserService {
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
	@Transactional(propagation = Propagation.SUPPORTS)
	public List<TransUserPO> addGouCaiTransRecordBatch(List<PayOrderBaseInfoVO> orderTotalList, PayNotifyResultVO payNotifyResult, UserWalletPO userWalletPO, TransRechargeBO transRecharge) throws Exception {
		TransUserPO transUserPO = null;
		List<TransUserPO> list = new ArrayList<TransUserPO>();
		// 使用红包金额，批量支付和追号计划，不使用红包
		Double redAmount = ObjectUtil.isBlank(transRecharge.getRedAmount()) ? 0d : transRecharge.getRedAmount();
		Double totalBalance = userWalletPO.getTotalCashBalance();// 剩余总现金金额
		Double oriTotalBalance = userWalletPO.getOriTotalBalance();// 原始总金额
		Double use20Balance = userWalletPO.getUse20Balance();// 使用20%金额
		Double use80Balance = userWalletPO.getUse80Balance();// 使用80%金额
		Double useWinBalance = userWalletPO.getUseWinBalance();// 使用中奖金额
		// Double serviceAmount = ObjectUtil.isBlank(transRecharge.getServiceCharge()) ? 0.0 : transRecharge.getServiceCharge();// 服务费（购彩一般不会有手续费）
		Double serviceAmount = 0d;

		// 批量支付不能使用红包
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : orderTotalList) {
			transUserPO = new TransUserPO();
			String orderInfo = PayUtil.getGouCaiRemark(payOrderBaseInfoVO);
			transUserPO.setOrderInfo(orderInfo);
			transUserPO.setRemark(payNotifyResult.getRemark());
			transUserPO.setChannelId(transRecharge.getChannelId());
			transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余可用红包金额
			transUserPO.setOrderCode(payOrderBaseInfoVO.getOrderCode());
			transUserPO.setServiceCharge(serviceAmount);// 服务费
			transUserPO.setThirdTransId(payNotifyResult.getThirdTradeNo());// 第三方交易号
			transUserPO.setRedCode(transRecharge.getRedCode());// 红包编号
			transUserPO.setTradeCode(transRecharge.getTransRechargeCode());

			Double useCashAmount = payOrderBaseInfoVO.getOrderAmount();
			transUserPO.setCashAmount(useCashAmount);// 使用现金金额（使用了用户钱包中的金额）

			transUserPO.setRedTransAmount(redAmount);
			if (MathUtil.compareTo(useCashAmount, redAmount) >= 0) {
				transUserPO.setCashAmount(MathUtil.sub(useCashAmount, redAmount));// 使用现金金额（使用了用户钱包中的金额）
			} else {
				logger.error("使用红包金额大于订单金额，红包金额【" + redAmount + "】，订单金额【" + useCashAmount + "】");
				throw new RuntimeException();
			}
			transUserPO.setTransAmount(MathUtil.add(useCashAmount, serviceAmount));// 交易总金额；现金金额+红包金额+服务费
			// 剩余需要扣除的现金金额
			useCashAmount = MathUtil.sub(useCashAmount, redAmount);
			Double needUseAmount = useCashAmount;//
			if (MathUtil.compareTo(useCashAmount, 0d) != 0) {
				if (MathUtil.compareTo(use20Balance, useCashAmount) >= 0) {
					// 使用20%部分足够支付
					transUserPO.setAmount20(useCashAmount);
					use20Balance = MathUtil.sub(use20Balance, useCashAmount);// 20%账户剩余金额
				} else {
					transUserPO.setAmount20(use20Balance);
					useCashAmount = MathUtil.sub(useCashAmount, use20Balance);
					use20Balance = 0d;
					if (MathUtil.compareTo(use80Balance, useCashAmount) >= 0) {
						// 使用80%账户足够支付
						transUserPO.setAmount80(useCashAmount);
						use80Balance = MathUtil.sub(use80Balance, useCashAmount);// 80%账户剩余金额
					} else {
						transUserPO.setAmount80(use80Balance);
						useCashAmount = MathUtil.sub(useCashAmount, use80Balance);
						use80Balance = 0d;
						if (MathUtil.compareTo(useWinBalance, useCashAmount) >= 0) {
							transUserPO.setAmountWin(useCashAmount);
						} else {
							logger.error("用户【" + userWalletPO.getUserId() + "】中奖金额【" + useWinBalance + "】不足以支付剩余金额【" + useCashAmount + "】");
						}
					}
				}
			}

			if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
				transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
				oriTotalBalance = MathUtil.sub(oriTotalBalance, needUseAmount);
				transUserPO.setTotalCashBalance(oriTotalBalance);// 剩余总现金金额
			} else {
				transUserPO.setTotalCashBalance(totalBalance);// 剩余总现金金额
				transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey());// 交易失败
			}
			// 购彩记录
			Short transType = PayConstants.TransTypeEnum.LOTTERY.getKey();
			transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
			transUserPO.setUserId(transRecharge.getUserId());
			// 获取交易号
			transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType)));
			transUserPO.setSourceType(PayConstants.SourceTypeEnum.LOTTERY.getKey());
			list.add(transUserPO);
		}
		int num = 0;
		try {
			// 交易记录不为空，并且交易记录数量为1，用单个修改
			if (!list.isEmpty() && list.size() == 1) {
				num = transUserMapper.addUserTrans(transUserPO);
			} else {
				num = transUserMapper.addUserTransBatch(list);
			}
		} catch (Exception e) {
			logger.info("批量添加用户购彩交易记录异常：" + JSON.toJSONString(list), e);
			throw new RuntimeException("批量添加用户购彩交易记录异常");
		}
		if (num <= 0) {
			logger.info("批量添加用户购彩交易记录失败：" + transUserPO.toString());
			throw new RuntimeException("批量添加用户购彩交易记录失败");
		}
		return list;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addOrderResetRecord(OrderInfoBO orderInfoBO, Double needSubAmount, UserWalletPO userWalletPO) throws Exception {
		// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
		Short transType = PayConstants.TransTypeEnum.DEDUCT.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		String orderInfo = Constants.SEND_PRIZE_ERROR_INFO;
		TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, orderInfo);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transUserPO.setCashAmount(needSubAmount);
		transUserPO.setOrderCode(orderInfoBO.getOrderCode());
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(needSubAmount);// 交易总金额；现金金额+红包金额+服务费
		// transUserPO.setThirdTransId("");
		transUserPO.setChannelId(orderInfoBO.getChannelId());
		transUserPO.setAmount20(userWalletPO.getUse20Balance());
		transUserPO.setAmount80(userWalletPO.getUse80Balance());
		transUserPO.setAmountWin(userWalletPO.getUseWinBalance());
		// transUserPO.setTradeCode();
		transUserPO.setRedTransAmount(0d);
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余可用红包金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(orderInfo);
		int num = addTransRecord(transUserPO);
		logger.info("添加重置开奖交易流水" + (num > 0 ? "成功" : "失败"));
		if (num <= 0) {
			logger.info("添加重置开奖交易流水失败：" + transUserPO.toString());
			throw new RuntimeException("添加重置开奖交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addOrderRedResetRecord(OrderInfoBO orderInfoBO, Double needSubRedAmount, UserWalletPO userWalletPO) throws Exception {
		// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
		Short transType = PayConstants.TransTypeEnum.DEDUCT.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		String orderInfo = Constants.SEND_PRIZE_ERROR_INFO;
		TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, orderInfo);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transUserPO.setCashAmount(0d);
		transUserPO.setOrderCode(orderInfoBO.getOrderCode());
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(needSubRedAmount);// 交易总金额；现金金额+红包金额+服务费
		// transUserPO.setThirdTransId("");
		transUserPO.setChannelId(orderInfoBO.getChannelId());
		transUserPO.setAmount20(userWalletPO.getUse20Balance());
		transUserPO.setAmount80(userWalletPO.getUse80Balance());
		transUserPO.setAmountWin(userWalletPO.getUseWinBalance());
		// transUserPO.setTradeCode();
		transUserPO.setRedTransAmount(needSubRedAmount);
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余可用红包金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(orderInfo);
		int num = addTransRecord(transUserPO);
		logger.info("添加红包重置开奖交易流水" + (num > 0 ? "成功" : "失败"));
		if (num <= 0) {
			logger.info("添加红包重置开奖交易流水失败：" + transUserPO.toString());
			throw new RuntimeException("添加红包重置开奖交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addActivityTransRecord(TransRechargeBO transRecharge, Double totalCashBalance, Double totalRedBalance, OperateCouponPO operateCouponPO) throws Exception {
		// 添加生成彩金红包交易流水。
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setChannelId(transRecharge.getChannelId());
		transUserPO.setCashAmount(0d);
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setTradeCode(transRecharge.getTransRechargeCode());
		transUserPO.setRedCode(operateCouponPO.getRedCode());
		transUserPO.setRedTransAmount(operateCouponPO.getRedValue());// 红包交易金额
		transUserPO.setTotalRedBalance(totalRedBalance);
		transUserPO.setTotalCashBalance(totalCashBalance);// 剩余总现金金额
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(operateCouponPO.getRedValue());// 交易总金额；现金金额+红包金额+服务费
		// 充值记录
		transUserPO.setTransType(PayConstants.TransTypeEnum.ACTIVITY_GIVE.getKey());// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款；10：返佣
		transUserPO.setUserId(transRecharge.getUserId());
		transUserPO.setOrderInfo(Constants.RED_REMARK_ORDER_INFO);
		transUserPO.setRemark(Constants.ACTIVITY_SEND);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		if (!ObjectUtil.isBlank(transRecharge)) {
			// 交易流水的来源
			Short sourceType = PayConstants.SourceTypeEnum.LOTTERY.getKey();
			// 充值类型不为空
			if (!ObjectUtil.isBlank(transRecharge.getRechargeType()) && transRecharge.getRechargeType().equals(PayConstants.RechargeTypeEnum.RECHARGE.getKey())) {
				sourceType = PayConstants.SourceTypeEnum.RECHARGE.getKey();
			}
			transUserPO.setSourceType(sourceType);
		}
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加充值交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加充值交易明细失败");
		}
		return transUserPO;
	}

	/**
	 * 方法说明: 添加用户充值交易记录，充值使用了红包，不在交易记录中展示，充值交易记录中的红包金额都为0
	 * 
	 * @auth: xiongJinGang
	 * @param transRecharge
	 * @param payNotifyResult
	 * @time: 2017年3月23日 下午5:24:55
	 * @return: void
	 */
	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, UserWalletPO userWalletPO, String orderInfo) throws Exception {
		Double serviceAmount = transRecharge.getServiceCharge();// 服务费
		Double redTransAmount = 0d;// 充值时，红包交易金额都为0，因为只能用充值红包

		TransUserPO transUserPO = new TransUserPO();
		// 如果是渠道充值并且红包编号不为空，记录红包编号
		if (Constants.CHANNEL_RECHARGE.equals(orderInfo) && !ObjectUtil.isBlank(transRecharge.getRedCode())) {
			transUserPO.setRedCode(transRecharge.getRedCode());
		}
		transUserPO.setChannelId(transRecharge.getChannelId());
		// 充值不存订单号，在购彩的时候才添加
		// transUserPO.setOrderCode(orderCode);
		// transUserVO.setOrderInfo(orderInfo);
		Double cashAmount = ObjectUtil.isBlank(transRecharge.getArrivalAmount()) ? 0.0 : transRecharge.getArrivalAmount();// 扣除手续费后的现金金额
		Short transStatus = PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey();// 默认交易失败
		if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
			transStatus = PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();// 交易成功
			transUserPO.setCashAmount(cashAmount);
			transUserPO.setThirdTransId(payNotifyResult.getThirdTradeNo());
			// 大于等于1毛钱才进行20%和80%的拆分
			if (MathUtil.compareTo(cashAmount, 0.1d) >= 0) {
				transUserPO.setAmount20(MathUtil.mul(cashAmount, Constants.USER_WALLET_TWENTY_PERCENT));
				transUserPO.setAmount80(MathUtil.mul(cashAmount, Constants.USER_WALLET_EIGHTY_PERCENT));
			} else {
				transUserPO.setAmount80(cashAmount);
				transUserPO.setAmount20(0d);
			}
		} else {
			transStatus = PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey();// 交易失败
			transUserPO.setCashAmount(cashAmount);
		}
		transUserPO.setTransStatus(transStatus);
		if (!ObjectUtil.isBlank(transRecharge.getTransRechargeCode())) {
			transUserPO.setTradeCode(transRecharge.getTransRechargeCode());
		}

		transUserPO.setRedTransAmount(redTransAmount);
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余总红包金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(serviceAmount);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(MathUtil.add(cashAmount, serviceAmount, redTransAmount));// 交易总金额；现金金额+红包金额+服务费

		// 充值记录
		Short transType = PayConstants.TransTypeEnum.RECHARGE.getKey();
		if (!ObjectUtil.isBlank(transRecharge.getTradeType())) {
			transType = transRecharge.getTradeType();
		}
		transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
		transUserPO.setUserId(transRecharge.getUserId());
		transUserPO.setOrderInfo(orderInfo);
		transUserPO.setRemark(orderInfo);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType)));
		// 交易流水的来源
		Short sourceType = PayConstants.SourceTypeEnum.LOTTERY.getKey();
		if (transRecharge.getRechargeType().equals(PayConstants.RechargeTypeEnum.RECHARGE.getKey())) {
			sourceType = PayConstants.SourceTypeEnum.RECHARGE.getKey();
		}
		transUserPO.setSourceType(sourceType);

		// 代理编号
		if (!ObjectUtil.isBlank(transRecharge.getChannelCode()) && PayConstants.ChannelEnum.AGENT.getKey().equals(transRecharge.getChannelId())) {
			transUserPO.setAgentCode(transRecharge.getChannelCode());
		}

		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加充值交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加充值交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public int addTransRecord(TransUserPO transUserPO) throws Exception {
		return transUserMapper.addUserTrans(transUserPO);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addWinTransUser(OrderInfoBO orderInfoBO, Double redValue, String redCode, Double aftBonus, UserWalletPO userWalletPO) throws Exception {
		// 添加中奖交易流水
		String orderInfo = PayUtil.getGouCaiRemark(orderInfoBO);
		Short transType = PayConstants.TransTypeEnum.RETURN_AWARD.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, orderInfo);
		transUserPO.setOrderCode(orderInfoBO.getOrderCode());
		transUserPO.setChannelId(orderInfoBO.getChannelId());
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 默认交易成功
		transUserPO.setTransAmount(MathUtil.add(aftBonus, redValue));// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setCashAmount(aftBonus);
		transUserPO.setAmountWin(aftBonus);// 中奖金额
		transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
		transUserPO.setOrderInfo(orderInfo);// 描述
		transUserPO.setRedTransAmount(redValue);
		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(Constants.TRANS_USER_ORDER_WIN);// 派奖
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余总红包金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setRedCode(redCode);// 红包编号
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加中奖交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加中奖交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addOrderGroupWinTransUser(OrderInfoBO orderInfoBO, Double commissionAmount, UserWalletPO userWalletPO) throws Exception {
		// 添加中奖交易流水
		String orderInfo = PayUtil.getOrderGroupCommissonRemark(orderInfoBO);
		Short transType = PayConstants.TransTypeEnum.RETURN_AWARD.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		TransUserPO transUserPO = new TransUserPO(userWalletPO.getUserId(), transCode, transType, orderInfo);
		transUserPO.setOrderCode(orderInfoBO.getOrderCode());
		transUserPO.setChannelId(orderInfoBO.getChannelId());
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 默认交易成功
		transUserPO.setTransAmount(commissionAmount);// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setCashAmount(commissionAmount);
		transUserPO.setAmountWin(commissionAmount);// 中奖金额
		transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
		transUserPO.setOrderInfo(orderInfo);// 描述
		transUserPO.setRedTransAmount(0d);
		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(Constants.TRANS_USER_ORDER_WIN);// 派奖
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余总红包金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setRedCode(null);// 红包编号
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加抽成中奖交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加抽成中奖交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addOfficialBonusTransUser(OrderInfoBO orderInfoBO, Double aftBonus, Double totalRedBalance, Double totalCashBalance) throws Exception {
		TransUserPO transUser = new TransUserPO();
		transUser.setUserId(orderInfoBO.getUserId());
		transUser.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transUser.setAmountWin(aftBonus);
		transUser.setCashAmount(aftBonus);
		transUser.setTransAmount(aftBonus);// 交易总金额；现金金额+红包金额+服务费
		transUser.setOrderInfo(Constants.OFFICIAL_AWARD);// 官方加奖
		transUser.setTransType(TransTypeEnum.RETURN_AWARD.getKey());// 返奖
		transUser.setTotalRedBalance(totalRedBalance);// 剩余红包总金额
		transUser.setTotalCashBalance(totalCashBalance);// 账户总余额
		transUser.setOrderCode(orderInfoBO.getOrderCode());
		transUser.setChannelId(orderInfoBO.getChannelId());
		transUser.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 默认交易成功
		transUser.setRedTransAmount(0d);
		transUser.setServiceCharge(0d);
		transUser.setRemark(Constants.OFFICIAL_AWARD);// 派奖
		// 交易时间
		transUser.setTransTime(DateUtil.getNowDate());
		transUser.setTransEndTime(DateUtil.getNowDate());
		transUser.setThirdTransTime(DateUtil.getNowDate());
		int num = addTransRecord(transUser);
		if (num <= 0) {
			logger.info("添加官方加奖交易明细失败：" + transUser.toString());
			throw new RuntimeException("添加中奖交易明细失败");
		}
		return transUser;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addWebSiteBonusTransUser(OrderInfoBO orderInfoBO, Double totalRedBalance, Double totalCashBalance, OperateCouponBO operateCouponBO) throws Exception {
		TransUserPO transUser = new TransUserPO();
		String redName = ObjectUtil.isBlank(operateCouponBO.getRemark()) ? Constants.WEBSITE_AWARD : operateCouponBO.getRemark();
		transUser.setUserId(orderInfoBO.getUserId());
		transUser.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transUser.setAmountWin(0d);
		transUser.setCashAmount(0d);
		transUser.setTransAmount(operateCouponBO.getRedBalance());// 交易总金额；现金金额+红包金额+服务费
		transUser.setOrderInfo(redName);// 本站加奖
		transUser.setTransType(TransTypeEnum.RETURN_AWARD.getKey());// 返奖
		transUser.setTotalRedBalance(totalRedBalance);// 剩余红包总金额
		transUser.setTotalCashBalance(totalCashBalance);// 账户总余额
		transUser.setOrderCode(orderInfoBO.getOrderCode());
		transUser.setChannelId(orderInfoBO.getChannelId());
		transUser.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 默认交易成功
		transUser.setRedTransAmount(operateCouponBO.getRedBalance());
		transUser.setServiceCharge(0d);
		transUser.setRemark(redName);// 派奖
		// 交易时间
		transUser.setTransTime(DateUtil.getNowDate());
		transUser.setTransEndTime(DateUtil.getNowDate());
		transUser.setThirdTransTime(DateUtil.getNowDate());
		int num = addTransRecord(transUser);
		if (num <= 0) {
			logger.info("添加本站加奖交易明细失败：" + transUser.toString());
			throw new RuntimeException("添加中奖交易明细失败");
		}
		return transUser;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addTransUser(UserRedAddParamVo urap) throws Exception {
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setCashAmount(urap.getCashAmount());
		transUserPO.setTransAmount(urap.getTransAmount());// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setAmountWin(urap.getAmountWin());// 中奖金额
		transUserPO.setAmount20(urap.getAmount20());
		transUserPO.setAmount80(urap.getAmount80());
		transUserPO.setTotalRedBalance(urap.getTotalRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(urap.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setChannelId(urap.getChannelId());
		transUserPO.setOrderInfo(urap.getOrderInfo());
		transUserPO.setRedCode(urap.getRedCode());
		transUserPO.setRedTransAmount(urap.getRedAmount());
		transUserPO.setRemark(urap.getOrderInfo());
		Short transStatus = urap.getTransStatus();
		if (ObjectUtil.isBlank(transStatus) && !transStatus.equals("0")) {
			transStatus = urap.getStatus();
		}
		transUserPO.setTransStatus(transStatus);
		transUserPO.setUserId(urap.getUserId());

		transUserPO.setThirdTransId(urap.getRedCode());

		Short transType = urap.getTransType();
		if (ObjectUtil.isBlank(transType)) {
			transType = PayConstants.TransTypeEnum.RECHARGE.getKey();
		}

		if (!ObjectUtil.isBlank(urap.getSourceType())) {
			transUserPO.setSourceType(urap.getSourceType());
		}

		transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType)));
		transUserPO.setTransType(transType);

		// 交易时间
		transUserPO.setTransTime(DateUtil.getNowDate());
		transUserPO.setTransEndTime(DateUtil.getNowDate());
		transUserPO.setThirdTransTime(DateUtil.getNowDate());

		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加充值交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加充值交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public int addTransUserByBatch(List<TransUserPO> list) throws Exception {
		return transUserMapper.addUserTransBatch(list);
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addTransUserRecord(PayChildWalletVO pcw, TransRechargeBO transRecharge) throws Exception {
		TransUserPO transUserPO = new TransUserPO();
		String channelId = ObjectUtil.isBlank(pcw.getChannelId()) ? PayConstants.ChannelEnum.UNKNOWN.getKey() : pcw.getChannelId();
		transUserPO.setChannelId(channelId);
		transUserPO.setOrderCode(pcw.getOrderCode());
		Short transStatus = PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();// 默认交易成功
		transUserPO.setTransStatus(transStatus);
		transUserPO.setTransAmount(pcw.getTradeAmount());// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setCashAmount(pcw.getBackCachAmount());// 现金金额

		// 充值记录
		transUserPO.setTransType(pcw.getTransType());// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
		transUserPO.setUserId(pcw.getUserId());
		transUserPO.setOrderInfo(pcw.getOrderInfo());// 描述
		transUserPO.setRemark(pcw.getOperateRemark());// 后台描述
		transUserPO.setRedTransAmount(pcw.getRedAmount());
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(pcw.getTransType())));
		transUserPO.setTotalRedBalance(pcw.getTotalRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(pcw.getTotalCashAmount());// 剩余总现金金额
		transUserPO.setRedCode(pcw.getRedCode());// 红包编号
		transUserPO.setAmount20(pcw.getUseAmount20());
		transUserPO.setAmount80(pcw.getUseAmount80());
		transUserPO.setAmountWin(pcw.getUseAmountWin());
		transUserPO.setTradeCode(pcw.getTradeCode());
		if (!ObjectUtil.isBlank(transRecharge)) {
			// 交易流水的来源
			Short sourceType = PayConstants.SourceTypeEnum.LOTTERY.getKey();
			if (transRecharge.getRechargeType().equals(PayConstants.RechargeTypeEnum.RECHARGE.getKey())) {
				sourceType = PayConstants.SourceTypeEnum.RECHARGE.getKey();
			}
			transUserPO.setSourceType(sourceType);
		}
		int num = transUserMapper.addUserTrans(transUserPO);
		if (num <= 0) {
			logger.info("添加交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addTransRecord(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception {
		// 添加生成彩金红包交易流水。
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setChannelId(transRechargeBO.getChannelId());
		transUserPO.setCashAmount(0d);
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setTradeCode(cmsRecharge.getRechargeCode());
		transUserPO.setRedCode(transRechargeBO.getRedCode());
		transUserPO.setRedTransAmount(cmsRecharge.getRechargeAmount());// 红包交易金额
		transUserPO.setTotalRedBalance(uwp.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(uwp.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(cmsRecharge.getRechargeAmount());// 交易总金额；现金金额+红包金额+服务费
		// 充值记录
		transUserPO.setTransType(PayConstants.TransTypeEnum.ACTIVITY_GIVE.getKey());// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动
		transUserPO.setUserId(cmsRecharge.getUserId());
		transUserPO.setOrderInfo(cmsRecharge.getOrderInfo());
		transUserPO.setRemark(Constants.ACTIVITY_SEND);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addTransCostRecord(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception {
		// 购买彩金红包流水
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setChannelId(transRechargeBO.getChannelId());
		transUserPO.setCashAmount(cmsRecharge.getRechargeAmount());
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setTradeCode(cmsRecharge.getRechargeCode());
		transUserPO.setRedTransAmount(0d);// 红包交易金额
		transUserPO.setAmount20(uwp.getUse20Balance());// 使用20%金额
		transUserPO.setAmount80(uwp.getUse80Balance());// 使用80%金额
		transUserPO.setAmountWin(uwp.getUseWinBalance());// 使用中奖金额
		transUserPO.setTotalRedBalance(uwp.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(uwp.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setServiceCharge(0d);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(cmsRecharge.getRechargeAmount());// 交易总金额；现金金额+红包金额+服务费
		// 充值记录
		Short transType = PayConstants.TransTypeEnum.ACTIVITY_CONSUME.getKey();
		transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动
		transUserPO.setUserId(cmsRecharge.getUserId());
		transUserPO.setOrderInfo(cmsRecharge.getOrderInfo());
		transUserPO.setRemark(Constants.RECHARGE_BUY_RED_REMARK_INFO);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addPushPayTransUser(OrderCopyPayInfoBO orderCopyPayInfoBO, UserWalletPO userWalletPO) throws Exception {
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setCashAmount(orderCopyPayInfoBO.getPrice());
		transUserPO.setTransAmount(orderCopyPayInfoBO.getPrice());// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setAmountWin(0d);// 中奖金额
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setChannelId(orderCopyPayInfoBO.getChannelId());
		transUserPO.setOrderInfo(orderCopyPayInfoBO.getOrderInfo());// 支付方案推荐
		transUserPO.setRedTransAmount(0d);
		transUserPO.setRemark(orderCopyPayInfoBO.getOrderInfo());
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setUserId(userWalletPO.getUserId());
		Short transType = orderCopyPayInfoBO.getTransType();
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType)));
		transUserPO.setTransType(transType);
		// 交易时间
		transUserPO.setTransTime(DateUtil.getNowDate());
		transUserPO.setTransEndTime(DateUtil.getNowDate());
		transUserPO.setThirdTransTime(DateUtil.getNowDate());

		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加推单方案交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加推单方案交易明细失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addActivityConsume(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception {
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setChannelId(transRechargeBO.getChannelId());
		transUserPO.setCashAmount(transRechargeBO.getArrivalAmount());
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setTradeCode(cmsRecharge.getRechargeCode());
		transUserPO.setRedTransAmount(0d);// 红包交易金额
		transUserPO.setAmount20(uwp.getUse20Balance());// 使用20%金额
		transUserPO.setAmount80(uwp.getUse80Balance());// 使用80%金额
		transUserPO.setAmountWin(uwp.getUseWinBalance());// 使用中奖金额
		transUserPO.setTotalRedBalance(uwp.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(uwp.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setServiceCharge(0d);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(transRechargeBO.getArrivalAmount());// 交易总金额；现金金额+红包金额+服务费
		// 充值记录
		Short transType = PayConstants.TransTypeEnum.ACTIVITY_CONSUME.getKey();
		transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动
		transUserPO.setUserId(cmsRecharge.getUserId());
		transUserPO.setOrderInfo(cmsRecharge.getOrderInfo());
		transUserPO.setRemark(cmsRecharge.getOrderInfo());
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加交易明细失败");
		}
		return transUserPO;
	}

	/**  
	* 方法说明: 代理系统给会员充值
	* @auth: xiongJinGang
	* @param transferAccounts
	* @param memberInfo
	* @param updateAmount
	* @param transLogPO
	* @param operateCouponPO
	* @param userWalletPO
	* @time: 2018年3月5日 下午3:27:22
	* @return: void 
	* @throws Exception 
	*/
	@Override
	public TransUserPO addTransRecord(TransferAccountsVO transferAccounts, UserInfoBO memberInfo, Double updateAmount, String rechargeCode, OperateCouponPO operateCouponPO, UserWalletPO userWalletPO) throws Exception {
		// 添加生成彩金红包交易流水。
		TransUserPO transUserPO = new TransUserPO();
		transUserPO.setChannelId(transferAccounts.getChannelId());
		transUserPO.setCashAmount(0d);
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		transUserPO.setTradeCode(rechargeCode);// 存充值的编号，用户可以根据这个编号查看充值详情
		transUserPO.setRedCode(operateCouponPO.getRedCode());
		transUserPO.setRedTransAmount(updateAmount);// 红包交易金额
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额
		transUserPO.setServiceCharge(0d);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(updateAmount);// 交易总金额；现金金额+红包金额+服务费
		// 充值记录
		transUserPO.setTransType(PayConstants.TransTypeEnum.RECHARGE.getKey());// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动
		transUserPO.setUserId(memberInfo.getId());
		transUserPO.setOrderInfo("代理充值");
		transUserPO.setRemark(Constants.RED_AGENT_RECHARGE);
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));

		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("代理给会员转账交易明细失败：" + transUserPO.toString());
			throw new RuntimeException("添加代理转账交易明细失败");
		}
		return transUserPO;
	}

	@Override
	public TransUserPO addOrderGroup(PayNotifyResultVO payNotifyResult, OrderInfoBO orderInfo, OrderGroupBO orderGroup, UserWalletPO userWalletPOSubOne) throws Exception {
		// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
		Short transType = PayConstants.TransTypeEnum.LOTTERY.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		TransUserPO transUserPO = new TransUserPO(userWalletPOSubOne.getUserId(), transCode, transType, Constants.ORDER_GROUP_GUARANTEE);
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transUserPO.setCashAmount(orderGroup.getGuaranteeAmount());// 保底金额
		transUserPO.setOrderCode(orderInfo.getOrderCode());
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(orderGroup.getGuaranteeAmount());// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setThirdTransId(payNotifyResult.getThirdTradeNo());
		transUserPO.setChannelId(orderInfo.getChannelId());
		transUserPO.setAmount20(userWalletPOSubOne.getUse20Balance());
		transUserPO.setAmount80(userWalletPOSubOne.getUse80Balance());
		transUserPO.setAmountWin(userWalletPOSubOne.getUseWinBalance());
		transUserPO.setTradeCode(payNotifyResult.getOrderCode());
		transUserPO.setRedTransAmount(0d);
		transUserPO.setTotalRedBalance(userWalletPOSubOne.getEffRedBalance());// 剩余可用红包金额
		transUserPO.setTotalCashBalance(userWalletPOSubOne.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(Constants.ORDER_GROUP_GUARANTEE);
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加合买订单保底金额交易记录失败：" + transUserPO.toString());
			throw new RuntimeException("添加合买订单保底金额交易记录失败");
		}
		return transUserPO;
	}

	@Override
	@Transactional(propagation = Propagation.SUPPORTS)
	public TransUserPO addSubscription(String buyCode, String oldTransCode, String channelId, Double buyAmount, UserWalletBO userWallet) throws Exception {
		// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
		Short transType = PayConstants.TransTypeEnum.LOTTERY.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
		TransUserPO transUserPO = new TransUserPO(userWallet.getUserId(), transCode, transType, Constants.ORDER_GROUP_GUARANTEE_BUY);
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transUserPO.setCashAmount(buyAmount);// 购买金额
		transUserPO.setOrderCode(buyCode);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(buyAmount);// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setThirdTransId(oldTransCode);
		transUserPO.setChannelId(channelId);
		transUserPO.setAmount20(0d);
		transUserPO.setAmount80(buyAmount);// 全部使用80%账户
		transUserPO.setAmountWin(0d);
		transUserPO.setTradeCode(oldTransCode);
		transUserPO.setRedTransAmount(0d);
		transUserPO.setTotalRedBalance(userWallet.getEffRedBalance());// 剩余可用红包金额
		transUserPO.setTotalCashBalance(userWallet.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		transUserPO.setRemark(Constants.ORDER_GROUP_GUARANTEE);
		int num = addTransRecord(transUserPO);
		if (num <= 0) {
			logger.info("添加合买订单保底金额转认购交易记录失败：" + transUserPO.toString());
			throw new RuntimeException("添加合买订单保底金额认购交易记录失败");
		}
		return transUserPO;
	}

	@Override
	public TransUserBO findTransUserBy(String tradeNo, Short tradeStatus) throws Exception {
		return transUserMapper.getTransUserBy(tradeNo, tradeStatus);
	}

	@Override
	public TransUserBO getTransUserByType(String tradeNo, Short tradeStatus, Short transType) throws Exception {
		return transUserMapper.getTransUserByType(tradeNo, tradeStatus, transType);
	}

	@Override
	public ResultBO<?> findUserTransByOrderCode(TransUserVO transUser) throws Exception {
		try {
			List<TransUserBO> list = transUserMapper.getUserTransRecordByOrderCode(transUser);
			if (ObjectUtil.isBlank(list)) {
				return ResultBO.err(MessageCodeConstants.DATA_NOT_FOUND_SYS);
			} else {
				return ResultBO.ok(list);
			}
		} catch (Exception e) {
			logger.error("查询用户{}订单{}保底流水异常", transUser.getUserId(), transUser.getOrderCode(), e);
			return ResultBO.err(MessageCodeConstants.FIND_DATA_EXCEPTION_SERVICE);
		}
	}

	@Override
	public List<TransUserBO> findOrderGroupTransRecord(List<String> list, Short transType, Short transStatus) {
		return transUserMapper.getOrderGroupTransRecord(list, transType, transStatus);
	}

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
			return resultBO;
		}
		try {
			return ResultBO.ok(transUserMapper.getUserTransRecordByOrderCode(transUser));
		} catch (Exception e) {
			logger.error(ResultBO.getMsg(MessageCodeConstants.FIND_DATA_EXCEPTION_SERVICE, e));
			return ResultBO.err(MessageCodeConstants.FIND_DATA_EXCEPTION_SERVICE);
		}
	}

	@Override
	public int updateTransUserByBatch(List<TransUserPO> list) throws Exception {
		return transUserMapper.updateTransUserByBatch(list);
	}

	@Override
	public int updateAwardFlagById(Short awardFlag, Integer id) throws Exception {
		return transUserMapper.updateAwardFlagById(awardFlag, id);
	}

	/**
	 * 查询渠道充值列表
	 * @author zhouyang
	 * @param vo
	 * @date 2018.6.8
	 * @return
	 */
	@Override
	public List<ChannelRechargeBO> findChannelTransRechargeList(OrderChannelVO vo) {
		List<ChannelRechargeBO> rList = transUserMapper.queryChannelRechargeList(vo);
		if (!ObjectUtil.isBlank(rList)) {
			for (ChannelRechargeBO rechargeBO : rList) {
				if (!ObjectUtil.isBlank(rechargeBO.getChannelMemberId()) && rechargeBO.getChannelMemberId().contains("_")) {
					String memberId = rechargeBO.getChannelMemberId().split("_")[1].toString().trim();
					rechargeBO.setChannelMemberId(memberId);
				}
			}
		}
		return rList;
	}

}
