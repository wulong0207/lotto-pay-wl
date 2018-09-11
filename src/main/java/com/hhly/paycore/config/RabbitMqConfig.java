package com.hhly.paycore.config;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.hhly.paycore.service.CancellationRefundOrderGroupService;
import com.hhly.paycore.service.CancellationRefundService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;
import com.hhly.skeleton.base.util.JsonUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.rabbitmq.client.Channel;

/**
 * @desc RabbitMq配置类
 * @author wulong
 * @date 2018年7月26日
 * @company 益彩网络
 * @version v1.0
 */
@Configuration
public class RabbitMqConfig {

	@Value("${spring.rabbitmq.host}")
	private String ADDRESSES;
	@Value("${spring.rabbitmq.username}")
	private String USER_NAME;
	@Value("${spring.rabbitmq.password}")
	private String PASS_WORD;
	@Value("${spring.rabbitmq.port}")
	private String PORT;
	@Value("${spring.rabbitmq.virtualHost}")
	private String VIRTUAL_HOST;
	@Value("${order_cancel_queuename}")
	private String QUEUENAME;
	
	private Logger logger = LoggerFactory.getLogger(RabbitMqConfig.class);
	
	
	@Bean
	public ConnectionFactory connectionFactory() {
		CachingConnectionFactory connectionFactory = new CachingConnectionFactory(ADDRESSES,Integer.valueOf(PORT));
		connectionFactory.setUsername(USER_NAME);
		connectionFactory.setPassword(PASS_WORD);
		connectionFactory.setVirtualHost(VIRTUAL_HOST);
		connectionFactory.setPublisherConfirms(true); //必须要设置
		return connectionFactory;
	}
	
	@Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    //必须是prototype类型
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate template = new RabbitTemplate(connectionFactory());
        return template;
    }
	

    /**
     * 配置消息交换机
     * 针对消费者配置
     * FanoutExchange: 将消息分发到所有的绑定队列，无routingkey的概念
     * HeadersExchange ：通过添加属性key-value匹配
     * DirectExchange:按照routingkey分发到指定队列
     * TopicExchange:多关键字匹配
     */
    @Bean
    public DirectExchange defaultExchange() {
    	Map<String,Object> args = new HashMap<String, Object>();  
        args.put("x-max-priority",10); //队列的属性参数 有10个优先级别
        return new DirectExchange(null, true, false, args);
    }
	
	@Bean
    public Queue queue() {
        return new Queue(QUEUENAME, true); //队列持久
    }
	
	/**
     * 将消息队列1与交换机绑定
     * 针对消费者配置
     *
     * @return
     */
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(defaultExchange()).with(QUEUENAME);
    }
    
    @Autowired
	private CancellationRefundService cancellationRefundService;
	@Resource
	private CancellationRefundOrderGroupService cancellationRefundOrderGroupService;

	@Bean
    public SimpleMessageListenerContainer messageContainer() {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory());
        container.setQueues(queue());
        container.setExposeListenerChannel(true);
        container.setMaxConcurrentConsumers(1);
        container.setConcurrentConsumers(1);
        container.setAcknowledgeMode(AcknowledgeMode.MANUAL); //设置确认模式手工确认
        container.setMessageListener(new ChannelAwareMessageListener() {
 
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
            	try {
                	if (ObjectUtil.isBlank(message)) {
            			logger.error("接收【用户撤单退款消息】请求参数为空，不处理！");
            			return;
            		}
            		OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
            		String msg = null;
            		msg = new String(message.getBody(), "UTF-8");
            		logger.info("收到撤单消息【" + msg + "】");
            		cancellationRefundBO = (OrderCancelMsgModel) JsonUtil.json2Object(msg, OrderCancelMsgModel.class);
            		ResultBO<?> resultBO = null;
            		if (cancellationRefundBO.getOrderType().intValue() != CancellationConstants.OrderTypeEnum.ORDERGROUP.getKey().intValue()) {
            			// 合买退款类型为空，走正常流程退款
            			resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
            		} else {
            			// 合买退款类型不为空，走合买流程退款
            			resultBO = cancellationRefundOrderGroupService.doCancellation(cancellationRefundBO);
            		}
            		logger.info("执行撤单返回【" + resultBO.getMessage() + "】");
                } catch (Exception e) {
                    logger.error("接收【用户撤单退款消息】异常：" + e.getMessage(), e);
                }finally{
                	//消息的标识，false只确认当前一个消息收到，true确认所有consumer获得的消息
        			channel.basicAck(message.getMessageProperties().getDeliveryTag(), false); 
                	//内容格式(1:支付完成,2:完成当前期追号订单,格式:订单编号1,订单编号2#1)
                	//渠道来源(1:支付完成,2:完成当前期追号订单,3:未知来源)
                }
            }
        });
        return container;
    }
}
