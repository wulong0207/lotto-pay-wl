package com.hhly.paycore.service;

import java.util.List;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayBankcardVO;
import com.hhly.skeleton.pay.vo.TakenBankCardVO;
import com.hhly.skeleton.pay.vo.TakenReqParamVO;

/**
 * @auth chenkangning
 * @date 2017/3/2
 * @desc 用户银行卡管理接口
 * @compay 益彩网络科技有限公司
 * @version 1.0
 */
public interface BankcardService {

	/**  
	* 方法说明: 根据用户ID获取银行卡列表
	* @auth: xiongjingang
	* @param userId
	* @time: 2017年3月17日 下午2:17:54
	* @return: List<PayBankcardBO> 
	*/
	List<PayBankcardBO> findUserBankList(Integer userId);

	/**  
	* 方法说明: 根据银行卡Id获取用户的银行信息
	* @auth: xiongJinGang
	* @param userId
	* @param bankCardId
	* @time: 2017年4月8日 下午4:42:54
	* @return: PayBankcardBO 
	*/
	PayBankcardBO findUserBankById(Integer userId, Integer bankCardId);

	/**  
	* 方法说明: 根据用户ID获取银行卡号，有默认的取默认的，没有默认的取第一个
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月18日 下午2:55:13
	* @return: String 
	*/
	List<PayBankcardBO> findUserBankListFromCache(Integer userId);

	/**  
	* 方法说明: 获取用户默认的银行卡
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月19日 上午10:31:05
	* @return: PayBankcardBO 
	*/
	PayBankcardBO getSingleBankCard(Integer userId);

	/**  
	* 方法说明: 获取用户指定的银行卡信息
	* @auth: xiongJinGang
	* @param cardCode
	* @param userId
	* @time: 2017年4月21日 上午10:59:41
	* @return: PayBankcardBO 
	*/
	PayBankcardBO getBankCardByCardCodeFromCache(String cardCode, Integer userId);

	/**  
	* 方法说明: 获取用户具体的银行卡信息，对比银行名称，不匹配则更新，匹配则不操作
	* @auth: xiongJinGang
	* @param userId
	* @param takenReqParamVO
	* @time: 2017年5月4日 下午6:06:51
	* @return: void 
	*/
	void findBankByIdAndCheckName(Integer userId, TakenReqParamVO takenReqParamVO);

	ResultBO<?> findUserBankCardByCardId(Integer userId, Integer bankCardId);

	/**  
	* 方法说明: 更新默认银行卡
	* @auth: xiongJinGang
	* @param payBankcardVO
	* @throws Exception
	* @time: 2017年6月2日 下午4:54:13
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateDefault(PayBankcardVO payBankcardVO) throws Exception;

	/**  
	* 方法说明: 根据用户ID和银行卡号获取银行信息
	* @auth: xiongJinGang
	* @param userId
	* @param cardCode
	* @time: 2017年7月26日 下午4:04:23
	* @return: PayBankcardBO 
	*/
	PayBankcardBO findUserBankByCode(Integer userId, String cardCode);

	/**  
	* 方法说明: 获取用户可用储蓄卡列表
	* @auth: xiongJinGang
	* @param userId
	* @param bankCardId 银行卡ID，可以为空
	* @time: 2017年4月19日 上午10:30:44
	* @return: List<TakenBankCardVO> 
	*/
	List<TakenBankCardVO> getUserBankInfo(Integer userId, Integer bankCardId);

	/**  
	* 方法说明: 更新默认银行
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年11月9日 下午5:10:29
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateDefaultBank(TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明: 储蓄卡列表
	* @auth: xiongJinGang
	* @param token
	* @throws Exception
	* @time: 2018年3月7日 下午2:17:59
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findBankList(String token) throws Exception;

}
