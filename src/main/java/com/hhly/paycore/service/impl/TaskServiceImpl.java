package com.hhly.paycore.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.remote.service.IPayService;
import com.hhly.paycore.service.TaskService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 任务关单
 * @author xiongJinGang
 * @date 2017年8月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service
public class TaskServiceImpl implements TaskService {
	private static Logger logger = LoggerFactory.getLogger(TaskServiceImpl.class);
	private static final int SHOW_COUNT = 100;// 一次最多查询100条

	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private IPayService payService;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public ResultBO<?> closeRechargeStatus() {
		List<TransRechargeBO> list = null;
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("tradeStatus", PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey());// 过了交易结束时间，还是进行中的
		map.put("showCount", SHOW_COUNT);// 一次最多查询多少条
		map.put("endSaleDate", DateUtil.getNowDate());// 交易结束时间

		logger.debug("查询需要关闭充值状态的条件：" + map.toString());
		// 获取超过支付截止时间，还没有改变支付状态的充值记录
		list = transRechargeService.findRechargeByParam(map);
		if (!ObjectUtil.isBlank(list)) {
			for (TransRechargeBO transRecharge : list) {
				payService.modifyCloseOrder(transRecharge);
			}
		}
		return ResultBO.ok();
	}

}
