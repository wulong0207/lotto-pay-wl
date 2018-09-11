package com.hhly.paycore.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayBankLimitDaoMapper;
import com.hhly.paycore.service.PayBankLimitService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PayBankLimitBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 银行支付限额
 * @author xiongJinGang
 * @date 2017年6月21日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("payBankLimitService")
public class PayBankLimitServiceImpl implements PayBankLimitService {
	private static Logger logger = Logger.getLogger(PayBankLimitService.class);

	@Autowired
	private PayBankLimitDaoMapper payBankLimitDaoMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public PayBankLimitBO findPayBankLimitByBankIdFromCache(Integer bankId, Short bankType) {
		PayBankLimitBO payBankLimitBO = null;
		if (ObjectUtil.isBlank(bankType)) {
			String key = CacheConstants.P_CORE_PAY_BANK_LIMIT_SINGLE + bankId;
			payBankLimitBO = redisUtil.getObj(key, new PayBankLimitBO());
			if (ObjectUtil.isBlank(payBankLimitBO)) {
				List<PayBankLimitBO> list = payBankLimitDaoMapper.getPayBankLimitByBankId(bankId);
				if (!ObjectUtil.isBlank(list)) {
					payBankLimitBO = list.get(0);
					redisUtil.addObj(key, payBankLimitBO, CacheConstants.TWO_HOURS);
				}
			}
		} else {
			String key = CacheConstants.P_CORE_PAY_BANK_LIMIT_SINGLE + bankId + "_" + bankType;
			payBankLimitBO = redisUtil.getObj(key, new PayBankLimitBO());
			if (ObjectUtil.isBlank(payBankLimitBO)) {
				payBankLimitBO = payBankLimitDaoMapper.getByBankIdAndType(bankId, bankType);
				if (!ObjectUtil.isBlank(payBankLimitBO)) {
					redisUtil.addObj(key, payBankLimitBO, CacheConstants.TWO_HOURS);
				}
			}
			if (ObjectUtil.isBlank(payBankLimitBO)) {
				key = CacheConstants.P_CORE_PAY_BANK_LIMIT_SINGLE + bankId;
				payBankLimitBO = redisUtil.getObj(key, new PayBankLimitBO());
				if (ObjectUtil.isBlank(payBankLimitBO)) {
					List<PayBankLimitBO> list = payBankLimitDaoMapper.getPayBankLimitByBankId(bankId);
					if (!ObjectUtil.isBlank(list)) {
						payBankLimitBO = list.get(0);
						redisUtil.addObj(key, payBankLimitBO, CacheConstants.TWO_HOURS);
					}
				}
			}
		}
		if (ObjectUtil.isBlank(payBankLimitBO)) {
			logger.info("银行ID【" + bankId + "】未配置支付限额");
		}
		return payBankLimitBO;
	}

}
