package com.hhly.paycore.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayChannelLimitMapper;
import com.hhly.paycore.service.PayChannelLimitService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.channel.bo.PayChannelLimitBO;
import com.hhly.utils.RedisUtil;

@Service("payChannelLimitService")
public class PayChannelLimitServiceImpl implements PayChannelLimitService {

	@Resource
	private PayChannelLimitMapper payChannelLimitMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public List<PayChannelLimitBO> findChannelList() {
		return payChannelLimitMapper.getAll();
	}

	// 暂时没用这个方法
	@SuppressWarnings("unused")
	private Map<Integer, Map<String, PayChannelLimitBO>> findChannelListFromCache() {
		String key = CacheConstants.P_CORE_PAY_CHANNEL_LIMIT_LIST;
		Map<Integer, Map<String, PayChannelLimitBO>> cacheMap = redisUtil.getObj(key, new HashMap<Integer, Map<String, PayChannelLimitBO>>());
		if (ObjectUtil.isBlank(cacheMap)) {
			cacheMap = new HashMap<Integer, Map<String, PayChannelLimitBO>>();
			List<PayChannelLimitBO> list = findChannelList();
			Map<String, PayChannelLimitBO> sameMap = null;

			for (PayChannelLimitBO payChannelLimitBO : list) {
				Integer mapKey = payChannelLimitBO.getPayChannelMgrId();
				String sameKey = payChannelLimitBO.getPayType() + "_" + payChannelLimitBO.getCardType();
				if (cacheMap.containsKey(mapKey)) {
					sameMap = cacheMap.get(mapKey);
					sameMap.put(sameKey, payChannelLimitBO);
				} else {
					sameMap = new HashMap<String, PayChannelLimitBO>();
					sameMap.put(sameKey, payChannelLimitBO);
				}
				cacheMap.put(mapKey, sameMap);
			}
			if (!ObjectUtil.isBlank(cacheMap)) {
				redisUtil.addObj(key, cacheMap, CacheConstants.ONE_WEEK);// 渠道支付限额，保存一周
			}
		}
		return cacheMap;
	}

	@Override
	public Map<String, PayChannelLimitBO> findSingleChannelLimit(Integer payChannelMgrId) {
		String key = CacheConstants.P_CORE_PAY_CHANNEL_LIMIT_LIST + "_" + payChannelMgrId;
		Map<String, PayChannelLimitBO> map = redisUtil.getObj(key, new HashMap<String, PayChannelLimitBO>());
		if (ObjectUtil.isBlank(map)) {
			List<PayChannelLimitBO> list = payChannelLimitMapper.selectById(payChannelMgrId);
			map = new HashMap<String, PayChannelLimitBO>();
			if (!ObjectUtil.isBlank(list)) {
				for (PayChannelLimitBO payChannelLimitBO : list) {
					String mapKey = payChannelLimitBO.getPayType() + "_" + payChannelLimitBO.getCardType();
					map.put(mapKey, payChannelLimitBO);
				}
			}
			redisUtil.addObj(key, map, CacheConstants.ONE_WEEK);// 渠道支付限额，保存一周
		}
		return map;

	}

}
