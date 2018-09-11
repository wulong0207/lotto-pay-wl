package com.hhly.paycore.dao;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.TransRechargePO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;

/**
 * @desc 充值mapper
 * @author xiongjingang
 * @date 2017年3月2日 下午12:30:31
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransRechargeMapper {
	/**  
	* 方法说明: 根据充值编号查找用户充值详情
	* @param userId
	* @param rechargeCode
	* @time: 2017年3月2日 下午12:30:50
	* @return: TransRechargeBO 
	*/
	TransRechargeBO getUserRechargeByCode(@Param("userId") Integer userId, @Param("rechargeCode") String rechargeCode);

	/**  
	* 方法说明: 根据交易流水获取交易记录
	* @auth: xiongJinGang
	* @param rechargeCode 交易流水
	* @time: 2017年3月23日 下午2:44:33
	* @return: TransRechargeBO 
	*/
	TransRechargeBO getRechargeByCode(String rechargeCode);

	/**  
	* 方法说明: 根据批次号查询
	* @auth: xiongJinGang
	* @param batchNum
	* @time: 2017年5月10日 下午8:13:20
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> getRechargeByBatchCode(String batchNum);

	/**  
	* 方法说明: 根据充值参数获取充值记录
	* @auth: xiongjingang
	* @param transRecharge
	* @time: 2017年3月20日 上午10:44:38
	* @return: TransRechargeBO
	*/
	TransRechargeBO getByTransRecharge(TransRechargeVO transRecharge);

	/**  
	* 方法说明: 分页获取充值列表
	* @param transParamVO
	* @time: 2017年3月9日 上午10:21:41
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> getRechargeListByPage(TransParamVO transParamVO);

	/**  
	* 方法说明: 获取过了30分钟，充值未到账的关闭
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年7月25日 下午3:03:23
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> getRechargeByParam(Map<String, Object> map);

	/**  
	* 方法说明: 根据条件获取总数量
	* @param transParamVO
	* @time: 2017年3月9日 上午10:21:58
	* @return: int 
	*/
	int getRechargeListCount(TransParamVO transParamVO);

	/**  
	* 方法说明: 添加充值记录
	* @param transRechargePO
	* @time: 2017年3月9日 上午10:22:59
	* @return: int 
	*/
	int addRechargeTrans(TransRechargePO transRechargePO);

	/**  
	* 方法说明: 批量添加充值记录
	* @param transRechargePO
	* @time: 2017年3月9日 上午10:22:59
	* @return: int 
	*/
	int addRechargeTransList(List<TransRechargePO> list);

	/**  
	* 方法说明: 批量更新
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月11日 上午9:47:18
	* @return: int 
	*/
	int updateBatch(List<TransRechargePO> list);

	/**  
	* 方法说明: 批量更新充值提款状态
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年8月25日 上午11:20:01
	* @return: int 
	*/
	int updateRechargeTakenStatusByBatch(List<TransRechargePO> list);

	/**  
	* 方法说明: 更新银行返回的时间及状态信息
	* @param transRechargePO
	* @time: 2017年3月10日 上午11:21:11
	* @return: int 
	*/
	int updateRechargeTrans(TransRechargePO transRechargePO);

	/**  
	* 方法说明: 更新充值记录
	* @auth: xiongJinGang
	* @param transRechargePO
	* @time: 2017年5月6日 下午3:07:44
	* @return: int 
	*/
	int update(TransRechargePO transRechargePO);
}
