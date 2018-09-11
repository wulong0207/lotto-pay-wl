package com.hhly.paycore.common;

import java.util.List;

import org.apache.log4j.Logger;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.CancellationConstants.OrderAddIssueStatusEnum;
import com.hhly.skeleton.base.constants.CancellationConstants.OrderAddStatusEnum;
import com.hhly.skeleton.base.constants.CancellationConstants.RefundTagetEnum;
import com.hhly.skeleton.base.constants.CancellationConstants.RefundTypeEnum;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OrderAddedIssueBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * @desc 撤单退款工具类
 * @author xiongJinGang
 * @date 2017年8月22日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class RefundUtil {
	private static Logger logger = Logger.getLogger(RefundUtil.class);

	/**
	 * 撤单基本参数验证
	 *
	 * @param cancellationRefundBO
	 * @return
	 */
	public static ResultBO<?> validateBaseParams(CancellationRefundBO cancellationRefundBO) {
		if (ObjectUtil.isBlank(cancellationRefundBO.getOrderCode())) {
			return ResultBO.err(MessageCodeConstants.ORDER_CODE_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(cancellationRefundBO.getOrderType())) {
			return ResultBO.err(MessageCodeConstants.ORDER_TYPE_IS_NULL_FIELD);
		}

		// 追号订单参数验证
		if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(cancellationRefundBO.getOrderType())) {
			// 退款类型必填
			if (ObjectUtil.isBlank(cancellationRefundBO.getRefundType())) {
				return ResultBO.err(MessageCodeConstants.REFUND_TYPE_IS_NULL_FIELD);
			}

			// 单期撤单退款
			if (CancellationConstants.RefundTypeEnum.CURISSUEREFUND.getKey().equals(cancellationRefundBO.getRefundType())) {
				// 如果退款类型为单期撤单退款，那么必传彩期期号
				if (ObjectUtil.isBlank(cancellationRefundBO.getIssueCode())) {
					logger.debug("订单【" + cancellationRefundBO.getOrderCode() + "】单期追号撤单失败，彩期为空");
					return ResultBO.err(MessageCodeConstants.ISSUE_CODE_IS_NULL_FIELD);
				}
			}
		}

		// 退款金额不为空，判断传入的金额是否为负数
		if (ObjectUtil.isBlank(cancellationRefundBO.getRefundAmount())) {
			return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_IS_NULL_FIELD);
		}

		if (Double.compare(Double.valueOf(cancellationRefundBO.getRefundAmount()), 0) < 0) {
			return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_ERROR_SERVICE);
		}
		return ResultBO.ok();
	}

	/**
	 * 验证订单状态和退款金额是否正确
	 *
	 * @param cancelOrderInfo      订单信息或追号计划信息
	 * @param cancellationRefundBO 撤单传入BO
	 * @param orderAddedIssues     追号计划详情（订单类型为追号此参数才有值，如果只撤一期只有一条数据，撤所有则有多条数据）
	 * @return
	 */
	public static ResultBO<?> validateOrder(PayOrderBaseInfoVO cancelOrderInfo, CancellationRefundBO cancellationRefundBO, List<OrderAddedIssueBO> orderAddedIssues) {
		Double refundAmount = cancellationRefundBO.getRefundAmount();
		logger.info("订单号【" + cancelOrderInfo.getOrderCode() + "】订单状态：" + cancelOrderInfo.getOrderStatus() + "，退款金额：" + refundAmount);
		if (CancellationConstants.OrderTypeEnum.INDENTORDER.getKey().equals(cancellationRefundBO.getOrderType())) {
			// 代购订单，订单状态为正在撤单中
			if (!CancellationConstants.OrderStatusEnum.INCANCELLATION.getKey().equals(cancelOrderInfo.getOrderStatus())) {
				logger.info("订单号【" + cancelOrderInfo.getOrderCode() + "】订单状态：" + cancelOrderInfo.getOrderStatus() + "不是撤单中");
				return ResultBO.err(MessageCodeConstants.ORDER_STATUS_ERROR_SERVICE);
			}
			// 代购订单退款金额需和订单总金额一致
			if (MathUtil.compareTo(refundAmount, cancelOrderInfo.getOrderAmount()) != 0) {
				logger.info("订单号【" + cancelOrderInfo.getOrderCode() + "】订单金额" + cancelOrderInfo.getOrderAmount() + " 与申请撤单金额" + refundAmount + " 不相等");
				return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_ERROR_SERVICE);
			}
		} else if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(cancellationRefundBO.getOrderType())) {
			// 追号订单
			Double countAmount = getWaitAddedAmountTotal(orderAddedIssues);// 计算出需要退款金额
			int compareResult = MathUtil.compareTo(refundAmount, countAmount);
			if (compareResult != 0) {
				logger.info("订单号【" + cancelOrderInfo.getOrderCode() + "】撤单中金额" + countAmount + " 与申请撤单金额" + refundAmount + " 不相等");
				return ResultBO.err(MessageCodeConstants.REFUND_AMOUNT_ERROR_SERVICE);
			}
		}
		return ResultBO.ok();
	}

	/**
	 * 获取等待追号的总认购金额
	 *
	 * @param orderAddeds
	 * @return
	 */
	public static Double getWaitAddedAmountTotal(List<OrderAddedIssueBO> orderAddeds) {
		Double addedAmountTotal = 0.0d;
		for (OrderAddedIssueBO o : orderAddeds) {
			addedAmountTotal = MathUtil.add(addedAmountTotal, o.getBuyAmount());
		}
		return addedAmountTotal;
	}

	/**
	 * 根据原追号状态获取退款后的追号状态
	 *
	 * @param orderStatus
	 * @return
	 */
	public static OrderAddIssueStatusEnum getOrderAddedIssueAddStatus(Short orderStatus) {
		OrderAddIssueStatusEnum addIssueStatusEnum = OrderAddIssueStatusEnum.getEnum(orderStatus);
		OrderAddIssueStatusEnum addStatusEnum = null;
		switch (addIssueStatusEnum) {
		case INCANCELLATION:
		case INSTOPADDCANCELLATION:
			addStatusEnum = OrderAddIssueStatusEnum.CANCELLATIONSYSTEM;
			break;
		case INUSERCANCELLATION:
			addStatusEnum = OrderAddIssueStatusEnum.CANCELLATIONUSER;
			break;
		default:
			break;
		}
		return addStatusEnum;
	}

	/**
	 * 根据追号详情状态获取追号计划add_status
	 *
	 * @param orderStatus
	 * @return
	 */
	public static OrderAddStatusEnum getOrderAddedAddStatus(Short orderStatus) {
		OrderAddStatusEnum addStatusEnum = null;
		OrderAddIssueStatusEnum addIssueStatusEnum = OrderAddIssueStatusEnum.getEnum(orderStatus);
		switch (addIssueStatusEnum) {
		case INCANCELLATION:
		case INSTOPADDCANCELLATION:
			addStatusEnum = OrderAddStatusEnum.CANCELLATIONSYSTEM;
			break;
		case INUSERCANCELLATION:
			addStatusEnum = OrderAddStatusEnum.CANCELLATIONUSER;
			break;
		default:
			break;
		}
		return addStatusEnum;
	}

	/**
	 * 获取退款目标 目前系统撤单退至余额 用户撤单退至彩金
	 * @param orderType
	 * @param refundType
	 * @return
	 */
	public static RefundTagetEnum getRefundTaget(Short orderType, Short refundType) {
		RefundTagetEnum refundTagetEnum = null;
		RefundTypeEnum refundTypeEnum = CancellationConstants.RefundTypeEnum.getEnum(refundType);

		if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(orderType)) {
			switch (refundTypeEnum) {
			case WINNINGSTOPREFUND:
			case CURISSUEREFUND:
			case MULTIISSUEREFUND:
				refundTagetEnum = RefundTagetEnum.REFUNDTO80BALANCE;
				break;
			case USERCANCELREFUND:
				refundTagetEnum = RefundTagetEnum.REFUNDTOHANDSEL;
				break;
			}
		} else {
			refundTagetEnum = RefundTagetEnum.REFUNDTO80BALANCE;
		}
		return refundTagetEnum;
	}

	/**  
	* 方法说明: 验证订单字段中有多少个订单号，单个返回true【 D1705222208440100066,1;D1705222201040100065,1;】
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年8月22日 上午10:06:59
	* @return: boolean 
	*/
	public static boolean isSinglePay(String orderCode) {
		boolean flag = false;
		String[] orderCodes = orderCode.split(";");
		if (orderCodes.length == 1) {
			flag = true;
		}
		return flag;
	}

}
