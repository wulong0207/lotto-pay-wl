package com.hhly.paycore.jms;

import java.util.concurrent.DelayQueue;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.skeleton.base.constants.Constants;
import com.hhly.utils.ThreadPoolUtil;

@Service
public class DelayService {
	private static final Logger log = Logger.getLogger(DelayService.class);

	private boolean start;
	private OnDelayedListener listener;
	private DelayQueue<CloseRechargeDelay> delayQueue = new DelayQueue<CloseRechargeDelay>();

	public static interface OnDelayedListener {
		public void onDelayedArrived(CloseRechargeDelay order);
	}

	public void start(OnDelayedListener listener) {
		if (start) {
			return;
		}
		log.info("延时处理DelayService 启动");
		start = true;
		this.listener = listener;
		ThreadPoolUtil.execute(new Runnable() {
			@Override
			public void run() {
				try {
					while (true) {
						CloseRechargeDelay order = delayQueue.take();
						if (DelayService.this.listener != null) {
							DelayService.this.listener.onDelayedArrived(order);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void add(CloseRechargeDelay order) {
		delayQueue.put(order);
	}

	public boolean remove(CloseRechargeDelay order) {
		return delayQueue.remove(order);
	}

	public void add(String rechargeCode) {
		delayQueue.put(new CloseRechargeDelay(rechargeCode, Constants.RECHARGE_EFFECTIVE_TIME));
	}

	public void remove(String rechargeCode) {
		CloseRechargeDelay[] array = delayQueue.toArray(new CloseRechargeDelay[] {});
		if (array == null || array.length <= 0) {
			return;
		}
		CloseRechargeDelay target = null;
		for (CloseRechargeDelay recharge : array) {
			if (recharge.getRechargeCode().equals(rechargeCode)) {
				target = recharge;
				break;
			}
		}
		if (target != null) {
			delayQueue.remove(target);
		}
	}
}
