package com.hhly.paycore.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.common.RefundUtil;
import com.hhly.paycore.dao.OrderAddedIssueMapper;
import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.dao.TransRechargeDetailMapper;
import com.hhly.paycore.dao.TransRechargeMapper;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.OrderAddedIssuePO;
import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.RefundOrderService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderAddedIssueBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransRechargeDetailBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

@Service
public class RefundOrderServiceImpl implements RefundOrderService {
	private static final Logger logger = Logger.getLogger(RefundOrderServiceImpl.class);

	@Resource
	private UserWalletService userWalletService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Resource
	private OrderAddedIssueMapper orderAddedIssueMapper;
	@Resource
	private TransRechargeMapper transRechargeMapper;
	@Resource
	private TransRechargeDetailMapper transRechargeDetailMapper;

	@Override
	public ResultBO<?> modifyRefundOrder(CancellationRefundBO cancellationRefundBO, TransUserBO transUserBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo, List<OrderAddedIssueBO> orderAddedIssues) throws Exception {
		// 1. 根据订单类型及退款方式（系统撤单还是人为撤单）
		updateUserwalletOrAddRedColor(cancellationRefundBO, transUserBO, operateCouponBO, orderInfo);

		// 2. 修改订单状态
		updateOrderStatus(cancellationRefundBO, orderAddedIssues, orderInfo);
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> modifyRefundOrderByBatch(Map<CancellationRefundBO, List<OrderAddedIssueBO>> map, TransUserBO transUserBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		for (Map.Entry<CancellationRefundBO, List<OrderAddedIssueBO>> singleMap : map.entrySet()) {
			// 1. 根据订单类型及退款方式（系统撤单还是人为撤单）
			updateUserwalletOrAddRedColor(singleMap.getKey(), transUserBO, operateCouponBO, orderInfo);

			// 2. 修改订单状态
			updateOrderStatus(singleMap.getKey(), singleMap.getValue(), orderInfo);
		}
		return ResultBO.ok();
	}

	/**
	 * 按金额使用组成部分，退至相应的账户
	 * 代购和追号中（用户撤单），判断优惠券是否为上一次退款所生成，如果是，不会重新退回
	 * @param cancellationRefundBO
	 * @param transUserBO 交易流水
	 * @return
	 */
	public void updateUserwalletOrAddRedColor(CancellationRefundBO cancellationRefundBO, TransUserBO transUserBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		// String orderCode = orderInfo.getOrderCode();
		Integer userId = orderInfo.getUserId();
		// 代购订单撤单
		if (CancellationConstants.OrderTypeEnum.INDENTORDER.getKey().equals(cancellationRefundBO.getOrderType())) {
			// 代购订单，撤单
			fullBackBalance(transUserBO, cancellationRefundBO, operateCouponBO, orderInfo);
		} else {
			// 追号订单。已消费部分按购彩使用顺序扣除后在按订单金额组成倒序进行退款，如使用彩金红包则退回一个未使用金额等值的新的彩金红包至红包账户
			Double needRefundAmount = cancellationRefundBO.getRefundAmount();// 需要退款金额
			Double orderAmount = orderInfo.getOrderAmount();// 订单支付金额
			// 订单金额与需要退款的金额一致，全额退
			if (MathUtil.compareTo(orderAmount, needRefundAmount) == 0) {
				logger.info("订单【" + orderInfo.getOrderCode() + "】全额退款金额【" + needRefundAmount + "】");
				fullBackBalance(transUserBO, cancellationRefundBO, operateCouponBO, orderInfo);
			} else {
				String redCode = "";
				if (!ObjectUtil.isBlank(operateCouponBO)) {
					if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
						// 使用的彩金红包
						redCode = OrderNoUtil.getOrderNo(NumberCode.COUPON);
						// 部分退款
						ResultBO<?> resultBO = partBackBalance(transUserBO, cancellationRefundBO, orderInfo);
						PayChildWalletVO payChildWallet = (PayChildWalletVO) resultBO.getData();
						Double redBalance = payChildWallet.getUseAmountRed();
						// 红包金额大于0，才生成红包
						if (MathUtil.compareTo(redBalance, 0d) > 0) {
							// 重新生成彩金红包
							String activityCode = operateCouponBO.getRedCode();
							operateCouponBO.setActivityCode(activityCode);// 活动编号，用上一个红包的金额设置
							operateCouponBO.setRedRemark(Constants.RED_REMARK_CANCEL_SEND);
							operateCouponBO.setRedCode(redCode);
							operateCouponBO.setRedBalance(redBalance);
							operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 设置成可使用
							operateCouponBO.setRedValue(redBalance);
							operateCouponBO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
							operateCouponBO.setUseTime(null);
							operateCouponBO.setActiveEndTime(null);// 激活截止时间
							operateCouponBO.setObtainTime(DateUtil.convertStrToDate(DateUtil.getNow()));// 用户获取红包的时间
							// String sevenAfterDay = DateUtil.getBeforeOrAfterYearForString(Constants.RED_COLOR_OVERDUE_TIME, DateUtil.DATE_FORMAT) + " 23:59:59";
							// Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
							// operateCouponBO.setRedOverdueTime(redOverdueTime);
							// 1、生成红包
							operateCouponService.addRedColor(operateCouponBO);
							// 2、生成红包生成记录
							transRedService.addTransRed(redCode, redBalance, userId, cancellationRefundBO.getOrderCode(), PayConstants.RedTypeEnum.RED_COLOR.getKey());
						}
						// 3、添加交易记录
						addTransRecord(payChildWallet, redCode, orderInfo, redBalance);
					} else {
						// 使用的优惠券
						// 退款账户顺序：中奖账户>80%账户>20%账户>彩金红包账户
						Double redValue = operateCouponBO.getRedValue();// 红包面额
						Double newRedValue = 0d;
						if (CancellationConstants.RefundTypeEnum.USERCANCELREFUND.getKey().equals(cancellationRefundBO.getRefundType())) {// 1-中奖停追退款;2-单期撤单退款;3-用户撤单退款
							// 用户撤单，优先用钱包中的金额，再退回有效期为7天的优惠券
							ResultBO<?> resultBO = updateUserRequestBack(transUserBO, cancellationRefundBO, operateCouponBO, orderInfo);
							PayChildWalletVO payChildWallet = (PayChildWalletVO) resultBO.getData();//
							Double useRedBalance = payChildWallet.getNeedSubCouponAmount();// 需要使用红包
							if (MathUtil.compareTo(redValue, useRedBalance) > 0) {
								// 红包来源不为空，并且不是撤单生成的，可以重新生成一个优惠券，如果这个红包是之前撤单生成的，就不再生成优惠券了
								if (!ObjectUtil.isBlank(operateCouponBO.getRedSource()) && PayConstants.RedSourceEnum.BACK_ORDER.getKey().equals(operateCouponBO.getRedSource())) {
									logger.info("用户【" + userId + "】红包【" + operateCouponBO + "】来源为系统撤单生成，再次撤单不会重新生成");
								} else {
									redCode = OrderNoUtil.getOrderNo(NumberCode.COUPON);
									// 生成一个优惠券
									newRedValue = MathUtil.sub(redValue, useRedBalance);
									// 重新生成一张优惠券
									regenerateCoupon(operateCouponBO, newRedValue, orderInfo.getOrderCode(), redCode);
								}
							}
							addTransRecord(payChildWallet, redCode, orderInfo, newRedValue);
						} else {
							// 系统撤单
							ResultBO<?> resultBO = updateSystemRequestBack(transUserBO, cancellationRefundBO, operateCouponBO, orderInfo);
							PayChildWalletVO payChildWallet = (PayChildWalletVO) resultBO.getData();//
							newRedValue = payChildWallet.getNeedSubCouponAmount();// 需要使用红包
							redCode = OrderNoUtil.getOrderNo(NumberCode.COUPON);
							// 重新生成一张优惠券
							regenerateCoupon(operateCouponBO, newRedValue, orderInfo.getOrderCode(), redCode);
							addTransRecord(payChildWallet, redCode, orderInfo, newRedValue);
						}
					}
				} else {
					// 没有使用红包及优惠券
					ResultBO<?> resultBO = partBackBalance(transUserBO, cancellationRefundBO, orderInfo);
					PayChildWalletVO payChildWallet = (PayChildWalletVO) resultBO.getData();//
					payChildWallet.setTradeAmount(needRefundAmount);
					addTransRecord(payChildWallet, redCode, orderInfo, 0d);
				}
			}
		}
	}

