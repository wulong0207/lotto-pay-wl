package com.hhly.paycore.service.impl;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.ThirdTransDetailMapper;
import com.hhly.paycore.po.ThirdTransDetailPO;
import com.hhly.paycore.service.ThirdTransDetailService;
import com.hhly.skeleton.pay.trans.bo.ThirdTransDetailBO;

@Service
public class ThirdTransDetailServiceImpl implements ThirdTransDetailService {
	@Resource
	private ThirdTransDetailMapper thirdTransDetailMapper;

	@Override
	public ThirdTransDetailBO findUserDetail(ThirdTransDetailBO transDetailBO) {
		return thirdTransDetailMapper.findTransUserByCode(transDetailBO.getGuestId(), transDetailBO.getTransCode());
	}

	@Override
	public int addUserDetail(ThirdTransDetailBO transDetailBO) {
		ThirdTransDetailPO thirdTransDetailPO = new ThirdTransDetailPO(transDetailBO);
		return thirdTransDetailMapper.addUserTrans(thirdTransDetailPO);
	}

	@Override
	public int addUserDetail(ThirdTransDetailPO thirdTransDetailPO) {
		return thirdTransDetailMapper.addUserTrans(thirdTransDetailPO);
	}

}
