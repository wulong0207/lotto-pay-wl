package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayChannelMapper;
import com.hhly.paycore.service.PayChannelLimitService;
import com.hhly.paycore.service.PayChannelMgrService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelLimitBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelMgrBO;
import com.hhly.skeleton.pay.channel.vo.PayChannelVO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 支付渠道
 * @author xiongJinGang
 * @date 2017年12月12日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service
public class PayChannelServiceImpl implements PayChannelService {

	@Resource
	private PayChannelMapper payChannelDaoMapper;
	@Resource
	private PayChannelMgrService payChannelMgrService;// 支付渠道管理
	@Resource
	private PayChannelLimitService payChannelLimitService;// 支付渠道限额
	@Resource
	private RedisUtil redisUtil;

	/**
	 * 获取所有支付渠道
	 *
	 * @return
	 */
	@Override
	public List<PayChannelBO> finAllChannel() {
		return payChannelDaoMapper.selectAll();
	}

	/**
	 * 根据条件查询
	 *
	 * @param payChannelVO 条件对象
	 * @return List<PayChannelPO> 结果集
	 */
	@Override
	public List<PayChannelBO> selectByCondition(PayChannelVO payChannelVO) {
		return payChannelDaoMapper.selectByCondition(payChannelVO);
	}

	@Override
	public List<PayChannelBO> findChannelByBankIdUseCache(Integer bankId) {
		String key = CacheConstants.P_CORE_PAY_BANK_CHANNEL_SINGLE + "bank_" + bankId;
		List<PayChannelBO> list = redisUtil.getObj(key, new ArrayList<PayChannelBO>());
		if (ObjectUtil.isBlank(list)) {
			// 已经根据order_id进行升序排序了
			list = payChannelDaoMapper.getChannelByBankId(bankId);
			if (!ObjectUtil.isBlank(list)) {
				for (PayChannelBO payChannelBO : list) {
					// 获取单日最低、最高限额
					PayChannelMgrBO payChannelMgrBO = payChannelMgrService.findChannelMgrById(payChannelBO.getPayChannelMgrId());
					if (!ObjectUtil.isBlank(payChannelMgrBO)) {
						payChannelBO.setMinPay(payChannelMgrBO.getMinPay());
						payChannelBO.setMaxPay(payChannelMgrBO.getMaxPay());
					}
					// 获取支付渠道的限额
					Map<String, PayChannelLimitBO> limitMap = payChannelLimitService.findSingleChannelLimit(payChannelBO.getPayChannelMgrId());
					payChannelBO.setLimitMap(limitMap);
				}
			}
			redisUtil.addObj(key, list, CacheConstants.ONE_WEEK);// 存一天
		}
		return list;
	}

	@Override
	public PayChannelBO findChannelByIdUseCache(Integer id) {
		String key = CacheConstants.P_CORE_PAY_BANK_CHANNEL_SINGLE + "channel_" + id;
		PayChannelBO payChannelBO = redisUtil.getObj(key, new PayChannelBO());
		if (ObjectUtil.isBlank(payChannelBO)) {
			payChannelBO = payChannelDaoMapper.getChannelById(id);
			if (!ObjectUtil.isBlank(payChannelBO)) {
				redisUtil.addObj(key, payChannelBO, CacheConstants.ONE_WEEK);// 存一天
			}
		}
		return payChannelBO;
	}

}
