package com.hhly.paycore.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.OrderGroupContentMapper;
import com.hhly.paycore.service.OrderGroupContentService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.utils.RedisUtil;

@Service("orderGroupContentService")
public class OrderGroupContentServiceImpl implements OrderGroupContentService {

	@Resource
	private OrderGroupContentMapper orderGroupContentMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public List<OrderGroupContentBO> findOrderGroupContentByOrderCode(String orderCode) {
		return orderGroupContentMapper.getOrderGroupContentByOrderCode(orderCode);
	}

	@Override
	public OrderGroupContentBO findOrderGroupContentByBuyCodeFromCache(String buyCode) {
		String key = CacheConstants.P_CORE_ORDER_GROUP_CONTENT + buyCode;
		OrderGroupContentBO orderGroupContentBO = redisUtil.getObj(key, OrderGroupContentBO.class);
		if (ObjectUtil.isBlank(orderGroupContentBO)) {
			orderGroupContentBO = orderGroupContentMapper.getOrderGroupContentByBuyCode(buyCode);
			if (!ObjectUtil.isBlank(orderGroupContentBO)) {
				redisUtil.addObj(key, orderGroupContentBO, CacheConstants.TWO_HOURS);
			}
		}
		return orderGroupContentBO;
	}

	@Override
	public int updateOrderGroupContentStatus(String orderCode, Short status) {
		Map<String, Object> map = new HashMap<>();
		map.put("buyCode", orderCode);
		map.put("refundStatus", status);
		return orderGroupContentMapper.updateOrderGroupContentStatus(map);
	}

}
