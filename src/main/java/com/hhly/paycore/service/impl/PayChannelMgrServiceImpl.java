package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayChannelMgrMapper;
import com.hhly.paycore.service.PayChannelMgrService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.channel.bo.PayChannelMgrBO;
import com.hhly.skeleton.pay.channel.vo.ChannelMgrVO;
import com.hhly.utils.RedisUtil;

@Service("payChannelMgrService")
public class PayChannelMgrServiceImpl implements PayChannelMgrService {
	private static final Logger logger = Logger.getLogger(PayChannelMgrServiceImpl.class);
	@Resource
	private PayChannelMgrMapper payChannelMgrMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public List<PayChannelMgrBO> findChannelMgrList() {
		return payChannelMgrMapper.getAll();
	}

	@Override
	public List<PayChannelMgrBO> findChannelMgrListFromCache() {
		String key = CacheConstants.P_CORE_PAY_CHANNEL_MGR_LIST;
		List<PayChannelMgrBO> list = redisUtil.getObj(key, new ArrayList<PayChannelMgrBO>());
		if (ObjectUtil.isBlank(list)) {
			list = findChannelMgrList();
			if (!ObjectUtil.isBlank(list)) {
				redisUtil.addObj(key, list, CacheConstants.ONE_WEEK);
			}
		}
		return list;
	}

	@Override
	public PayChannelMgrBO findChannelMgrByCode(String code) {
		String key = CacheConstants.P_CORE_PAY_CHANNEL_MGR_LIST + "_" + code;
		PayChannelMgrBO payChannelMgrBO = redisUtil.getObj(key, new PayChannelMgrBO());
		if (ObjectUtil.isBlank(payChannelMgrBO)) {
			List<PayChannelMgrBO> list = findChannelMgrListFromCache();
			if (!ObjectUtil.isBlank(list)) {
				for (PayChannelMgrBO payChannelMgr : list) {
					if (payChannelMgr.getCode().equals(code)) {
						redisUtil.addObj(key, payChannelMgr, CacheConstants.ONE_WEEK);
						return payChannelMgr;
					}
				}
			}
		}
		return payChannelMgrBO;
	}

	@Override
	public PayChannelMgrBO findChannelMgrById(Integer mgrId) {
		String key = CacheConstants.P_CORE_PAY_CHANNEL_MGR_LIST + "_" + mgrId;
		PayChannelMgrBO payChannelMgrBO = redisUtil.getObj(key, new PayChannelMgrBO());
		if (ObjectUtil.isBlank(payChannelMgrBO)) {
			payChannelMgrBO = payChannelMgrMapper.selectById(mgrId);
			if (!ObjectUtil.isBlank(payChannelMgrBO)) {
				redisUtil.addObj(key, payChannelMgrBO, CacheConstants.ONE_WEEK);
			}
		}
		return payChannelMgrBO;
	}

	@Override
	public ChannelMgrVO findInUseChannel(Integer mgrId) {
		String keyEnd = ObjectUtil.isBlank(mgrId) ? "" : String.valueOf(mgrId);
		String key = CacheConstants.P_CORE_PAY_CHANNEL_MGR_LIST + "_USING_" + keyEnd;
		ChannelMgrVO channelMgrVO = redisUtil.getObj(key, new ChannelMgrVO());
		if (ObjectUtil.isBlank(channelMgrVO)) {
			List<PayChannelMgrBO> mgrList = findChannelMgrListFromCache();
			List<Integer> idList = new ArrayList<Integer>();
			Iterator<PayChannelMgrBO> iterator = mgrList.iterator();
			Date nowDate = DateUtil.getNowDate();// 当前时间
			Double leastAmount = 0d;// 单笔最低限额
			Double highestAmount = 0d;// 单笔最高限额
			while (iterator.hasNext()) {
				PayChannelMgrBO payChannelMgrBO = iterator.next();
				if (payChannelMgrBO.getAvailable()) {// 判断支付渠道是否可用
					iterator.remove();
				}
				// 是否启用了渠道暂停设置，并且当前时间在维护时间内，过滤掉
				if (payChannelMgrBO.getPause() && !ObjectUtil.isBlank(payChannelMgrBO.getBeginTime()) && !ObjectUtil.isBlank(payChannelMgrBO.getEndTime())) {
					int bigBigin = DateUtil.compare(nowDate, payChannelMgrBO.getBeginTime());//
					int bigEnd = DateUtil.compare(nowDate, payChannelMgrBO.getEndTime());
					// 当前时间在暂停使用时间内
					if (bigBigin >= 0 && bigEnd <= 0) {
						iterator.remove();
					}
				}
				// 获取最低支付金额
				if (MathUtil.compareTo(leastAmount, 0) == 0 || MathUtil.compareTo(payChannelMgrBO.getMinPay(), leastAmount) < 0) {
					leastAmount = payChannelMgrBO.getMinPay();
				}
				// 获取最高支付金额
				if (MathUtil.compareTo(highestAmount, 0) == 0 || MathUtil.compareTo(payChannelMgrBO.getMaxPay(), highestAmount) > 0) {
					highestAmount = payChannelMgrBO.getMaxPay();
				}
				idList.add(payChannelMgrBO.getId());
				// 渠道管理ID不为空
				if (!ObjectUtil.isBlank(mgrId)) {
					if (!payChannelMgrBO.getId().equals(mgrId)) {
						break;
					}
				}
			}
			channelMgrVO = new ChannelMgrVO(idList, mgrList, leastAmount, highestAmount);
			if (!ObjectUtil.isBlank(channelMgrVO)) {
				redisUtil.addObj(key, channelMgrVO, CacheConstants.ONE_WEEK);
			}
		}
		return channelMgrVO;
	}

	@Override
	public ResultBO<?> validateChannel(Double payAmount, Short transType) {
		// 验证是否存在可用渠道
		ChannelMgrVO channelMgr = findInUseChannel(null);
		// 渠道为空，表示无可用渠道
		if (ObjectUtil.isBlank(channelMgr.getMgrList())) {
			logger.info("判断支付渠道是否可用，是否维护，使用平台，没有可用的支付渠道");
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}
		String transTypeName = PayConstants.RechargeTypeEnum.getEnum(transType);
		// 判断渠道的单笔支付限额
		if (MathUtil.compareTo(payAmount, channelMgr.getLeastAmount()) < 0) {
			logger.info(transTypeName + "金额：" + payAmount + "小于渠道最低交易金额：" + channelMgr.getLeastAmount());
			return ResultBO.err(MessageCodeConstants.PAY_MONEY_LESS_THAN_LIMIT, transTypeName, channelMgr.getLeastAmount());
		}
		if (MathUtil.compareTo(payAmount, channelMgr.getHighestAmount()) > 0) {
			logger.info(transTypeName + "金额：" + payAmount + "高于渠道最高交易金额：" + channelMgr.getLeastAmount());
			return ResultBO.err(MessageCodeConstants.PAY_MONEY_HIGHER_THAN_LIMIT, transTypeName, channelMgr.getHighestAmount());
		}
		return ResultBO.ok(channelMgr);
	}
}
