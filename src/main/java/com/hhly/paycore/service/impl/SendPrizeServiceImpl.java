package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.common.SendPrizeUtil;
import com.hhly.paycore.dao.OrderFollowedInfoDaoMapper;
import com.hhly.paycore.dao.OrderIssueInfoDaoMapper;
import com.hhly.paycore.dao.OrderResetExceptionMapper;
import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.dao.TicketInfoMapper;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.OrderResetExceptionPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.OrderGroupContentService;
import com.hhly.paycore.service.OrderGroupService;
import com.hhly.paycore.service.SendPrizeService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderEnum.OrderStatus;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.mq.msg.MessageModel;
import com.hhly.skeleton.base.mq.msg.OperateNodeMsg;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.cms.ticketmgr.bo.TicketInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderFollowedInfoBO;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderIssueInfoBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.TransUserVO;
import com.hhly.skeleton.user.bo.UserWalletBO;

/**
 * @desc 派奖服务类
 * @author xiongJinGang
 * @date 2017年9月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("sendPrizeService")
public class SendPrizeServiceImpl implements SendPrizeService {

	private static final Logger logger = Logger.getLogger(SendPrizeServiceImpl.class);

	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Resource
	private TicketInfoMapper ticketInfoMapper;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private OrderResetExceptionMapper orderResetExceptionMapper;
	@Resource
	private OrderIssueInfoDaoMapper orderIssueInfoDaoMapper;
	@Resource
	private OrderFollowedInfoDaoMapper followedInfoDaoMapper;
	@Resource
	private OrderGroupContentService orderGroupContentService;// 合买交易记录详情
	@Resource
	private OrderGroupService orderGroupService;// 合买订单

	/****************************************派奖开始********************************************************/

	@SuppressWarnings("unchecked")
	@Override
	public ResultBO<?> updateSendPrize(String orderCode) throws Exception {
		logger.info("订单【" + orderCode + "】派奖开始");
		ResultBO<?> result = checkOrderAndTicket(orderCode);
		if (result.isError()) {
			return result;
		}
		OrderInfoBO orderInfo = (OrderInfoBO) result.getData();

		// *********************************需要验证订单是否已经退过款******************
		TransUserVO refundTransUser = new TransUserVO(orderInfo.getUserId(), orderCode, PayConstants.TransTypeEnum.RETURN_AWARD.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		// 5、根据订单号，查询交易记录
		ResultBO<?> resultBO = transUserService.findUserTransByOrderCode(refundTransUser);
		if (resultBO.isOK()) {
			List<TransUserBO> list = (List<TransUserBO>) resultBO.getData();
			TransUserBO transUserBO = list.get(0);
			if (transUserBO.getAwardFlag().intValue() == 0) {
				logger.info("订单【" + orderCode + "】已派奖，不能重复派奖");
				return ResultBO.ok();
			}
		}
		if (resultBO.isError() && !resultBO.getErrorCode().equals(MessageCodeConstants.DATA_NOT_FOUND_SYS)) {
			logger.info("订单【" + orderCode + "】查询交易流水异常：" + resultBO.getMessage());
			return resultBO;
		}

		// 购买类型不为空，并且是合买类型
		if (!ObjectUtil.isBlank(orderInfo.getBuyType()) && orderInfo.getBuyType().intValue() == PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey().intValue()) {
			updateSendOrderGroupPrize(orderCode, orderInfo);
		} else {
			// 不是合买类型，新增用户交易流水记录
			result = updateUserwallet(orderInfo);
			if (result.isError()) {
				return result;
			}
			// 推单订单操作
			if (orderInfo.getOrderType() == 2) {
				followInfoHandle(orderCode, orderInfo);
			}
		}
		logger.info("给订单【" + orderCode + "】派奖结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 派发合买用户中奖金额
	* @auth: xiongJinGang
	* @param orderCode
	* @param orderInfo
	* @throws Exception
	* @throws RuntimeException
	* @time: 2018年5月22日 上午10:05:45
	* @return: void 
	*/
	public void updateSendOrderGroupPrize(String orderCode, OrderInfoBO orderInfo) throws Exception, RuntimeException {
		int userId = orderInfo.getUserId();
		String oldOrderCode = orderInfo.getOrderCode();
		// 合买订单记录
		OrderGroupBO orderGroupBO = orderGroupService.findOrderGroupByOrderCode(oldOrderCode);
		// 根据订单号获取合买交易详情
		List<OrderGroupContentBO> orderGroupContentList = orderGroupContentService.findOrderGroupContentByOrderCode(orderCode);
		// 合买中的网站加奖和官方加奖，先加给发起合买的人
		Double addBonus = ObjectUtil.isBlank(orderInfo.getAddedBonus()) ? 0d : orderInfo.getAddedBonus();// 官方加奖金额
		Double sitBonus = ObjectUtil.isBlank(orderInfo.getWebsiteBonus()) ? 0d : orderInfo.getWebsiteBonus();// 本站加奖

		// 有官方加奖或者网站加奖，全部加到发起合买用户的账户
		if (MathUtil.compareTo(addBonus, 0d) > 0 || MathUtil.compareTo(sitBonus, 0d) > 0) {
			// 总的中奖金额
			ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(userId, 0d, 0d, addBonus, sitBonus, PayConstants.MoneyFlowEnum.IN.getKey());
			if (resultBO.isError()) {
				throw new RuntimeException(resultBO.getMessage());
			}
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			// 添加官方加奖，网站加奖派奖流水
			addOrderGroupSendPrizeTransUser(orderInfo, addBonus, sitBonus, userWalletPO);
		}

		// 合买详情，给每一个参与合买的用户发奖金
		for (OrderGroupContentBO orderGroupContentBO : orderGroupContentList) {
			// 合买类型，更新合买用户交易流水记录
			// updateOrderGroupPrize(orderInfo, orderGroupContentBO);
			addOrderGroupContentSendPrizeTransUser(orderInfo, orderGroupContentBO);
		}
		logger.info("订单【" + orderCode + "】合买详情：" + orderGroupContentList.size() + "条");
		// 合买订单中的抽成金额大于0
		Double singleCommissionAmount = ObjectUtil.isBlank(orderGroupBO.getCommissionAmount()) ? 0d : orderGroupBO.getCommissionAmount();
		if (MathUtil.compareTo(singleCommissionAmount, 0d) > 0) {
			logger.info("订单【" + orderCode + "】合买的提成总金额：" + singleCommissionAmount);
			// 往发起人钱包中加抽成金额
			ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(userId, singleCommissionAmount, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
			if (resultBO.isError()) {
				throw new RuntimeException(resultBO.getMessage());
			}
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			// 生成用户中奖交易流水
			orderInfo.setUserId(userId);
			orderInfo.setOrderCode(oldOrderCode);
			TransUserPO transUserPO = transUserService.addOrderGroupWinTransUser(orderInfo, singleCommissionAmount, userWalletPO);
			transUserLogService.addTransLogRecord(transUserPO);
			logger.info("更新用户合买订单抽成金额【userId：" + orderInfo.getUserId() + "，orderCode：" + oldOrderCode + "，commissionAmount：" + singleCommissionAmount + "】结束");

		}
	}

	/**  
	* 方法说明: 添加合买本站加奖、官方加奖交易流水
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param aftBonus
	* @param addBonus
	* @param sitBonus
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年7月20日 上午9:46:27
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> addOrderGroupContentSendPrizeTransUser(OrderInfoBO orderInfo, OrderGroupContentBO orderGroupContentBO) throws Exception {
		Double aftBonus = ObjectUtil.isBlank(orderGroupContentBO.getSendBonus()) ? 0d : orderGroupContentBO.getSendBonus();// 税后奖金
		Double addBonus = ObjectUtil.isBlank(orderGroupContentBO.getAddedBonus()) ? 0d : orderGroupContentBO.getAddedBonus();// 官方加奖
		Double sitBonus = ObjectUtil.isBlank(orderGroupContentBO.getSiteAddedBonus()) ? 0d : orderGroupContentBO.getSiteAddedBonus();// 本站加奖

		Integer userId = orderGroupContentBO.getUserId();
		logger.info("更新用户【" + userId + "】订单【" + orderGroupContentBO.getBuyCode() + "】中奖信息【税后奖金：" + aftBonus + "，官方加奖：" + addBonus + "本站加奖：" + sitBonus + "】开始");
		// 总的中奖金额
		Double totalBonus = MathUtil.add(aftBonus, addBonus);// 中奖金额+官方加奖金额 = 实际到账的金额
		ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(userId, 0d, 0d, totalBonus, sitBonus, PayConstants.MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			logger.info("更新用户【" + userId + "】钱包失败");
			throw new RuntimeException(resultBO.getMessage());
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		Double totalCashBalance = userWalletPO.getTotalCashBalance();// 总现金金额

		userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, addBonus));
		userWalletPO.setEffRedBalance(MathUtil.sub(userWalletPO.getEffRedBalance(), sitBonus));
		// 生成用户中奖交易流水
		orderInfo.setUserId(orderGroupContentBO.getUserId());
		orderInfo.setOrderCode(orderGroupContentBO.getBuyCode());
		TransUserPO transUserPO = transUserService.addWinTransUser(orderInfo, 0d, null, aftBonus, userWalletPO);
		transUserLogService.addTransLogRecord(transUserPO);

		// 生成官方加奖交易流水
		if (MathUtil.compareTo(addBonus, 0d) > 0) {
			TransUserPO transUser = transUserService.addOfficialBonusTransUser(orderInfo, addBonus, userWalletPO.getEffRedBalance(), totalCashBalance);
			transUserLogService.addTransLogRecord(transUser);
		}

		// 生成本站加奖交易流水
		if (MathUtil.compareTo(sitBonus, 0d) > 0) {
			// 获取本站加奖得到的彩金红包，判断
			if (!ObjectUtil.isBlank(orderGroupContentBO.getRedCodeGet())) {
				String[] redCodes = orderGroupContentBO.getRedCodeGet().split(",");
				List<OperateCouponBO> couponList = new ArrayList<OperateCouponBO>();
				Double totalSendRedValue = 0d;// 总的送红包金额
				for (String redCode : redCodes) {
					OperateCouponBO coupon = operateCouponService.findByRedCode(redCode);// 本站加奖获取到的红包
					totalSendRedValue = MathUtil.add(totalSendRedValue, coupon.getRedValue());
					couponList.add(coupon);
				}

				if (MathUtil.compareTo(totalSendRedValue, sitBonus) != 0) {
					logger.error("本站加奖金额错误，红包【" + orderGroupContentBO.getRedCodeGet() + "】金额：" + totalSendRedValue + "，订单中本站加奖金额：" + sitBonus + "不相等");
					throw new RuntimeException();
				}

				// 没加红包金额时，账户中的红包金额
				Double oldRedValue = userWalletPO.getEffRedBalance();
				for (OperateCouponBO operateCouponBO : couponList) {
					// 红包状态是待激活，更新成可使用
					if (PayConstants.RedStatusEnum.WAITTING_ACTIVATION.getKey().equals(operateCouponBO.getRedStatus())) {
						operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 改成可使用
						int num = operateCouponService.updateOperateCoupon(operateCouponBO);
						logger.info("将红包【" + operateCouponBO.getRedCode() + "】待激活状态改成可使用返回：" + (num > 0 ? "成功" : "失败"));
					} else {
						logger.info("红包【" + operateCouponBO.getRedCode() + "】不是待激活状态，不修改");
					}
					// 在原有的红包金额基础上，累加红包金额
					oldRedValue = MathUtil.add(oldRedValue, operateCouponBO.getRedBalance());
					// 添加红包交易流水
					TransUserPO transUser = transUserService.addWebSiteBonusTransUser(orderInfo, oldRedValue, totalCashBalance, operateCouponBO);
					transUserLogService.addTransLogRecord(transUser);
				}
			}
		}
		logger.info("更新用户订单中奖信息【userId：" + orderGroupContentBO.getUserId() + "，orderCode：" + orderGroupContentBO.getOrderCode() + "，aftBonus：" + aftBonus + "，addedBonus：" + addBonus + "，websiteBonus：" + sitBonus + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 检查订单信息以及票信息
	* @auth: xiongJinGang
	* @param orderCode
	* @throws Exception
	* @time: 2018年5月19日 下午5:48:36
	* @return: OrderInfoBO 
	*/
	public ResultBO<?> checkOrderAndTicket(String orderCode) throws Exception {
		// 1.验证基本参数
		if (ObjectUtil.isBlank(orderCode)) {
			return ResultBO.err(MessageCodeConstants.ORDER_CODE_IS_NULL_FIELD);
		}
		// 2.查询订单和票信息
		OrderInfoBO orderInfo = payOrderUpdateMapper.getOrderInfo(orderCode);
		if (ObjectUtil.isBlank(orderInfo)) {
			logger.info("派奖未获取到订单号【" + orderCode + "】详情");
			return ResultBO.err(MessageCodeConstants.TRANS_ORDER_CODE_IS_ERROR_SERVICE);
		}
		if (!CancellationConstants.OrderWinningStatusEnum.WINNING.getKey().equals(orderInfo.getWinningStatus())) {
			logger.info("订单号【" + orderCode + "】不是已中奖状态");
			return ResultBO.err(MessageCodeConstants.ORDER_STATUS_ERROR_SERVICE);
		}

		TicketInfoBO ticketInfoBO = new TicketInfoBO();
		ticketInfoBO.setOrderCode(orderCode);
		List<TicketInfoBO> tickets = ticketInfoMapper.getTickets(ticketInfoBO);
		if (ObjectUtil.isBlank(tickets)) {
			logger.info("派奖未获取到订单号【" + orderCode + "】的票信息");
			return ResultBO.err(MessageCodeConstants.TICKET_NOT_EXIST);
		}
		// 1.未推单 2，已推单 走原来的流程 3.核对金额
		ResultBO<?> result = SendPrizeUtil.validateOrder(orderInfo, tickets);
		if (result.isError()) {
			return result;
		}
		return ResultBO.ok(orderInfo);
	}

	/** 
	 * @Description: 针对跟单的订单做处理
	 * @param orderCode
	 * @param orderInfo
	 * @throws Exception
	 * @throws RuntimeException
	 * @author wuLong
	 * @date 2017年10月12日 上午11:31:11
	 */
	public ResultBO<?> followInfoHandle(String orderCode, OrderInfoBO orderInfo) throws Exception, RuntimeException {
		// 根据跟单订单编号查找到推单信息
		OrderIssueInfoBO orderIssueInfoBO = orderIssueInfoDaoMapper.getOrderIssueInfo(orderCode);
		if (!ObjectUtil.isBlank(orderIssueInfoBO)) {
			List<String> followOrderCodes = followedInfoDaoMapper.getFollowOrderCode(orderIssueInfoBO.getId());
			if (!ObjectUtil.isBlank(followOrderCodes)) {
				List<OrderInfoBO> followOrderInfoBOs = payOrderUpdateMapper.getOrderList(followOrderCodes);
				if (!ObjectUtil.isBlank(followOrderInfoBOs)) {
					float commissionrate = orderIssueInfoBO.getCommissionRate();
					for (OrderInfoBO orderInfoBO : followOrderInfoBOs) {
						OrderStatus orderStatus = OrderStatus.parseOrderStatus(orderInfoBO.getOrderStatus());
						if (orderStatus != OrderStatus.TICKETED) {
							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + ",订单状态:" + orderStatus.getDesc() + ",不能派奖");
							continue;
						}
						logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "派奖开始");
						updateSendPrize(orderInfoBO.getOrderCode());
						// 根据推单的佣金提成 根据跟单订单税后金额计算出佣金
						double ylamount = orderInfoBO.getAftBonus() - orderInfoBO.getOrderAmount();// 跟单盈利的钱
						double commissionamount = MathUtil.mul(ylamount, commissionrate);
						double bjamount = MathUtil.mul(orderInfoBO.getOrderAmount(), 0.1);
						if (ylamount < bjamount) {
							commissionamount = 0;
							logger.info("跟单方案:" + orderInfoBO.getOrderCode() + "盈利小于本金的10%,本金：" + orderInfoBO.getOrderAmount() + ",税后奖金：" + orderInfoBO.getAftBonus() + ",盈利：" + ylamount);
						}
						if (commissionamount > 0) {
							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "佣金的跟单方案扣款开始：" + commissionamount);
							// 跟单的佣金支出
							followedInfoDaoMapper.update(commissionamount, orderInfoBO.getOrderCode());
							// 跟单中奖账户支出
							ResultBO<?> zcResultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), commissionamount, PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (zcResultBO.isError()) {
								throw new RuntimeException(zcResultBO.getMessage());
							}
							UserWalletPO zcUserWalletPO = (UserWalletPO) zcResultBO.getData();
							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "佣金的跟单方案扣款交易流水开始");
							// 写入跟单支出流水
							// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
							Short transType = PayConstants.TransTypeEnum.DEDUCT.getKey();
							String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							String message = Constants.FOLLEW_COMMISSIONAMOUNT_INFO;
							TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, message);
							// 获取交易号
							transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							transUserPO.setCashAmount(commissionamount);
							transUserPO.setOrderCode(orderInfoBO.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							transUserPO.setTransAmount(commissionamount);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							transUserPO.setChannelId(orderInfoBO.getChannelId());
							transUserPO.setAmount20(0d);
							transUserPO.setAmount80(0d);
							transUserPO.setAmountWin(commissionamount);
							// transUserPO.setTradeCode();
							transUserPO.setRedTransAmount(0d);
							transUserPO.setTotalCashBalance(zcUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							transUserPO.setServiceCharge(0d);
							transUserPO.setRemark(message);
							transUserService.addTransRecord(transUserPO);
							transUserLogService.addTransLogRecord(transUserPO);

							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "佣金返奖给推单开始：" + commissionamount);
							// 推单的佣金收入
							orderIssueInfoDaoMapper.update(orderIssueInfoBO.getId(), commissionamount);
							// 推单账户佣金收入
							ResultBO<?> srResultBO = userWalletService.updateUserWalletBySplit(orderInfo.getUserId(), commissionamount, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (srResultBO.isError()) {
								throw new RuntimeException(srResultBO.getMessage());
							}
							UserWalletPO srUserWalletPO = (UserWalletPO) srResultBO.getData();

							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "佣金返奖给推单交易流水开始");
							// 推单流水写入
							transType = PayConstants.TransTypeEnum.RETURN_AWARD.getKey();
							transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							TransUserPO srTransUserPO = new TransUserPO(orderInfo.getUserId(), transCode, transType, message);
							// 获取交易号
							srTransUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							srTransUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							srTransUserPO.setCashAmount(commissionamount);
							srTransUserPO.setOrderCode(orderInfo.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							srTransUserPO.setTransAmount(commissionamount);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							srTransUserPO.setChannelId(orderInfo.getChannelId());
							srTransUserPO.setAmount20(0d);
							srTransUserPO.setAmount80(0d);
							srTransUserPO.setAmountWin(commissionamount);
							// transUserPO.setTradeCode();
							srTransUserPO.setRedTransAmount(0d);
							srTransUserPO.setTotalCashBalance(srUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							srTransUserPO.setServiceCharge(0d);
							srTransUserPO.setRemark(message);
							transUserService.addTransRecord(srTransUserPO);
							transUserLogService.addTransLogRecord(srTransUserPO);

							logger.info("抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的派奖扣款提佣金写入交易流水结束");
						}
					}
				} else {
					logger.info("抄单的推单方案：" + orderCode + ",对应的跟单订单编号：" + JSONObject.toJSONString(followOrderInfoBOs) + ",数据库中不存在");
				}
			} else {
				logger.info("抄单的推单方案：" + orderCode + ",没有人跟单");
			}
		} else {
			logger.info("抄单的推单方案：" + orderCode + "不存在");
		}
		return null;
	}

	/**  
	* 方法说明: 派奖：无加奖派奖，全部入中奖账户；有加奖派奖，中奖金额入中奖账户，加奖金额入红包账户（使用了加奖红包，中奖的金额）
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @throws Exception
	* @time: 2017年7月13日 下午4:11:41
	* @return: void 
	*/
	public ResultBO<?> updateUserwallet(OrderInfoBO orderInfoBO) throws Exception {
		// 正常派奖
		Double aftBonus = ObjectUtil.isBlank(orderInfoBO.getAftBonus()) ? 0d : orderInfoBO.getAftBonus();// 中奖金额
		Double addBonus = ObjectUtil.isBlank(orderInfoBO.getAddedBonus()) ? 0d : orderInfoBO.getAddedBonus();// 官方加奖金额
		Double sitBonus = ObjectUtil.isBlank(orderInfoBO.getWebsiteBonus()) ? 0d : orderInfoBO.getWebsiteBonus();// 本站加奖

		logger.info("更新用户订单中奖信息【userId：" + orderInfoBO.getUserId() + "，orderCode：" + orderInfoBO.getOrderCode() + "，aftBonus：" + aftBonus + "，官方加奖：" + addBonus + "本站加奖：" + sitBonus + "】开始");
		// 总的中奖金额
		Double totalBonus = MathUtil.add(aftBonus, addBonus);// 中奖金额+官方加奖金额 = 实际到账的金额
		ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(orderInfoBO.getUserId(), 0d, 0d, totalBonus, sitBonus, PayConstants.MoneyFlowEnum.IN.getKey());
		if (resultBO.isError()) {
			throw new RuntimeException(resultBO.getMessage());
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		// 判断是否使用了加奖红包，如果使用了，需要生成彩金红包。先屏蔽，现在还没有做加奖红包功能2018-01-10
		// OperateCouponBO operateCouponBO = addRedColor(orderInfoBO, aftBonus);

		// 添加派奖流水
		resultBO = addSendPrizeTransUser(orderInfoBO, aftBonus, addBonus, sitBonus, userWalletPO);
		if (resultBO.isError()) {
			return resultBO;
		}

		logger.info("更新用户订单中奖信息【userId：" + orderInfoBO.getUserId() + "，orderCode：" + orderInfoBO.getOrderCode() + "，aftBonus：" + aftBonus + "，addedBonus：" + addBonus + "，websiteBonus：" + sitBonus + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 更新用户合买订单中奖信息
	* @auth: xiongJinGang
	* @param orderInfo
	* @param orderGroupContentBO
	* @throws Exception
	* @time: 2018年5月22日 上午9:55:45
	* @return: void 
	*/

	/*public void updateOrderGroupPrize(OrderInfoBO orderInfo, OrderGroupContentBO orderGroupContentBO) throws Exception {
		String oldOrderCode = orderInfo.getOrderCode();
		// 正常派奖
		Double aftBonus = ObjectUtil.isBlank(orderGroupContentBO.getSendBonus()) ? 0d : orderGroupContentBO.getSendBonus();// 税后奖金
		Double addedBonus = ObjectUtil.isBlank(orderGroupContentBO.getAddedBonus()) ? 0d : orderGroupContentBO.getAddedBonus();//官方加奖
		Double siteAddedBonus = ObjectUtil.isBlank(orderGroupContentBO.getSiteAddedBonus()) ? 0d : orderGroupContentBO.getSiteAddedBonus();//本站加奖
		if (MathUtil.compareTo(aftBonus, 0d) > 0) {
			logger.info("更新用户合买订单中奖信息【userId：" + orderGroupContentBO.getUserId() + "，orderCode：" + oldOrderCode + "，buyCode：" + orderGroupContentBO.getBuyCode() + "，aftBonus：" + aftBonus + "】开始");
			// 合买用户中奖金额
			ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(orderGroupContentBO.getUserId(), aftBonus, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
			if (resultBO.isError()) {
				throw new RuntimeException(resultBO.getMessage());
			}
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		
			// 生成用户中奖交易流水
			orderInfo.setUserId(orderGroupContentBO.getUserId());
			orderInfo.setOrderCode(orderGroupContentBO.getBuyCode());
			TransUserPO transUserPO = transUserService.addWinTransUser(orderInfo, 0d, null, aftBonus, userWalletPO);
			transUserLogService.addTransLogRecord(transUserPO);
			logger.info("更新用户合买订单中奖信息【userId：" + orderInfo.getUserId() + "，orderCode：" + oldOrderCode + "，buyCode：" + orderGroupContentBO.getBuyCode() + "，aftBonus：" + aftBonus + "】结束");
		}
	}*/

	/**  
	* 方法说明: 生成加奖红包（到2018-01-10，还没有加奖红包业务，先屏蔽）
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param userId
	* @param aftBonus
	* @throws NumberFormatException
	* @throws Exception
	* @time: 2018年1月10日 下午12:10:40
	* @return: void 
	*/
	@Deprecated
	public OperateCouponBO addRedColor(OrderInfoBO orderInfoBO, Double aftBonus) throws NumberFormatException, Exception {
		// 使用了红包，并且是加奖红包，中奖金额入中奖账户，加奖金额入红包账户（使用了加奖红包，中奖的金额）
		if (!ObjectUtil.isBlank(orderInfoBO.getRedCodeUsed())) {
			ResultBO<?> resultBO = operateCouponService.findCouponByRedCode(orderInfoBO.getRedCodeUsed());
			if (resultBO.isError()) {
				logger.info("给用户【" + orderInfoBO.getUserId() + "】派奖时，获取订单【" + orderInfoBO.getOrderCode() + "】已使用红包【" + orderInfoBO.getRedCodeUsed() + "】失败");
			} else {
				OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();
				// 判断红包是否为加奖红包
				if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.BONUS_RED.getKey())) {
					Integer minSpendAmount = operateCouponBO.getMinSpendAmount();// 最低中奖金额
					logger.info("订单【" + orderInfoBO.getOrderCode() + "】使用了加奖红包【" + operateCouponBO.getRedCode() + "】，中奖金额【" + aftBonus + "】");
					Double minSpendAmountDou = Double.parseDouble(String.valueOf(minSpendAmount));
					Double redValue = operateCouponBO.getRedValue();// 返多少金额
					// 中奖金额大于加奖红包中的最低金额，返一个红包，添加彩金红包
					if (MathUtil.compareTo(aftBonus, minSpendAmountDou) >= 0) {
						logger.info("订单【" + orderInfoBO.getOrderCode() + "】中奖金额【" + aftBonus + "】大于等于加奖红包使用条件金额【" + minSpendAmountDou + "】，生成一个彩金红包");
						resultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), redValue, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
						// 生成一个彩金红包
						operateCouponBO.setUserId(orderInfoBO.getUserId());
						operateCouponBO.setLimitPlatform("");
						operateCouponBO.setChannelId("");
						OperateCouponPO operateCouponPO = operateCouponService.addRedColor(operateCouponBO, redValue);
						String redCode = operateCouponPO.getRedCode();
						// 生成红包交易流水
						transRedService.addTransRed(redCode, redValue, orderInfoBO.getUserId(), null, PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
						// 发送消息
						sendMessage(orderInfoBO.getUserId(), redCode);
						return operateCouponBO;
					}
				}
			}
		}
		return null;
	}

	/**  
	* 方法说明:  添加派奖流水（包括正常派奖流水、官方加奖流水、本站加奖流水）
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param aftBonus 税后金额
	* @param addBonus 官方加奖金额
	* @param sitBonus 本站加奖金额
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年1月10日 上午11:55:55
	* @return: void 
	*/
	public ResultBO<?> addSendPrizeTransUser(OrderInfoBO orderInfoBO, Double aftBonus, Double addBonus, Double sitBonus, UserWalletPO userWalletPO) throws Exception {
		Double totalCashBalance = userWalletPO.getTotalCashBalance();// 总现金金额

		userWalletPO.setTotalCashBalance(MathUtil.sub(totalCashBalance, addBonus));
		userWalletPO.setEffRedBalance(MathUtil.sub(userWalletPO.getEffRedBalance(), sitBonus));
		// 生成用户中奖交易流水
		TransUserPO transUserPO = transUserService.addWinTransUser(orderInfoBO, 0d, null, aftBonus, userWalletPO);
		transUserLogService.addTransLogRecord(transUserPO);

		// 生成官方加奖交易流水
		if (MathUtil.compareTo(addBonus, 0d) > 0) {
			TransUserPO transUser = transUserService.addOfficialBonusTransUser(orderInfoBO, addBonus, userWalletPO.getEffRedBalance(), totalCashBalance);
			transUserLogService.addTransLogRecord(transUser);
		}

		// 生成本站加奖交易流水
		if (MathUtil.compareTo(sitBonus, 0d) > 0) {
			// 获取本站加奖得到的彩金红包，判断
			if (!ObjectUtil.isBlank(orderInfoBO.getRedCodeGet())) {
				String[] redCodes = orderInfoBO.getRedCodeGet().split(",");
				List<OperateCouponBO> couponList = new ArrayList<OperateCouponBO>();
				Double totalSendRedValue = 0d;// 总的送红包金额
				for (String redCode : redCodes) {
					OperateCouponBO coupon = operateCouponService.findByRedCode(redCode);// 本站加奖获取到的红包
					totalSendRedValue = MathUtil.add(totalSendRedValue, coupon.getRedValue());
					couponList.add(coupon);
				}

				if (MathUtil.compareTo(totalSendRedValue, sitBonus) != 0) {
					logger.error("本站加奖金额错误，红包【" + orderInfoBO.getRedCodeGet() + "】金额：" + totalSendRedValue + "，订单中本站加奖金额：" + sitBonus + "不相等");
					throw new RuntimeException();
				}

				// 没加红包金额时，账户中的红包金额
				Double oldRedValue = userWalletPO.getEffRedBalance();
				for (OperateCouponBO operateCouponBO : couponList) {
					// 红包状态是待激活，更新成可使用
					if (PayConstants.RedStatusEnum.WAITTING_ACTIVATION.getKey().equals(operateCouponBO.getRedStatus())) {
						operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 改成可使用
						int num = operateCouponService.updateOperateCoupon(operateCouponBO);
						logger.info("将红包【" + operateCouponBO.getRedCode() + "】待激活状态改成可使用返回：" + (num > 0 ? "成功" : "失败"));
					} else {
						logger.info("红包【" + operateCouponBO.getRedCode() + "】不是待激活状态，不修改");
					}
					// 在原有的红包金额基础上，累加红包金额
					oldRedValue = MathUtil.add(oldRedValue, operateCouponBO.getRedBalance());
					// 添加红包交易流水
					TransUserPO transUser = transUserService.addWebSiteBonusTransUser(orderInfoBO, oldRedValue, totalCashBalance, operateCouponBO);
					transUserLogService.addTransLogRecord(transUser);
				}
			}
		}
		logger.info("更新用户订单中奖信息【userId：" + orderInfoBO.getUserId() + "，orderCode：" + orderInfoBO.getOrderCode() + "，aftBonus：" + aftBonus + "，addedBonus：" + addBonus + "，websiteBonus：" + sitBonus + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 合买订单，本站加奖和官方加奖，都加到发起人的账户
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param addBonus
	* @param sitBonus
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年5月22日 上午10:17:26
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> addOrderGroupSendPrizeTransUser(OrderInfoBO orderInfoBO, Double addBonus, Double sitBonus, UserWalletPO userWalletPO) throws Exception {
		// 生成官方加奖交易流水
		if (MathUtil.compareTo(addBonus, 0d) > 0) {
			TransUserPO transUser = transUserService.addOfficialBonusTransUser(orderInfoBO, addBonus, userWalletPO.getEffRedBalance(), userWalletPO.getTotalCashBalance());
			transUserLogService.addTransLogRecord(transUser);
		}

		// 生成本站加奖交易流水
		if (MathUtil.compareTo(sitBonus, 0d) > 0) {
			// 获取本站加奖得到的彩金红包，判断
			if (!ObjectUtil.isBlank(orderInfoBO.getRedCodeGet())) {
				String[] redCodes = orderInfoBO.getRedCodeGet().split(",");
				List<OperateCouponBO> couponList = new ArrayList<OperateCouponBO>();
				Double totalSendRedValue = 0d;// 总的送红包金额
				for (String redCode : redCodes) {
					OperateCouponBO coupon = operateCouponService.findByRedCode(redCode);// 本站加奖获取到的红包
					totalSendRedValue = MathUtil.add(totalSendRedValue, coupon.getRedValue());
					couponList.add(coupon);
				}

				if (MathUtil.compareTo(totalSendRedValue, sitBonus) != 0) {
					logger.error("本站加奖金额错误，红包【" + orderInfoBO.getRedCodeGet() + "】金额：" + totalSendRedValue + "，订单中本站加奖金额：" + sitBonus + "不相等");
					throw new RuntimeException();
				}

				// 没加红包金额时，账户中的红包金额
				Double oldRedValue = userWalletPO.getEffRedBalance();
				for (OperateCouponBO operateCouponBO : couponList) {
					// 红包状态是待激活，更新成可使用
					if (PayConstants.RedStatusEnum.WAITTING_ACTIVATION.getKey().equals(operateCouponBO.getRedStatus())) {
						operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 改成可使用
						int num = operateCouponService.updateOperateCoupon(operateCouponBO);
						logger.info("将红包【" + operateCouponBO.getRedCode() + "】待激活状态改成可使用返回：" + (num > 0 ? "成功" : "失败"));
					} else {
						logger.info("红包【" + operateCouponBO.getRedCode() + "】不是待激活状态，不修改");
					}
					// 在原有的红包金额基础上，累加红包金额
					oldRedValue = MathUtil.add(oldRedValue, operateCouponBO.getRedBalance());
					// 添加红包交易流水
					TransUserPO transUser = transUserService.addWebSiteBonusTransUser(orderInfoBO, oldRedValue, userWalletPO.getTotalCashBalance(), operateCouponBO);
					transUserLogService.addTransLogRecord(transUser);
				}
			}
		}
		logger.info("更新用户订单中奖信息【userId：" + orderInfoBO.getUserId() + "，orderCode：" + orderInfoBO.getOrderCode() + "，addedBonus：" + addBonus + "，websiteBonus：" + sitBonus + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 发送消息
	* @auth: xiongJinGang
	* @param userId
	* @param redCode
	* @time: 2017年7月26日 下午5:12:54
	* @return: void 
	*/
	private void sendMessage(Integer userId, String redCode) {
		// 发送充值消息
		MessageModel messageModel = new MessageModel();
		messageModel.setKey(Constants.MSG_NODE_RESEND);
		messageModel.setMessageSource("lotto-pay");
		OperateNodeMsg operateNodeMsg = new OperateNodeMsg();
		operateNodeMsg.setNodeId(2);
		operateNodeMsg.setNodeData(userId + ";" + redCode);// 用户ID;充值交易号
		messageModel.setMessage(operateNodeMsg);
		messageProvider.sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, messageModel);
	}

	/****************************************派奖结束********************************************************/

	/****************************************重置派奖开始********************************************************/
	@Override
	public ResultBO<?> updateResetSendPrize(String orderCode) throws Exception {
		logger.info("订单【" + orderCode + "】重置派奖开始");
		// 1.验证基本参数
		if (ObjectUtil.isBlank(orderCode)) {
			return ResultBO.err(MessageCodeConstants.ORDER_CODE_IS_NULL_FIELD);
		}
		// 2.查询订单和票信息
		OrderInfoBO orderInfoBO = payOrderUpdateMapper.getOrderInfo(orderCode);
		if (ObjectUtil.isBlank(orderInfoBO)) {
			logger.info("重置派奖未获取到订单【" + orderCode + "】详情");
			return ResultBO.err(MessageCodeConstants.TRANS_ORDER_CODE_IS_ERROR_SERVICE);
		}
		// 1：未开奖；2：未中奖；3：已中奖；4：已派奖
		if (CancellationConstants.OrderWinningStatusEnum.NOTDRAWLOTTERY.getKey().equals(orderInfoBO.getWinningStatus()) || CancellationConstants.OrderWinningStatusEnum.WINNING.getKey().equals(orderInfoBO.getWinningStatus())) {
			logger.info("重置派奖订单【" + orderCode + "】状态【" + orderInfoBO.getWinningStatus() + "】不等于（2：未中奖，4：已派奖），无法重置派奖");
			return ResultBO.err(MessageCodeConstants.ORDER_WINNING_STATUS_ERROR_SERVICE);
		}
		Double aftBonus = ObjectUtil.isBlank(orderInfoBO.getAftBonus()) ? 0d : orderInfoBO.getAftBonus();// 税后奖金
		Double addBonus = ObjectUtil.isBlank(orderInfoBO.getAddedBonus()) ? 0d : orderInfoBO.getAddedBonus();// 官方加奖
		Double webSiteBonus = ObjectUtil.isBlank(orderInfoBO.getWebsiteBonus()) ? 0d : orderInfoBO.getWebsiteBonus();// 本站加奖
		logger.info("订单【" + orderCode + "】需要重置的资金：税后奖金=" + aftBonus + "；官方加奖=" + addBonus + "：本站加奖=" + webSiteBonus);

		// 三个中奖字段都为空，返回错误
		if (ObjectUtil.isBlank(aftBonus) && ObjectUtil.isBlank(addBonus) && ObjectUtil.isBlank(webSiteBonus)) {
			logger.info("订单【" + orderCode + "】重置派奖订单【" + orderCode + "】中奖金额【" + aftBonus + "】，官方加奖金额【" + addBonus + "】，本站加奖金额【" + webSiteBonus + "】错误，无法重置派奖");
			return ResultBO.err(MessageCodeConstants.WINNING_AMOUNT_ERROR);
		}

		/*************合买订单和代购订单，重置开奖不一样*****************/
		if (!ObjectUtil.isBlank(orderInfoBO.getBuyType()) && orderInfoBO.getBuyType().intValue() == PayConstants.BuyTypeEnum.JOINT_PURCHASE.getKey().intValue()) {
			Integer buyUserId = orderInfoBO.getUserId();// 发起合买人的用户ID
			// 先处理发起合买人的官方加奖和本站加奖
			UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(buyUserId);

			// 1、更新官方加奖和本站加奖金额（只有发起人有）
			updateResetAddPrize(orderCode, orderInfoBO, addBonus, webSiteBonus, userWalletBO.getTotalCashBalance());

			// 2、修改订单的中奖状态为 已中奖
			orderInfoBO.setWinningStatus(CancellationConstants.OrderWinningStatusEnum.WINNING.getKey());
			int num = payOrderUpdateMapper.updateOrderWinningStatus(orderInfoBO);
			logger.info("重置开奖，更新订单【" + orderCode + "】中奖状态到已中奖：" + (num > 0 ? "成功" : "失败"));

			// 根据订单号获取合买交易详情
			List<OrderGroupContentBO> orderGroupContentList = orderGroupContentService.findOrderGroupContentByOrderCode(orderCode);

			// 抽成金额
			for (OrderGroupContentBO orderGroupContentBO : orderGroupContentList) {
				orderInfoBO.setOrderCode(orderGroupContentBO.getBuyCode());
				UserWalletBO userWalletInfo = userWalletService.findUserWalletByUserId(orderGroupContentBO.getUserId());
				updateUserWallet(orderGroupContentBO.getBuyCode(), orderInfoBO, orderGroupContentBO.getSendBonus(), userWalletInfo.getTotalCashBalance(), orderGroupContentBO.getUserId());
			}
			logger.info("订单【" + orderCode + "】参与合买记录：" + orderGroupContentList.size() + "条");
			OrderGroupBO orderGroupBO = orderGroupService.findOrderGroupByOrderCode(orderCode);
			// 合买订单的提成金额
			Double commissionAmount = ObjectUtil.isBlank(orderGroupBO.getCommissionAmount()) ? 0d : orderGroupBO.getCommissionAmount();
			if (MathUtil.compareTo(commissionAmount, 0d) > 0) {
				logger.info("订单【" + orderCode + "】合买的提成总金额：" + commissionAmount);
				// 重新设置订单中的用户ID为发起合买人的用户ID
				UserWalletBO userWalletNew = userWalletService.findUserWalletByUserId(buyUserId);
				updateUserWallet(orderCode, orderInfoBO, commissionAmount, userWalletNew.getTotalCashBalance(), buyUserId);
			}
		} else {
			// 先处理中奖的金额
			UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(orderInfoBO.getUserId());
			Double totalCashBalance = userWalletBO.getTotalCashBalance();// 账户总金额
			ResultBO<?> resultBO = null;

			// 中奖金额大于0，执行下面操作
			if (MathUtil.compareTo(aftBonus, 0d) > 0) {
				resultBO = updateUserWallet(orderCode, orderInfoBO, aftBonus, totalCashBalance, orderInfoBO.getUserId());
				if (resultBO.isError()) {
					return resultBO;
				}
				UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
				totalCashBalance = userWalletPO.getTotalCashBalance();
			}

			// 官方加奖金额大于0，账户中还需要扣除官方加奖的金额
			if (MathUtil.compareTo(addBonus, 0d) > 0) {
				resultBO = updateUserWallet(orderCode, orderInfoBO, addBonus, totalCashBalance, orderInfoBO.getUserId());
				if (resultBO.isError()) {
					return resultBO;
				}
			}

			// 中奖得到的红包及本站加奖的金额不为空，修改红包状态，红包账户扣除相应的金额
			if (!ObjectUtil.isBlank(orderInfoBO.getRedCodeGet()) && !ObjectUtil.isBlank(webSiteBonus)) {
				String[] redCodes = orderInfoBO.getRedCodeGet().split(",");
				for (String redCode : redCodes) {
					OperateCouponBO coupon = operateCouponService.findByRedCode(redCode);// 本站加奖获取到的红包
					OperateCouponPO operateCouponPO = new OperateCouponPO();
					operateCouponPO.setRedCode(coupon.getRedCode());
					operateCouponPO.setRedBalance(0d);
					operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.INVALID.getKey());// 已作废
					operateCouponService.updateOperateCoupon(operateCouponPO);
					resultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), coupon.getRedBalance(), PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
					if (resultBO.isError()) {
						throw new RuntimeException();
					}
					UserWalletPO userWalletPO2 = (UserWalletPO) resultBO.getData();

					// 1、添加重置开奖交易流水
					TransUserPO transUserPO = transUserService.addOrderRedResetRecord(orderInfoBO, coupon.getRedBalance(), userWalletPO2);
					transUserLogService.addTransLogRecord(transUserPO);
				}
			}

			// 2、修改订单的中奖状态为 已中奖
			orderInfoBO.setWinningStatus(CancellationConstants.OrderWinningStatusEnum.WINNING.getKey());
			int num = payOrderUpdateMapper.updateOrderWinningStatus(orderInfoBO);
			logger.info("重置开奖，更新订单【" + orderCode + "】中奖状态到已中奖：" + (num > 0 ? "成功" : "失败"));

			// 更新返奖流水
			updateReturnAwardTransUser(orderCode, orderInfoBO);

			// 重置跟单的中奖信息
			if (orderInfoBO.getOrderType() == 2) {
				resetfollowInfoHandle(orderCode, orderInfoBO);
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 重置开奖,更新官方加奖和本站加奖
	* @auth: xiongJinGang
	* @param orderCode
	* @param orderInfoBO
	* @param addBonus
	* @param webSiteBonus
	* @param totalCashBalance
	* @throws Exception
	* @time: 2018年5月22日 上午10:40:23
	*/
	public void updateResetAddPrize(String orderCode, OrderInfoBO orderInfoBO, Double addBonus, Double webSiteBonus, Double totalCashBalance) throws Exception {
		ResultBO<?> resultBO;
		// 官方加奖金额大于0，账户中还需要扣除官方加奖的金额
		if (MathUtil.compareTo(addBonus, 0d) > 0) {
			updateUserWallet(orderCode, orderInfoBO, addBonus, totalCashBalance, orderInfoBO.getUserId());
		}

		// 中奖得到的红包及本站加奖的金额不为空，修改红包状态，红包账户扣除相应的金额
		if (!ObjectUtil.isBlank(orderInfoBO.getRedCodeGet()) && !ObjectUtil.isBlank(webSiteBonus)) {
			String[] redCodes = orderInfoBO.getRedCodeGet().split(",");
			for (String redCode : redCodes) {
				OperateCouponBO coupon = operateCouponService.findByRedCode(redCode);// 本站加奖获取到的红包
				OperateCouponPO operateCouponPO = new OperateCouponPO();
				operateCouponPO.setRedCode(coupon.getRedCode());
				operateCouponPO.setRedBalance(0d);
				operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.INVALID.getKey());// 已作废
				operateCouponService.updateOperateCoupon(operateCouponPO);
				resultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), coupon.getRedBalance(), PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
				if (resultBO.isError()) {
					throw new RuntimeException();
				}
				UserWalletPO userWalletPO2 = (UserWalletPO) resultBO.getData();

				// 1、添加重置开奖交易流水
				TransUserPO transUserPO = transUserService.addOrderRedResetRecord(orderInfoBO, coupon.getRedBalance(), userWalletPO2);
				transUserLogService.addTransLogRecord(transUserPO);
			}
		}
	}

	/**  
	* 方法说明: 更新用户钱包金额及添加交易流水
	* @auth: xiongJinGang
	* @param orderCode
	* @param orderInfoBO
	* @param aftBonus
	* @param userWalletBO
	* @throws Exception
	* @time: 2018年1月17日 上午9:35:13
	* @return: UserWalletPO 
	*/
	public ResultBO<?> updateUserWallet(String orderCode, OrderInfoBO orderInfoBO, Double aftBonus, Double totalCashBalance, Integer userId) throws Exception {
		Double needSubAmount = aftBonus;
		orderInfoBO.setUserId(userId);
		// 税后奖金大于账户总现金金额
		if (MathUtil.compareTo(aftBonus, totalCashBalance) > 0) {
			// 该订单的中奖金额大于总现金金额，记录一条异常流水
			OrderResetExceptionPO orderResetExceptionPO = new OrderResetExceptionPO(orderCode, aftBonus, totalCashBalance, orderInfoBO.getUserId());
			orderResetExceptionMapper.addOrderReset(orderResetExceptionPO);
			logger.info("订单【" + orderCode + "】中奖金额【" + aftBonus + "】大于现金总余额【" + totalCashBalance + "】，扣除钱包账户所有金额");
			needSubAmount = totalCashBalance;// 账户总金额不够扣除中奖的金额，将账户金额全部扣除
		}

		// 1、扣除账户金额
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), needSubAmount, PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.WINNING_EIGHTY_TWENTY.getKey());
		if (resultBO.isError()) {
			return resultBO;
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		// 添加取消中奖的交易流水
		TransUserPO transUserPO = transUserService.addOrderResetRecord(orderInfoBO, needSubAmount, userWalletPO);
		transUserLogService.addTransLogRecord(transUserPO);

		// 更新返奖流水
		updateReturnAwardTransUser(orderCode, orderInfoBO);

		return ResultBO.ok(userWalletPO);
	}

	/**  
	* 方法说明: 重置开奖，更新派奖交易流水中的返奖标识
	* @auth: xiongJinGang
	* @param orderCode
	* @param orderInfoBO
	* @throws Exception
	* @throws NumberFormatException
	* @time: 2018年7月31日 下午12:13:23
	* @return: void 
	*/
	@SuppressWarnings("unchecked")
	public void updateReturnAwardTransUser(String orderCode, OrderInfoBO orderInfoBO) throws Exception, NumberFormatException {
		ResultBO<?> resultBO;
		TransUserVO refundTransUser = new TransUserVO(orderInfoBO.getUserId(), orderCode, PayConstants.TransTypeEnum.RETURN_AWARD.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		// 根据订单号，查询交易记录，将流水中的派
		resultBO = transUserService.findUserTransByOrderCode(refundTransUser);
		if (resultBO.isOK()) {
			List<TransUserBO> list = (List<TransUserBO>) resultBO.getData();
			TransUserBO transUserBO = list.get(0);
			transUserService.updateAwardFlagById(Short.parseShort("1"), transUserBO.getId());
		}
	}

	/** 
	 * @Description: 针对跟单的订单做处理
	 * @param orderCode
	 * @param orderInfo
	 * @throws Exception
	 * @throws RuntimeException
	 * @author wuLong
	 * @date 2017年10月12日 上午11:31:11
	 */
	public ResultBO<?> resetfollowInfoHandle(String orderCode, OrderInfoBO orderInfo) throws Exception, RuntimeException {
		// 根据跟单订单编号查找到推单信息
		OrderIssueInfoBO orderIssueInfoBO = orderIssueInfoDaoMapper.getOrderIssueInfo(orderCode);
		if (!ObjectUtil.isBlank(orderIssueInfoBO)) {
			List<String> followOrderCodes = followedInfoDaoMapper.getFollowOrderCode(orderIssueInfoBO.getId());
			if (!ObjectUtil.isBlank(followOrderCodes)) {
				List<OrderInfoBO> followOrderInfoBOs = payOrderUpdateMapper.getOrderList(followOrderCodes);
				if (!ObjectUtil.isBlank(followOrderInfoBOs)) {
					// 更新推单总佣金为0
					orderIssueInfoDaoMapper.updateToZero(orderIssueInfoBO.getId());
					// 推单佣金提成
					float commissionrate = orderIssueInfoBO.getCommissionRate();
					// 推单对应的所有的跟单信息map集合
					Map<String, OrderFollowedInfoBO> orderFollowedInfoMap = followedInfoDaoMapper.findOrderFollowMap(orderIssueInfoBO.getId());
					for (OrderInfoBO orderInfoBO : followOrderInfoBOs) {
						OrderStatus orderStatus = OrderStatus.parseOrderStatus(orderInfoBO.getOrderStatus());
						if (orderStatus != OrderStatus.TICKETED) {
							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + ",订单状态:" + orderStatus.getDesc() + ",不能做重置派奖");
							continue;
						}
						logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "重新派奖开始");
						updateResetSendPrize(orderInfoBO.getOrderCode());

						OrderFollowedInfoBO orderFollowedInfoBO = orderFollowedInfoMap.get(orderInfoBO.getOrderCode());
						float ysCommissionAmount = orderFollowedInfoBO.getCommissionAmount();
						// 重置之前的跟单佣金
						if (ysCommissionAmount > 0) {
							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的原佣金的跟单方案退回账户开始：" + ysCommissionAmount);
							double ca = Double.valueOf(ysCommissionAmount);
							// 跟单中奖账户退回支出的佣金
							ResultBO<?> srResultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), ca, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (srResultBO.isError()) {
								throw new RuntimeException(srResultBO.getMessage());
							}
							UserWalletPO srUserWalletPO = (UserWalletPO) srResultBO.getData();
							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的原佣金的跟单方案退回账户交易流水开始");
							// 写入跟单退回的收入流水
							// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
							Short transType = PayConstants.TransTypeEnum.REFUND.getKey();
							String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							String message = Constants.FOLLEW_COMMISSIONAMOUNT_INFO;
							TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, message);
							// 获取交易号
							transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							transUserPO.setCashAmount(ca);
							transUserPO.setOrderCode(orderInfoBO.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							transUserPO.setTransAmount(ca);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							transUserPO.setChannelId(orderInfoBO.getChannelId());
							transUserPO.setAmount20(0d);
							transUserPO.setAmount80(0d);
							transUserPO.setAmountWin(srUserWalletPO.getWinningBalance());
							// transUserPO.setTradeCode();
							transUserPO.setRedTransAmount(0d);
							transUserPO.setTotalRedBalance(srUserWalletPO.getEffRedBalance());// 剩余总红包金额
							transUserPO.setTotalCashBalance(srUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							transUserPO.setServiceCharge(0d);
							transUserPO.setRemark(message);
							transUserService.addTransRecord(transUserPO);
							transUserLogService.addTransLogRecord(transUserPO);

							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的推单方案扣除账户的原佣金开始" + ysCommissionAmount);
							// 推单账户退回的佣金支出
							ResultBO<?> zcResultBO = userWalletService.updateUserWalletBySplit(orderInfo.getUserId(), ca, PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (zcResultBO.isError()) {
								throw new RuntimeException(zcResultBO.getMessage());
							}
							UserWalletPO zcUserWalletPO = (UserWalletPO) zcResultBO.getData();

							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的推单方案扣除账户的原佣金的交易流水开始");
							// 推单退回的佣金支出流水
							transType = PayConstants.TransTypeEnum.REFUND.getKey();
							transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							TransUserPO zcTransUserPO = new TransUserPO(orderInfo.getUserId(), transCode, transType, message);
							// 获取交易号
							zcTransUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							zcTransUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							zcTransUserPO.setCashAmount(ca);
							zcTransUserPO.setOrderCode(orderInfo.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							zcTransUserPO.setTransAmount(ca);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							zcTransUserPO.setChannelId(orderInfo.getChannelId());
							zcTransUserPO.setAmount20(0d);
							zcTransUserPO.setAmount80(0d);
							zcTransUserPO.setAmountWin(zcUserWalletPO.getWinningBalance());
							// transUserPO.setTradeCode();
							zcTransUserPO.setRedTransAmount(0d);
							zcTransUserPO.setTotalRedBalance(zcUserWalletPO.getEffRedBalance());// 剩余红包金额
							zcTransUserPO.setTotalCashBalance(zcUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							zcTransUserPO.setServiceCharge(0d);
							zcTransUserPO.setRemark(message);
							transUserService.addTransRecord(zcTransUserPO);
							transUserLogService.addTransLogRecord(zcTransUserPO);
						}
						// 重新计算佣金
						// 根据推单的佣金提成 根据跟单订单税后金额计算出佣金
						double ylamount = orderInfoBO.getAftBonus() - orderInfoBO.getOrderAmount();// 跟单盈利的钱
						double commissionamount = MathUtil.mul(ylamount, commissionrate);
						double bjamount = MathUtil.mul(orderInfoBO.getOrderAmount(), 0.1);
						if (ylamount < bjamount) {
							commissionamount = 0;
							logger.info("跟单方案:" + orderInfoBO.getOrderCode() + "盈利小于本金的10%,本金：" + orderInfoBO.getOrderAmount() + ",税后奖金：" + orderInfoBO.getAftBonus() + ",盈利：" + ylamount);
						}
						if (commissionamount > 0) {

							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "新的佣金支出给推单账户开始：" + commissionamount);

							// 跟单的佣金支出
							followedInfoDaoMapper.update(commissionamount, orderInfoBO.getOrderCode());
							// 跟单中奖账户支出
							ResultBO<?> zcResultBO = userWalletService.updateUserWalletBySplit(orderInfoBO.getUserId(), commissionamount, PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (zcResultBO.isError()) {
								throw new RuntimeException(zcResultBO.getMessage());
							}
							UserWalletPO zcUserWalletPO = (UserWalletPO) zcResultBO.getData();

							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "新的佣金支出给推单账户的交易流水开始");
							// 写入跟单支出流水
							// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动赠送；8：活动消费；9：扣款
							Short transType = PayConstants.TransTypeEnum.DEDUCT.getKey();
							String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							String message = Constants.FOLLEW_COMMISSIONAMOUNT_INFO;
							TransUserPO transUserPO = new TransUserPO(orderInfoBO.getUserId(), transCode, transType, message);
							// 获取交易号
							transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							transUserPO.setCashAmount(commissionamount);
							transUserPO.setOrderCode(orderInfoBO.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							transUserPO.setTransAmount(commissionamount);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							transUserPO.setChannelId(orderInfoBO.getChannelId());
							transUserPO.setAmount20(0d);
							transUserPO.setAmount80(0d);
							transUserPO.setAmountWin(zcUserWalletPO.getWinningBalance());
							// transUserPO.setTradeCode();
							transUserPO.setRedTransAmount(0d);
							transUserPO.setTotalRedBalance(zcUserWalletPO.getEffRedBalance());// 剩余红包金额
							transUserPO.setTotalCashBalance(zcUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							transUserPO.setServiceCharge(0d);
							transUserPO.setRemark(message);
							transUserService.addTransRecord(transUserPO);
							transUserLogService.addTransLogRecord(transUserPO);

							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "推单账户收入新的佣金开始：" + commissionamount);
							// 推单的佣金收入
							orderIssueInfoDaoMapper.update(orderIssueInfoBO.getId(), commissionamount);
							// 推单账户佣金收入
							ResultBO<?> srResultBO = userWalletService.updateUserWalletBySplit(orderInfo.getUserId(), commissionamount, PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.WINNING.getKey());
							if (srResultBO.isError()) {
								throw new RuntimeException(srResultBO.getMessage());
							}
							UserWalletPO srUserWalletPO = (UserWalletPO) srResultBO.getData();
							logger.info("重置的抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "推单账户收入新的佣金的交易流水开始");
							// 推单流水写入
							transType = PayConstants.TransTypeEnum.RETURN_AWARD.getKey();
							transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));
							TransUserPO srTransUserPO = new TransUserPO(orderInfo.getUserId(), transCode, transType, message);
							// 获取交易号
							srTransUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
							srTransUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
							srTransUserPO.setCashAmount(commissionamount);
							srTransUserPO.setOrderCode(orderInfo.getOrderCode());
							// 交易总金额；现金金额+红包金额+服务费
							srTransUserPO.setTransAmount(commissionamount);// 交易总金额；现金金额+红包金额+服务费
							// transUserPO.setThirdTransId("");
							srTransUserPO.setChannelId(orderInfo.getChannelId());
							srTransUserPO.setAmount20(0d);
							srTransUserPO.setAmount80(0d);
							srTransUserPO.setAmountWin(srUserWalletPO.getWinningBalance());
							// transUserPO.setTradeCode();
							srTransUserPO.setRedTransAmount(0d);
							srTransUserPO.setTotalRedBalance(srUserWalletPO.getEffRedBalance());// 剩余红包金额
							srTransUserPO.setTotalCashBalance(srUserWalletPO.getTotalCashBalance());// 剩余总现金金额

							srTransUserPO.setServiceCharge(0d);
							srTransUserPO.setRemark(message);
							transUserService.addTransRecord(srTransUserPO);
							transUserLogService.addTransLogRecord(srTransUserPO);

							logger.info("重置抄单的推单方案：" + orderCode + ",对应的跟单方案：" + orderInfoBO.getOrderCode() + "的派奖扣款提佣金写入交易流水结束");
						}
					}
				}
			} else {
				logger.info("重置抄单的推单方案：" + orderCode + ",没有人跟单");
			}
		} else {
			logger.info("重置抄单的推单方案：" + orderCode + "不存在");
		}
		return null;
	}

	/****************************************重置派奖结束********************************************************/
}
