package com.hhly.paycore.jms;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

import com.hhly.paycore.service.SendPrizeService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.util.ObjectUtil;

@Component("orderSendPrizeMessageListener")
public class OrderSendPrizeMessageListener implements MessageListener {

	private Logger logger = LoggerFactory.getLogger(OrderSendPrizeMessageListener.class);

	@Resource(name = "sendPrizeService")
	private SendPrizeService sendPrizeService;

	@Override
	public void onMessage(Message message) {
		if (ObjectUtil.isBlank(message)) {
			logger.error("接收【用户派奖修改账户信息消息】请求参数为空，不处理！");
			return;
		}
		String msg = "";
		try {
			msg = new String(message.getBody(), "UTF-8");
			logger.info("收到派奖消息【" + msg + "】");
			ResultBO<?> resultBO = sendPrizeService.updateSendPrize(msg);
			logger.info("执行派奖返回【" + resultBO.getMessage() + "】");
		} catch (Exception e) {
			logger.error("接收【用户派奖修改账户信息消息】异常，orderCode=" + msg, e);
		}
	}

}
