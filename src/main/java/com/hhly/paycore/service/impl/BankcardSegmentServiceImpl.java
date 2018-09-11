package com.hhly.paycore.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.PayBankSegmentMapper;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PayBankSegmentBO;
import com.hhly.utils.RedisUtil;

@Service("bankcardSegmentService")
public class BankcardSegmentServiceImpl implements BankcardSegmentService {

	@Resource
	private PayBankSegmentMapper payBankSegmentMapper;
	@Resource
	private RedisUtil redisUtil;

	@SuppressWarnings("unchecked")
	@Override
	public List<PayBankSegmentBO> findList() {
		String key = CacheConstants.P_CORE_PAY_BANK_CARD_SEGMENT_LIST;
		Object object = redisUtil.getObj(key);
		List<PayBankSegmentBO> list = null;
		if (ObjectUtil.isBlank(object)) {
			list = payBankSegmentMapper.getList();
			// 设置缓存，永不过期
			redisUtil.addObj(key, list, null);
		} else {
			list = (List<PayBankSegmentBO>) object;
		}
		return list;
	}

	@Override
	public ResultBO<?> findPayBankSegmentByCard(String bankCard) {
		// 1从缓存里面获取PayBankSegmentBO集合
		List<PayBankSegmentBO> payBankSegmentBOList = this.findList();
		if (ObjectUtil.isBlank(payBankSegmentBOList)) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_SEGMENT_NOT_FOUND_ERROR_SERVICE);
		}
		PayBankSegmentBO bo = null;
		for (PayBankSegmentBO payBankSegmentBO : payBankSegmentBOList) {
			if (!ObjectUtil.isBlank(payBankSegmentBO.getTopCut())) {
				// 先检索是不是符合xxxxx开头的
				if (bankCard.startsWith(String.valueOf(payBankSegmentBO.getTopCut()))) {
					bo = payBankSegmentBO;
					break;
				}
			}
		}
		// 如果对象为空，表示卡号不符合要求
		if (ObjectUtil.isBlank(bo)) {
			return ResultBO.err(MessageCodeConstants.BANKCARD_ERROR_SERVICE);
		} else if (bankCard.length() != bo.getCardLength()) {
			// 如果是以xxxx开头的，再检查卡号长度是不是一样
			return ResultBO.err(MessageCodeConstants.BANKCARD_ERROR_SERVICE);
		}
		return ResultBO.ok(bo);
	}

	@Override
	public String findBankSegmentCodeByCard(String bankCard) {
		String bankCode = null;
		List<PayBankSegmentBO> payBankSegmentBOList = this.findList();
		PayBankSegmentBO bankSegmentBO = null;
		if (!ObjectUtil.isBlank(payBankSegmentBOList)) {
			for (PayBankSegmentBO payBankSegmentBO : payBankSegmentBOList) {
				if (!ObjectUtil.isBlank(payBankSegmentBO.getTopCut())) {
					// 先检索是不是符合xxxxx开头的
					if (bankCard.startsWith(String.valueOf(payBankSegmentBO.getTopCut()))) {
						bankSegmentBO = payBankSegmentBO;
						break;
					}
				}
			}
		}
		if (!ObjectUtil.isBlank(bankSegmentBO) && !ObjectUtil.isBlank(bankSegmentBO.getBankName())) {
			// 青州中银富登村镇银行(15194588) 要获取到括号中的内容
			bankCode = bankSegmentBO.getBankName().substring(bankSegmentBO.getBankName().lastIndexOf("(") + 1, bankSegmentBO.getBankName().lastIndexOf(")"));
		}
		return bankCode;
	}
}
