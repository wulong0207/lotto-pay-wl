package com.hhly.paycore.remote.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayBankLimitDaoMapper;
import com.hhly.paycore.remote.service.IPayBankLimitService;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PayBankLimitBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 【对外暴露hession接口】 银行支付限额
 * @author xiongJinGang
 * @date 2017年6月21日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iPayBankLimitService")
public class PayBankLimitServiceImpl implements IPayBankLimitService {

	@Autowired
	private PayBankLimitDaoMapper payBankLimitDaoMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public List<PayBankLimitBO> selectAll() {
		return queryAllBankLimitFromCache();
	}

	/**  
	* 方法说明: 从缓存中获取支付限额
	* @auth: xiongJinGang
	* @time: 2017年11月28日 上午10:33:01
	* @return: List<PayBankLimitBO> 
	*/
	@SuppressWarnings("unchecked")
	private List<PayBankLimitBO> queryAllBankLimitFromCache() {
		String key = CacheConstants.P_CORE_PAY_BANK_LIMIT_LIST;
		Object object = redisUtil.getObj(key);
		List<PayBankLimitBO> list = null;
		if (ObjectUtil.isBlank(object)) {
			list = payBankLimitDaoMapper.selectAll();
		} else {
			list = (List<PayBankLimitBO>) object;
			if (!ObjectUtil.isBlank(list)) {
				redisUtil.addObj(key, list, CacheConstants.TWO_HOURS);
			}
		}
		return list;
	}

}
