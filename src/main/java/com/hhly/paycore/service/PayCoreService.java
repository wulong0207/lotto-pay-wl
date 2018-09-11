package com.hhly.paycore.service;

import java.util.List;

import com.hhly.paycore.po.TransRechargePO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.channel.vo.ChannelParamVO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.OrderGroupVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;
import com.hhly.skeleton.user.bo.UserWalletBO;

public interface PayCoreService {

	/**  
	* 方法说明: 钱包余额或者红包支付，更新账户钱包及各种状态 
	* @auth: xiongJinGang
	* @param payParam
	* @param toPayEndTimeVO
	* @param orderBaseList
	* @throws RuntimeException
	* @throws Exception
	* @time: 2017年6月10日 下午4:22:25
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyBalanceAndStatusForLocal(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO, List<PayOrderBaseInfoVO> orderBaseList) throws RuntimeException, Exception;

	/**  
	* 方法说明: 批量修改支付成功的相关数据
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param list
	* @throws Exception
	* @time: 2017年6月16日 下午2:56:22
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyPaySuccessTransRecordForBatch(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, List<PayOrderBaseInfoVO> list) throws Exception;

	ResultBO<?> modifyFailTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, List<PayOrderBaseInfoVO> list) throws Exception;

	List<PayOrderBaseInfoVO> transOrder(List<OrderBaseInfoBO> list, Integer userId) throws Exception;

	ResultBO<?> findPayBankLimitAndValidate(PayBankcardBO payBankcardBO, PayParamVO payParam);

	TransRechargeBO findRechargeRecord(RefundParamVO refundParam);

	/**  
	* 方法说明: 更新充值成功交易记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @throws Exception
	* @time: 2017年11月10日 上午9:46:10
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyRechargeSuccessTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception;

	/**  
	* 方法说明: 添加充值失败记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @throws Exception
	* @time: 2017年4月28日 上午10:37:32
	* @return: void 
	*/
	ResultBO<?> modifyFailTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception;

	/**  
	*  cms后台用，添加充值流水，更新用户钱包，添加交易流水（2017-07-07 与产品沟通，cms人工充值的金额，全部入80%账户）
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年11月10日 上午10:11:43
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyUserWalletCash(TransRechargePO transRecharge, CmsRechargeVO cmsRecharge) throws Exception;

	/**  
	* 方法说明: CMS后台用，添加用户钱包中彩金金额、添加彩金红包、添加彩金红包使用记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年7月6日 下午5:52:24
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyUserWalletRed(TransRechargePO transRecharge, CmsRechargeVO cmsRecharge) throws Exception;

	/**  
	* 方法说明: 代理系统充值使用，添加用户钱包中彩金金额、添加彩金红包、添加彩金红包使用记录、添加用户交易流水
	* @auth: YiJian
	* @param transRecharge
	* @throws Exception
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyUserWalletRedForAgent(TransRechargePO transRecharge) throws Exception;

	/**  
	* 方法说明: 选择具体的支付渠道进行支付
	* @auth: xiongJinGang
	* @param channelParam
	* @time: 2017年12月14日 下午4:24:57
	* @return: ResultBO<?> 
	*/
	ResultBO<?> getPayChannel(ChannelParamVO channelParam);

	/**  
	* 方法说明: 添加或者更新当日限额
	* @auth: xiongJinGang
	* @param channelId
	* @param payType
	* @param cardType
	* @param amount
	* @time: 2017年12月14日 下午4:27:22
	* @return: Double 
	*/
	Double addDayLimitAmount(Integer channelId, Short payType, Short cardType, Double amount);

	/**  
	* 方法说明: 添加或者更新当日限额
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2017年12月15日 下午6:00:12
	* @return: Double 
	*/
	Double addDayLimitAmount(TransRechargeBO transRecharge);

	/**  
	* 方法说明: 推单本地支付
	* @auth: xiongJinGang
	* @param payParam
	* @throws RuntimeException
	* @throws Exception
	* @time: 2018年1月11日 下午6:30:29
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyPushStatusForLocal(PayParamVO payParam) throws RuntimeException, Exception;

	/**  
	* 方法说明: 验证充值状态
	* @auth: xiongJinGang
	* @param transRecharge
	* @time: 2018年2月27日 上午10:56:05
	* @return: void 
	*/
	ResultBO<?> checkTransRechargeStatus(String transRechargeCode);

	/**  
	* 方法说明: 代理现金充值
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2018年4月8日 下午12:14:51
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyUserWalletCashForAgent(TransRechargePO transRecharge) throws Exception;

	/**  
	* 方法说明: 更新合买交易记录（没有合买成功，当充值）
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param orderInfo
	* @param buyTogetherAmount
	* @throws Exception
	* @time: 2018年4月28日 下午5:40:03
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyBuyTogetherToRecharge(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, OrderInfoBO orderInfo, Double buyTogetherAmount, OrderGroupBO orderGroup, OrderGroupContentBO orderGroupContentBO) throws Exception;

	/**  
	* 方法说明: 合买本地支付，添加交易记录
	* @auth: xiongJinGang
	* @param payParam
	* @param orderInfo
	* @param toPayEndTimeVO
	* @param needBuyTogetherAmount
	* @param orderGroup
	* @throws RuntimeException
	* @throws Exception
	* @time: 2018年5月3日 下午4:48:22
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyBuyTogetherForLocal(PayParamVO payParam, OrderInfoBO orderInfo, ToPayEndTimeVO toPayEndTimeVO, Double needBuyTogetherAmount, OrderGroupBO orderGroup, OrderGroupContentBO orderGroupContentBO) throws RuntimeException, Exception;

	/**  
	* 方法说明: 未满员，平台垫起90%-100%部分，平台垫钱，扣平台账户的钱，然后加交易记录
	* @auth: xiongJinGang
	* @param orderInfo
	* @param orderGroupVO
	* @param orderGroup
	* @throws RuntimeException
	* @throws Exception
	* @time: 2018年5月4日 下午3:27:32
	* @return: ResultBO<?> 
	*/
	ResultBO<?> modifyPlatformGuarantee(OrderInfoBO orderInfo, OrderGroupVO orderGroupVO, OrderGroupBO orderGroup) throws RuntimeException, Exception;

	UserWalletBO modifyQueryUserWallet(Integer userId);

}
