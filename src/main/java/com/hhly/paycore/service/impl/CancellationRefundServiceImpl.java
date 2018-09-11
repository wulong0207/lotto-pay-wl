package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.RefundUtil;
import com.hhly.paycore.dao.OrderAddedIssueMapper;
import com.hhly.paycore.service.CancellationRefundService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.RefundOrderService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderFlowInfoEnum;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderAddedIssueBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.TransUserVO;

/**
 * @desc 追号计划撤单、订单撤单
 * @author xiongJinGang
 * @date 2017年11月14日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("cancellationRefundService")
public class CancellationRefundServiceImpl implements CancellationRefundService {

	private static final Logger logger = LoggerFactory.getLogger(CancellationRefundServiceImpl.class);
	private static final String SPLIT_SYMBOL = ",";// 退款分隔符

	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private RefundOrderService refundOrderService;
	@Resource
	private OrderAddedIssueMapper orderAddedIssueMapper;
	@Resource
	private MessageProvider messageProvider;

	@Override
	public ResultBO<?> doCancellation(OrderCancelMsgModel orderCancelMsgModel) throws Exception {
		// 退款类型不为空，并且包含逗号，表示是CMS后台批量过来退款处理，存在多种退款状态
		if (!ObjectUtil.isBlank(orderCancelMsgModel.getRefundType()) && orderCancelMsgModel.getRefundType().contains(",")) {
			return batchRefund(orderCancelMsgModel);
		} else {
			// 系统自动撤单，走原路的方式
			CancellationRefundBO cancellationRefundBO = new CancellationRefundBO(orderCancelMsgModel);
			if (!ObjectUtil.isBlank(orderCancelMsgModel.getRefundType()) && !orderCancelMsgModel.getRefundType().equals("null")) {
				cancellationRefundBO.setRefundType(Short.parseShort(orderCancelMsgModel.getRefundType()));
			}
			cancellationRefundBO.setRefundAmount(Double.parseDouble(orderCancelMsgModel.getRefundAmount()));
			return singleRefund(cancellationRefundBO);
		}
	}

	/**  
	* 方法说明:单个退款 
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @time: 2017年11月14日 下午5:09:26
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("unchecked")
	private ResultBO<?> singleRefund(CancellationRefundBO cancellationRefundBO) throws Exception {
		Short orderType = cancellationRefundBO.getOrderType(); // 订单类型；1-代购订单；2-追号计划
		String orderCode = cancellationRefundBO.getOrderCode();
		logger.info("######## 【" + orderCode + "】撤单开始 #########");
		// 1. 基本参数验证
		ResultBO<?> validateParamsResult = RefundUtil.validateBaseParams(cancellationRefundBO);
		if (validateParamsResult.isError()) {
			logger.info("订单【" + orderCode + "】撤单基本参数验证不过");
			return validateParamsResult;
		}

		// 2. 根据订单编号和订单类型查询订单或追号信息
		ResultBO<?> orderBaseResult = getOrderInfo(orderCode, orderType);
		if (orderBaseResult.isError()) {
			logger.info("根据订单编号【" + orderCode + "】和订单类型【" + orderType + "】查询订单或追号信息");
			return orderBaseResult;
		}
		PayOrderBaseInfoVO orderInfo = (PayOrderBaseInfoVO) orderBaseResult.getData();
		List<OrderAddedIssueBO> orderAddedIssues = null;
		// 追号计划撤单时查询追号期数详情
		if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(orderType)) {
			orderAddedIssues = getOrderAddedIssues(cancellationRefundBO);
			if (ObjectUtil.isBlank(orderAddedIssues)) {
				return ResultBO.err(MessageCodeConstants.ORDER_ADDED_ISSUE_CANNOT_FIND_SERVICE);
			}
		}
		// 3. 验证订单状态、金额是否正确
		ResultBO<?> resultBO = RefundUtil.validateOrder(orderInfo, cancellationRefundBO, orderAddedIssues);
		if (resultBO.isError()) {
			return resultBO;
		}
		Short transType = PayConstants.TransTypeEnum.LOTTERY.getKey();// 查询购彩的交易记录
		String orderNo = orderCode;
		// 如果是代购订单，判断refundType类型
		if (CancellationConstants.OrderTypeEnum.INDENTORDER.getKey().equals(orderType)) {
			// 如果退款类型不为空，并且是代购撤单。订单号为正常的订单号，要根据这个订单号去查询追号订单号
			if (!ObjectUtil.isBlank(cancellationRefundBO.getRefundType()) && cancellationRefundBO.getRefundType().equals(CancellationConstants.OrderRefundTypeEnum.CHASE_REFUND.getKey())) {
				OrderAddedIssueBO orderAddedIssueBO = orderAddedIssueMapper.getOrderInfo(orderCode);
				orderNo = orderAddedIssueBO.getOrderAddCode();
			}
		}

		TransUserVO transUserVO = new TransUserVO(orderInfo.getUserId(), orderNo, transType, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		// 4、根据订单号，查询交易记录
		resultBO = transUserService.findUserTransByOrderCode(transUserVO);
		if (resultBO.isError()) {
			logger.info("获取用户【" + orderInfo.getUserId() + "】，订单【" + orderCode + "】，追号订单【" + orderNo + "】，交易类型【" + transType + "】的交易数据为空");
			return resultBO;
		}
		List<TransUserBO> tradeList = (List<TransUserBO>) resultBO.getData();
		TransUserBO transUserBO = tradeList.get(0);

		// *********************************需要验证订单是否已经退过款******************
		// 不是追号订单,需要验证是否退过款；追号订单撤单，每期单号都一样。如果要修改，流水中需要记录每一期的期号
		if (!CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(orderType)) {
			TransUserVO refundTransUser = new TransUserVO(orderInfo.getUserId(), orderCode, PayConstants.TransTypeEnum.REFUND.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
			// 5、根据订单号，查询交易记录
			resultBO = transUserService.findUserTransByOrderCode(refundTransUser);
			if (resultBO.isOK()) {
				List<TransUserBO> refundTradeList = (List<TransUserBO>) resultBO.getData();
				if (!refundTradeList.isEmpty()) {
					logger.info("订单【" + orderCode + "】撤单退款失败，交易流水中已经存在该订单的撤单记录，不能重复退款，将订单状修改成已退款");
					refundOrderService.updateOrderStatus(cancellationRefundBO, orderAddedIssues, orderInfo);
					return ResultBO.err(MessageCodeConstants.REFUND_REQUEST_FAIL_ERROR_SERVICE);
				}
			}
			if (resultBO.isError() && !resultBO.getErrorCode().equals(MessageCodeConstants.DATA_NOT_FOUND_SYS)) {
				logger.info("订单【" + orderCode + "】查询交易流水异常：" + resultBO.getMessage());
				return resultBO;
			}
		}

		// 查询是否使用红包
		OperateCouponBO operateCouponBO = null;
		if (!ObjectUtil.isBlank(transUserBO.getRedCode())) {
			resultBO = operateCouponService.findCouponByRedCode(transUserBO.getRedCode());
			if (resultBO.isError()) {
				logger.debug("获取红包【" + transUserBO.getRedCode() + "】详情失败");
				return resultBO;
			}
			operateCouponBO = (OperateCouponBO) resultBO.getData();
		}
		// 修改订单状态、退款到用户钱包等（另外开启一个事务，避免事务没提交，MQ已经发了）
		refundOrderService.modifyRefundOrder(cancellationRefundBO, transUserBO, operateCouponBO, orderInfo, orderAddedIssues);
		logger.info("######## 【" + orderCode + "】撤单结束，开始发送MQ消息 #########");
		// 发送撤单方案详情MQ消息
		Short status = Short.parseShort(OrderFlowInfoEnum.StatusEnum.CANCEL_ORDER.getKey() + "");
		messageProvider.sendOrderFlowMessage(orderCode, null, status, orderType);
		return ResultBO.ok();
	}

	/**  
	* 方法说明: CMS批量退款
	* @auth: xiongJinGang
	* @param orderCancelMsgModel
	* @throws Exception
	* @time: 2017年11月14日 下午5:29:09
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("unchecked")
	private ResultBO<?> batchRefund(OrderCancelMsgModel orderCancelMsgModel) throws Exception {
		String orderCode = orderCancelMsgModel.getOrderCode();
		logger.info("######## 【" + orderCode + "】CMS批量撤单开始 #########");

		Short orderType = orderCancelMsgModel.getOrderType(); // 订单类型；1-代购订单；2-追号计划

		List<CancellationRefundBO> refundList = new ArrayList<CancellationRefundBO>();
		CancellationRefundBO cancellationRefund = null;
		// 退款类型和金额是一一对应的关系（通过逗号分隔）
		String[] refundTypes = orderCancelMsgModel.getRefundType().split(SPLIT_SYMBOL);
		String[] refundAmount = orderCancelMsgModel.getRefundAmount().split(SPLIT_SYMBOL);

		// 循环获取退款类型和退款金额
		for (int i = 0; i < refundTypes.length; i++) {
			cancellationRefund = new CancellationRefundBO(orderCancelMsgModel);
			cancellationRefund.setRefundType(Short.parseShort(refundTypes[i]));
			cancellationRefund.setRefundAmount(Double.parseDouble(refundAmount[i]));

			// 1. 基本参数验证
			ResultBO<?> resultBO = RefundUtil.validateBaseParams(cancellationRefund);
			if (resultBO.isError()) {
				logger.info("订单【" + orderCode + "】撤单基本参数验证不过");
				return resultBO;
			}
			refundList.add(cancellationRefund);
		}

		// 2. 根据订单编号和订单类型查询订单或追号信息
		ResultBO<?> orderBaseResult = getOrderInfo(orderCode, orderType);
		if (orderBaseResult.isError()) {
			logger.info("根据订单编号【" + orderCode + "】和订单类型【" + orderType + "】查询订单或追号信息");
			return orderBaseResult;
		}
		PayOrderBaseInfoVO orderInfo = (PayOrderBaseInfoVO) orderBaseResult.getData();
		Short transType = PayConstants.TransTypeEnum.LOTTERY.getKey();// 查询购彩的交易记录
		TransUserBO transUserBO = null;
		Map<CancellationRefundBO, List<OrderAddedIssueBO>> map = new HashMap<CancellationRefundBO, List<OrderAddedIssueBO>>();

		// 各种退款类型的list
		for (CancellationRefundBO cancellationRefundBO : refundList) {
			List<OrderAddedIssueBO> orderAddedIssues = null;
			// 追号计划撤单时查询追号期数详情
			if (CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey().equals(orderType)) {
				orderAddedIssues = getOrderAddedIssues(cancellationRefundBO);
				if (ObjectUtil.isBlank(orderAddedIssues)) {
					return ResultBO.err(MessageCodeConstants.ORDER_ADDED_ISSUE_CANNOT_FIND_SERVICE);
				}
			}
			// 3. 验证订单状态、金额是否正确
			ResultBO<?> resultBO = RefundUtil.validateOrder(orderInfo, cancellationRefundBO, orderAddedIssues);
			if (resultBO.isError()) {
				return resultBO;
			}

			// 不同类型的存入到map中
			map.put(cancellationRefundBO, orderAddedIssues);

			// 交易流水为空，需要获取【refundList为同一个订单的信息，获取交易详情，只需要获取一次就行】
			if (ObjectUtil.isBlank(transUserBO)) {
				String orderNo = orderCode;
				// 如果是代购订单，判断refundType类型
				if (CancellationConstants.OrderTypeEnum.INDENTORDER.getKey().equals(orderType)) {
					// 如果退款类型不为空，并且是代购撤单。订单号为正常的订单号，要根据这个订单号去查询追号订单号
					if (!ObjectUtil.isBlank(cancellationRefundBO.getRefundType()) && cancellationRefundBO.getRefundType().equals(CancellationConstants.OrderRefundTypeEnum.CHASE_REFUND.getKey())) {
						OrderAddedIssueBO orderAddedIssueBO = orderAddedIssueMapper.getOrderInfo(orderCode);
						orderNo = orderAddedIssueBO.getOrderAddCode();
					}
				}

				// 获取交易流水，得到各个子账户上花费的金额
				TransUserVO transUserVO = new TransUserVO(orderInfo.getUserId(), orderNo, transType, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
				// 4、查询交易记录
				resultBO = transUserService.findUserTransByOrderCode(transUserVO);
				if (resultBO.isError()) {
					logger.info("获取用户【" + orderInfo.getUserId() + "】，订单【" + orderCode + "】，追号订单【" + orderNo + "】，交易类型【" + transType + "】的交易数据为空");
					return resultBO;
				}
				List<TransUserBO> tradeList = (List<TransUserBO>) resultBO.getData();
				transUserBO = tradeList.get(0);
			}
		}

		// 交易流水为空，返回错误
		if (ObjectUtil.isBlank(transUserBO)) {
			logger.info("订单【" + orderCode + "】交易流水号获取失败");
			return ResultBO.err(MessageCodeConstants.PAY_QUERY_PARAM_ERROR_SERVICE);
		}
		// 查询是否使用红包
		OperateCouponBO operateCouponBO = null;
		if (!ObjectUtil.isBlank(transUserBO.getRedCode())) {
			ResultBO<?> resultBO = operateCouponService.findCouponByRedCode(transUserBO.getRedCode());
			if (resultBO.isError()) {
				logger.info("获取红包【" + transUserBO.getRedCode() + "】详情失败");
				return resultBO;
			}
			operateCouponBO = (OperateCouponBO) resultBO.getData();
		}
		// 修改订单状态、退款到用户钱包等（另外开启一个事务，避免事务没提交，MQ已经发了）
		logger.info("【" + orderCode + "】批量修改订单撤单状态开始");
		refundOrderService.modifyRefundOrderByBatch(map, transUserBO, operateCouponBO, orderInfo);

		logger.info("######## 【" + orderCode + "】CMS撤单结束，开始发送MQ消息 #########");
		// 发送撤单方案详情MQ消息
		Short status = Short.parseShort(OrderFlowInfoEnum.StatusEnum.CANCEL_ORDER.getKey() + "");
		messageProvider.sendOrderFlowMessage(orderCode, null, status, orderType);
		return ResultBO.ok();
	}

	/**
	 * 获取订单信息或订单追号信息
	 * @param orderCode
	 * @param buyType
	 * @return
	 */
	private ResultBO<?> getOrderInfo(String orderCode, Short buyType) {
		PayOrderBaseInfoVO orderInfo = new PayOrderBaseInfoVO();
		try {
			if (CancellationConstants.OrderTypeEnum.INDENTORDER.getKey().equals(buyType)) {
				orderInfo = payOrderUpdateService.findOrderInfo(orderCode);
			} else {
				orderInfo = payOrderUpdateService.findOrderAdded(orderCode);
			}
		} catch (Exception e) {
			logger.error("获取订单【" + orderCode + "】异常：", e);
			return ResultBO.err(MessageCodeConstants.GET_ORDER_ERROR_SERVICE);
		}
		if (ObjectUtil.isBlank(orderInfo)) {
			logger.info("获取订单【" + orderCode + "】详情失败");
			return ResultBO.err(MessageCodeConstants.ORDER_CANNOT_FIND_SERVICE);
		}
		return ResultBO.ok(orderInfo);
	}

	/**
	 * 组装参数 获取撤单的追号详情
	 * @param cancellationRefundBO
	 * @return
	 */
	private List<OrderAddedIssueBO> getOrderAddedIssues(CancellationRefundBO cancellationRefundBO) {
		OrderAddedIssueBO addIssueBO = new OrderAddedIssueBO();
		if (CancellationConstants.RefundTypeEnum.WINNINGSTOPREFUND.getKey().equals(cancellationRefundBO.getRefundType())) {
			addIssueBO.setAddStatus(CancellationConstants.OrderAddIssueStatusEnum.INSTOPADDCANCELLATION.getKey());// 停追撤单中
		} else if (CancellationConstants.RefundTypeEnum.CURISSUEREFUND.getKey().equals(cancellationRefundBO.getRefundType()) || CancellationConstants.RefundTypeEnum.MULTIISSUEREFUND.getKey().equals(cancellationRefundBO.getRefundType())) {
			addIssueBO.setAddStatus(CancellationConstants.OrderAddIssueStatusEnum.INCANCELLATION.getKey());// 撤单中
		} else if (CancellationConstants.RefundTypeEnum.USERCANCELREFUND.getKey().equals(cancellationRefundBO.getRefundType())) {
			addIssueBO.setAddStatus(CancellationConstants.OrderAddIssueStatusEnum.INUSERCANCELLATION.getKey());// 用户撤单中
		}
		addIssueBO.setOrderAddCode(cancellationRefundBO.getOrderCode());// 订单号

		if (!ObjectUtil.isBlank(cancellationRefundBO.getIssueCode())) {
			addIssueBO.setIssueCode(cancellationRefundBO.getIssueCode());// 彩期
		}
		return orderAddedIssueMapper.getCancelOrderAddedIssues(addIssueBO);
	}

}
