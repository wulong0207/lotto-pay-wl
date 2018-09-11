package com.hhly.paycore.jms;

import java.util.Set;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.hhly.paycore.jms.DelayService.OnDelayedListener;
import com.hhly.paycore.remote.service.IPayService;
import com.hhly.paycore.service.TaskService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.utils.RedisLock;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.ThreadPoolUtil;

@Component("StartupListener") // 用注解就不需要在配置文件中添加注入
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {
	private static final Logger logger = Logger.getLogger(StartupListener.class);
	private static final String KEY_PREFIX = "delay:recharge:";

	@Resource
	private IPayService payService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private StringRedisTemplate strRedisTemplate;
	@Resource
	DelayService delayService;
	@Resource
	private TaskService taskService;
	@Resource
	private RedisUtil redisUtil;
//	@Resource(name = "scanExecutor")
//	private TaskExecutor scanExecutor;
	@Value("${delayQueue.isopen}")
	private String openDelayQueue;// true 打开delayQueue延时关闭，false关闭

	@Override
	public void onApplicationEvent(ContextRefreshedEvent evt) {
		// 避免onApplicationEvent方法被执行两次。root application context 没有parent，他就是老大.

		Boolean isOpenDelayQueue = Boolean.parseBoolean(openDelayQueue);
		if (evt.getApplicationContext().getParent() == null && isOpenDelayQueue) {
			logger.info(">>>>>>>>>>>>系统启动完成，onApplicationEvent()");
			// 自动关闭订单，到时间的充值记录会监听到
			delayService.start(new OnDelayedListener() {
				@Override
				public void onDelayedArrived(final CloseRechargeDelay order) {
					if (!ObjectUtil.isBlank(order)) {
						process(order);
					}
				}
			});
			// 查找需要入队的充值流水
			ThreadPoolUtil.execute(new Runnable() {
				@Override
				public void run() {
					// 扫描redis，找到所有可能的orderId
					Set<String> keys = redisUtil.keys(CacheConstants.P_CORE_RECHARGE_ORDER);
					if (ObjectUtil.isBlank(keys)) {
						return;
					}
					logger.info("需要入队的订单keys：" + keys);
					// 写到DelayQueue
					for (String key : keys) {
						CloseRechargeDelay order = redisUtil.getObj(key, new CloseRechargeDelay());
						logger.info("redis中，充值中的key：" + key);
						// 对象不为空，并且第一次启动添加，则继续添加
						if (!ObjectUtil.isBlank(order)) {
							delayService.add(order);
							logger.info("系统启动，未关闭充值流水【" + order.getRechargeCode() + "】再次入延时消费队列：");
							// 更新redis中支付中的充值记录
							order.setIsAdd((short) 1);
							redisUtil.addObj(CacheConstants.P_CORE_RECHARGE_ORDER + order.getRechargeCode(), order, CacheConstants.TWO_HOURS);
						}
					}
				}
			});
		}

	}

	/**  
	* 方法说明: 交易记录公共处理，引入分布式锁
	* @auth: xiongJinGang
	* @param order
	* @time: 2017年8月1日 下午3:59:25
	* @return: void 
	*/
	private void process(CloseRechargeDelay order) {
		String rechargeCode = order.getRechargeCode();
		String key = KEY_PREFIX + "process";
		RedisLock lock = new RedisLock(strRedisTemplate, key, 10000, 20000);
		try {
			if (lock.lock()) {
				logger.debug("【" + rechargeCode + "】进入锁【" + key + "】");
				TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(rechargeCode);
				if (!ObjectUtil.isBlank(transRechargeBO)) {
					// 交易状态为进行中的记录，才需要更新状态
					if (transRechargeBO.getTransStatus().equals(PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey())) {
						payService.modifyCloseOrder(transRechargeBO);
						logger.debug("充值编号【" + rechargeCode + "】处理完成");
						// 先不使用多线程，用上面单线程关单
						// scanExecutor.execute(new RechargeCloseTask(transRechargeBO, payService, transRechargeService));
					} else {
						redisUtil.delObj(CacheConstants.P_CORE_RECHARGE_ORDER + rechargeCode);
						logger.debug("充值【" + rechargeCode + "】编号不是进行中，不处理");
					}
				} else {
					logger.info("未获取到充值【" + rechargeCode + "】记录");
				}
			}
		} catch (Exception e) {
			logger.error("交易号【" + rechargeCode + "】自动关闭异常！", e);
		} finally {
			logger.debug("释放锁【" + key + "】");
			// 为了让分布式锁的算法更稳键些，持有锁的客户端在解锁之前应该再检查一次自己的锁是否已经超时，再去做DEL操作，因为可能客户端因为某个耗时的操作而挂起，
			// 操作完的时候锁因为超时已经被别人获得，这时就不必解锁了。 ————这里没有做
			lock.unlock();
		}
	}

	// 异步关闭充值中的交易
	/*private static class RechargeCloseTask implements Runnable {
		private final TransRechargeBO transRecharge;
		private final IPayService payService;
		private final ITransRechargeService transRechargeService;
	
		private RechargeCloseTask(TransRechargeBO transRecharge, IPayService payService, ITransRechargeService transRechargeService) {
			this.transRecharge = transRecharge;
			this.payService = payService;
			this.transRechargeService = transRechargeService;
		}
	
		@Override
		public void run() {
			try {
				PayQueryParamVO payQueryParamVO = new PayQueryParamVO();
				payQueryParamVO.setTransCode(transRecharge.getTransRechargeCode());
				payQueryParamVO.setRechargeChannel(transRecharge.getRechargeChannel());
				ResultBO<?> resultBO = payService.payQuery(payQueryParamVO);
				// 未获取到结果或者失败，改为已过期
				if (resultBO.isError()) {
					if (resultBO.getErrorCode().equals(MessageCodeConstants.NO_PAY_OVERDUE)) {
						try {
							TransRechargePO transRechargePO = new TransRechargePO();
							transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_CLOSED.getKey());// 设置成已关闭
							transRechargePO.setModifyBy(Constants.SYSTEM_OPERATE);// 系统操作
							transRechargePO.setId(transRecharge.getId());
							int num = transRechargeService.updateRecharge(transRechargePO);
							if (num <= 0) {
								logger.info("系统将交易号【" + transRecharge.getTransRechargeCode() + "】状态设置成已关闭失败。");
							}
						} catch (Exception e) {
							logger.error("系统将交易号【" + transRecharge.getTransRechargeCode() + "】状态设置成已关闭异常", e);
						}
					}
				} else {
					// 支付成功，判断支付结果是否为未支付或者支付过期
					PayQueryResultVO payQueryResultVO = (PayQueryResultVO) resultBO.getData();
					if (!ObjectUtil.isBlank(payQueryResultVO)) {
						if (payQueryResultVO.getTradeStatus().equals(PayConstants.PayStatusEnum.OVERDUE_PAYMENT) || payQueryResultVO.getTradeStatus().equals(PayConstants.PayStatusEnum.USER_CANCELLED_PAYMENT)) {
							try {
								TransRechargePO transRechargePO = new TransRechargePO();
								transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_CLOSED.getKey());// 设置成已关闭
								transRechargePO.setModifyBy(Constants.SYSTEM_OPERATE);// 系统操作
								transRechargePO.setId(transRecharge.getId());
								int num = transRechargeService.updateRecharge(transRechargePO);
								if (num <= 0) {
									logger.info("系统将交易号【" + transRecharge.getTransRechargeCode() + "】状态设置成已关闭失败。");
								}
							} catch (Exception e) {
								logger.error("系统将交易号【" + transRecharge.getTransRechargeCode() + "】状态设置成已关闭异常", e);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}*/

}
