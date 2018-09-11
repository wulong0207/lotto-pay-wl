//package com.hhly.paycore.jms;
//
//import javax.annotation.Resource;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.amqp.core.Message;
//import org.springframework.amqp.core.MessageListener;
//import org.springframework.amqp.rabbit.annotation.RabbitListener;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import com.hhly.paycore.service.CancellationRefundOrderGroupService;
//import com.hhly.paycore.service.CancellationRefundService;
//import com.hhly.skeleton.base.bo.ResultBO;
//import com.hhly.skeleton.base.constants.CancellationConstants;
//import com.hhly.skeleton.base.mq.OrderCancelMsgModel;
//import com.hhly.skeleton.base.util.JsonUtil;
//import com.hhly.skeleton.base.util.ObjectUtil;
//
//@Component("orderCancelMessageListener")
//public class OrderCancelMessageListener implements MessageListener {
//
//	private Logger logger = LoggerFactory.getLogger(OrderCancelMessageListener.class);
//
//	@Autowired
//	private CancellationRefundService cancellationRefundService;
//	@Resource
//	private CancellationRefundOrderGroupService cancellationRefundOrderGroupService;
//
//	@RabbitListener(queues="${order_cancel_queuename}", containerFactory="rabbitmqContainerFactory" , priority = "10")
//	public void onMessage(Message message) {
//		if (ObjectUtil.isBlank(message)) {
//			logger.error("接收【用户撤单退款消息】请求参数为空，不处理！");
//			return;
//		}
//		OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
//		String msg = null;
//		try {
//			msg = new String(message.getBody(), "UTF-8");
//			logger.info("收到撤单消息【" + msg + "】");
//			cancellationRefundBO = (OrderCancelMsgModel) JsonUtil.json2Object(msg, OrderCancelMsgModel.class);
//			ResultBO<?> resultBO = null;
//			if (cancellationRefundBO.getOrderType().intValue() != CancellationConstants.OrderTypeEnum.ORDERGROUP.getKey().intValue()) {
//				// 合买退款类型为空，走正常流程退款
//				resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
//			} else {
//				// 合买退款类型不为空，走合买流程退款
//				resultBO = cancellationRefundOrderGroupService.doCancellation(cancellationRefundBO);
//			}
//			logger.info("执行撤单返回【" + resultBO.getMessage() + "】");
//		} catch (Exception e) {
//			logger.error("接收【用户撤单退款消息】异常：" + msg, e);
//		}
//	}
//
//}
