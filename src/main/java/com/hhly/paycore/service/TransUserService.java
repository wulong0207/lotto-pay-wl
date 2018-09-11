package com.hhly.paycore.service;

import java.util.List;

import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderCopyPayInfoBO;
import com.hhly.skeleton.pay.agent.vo.TransferAccountsVO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.channel.bo.ChannelRechargeBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.TransUserVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.task.order.vo.OrderChannelVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;

/**
 * @author xiongjingang
 * @version 1.0
 * @desc 用户交易接口
 * @date 2017年3月3日 上午10:32:26
 * @company 益彩网络科技公司
 */
public interface TransUserService {
	/**
	 * 方法说明: 批量添加购彩交易流水
	 * @param orderTotalList  订单列表
	 * @param payNotifyResult 支付回调
	 * @param userWalletPO    账户总金额
	 * @param transRecharge   充值交易记录
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年5月11日 下午6:54:36
	 * @return: List<TransUserPO>
	 */
	List<TransUserPO> addGouCaiTransRecordBatch(List<PayOrderBaseInfoVO> orderTotalList, PayNotifyResultVO payNotifyResult, UserWalletPO userWalletPO, TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明: 添加重置开奖交易流水
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param needSubAmount
	* @param userWalletPO
	* @throws Exception
	* @time: 2017年9月8日 上午11:25:15
	* @return: TransUserPO 
	*/
	TransUserPO addOrderResetRecord(OrderInfoBO orderInfoBO, Double needSubAmount, UserWalletPO userWalletPO) throws Exception;

	/**  
	* 方法说明: 生成彩金红包交易流水【做活动时调用】
	* @auth: xiongJinGang
	* @param transRecharge
	* @param totalCashBalance
	* @param totalRedBalance
	* @param operateCouponPO
	* @throws Exception
	* @time: 2017年11月8日 下午3:16:23
	* @return: TransUserPO 
	*/
	TransUserPO addActivityTransRecord(TransRechargeBO transRecharge, Double totalCashBalance, Double totalRedBalance, OperateCouponPO operateCouponPO) throws Exception;

	/**
	 * 方法说明: 添加交易记录
	 *
	 * @param transRecharge
	 * @param payNotifyResult
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年3月27日 下午3:45:18
	 * @return: TransUserPO
	 */
	TransUserPO addTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, UserWalletPO userWalletPO, String orderInfo) throws Exception;

	/**
	 * 方法说明: 添加交易记录
	 *
	 * @param transUserPO
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年5月31日 下午5:10:46
	 * @return: int
	 */
	int addTransRecord(TransUserPO transUserPO) throws Exception;

	/**  
	* 方法说明: 添加交易流水记录（添加彩金红包，红包作废等）【供CMS端人工充值用】
	* @auth: xiongJinGang
	* @param urap
	* @param transStatus 交易状态 
	* @throws Exception
	* @time: 2017年11月8日 下午3:25:20
	* @return: TransUserPO 
	*/
	TransUserPO addTransUser(UserRedAddParamVo urap) throws Exception;

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
	* 方法说明: 生成用户交易流水记录（80%、20%、中奖金额、红包金额各消费多少）
	* @auth: xiongJinGang
	* @param pcw
	* @throws Exception
	* @time: 2017年7月14日 下午6:53:08
	* @return: TransUserPO 
	*/
	TransUserPO addTransUserRecord(PayChildWalletVO pcw, TransRechargeBO transRecharge) throws Exception;

	/**  
	* 方法说明: 根据交易编号和交易状态，获取交易流水
	* @auth: xiongJinGang
	* @param tradeNo
	* @param tradeStatus
	* @throws Exception
	* @time: 2017年8月7日 下午3:25:45
	* @return: TransUserBO 
	*/
	TransUserBO findTransUserBy(String tradeNo, Short tradeStatus) throws Exception;

	/**  
	* 方法说明: 查询交易记录
	* @auth: xiongJinGang
	* @param transUser
	* @throws Exception
	* @time: 2017年7月15日 上午9:58:13
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findUserTransByOrderCode(TransUserVO transUser) throws Exception;

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
	 * 方法说明: 根据订单号[交易类型]获取用户的交易记录
	 *
	 * @param transUser
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年4月5日 下午5:08:53
	 * @return: ResultBO<?>
	 */
	ResultBO<?> findUserTransRecordByOrderCode(TransUserVO transUser) throws Exception;

