package com.hhly.paycore.common;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.UnifiedPayService;
import com.hhly.skeleton.base.constants.PayConstants.ChannelTypeEnum;

public class PayCommon {
	private static final Logger logger = Logger.getLogger(PayCommon.class);
	public final Map<String, UnifiedPayService> realPayServices;

	public PayCommon() {
		this.realPayServices = new HashMap<>();
		for (ChannelTypeEnum channel : ChannelTypeEnum.values()) {
			if (!realPayServices.containsKey(channel.getChannel())) {
				try {
					UnifiedPayService service = (UnifiedPayService) Class.forName(channel.getClazz()).newInstance();
					realPayServices.put(channel.getChannel(), service);
				} catch (Exception e) {
					logger.error("instance " + channel.getClazz() + " error.", e);
				}
			}
		}
	}
}
