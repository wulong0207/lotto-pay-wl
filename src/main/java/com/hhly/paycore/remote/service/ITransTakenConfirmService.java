package com.hhly.paycore.remote.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.vo.TakenReqParamVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransTakenVO;

/**
 * @desc 【对外暴露hession接口】 提款交易服务接口
 * @author xiongjingang
 * @date 2017年3月2日 上午10:44:46
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface ITransTakenConfirmService {

	/**    
	* 方法说明： 根据交易流水号查找用户的交易详情
	* @param userId 用户ID
	* @param transCode 交易流水号
	* @time: 2017年3月2日 下午12:17:44
	* @return: ResultBO<?> 
	* @throws Exception 
	*/
	public ResultBO<?> findTakenByCode(String token, String transCode) throws Exception;

	/**  
	* 方法说明: 分页查询提款记录
	* @param transParamVO
	* @throws Exception
	* @time: 2017年3月7日 上午11:49:29
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> findTakenListByPage(TransParamVO transParamVO) throws Exception;

	/**  
	* 方法说明: 添加提款记录
	* @param transTakenVO
	* @throws Exception
	* @time: 2017年3月9日 上午11:59:16
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> addTransTaken(TransTakenVO transTakenVO) throws Exception;

	/**  
	* 方法说明: 用户提交银行卡号，具体的提款金额，返回确认提款页面
	* @auth: xiongJinGang
	* @param takenValidateTypeVO
	* @throws Exception
	* @time: 2017年4月19日 上午10:24:22
	* @return: ResultBO<?> 
	*/
	ResultBO<?> taken(TakenReqParamVO takenReqParamVO) throws Exception;

	/**  
	* 方法说明: 提款确认
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年4月21日 下午12:17:57
	* @return: ResultBO<?> 
	*/
	ResultBO<?> takenConfirm(TakenReqParamVO takenReqParamVO) throws Exception;

	/**  
	* 方法说明: 移动端提款请求
	* @auth: xiongJinGang
	* @param token
	* @throws Exception
	* @time: 2017年5月4日 下午3:50:18
	* @return: ResultBO<?> 
	*/
	ResultBO<?> takenReqForApp(String token) throws Exception;

	/**  
	* 方法说明: 供CMS调用批量更新提款审核状态。提交过来的list都是统一状态，全部通过或者全部不过。
	* @auth: xiongJinGang
	* @param list；trans_taken_code、review_by、trans_status、user_id必填；trans_fail_info为审核不通过时，必填
	* @param operateType；操作类型，1审核、2提交银行、3银行处理结果、4CMS确认完成，参考 TakenOperateTypeEnum
	* @throws Exception
	* @time: 2017年8月7日 下午3:39:54
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateTakenStatusByBatch(List<TransTakenBO> list, Short operateType) throws Exception;

	/**  
	* 方法说明: 提款前，用户输入提款金额后异步加载提示
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年8月22日 下午6:28:26
	* @return: ResultBO<?> 
	*/
	ResultBO<?> takenCount(TakenReqParamVO takenReqParamVO) throws Exception;

	/**  
	* 方法说明: 验证用户提款次数是否超过当日最高提款次数，如有超过，返回正在处理中的所有提款记录
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年11月4日 下午2:57:28
	* @return: ResultBO<?> 
	*/
	ResultBO<?> validateTakenCount(TakenReqParamVO takenReqParamVO) throws Exception;

}