	/**  
	* 方法说明: 全额退款 
	* @auth: xiongJinGang
	* @param transUserBO
	* @param cancellationRefundBO
	* @param operateCouponBO
	* @param orderInfo
	* @throws Exception
	* @time: 2017年7月12日 下午4:53:24
	* @return: void 
	*/
	private void fullBackBalance(TransUserBO transUserBO, CancellationRefundBO cancellationRefundBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		Double useAmountRed = transUserBO.getRedTransAmount();// 使用彩金红包金额
		Double useAmount20 = transUserBO.getAmount20();// 使用20%部分金额
		Double useAmount80 = transUserBO.getAmount80();// 使用80%部分金额
		Double useAmountWin = transUserBO.getAmountWin();// 使用中奖部分金额
		Integer userId = transUserBO.getUserId();
		String orderCode = cancellationRefundBO.getOrderCode();
		String orderRemark = "代购订单";// 订单类型；1-代购订单；2-追号计划
		if (cancellationRefundBO.getOrderType().equals(2)) {
			orderRemark = "追号订单";
		}

		logger.info(orderRemark + "【" + orderCode + "】退款至账户钱包开始");

		// 代购订单中的追号撤单
		if (!ObjectUtil.isBlank(cancellationRefundBO.getRefundType()) && cancellationRefundBO.getRefundType().equals(CancellationConstants.OrderRefundTypeEnum.CHASE_REFUND.getKey())) {
			Double refundAmount = cancellationRefundBO.getRefundAmount();// 退款金额

			// （先消费彩金红包，再20%，再80%，再中奖金额）
			if (MathUtil.compareTo(refundAmount, useAmountWin) > 0) {
				// 中奖金额全退，剩余需要退款的金额
				refundAmount = MathUtil.sub(refundAmount, useAmountWin);
				if (MathUtil.compareTo(refundAmount, useAmount80) > 0) {
					// 剩余退款金额大于使用的80%金额，80%金额全退
					refundAmount = MathUtil.sub(refundAmount, useAmount80);
					if (MathUtil.compareTo(refundAmount, useAmount20) > 0) {
						// 剩余退款金额大于使用的20%金额，20%金额全退。
						// 剩余退还红包金额
						useAmountRed = MathUtil.sub(refundAmount, useAmount20);
					} else {
						useAmount20 = refundAmount;
						useAmountRed = 0d;
					}
				} else {
					useAmount80 = refundAmount;
					useAmount20 = 0d;
					useAmountRed = 0d;
				}
			} else {
				useAmountWin = refundAmount;// 全部中奖金额账户返回
				useAmount20 = 0d;
				useAmount80 = 0d;
				useAmountRed = 0d;
			}
			//
			ResultBO<?> resultBO = updateRechargeTakenStatus(orderCode, useAmount20);
			// 返回成功，需要将退到20%的金额退到80%中
			String data = (String) resultBO.getData();
			if (Boolean.parseBoolean(data) && MathUtil.compareTo(useAmount20, 0d) > 0) {
				useAmount80 = MathUtil.add(useAmount80, useAmount20);
				useAmount20 = 0d;
			}
			Double addRedAmount = useAmountRed;
			// 使用了红包 ，并且不是彩金红包，不要向账户中的彩金红包中加钱
			if (!ObjectUtil.isBlank(operateCouponBO) && !operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
				addRedAmount = 0d;
			}
			resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, addRedAmount, MoneyFlowEnum.IN.getKey());
			if (resultBO.isError()) {
				logger.error(orderRemark + "【" + orderCode + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
				throw new RuntimeException("更新子账户钱包金额失败");
			}
			// 这里要重新生成一个红包，2017-08-04之前 ，追号订单还没有使用过红包
			String redCode = createRedColor(cancellationRefundBO, operateCouponBO, useAmountRed, userId, orderCode);

			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, useAmountRed, useAmount20, useAmount80, useAmountWin, 0d, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
			payChildWallet.setTradeAmount(cancellationRefundBO.getRefundAmount());
			// 添加交易流水
			addTransRecord(payChildWallet, redCode, orderInfo, useAmountRed);
		} else {
			// 代购订单撤单
			if (MathUtil.compareTo(orderInfo.getOrderAmount(), cancellationRefundBO.getRefundAmount()) != 0) {
				logger.info("代购订单金额【" + orderInfo.getOrderAmount() + "】与撤单退款金额【" + cancellationRefundBO.getRefundAmount() + "】不符");
				throw new RuntimeException("代购订单金额与撤单退款金额不符");
			}
			Double addRedAmount = useAmountRed;
			// 使用了红包 ，并且不是彩金红包，不要向账户中的彩金红包中加钱
			if (!ObjectUtil.isBlank(operateCouponBO) && !operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
				addRedAmount = 0d;
			}
			// 代购订单。按订单金额组成部分进行退款，如使用了彩金红包，则退回一个使用金额等值的新的彩金红包，已用优惠劵重新生成一张等额且有效期为7天的新优惠劵
			ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, addRedAmount, MoneyFlowEnum.IN.getKey());
			if (resultBO.isError()) {
				logger.error(orderRemark + "【" + orderCode + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
				throw new RuntimeException("更新子账户钱包金额失败");
			}
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

			if (ObjectUtil.isBlank(operateCouponBO) && !ObjectUtil.isBlank(useAmountRed)) {
				logger.error(orderRemark + "【" + orderCode + "】撤单，使用了彩金红包金额，交易流水中没有红包编号");
				throw new RuntimeException("红包使用错误");
			}
			// 生成彩金红包或者优惠券
			String redCode = createRedColor(cancellationRefundBO, operateCouponBO, useAmountRed, userId, orderCode);

			// 更新充值记录中的提款状态，如果是追号的退款，根据提款状态判断是否需要将20%的金额转移到80%中
			resultBO = updateRechargeTakenStatus(orderCode, useAmount20);
			// 返回成功，需要将退到20%的金额退到80%中
			String data1 = (String) resultBO.getData();
			if (Boolean.parseBoolean(data1) && MathUtil.compareTo(useAmount20, 0d) > 0) {
				useAmount80 = MathUtil.add(useAmount80, useAmount20);
				useAmount20 = 0d;
			}
			PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, useAmountRed, useAmount20, useAmount80, useAmountWin, 0d, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
			payChildWallet.setTradeAmount(cancellationRefundBO.getRefundAmount());
			// 添加交易流水
			addTransRecord(payChildWallet, redCode, orderInfo, useAmountRed);
		}
	}

