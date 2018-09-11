package com.hhly.paycore.remote.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.ActivityPayUtil;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.remote.service.IActivityPayService;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.pay.activity.vo.ActivityPayParamVO;
import com.hhly.skeleton.pay.activity.vo.ActivityPayParamVO.ACTIVITY_TYPE;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayResultVO;
import com.hhly.skeleton.user.bo.UserWalletBO;

@Service("iActivityPayService")
public class ActivityPayServiceImpl implements IActivityPayService {
	private static final Logger logger = LoggerFactory.getLogger(ActivityPayServiceImpl.class);

	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;

	@Override
	public ResultBO<?> activityPay(ActivityPayParamVO activityPayParam) throws Exception {
		ResultBO<?> resultBO = ActivityPayUtil.validateParam(activityPayParam);
		if (resultBO.isError()) {
			logger.error(resultBO.getMessage());
			return resultBO;
		}

		// 判断活动类型
		PayOrderBaseInfoVO orderInfo = null;
		try {
			if (ACTIVITY_TYPE.DOUBLE_CHROMOSPHERE.getKey().equals(activityPayParam.getActivityType())) {
				// 双色球活动
				orderInfo = payOrderUpdateService.findOrderInfo(activityPayParam.getOrderCode());
			} else if (ACTIVITY_TYPE.ELEVEN_CHOSE_FIVE.getKey().equals(activityPayParam.getActivityType())) {
				// 11选5活动
				orderInfo = payOrderUpdateService.findOrderAdded(activityPayParam.getOrderCode());
			}
		} catch (Exception e) {
			logger.error("获取订单【" + activityPayParam.getOrderCode() + "】信息异常", e);
			return ResultBO.err(MessageCodeConstants.GET_ORDER_ERROR_SERVICE);
		}
		if (ObjectUtil.isBlank(orderInfo)) {
			logger.error("获取订单【" + activityPayParam.getOrderCode() + "】信息为空");
			return ResultBO.err(MessageCodeConstants.GET_ORDER_ERROR_SERVICE);
		}

		// 验证订单状态
		resultBO = ActivityPayUtil.validateOrderStatus(orderInfo);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 组装充值BO
		TransRechargeBO transRechargeBO = packageRecharge(activityPayParam, orderInfo);
		/*****************生成活动红包 ********************/
		transRechargeBO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));
		OperateCouponBO operateCouponBO = addActivityInfo(activityPayParam, orderInfo, transRechargeBO);

		/*******************************/
		// 2、使用了红包【更新红包余额、状态等信息】、添加红包使用记录
		StringBuffer remark = new StringBuffer();
		if (!ObjectUtil.isBlank(operateCouponBO.getRedCode())) {
			remark.append("红包");
			Short transType = TransTypeEnum.LOTTERY.getKey();// 购彩
			// 更新红包状态，添加红包使用记录
			resultBO = operateCouponService.updateRedInfo(transRechargeBO, transType, TransTypeEnum.LOTTERY.getValue());
			operateCouponBO = (OperateCouponBO) resultBO.getData();
		}

		// 3、更新账户中彩金红包金额
		resultBO = userWalletService.updateUserWalletBySplit(orderInfo.getUserId(), operateCouponBO.getRedValue(), MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		remark.append("支付");

		// 4、批量更新订单支付成功状态
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);// 支付成功
		payNotifyResult.setRemark(remark.toString());
		List<PayOrderBaseInfoVO> orderList = new ArrayList<PayOrderBaseInfoVO>();
		orderList.add(orderInfo);
		payOrderUpdateService.updateOrderBatch(orderList, payNotifyResult, transRechargeBO);

		// 5、添加交易记录
		List<TransUserPO> transUserList = transUserService.addGouCaiTransRecordBatch(orderList, payNotifyResult, userWalletPO, transRechargeBO);
		// 添加给用户查看的交易流水
		transUserLogService.addTransUserByBatch(transUserList);

		// 6、拼装支付结果
		return packagePayResult(orderInfo, transRechargeBO, operateCouponBO);
	}

	/**  
	* 方法说明: 组装支付结果
	* @auth: xiongJinGang
	* @param orderInfo
	* @param transRechargeBO
	* @param operateCouponBO
	* @time: 2018年1月9日 上午10:38:44
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> packagePayResult(PayOrderBaseInfoVO orderInfo, TransRechargeBO transRechargeBO, OperateCouponBO operateCouponBO) {
		PayResultVO payResultVO = new PayResultVO();
		payResultVO.setTransRechargeCode(transRechargeBO.getTransRechargeCode());
		payResultVO.setOrderCode(transRechargeBO.getOrderCode());
		payResultVO.setBuyType(orderInfo.getBuyType() + "");
		payResultVO.setPayStatus(transRechargeBO.getTransStatus());
		payResultVO.setTransTime(DateUtil.getNowDate());
		payResultVO.setOrderAmount(orderInfo.getOrderAmount());
		payResultVO.setRedName(operateCouponBO.getRedName());
		payResultVO.setRedAmount(operateCouponBO.getRedValue());
		payResultVO.setServiceCharge(0d);
		payResultVO.setLotteryCode(orderInfo.getLotteryCode() + "");
		payResultVO.setOrderCode(orderInfo.getOrderCode());
		payResultVO.setPayAmount(orderInfo.getOrderAmount());
		return ResultBO.ok(payResultVO);
	}

	/**  
	* 方法说明: 拼装充值请求参数
	* @auth: xiongJinGang
	* @param activityPayParam
	* @param orderInfo
	* @time: 2018年1月9日 上午10:23:04
	* @return: void 
	*/
	private TransRechargeBO packageRecharge(ActivityPayParamVO activityPayParam, PayOrderBaseInfoVO orderInfo) {
		TransRechargeBO transRechargeBO = new TransRechargeBO();
		transRechargeBO.setUserId(orderInfo.getUserId());// 设置用户Id
		transRechargeBO.setRedAmount(orderInfo.getOrderAmount());// 使用红包金额
		transRechargeBO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transRechargeBO.setChannelId(orderInfo.getChannelId());// 渠道ID
		transRechargeBO.setTransRechargeCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRechargeBO.setRechargeAmount(0d);// 充值金额
		transRechargeBO.setArrivalAmount(0d);// 到账金额
		transRechargeBO.setActivityCode(activityPayParam.getActivityType() + "");
		transRechargeBO.setOrderCode(orderInfo.getOrderCode());
		return transRechargeBO;
	}

	/**  
	* 方法说明: 添加活动红包、交易流水等
	* @auth: xiongJinGang
	* @param activityPayParam
	* @param orderInfo
	* @throws Exception
	* @time: 2018年1月9日 上午10:16:52
	* @return: void 
	*/
	private OperateCouponBO addActivityInfo(ActivityPayParamVO activityPayParam, PayOrderBaseInfoVO orderInfo, TransRechargeBO transRechargeBO) throws Exception {

		// 生成红包
		Double orderAmount = orderInfo.getOrderAmount();// 订单金额
		// 组装优惠券参数
		OperateCouponBO operateCouponBO = new OperateCouponBO(PayConstants.RedTypeEnum.RED_COLOR.getValue(), transRechargeBO.getActivityCode(), orderInfo.getUserId(), PayConstants.RedSourceEnum.ACTIVITY.getKey(), Constants.ACTIVITY_SEND);
		OperateCouponPO operateCouponPO = operateCouponService.addRedColor(operateCouponBO, orderAmount);// 红包金额就是订单金额
		transRechargeBO.setRedCode(operateCouponPO.getRedCode());// 红包编号
		// 2、 添加红包交易流水
		transRedService.addRedTransRecord(operateCouponPO, transRechargeBO);
		// 本地支付时，需要先插入红包金额
		// 先加彩金红包，后面再减彩金红包
		userWalletService.updateUserWalletBySplit(transRechargeBO.getUserId(), transRechargeBO.getRedAmount(), MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		// 3、添加彩金红包生成记录
		try {
			UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(transRechargeBO.getUserId());
			TransUserPO transUserPO = transUserService.addActivityTransRecord(transRechargeBO, userWalletBO.getTotalCashBalance(), userWalletBO.getEffRedBalance(), operateCouponPO);
			// 添加给用户查看的交易流水
			transUserLogService.addTransLogRecord(transUserPO);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return operateCouponBO;
	}

}
