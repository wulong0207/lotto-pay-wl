package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.service.CancellationRefundOrderGroupService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OrderGroupContentService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.RefundOrderGroupService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.OrderGroupConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.CancellationRefundBO;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * @desc 合买订单退款
 * @author xiongJinGang
 * @date 2018年5月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("cancellationRefundBuyTogetherService")
public class CancellationRefundOrderGroupServiceImpl implements CancellationRefundOrderGroupService {

	private static final Logger logger = LoggerFactory.getLogger(CancellationRefundOrderGroupServiceImpl.class);
	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private TransUserService transUserService;
	@Resource // 合买订单详情
	private OrderGroupContentService orderGroupContentService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private RefundOrderGroupService refundOrderGroupService;

	@Override
	public ResultBO<?> doCancellation(OrderCancelMsgModel orderCancelMsgModel) throws Exception {
		CancellationRefundBO cancellationRefundBO = new CancellationRefundBO(orderCancelMsgModel);
		return singleRefund(cancellationRefundBO);
	}

	/**  
	* 方法说明:单个退款 
	* @auth: xiongJinGang
	* @param cancellationRefundBO
	* @time: 2017年11月14日 下午5:09:26
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> singleRefund(CancellationRefundBO cancellationRefundBO) throws Exception {
		String orderCode = cancellationRefundBO.getOrderCode();
		logger.info("合买订单【" + orderCode + "】撤单开始");

		// 2. 根据订单编号和订单类型查询订单或追号信息
		ResultBO<?> orderBaseResult = getOrderInfo(orderCode);
		if (orderBaseResult.isError()) {
			logger.info("根据合买订单【" + orderCode + "】未获取到订单信息");
			return orderBaseResult;
		}
		PayOrderBaseInfoVO orderInfo = (PayOrderBaseInfoVO) orderBaseResult.getData();

		// 查找合买订单详情
		List<OrderGroupContentBO> orderGroupContent = orderGroupContentService.findOrderGroupContentByOrderCode(orderCode);
		if (ObjectUtil.isBlank(orderGroupContent)) {
			logger.info("获取合买订单【" + orderCode + "】详情失败");
			return ResultBO.err(MessageCodeConstants.ORDER_CANNOT_FIND_SERVICE);
		}
		List<String> orderCodeList = new ArrayList<String>();
		double totalOrderGroupAmount = 0d;// 合买详情中总的认购金额
		for (OrderGroupContentBO orderGroupContentBO : orderGroupContent) {
			if (orderGroupContentBO.getRefundStatus().intValue() == OrderGroupConstants.OrderGroupContentStatusEnum.YES.getKey().intValue()) {
				logger.info("合买订单" + orderGroupContentBO.getBuyCode() + "已完成退款，不能重复退款");
				continue;
			}
			totalOrderGroupAmount = MathUtil.add(totalOrderGroupAmount, orderGroupContentBO.getBuyAmount());
			orderCodeList.add(orderGroupContentBO.getBuyCode());
		}
		if (ObjectUtil.isBlank(orderCodeList)) {
			logger.info("获取合买订单【" + orderCode + "】详情为空，或者不能退款");
			return ResultBO.err(MessageCodeConstants.ORDER_CANNOT_FIND_SERVICE);
		}

		// 4、根据订单号，查询合买交易记录，合买详情记录是先添加的
		List<TransUserBO> transUserList = transUserService.findOrderGroupTransRecord(orderCodeList, PayConstants.TransTypeEnum.LOTTERY.getKey(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		if (ObjectUtil.isBlank(transUserList)) {
			logger.info("未获取到合买订单【" + ArrayUtils.toString(orderCodeList) + "】交易流水，不能退款");
			return ResultBO.err(MessageCodeConstants.PAY_QUERY_PARAM_IS_NULL_FIELD);
		}

		// 修改订单状态、退款到用户钱包等（另外开启一个事务，避免事务没提交，MQ已经发了）
		ResultBO<?> resultBO = refundOrderGroupService.modifyRefundOrder(cancellationRefundBO, transUserList, orderInfo, totalOrderGroupAmount);
		if (resultBO.isOK()) {
			logger.info("合买订单 【" + orderCode + "】撤单结束，开始发送MQ消息");
			// 合买的订单流程MQ，数据结构和以前一样，buyType传1，status, 等待出票: 2，招募中: 20，合买未满员流产: 21
			if (cancellationRefundBO.getBuyTogetherRefundType().equals(OrderGroupConstants.OrderGroupRefundTypeEnum.NOT_FULL_ONE.getKey())) {
				messageProvider.sendOrderFlowMessage(orderCode, null, Short.valueOf("21"), Short.valueOf("1"));
			} else if (cancellationRefundBO.getBuyTogetherRefundType().equals(OrderGroupConstants.OrderGroupRefundTypeEnum.SYSTEM_REFUND.getKey())
					|| cancellationRefundBO.getBuyTogetherRefundType().equals(OrderGroupConstants.OrderGroupRefundTypeEnum.FAIL_REFUND.getKey())) {
				messageProvider.sendOrderFlowMessage(orderCode, null, Short.valueOf("7"), Short.valueOf("1"));
			}
		}
		return resultBO;
	}

	/**
	 * 获取订单信息或订单追号信息
	 * @param orderCode
	 * @param buyType
	 * @return
	 */
	private ResultBO<?> getOrderInfo(String orderCode) {
		PayOrderBaseInfoVO orderInfo = null;
		try {
			orderInfo = payOrderUpdateService.findOrderInfo(orderCode);
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

}
