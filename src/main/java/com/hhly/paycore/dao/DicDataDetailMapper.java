package com.hhly.paycore.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.hhly.skeleton.lotto.base.dic.bo.DicDataDetailBO;

public interface DicDataDetailMapper {

	List<DicDataDetailBO> findDataList(@Param("dicCode") String dicCode);

}