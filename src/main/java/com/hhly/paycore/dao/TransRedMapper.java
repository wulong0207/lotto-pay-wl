package com.hhly.paycore.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.TransRedPO;
import com.hhly.skeleton.pay.bo.TransRedBO;
import com.hhly.skeleton.pay.vo.TransRedVO;

/**
 * @desc 红包交易记录Mapper
 * @author xiongJinGang
 * @date 2017年3月24日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransRedMapper {

	/**  
	* 方法说明: 添加红包交易记录
	* @auth: xiongJinGang
	* @param record
	* @time: 2017年3月24日 上午11:14:52
	* @return: int 
	*/
	int addTransRed(TransRedPO record);

	/**  
	* 方法说明: 获取用户的红包交易记录【一个彩金红包的交易记录有1-N条】
	* @auth: xiongJinGang
	* @param userId
	* @param redCode
	* @time: 2017年3月24日 上午11:15:06
	* @return: List<TransRedBO> 
	*/
	List<TransRedBO> getUserTransRedByCode(@Param("userId") Integer userId, @Param("redCode") Integer redCode);

	/** 
	* @Title: findUserTransRedListByPage 
	* @Description:
	*  @param transRed
	*  @return
	* @time 2017年5月8日 上午10:25:12
	*/
	List<TransRedBO> findUserTransRedListByPage(TransRedVO transRed);

	int findUserTransRedListCount(TransRedVO transRed);

}