package com.hhly.paycore.remote.service;

import java.util.List;
import java.util.Map;

import com.hhly.paycore.po.TransRechargePO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;
import com.hhly.skeleton.user.bo.UserInfoBO;

/**
 * @desc 【对外暴露hession接口】充值接口
 * @author xiongJinGang
 * @date 2017年12月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface ITransRechargeService {

	/**  
	* 方法说明: 根据充值流水号查找用户充值记录详情
	* @param userId 用户Id
	* @param rechargeCode 充值流水号
	* @time: 2017年3月2日 下午12:25:27
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	public ResultBO<?> findRechargeByCode(String token, String rechargeCode) throws Exception;

	/**  
	* 方法说明: 分页查找充值列表
	* @param transParamVO 交易列表查询
	* @throws Exception
	* @time: 2017年3月7日 上午10:47:30
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> findRechargeListByPage(TransParamVO transParamVO) throws Exception;

	/**  
	* 方法说明: 添加充值记录（不对外提供接口）
	* @param transRecharge 充值记录
	* @param userInfo 用户信息
	* @time: 2017年3月9日 下午12:05:14
	* @return: ResultBO<?> 
	*/
	ResultBO<?> addRechargeTrans(TransRechargeVO transRecharge, UserInfoBO userInfo);

	/**  
	* 方法说明: 根据交易号查找充值记录
	* @auth: xiongJinGang
	* @param rechargeCode
	* @time: 2017年3月23日 下午4:08:30
	* @return: TransRechargeBO 
	*/
	TransRechargeBO findRechargeByTransCode(String rechargeCode);

	/**  
	* 方法说明: 支付完成后更新充值记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @time: 2017年4月10日 下午6:03:18
	* @return: ResultBO<?> 
	*/
	void updateRechargeTrans(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, Short operate) throws Exception;

	/**  
	* 方法说明: 批量更新充值记录
	* @auth: xiongJinGang
	* @param rechargeList
	* @param payNotifyResult
	* @throws Exception
	* @time: 2017年5月10日 下午9:14:40
	* @return: void 
	*/
	void updateRechargeTransForBatch(List<TransRechargeBO> rechargeList, PayNotifyResultVO payNotifyResult) throws Exception;

	/**  
	* 方法说明: 更新红包信息
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年4月11日 上午11:11:57
	* @return: int 
	* @throws Exception 
	*/
	int updateRechargeTransRedInfo(TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明:  根据充值对象查找用户充值记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年10月16日 下午6:57:15
	* @return: TransRechargeBO 
	*/
	TransRechargeBO findByTransRecharge(TransRechargeVO transRecharge);

	/**  
	* 方法说明: 根据批次号查询
	* @auth: xiongJinGang
	* @param batchNum
	* @time: 2017年5月10日 下午8:13:20
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> findRechargeByBatchCode(String batchNum);

	/**  
	* 方法说明: 更新充值记录
	* @auth: xiongJinGang
	* @param transRechargePO
	* @throws Exception
	* @time: 2017年5月6日 下午3:18:39
	* @return: int 
	*/
	int updateRecharge(TransRechargePO transRechargePO) throws Exception;

	/**  
	* 方法说明: 批量添加充值记录
	* @auth: xiongJinGang
	* @param userInfo
	* @param payParam
	* @time: 2017年5月10日 下午4:07:04
	* @return: ResultBO<?> 
	*/
	ResultBO<?> addRechargeTransList(UserInfoBO userInfo, PayParamVO payParam, String channelType);

	/**  
	* 方法说明: 添加充值记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年7月6日 下午4:32:55
	* @return: ResultBO<?> 
	*/
	ResultBO<?> addRechargeTrans(TransRechargePO transRecharge);

	/**  
	* 方法说明: 根据条件查询交易记录（供任务关闭未支付订单用）
	* @auth: xiongJinGang
	* @param map
	* @time: 2017年7月25日 下午3:00:58
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> findRechargeByParam(Map<String, Object> map);

	/**  
	* 方法说明: 分页查找充值成功的记录（过滤掉人工充值的充值记录）
	* @auth: xiongJinGang
	* @param userId
	* @param currendResult
	* @param endResult
	* @time: 2017年4月19日 下午6:18:52
	* @return: List<TransRechargeBO> 
	*/
	List<TransRechargeBO> findRechargeRecordForTaken(Integer userId, Integer currentPage);

	/**  
	* 方法说明: 获取用户的总充值记录
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月24日 上午11:38:32
	* @return: Integer 
	*/
	Integer findRechargeRecordCountForTaken(Integer userId);

	/**  
	* 方法说明: 批量更新提款充值记录中的提款状态 
	* @auth: xiongJinGang
	* @param rechargeList
	* @throws Exception
	* @time: 2017年8月25日 上午11:25:25
	* @return: void 
	*/
	void updateRechargeTakenStatusByBatch(List<TransRechargePO> rechargeList) throws Exception;

}
