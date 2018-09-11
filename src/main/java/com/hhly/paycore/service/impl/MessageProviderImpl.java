package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.skeleton.activity.vo.ActivityMessageModel;
import com.hhly.skeleton.activity.vo.MsgVO;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.mq.ChaseOrderCreateMsgModel;
import com.hhly.skeleton.base.mq.msg.MessageModel;
import com.hhly.skeleton.base.mq.msg.OperateNodeMsg;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * 发送消息
 *
 * @author wulong
 * @create 2017/5/11 17:08
 */
@Service
public class MessageProviderImpl implements MessageProvider {
	private static final Logger logger = LoggerFactory.getLogger(MessageProviderImpl.class);
	private static final String MESSAGE_SOURCE = "lotto-pay";
	@Autowired
	private AmqpTemplate amqpTemplate;

	/**
	 * 向消息队列中发送消息
	 * @param queueKey 队列名
	 * @param message  消息
	 */
	@Override
	public void sendMessage(String queueKey, Object message) {
		String jsonStr = "";
		if (message instanceof String) {
			jsonStr = (String) message;
		} else {
			jsonStr = JSON.toJSONString(message);
		}
		try {
			byte[] body = jsonStr.getBytes();
			MessageProperties properties = new MessageProperties();
			properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
			properties.setPriority(new Random().nextInt(10));
			Message message2 = new Message(body, properties);
			amqpTemplate.send(queueKey, message2);
			logger.info("发送消息成功！queueKey：" + queueKey + "，消息内容：" + jsonStr);
		} catch (Exception e) {
			logger.error("发送消息异常！queueKey：" + queueKey + "，消息内容：" + jsonStr, e);
		}
	}

	@Override
	public void sendOrderFlowMessage(String orderCode, String payTime, Short status, Short buyType) {
		// 1：提交方案2:支付成功3：支付失败4:未支付过期 代购专有：（等待出票）5：出票中 6 出票失败7.已撤单8：等待开奖9：已中奖10：未中奖11：已派奖 追号专有：12：追号中13：追号结束14：中奖追停15：追号撤单
		JSONObject jsonObject = new JSONObject();
		// 订单类型、1代购订单；2追号订单
		if (buyType.equals(CancellationConstants.OrderTypeEnum.ADDEDORDER.getKey())) {
			jsonObject.put("orderAddCode", StringUtil.interceptEndSymbol(orderCode, ","));
		} else {
			jsonObject.put("orderCode", StringUtil.interceptEndSymbol(orderCode, ","));
		}
		if (ObjectUtil.isBlank(payTime)) {
			payTime = DateUtil.getNow();
		}
		jsonObject.put("createTime", payTime);
		jsonObject.put("buyType", buyType);// 1:代购, 2:追号
		jsonObject.put("status", status);
		sendMessage(Constants.QUEUE_NAME_FOR_ORDER_FLOW, jsonObject.toJSONString());
	}

	@Override
	public void sendOrderAddMessage(List<PayOrderBaseInfoVO> list) {
		List<ChaseOrderCreateMsgModel> list2 = new ArrayList<ChaseOrderCreateMsgModel>();
		ChaseOrderCreateMsgModel chaseOrderCreateMsgModel = null;
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : list) {
			if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				chaseOrderCreateMsgModel = new ChaseOrderCreateMsgModel();
				chaseOrderCreateMsgModel.setLotteryCode(payOrderBaseInfoVO.getLotteryCode());
				chaseOrderCreateMsgModel.setOrderAddCode(payOrderBaseInfoVO.getOrderCode());
				chaseOrderCreateMsgModel.setUserId(Long.parseLong(String.valueOf(payOrderBaseInfoVO.getUserId())));
				list2.add(chaseOrderCreateMsgModel);
			}
		}
		if (!ObjectUtil.isBlank(list2)) {
			sendMessage(Constants.CHASE_ORDER_CREATE_QUEUE, JSON.toJSONString(list2));
		}
	}

	@Override
	public void sendRechargeMessage(Integer userId, String rechargeCode) {
		// 发送现金充值消息
		MessageModel messageModel = new MessageModel();
		messageModel.setKey(Constants.MSG_NODE_RESEND);
		messageModel.setMessageSource(MESSAGE_SOURCE);
		OperateNodeMsg operateNodeMsg = new OperateNodeMsg();
		operateNodeMsg.setNodeId(4);
		operateNodeMsg.setNodeData(userId + ";" + rechargeCode);// 用户ID;充值交易号
		messageModel.setMessage(operateNodeMsg);
		sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, messageModel);
	}

	@Override
	public void sendRedMessage(Integer userId, String redCode) {
		// 发送红包充值消息
		MessageModel messageModel = new MessageModel();
		messageModel.setKey(Constants.MSG_NODE_RESEND);
		messageModel.setMessageSource(MESSAGE_SOURCE);
		OperateNodeMsg colorRedMsg = new OperateNodeMsg();
		colorRedMsg.setNodeId(2);
		colorRedMsg.setNodeData(userId + ";" + redCode);// 用户ID;红包编号
		messageModel.setMessage(colorRedMsg);
		sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, colorRedMsg);
	}

	@Override
	public void sendRechargeActivityMessage(String activityCode, String transCode, String activityPage) {
		// 发送充值活动消息
		ActivityMessageModel activityMessageModel = new ActivityMessageModel();
		activityMessageModel.setMessageSource(MESSAGE_SOURCE);
		activityMessageModel.setType(3);// 1:派奖后触发2:开奖后触发3:充值触发
		MsgVO msgVO = new MsgVO();
		msgVO.setActivityCode(activityCode);
		msgVO.setTransId(transCode);
		msgVO.setActivityPage(activityPage);
		activityMessageModel.setMessage(msgVO);
		sendMessage(Constants.QUEUE_ACTIVITY_CHANNEL_QUEUENAME, activityMessageModel);
	}

	@Override
	public void sendAlarmMessage(List<PayOrderBaseInfoVO> alarmList) {
		for (PayOrderBaseInfoVO info : alarmList) {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("alarmChild", "3");// 大额充值
			jsonObject.put("alarmInfo", info.getLotteryName() + "" + info.getOrderCode() + "金额为" + info.getOrderAmount() + "，请注意");// {双色球}{订单号}金额为20000，请注意
			jsonObject.put("alarmLevel", "1");// 0 低级、 1 一般、2 最高
			jsonObject.put("alarmType", "1");// 1业务报警、2 系统报警
			jsonObject.put("remark", "lotto-pay");
			jsonObject.put("alarmTime", DateUtil.getNow());
			sendMessage(Constants.QUEUE_NAME_ALERM_INFO, jsonObject.toJSONString());
			// sendMessage("alarm_info_test", jsonObject.toJSONString());

		}
	}

	@Override
	public void sendAutoCheckMessage(String takenCode) {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("alarmChild", "41");// 大额充值
		jsonObject.put("alarmInfo", "提款流水" + takenCode + "自动审核异常，请注意");
		jsonObject.put("alarmLevel", "1");// 0 低级、 1 一般、2 最高
		jsonObject.put("alarmType", "1");// 1业务报警、2 系统报警
		jsonObject.put("remark", "lotto-pay");
		jsonObject.put("alarmTime", DateUtil.getNow());
		sendMessage(Constants.QUEUE_NAME_ALERM_INFO, jsonObject.toJSONString());
	}
}