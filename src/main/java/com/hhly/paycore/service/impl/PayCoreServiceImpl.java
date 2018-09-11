package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.log4j.Logger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.OrderGroupUtil;
import com.hhly.paycore.common.PayCoreUtil;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.ThirdTransDetailPO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.OperateMarketChannelService;
import com.hhly.paycore.service.PayBankLimitService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.PayChannelMgrService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.paycore.service.ThirdTransDetailService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.BankStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.MoneyFlowEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayChannelEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.constants.RechargeConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderCopyPayInfoBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankLimitBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelLimitBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelMgrBO;
import com.hhly.skeleton.pay.channel.vo.ChannelParamVO;
import com.hhly.skeleton.pay.channel.vo.PayTypeResultVO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.OrderGroupVO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayResultVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 支付、充值核心类
 * @author xiongJinGang
 * @date 2017年7月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service
public class PayCoreServiceImpl implements PayCoreService {
	private static final Logger logger = Logger.getLogger(PayCoreServiceImpl.class);
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private PayBankLimitService payBankLimitService;
	@Resource
	private PayChannelService payChannelService;
	@Resource
	private PayChannelMgrService payChannelMgrService;
	@Resource
	private PayBankService payBankService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private ThirdTransDetailService thirdTransDetailService;
	@Resource
	private OperateMarketChannelService operateMarketChannelService;
	@Resource
	private RedissonClient redissonClient;
	@Value("${footboll.activity.code}")
	private String footbollActivityCode;// 竟足活动编号
	@Value("${basketboll.activity.code}")
	private String basketbollActivityCode;// 竟篮活动编号
	@Value("${redcent.activity.code}")
	private String redCentActivityCode;// 广东1分钱活动编号
	@Value("${redcentjx.activity.code}")
	private String redCentjxActivityCode;// 江西1分钱活动编号
	@Value("${pay.huayi.wechat.public}")
	private String wechatPublic;// 华移支付公众号支付指定的appID

	/*******余额、红包支付********/
	@Override
	public ResultBO<?> modifyBalanceAndStatusForLocal(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO, List<PayOrderBaseInfoVO> orderBaseList) throws RuntimeException, Exception {
		// 组装充值BO
		TransRechargeBO transRecharge = PayCoreUtil.packageTransRecharge(payParam, payParam.getUserId());
		// 没有使用第三方的支付，不需要添加充值记录

		// 1、添加活动红包
		Double colorRedAmount = addActivytyRed(transRecharge, true);// 使用彩金红包金额，如果不是彩金红包，账户中的红包余额不扣减

		// 2、使用了红包【更新红包余额、状态等信息】、添加红包使用记录
		StringBuffer remark = new StringBuffer();
		OperateCouponBO operateCouponBO = null;
		if (!ObjectUtil.isBlank(transRecharge.getRedCode())) {
			remark.append("红包");
			Short transType = TransTypeEnum.LOTTERY.getKey();// 购彩
			// 更新红包状态，添加红包使用记录
			ResultBO<?> resultBO = operateCouponService.updateRedInfo(transRecharge, transType, TransTypeEnum.LOTTERY.getValue());
			operateCouponBO = (OperateCouponBO) resultBO.getData();
			// 是彩金红包并且生成的彩金红包为空
			if (!ObjectUtil.isBlank(operateCouponBO) && operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey()) && ObjectUtil.isBlank(colorRedAmount)) {
				colorRedAmount = payParam.getUseRedAmount();
			}
		}

		// 3、更新账户中余额，彩金红包金额
		ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(payParam.getUserId(), payParam.getUseBalance(), MoneyFlowEnum.OUT.getKey(), colorRedAmount, MoneyFlowEnum.OUT.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		if (!ObjectUtil.isBlank(payParam.getUseBalance())) {
			remark.append("余额");
		}
		remark.append("支付");

		// 4、批量更新订单支付成功状态
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);// 支付成功
		payNotifyResult.setRemark(remark.toString());
		payOrderUpdateService.updateOrderBatch(orderBaseList, payNotifyResult, transRecharge);

		// 5、添加交易记录
		List<TransUserPO> transUserList = transUserService.addGouCaiTransRecordBatch(orderBaseList, payNotifyResult, userWalletPO, transRecharge);
		// 添加给用户查看的交易流水
		transUserLogService.addTransUserByBatch(transUserList);