	/**  
	* 方法说明: 生成彩金红包 
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @param operateCouponBO
	* @param useAmountRed
	* @param userId
	* @param orderCode
	* @throws Exception
	* @time: 2017年9月13日 下午5:56:40
	* @return: String 
	*/
	private String createRedColor(CancellationRefundBO cancellationRefundBO, OperateCouponBO operateCouponBO, Double useAmountRed, Integer userId, String orderCode) throws Exception {
		String redCode = "";
		// 使用了彩金或者优惠券，并且金额大于0，才重新生成
		if (!ObjectUtil.isBlank(operateCouponBO) && MathUtil.compareTo(useAmountRed, 0d) > 0) {
			redCode = OrderNoUtil.getOrderNo(NumberCode.COUPON);
			String activityCode = operateCouponBO.getRedCode();
			operateCouponBO.setActivityCode(activityCode);// 活动编号，用上一个红包的金额设置
			operateCouponBO.setRedRemark(Constants.RED_REMARK_CANCEL_SEND);
			operateCouponBO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
			operateCouponBO.setUseTime(null);
			operateCouponBO.setActiveEndTime(null);// 激活截止时间
			operateCouponBO.setObtainTime(DateUtil.convertStrToDate(DateUtil.getNow()));// 用户获取红包的时间
			// 使用了彩金红包
			if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
				// 重新生成彩金红包
				operateCouponBO.setRedBalance(useAmountRed);
				operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 设置成可使用
				operateCouponBO.setRedValue(useAmountRed);
				operateCouponBO.setRedCode(redCode);
				// String sevenAfterDay = DateUtil.getBeforeOrAfterYearForString(Constants.RED_COLOR_OVERDUE_TIME, DateUtil.DATE_FORMAT) + " 23:59:59";
				// Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
				// operateCouponBO.setRedOverdueTime(redOverdueTime);
				// 1、生成红包
				operateCouponService.addRedColor(operateCouponBO);
				// 2、生成红包生成记录
				transRedService.addTransRed(redCode, useAmountRed, userId, cancellationRefundBO.getOrderCode(), PayConstants.RedTypeEnum.RED_COLOR.getKey());
			} else {
				// 使用了优惠券
				// 红包来源不为空，并且不是撤单生成的，可以重新生成一个优惠券，如果这个红包是之前撤单生成的，就不再生成优惠券了
				if (!ObjectUtil.isBlank(operateCouponBO.getRedSource()) && PayConstants.RedSourceEnum.BACK_ORDER.getKey().equals(operateCouponBO.getRedSource())) {
					logger.info("用户【" + userId + "】红包【" + operateCouponBO + "】来源为系统撤单生成，再次撤单不会重新生成");
				} else {
					// 重新生成7天有效期的红包
					regenerateCoupon(operateCouponBO, operateCouponBO.getRedValue(), orderCode, redCode);
				}
			}
		}
		return redCode;
	}

	/**  
	* 方法说明: 部分退款（先消费彩金红包，再20%，再80%，再中奖金额）
	* @auth: xiongJinGang
	* @param transUserBO
	* @param cancellationRefundBO
	* @param operateCouponBO
	* @param orderInfo
	* @throws Exception
	* @time: 2017年7月13日 下午3:44:24
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> partBackBalance(TransUserBO transUserBO, CancellationRefundBO cancellationRefundBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		Double useAmountRed = transUserBO.getRedTransAmount();// 使用彩金红包金额
		Double useAmount20 = transUserBO.getAmount20();// 使用20%部分金额
		Double useAmount80 = transUserBO.getAmount80();// 使用80%部分金额
		Double useAmountWin = transUserBO.getAmountWin();// 使用中奖部分金额
		Integer userId = transUserBO.getUserId();
		String orderCode = cancellationRefundBO.getOrderCode();
		String orderRemark = "追号订单";// 订单类型；1-代购订单；2-追号计划
		logger.info(orderRemark + "【" + orderCode + "】退款至账户钱包开始，");

		Double needBackAmount = cancellationRefundBO.getRefundAmount();// 需要退款的金额

		Double threeAmount = MathUtil.add(useAmount20, useAmount80, useAmountWin);
		Double twoAmount = MathUtil.add(useAmount80, useAmountWin);
		// 退款金额大于3个账户金额之和，3个账户全额退
		if (MathUtil.compareTo(needBackAmount, threeAmount) >= 0) {
			// 需要退红包金额
			useAmountRed = MathUtil.sub(needBackAmount, threeAmount);
		} else if (MathUtil.compareTo(needBackAmount, twoAmount) >= 0 && MathUtil.compareTo(needBackAmount, threeAmount) < 0) {
			useAmountRed = 0d;
			useAmount20 = MathUtil.sub(needBackAmount, twoAmount);
		} else if (MathUtil.compareTo(needBackAmount, useAmountWin) >= 0 && MathUtil.compareTo(needBackAmount, twoAmount) < 0) {
			useAmountRed = 0d;
			useAmount20 = 0d;
			useAmount80 = MathUtil.sub(needBackAmount, useAmountWin);
		} else {
			useAmountRed = 0d;
			useAmount20 = 0d;
			useAmount80 = 0d;
			useAmountWin = needBackAmount;
		}
		// 更新充值记录中的提款状态，如果是追号的退款，根据提款状态判断是否需要将20%的金额转移到80%中
		ResultBO<?> resultBO = updateRechargeTakenStatus(orderCode, useAmount20);
		// 返回成功，需要将退到20%的金额退到80%中
		String data = (String) resultBO.getData();
		if (Boolean.parseBoolean(data) && MathUtil.compareTo(useAmount20, 0d) > 0) {
			useAmount80 = MathUtil.add(useAmount80, useAmount20);
			useAmount20 = 0d;
		}

		// 代购订单。按订单金额组成部分进行退款，如使用了彩金红包，则退回一个使用金额等值的新的彩金红包，已用优惠劵重新生成一张等额且有效期为7天的新优惠劵
		resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, useAmountRed, MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			logger.error(orderRemark + "【" + orderCode + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
			throw new RuntimeException("更新子账户钱包金额失败");
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, useAmountRed, useAmount20, useAmount80, useAmountWin, 0d, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
		payChildWallet.setTradeAmount(cancellationRefundBO.getRefundAmount());
		return ResultBO.ok(payChildWallet);
	}

	/**  
	* 方法说明: 用户撤单，优先使用钱包金额，未使用优惠劵时退回等额的7天有效期的新优惠劵与未使用的钱包金额
	* // 退款账户顺序：中奖账户>80%账户>20%账户>优惠券
	* @auth: xiongJinGang
	* @param transUserBO
	* @param cancellationRefundBO
	* @param operateCouponBO
	* @param orderInfo
	* @throws Exception
	* @time: 2017年7月12日 下午6:46:51
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateUserRequestBack(TransUserBO transUserBO, CancellationRefundBO cancellationRefundBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		Double useAmountRed = 0d;// 使用彩金红包金额
		Double needSubCouponAmount = 0d;// 需要扣除优惠卷中的金额
		Double useAmount20 = transUserBO.getAmount20();// 使用20%部分金额
		Double useAmount80 = transUserBO.getAmount80();// 使用80%部分金额
		Double useAmountWin = transUserBO.getAmountWin();// 使用中奖部分金额
		Integer userId = transUserBO.getUserId();
		String orderCode = cancellationRefundBO.getOrderCode();

		String orderRemark = "追号订单，用户撤单";// 订单类型；1-代购订单；2-追号计划
		logger.info(orderRemark + "【" + orderCode + "】退款至账户钱包开始");
		Double needBackAmount = cancellationRefundBO.getRefundAmount();// 需要退款的金额
		Double totalOrderAmount = orderInfo.getOrderAmount();// 订单总金额
		// 已经使用的金额
		Double alreadyUseAmount = MathUtil.sub(totalOrderAmount, needBackAmount);

		Double totalAmount = useAmount20;// 总使用金额

		if (MathUtil.compareTo(alreadyUseAmount, useAmount20) > 0) {
			useAmount20 = 0d;
			// 20% 加上 80%金额
			totalAmount = MathUtil.add(totalAmount, useAmount80);
			if (MathUtil.compareTo(alreadyUseAmount, totalAmount) >= 0) {
				useAmount80 = 0d;
				// 20% 加上 80% 加上中奖金额
				totalAmount = MathUtil.add(totalAmount, useAmountWin);
				if (MathUtil.compareTo(alreadyUseAmount, totalAmount) >= 0) {
					useAmountWin = 0d;
					needSubCouponAmount = MathUtil.sub(alreadyUseAmount, totalAmount);// 需要扣除红包的金额
				} else {
					useAmountWin = MathUtil.sub(totalAmount, alreadyUseAmount);
				}
			} else {
				// 20%部分已经使用完
				useAmount80 = MathUtil.sub(totalAmount, alreadyUseAmount);
			}
		} else {
			// 已使用金额小于20%使用部分，20%退还部分为（使用20%部分金额-已使用20%金额），其它钱包账户金额不变
			useAmount20 = MathUtil.sub(useAmount20, alreadyUseAmount);
		}

		// 更新充值记录中的提款状态，如果是追号的退款，根据提款状态判断是否需要将20%的金额转移到80%中
		ResultBO<?> resultBO = updateRechargeTakenStatus(orderCode, useAmount20);
		// 返回成功，需要将退到20%的金额退到80%中
		String data = (String) resultBO.getData();
		if (Boolean.parseBoolean(data) && MathUtil.compareTo(useAmount20, 0d) > 0) {
			useAmount80 = MathUtil.add(useAmount80, useAmount20);
			useAmount20 = 0d;
		}
		Double addRedAmount = useAmountRed;
		// 使用了红包 ，并且不是彩金红包，不要向账户中的彩金红包中加钱
		if (!ObjectUtil.isBlank(operateCouponBO) && !operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
			addRedAmount = 0d;
		}
		// 代购订单。按订单金额组成部分进行退款，如使用了彩金红包，则退回一个使用金额等值的新的彩金红包，已用优惠劵重新生成一张等额且有效期为7天的新优惠劵
		resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, addRedAmount, MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			logger.error(orderRemark + "【" + orderCode + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
			throw new RuntimeException("更新子账户钱包金额失败");
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, useAmountRed, useAmount20, useAmount80, useAmountWin, needSubCouponAmount, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
		payChildWallet.setTradeAmount(cancellationRefundBO.getRefundAmount());
		return ResultBO.ok(payChildWallet);
	}

	/**  
	* 方法说明：系统撤单 
	* @auth: xiongJinGang
	* @param transUserBO
	* @param cancellationRefundBO
	* @param operateCouponBO
	* @param orderInfo
	* @throws Exception
	* @time: 2017年8月22日 上午10:53:29
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateSystemRequestBack(TransUserBO transUserBO, CancellationRefundBO cancellationRefundBO, OperateCouponBO operateCouponBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		Double useAmountRed = 0d;// 使用彩金红包金额
		Double needSubCouponAmount = 0d;// 需要扣除优惠卷中的金额
		Double useAmount20 = transUserBO.getAmount20();// 使用20%部分金额
		Double useAmount80 = transUserBO.getAmount80();// 使用80%部分金额
		Double useAmountWin = transUserBO.getAmountWin();// 使用中奖部分金额
		Integer userId = transUserBO.getUserId();
		String orderCode = cancellationRefundBO.getOrderCode();

		String orderRemark = "追号订单，用户撤单";// 订单类型；1-代购订单；2-追号计划
		logger.info(orderRemark + "【" + orderCode + "】退款至账户钱包开始，");
		Double needBackAmount = cancellationRefundBO.getRefundAmount();// 需要退款的金额

		Double totalAmount = 0d;// 总使用金额
		// 需要退款的金额大于等于使用的中奖金额，中奖账户全额退
		if (MathUtil.compareTo(needBackAmount, useAmountWin) > 0) {// 中奖账户判断
			totalAmount = MathUtil.add(totalAmount, useAmountWin);
			// 中奖金额+80%金额
			totalAmount = MathUtil.add(totalAmount, useAmount80);// 80%账户判断
			if (MathUtil.compareTo(needBackAmount, totalAmount) > 0) {
				// 中奖金额+80%金额+20%金额
				totalAmount = MathUtil.add(totalAmount, useAmount20);// 20%账户判断

				if (MathUtil.compareTo(needBackAmount, totalAmount) > 0) {
					needSubCouponAmount = MathUtil.sub(needBackAmount, totalAmount);
				} else {
					// 需要退款的金额小于等于中奖金额+80%金额，80%退款金额=需要退款的金额-中奖金额
					useAmountRed = 0d;
					useAmount20 = MathUtil.sub(needBackAmount, MathUtil.add(useAmountWin, useAmount80));
				}
			} else {
				// 需要退款的金额小于等于中奖金额+80%金额，80%退款金额=需要退款的金额-中奖金额
				useAmountRed = 0d;
				useAmount20 = 0d;
				useAmount80 = MathUtil.sub(needBackAmount, useAmountWin);
			}
		} else {
			// 使用的资金金额大于需要退款的金额，中奖账户直接加上需要退款的金额
			useAmountRed = 0d;
			useAmount20 = 0d;
			useAmount80 = 0d;
			useAmountWin = needBackAmount;
		}

		// 更新充值记录中的提款状态，如果是追号的退款，根据提款状态判断是否需要将20%的金额转移到80%中
		ResultBO<?> resultBO = updateRechargeTakenStatus(orderCode, useAmount20);
		// 返回成功，需要将退到20%的金额退到80%中
		String data = (String) resultBO.getData();
		if (Boolean.parseBoolean(data) && MathUtil.compareTo(useAmount20, 0d) > 0) {
			useAmount80 = MathUtil.add(useAmount80, useAmount20);
			useAmount20 = 0d;
		}
		Double addRedAmount = useAmountRed;
		// 使用了红包 ，并且不是彩金红包，不要向账户中的彩金红包中加钱
		if (!ObjectUtil.isBlank(operateCouponBO) && !operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
			addRedAmount = 0d;
		}
		// 代购订单。按订单金额组成部分进行退款，如使用了彩金红包，则退回一个使用金额等值的新的彩金红包，已用优惠劵重新生成一张等额且有效期为7天的新优惠劵
		resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, addRedAmount, MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			logger.error(orderRemark + "【" + orderCode + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
			throw new RuntimeException("更新子账户钱包金额失败");
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, useAmountRed, useAmount20, useAmount80, useAmountWin, needSubCouponAmount, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
		payChildWallet.setTradeAmount(cancellationRefundBO.getRefundAmount());
		return ResultBO.ok(payChildWallet);
	}

	/**
	 * 更新订单状态
	 *
	 * @param cancellationRefundBO
	 * @param orderAddedIssues     追号详情List
	 * @throws Exception
	 */
	@Override
	public void updateOrderStatus(CancellationRefundBO cancellationRefundBO, List<OrderAddedIssueBO> orderAddedIssues, PayOrderBaseInfoVO cancelOrderInfo) throws Exception {
		try {
			PayOrderUpdatePO payOrderUpdatePO = new PayOrderUpdatePO();
			payOrderUpdatePO.setOrderCode(cancellationRefundBO.getOrderCode());
			// payOrderUpdatePO.setPayStatus(PayConstants.PayStatusEnum.REFUND.getKey());
			if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(cancellationRefundBO.getOrderType())) {
				// 如果撤单追号期数记录条数等于剩余待追号期数，修改追号计划记录的支付状态和追号状态
				for (OrderAddedIssueBO addedIssue : orderAddedIssues) {
					OrderAddedIssuePO orderAddedIssuePO = new OrderAddedIssuePO();
					orderAddedIssuePO.setAddStatus(RefundUtil.getOrderAddedIssueAddStatus(addedIssue.getAddStatus()).getKey());
					orderAddedIssuePO.setId(addedIssue.getId());
					logger.debug("修改追号详情ID【" + addedIssue.getId() + "】状态为：" + RefundUtil.getOrderAddedIssueAddStatus(addedIssue.getAddStatus()).getKey());
					orderAddedIssueMapper.updateAddedIssue(orderAddedIssuePO);
				}
			} else {
				payOrderUpdatePO.setOrderStatus(CancellationConstants.OrderStatusEnum.CANCELLATIONOK.getKey());
				payOrderUpdateMapper.updateOrderPayStatus(payOrderUpdatePO);
			}
		} catch (Exception e) {
			logger.error("更新订单【" + cancellationRefundBO.getOrderCode() + "】状态失败", e);
			throw new Exception("更新订单状态失败");
		}
	}

	/**  
	* 方法说明: 添加撤单交易记录
	* @auth: xiongJinGang
	* @param payChildWallet
	* @param redCode
	* @param orderCode
	* @param redBalance
	* @throws Exception
	* @time: 2017年7月15日 上午10:50:33
	* @return: void 
	*/
	public void addTransRecord(PayChildWalletVO payChildWallet, String redCode, PayOrderBaseInfoVO orderInfo, Double redBalance) throws Exception {
		Short transType = PayConstants.TransTypeEnum.REFUND.getKey();// 退款
		payChildWallet.setRedCode(redCode);
		payChildWallet.setOrderCode(orderInfo.getOrderCode());
		payChildWallet.setOperateRemark(Constants.RED_REMARK_CANCEL_SEND);
		payChildWallet.setOrderInfo(PayUtil.getRefundRemark(orderInfo));
		payChildWallet.setRedAmount(redBalance);
		payChildWallet.setTransType(transType);
		payChildWallet.setChannelId(orderInfo.getChannelId());
		// 生成后端的交易流水
		TransUserPO transUserPO = transUserService.addTransUserRecord(payChildWallet, null);
		// 生成给用户查看的交易流水
		transUserLogService.addTransLogRecord(transUserPO);
	}

	/**  
	* 方法说明: 重新生成一个优惠券
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @param newRedValue
	* @param orderCode
	* @throws Exception
	* @time: 2017年7月13日 下午3:32:23
	* @return: void 
	*/
	public void regenerateCoupon(OperateCouponBO operateCouponBO, Double newRedValue, String orderCode, String redCode) throws Exception {
		if (MathUtil.compareTo(newRedValue, 0d) > 0) {
			OperateCouponPO operateCouponInfo = new OperateCouponPO(operateCouponBO);
			Short redType = operateCouponBO.getRedType();
			// 生成一个优惠券
			operateCouponInfo.setActivityCode(operateCouponBO.getRedCode());// 活动编号，用上一个红包的金额设置
			operateCouponInfo.setRedRemark(Constants.RED_REMARK_CANCEL_SEND);
			operateCouponInfo.setRedCode(redCode);
			operateCouponInfo.setActiveEndTime(null);
			operateCouponInfo.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 设置成可使用
			operateCouponInfo.setUseTime(null);

			String sevenAfterDay = DateUtil.getBeforeOrAfterDate(Constants.RED_COLOR_EFFECTIVE_DAYS, DateUtil.DATE_FORMAT) + " 23:59:59";
			Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
			operateCouponInfo.setRedOverdueTime(redOverdueTime);
			operateCouponInfo.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
			operateCouponInfo.setEctivityDay(Constants.RED_COLOR_EFFECTIVE_DAYS);// 红包有效天数
			// 满减红包
			if (PayConstants.RedTypeEnum.CONSUMPTION_DISCOUNT.getKey().equals(redType)) {
				operateCouponInfo.setRedValue(newRedValue);// 满100减20，但是用了20中的一部分，生成的彩金红包就是满100减剩余金额
				operateCouponInfo.setRedName("满减红包");
			}

			operateCouponInfo.setRedSource(PayConstants.RedSourceEnum.BACK_ORDER.getKey());// 系统撤单生成
			logger.info("撤单重新生成一个优惠券：" + redCode);
			operateCouponService.addCoupon(operateCouponInfo);
			transRedService.addTransRed(redCode, newRedValue, operateCouponBO.getUserId(), orderCode, PayConstants.RedTypeEnum.CONSUMPTION_DISCOUNT.getKey());
		}
	}

	/**  
	* 方法说明: 根据订单编号获取充值记录
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年8月21日 下午6:55:04
	* @return: TransRechargeBO 
	*/
	private TransRechargeBO findTransRecharge(String orderCode) {
		TransRechargeDetailBO transRechargeDetailBO = transRechargeDetailMapper.getRechargeDetailByOrderCode(orderCode);
		if (!ObjectUtil.isBlank(transRechargeDetailBO)) {
			return transRechargeMapper.getRechargeByCode(transRechargeDetailBO.getRechargeCode());
		}
		return null;
	}

	/**  
	* 方法说明: 退款时，需要更新充值记录中即买即付的提款状态：不可提款状态修改成可提款状态，如果批量支付的已提状态，要将当前退款的20%账户的金额转移到80%账户中
	* 返回ResultBO.ok(true)时，表示20%账户金额需要转移，否则不需要处理
	* @auth: xiongJinGang
	* @param orderCode
	* @throws Exception
	* @time: 2017年8月22日 上午10:37:42
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> updateRechargeTakenStatus(String orderCode, Double useAmount20) throws Exception {
		TransRechargeBO transRechargeBO = findTransRecharge(orderCode);
		// 是即买即付的充值记录并且需要退还20%账户大于0，才允许修改充值记录的状态
		if (!ObjectUtil.isBlank(transRechargeBO) && MathUtil.compareTo(useAmount20, 0d) > 0) {
			// D1705222208440100066,1;D1705222201040100065,1;
			String orderCodes = transRechargeBO.getOrderCode();
			// 订单号为空，不是即买即付的充值记录
			if (ObjectUtil.isBlank(orderCodes)) {
				logger.info("充值记录【" + transRechargeBO.getTransRechargeCode() + "】的订单编号为空，无法更新充值记录中的提款状态");
				return ResultBO.err();
			}
			if (StringUtils.isBlank(String.valueOf(transRechargeBO.getTakenStatus()))) {
				logger.info("充值记录【" + transRechargeBO.getTransRechargeCode() + "】的提款为空，无法更新充值记录中的提款状态");
				return ResultBO.err();
			}
			// 提款状态不为空，并且提款状态为不可提，需要修改充值状态值为可提
			if (transRechargeBO.getTakenStatus().equals(PayConstants.RechargeTakenStatusEnum.NOT_ALLOW.getKey())) {
				TransRechargePO transRechargePO = new TransRechargePO(transRechargeBO.getId(), PayConstants.RechargeTakenStatusEnum.ALLOW.getKey());
				int result = transRechargeMapper.update(transRechargePO);
				if (result <= 0) {
					logger.info("更新充值记录【" + transRechargeBO.getTransRechargeCode() + "】提款状态到可提失败");
					throw new RuntimeException("更新充值记录的提款状态失败");
				}
				return ResultBO.ok("false");
			}
			// 单个订单支付与多个订单支付，处理逻辑不一样
			boolean isSinglePay = RefundUtil.isSinglePay(orderCodes);
			// 验证是否为单个支付，如果是单个支付，需要将充值记录的提款状态改成可提，每个账户的退款金额是多少，退多少
			if (!isSinglePay) {
				// 如果是批量支付，并且提款状态为已提，需要将20%的金额转移到80%，退至到用户的钱包中
				if (transRechargeBO.getTakenStatus().equals(PayConstants.RechargeTakenStatusEnum.FINISHED.getKey())) {
					return ResultBO.ok("true");
				}
			} else {
				logger.info("单个订单充值记录【" + transRechargeBO.getTransRechargeCode() + "】当前的提款状态【" + transRechargeBO.getTakenStatus() + "】");
				// 单个订单充值记录的提款状态如果是已提，全部退到80%账户
				if (transRechargeBO.getTakenStatus().equals(PayConstants.RechargeTakenStatusEnum.FINISHED.getKey())) {
					return ResultBO.ok("true");
				}
				// throw new RuntimeException("单个订单充值记录的提款状态错误");
			}
		}
		return ResultBO.ok("false");
	}
}
