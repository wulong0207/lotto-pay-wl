package com.hhly.paycore.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.dao.TransRechargeMapper;
import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.OrderGroupContentService;
import com.hhly.paycore.service.OrderGroupService;
import com.hhly.paycore.service.RefundOrderGroupService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.OrderGroupConstants;
import com.hhly.skeleton.base.constants.OrderGroupConstants.OrderGroupRefundTypeEnum;
import com.hhly.skeleton.base.constants.OrderGroupConstants.OrderGroupTransTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.TransUserVO;
import com.hhly.skeleton.user.bo.UserWalletBO;

@Service
public class RefundOrderGroupServiceImpl implements RefundOrderGroupService {
	private static final Logger logger = Logger.getLogger(RefundOrderGroupServiceImpl.class);

	@Resource
	private UserWalletService userWalletService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Resource
	private TransRechargeMapper transRechargeMapper;
	@Resource
	private OrderGroupContentService orderGroupContentService;
	@Resource // 合买订单服务类
	private OrderGroupService orderGroupService;

	/*合买退款类型，只有是合买订单退款时，才有值；代购、追号订单退款时为空
	1.未满员，合买流产退款处理（全退）
	2.未满员，平台保底认购账户处理（平台垫钱，扣平台账户的钱，然后加交易记录，http调用）
	3.满员，退发起人的保底账户金额的处理（全额退保底金额）
	4.系统发起的合买退款处理（同第一条）
	5.合买单出票失败的退款处理（同第一条）
	6.认购结束，合买进度大于90%时，需要退发起人一部分保底金额（如发起人保底金额20，需要扣除10元认购，另外10元退还给用户），参数refundAmount必须有值 */
	@Override
	public ResultBO<?> modifyRefundOrder(CancellationRefundBO cancellationRefundBO, List<TransUserBO> groupTransUserList, PayOrderBaseInfoVO orderInfo, double totalOrderGroupAmount) throws Exception {
		OrderGroupBO orderGroupBO = orderGroupService.findOrderGroupByOrderCode(cancellationRefundBO.getOrderCode());
		// 查找保底流水
		TransUserBO baoDiTransUserBO = queryBaoDiTransUser(orderGroupBO);

		OrderGroupRefundTypeEnum refundType = OrderGroupConstants.OrderGroupRefundTypeEnum.getEnum(cancellationRefundBO.getBuyTogetherRefundType());
		switch (refundType) {
		case NOT_FULL_ONE:// 1、未满员，合买详情中的全退，合买有保底的，需要退保底金额
			// case SYSTEM_REFUND:// 4、系统撤单
			// case FAIL_REFUND:// 5、出票失败撤单，退认购记录+保底
			// 保底交易流水不为空，并且订单金额与合买认购详情金额不一致，才需要退保底金额
			if (!ObjectUtil.isBlank(baoDiTransUserBO)) {
				// 1、先退保底金额
				logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】保底金额" + baoDiTransUserBO.getCashAmount() + "开始");
				refundByTransUser(baoDiTransUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.GUARANTEE);
			}
			// 2、退参与合买用户的金额
			logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】合买金额开始");
			refundOrderGroup(groupTransUserList, cancellationRefundBO, orderInfo);
			// 3、修改订单状态
			logger.info("修改订单【" + cancellationRefundBO.getOrderCode() + "】退款状态开始");
			updateOrderStatus(cancellationRefundBO);
			return ResultBO.ok();
		case SYSTEM_REFUND:// 4、系统撤单
		case FAIL_REFUND:// 5、出票失败撤单，全额退认购记录
			// 2、退参与合买用户的金额
			logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】合买金额开始");
			refundOrderGroup(groupTransUserList, cancellationRefundBO, orderInfo);
			// 3、修改订单状态
			logger.info("修改订单【" + cancellationRefundBO.getOrderCode() + "】退款状态开始");
			updateOrderStatus(cancellationRefundBO);
			return ResultBO.ok();

		/*// 订单金额与合买详情中的金额一致，则按合买详情中的交易流水撤单
		if (MathUtil.compareTo(orderInfo.getOrderAmount(), totalOrderGroupAmount) == 0) {
			logger.info("订单【" + cancellationRefundBO.getOrderCode() + "】金额：" + orderInfo.getOrderAmount() + "和合买详情中的金额：" + totalOrderGroupAmount + "一致，只需要退合买详情中的记录");
			// 2、退参与合买用户的金额
			logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】合买金额开始");
			refundOrderGroup(groupTransUserList, cancellationRefundBO, orderInfo);
			// 3、修改订单状态
			logger.info("修改订单【" + cancellationRefundBO.getOrderCode() + "】退款状态开始");
			updateOrderStatus(cancellationRefundBO);
		} else {// 合买详情中的金额小于订单金额，则还有一部分保底
			if (MathUtil.compareTo(orderGroupBO.getGuaranteeAmount(), 0d) > 0) {
				// 还需要退款的金额
				Double needBackAmount = MathUtil.sub(orderInfo.getOrderAmount(), totalOrderGroupAmount);
				// 计算出来还需要退款的金额等于保底金额，全额退保底
				if (MathUtil.compareTo(orderGroupBO.getGuaranteeAmount(), needBackAmount) == 0) {
					// 1、先退保底金额
					logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】保底金额" + baoDiTransUserBO.getCashAmount() + "开始");
					refundByTransUser(baoDiTransUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.GUARANTEE);
				} else {
					// 保底金额小于需要退款的金额
					if (MathUtil.compareTo(orderGroupBO.getGuaranteeAmount(), needBackAmount) <= 0 && !ObjectUtil.isBlank(baoDiTransUserBO)) {
						// 1、先退保底金额
						logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】保底金额" + baoDiTransUserBO.getCashAmount() + "开始");
						refundByTransUser(baoDiTransUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.GUARANTEE);
					} else {
						// 计算出来还需要退款的金额不等于保底金额，部分退保底
						cancellationRefundBO.setRefundAmount(needBackAmount);
						refundPartOrderGroup(cancellationRefundBO, orderInfo, baoDiTransUserBO);
					}
				}
			}
			// 2、退参与合买用户的金额
			logger.info("退订单【" + cancellationRefundBO.getOrderCode() + "】合买金额开始");
			refundOrderGroup(groupTransUserList, cancellationRefundBO, orderInfo);
			// 3、修改订单状态
			logger.info("修改订单【" + cancellationRefundBO.getOrderCode() + "】退款状态开始");
			updateOrderStatus(cancellationRefundBO);
		}
		return ResultBO.ok();*/
		case NOT_FULL_TWO:
			// 这种不处理，通过HTTP调用
			logger.info("MQ发过来的合买退款类型为" + cancellationRefundBO.getBuyTogetherRefundType() + "，与约定的不一致，无法完成退款");
			break;
		case FULL:
			// 满员，仅退发起人保底金额
			if (null == baoDiTransUserBO) {
				logger.info("满员，仅退发起人保底金额。查询保底金额交易流水为空，不做退款处理");
			} else {
				logger.info(cancellationRefundBO.getOrderCode() + "已满员，仅退订单【" + cancellationRefundBO.getOrderCode() + "】中的保底金额：" + baoDiTransUserBO.getCashAmount());
				refundByTransUser(baoDiTransUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.GUARANTEE);
				return ResultBO.ok();
			}
		case PART_REFUND:
			if (null == baoDiTransUserBO) {
				logger.info("退部分保底金额。查询保底金额交易流水为空，不做退款处理");
			} else {
				// 退部分保底金额
				return refundPartOrderGroup(cancellationRefundBO, orderInfo, baoDiTransUserBO);
			}
		default:
			logger.info("MQ发过来的合买退款类型为" + cancellationRefundBO.getBuyTogetherRefundType() + "，与约定的不一致，无法完成退款");
			break;
		}
		return ResultBO.err(MessageCodeConstants.ORDER_GROUP_REFUND_ERROR);
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @param orderInfo
	* @param baoDiTransUserBO
	* @throws Exception
	* @time: 2018年5月30日 下午4:50:06
	* @return: void 
	*/
	public ResultBO<?> refundPartOrderGroup(CancellationRefundBO cancellationRefundBO, PayOrderBaseInfoVO orderInfo, TransUserBO baoDiTransUserBO) throws Exception {
		// 保底部分退款，需要
		logger.info("订单【" + cancellationRefundBO.getOrderCode() + "】保底金额" + baoDiTransUserBO.getCashAmount() + "，需要退保底金额：" + cancellationRefundBO.getRefundAmount());
		// 判断退款金额跟保底流水中的交易金额
		if (MathUtil.compareTo(baoDiTransUserBO.getCashAmount(), cancellationRefundBO.getRefundAmount()) <= 0) {
			logger.info("订单【" + cancellationRefundBO.getOrderCode() + "】该退款类型退款失败，保底金额必须大于退款金额才能退款");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_GUARANTEE_AMOUNT_REFUND_ERROR);
		}
		// 1、需要加一条认购记录，认购金额是保底金额-退款金额
		Double buyAmount = MathUtil.sub(baoDiTransUserBO.getCashAmount(), cancellationRefundBO.getRefundAmount());
		// 合买人钱包记录
		UserWalletBO userWallet = userWalletService.findUserWalletByUserId(baoDiTransUserBO.getUserId());
		transUserService.addSubscription(cancellationRefundBO.getBuyCode(), baoDiTransUserBO.getTransCode(), orderInfo.getChannelId(), buyAmount, userWallet);
		// 2、退保底金额，如果需要转成购买金额大于0，
		if (MathUtil.compareTo(cancellationRefundBO.getRefundAmount(), 0d) > 0) {
			// 如果保底流水中20%部分大于认购金额
			double newAmount20 = 0d;
			double newAmount80 = 0d;
			double newAmountWin = 0d;
			// 保底流水中，如果使用的20%的账户金额大于需要退的金额，直接退到20%部分
			if (MathUtil.compareTo(baoDiTransUserBO.getAmount20(), cancellationRefundBO.getRefundAmount()) > 0) {
				// 使用的20%金额大于需要退的金额，直接将退的金额退到20%账户
				// newAmount20 = MathUtil.sub(baoDiTransUserBO.getAmount20(), cancellationRefundBO.getRefundAmount());
				newAmount20 = cancellationRefundBO.getRefundAmount();
			} else {
				// 使用的20%部分小于需要退的，需要退一部分到80%部分
				newAmount20 = baoDiTransUserBO.getAmount20();
				double needSub80 = MathUtil.sub(cancellationRefundBO.getRefundAmount(), baoDiTransUserBO.getAmount20());
				if (MathUtil.compareTo(baoDiTransUserBO.getAmount80(), needSub80) > 0) {
					// 保底流水中，使用80%大于剩余需要退的金额，直接退还
					newAmount80 = needSub80;
				} else {
					newAmount80 = baoDiTransUserBO.getAmount80();
					// 80%不够支付，从中奖账户中扣
					newAmountWin = MathUtil.sub(cancellationRefundBO.getRefundAmount(), MathUtil.add(baoDiTransUserBO.getAmount80(), baoDiTransUserBO.getAmount20()));
				}
			}
			baoDiTransUserBO.setAmount20(newAmount20);
			baoDiTransUserBO.setAmount80(newAmount80);
			baoDiTransUserBO.setAmountWin(newAmountWin);
			baoDiTransUserBO.setCashAmount(cancellationRefundBO.getRefundAmount());
			refundByTransUser(baoDiTransUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.GUARANTEE);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 退保底金额
	* @auth: xiongJinGang
	* @param transUserBO
	* @param cancellationRefundBO
	* @param orderInfo
	* @throws Exception
	* @time: 2018年5月4日 上午11:35:56
	* @return: void 
	*/
	public void refundByTransUser(TransUserBO transUserBO, CancellationRefundBO cancellationRefundBO, PayOrderBaseInfoVO orderInfo, OrderGroupTransTypeEnum orderGroupTransTypeEnum) throws Exception {
		// 三种情况是同一种退款方式，全额退
		Double useAmount20 = transUserBO.getAmount20();// 使用20%部分金额
		Double useAmount80 = transUserBO.getAmount80();// 使用80%部分金额
		Double useAmountWin = transUserBO.getAmountWin();// 使用中奖部分金额
		Integer userId = transUserBO.getUserId();
		String orderRemark = "合买订单";// 订单类型；1-代购订单；2-追号计划；3-合买

		logger.info(orderRemark + "【" + transUserBO.getOrderCode() + "】退总金额" + transUserBO.getCashAmount() + "，其中20%：" + useAmount20 + "，80%：" + useAmount80 + "，中奖：" + useAmountWin + "至账户钱包开始");
		// 代购订单。按订单金额组成部分进行退款，如使用了彩金红包，则退回一个使用金额等值的新的彩金红包，已用优惠劵重新生成一张等额且有效期为7天的新优惠劵
		ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(userId, useAmount80, useAmount20, useAmountWin, 0d, MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			logger.error(orderRemark + "【" + transUserBO.getOrderCode() + "】撤单，按每个子账户的使用金额返回失败：" + resultBO.getMessage());
			throw new RuntimeException("更新子账户钱包金额失败");
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		String remark = "保底退款";
		// 如果是认购合买，需要更新合买详情的状态为已退
		if (orderGroupTransTypeEnum.equals(OrderGroupTransTypeEnum.BUY_TOGETHER)) {
			orderGroupContentService.updateOrderGroupContentStatus(transUserBO.getOrderCode(), OrderGroupConstants.OrderGroupContentStatusEnum.YES.getKey());
			remark = "认购退款";
		}

		// 更新充值记录中的提款状态，如果是追号的退款，根据提款状态判断是否需要将20%的金额转移到80%中
		resultBO = updateRechargeTakenStatus(transUserBO.getOrderCode(), useAmount20, transUserBO.getTradeCode());
		// 返回成功，需要将退到20%的金额退到80%中
		String data1 = (String) resultBO.getData();
		if (Boolean.parseBoolean(data1) && MathUtil.compareTo(useAmount20, 0d) > 0) {
			useAmount80 = MathUtil.add(useAmount80, useAmount20);
			useAmount20 = 0d;
		}
		PayChildWalletVO payChildWallet = new PayChildWalletVO(userId, 0d, useAmount20, useAmount80, useAmountWin, 0d, userWalletPO.getTotalCashBalance(), userWalletPO.getEffRedBalance());
		payChildWallet.setTradeAmount(transUserBO.getCashAmount());
		payChildWallet.setOrderCode(transUserBO.getOrderCode());
		// 添加交易流水
		addTransRecord(payChildWallet, orderInfo, remark);
	}

	public void refundOrderGroup(List<TransUserBO> groupTransUserList, CancellationRefundBO cancellationRefundBO, PayOrderBaseInfoVO orderInfo) throws Exception {
		for (TransUserBO transUserBO : groupTransUserList) {
			refundByTransUser(transUserBO, cancellationRefundBO, orderInfo, OrderGroupConstants.OrderGroupTransTypeEnum.BUY_TOGETHER);
		}

	}

	/**  
	* 方法说明: 更新订单为退款状态
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @throws Exception
	* @time: 2018年5月4日 上午11:24:32
	* @return: void 
	*/
	private void updateOrderStatus(CancellationRefundBO cancellationRefundBO) throws Exception {
		try {
			PayOrderUpdatePO payOrderUpdatePO = new PayOrderUpdatePO();
			payOrderUpdatePO.setOrderCode(cancellationRefundBO.getOrderCode());
			payOrderUpdatePO.setOrderStatus(CancellationConstants.OrderStatusEnum.CANCELLATIONOK.getKey());
			payOrderUpdateMapper.updateOrderPayStatus(payOrderUpdatePO);
		} catch (Exception e) {
			logger.error("更新订单【" + cancellationRefundBO.getOrderCode() + "】状态失败", e);
			throw new Exception("更新订单状态失败");
		}
	}

	/**  
	* 方法说明: 添加撤单交易记录
	* @auth: xiongJinGang
	* @param payChildWallet
	* @param orderInfo
	* @throws Exception
	* @time: 2018年5月4日 上午11:08:12
	* @return: void 
	*/
	private void addTransRecord(PayChildWalletVO payChildWallet, PayOrderBaseInfoVO orderInfo, String remark) throws Exception {
		Short transType = PayConstants.TransTypeEnum.REFUND.getKey();// 退款
		payChildWallet.setOrderCode(payChildWallet.getOrderCode());
		payChildWallet.setOperateRemark(Constants.RED_REMARK_CANCEL_SEND);
		payChildWallet.setOrderInfo(remark);
		payChildWallet.setTransType(transType);
		payChildWallet.setChannelId(orderInfo.getChannelId());
		// 生成后端的交易流水
		TransUserPO transUserPO = transUserService.addTransUserRecord(payChildWallet, null);
		// 生成给用户查看的交易流水
		transUserLogService.addTransLogRecord(transUserPO);
	}

	/**  
	* 方法说明: 根据充值编号获取充值记录
	* @auth: xiongJinGang
	* @param transRechargeCode
	* @time: 2018年5月4日 上午11:22:17
	* @return: TransRechargeBO 
	*/
	private TransRechargeBO findTransRecharge(String transRechargeCode) {
		return transRechargeMapper.getRechargeByCode(transRechargeCode);
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
	private ResultBO<?> updateRechargeTakenStatus(String orderCode, Double useAmount20, String transRechargeCode) throws Exception {
		TransRechargeBO transRechargeBO = findTransRecharge(transRechargeCode);
		// 是即买即付的充值记录并且需要退还20%账户大于0，才允许修改充值记录的状态
		if (!ObjectUtil.isBlank(transRechargeBO) && MathUtil.compareTo(useAmount20, 0d) > 0) {
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
			logger.info("单个订单充值记录【" + transRechargeBO.getTransRechargeCode() + "】当前的提款状态【" + transRechargeBO.getTakenStatus() + "】");
			// 单个订单充值记录的提款状态如果是已提，全部退到80%账户
			if (transRechargeBO.getTakenStatus().equals(PayConstants.RechargeTakenStatusEnum.FINISHED.getKey())) {
				return ResultBO.ok("true");
			}
		}
		return ResultBO.ok("false");
	}

	/**  
	* 方法说明: 查询保底记录
	* @auth: xiongJinGang
	* @param orderGroupBO
	* @param orderCode
	* @throws Exception
	* @time: 2018年5月30日 下午4:25:40
	* @return: TransUserBO 
	*/
	@SuppressWarnings("unchecked")
	private TransUserBO queryBaoDiTransUser(OrderGroupBO orderGroupBO) throws Exception {
		TransUserBO baoDiTransUserBO = null;
		// 保底金额大于0，并且总金额和订单金额不一致，才查询保底记录
		if (MathUtil.compareTo(orderGroupBO.getGuaranteeAmount(), 0d) > 0) {
			/*TransUserVO refundTransUser = new TransUserVO(orderCode, PayConstants.TransTypeEnum.REFUND.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
			ResultBO<?> refundResult = transUserService.findUserTransByOrderCode(refundTransUser);
			if (refundResult.isOK()) {
				List<TransUserBO> tradeList = (List<TransUserBO>) refundResult.getData();
				if (!tradeList.isEmpty()) {
					logger.info("合买订单【" + orderCode + "】保底金额已退，不能重复退款");
					return ResultBO.err(MessageCodeConstants.ORDER_GROUP_REFUND_ERROR);
				}
			}*/

			// 获取合买的保底的交易记录
			TransUserVO transUserVO = new TransUserVO(orderGroupBO.getOrderCode(), PayConstants.TransTypeEnum.LOTTERY.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
			ResultBO<?> resultBO = transUserService.findUserTransByOrderCode(transUserVO);
			if (resultBO.isError()) {
				logger.info("获取用户【" + orderGroupBO.getUserId() + "】合买订单【" + orderGroupBO.getOrderCode() + "】保底金额交易数据为空，可能没有保底");
			} else {
				// 查询出保底记录
				List<TransUserBO> tradeList = (List<TransUserBO>) resultBO.getData();
				baoDiTransUserBO = tradeList.get(0);// 保底的交易流水

				// 如果获取的交易记录中的金额与合买中的保底金额不一致，返回错误
				/*if (MathUtil.compareTo(baoDiTransUserBO.getCashAmount(), orderGroupBO.getGuaranteeAmount()) != 0) {
					logger.info("合买订单【" + orderCode + "】交易记录【" + baoDiTransUserBO.getTransCode() + "】的金额" + baoDiTransUserBO.getCashAmount() + "与保底金额" + orderGroupBO.getGuaranteeAmount() + "不一致，不能退款");
					return ResultBO.err(MessageCodeConstants.ORDER_GROUP_MONEY_ERROR);
				}*/
			}
		}
		return baoDiTransUserBO;
	}
}
