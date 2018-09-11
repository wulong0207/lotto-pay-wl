package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.ThirdTransDetailPO;
import com.hhly.skeleton.pay.trans.bo.ThirdTransDetailBO;
import com.hhly.skeleton.pay.trans.vo.ThirdTransDetailVO;

public interface ThirdTransDetailMapper {

	List<ThirdTransDetailBO> findUserTransListByPage(ThirdTransDetailVO thirdTransDetailVO);

	ThirdTransDetailBO findTransUserByCode(@Param("guestId") Integer guestId, @Param("orderCode") String orderCode);

	int addUserTrans(ThirdTransDetailPO thirdTransDetailPO);

}
