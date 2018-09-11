package com.hhly.paycore.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.TransTakenPO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.vo.TransParamVO;

/**
 * @desc 用户提款Mapper
 * @author xiongjingang
 * @date 2017年3月2日 上午10:32:44
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransTakenMapper {

	/**  
	* 方法说明: 根据交易编号查找记录详情
	* @param userId 用户ID
	* @param transCode 交易编号
	* @time: 2017年3月7日 上午11:54:48
	* @return: TakenAppVO 
	*/
	TransTakenBO findUserTakenByCode(@Param("userId") Integer userId, @Param("transCode") String transCode);

	/**  
	* 方法说明: 分页获取用户的提款记录
	* @param transParamVO
	* @time: 2017年3月7日 上午11:55:22
	* @return: List<TransRechargeBO> 
	*/
	List<TransTakenBO> findTakenListByPage(TransParamVO transParamVO);

	/**  
	* 方法说明: 根据提款编号,批量获取提款记录
	* @auth: xiongJinGang
	* @param takenCodeList
	* @time: 2017年8月25日 下午4:46:41
	* @return: List<TransRechargeBO> 
	*/
	List<TransTakenBO> getTakenBatch(List<String> codes);

	/**  
	* 方法说明: 获取用户当天提款的次数
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月24日 上午10:04:35
	* @return: int 
	*/
	int getUserTakenTimes(@Param("userId") Integer userId, @Param("today") String today);

	/**  
	* 方法说明: 按条件查询总数量
	* @param transParamVO
	* @time: 2017年3月9日 上午10:16:21
	* @return: int 
	*/
	int findTakenListCount(TransParamVO transParamVO);

	/**  
	* 方法说明: 添加提款记录
	* @param transTakenPO
	* @time: 2017年3月9日 上午10:21:18
	* @return: int 
	*/
	int addTakenTrans(TransTakenPO transTakenPO);

	/**  
	* 方法说明: 更新提款信息（第三方接口返回用，提款走线下，这个暂时用不上）
	* @param transTakenPO
	* @time: 2017年3月10日 上午11:09:01
	* @return: int 
	*/
	int updateTakenTrans(TransTakenPO transTakenPO);

	/**  
	* 方法说明: 批量更新提款记录
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年8月7日 上午11:06:12
	* @return: int 
	*/
	int updateTakenByBatch(@Param("list") List<TransTakenPO> list);

	/**  
	* 方法说明: 查找用户处理中的提款记录（已到账的不查）
	* @auth: xiongJinGang
	* @param paramMap
	* @time: 2017年11月4日 下午12:04:22
	* @return: List<TakenAppVO> 
	*/
	List<TransTakenBO> getProcessTakenList(Map<String, Object> paramMap);

	/**  
	* 方法说明: 查询满足自动审核的提款记录
	* @auth: xiongJinGang
	* @param paramMap
	* @time: 2018年3月7日 下午3:26:54
	* @return: List<TransTakenBO> 
	*/
	List<TransTakenBO> findWaitVerifyTakenList(Map<String, Object> paramMap);
}
