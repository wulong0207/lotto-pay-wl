package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayBankMapper;
import com.hhly.paycore.service.PayBankService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 银行实现层
 * @author xiongJinGang
 * @date 2017年4月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("payBankService")
public class PayBankServiceImpl implements PayBankService {
	@Resource
	private PayBankMapper payBankMapper;
	@Resource
	private RedisUtil redisUtil;
	@Value("${before_file_url}")
	private String imgUrl;// 图片路径

	@Override
	public List<PayBankBO> findBankListByPlatFromCache(String platform, Short bankType) {
		String key = CacheConstants.P_CORE_PAY_BANK_LIST + platform + "_" + bankType;
		List<PayBankBO> list = redisUtil.getObj(key, new ArrayList<PayBankBO>());
		if (ObjectUtil.isBlank(list)) {
			list = payBankMapper.getSortBankByPlatform(platform, bankType);
			if (!ObjectUtil.isBlank(list)) {
				for (PayBankBO payBankBO : list) {
					String bLogo = payBankBO.getbLogo();
					if (!ObjectUtil.isBlank(bLogo) && !bLogo.startsWith("http://")) {
						payBankBO.setbLogo(imgUrl + bLogo);
					}
					String sLogo = payBankBO.getsLogo();
					if (!ObjectUtil.isBlank(sLogo) && !sLogo.startsWith("http://")) {
						payBankBO.setsLogo(imgUrl + sLogo);
					}
				}
				redisUtil.addObj(key, list, CacheConstants.ONE_MONTH);// 保存一个星期
			}
		}
		return list;
	}

	@Override
	public PayBankBO findBankById(Integer id) {
		PayBankBO payBankBO = payBankMapper.getBankById(id);
		if (!ObjectUtil.isBlank(payBankBO)) {
			String bLogo = payBankBO.getbLogo();
			if (!ObjectUtil.isBlank(bLogo) && !bLogo.startsWith("http://")) {
				payBankBO.setbLogo(imgUrl + bLogo);
			}
			String sLogo = payBankBO.getsLogo();
			if (!ObjectUtil.isBlank(sLogo) && !sLogo.startsWith("http://")) {
				payBankBO.setsLogo(imgUrl + sLogo);
			}
		}
		return payBankBO;
	}

	@Override
	public ResultBO<?> findBankByIdAndValidate(Integer id) {
		PayBankBO payBankBO = this.findBankById(id);
		if (ObjectUtil.isBlank(payBankBO)) {
			return ResultBO.err(MessageCodeConstants.TRANS_TAKEN_BANK_IS_NULL_FIELD);
		}
		// 银行暂停支付
		if (payBankBO.getStatus().equals(PayConstants.BankStatusEnum.DISABLE.getKey())) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_DISABLE_ERROR_SERVICE);
		}
		return ResultBO.ok(payBankBO);
	}

	@Override
	public PayBankBO findBankFromCache(Integer bankId) {
		List<PayBankBO> list = findAllBankFromCache();
		if (!ObjectUtil.isBlank(list)) {
			for (PayBankBO payBankBO : list) {
				if (payBankBO.getId().equals(bankId)) {
					String bLogo = payBankBO.getbLogo();
					if (!ObjectUtil.isBlank(bLogo) && !bLogo.startsWith("http://")) {
						payBankBO.setbLogo(imgUrl + bLogo);
					}
					String sLogo = payBankBO.getsLogo();
					if (!ObjectUtil.isBlank(sLogo) && !sLogo.startsWith("http://")) {
						payBankBO.setsLogo(imgUrl + sLogo);
					}
					return payBankBO;
				}
			}
		}
		return null;
	}

	/**  
	* 方法说明: 获取所有银行列表，存到缓存中
	* @auth: xiongJinGang
	* @time: 2017年4月20日 下午2:53:39
	* @return: List<PayBankBO> 
	*/
	private List<PayBankBO> findAllBankFromCache() {
		String key = CacheConstants.P_CORE_PAY_BANK_LIST;
		List<PayBankBO> list = redisUtil.getObj(key, new ArrayList<PayBankBO>());
		if (ObjectUtil.isBlank(list)) {
			list = findAllBank();
			if (!ObjectUtil.isBlank(list)) {

				redisUtil.addObj(key, list, CacheConstants.ONE_MONTH);
			}
		}
		return list;
	}

	@Override
	public PayBankBO findSigleBankFromCache(Integer bankId) {
		String key = CacheConstants.P_CORE_PAY_BANK_LIST + bankId;
		PayBankBO payBank = redisUtil.getObj(key, PayBankBO.class);
		if (ObjectUtil.isBlank(payBank)) {
			payBank = findBankById(bankId);
			if (!ObjectUtil.isBlank(payBank)) {
				redisUtil.addObj(key, payBank, CacheConstants.ONE_MONTH);
			}
		}
		return payBank;
	}

	@Override
	public List<PayBankBO> findAllBank() {
		List<PayBankBO> list = payBankMapper.getAll();
		if (!ObjectUtil.isBlank(list)) {
			for (PayBankBO payBankBO : list) {
				String bLogo = payBankBO.getbLogo();
				if (!ObjectUtil.isBlank(bLogo) && !bLogo.startsWith("http://")) {
					payBankBO.setbLogo(imgUrl + bLogo);
				}
				String sLogo = payBankBO.getsLogo();
				if (!ObjectUtil.isBlank(sLogo) && !sLogo.startsWith("http://")) {
					payBankBO.setsLogo(imgUrl + sLogo);
				}
			}
		}
		return list;
	}

	/*	@Override
		public List<PayBankBO> findBankByType(Short payType) {
			String key = CacheConstants.PAY_BANK_LIST_BY_TYPE + payType;// 银行类型列表
	
			List<PayBankBO> list = redisUtil.getObj(key, new ArrayList<PayBankBO>());
			if (ObjectUtil.isBlank(list)) {
				List<PayBankBO> bankList = findAllBankFromCache();
				for (int i = bankList.size() - 1; i >= 0; i--) { // 倒序
					// 银行类型不匹配或者状态是信用，都不要
					if (!bankList.get(i).getPayType().equals(payType) || bankList.get(i).getStatus().equals(PayConstants.BankStatusEnum.DISABLE.getKey())) {
						bankList.remove(i);
					}
				}
				if (!ObjectUtil.isBlank(bankList)) {
					redisUtil.addObj(key, bankList, CacheConstants.TWO_HOURS);
				}
				return bankList;
			}
			return list;
		}*/
}