	/**  
	* 方法说明: 生成彩金红包交易流水
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param transRechargeBO
	* @param uwp
	* @throws Exception
	* @time: 2017年8月21日 下午5:28:13
	* @return: void 
	*/
	TransUserPO addTransRecord(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception;

	/**  
	* 方法说明: 添加账户消费流水
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param transRechargeBO
	* @param uwp
	* @throws Exception
	* @time: 2017年8月21日 下午5:40:09
	* @return: void 
	*/
	TransUserPO addTransCostRecord(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception;

	/**  
	* 方法说明: 添加中奖交易流水
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param userId
	* @param redValue
	* @param redCode
	* @param aftBonus
	* @param userWalletPO
	* @throws Exception
	* @time: 2017年11月17日 下午4:17:23
	* @return: TransUserPO 
	*/
	TransUserPO addWinTransUser(OrderInfoBO orderInfoBO, Double redValue, String redCode, Double aftBonus, UserWalletPO userWalletPO) throws Exception;

	/**  
	* 方法说明: 添加官方加奖流水
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param aftBonus
	* @param totalRedBalance
	* @param totalCashBalance
	* @time: 2018年1月10日 上午11:40:18
	* @return: TransUserPO 
	* @throws Exception 
	*/
	TransUserPO addOfficialBonusTransUser(OrderInfoBO orderInfoBO, Double aftBonus, Double totalRedBalance, Double totalCashBalance) throws Exception;

	/**  
	* 方法说明: 本站加奖
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param totalRedBalance
	* @param totalCashBalance
	* @time: 2018年1月10日 上午11:51:37
	* @return: TransUserPO 
	* @throws Exception 
	*/
	TransUserPO addWebSiteBonusTransUser(OrderInfoBO orderInfoBO, Double totalRedBalance, Double totalCashBalance, OperateCouponBO operateCouponBO) throws Exception;

	/**  
	* 方法说明: 添加推单方案详情交易记录
	* @auth: xiongJinGang
	* @param orderCopyPayInfoBO
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年1月11日 下午5:41:26
	* @return: TransUserPO 
	*/
	TransUserPO addPushPayTransUser(OrderCopyPayInfoBO orderCopyPayInfoBO, UserWalletPO userWalletPO) throws Exception;

	/**  
	* 方法说明: 添加红包重置开奖交易流水
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param needSubRedAmount
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年1月17日 上午9:45:27
	* @return: TransUserPO 
	*/
	TransUserPO addOrderRedResetRecord(OrderInfoBO orderInfoBO, Double needSubRedAmount, UserWalletPO userWalletPO) throws Exception;

	/**  
	* 方法说明: 代理给用户转账交易流水
	* @auth: xiongJinGang
	* @param transferAccounts
	* @param memberInfo
	* @param updateAmount
	* @param rechargeCode 充值编号
	* @param operateCouponPO
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年3月5日 下午3:29:28
	* @return: TransUserPO 
	*/
	TransUserPO addTransRecord(TransferAccountsVO transferAccounts, UserInfoBO memberInfo, Double updateAmount, String rechargeCode, OperateCouponPO operateCouponPO, UserWalletPO userWalletPO) throws Exception;

	/**  
	* 方法说明: 添加扣除账户金额的流水
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param transRechargeBO
	* @param rechargeAmount
	* @param uwp
	* @time: 2018年3月6日 上午10:48:13
	* @return: TransUserPO 
	*/
	TransUserPO addActivityConsume(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception;

	/**  
	* 方法说明: 查询交易流水
	* @auth: xiongJinGang
	* @param tradeNo 交易编号
	* @param tradeStatus 交易状态
	* @param transType 交易类型
	* @throws Exception
	* @time: 2018年3月6日 下午3:12:05
	* @return: TransUserBO 
	*/
	TransUserBO getTransUserByType(String tradeNo, Short tradeStatus, Short transType) throws Exception;

	/**  
	* 方法说明: 添加合买订单保底金额交易记录
	* @auth: xiongJinGang
	* @param payNotifyResult
	* @param orderInfo
	* @param orderGroup
	* @param userWalletPOSubOne
	* @time: 2018年5月3日 下午4:08:35
	* @return: void 
	* @throws Exception 
	*/
	TransUserPO addOrderGroup(PayNotifyResultVO payNotifyResult, OrderInfoBO orderInfo, OrderGroupBO orderGroup, UserWalletPO userWalletPOSubOne) throws Exception;

	/**  
	* 方法说明: 获取合买订单交易记录明细
	* @auth: xiongJinGang
	* @param list
	* @param transType 交易类型(购彩)
	* @param transStatus 交易状态(成功)
	* @time: 2018年5月3日 下午6:04:44
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> findOrderGroupTransRecord(List<String> list, Short transType, Short transStatus);

	/**  
	* 方法说明: 保底金额的一部分转成认购金额，记录交易流水
	* @auth: xiongJinGang
	* @param buyCode
	* @param oldTransCode
	* @param channelId
	* @param buyAmount
	* @param UserWalletBO
	* @throws Exception
	* @time: 2018年5月19日 下午4:46:20
	* @return: TransUserPO 
	*/
	TransUserPO addSubscription(String buyCode, String oldTransCode, String channelId, Double buyAmount, UserWalletBO userWallet) throws Exception;

	/**  
	* 方法说明: 添加合买抽成交易流水记录
	* @auth: xiongJinGang
	* @param orderInfoBO
	* @param commissionAmount
	* @param userWalletPO
	* @throws Exception
	* @time: 2018年5月22日 下午12:04:39
	* @return: TransUserPO 
	*/
	TransUserPO addOrderGroupWinTransUser(OrderInfoBO orderInfoBO, Double commissionAmount, UserWalletPO userWalletPO) throws Exception;

	/**
	 * 查询渠道充值列表
	 * @author zhouyang
	 * @param vo
	 * @date 2018.6.8
	 * @return
	 */
	List<ChannelRechargeBO> findChannelTransRechargeList(OrderChannelVO vo);

	/**  
	* 方法说明: 更新
	* @auth: xiongJinGang
	* @param awardFlag
	* @param id
	* @throws Exception
	* @time: 2018年7月31日 下午12:06:02
	* @return: int 
	*/
	int updateAwardFlagById(Short awardFlag, Integer id) throws Exception;

}
