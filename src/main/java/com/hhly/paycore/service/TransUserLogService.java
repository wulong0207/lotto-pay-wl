package com.hhly.paycore.service;

import java.util.List;

import com.hhly.paycore.po.TransUserPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.vo.TransParamVO;

/**
 * @author xiongjingang
 * @version 1.0
 * @desc 用户交易接口
 * @date 2017年3月3日 上午10:32:26
 * @company 益彩网络科技公司
 */
public interface TransUserLogService {
	/**
	 * 方法说明: 根据条件获取用户的交易记录详情
	 *
	 * @param token
	 * @param tradeCode
	 * @throws Exception
	 * @time: 2017年3月7日 下午2:40:18
	 * @return: ResultBO<?>
	 */
	public ResultBO<?> findTransUserByCode(String token, String tradeCode) throws Exception;

	/**
	 * 方法说明: 根据条件获取用户的交易记录，默认为交易明细【提款API接口】
	 *
	 * @param transParamVO
	 * @throws Exception
	 * @time: 2017年3月3日 下午2:15:27
	 * @return: ResultBO<?>
	 */
	public ResultBO<?> findTransUserByPage(TransParamVO transParamVO) throws Exception;

	/**
	 * 方法说明: 给移动端提供接口
	 *
	 * @param transParamVO
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年4月26日 下午3:25:18
	 * @return: ResultBO<?>
	 */
	ResultBO<?> findAppTransUserByPage(TransParamVO transParamVO) throws Exception;

	/**  
	* 方法说明: 批量添加交易流水
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年8月3日 下午5:48:51
	* @return: int 
	*/
	int addTransUserByBatch(List<TransUserPO> list) throws Exception;

	/**  
	* 方法说明: 批量更新交易流水记录状态（CMS更新提款的交易流水状态）
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年8月7日 上午10:54:22
	* @return: int 
	*/
	int updateTransUserByBatch(List<TransUserPO> list) throws Exception;

	/**  
	* 方法说明: 添加交易流水记录
	* @auth: xiongJinGang
	* @param transUserPO
	* @throws Exception
	* @time: 2017年11月9日 下午6:22:26
	* @return: int 
	*/
	int addTransLogRecord(TransUserPO transUserPO) throws Exception;

	/**  
	* 方法说明: 更新交易流水
	* @auth: xiongJinGang
	* @param transUserPO
	* @throws Exception
	* @time: 2017年11月22日 下午3:41:48
	* @return: int 
	*/
	int updateTransUser(TransUserPO transUserPO) throws Exception;

}