		// 6、拼装支付结果
		return PayCoreUtil.packagePayResult(payParam, toPayEndTimeVO, orderBaseList, transRecharge, operateCouponBO);
	}

	/*******余额、红包支付推单信息********/
	@Override
	public ResultBO<?> modifyPushStatusForLocal(PayParamVO payParam) throws RuntimeException, Exception {
		// 组装充值BO
		OrderCopyPayInfoBO orderCopyPayInfoBO = payParam.getOrderCopyPayInfoBO();
		orderCopyPayInfoBO.setChannelId(payParam.getChannelId());
		// 推单者添加收入

		// 添加跟单者支出流水
		addPushPayOutTransUser(payParam, orderCopyPayInfoBO);
		// 6、拼装支付结果
		PayResultVO payResultVO = new PayResultVO();
		payResultVO.setPayStatus(PayStatusEnum.PAYMENT_SUCCESS.getKey());
		payResultVO.setPayAmount(orderCopyPayInfoBO.getPrice());// 充值金额
		return ResultBO.ok(payResultVO);
	}

	/**  
	* 方法说明: 添加推单者收入流水
	* @auth: xiongJinGang
	* @param payParam
	* @param orderCopyPayInfoBO
	* @throws Exception
	* @time: 2018年1月11日 下午6:14:41
	* @return: void 
	*/
	/*	private void addPushPayInTransUser(PayParamVO payParam, OrderCopyPayInfoBO orderCopyPayInfoBO) throws Exception {
			// 更新账户中余额，收入全部入80%账户
			ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(orderCopyPayInfoBO.getUserIssueId(), payParam.getUseBalance(), MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.EIGHTY_PERCENT.getKey());
			UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
			orderCopyPayInfoBO.setTransType(PayConstants.TransTypeEnum.REBATE.getKey());
			orderCopyPayInfoBO.setOrderInfo(Constants.ISSUE_PUSH_INCOME);
			// 5、添加交易记录
			TransUserPO transUserPO = transUserService.addPushPayTransUser(orderCopyPayInfoBO, userWalletPO);
			// 添加给用户查看的交易流水
			transUserLogService.addTransLogRecord(transUserPO);
			//
			ThirdTransDetailPO thirdTransDetailPO = new ThirdTransDetailPO();
			thirdTransDetailPO.setAmount(orderCopyPayInfoBO.getPrice());
			thirdTransDetailPO.setBalance(userWalletPO.getTotalCashBalance());// master 或者 guest账户余额
			thirdTransDetailPO.setGuestId(payParam.getUserId());// 跟单用户Id
			thirdTransDetailPO.setMasterId(orderCopyPayInfoBO.getUserIssueId());// 专家账户id
			thirdTransDetailPO.setOrderCode(orderCopyPayInfoBO.getIssueCode());
			thirdTransDetailPO.setRemark("推单收入");
			thirdTransDetailPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
			thirdTransDetailPO.setType(PayConstants.MoneyFlowEnum.IN.getKey());// 收入
			// 添加用户支付方案详情
			thirdTransDetailService.addUserDetail(thirdTransDetailPO);
		}*/

	/**  
	* 方法说明: 添加跟单者支出流水
	* @auth: xiongJinGang
	* @param payParam
	* @param orderCopyPayInfoBO
	* @throws Exception
	* @time: 2018年1月11日 下午6:13:50
	* @return: void 
	*/
	private void addPushPayOutTransUser(PayParamVO payParam, OrderCopyPayInfoBO orderCopyPayInfoBO) throws Exception {
		// 更新账户中余额
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(payParam.getUserId(), payParam.getUseBalance(), MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_WINNING.getKey());
		orderCopyPayInfoBO.setTransType(PayConstants.TransTypeEnum.REBATE.getKey());
		orderCopyPayInfoBO.setOrderInfo(Constants.ISSUE_PUSH_INCOME);
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		// 5、添加交易记录
		TransUserPO transUserPO = transUserService.addPushPayTransUser(orderCopyPayInfoBO, userWalletPO);
		// 添加给用户查看的交易流水
		transUserLogService.addTransLogRecord(transUserPO);

		//
		ThirdTransDetailPO thirdTransDetailPO = new ThirdTransDetailPO();
		thirdTransDetailPO.setAmount(orderCopyPayInfoBO.getPrice());
		thirdTransDetailPO.setBalance(userWalletPO.getTotalCashBalance());// master 或者 guest账户余额
		thirdTransDetailPO.setGuestId(payParam.getUserId());// 跟单用户Id
		thirdTransDetailPO.setMasterId(orderCopyPayInfoBO.getUserIssueId());// 专家账户id
		thirdTransDetailPO.setOrderCode(orderCopyPayInfoBO.getIssueCode());
		thirdTransDetailPO.setRemark("推单收入");
		thirdTransDetailPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		thirdTransDetailPO.setType(PayConstants.MoneyFlowEnum.IN.getKey());// 收入
		// 添加用户支付方案详情
		thirdTransDetailService.addUserDetail(thirdTransDetailPO);
	}

	/**  
	* 方法说明: 批量支付，【多个订单合并支付，只有一条充值流水，一条充值交易记录，多条购彩记录】
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param list 批量支付的订单号信息
	* @throws Exception
	* @time: 2017年5月11日 下午3:33:41
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("unchecked")
	@Override
	public ResultBO<?> modifyPaySuccessTransRecordForBatch(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, List<PayOrderBaseInfoVO> list) throws Exception {
		// 充值流水中活动编号不为空，并且竞足活动编号 或者 1分钱活动编号不为空
		Double needAddRedAmount = addActivytyRed(transRecharge, false);// 需要添加的彩金红包金额

		// 1、更新充值记录（仅一条）
		// 验证充值状态是否已更新
		ResultBO<?> resultBO = checkTransRechargeStatus(transRecharge.getTransRechargeCode());
		if (resultBO.isError()) {
			return resultBO;
		}
		transRechargeService.updateRechargeTrans(transRecharge, payNotifyResult, PayConstants.RedTypeEnum.RED_COLOR.getKey());
		transRecharge.setArrivalAmount(payNotifyResult.getOrderAmt());// 到账金额
		// 批量获取订单信息
		resultBO = payOrderUpdateService.findOrderAndValidate(list);
		if (resultBO.isError()) {
			return resultBO;
		}
		// 订单列表
		List<PayOrderBaseInfoVO> orderTotalList = (List<PayOrderBaseInfoVO>) resultBO.getData();

		// 2、给用户钱包账户加款（按80%、20%比例分配）
		resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), payNotifyResult.getOrderAmt(), MoneyFlowEnum.IN.getKey(), needAddRedAmount, PayConstants.MoneyFlowEnum.IN.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		// 取银行信息
		PayBankBO payBankBO = payBankService.findBankFromCache(transRecharge.getRechargeBank());
		String remark = PayUtil.getRechargeRemark(transRecharge, payBankBO);// 充值描述【招行储蓄卡:尾号888支付】
		// 3、添加充值的交易流水记录（仅一条）
		transUserService.addTransRecord(transRecharge, payNotifyResult, userWalletPO, remark);

		Double redAmount = 0.0;// 彩金红包金额
		// 验证是否使用红包
		if (!ObjectUtil.isBlank(transRecharge.getRedCode())) {
			// 处理红包状态，添加红包使用记录
			Short transType = TransTypeEnum.LOTTERY.getKey();// 购彩
			resultBO = operateCouponService.updateRedInfo(transRecharge, transType, TransTypeEnum.LOTTERY.getValue());
			// 数据库中没有查询到红包信息，将充值记录中的红包编号
			if (resultBO.isOK()) {
				OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();
				// 如果是彩金红包，需要修改账户红包余额记录
				if (!ObjectUtil.isBlank(operateCouponBO) && operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
					redAmount = transRecharge.getRedAmount();// 使用的红包金额，钱包中需要减去减金额
				}
			}
		}
		// 更新最后一次使用银行卡号和Id
		updateDefaultBank(transRecharge);

		// 判断订单是否可以支付
		PayResultVO payResultVO = Constants.validateOrderPayEndTime(orderTotalList);
		// 需要支付的订单
		List<PayOrderBaseInfoVO> successList = payResultVO.getOrderSuccessList();
		// 支付状态不是待支付和支付中订单
		List<PayOrderBaseInfoVO> failList = payResultVO.getOrderFailList();
		// 已过支付时间
		List<PayOrderBaseInfoVO> passList = payResultVO.getOrderPassList();

		// 可以正常支付的订单
		if (!ObjectUtil.isBlank(successList)) {
			// 更新订单支付状态，钱包余额及流水
			updateOrderAndWallet(transRecharge, redAmount, successList, payNotifyResult, payResultVO, userWalletPO.getTotalCashBalance());
		}

		// 已过支付时间，更新订单支付状态为支付失败
		if (!ObjectUtil.isBlank(passList)) {
			for (PayOrderBaseInfoVO payOrderBaseInfoVO : passList) {
				logger.info("订单【" + payOrderBaseInfoVO.getOrderCode() + "】已过支付截止时间【" + payOrderBaseInfoVO.getEndSysTime() + "】");
			}
		}

		// 支付状态错误
		if (!ObjectUtil.isBlank(failList)) {
			for (PayOrderBaseInfoVO payOrderBaseInfoVO : passList) {
				logger.info("订单【" + payOrderBaseInfoVO.getOrderCode() + "】当前支付状态【" + payOrderBaseInfoVO.getPayStatus() + "】错误");
			}
		}

		return ResultBO.ok(payResultVO);
	}

	/**  
	* 方法说明: 做活动时，首先给账户送一个红包，然后添加交易流水
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年11月9日 下午5:39:37
	* @return: Double 
	*/
	public Double addActivytyRed(TransRechargeBO transRecharge, boolean isAddRedAmount) throws Exception {
		Double needAddRedAmount = 0d;
		String activityCode = transRecharge.getActivityCode();
		// 如果是活动订单
		if (!ObjectUtil.isBlank(activityCode)) {
			logger.info("【" + transRecharge.getTransRechargeCode() + "】参数活动：" + activityCode);
			// 满足以下活动时，才需要加红包，其它活动不加
			if (activityCode.equals(footbollActivityCode) || activityCode.equals(basketbollActivityCode) || activityCode.equals(redCentActivityCode) || activityCode.equals(redCentjxActivityCode)) {
				OperateCouponBO operateCouponBO = new OperateCouponBO(PayConstants.RedTypeEnum.RED_COLOR.getValue(), transRecharge.getActivityCode(), transRecharge.getUserId(), PayConstants.RedSourceEnum.ACTIVITY.getKey(), Constants.ACTIVITY_SEND);
				OperateCouponPO operateCouponPO = operateCouponService.addRedColor(operateCouponBO, transRecharge.getRedAmount());
				transRecharge.setRedCode(operateCouponPO.getRedCode());// 红包编号
				needAddRedAmount = transRecharge.getRedAmount();
				// 2、 添加红包交易流水
				transRedService.addRedTransRecord(operateCouponPO, transRecharge);
				// 本地支付时，需要先插入红包金额
				if (isAddRedAmount) {
					// 先加彩金红包，后面再减彩金红包
					userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getRedAmount(), MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
				}
				// 3、添加彩金红包生成记录
				try {
					UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(transRecharge.getUserId());
					TransUserPO transUserPO = transUserService.addActivityTransRecord(transRecharge, userWalletBO.getTotalCashBalance(), userWalletBO.getEffRedBalance(), operateCouponPO);
					// 添加给用户查看的交易流水
					transUserLogService.addTransLogRecord(transUserPO);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return needAddRedAmount;
	}

	/**  
	* 方法说明: 更新订单支付状态，钱包余额及流水
	* @auth: xiongJinGang
	* @param transRecharge
	* @param redAmount
	* @param orderTotalList
	* @param payNotifyResult
	* @throws Exception
	* @time: 2017年6月14日 下午5:44:03
	* @return: PayResultVO 
	*/
	public void updateOrderAndWallet(TransRechargeBO transRecharge, Double redAmount, List<PayOrderBaseInfoVO> successList, PayNotifyResultVO payNotifyResult, PayResultVO payResultVO, Double totalAmount) throws Exception {
		if (!ObjectUtil.isBlank(successList)) {
			// 验证账户余额是否够支付，扣除用户钱包中的金额
			ResultBO<?> resultBO = PayUtil.validateUserWalletBalance(totalAmount, payResultVO.getOrderAmount(), transRecharge);
			// 钱包中的总现金金额不够支付，账户不扣钱，订单状态不更新，购彩交易流水不添加，方案进度不添加。以下操作是钱包中的金额大于订单总金额
			if (resultBO.isOK()) {
				// 验证订单的支付状态是否为未支付、支付中的，存在需要改订单状态和添加交易流水
				Double needSubCashAmount = MathUtil.sub(payResultVO.getOrderAmount(), transRecharge.getRedAmount());// 需要扣减的现金总金额，要扣除使用了红包的金额
				resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), needSubCashAmount, MoneyFlowEnum.OUT.getKey(), redAmount, PayConstants.MoneyFlowEnum.OUT.getKey());

				// 5、批量更新订单支付成功状态，红包编号
				payOrderUpdateService.updateOrderBatch(successList, payNotifyResult, transRecharge);
				UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
				// 6、批量添加购彩交易流水记录
				List<TransUserPO> transUserList = transUserService.addGouCaiTransRecordBatch(successList, payNotifyResult, userWalletPO, transRecharge);
				// 添加给用户看的交易流水
				transUserLogService.addTransUserByBatch(transUserList);
			} else {
				logger.info("账户余额【" + totalAmount + "】不够【" + payResultVO.getOrderAmount() + "】支付，更新【" + transRecharge.getOrderCode() + "】订单支付状态，钱包余额及流水失败。");
			}
		} else {
			logger.info("没有需要更新成功的订单");
		}
	}

	/**  
	* 方法说明: 更新失败记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param payAttachVO
	* @throws Exception
	* @time: 2017年4月26日 下午2:37:10
	* @return: ResultBO<?> 
	*/
	@SuppressWarnings("unchecked")
	@Override
	public ResultBO<?> modifyFailTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, List<PayOrderBaseInfoVO> list) throws Exception {
		PayResultVO payResultVO = new PayResultVO();
		// 1、更新充值记录
		transRechargeService.updateRechargeTrans(transRecharge, payNotifyResult, PayConstants.RedTypeEnum.RED_COLOR.getKey());
		// 2、获取订单信息
		ResultBO<?> resultBO = payOrderUpdateService.findOrderAndValidate(list);
		if (resultBO.isError()) {
			logger.error("获取订单【" + transRecharge.getOrderCode() + "】详情失败。" + resultBO.getMessage());
			return resultBO;
		}
		// 订单列表
		List<PayOrderBaseInfoVO> orderTotalList = (List<PayOrderBaseInfoVO>) resultBO.getData();
		if (!ObjectUtil.isBlank(orderTotalList)) {
			for (int i = orderTotalList.size() - 1; i <= 0; i--) {
				PayOrderBaseInfoVO payOrderBaseInfo = orderTotalList.get(i);
				// 支付状态是等待支付状态，才修改
				if (!payOrderBaseInfo.getPayStatus().equals(PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey())) {
					logger.info("充值编号【" + transRecharge.getTransRechargeCode() + "】对应的订单【" + payOrderBaseInfo.getOrderCode() + "】当前支付状态【" + payOrderBaseInfo.getPayStatus() + "】不是支付中，不修改支付状态");
					orderTotalList.remove(i);
				}
			}
		}
		if (!ObjectUtil.isBlank(orderTotalList)) {
			payResultVO.setOrderFailList(orderTotalList);
			// 4、更新订单支付失败状态
			payOrderUpdateService.updateOrderBatch(orderTotalList, payNotifyResult, transRecharge);
		}
		return ResultBO.ok(payResultVO);
	}

	/**  
	* 方法说明: list转换（支付成功的订单）
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年5月13日 下午6:18:41
	* @return: List<PayOrderBaseInfoVO> 
	*/
	@Override
	public List<PayOrderBaseInfoVO> transOrder(List<OrderBaseInfoBO> list, Integer userId) throws Exception {
		List<PayOrderBaseInfoVO> payOrderList = new ArrayList<PayOrderBaseInfoVO>();
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		for (OrderBaseInfoBO orderBaseInfo : list) {
			payOrderBaseInfoVO = new PayOrderBaseInfoVO(orderBaseInfo);
			payOrderBaseInfoVO.setLotteryCode(orderBaseInfo.getLotteryCode());
			payOrderBaseInfoVO.setUserId(userId);// 设置用户ID
			payOrderBaseInfoVO.setPayStatus(Short.parseShort(String.valueOf(orderBaseInfo.getPayStatus())));// 转成支付成功
			payOrderList.add(payOrderBaseInfoVO);
		}
		return payOrderList;
	}

	/**  
	* 方法说明: 获取支付限额表内容并且验证
	* @auth: xiongJinGang
	* @param bankId
	* @time: 2017年4月7日 下午12:03:06
	* @return: PayBankLimitBO 
	*/
	@Override
	public ResultBO<?> findPayBankLimitAndValidate(PayBankcardBO payBankcardBO, PayParamVO payParam) {
		PayBankLimitBO payBankLimit = payBankLimitService.findPayBankLimitByBankIdFromCache(payBankcardBO.getBankid(), payBankcardBO.getBanktype());
		return PayUtil.checkPayLimit(payParam, payBankLimit);
	}

	/**  
	* 方法说明: 根据订单号和登录token，获取用户交易成功的充值记录
	* @auth: xiongJinGang
	* @param refundParam
	* @param userInfo
	* @time: 2017年5月3日 下午12:16:29
	* @return: TransRechargeBO 
	*/
	@Override
	public TransRechargeBO findRechargeRecord(RefundParamVO refundParam) {
		TransRechargeVO transRecharge = new TransRechargeVO(refundParam);
		transRecharge.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功的
		return transRechargeService.findByTransRecharge(transRecharge);
	}

	@Override
	public ResultBO<?> modifyRechargeSuccessTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception {
		PayResultVO payResultVO = new PayResultVO();
		payResultVO.setPayStatus(PayStatusEnum.PAYMENT_SUCCESS.getKey());
		payResultVO.setPayAmount(transRecharge.getRechargeAmount());// 充值金额

		Double rate = 0d;// 充值费率
		PayChannelBO payChannelBO = payChannelService.findChannelByIdUseCache(transRecharge.getPayChannelId());
		// 支付渠道不为空，并且收手续费，手续费率不为0，手续费要除以100转成%多少的类型
		if (!ObjectUtil.isBlank(payChannelBO) && !ObjectUtil.isBlank(payChannelBO.getCharge()) && !ObjectUtil.isBlank(payChannelBO.getRate())) {
			rate = MathUtil.div(payChannelBO.getRate(), 100d);
		}

		// 服务费=到账金额*服务费率
		Double serviceCharge = MathUtil.mul(payNotifyResult.getOrderAmt(), rate);
		// 到账金额=充值金额-服务费
		Double arrivalAmount = MathUtil.sub(payNotifyResult.getOrderAmt(), serviceCharge);
		transRecharge.setArrivalAmount(arrivalAmount);
		transRecharge.setServiceCharge(serviceCharge);

		// 验证充值状态是否已更新
		ResultBO<?> resultBO = checkTransRechargeStatus(transRecharge.getTransRechargeCode());
		if (resultBO.isError()) {
			return resultBO;
		}

		transRechargeService.updateRechargeTrans(transRecharge, payNotifyResult, PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey());
		// 2、账户钱包中添加到账金额，已扣除手续费
		resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), arrivalAmount, PayConstants.MoneyFlowEnum.IN.getKey(), 0d, PayConstants.MoneyFlowEnum.IN.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		// 设置总余额
		payResultVO.setTotalAmount(userWalletPO.getTotalAmount());
		// 3、添加充值的交易记录
		PayBankBO payBankBO = payBankService.findBankFromCache(transRecharge.getRechargeBank());
		String remark = PayUtil.getRechargeRemark(transRecharge, payBankBO);// 充值描述【招行储蓄卡:尾号888支付】
		// 添加交易记录
		TransUserPO transUserPO = transUserService.addTransRecord(transRecharge, payNotifyResult, userWalletPO, remark);

		// 更新最后一次使用银行卡号和Id
		updateDefaultBank(transRecharge);

		// 使用了红包，才进行下面操作
		if (!ObjectUtil.isBlank(transRecharge.getRedCode())) {
			Short transType = TransTypeEnum.RECHARGE.getKey();
			// 更新红包状态，添加红包使用记录
			resultBO = operateCouponService.updateRedInfo(transRecharge, transType, Constants.RED_REMARK_RECHARGE_USE);

			// 4、红包状态不对，不使用红包，需要把充值记录中的红包信息更新
			if (resultBO.getErrorCode().equals(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE)) {
				transRecharge.setRedAmount(0.0);
				transRecharge.setRedCode("");
				int num = transRechargeService.updateRechargeTransRedInfo(transRecharge);
				logger.info("充值流水编号【" + transRecharge.getTransRechargeCode() + "】的红包状态不对，不使用红包，更新充值记录中的红包编号和红包金额为空返回：" + (num > 0 ? "成功" : "失败"));
			}
			logger.debug("修改红包使用状态成功");
			// 5、使用了充值红包，需要添加彩金红包
			if (resultBO.isOK()) {
				logger.debug("交易号【" + transRecharge.getTransRechargeCode() + "】使用了红包【" + transRecharge.getRedCode() + "】，需要将红包生成彩金");
				// 添加彩金红包
				OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();

				// 使用红包不为空，并且是充值红包，要给钱包账户中的可用红包中加钱，添加红包交易记录
				if (!ObjectUtil.isBlank(operateCouponBO) && operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey())) {
					// 原有的可用红包+充多少+送多少
					Double addRedAmount = MathUtil.add(operateCouponBO.getMinSpendAmount(), operateCouponBO.getRedValue());// 彩金红包金额，充100送20
					OperateCouponPO operateCouponPO = operateCouponService.addRedColor(operateCouponBO, addRedAmount);
					payResultVO.setRedCode(operateCouponPO.getRedCode());// 新生成的红包编号

					Double needSubAmount = Double.parseDouble(operateCouponBO.getMinSpendAmount() + "");
					// 这个充值金额是根据充值红包来的，充100送20，就会有120做为彩金红包
					resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), needSubAmount, PayConstants.MoneyFlowEnum.OUT.getKey(), addRedAmount, PayConstants.MoneyFlowEnum.IN.getKey());
					UserWalletPO uwp = (UserWalletPO) resultBO.getData();

					/************ 添加购买彩金红包交易流水。**************/
					PayChildWalletVO payChildWallet = PayCoreUtil.packageBuyRedTransRecord(transRecharge, uwp);
					payChildWallet.setTradeAmount(needSubAmount);
					payChildWallet.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余总红包金额（这里要使用加红包金额之前的）
					TransUserPO transUserPOSub = transUserService.addTransUserRecord(payChildWallet, transRecharge);

					/************  生成彩金红包交易记录**************/
					operateCouponBO.setRedCode(operateCouponPO.getRedCode());
					operateCouponBO.setRedType(operateCouponPO.getRedType());
					transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), transType, needSubAmount, addRedAmount, Constants.RED_REMARK_RECHARGE_INFO, transRecharge.getOrderCode());

					// 设置总余额
					payResultVO.setTotalAmount(uwp.getTotalAmount());
					// 添加彩金红包生成交易流水
					TransUserPO transUserPOAdd = transUserService.addActivityTransRecord(transRecharge, uwp.getTotalCashBalance(), uwp.getEffRedBalance(), operateCouponPO);

					// 合并充值记录到一条，供前端展示，取的是现金交易金额
					Double leaveAmount = MathUtil.sub(transUserPO.getCashAmount(), transUserPOSub.getCashAmount());// 充值金额减去买红包所用的金额
					if (MathUtil.compareTo(leaveAmount, 0d) < 0) {
						leaveAmount = 0d;
					}
					Double showAmount = MathUtil.add(leaveAmount, transUserPOAdd.getRedTransAmount());// 展示金额 = 买红包剩余金额+红包到账金额
					transUserPOAdd.setCashAmount(leaveAmount);//
					Double serviceAmount = ObjectUtil.isBlank(transUserPO.getServiceCharge()) ? 0d : transUserPO.getServiceCharge();// 手续费
					showAmount = MathUtil.sub(showAmount, serviceAmount);
					transUserPOAdd.setTransAmount(showAmount);// 需要给前端展示的金额不包括手续费，这里需要减除
					transUserPOAdd.setOrderInfo(transUserPO.getOrderInfo());
					transUserPOAdd.setRemark(transUserPO.getRemark());
					transUserPOAdd.setTransType(transType);
					transUserLogService.addTransLogRecord(transUserPOAdd);
				}
			}
		} else {
			// 完成了充值，但是没有使用红包，需要给用户添加充值流水【给用户查看的】
			transUserLogService.addTransLogRecord(transUserPO);
		}
		return ResultBO.ok(payResultVO);
	}

	@Override
	public ResultBO<?> checkTransRechargeStatus(String transRechargeCode) {
		// 1、更新充值记录
		TransRechargeBO transRechargeBo = transRechargeService.findRechargeByTransCode(transRechargeCode);
		if (ObjectUtil.isBlank(transRechargeBo)) {
			logger.info("获取充值流水【" + transRechargeCode + "】的充值记录失败，无法更新充值记录和添加交易记录");
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		if (transRechargeBo.getTransStatus().equals(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey())) {
			logger.info("充值流水【" + transRechargeCode + "】的状态已成功，不能重复添加");
			return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE);
		}
		return ResultBO.ok(transRechargeBo);
	}

	@Override
	public ResultBO<?> modifyFailTransRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception {
		// 1、更新充值记录
		transRechargeService.updateRechargeTrans(transRecharge, payNotifyResult, PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey());
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> modifyUserWalletCash(TransRechargePO transRecharge, CmsRechargeVO cmsRecharge) throws Exception {
		logger.debug("开始添加充值流水");
		ResultBO<?> resultBO = transRechargeService.addRechargeTrans(transRecharge);
		if (resultBO.isError()) {
			return resultBO;
		}
		logger.debug("开始更新用户钱包");
		resultBO = userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getArrivalAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.EIGHTY_PERCENT.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		TransRechargeBO transRechargeBO = new TransRechargeBO();
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
		BeanUtils.copyProperties(transRechargeBO, transRecharge);
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
		payNotifyResult.setThirdTradeNo(transRecharge.getThirdTransNum());
		logger.debug("开始添加交易流水");
		transRechargeBO.setTradeType(cmsRecharge.getTradeType());
		TransUserPO transUserPO = transUserService.addTransRecord(transRechargeBO, payNotifyResult, userWalletPO, Constants.RECHARGE_REMARK_INFO);
		// 添加供用户展示的交易流水
		transUserLogService.addTransLogRecord(transUserPO);
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> modifyUserWalletRed(TransRechargePO transRecharge, CmsRechargeVO cmsRecharge) throws Exception {
		logger.debug("开始生成彩金红包");
		OperateCouponBO operateCouponBO = new OperateCouponBO();
		operateCouponBO.setUserId(transRecharge.getUserId());
		operateCouponBO.setActivityCode("CMS");
		transRecharge.setChannelId(PayConstants.ChannelEnum.PC.getKey());
		// 添加彩金红包
		OperateCouponPO operateCouponPO = operateCouponService.addRedColor(operateCouponBO, transRecharge.getRechargeAmount());
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
		BeanUtils.copyProperties(operateCouponBO, operateCouponPO);
		// 添加红包交易记录
		logger.debug("添加红包生成记录");
		transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), TransTypeEnum.RECHARGE.getKey(), 0d, transRecharge.getRechargeAmount(), Constants.ACTIVITY_SEND, transRecharge.getOrderCode());
		logger.debug("开始更新用户钱包");
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getArrivalAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		if (resultBO.isError()) {
			throw new RuntimeException("更新用户【" + transRecharge.getUserId() + "】彩金红包金额失败");
		}
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		logger.debug("添加红包交易记录");
		transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), TransTypeEnum.ACTIVITY_GIVE.getKey(), transRecharge.getRechargeAmount(), 0d, Constants.ACTIVITY_SEND, transRecharge.getOrderCode());
		// 添加彩金红包交易流水
		addRedColorTransUser(transRecharge, operateCouponPO, userWalletPO, cmsRecharge);
		return ResultBO.ok(operateCouponBO);
	}

	/**  
	* 方法说明: 添加彩金红包交易流水
	* @auth: xiongJinGang
	* @param transRecharge
	* @param operateCouponPO
	* @param userWalletPO
	* @throws Exception
	* @time: 2017年11月10日 上午11:04:54
	* @return: void 
	*/
	public void addRedColorTransUser(TransRechargePO transRecharge, OperateCouponPO operateCouponPO, UserWalletPO userWalletPO, CmsRechargeVO cmsRecharge) throws Exception {
		UserRedAddParamVo userRedAddParam = new UserRedAddParamVo();
		userRedAddParam.setUserId(transRecharge.getUserId());// 用户ID
		userRedAddParam.setRedAmount(transRecharge.getRechargeAmount());// 添加红包金额
		userRedAddParam.setRedCode(operateCouponPO.getRedCode());// 红包编号
		Short transType = TransTypeEnum.ACTIVITY_GIVE.getKey();
		if (!ObjectUtil.isBlank(cmsRecharge.getTradeType())) {
			transType = cmsRecharge.getTradeType();
		}
		userRedAddParam.setTransType(transType);// 交易类型
		userRedAddParam.setOrderInfo(Constants.ACTIVITY_SEND);// 描述
		userRedAddParam.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易状态
		userRedAddParam.setTransAmount(transRecharge.getRechargeAmount());// 交易金额
		userRedAddParam.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
		userRedAddParam.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余现金总金额
		userRedAddParam.setChannelId(transRecharge.getChannelId());
		TransUserPO transUserPO = transUserService.addTransUser(userRedAddParam);
		// 添加供用户展示的交易流水
		transUserLogService.addTransLogRecord(transUserPO);
	}

	@Override
	public ResultBO<?> modifyUserWalletRedForAgent(TransRechargePO transRecharge) throws Exception {
		logger.debug("开始生成彩金红包");
		OperateCouponBO operateCouponBO = new OperateCouponBO();
		operateCouponBO.setUserId(transRecharge.getUserId());
		operateCouponBO.setActivityCode(transRecharge.getActivityCode());// 活动编号
		operateCouponBO.setRedCode(transRecharge.getChannelId());
		// transRecharge.setChannelId(PayConstants.ChannelEnum.PC.getKey());
		OperateCouponPO operateCouponPO = operateCouponService.addAgentRedColor(operateCouponBO, transRecharge.getRechargeAmount());
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
		BeanUtils.copyProperties(operateCouponBO, operateCouponPO);
		logger.debug("添加红包生成记录");
		transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), TransTypeEnum.RECHARGE.getKey(), 0d, transRecharge.getRechargeAmount(), Constants.CHANNEL_RECHARGE, transRecharge.getOrderCode());
		logger.debug("开始更新用户钱包");
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getArrivalAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		if (resultBO.isError()) {
			throw new RuntimeException("更新用户【" + transRecharge.getUserId() + "】彩金红包金额失败");
		}
		logger.debug("添加用户交易记录");
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		TransRechargeBO transRechargeBO = new TransRechargeBO();
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
		BeanUtils.copyProperties(transRechargeBO, transRecharge);
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
		payNotifyResult.setThirdTradeNo(transRecharge.getThirdTransNum());
		transRechargeBO.setTransRechargeCode(null);// 设置为空，不记录tradeCode，防止前端通过这个字段跳转到充值记录详情中去
		transRechargeBO.setRedCode(operateCouponPO.getRedCode());// 设置红包编号，在交易流水中关联
		TransUserPO transUserPO = transUserService.addTransRecord(transRechargeBO, payNotifyResult, userWalletPO, Constants.CHANNEL_RECHARGE);
		transUserLogService.addTransLogRecord(transUserPO);
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> modifyUserWalletCashForAgent(TransRechargePO transRecharge) throws Exception {
		// 添加充值记录
		transRechargeService.addRechargeTrans(transRecharge);
		logger.debug("开始更新用户钱包");
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getArrivalAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_PERCENT_RATE.getKey());
		if (resultBO.isError()) {
			throw new RuntimeException("更新用户【" + transRecharge.getUserId() + "】彩金红包金额失败");
		}
		logger.debug("添加用户交易记录");
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		TransRechargeBO transRechargeBO = new TransRechargeBO();
		ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
		BeanUtils.copyProperties(transRechargeBO, transRecharge);
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);
		payNotifyResult.setThirdTradeNo(transRecharge.getThirdTransNum());
		TransUserPO transUserPO = transUserService.addTransRecord(transRechargeBO, payNotifyResult, userWalletPO, Constants.AGENT_RECHARGE);
		transUserLogService.addTransLogRecord(transUserPO);
		return ResultBO.ok();
	}

	/**
	* 方法说明: 清除缓存
	* @auth: xiongJinGang
	* @param userInfoBO
	* @time: 2017年5月26日 下午3:53:00
	* @return: void 
	*/
	private void clearUserBankCache(Integer userId) {
		redisUtil.delAllString(CacheConstants.P_CORE_USER_PAY_CHANNEL + userId);
		redisUtil.delAllString(CacheConstants.P_CORE_USER_BANK_CARD_LIST + userId);
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_BANK_CHANNEL_SINGLE);
	}

	/**  
	* 方法说明: 更新最后一次使用银行并清除缓存
	* @auth: xiongJinGang
	* @param transRecharge
	* @throws Exception
	* @time: 2017年11月9日 下午5:16:02
	* @return: void 
	*/
	private void updateDefaultBank(TransRechargeBO transRecharge) throws Exception {
		bankcardService.updateDefaultBank(transRecharge);// 更新默认银行
		userInfoService.updateLastBankCard(transRecharge);// 更新最后一次使用记录
		clearUserBankCache(transRecharge.getUserId());// 清除缓存
		// 用户支付方式缓存清除
		redisUtil.delAllString(CacheConstants.P_CORE_USER_PAY_CHANNEL + transRecharge.getUserId());
		redisUtil.delAllString(CacheConstants.P_CORE_USER_BANK_CARD_LIST + transRecharge.getUserId());
	}

	@Override
	public Double addDayLimitAmount(Integer channelId, Short payType, Short cardType, Double amount) {
		String lockKey = PayUtil.getDayLimitKey(CacheConstants.P_CORE_LOCK, channelId, payType, cardType);
		RLock lock = redissonClient.getLock(lockKey);
		Double totalAmount = amount;
		try {
			// 尝试加锁，最多等待3秒，上锁以后5秒自动解锁
			boolean isLock = lock.tryLock(3, 5, TimeUnit.SECONDS);
			if (isLock) { // 成功
				totalAmount = redisUtil.addOrUpdate(lockKey, amount, CacheConstants.TWO_DAYS);
			}
		} catch (InterruptedException e) {
			logger.error("执行分布式锁：" + lockKey + "异常", e);
		} finally {
			lock.unlock();
		}
		return totalAmount;
	}

	@Override
	public Double addDayLimitAmount(TransRechargeBO transRecharge) {
		if (!ObjectUtil.isBlank(transRecharge) && transRecharge.getChannelCode().contains("_")) {
			String channelCode = transRecharge.getChannelCode().substring(0, transRecharge.getChannelCode().indexOf("_"));
			PayChannelMgrBO payChannelMgrBO = payChannelMgrService.findChannelMgrByCode(channelCode);

			// 支付方式，不是网银和快捷，全都是第三方支付
			Short payType = transRecharge.getPayType();
			if (!payType.equals(PayConstants.PayTypeEnum.BANK_PAYMENT.getKey()) && !payType.equals(PayConstants.PayTypeEnum.QUICK_PAYMENT.getKey())) {
				payType = PayConstants.PayTypeEnum.THIRD_PAYMENT.getKey();
			}
			Double totalAmount = MathUtil.add(transRecharge.getArrivalAmount(), transRecharge.getServiceCharge());
			String lockKey = PayUtil.getDayLimitKey(CacheConstants.P_CORE_LOCK, payChannelMgrBO.getId(), payType, transRecharge.getBankCardType());
			RLock lock = redissonClient.getLock(lockKey);
			try {
				// 尝试加锁，最多等待3秒，上锁以后5秒自动解锁
				boolean isLock = lock.tryLock(3, 5, TimeUnit.SECONDS);
				if (isLock) { // 成功
					String key = PayUtil.getDayLimitKey(CacheConstants.P_CORE_CHANNEL_LOCK, payChannelMgrBO.getId(), payType, transRecharge.getBankCardType());
					totalAmount = redisUtil.addOrUpdate(key, totalAmount, CacheConstants.TWO_DAYS);
				}
			} catch (InterruptedException e) {
				logger.error("执行分布式锁：" + lockKey + "异常", e);
			} finally {
				lock.unlock();
			}
			return totalAmount;
		}
		return null;
	}

	/**  
	* 方法说明: 获取当日限额
	* @auth: xiongJinGang
	* @param channelId
	* @param payType
	* @param cardType
	* @time: 2017年12月14日 下午4:28:35
	* @return: Double 
	*/
	private Double getDayLimitAmount(Integer channelId, Short payType, Short cardType) {
		String key = PayUtil.getDayLimitKey(CacheConstants.P_CORE_CHANNEL_LOCK, channelId, payType, cardType);
		Double dayLimit = redisUtil.getObj(key, Double.class);
		return ObjectUtil.isBlank(dayLimit) ? 0d : dayLimit;
	}

	@Override
	public ResultBO<?> getPayChannel(ChannelParamVO channelParam) {
		Double payAmount = channelParam.getPayAmount();
		PayBankBO payBankBO = channelParam.getPayBankBO();
		PayBankcardBO payBankcardBO = channelParam.getPayBankcardBO();

		// 判断当前渠道是否为马甲包渠道
		boolean isMajia = operateMarketChannelService.isMajia(channelParam.getChannelId());

		// 当前支付的平台
		TakenPlatformEnum takenPlatformEnum = PayConstants.TakenPlatformEnum.getByKey(channelParam.getPlatform());
		logger.info(takenPlatformEnum.getValue() + "选择支付渠道，使用银行：" + payBankBO.getName() + "，支付金额：" + payAmount);
		// 不是第三方支付，就是银行卡支付
		if (!payBankBO.getPayType().equals(PayConstants.PayBankPayTypeEnum.THIRD.getKey()) && ObjectUtil.isBlank(payBankcardBO)) {
			// 如果银行卡为空，返回错误
			logger.error("选择了银行支付，但是银行卡为空");
			return ResultBO.err(MessageCodeConstants.PAY_BANKCARD_NOT_FOUND_SERVICE);
		}
		PayTypeResultVO payTypeResultVO = null;
		Double leastAmount = 0d;// 单笔最低限额
		Double highestAmount = 0d;// 单笔最高限额
		Double dayLeastAmount = 0d;// 当日最低限额

		// 所有可用支付渠道列表
		for (PayChannelBO payChannelBO : channelParam.getPayChannelList()) {
			// 检查支付渠道是否可用，是否在维护等
			if (!checkPayChannel(payChannelBO)) {
				continue;
			}
			// 验证支付渠道对当前平台是否可用
			boolean platResult = getPlatFormStatus(takenPlatformEnum.getPlatForm(), payChannelBO, isMajia);
			if (!platResult) {
				continue;
			}

			// 判断用户选择的支付类型是银行卡支付还是第三方支付，如果是第三方支付
			if (payBankBO.getPayType().equals(PayConstants.PayBankPayTypeEnum.THIRD.getKey())) {
				// 获取第三方的支付渠道
				payTypeResultVO = getThirdPayType(payChannelBO, takenPlatformEnum, payBankBO, payAmount, isMajia, channelParam.getAppId());
			} else {
				// 获取网银、快捷支付渠道
				payTypeResultVO = getBankPayType(payChannelBO, takenPlatformEnum, payBankcardBO, payAmount, isMajia);
			}
			if (!ObjectUtil.isBlank(payTypeResultVO)) {
				// 验证支付渠道当日最低、最高支付限额是否满足支付金额，不满足时，继续选择下一个支付渠道
				if (MathUtil.compareTo(payAmount, payChannelBO.getMinPay()) < 0 || MathUtil.compareTo(payAmount, payChannelBO.getMaxPay()) > 0) {
					logger.info("支付金额【" + payAmount + "】不满足当前支付渠道【" + payChannelBO.getName() + "】单笔限额【" + payChannelBO.getMinPay() + "-" + payChannelBO.getMaxPay() + "】，继续获取下一个渠道");
					// 获取最低支付金额
					if (MathUtil.compareTo(leastAmount, 0) == 0 || MathUtil.compareTo(payChannelBO.getMinPay(), leastAmount) < 0) {
						leastAmount = payChannelBO.getMinPay();
					}
					// 获取最高支付金额
					if (MathUtil.compareTo(highestAmount, 0) == 0 || MathUtil.compareTo(payChannelBO.getMaxPay(), highestAmount) > 0) {
						highestAmount = payChannelBO.getMaxPay();
					}
					payTypeResultVO = null;
					continue;
				}
				// 验证当天限额（当月支付限额现在先不检验）
				Map<String, PayChannelLimitBO> limitMap = payChannelBO.getLimitMap();
				if (!ObjectUtil.isBlank(limitMap)) {
					String mapKey = payChannelBO.getType() + "_" + payChannelBO.getCardType();
					if (limitMap.containsKey(mapKey)) {
						PayChannelLimitBO channelLimitBO = limitMap.get(mapKey);
						if (!ObjectUtil.isBlank(channelLimitBO)) {
							Double dayLimit = channelLimitBO.getLimitday();// 当前支付渠道的限额
							Double nowLimit = getDayLimitAmount(payChannelBO.getPayChannelMgrId(), payChannelBO.getType(), payChannelBO.getCardType());// redis中存的当天该渠道的支付限额
							Double newDayLimit = MathUtil.add(nowLimit, payAmount);// 当前支付金额+redis中已消费金额
							/*************这里如果超过了当日限额，可以将渠道设置成维护中，目前没做，渠道当日交易金额不可能太低*****************/

							// 当前支付金额+redis中已消费金额 大于该渠道当日限额，继续查找下一个
							if (MathUtil.compareTo(newDayLimit, dayLimit) >= 0) {
								logger.info("当前支付金额：" + payAmount + "加上redis中该渠道已消费金额：" + nowLimit + " 大于该渠道当日限额：" + dayLimit + "，继续获取下一个渠道");
								if (MathUtil.compareTo(dayLeastAmount, 0) == 0 || MathUtil.compareTo(channelLimitBO.getLimitday(), dayLeastAmount) < 0) {
									dayLeastAmount = channelLimitBO.getLimitday();
								}
								payTypeResultVO = null;
								continue;
							}
						}
					}
				}
				break;
			}
		}
		String transTypeName = PayConstants.RechargeTypeEnum.getEnum(channelParam.getTransType());
		if (ObjectUtil.isBlank(payTypeResultVO)) {
			if (MathUtil.compareTo(leastAmount, 0) > 0 && MathUtil.compareTo(leastAmount, payAmount) > 0) {
				logger.info(transTypeName + "金额：" + payAmount + "小于渠道最低交易金额：" + leastAmount);
				return ResultBO.err(MessageCodeConstants.PAY_MONEY_LESS_THAN_LIMIT, transTypeName, leastAmount);
			}
			if (MathUtil.compareTo(highestAmount, 0) > 0 && MathUtil.compareTo(highestAmount, payAmount) < 0) {
				logger.info(transTypeName + "金额：" + payAmount + "高于渠道最高交易金额：" + highestAmount);
				return ResultBO.err(MessageCodeConstants.PAY_MONEY_HIGHER_THAN_LIMIT, transTypeName, highestAmount);
			}
			if (MathUtil.compareTo(dayLeastAmount, 0) > 0) {
				logger.info(transTypeName + "金额：" + payAmount + "加上redis中当前渠道已用金额高于渠道当日最高交易金额：" + dayLeastAmount);
				return ResultBO.err(MessageCodeConstants.PAY_CHANNEL_IS_REPAIRING, transTypeName, highestAmount);
			}
			logger.info(takenPlatformEnum.getValue() + "未获取到支付渠道");
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}
		return ResultBO.ok(payTypeResultVO);
	}

	/**  
	* 方法说明: 获取第三方支付渠道
	* @auth: xiongJinGang
	* @param payChannelBO
	* @param takenPlatformEnum
	* @param payBankBO
	* @param map
	* @time: 2017年12月13日 上午10:29:15
	* @return: boolean 
	*/
	private PayTypeResultVO getThirdPayType(PayChannelBO payChannelBO, TakenPlatformEnum takenPlatformEnum, PayBankBO payBankBO, Double payAmount, boolean isMajia, String appId) {
		PayTypeResultVO payTypeResultVO = null;
		// 渠道支付类型和卡类型都是第三方支付
		if (payChannelBO.getCardType().equals(PayConstants.PayChannelCardTypeEnum.THIRD.getKey()) && payChannelBO.getType().equals(PayConstants.PayTypeEnum.THIRD_PAYMENT.getKey())) {
			// 获取第三方支付方式编号 10：支付宝 11：微信支付 12：QQ钱包支付 13：充值卡支付 14：京东支付
			String payType = RechargeConstants.getThirdPayType(payBankBO);
			if (ObjectUtil.isBlank(payType)) {
				logger.error("未获取到银行【" + payBankBO.getcName() + "】第三方支付的支付类型");
				return payTypeResultVO;
			}
			String payTypeName = payChannelBO.getCode() + "_";// 支付名称后缀
			PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByType(payChannelBO.getCode());
			switch (payChannelEnum) {
			case DIVINEPAY_RECHARGE:// 神州支付
				// 充值卡支付
				if (payType.equals(PayConstants.PayTypeThirdEnum.RECHARGE_CARD_PAYMENT.getKey())) {
					payTypeName += "CARD";
				}
			case HUAYI_RECHARGE:// 华移支付，只支持指定公众号的支付
				// 只有公众号支付，并且appId一致，才能用华移的公众号支付
				if (takenPlatformEnum.equals(PayConstants.TakenPlatformEnum.JSAPI)) {
					if (!ObjectUtil.isBlank(wechatPublic) && !ObjectUtil.isBlank(appId) && !wechatPublic.equals(appId)) {
						return payTypeResultVO;
					}
				}
			default:
				payTypeName = getPayTypeName(takenPlatformEnum, payTypeName, payChannelBO, payType);
				break;
			}
			payTypeResultVO = new PayTypeResultVO(payChannelBO.getId(), payType, payTypeName, payChannelBO.getCode());
		}
		return payTypeResultVO;
	}

	/**  
	* 方法说明: 获取储蓄卡、快捷支付的渠道
	* @auth: xiongJinGang
	* @param payChannelBO
	* @param takenPlatformEnum
	* @param payBankcardBO
	* @param payAmount
	* @time: 2017年12月13日 下午2:49:41
	* @return: PayTypeResultVO 
	*/
	private PayTypeResultVO getBankPayType(PayChannelBO payChannelBO, TakenPlatformEnum takenPlatformEnum, PayBankcardBO payBankcardBO, Double payAmount, boolean isMajia) {
		PayTypeResultVO payTypeResultVO = null;
		// 银行不是第三方，但渠道是第三方，跳过
		if (payChannelBO.getType().equals(PayTypeEnum.THIRD_PAYMENT.getKey())) {
			return payTypeResultVO;
		}
		// 用银行卡支付
		Short bankType = payBankcardBO.getBanktype();// 银行卡类型:1储蓄卡;2信用卡
		Short cardType = payChannelBO.getCardType();// 卡类型:1储蓄卡;2信用卡,3第三方支付
		Short openBank = payBankcardBO.getOpenbank();// BandCardQuickEnum
		boolean result = false;
		// 判断银行卡是储蓄卡还是信用卡，如果都为银行卡，再判断支付类型
		if (bankType.equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey()) && cardType.equals(PayConstants.PayChannelCardTypeEnum.BANK_CARD.getKey())) {
			result = getPayChannel(payBankcardBO, payChannelBO, takenPlatformEnum, isMajia);
		}
		// 信用卡
		if (bankType.equals(PayConstants.BankCardTypeEnum.CREDIT.getKey()) && cardType.equals(PayConstants.PayChannelCardTypeEnum.CREDIT.getKey())) {
			result = getPayChannel(payBankcardBO, payChannelBO, takenPlatformEnum, isMajia);
		}
		if (result) {
			String payType = getPayType(bankType, openBank);// 支付类型
			String payTypeName = payChannelBO.getCode();
			// 如果是易宝支付，不能用平台来区分，只能通过支付类型
			if (PayConstants.PayChannelEnum.YEEPAY_RECHARGE.getType().equals(payChannelBO.getCode())) {
				payTypeName = getPayTypeName(takenPlatformEnum, openBank, payTypeName, isMajia, payChannelBO);
			} else if (PayConstants.PayChannelEnum.LIANLIAN_RECHARGE.getType().equals(payChannelBO.getCode())) {
				// 连连支付
				// payTypeName += takenPlatformEnum.getType();
				payTypeName = getPayTypeName(takenPlatformEnum, openBank, payTypeName, isMajia, payChannelBO);
			} else {
				payTypeName += "_" + takenPlatformEnum.getType();
			}
			payTypeResultVO = new PayTypeResultVO(payChannelBO.getId(), payType, payTypeName, payChannelBO.getCode());
		}
		return payTypeResultVO;
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param takenPlatformEnum
	* @param openBank
	* @param payTypeName
	* @return
	* @time: 2017年12月13日 下午2:40:32
	* @return: String 
	*/
	private String getPayTypeName(TakenPlatformEnum takenPlatformEnum, Short openBank, String payTypeName, Boolean isMajia, PayChannelBO payChannelBO) {
		Short appInvokeType = payChannelBO.getAppInvokeType();// app调用:1调用sdk，0调用h5
		switch (takenPlatformEnum) {
		case WEB:
			// 连连支付，如果开通了快捷，跳转到快捷
			if (openBank.equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey())) {
				// 连连快捷支付
				payTypeName += "_FAST";
			} else {
				// 没有开通快捷，跳转到网银
				payTypeName += "_WEB";
			}
			break;
		case WAP:
			payTypeName += "_" + takenPlatformEnum.getType();
			break;
		case IOS:
		case ANDROID:
			// 是马甲包并且配置的是走appsdk
			if (isMajia) {
				payTypeName += "_" + PayConstants.TakenPlatformEnum.WAP.getType();
			} else {
				// 如果为空，调用H5进行支付
				if (ObjectUtil.isBlank(appInvokeType)) {
					payTypeName += "_" + PayConstants.TakenPlatformEnum.WAP.getType();
				} else {
					payTypeName += "_" + takenPlatformEnum.getType();
				}
			}
			break;
		default:
			break;
		}
		return payTypeName;
	}

	/**  
	* 方法说明: 获取支付方式名称(2018-08-07屏蔽，所有h5和app走扫码)
	* @auth: xiongJinGang
	* @param takenPlatformEnum
	* @time: 2017年11月23日 下午5:11:28
	*/
	/*	private String getPayTypeName(TakenPlatformEnum takenPlatformEnum, String payTypeName, PayChannelBO payChannelBO) {
			switch (takenPlatformEnum) {
			case WEB:
				payTypeName += TakenPlatformEnum.WEB.getType();
				break;
			case WAP:
				payTypeName += TakenPlatformEnum.WAP.getType();
				break;
			case JSAPI:
				payTypeName += TakenPlatformEnum.JSAPI.getType();
				break;
			case ANDROID:
			case IOS:
				// app调用类型不为空并且配置的是调用sdk
				if (!ObjectUtil.isBlank(payChannelBO.getAppInvokeType()) && PayConstants.AppPayTypeEnum.SDK.getKey().equals(payChannelBO.getAppInvokeType())) {
					payTypeName += TakenPlatformEnum.IOS.getType();
				} else {
					// 默认走wap
					payTypeName += TakenPlatformEnum.WAP.getType();
				}
				break;
			default:
				break;
			}
			return payTypeName;
		}*/

	/**  
	* 方法说明: 所有h5和app走扫码
	* @auth: xiongJinGang
	* @param takenPlatformEnum
	* @param payTypeName
	* @param payChannelBO
	* @time: 2018年8月7日 下午2:46:29
	* @return: String 
	*/
	private String getPayTypeName(TakenPlatformEnum takenPlatformEnum, String payTypeName, PayChannelBO payChannelBO, String payType) {
		switch (takenPlatformEnum) {
		case WEB:
			payTypeName += TakenPlatformEnum.WEB.getType();
			break;
		case WAP:
			if (payType.equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
				payTypeName += TakenPlatformEnum.WEB.getType();
			} else {
				payTypeName += TakenPlatformEnum.WAP.getType();
			}
			break;
		case JSAPI:
			payTypeName += TakenPlatformEnum.JSAPI.getType();
			break;
		case ANDROID:
		case IOS:
			if (payType.equals(PayConstants.PayTypeThirdEnum.ALIPAY_PAYMENT.getKey())) {
				payTypeName += TakenPlatformEnum.WEB.getType();
			} else {
				// app调用类型不为空并且配置的是调用sdk
				if (!ObjectUtil.isBlank(payChannelBO.getAppInvokeType()) && PayConstants.AppPayTypeEnum.SDK.getKey().equals(payChannelBO.getAppInvokeType())) {
					payTypeName += TakenPlatformEnum.IOS.getType();
				} else {
					// 默认走wap
					payTypeName += TakenPlatformEnum.WAP.getType();
				}
			}
			break;
		default:
			break;
		}
		return payTypeName;
	}

	/**  
	* 方法说明: 检查支付渠道是否可用
	* @auth: xiongJinGang
	* @param payChannelBO
	* @time: 2017年10月11日 下午12:06:08
	* @return: boolean 
	*/
	private boolean checkPayChannel(PayChannelBO payChannelBO) {
		// 支付渠道可用并且没有暂停
		if (!payChannelBO.getAvailable().equals(BankStatusEnum.OPEN.getKey())) {
			return false;
		}
		// 如果支付渠道为可用，判断是否有暂停，有暂停，判断当前时间在不在暂停时间内
		if (payChannelBO.getPause().equals(BankStatusEnum.OPEN.getKey())) {
			Date beginTime = payChannelBO.getBeginTime();
			Date endTime = payChannelBO.getEndTime();
			if (ObjectUtil.isBlank(beginTime) || ObjectUtil.isBlank(endTime)) {
				return true;
			}
			Date nowTime = new Date();
			int num1 = DateUtil.compare(nowTime, beginTime);// 第一个大于第二个，返回1，否则返回小于0
			int num2 = DateUtil.compare(nowTime, endTime);
			// 在暂停时间内，不能使用
			if (num1 >= 0 && num2 <= 0) {
				return false;
			}
		}
		return true;
	}

	/**  
	* 方法说明: 获取银行卡支付方式
	* @auth: xiongJinGang
	* @param bankType
	* @param openBank
	* @time: 2017年9月9日 上午9:52:06
	* @return: void 
	*/
	private String getPayType(Short bankType, Short openBank) {
		String payType = PayConstants.PayTypeThirdEnum.BANK_DEBIT_CARD_PAYMENT.getKey();
		if (openBank.equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey())) {
			if (bankType.equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
				payType = PayConstants.PayTypeThirdEnum.QUICK_DEBIT_CARD_PAYMENT.getKey();// 网银快捷支付
			} else {
				payType = PayConstants.PayTypeThirdEnum.QUICK_CREDIT_CARD_PAYMENT.getKey();// 信用卡快捷支付
			}
		} else {
			if (bankType.equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
				payType = PayConstants.PayTypeThirdEnum.BANK_DEBIT_CARD_PAYMENT.getKey();// 网银快捷支付
			} else {
				payType = PayConstants.PayTypeThirdEnum.BANK_CREDIT_CARD_PAYMENT.getKey();// 信用卡快捷支付
			}
		}
		return payType;
	}

	private boolean getPayChannel(PayBankcardBO payBankcardBO, PayChannelBO payChannelBO, TakenPlatformEnum takenPlatformEnum, boolean isMajia) {
		boolean result = false;
		Short isQuickPay = payBankcardBO.getOpenbank();// 是否开启快捷支付 0：否，1：是
		Short channelType = payChannelBO.getType();// 渠道支付类型:1网银支付;2快捷支付,3第三方支付
		// 都为快捷支付
		if (isQuickPay.equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey()) && channelType.equals(PayConstants.PayTypeEnum.QUICK_PAYMENT.getKey())) {
			result = true;
		}
		// 储蓄卡支付
		if (isQuickPay.equals(PayConstants.BandCardQuickEnum.NOT_OPEN.getKey()) && channelType.equals(PayConstants.PayTypeEnum.BANK_PAYMENT.getKey())) {
			result = true;
		}
		if (result) {
			result = getPlatFormStatus(takenPlatformEnum.getPlatForm(), payChannelBO, isMajia);
		}
		return result;
	}

	/**  
	* 方法说明: 判断平台是否支持
	* @auth: xiongJinGang
	* @param takenPlatformEnum
	* @param payChannelBO
	* @time: 2017年4月10日 上午11:11:21
	* @return: boolean 
	*/
	private boolean getPlatFormStatus(String platform, PayChannelBO payChannelBO, boolean isMajia) {
		Short appInvokeType = payChannelBO.getAppInvokeType();// app调用:1调用sdk，0调用h5
		boolean result = false;
		switch (platform) {
		case "PC":
			result = payChannelBO.getPc().compareTo((short) 0) > 0;
			break;
		case "ANDROID":
			// 如果是马甲包并且配置了走sdk，返回false;
			if (isMajia && !ObjectUtil.isBlank(appInvokeType)) {
				return false;
			} else {
				/*if (ObjectUtil.isBlank(appInvokeType)) {
					result = payChannelBO.getAndroid().compareTo((short) 0) > 0;
					// android没有配置，不走h5
					if (!result) {
						result = payChannelBO.getH5().compareTo((short) 0) > 0;
					}
				} else {
					result = payChannelBO.getAndroid().compareTo((short) 0) > 0;
				}*/
				result = payChannelBO.getAndroid().compareTo((short) 0) > 0;
			}
			break;
		case "IOS":
			// 如果是马甲包并且配置了走sdk，返回false;
			if (isMajia && !ObjectUtil.isBlank(appInvokeType)) {
				return false;
			} else {
				/*if (ObjectUtil.isBlank(appInvokeType)) {
					result = payChannelBO.getIos().compareTo((short) 0) > 0;
					// ios没有配置，不走h5
					if (!result) {
						result = payChannelBO.getH5().compareTo((short) 0) > 0;
					}
				} else {
					result = payChannelBO.getIos().compareTo((short) 0) > 0;
				}*/
				result = payChannelBO.getIos().compareTo((short) 0) > 0;
			}
			break;
		case "H5":
		case "WAP":
			result = payChannelBO.getH5().compareTo((short) 0) > 0;
			break;
		case "WECHAT":
			Short wechat = ObjectUtil.isBlank(payChannelBO.getWechat()) ? 0 : payChannelBO.getWechat();
			result = wechat.compareTo((short) 0) > 0;
			break;
		default:
			break;
		}
		return result;
	}

	@Override
	public ResultBO<?> modifyBuyTogetherToRecharge(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, OrderInfoBO orderInfo, Double buyTogetherAmount, OrderGroupBO orderGroup, OrderGroupContentBO orderGroupContentBO) throws Exception {
		// 验证充值状态是否已更新
		ResultBO<?> resultBO = checkTransRechargeStatus(transRecharge.getTransRechargeCode());
		if (resultBO.isError()) {
			return resultBO;
		}
		// 先进行常规充值
		UserWalletPO userWalletPO = addRechargeRecord(transRecharge, payNotifyResult);

		addOrderGroupRecord(transRecharge, payNotifyResult, orderInfo, buyTogetherAmount, orderGroup, userWalletPO.getTotalCashBalance(), orderGroupContentBO);
		// 更新最后一次使用银行卡号和Id
		updateDefaultBank(transRecharge);
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @param orderInfo
	* @param buyTogetherAmount
	* @param orderGroup
	* @param userWalletPO
	* @throws Exception
	* @throws RuntimeException
	* @time: 2018年5月3日 下午4:42:42
	* @return: void 
	*/
	public void addOrderGroupRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult, OrderInfoBO orderInfo, Double buyTogetherAmount, OrderGroupBO orderGroup, Double totalCashBalance, OrderGroupContentBO orderGroupContentBO)
			throws Exception, RuntimeException {
		// 需要参与合买的金额
		if (MathUtil.compareTo(buyTogetherAmount, 0d) > 0) {
			// 钱包账户中扣除购彩金额去参与合买

			// 验证账户余额是否够支付，扣除用户钱包中的金额
			ResultBO<?> resultBO = PayUtil.validateUserWalletBalance(totalCashBalance, buyTogetherAmount, transRecharge);
			if (resultBO.isOK()) {
				// 拼装订单信息
				List<PayOrderBaseInfoVO> orderList = getOrderList(orderInfo, buyTogetherAmount);
				// 等待支付或者是支付中的订单，修改成支付成功
				if (PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey().intValue() == orderInfo.getPayStatus().intValue() || PayConstants.PayStatusEnum.BEING_PAID.getKey().intValue() == orderInfo.getPayStatus().intValue()) {
					// 更新订单支付状态
					payOrderUpdateService.updateOrderBatch(orderList, payNotifyResult, transRecharge);
				}

				// 如果orderGroup 不为空，是发起合买支付；发起合买，orderGroup 不为空，认购则为空
				if (!ObjectUtil.isBlank(orderGroup)) {
					UserWalletPO userWalletPOSubOne = null;
					// 发起合买的保底金额大于0，才添加记录
					if (MathUtil.compareTo(orderGroup.getGuaranteeAmount(), 0) > 0) {
						// 先扣除保底金额并添加交易流水
						resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), orderGroup.getGuaranteeAmount(), MoneyFlowEnum.OUT.getKey(), 0d, PayConstants.MoneyFlowEnum.OUT.getKey());
						if (resultBO.isError()) {
							logger.info("扣减订单【" + orderInfo.getOrderCode() + "】保底金额【" + orderGroup.getGuaranteeAmount() + "】失败");
							throw new RuntimeException();
						}
						userWalletPOSubOne = (UserWalletPO) resultBO.getData();
						// 发起合买的交易记录，要分开记录2条；一条是保底交易记录，一条是认购记录
						TransUserPO transUserPO = transUserService.addOrderGroup(payNotifyResult, orderInfo, orderGroup, userWalletPOSubOne);
						// 添加给用户看的交易流水
						transUserLogService.addTransLogRecord(transUserPO);
						buyTogetherAmount = MathUtil.sub(buyTogetherAmount, orderGroup.getGuaranteeAmount());
					}
				}

				if (MathUtil.compareTo(buyTogetherAmount, 0d) > 0) {
					// 购彩
					resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), buyTogetherAmount, MoneyFlowEnum.OUT.getKey(), 0d, PayConstants.MoneyFlowEnum.OUT.getKey());
					if (resultBO.isError()) {
						logger.info("扣减订单【" + orderInfo.getOrderCode() + "】认购金额【" + buyTogetherAmount + "】失败");
						throw new RuntimeException();
					}

					UserWalletPO userWalletPOSubTwo = (UserWalletPO) resultBO.getData();
					// 批量添加购彩交易流水记录
					orderInfo.setOrderCode(orderGroupContentBO.getBuyCode());
					List<PayOrderBaseInfoVO> orderList2 = getOrderList(orderInfo, buyTogetherAmount);
					List<TransUserPO> transUserList = transUserService.addGouCaiTransRecordBatch(orderList2, payNotifyResult, userWalletPOSubTwo, transRecharge);
					// 添加给用户看的交易流水
					transUserLogService.addTransUserByBatch(transUserList);
				} else {
					logger.info("订单【" + transRecharge.getOrderCode() + "】需要购彩金额为：" + buyTogetherAmount + "，不需要操作钱包和交易记录。");
				}
			} else {
				logger.info("账户余额【" + totalCashBalance + "】不够【" + buyTogetherAmount + "】支付，更新【" + transRecharge.getOrderCode() + "】订单支付状态，钱包余额及流水失败。");
			}
		}
	}

	/**  
	* 方法说明: 获取订单对象
	* @auth: xiongJinGang
	* @param orderInfo
	* @param buyTogetherAmount
	* @time: 2018年5月3日 下午4:18:22
	* @return: List<PayOrderBaseInfoVO> 
	*/
	public List<PayOrderBaseInfoVO> getOrderList(OrderInfoBO orderInfo, Double buyTogetherAmount) {
		List<PayOrderBaseInfoVO> orderList = new ArrayList<>();
		PayOrderBaseInfoVO payOrderBaseInfoVO = new PayOrderBaseInfoVO();
		payOrderBaseInfoVO.setOrderCode(orderInfo.getOrderCode());
		payOrderBaseInfoVO.setOrderAmount(buyTogetherAmount);
		payOrderBaseInfoVO.setLotteryName(orderInfo.getLotteryName());
		payOrderBaseInfoVO.setBuyType(orderInfo.getBuyType().intValue());
		payOrderBaseInfoVO.setLotteryIssue(orderInfo.getLotteryIssue());
		orderList.add(payOrderBaseInfoVO);
		return orderList;
	}

	/**  
	* 方法说明: 添加充值记录
	* @auth: xiongJinGang
	* @param transRecharge
	* @param payNotifyResult
	* @throws Exception
	* @time: 2018年4月28日 下午4:21:28
	* @return: void 
	*/
	public UserWalletPO addRechargeRecord(TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception {
		// 更新充值记录
		transRecharge.setArrivalAmount(payNotifyResult.getOrderAmt());// 到账金额
		transRechargeService.updateRechargeTrans(transRecharge, payNotifyResult, PayConstants.RedTypeEnum.RED_COLOR.getKey());

		// 2、给用户钱包账户加款（按80%、20%比例分配）
		ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(transRecharge.getUserId(), payNotifyResult.getOrderAmt(), MoneyFlowEnum.IN.getKey(), 0d, PayConstants.MoneyFlowEnum.IN.getKey());
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		// 取银行信息
		PayBankBO payBankBO = payBankService.findBankFromCache(transRecharge.getRechargeBank());
		String remark = PayUtil.getRechargeRemark(transRecharge, payBankBO);// 充值描述【招行储蓄卡:尾号888支付】
		// 3、添加充值的交易流水记录（仅一条）
		transUserService.addTransRecord(transRecharge, payNotifyResult, userWalletPO, remark);
		return userWalletPO;
	}

	@Override
	public ResultBO<?> modifyBuyTogetherForLocal(PayParamVO payParam, OrderInfoBO orderInfo, ToPayEndTimeVO toPayEndTimeVO, Double buyTogetherAmount, OrderGroupBO orderGroup, OrderGroupContentBO orderGroupContentBO)
			throws RuntimeException, Exception {
		// 组装充值BO
		TransRechargeBO transRecharge = PayCoreUtil.packageTransRecharge(payParam, payParam.getUserId());

		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);// 支付成功
		payNotifyResult.setRemark("余额支付");
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(payParam.getUserId());
		addOrderGroupRecord(transRecharge, payNotifyResult, orderInfo, buyTogetherAmount, orderGroup, userWalletBO.getTotalCashBalance(), orderGroupContentBO);
		// 6、拼装支付结果
		List<PayOrderBaseInfoVO> orderList = getOrderList(orderInfo, buyTogetherAmount);
		return PayCoreUtil.packageGroupPayResult(payParam, toPayEndTimeVO, orderList, transRecharge, null);
	}

	@Override
	public ResultBO<?> modifyPlatformGuarantee(OrderInfoBO orderInfo, OrderGroupVO orderGroupVO, OrderGroupBO orderGroup) throws RuntimeException, Exception {
		// 平台账户的金额
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(orderGroupVO.getUserId());
		// 验证账户余额是否够支付，扣除用户钱包中的金额
		ResultBO<?> resultBO = OrderGroupUtil.validateUserWalletBalance(userWalletBO.getTotalCashBalance(), orderGroupVO.getBuyAmount());
		if (resultBO.isError()) {
			logger.info("账户余额【" + userWalletBO.getTotalCashBalance() + "】不够【" + orderGroupVO.getBuyAmount() + "】支付，不能进行保底。");
			return resultBO;
		}

		// 购彩
		resultBO = userWalletService.updateUserWalletCommon(orderGroupVO.getUserId(), orderGroupVO.getBuyAmount(), MoneyFlowEnum.OUT.getKey(), 0d, PayConstants.MoneyFlowEnum.OUT.getKey());
		if (resultBO.isError()) {
			logger.info("扣减订单【" + orderInfo.getOrderCode() + "】认购金额【" + orderGroupVO.getBuyAmount() + "】失败");
			throw new RuntimeException();
		}

		UserWalletPO userWalletPOSubTwo = (UserWalletPO) resultBO.getData();
		// 拼装支付成功对象
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		payNotifyResult.setStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS);// 支付成功
		payNotifyResult.setRemark("余额支付");

		// 拼装充值请求参数
		TransRechargeBO transRecharge = PayCoreUtil.packageTransRecharge(orderInfo.getChannelId(), orderGroupVO.getUserId());
		transRecharge.setTransRechargeCode(orderGroupVO.getBuyCode());
		// 拼装订单对象
		orderInfo.setOrderCode(orderGroupVO.getBuyCode());// 交易流水中的订单编号，设置成合买详情中的编号
		List<PayOrderBaseInfoVO> orderList = getOrderList(orderInfo, orderGroupVO.getBuyAmount());
		List<TransUserPO> transUserList = transUserService.addGouCaiTransRecordBatch(orderList, payNotifyResult, userWalletPOSubTwo, transRecharge);
		// 添加给用户看的交易流水
		transUserLogService.addTransUserByBatch(transUserList);
		return ResultBO.ok(orderGroupVO);
	}

	@Override
	public UserWalletBO modifyQueryUserWallet(Integer userId) {
		return userWalletService.findUserWalletByUserId(userId);
	}
}
