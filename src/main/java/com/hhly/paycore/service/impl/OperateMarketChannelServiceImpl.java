package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.OperateMarketChannelMapper;
import com.hhly.paycore.service.OperateMarketChannelService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.channel.bo.OperateMarketChannelBO;
import com.hhly.utils.RedisUtil;

@Service("operateMarketChannelService")
public class OperateMarketChannelServiceImpl implements OperateMarketChannelService {
	@Resource
	private OperateMarketChannelMapper operateMarketChannelMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public List<OperateMarketChannelBO> findMarketListFromCache() {
		String key = CacheConstants.P_CORE_OPERATE_MARKET_CHANNEL_LIST;
		List<OperateMarketChannelBO> list = redisUtil.getObj(key, new ArrayList<OperateMarketChannelBO>());
		if (ObjectUtil.isBlank(list)) {
			list = operateMarketChannelMapper.getList();
			if (!ObjectUtil.isBlank(list)) {
				redisUtil.addObj(key, list, CacheConstants.ONE_MONTH);
			}
		}
		return list;
	}

	@Override
	public OperateMarketChannelBO findSingleMarket(String channelId) {
		String key = CacheConstants.P_CORE_OPERATE_MARKET_CHANNEL_LIST + channelId;
		OperateMarketChannelBO operateMarketChannelBO = redisUtil.getObj(key, new OperateMarketChannelBO());
		if (ObjectUtil.isBlank(operateMarketChannelBO)) {
			List<OperateMarketChannelBO> list = operateMarketChannelMapper.getList();
			if (!ObjectUtil.isBlank(list)) {
				for (OperateMarketChannelBO operateMarketChannelBO2 : list) {
					if (operateMarketChannelBO2.getChannelId().equals(channelId)) {
						redisUtil.addObj(key, operateMarketChannelBO2, CacheConstants.ONE_MONTH);
						return operateMarketChannelBO2;
					}
				}
			}
		}
		return operateMarketChannelBO;
	}

	@Override
	public boolean isMajia(String channelId) {
		boolean isMajia = false;
		OperateMarketChannelBO operateMarketChannelBO = findSingleMarket(channelId);
		// 如果是马甲包，返回True
		if (!ObjectUtil.isBlank(operateMarketChannelBO) && operateMarketChannelBO.getMajia().equals(PayConstants.BindFlagEnum.TRUE.getKey())) {
			isMajia = true;
		}
		return isMajia;
	}

}
