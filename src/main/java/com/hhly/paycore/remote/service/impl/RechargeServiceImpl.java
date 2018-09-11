package com.hhly.paycore.remote.service.impl;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.PayCommon;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.jms.CloseRechargeDelay;
import com.hhly.paycore.jms.DelayService;
import com.hhly.paycore.paychannel.UnifiedPayService;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransRedPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.remote.service.IRechargeService;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.ChannelTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.CmsRechargeTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayChannelEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.PropertyUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.channel.vo.ChannelParamVO;
import com.hhly.skeleton.pay.channel.vo.PayTypeResultVO;
import com.hhly.skeleton.pay.vo.AgentPayVO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.PayNotifyMockVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.PayResultVO;
import com.hhly.skeleton.pay.vo.PayReturnResultVO;
import com.hhly.skeleton.pay.vo.RechargeParamVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.CodeUtil;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.ThreadPoolUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】 充值实现类
 * @author xiongJinGang
 * @date 2017年4月8日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iRechargeService")
public class RechargeServiceImpl extends PayCommon implements IRechargeService {
	private static final Logger logger = LoggerFactory.getLogger(RechargeServiceImpl.class);
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private PayBankService payBankService;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private PayChannelService payChannelService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private PayCoreService payCoreService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private DelayService delayService;// 延时处理
	@Value("${delayQueue.isopen}")
	private String openDelayQueue;// true 打开delayQueue延时关闭，false关闭
	@Value("${recharge.remaining.valid.time}")
	private String validTime;// 支付有效时间
	@Value("${recharge.return.url}")
	private String returnUrl;// 支付同步返回URL
	@Value("${recharge.notify.url}")
	private String notifyUrl;// 支付异步返回URL
	@Value("${nowrecharge.return.url}")
	private String nowRechargeReturnUrl;// 现在充值同步返回地址
	@Value("${lianrecharge.return.url}")
	private String lianRechargeReturnUrl;// 连连充值同步返回地址

	@Override
	public ResultBO<?> recharge(RechargeParamVO rechargeParam) {
		logger.info("充值请求参数：" + rechargeParam.toString());

		// 1、 验证token是否为空，是否有用户信息
		UserInfoBO userInfo = userUtil.getUserByToken(rechargeParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}

		// 验证充值参数
		ResultBO<?> resultBO = PayUtil.validateRechargeParams(rechargeParam);
		if (resultBO.isError()) {
			logger.info("验证用户【" + userInfo.getId() + "】充值参数错误：" + PropertyUtil.getConfigValue(resultBO.getErrorCode()));
			return resultBO;
		}

		// 账户余额，获取账户钱包金额信息，计算账户总金额与3个子账户金额之和是否相等，不待返回错误
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userInfo.getId());
		resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 银行信息
		resultBO = payBankService.findBankByIdAndValidate(rechargeParam.getBankId());
		if (resultBO.isError()) {
			logger.info("获取银行【" + rechargeParam.getBankId() + "】信息错误：" + PropertyUtil.getConfigValue(resultBO.getErrorCode()));
			logger.info("获取用户【" + userInfo.getId() + "】银行【" + rechargeParam.getBankId() + "】信息错误：" + PropertyUtil.getConfigValue(resultBO.getErrorCode()));
			return resultBO;
		}
		PayBankBO payBankBO = (PayBankBO) resultBO.getData();
		rechargeParam.setPayBankBO(payBankBO);

		// 银行卡ID，如果为空，表示使用的第三方支付，需要根据bankId获取支付方式
		Integer bankCardId = rechargeParam.getBankCardId();
		if (!ObjectUtil.isBlank(bankCardId)) {
			// 银行卡信息
			resultBO = bankcardService.findUserBankCardByCardId(userInfo.getId(), bankCardId);
			if (resultBO.isError()) {
				return resultBO;
			}
			PayBankcardBO payBankcardBO = (PayBankcardBO) resultBO.getData();
			// 是否切换不为空，并且为切换时
			if (!ObjectUtil.isBlank(rechargeParam.getChange()) && rechargeParam.getChange().equals(PayConstants.ChangeEnum.YES.getKey())) {
				// 已开通快捷支付，切换成网银
				if (payBankcardBO.getOpenbank().equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey())) {
					payBankcardBO.setOpenbank(PayConstants.BandCardQuickEnum.NOT_OPEN.getKey());
				} else {
					// 未开通快捷支付，切换成快捷
					payBankcardBO.setOpenbank(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey());
				}
			}
			rechargeParam.setPayBankcardBO(payBankcardBO);

			// 银行卡附加信息
			String bankSegmentCode = bankcardSegmentService.findBankSegmentCodeByCard(payBankcardBO.getCardcode());
			if (ObjectUtil.isBlank(bankSegmentCode)) {
				logger.info("未获取到用户【" + userInfo.getId() + "】银行卡【" + payBankcardBO.getCardcode() + "】附加信息");
				return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_SEGMENT_NOT_FOUND_ERROR_SERVICE);
			}
			rechargeParam.setBankCode(bankSegmentCode);
		}

		OperateCouponBO operateCouponBO = null;
		String redCode = rechargeParam.getRedCode();
		if (!ObjectUtil.isBlank(redCode)) {
			resultBO = operateCouponService.findCouponByRedCode(redCode);
			if (resultBO.isError()) {
				logger.info("获取用户【" + userInfo.getId() + "】红包【" + redCode + "】详情失败：" + resultBO.getMessage());
				return resultBO;
			}
			operateCouponBO = (OperateCouponBO) resultBO.getData();
			// 验证充值红包类型、充值平台
			resultBO = PayUtil.validateRechargeRed(operateCouponBO, rechargeParam);
			if (resultBO.isError()) {
				logger.info("验证用户【" + userInfo.getId() + "】红包【" + redCode + "】使用条件失败：" + resultBO.getMessage());
				return resultBO;
			}
		}
		// 调用具体的第三方充值
		return callRecharge(rechargeParam, userInfo, operateCouponBO);
	}

	@Override
	public ResultBO<?> agentRecharge(AgentPayVO agentPayVO) throws Exception {
		logger.info("代理系统充值，参数【" + agentPayVO.toString() + "】");
		try {
			ResultBO<?> resultBO = PayUtil.validateAgentRecharge(agentPayVO);
			if (resultBO.isError()) {
				logger.info("代理系统充值失败：" + resultBO.getMessage());
				return resultBO;
			}
			// 构造充值流水
			TransRechargePO transRecharge = createAgentRechargeRecord(agentPayVO);
			// 添加彩金红包，修改用户彩金金额、添加交易流水
			resultBO = payCoreService.modifyUserWalletRedForAgent(transRecharge);
			if (resultBO.isError()) {
				logger.info("代理系统充值失败：" + resultBO.getMessage());
				return resultBO;
			}
			agentPayVO.setTradeNo(transRecharge.getTransRechargeCode());
			return ResultBO.ok(agentPayVO);
		} catch (Exception e) {
			logger.error("代理系统给用户【" + agentPayVO.getUserId() + "】充值异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@Override
	public ResultBO<?> agentRechargeCash(AgentPayVO agentPayVO) throws Exception {
		logger.info("代理系统充值现金，参数【" + agentPayVO.toString() + "】");
		try {
			ResultBO<?> resultBO = PayUtil.validateAgentRecharge(agentPayVO);
			if (resultBO.isError()) {
				logger.info("代理系统充值现金失败：" + resultBO.getMessage());
				return resultBO;
			}
			// 构造充值流水
			TransRechargePO transRecharge = createAgentRechargeRecord(agentPayVO);
			// 添加彩金红包，修改用户彩金金额、添加交易流水
			resultBO = payCoreService.modifyUserWalletCashForAgent(transRecharge);
			if (resultBO.isError()) {
				logger.info("代理系统充值失败：" + resultBO.getMessage());
				return resultBO;
			}
			agentPayVO.setTradeNo(transRecharge.getTransRechargeCode());
			return ResultBO.ok(agentPayVO);
		} catch (Exception e) {
			logger.error("代理系统给用户【" + agentPayVO.getUserId() + "】充值异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@Override
	public ResultBO<?> rechargeResult(RechargeParamVO rechargeParam) {
		// 1、 验证token是否为空，是否有用户信息
		UserInfoBO userInfo = userUtil.getUserByToken(rechargeParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			logger.info("获取充值结果失败，登录token失效");
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		try {

			if (!StringUtil.isBlank(rechargeParam.getTransCode()) && rechargeParam.getTransCode().contains("?")) {
				rechargeParam.setTransCode(rechargeParam.getTransCode().substring(0, rechargeParam.getTransCode().indexOf("?")));
			}

			ResultBO<?> resultBO = transRechargeService.findRechargeByCode(rechargeParam.getToken(), rechargeParam.getTransCode());
			if (resultBO.isOK()) {
				TransRechargeBO transRechargeBO = (TransRechargeBO) resultBO.getData();
				PayResultVO payResultVO = new PayResultVO(transRechargeBO);
				Double redAmount = ObjectUtil.isBlank(transRechargeBO.getRedAmount()) ? 0d : transRechargeBO.getRedAmount();
				payResultVO.setOrderAmount(MathUtil.add(transRechargeBO.getRechargeAmount(), redAmount));
				payResultVO.setPayAmount(transRechargeBO.getArrivalAmount());
				payResultVO.setRechargePlatform(transRechargeBO.getRechargePlatform());// 充值平台
				payResultVO.setActivityCode(transRechargeBO.getActivityCode());// 充值活动编号
				// 红包编号不为空
				if (!ObjectUtil.isBlank(transRechargeBO.getRedCode())) {
					resultBO = operateCouponService.findCouponByRedCode(transRechargeBO.getRedCode());
					OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();
					// 是充值红包
					if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey())) {
						// 面额是充值送的金额
						payResultVO.setRechargeRed(operateCouponBO.getRedValue());
						payResultVO.setRedName(operateCouponBO.getRedName());
					}
				}
				logger.info("获取交易号【" + rechargeParam.getTransCode() + "】充值结果为【" + payResultVO.getPayStatus() + "】");
				return ResultBO.ok(payResultVO);
			}
			logger.info("获取充值【" + rechargeParam.getTransCode() + "】结果失败，未查询到交易记录");
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("获取充值【" + rechargeParam.getTransCode() + "】结果异常", e);
			return ResultBO.err(MessageCodeConstants.ORDER_IS_BEING_PAID);
		}
	}

	@Override
	@Deprecated
	public ResultBO<?> rechargeReturn(Map<String, String> params) {
		logger.info("充值同步返回参数：" + params.toString());
		String rechargeCode = "";// 主要存的是充值流水号
		ChannelTypeEnum channelTypeEnum = null;
		// body不为空，初步判断为支付
		if (params.containsKey("body")) {
			// 支付宝
			rechargeCode = params.get("body");
			channelTypeEnum = PayConstants.ChannelTypeEnum.ALIPAY_WAP;
		} else if (params.containsKey("attach")) {
			// 连连支付
			rechargeCode = params.get("attach");
			channelTypeEnum = PayConstants.ChannelTypeEnum.WECHAT_WEB;
		}
		if (!ObjectUtil.isBlank(channelTypeEnum)) {
			// 根据支付渠道解析
			UnifiedPayService rechargeService = realPayServices.get(channelTypeEnum.getChannel());
			ResultBO<?> resultBO = rechargeService.payReturn(params);
			if (resultBO.isOK()) {
				PayReturnResultVO payReturnResult = (PayReturnResultVO) resultBO.getData();
				// 交易成功
				if (!ObjectUtil.isBlank(payReturnResult.getStatus()) && payReturnResult.getStatus().equals(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey())) {
					if (!ObjectUtil.isBlank(rechargeCode)) {
						TransRechargeBO transRecharge = transRechargeService.findRechargeByTransCode(rechargeCode);
						if (ObjectUtil.isBlank(transRecharge)) {
							logger.info("【充值】同步返回成功，获取交易【" + transRecharge + "】详情为空");
							return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
						}
					} else {
						return ResultBO.err(MessageCodeConstants.TRANS_CODE_IS_NULL_FIELD);
					}
				}
			}
			return resultBO;
		} else {
			return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_NULL_FIELD);
		}
	}

	@Override
	public ResultBO<?> rechargeNotify(Map<String, String> params) throws Exception {
		logger.info("充值接口异步通知参数：" + params.toString());
		try {
			String channelTypeName = params.get(Constants.PAY_CHANNEL_TYPE_NAME);
			if (ObjectUtil.isBlank(channelTypeName)) {
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_NULL_FIELD);
			}

			// 根据支付渠道得到具体的支付信息
			UnifiedPayService rechargeService = realPayServices.get(channelTypeName);
			params.remove(Constants.PAY_CHANNEL_TYPE_NAME);// 不删除，签名认证不过
			ResultBO<?> payResult = rechargeService.payNotify(params);
			if (payResult.isError()) {
				logger.info("解析第三方【" + channelTypeName + "】充值异步回调失败：" + payResult.getMessage());
				return payResult;
			}

			// 得到充值异步返回的参数
			PayNotifyResultVO payNotifyResult = (PayNotifyResultVO) payResult.getData();
			logger.info(channelTypeName + "充值返回结果：" + payNotifyResult.toString());

			String rechargeCode = payNotifyResult.getOrderCode();// 主要存的是充值流水号
			// 验证充值状态是否已更新
			ResultBO<?> resultBO = payCoreService.checkTransRechargeStatus(rechargeCode);
			if (resultBO.isError()) {
				return resultBO;
			}

			TransRechargeBO transRecharge = (TransRechargeBO) resultBO.getData();

			if (!ObjectUtil.isBlank(transRecharge)) {
				// 验证充值金额与到账户金额是否一致，不一致返回错误
				resultBO = PayUtil.validateRechargeAmount(payNotifyResult.getOrderAmt(), transRecharge.getRechargeAmount());
				if (resultBO.isError()) {
					logger.error("充值编号【" + rechargeCode + "】的充值金额【" + transRecharge.getRechargeAmount() + "】与到账金额【" + payNotifyResult.getOrderAmt() + "】不匹配");
					return resultBO;
				}
				transRecharge.setArrivalAmount(payNotifyResult.getOrderAmt());

				/***验证支付结果***/
				if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
					// 充值成功，更新充值、钱包、彩金红包、交易流水等记录
					ResultBO<?> resultBO1 = payCoreService.modifyRechargeSuccessTransRecord(transRecharge, payNotifyResult);

					if (resultBO1.isOK()) {
						PayResultVO payResultVO = (PayResultVO) resultBO1.getData();
						Integer userId = transRecharge.getUserId();
						// 发送充值消息
						messageProvider.sendRechargeMessage(userId, transRecharge.getTransRechargeCode());
						// 使用了红包，才发送红包消息
						if (!ObjectUtil.isBlank(payResultVO.getRedCode())) {
							// 发送得到彩金红包消息
							messageProvider.sendRedMessage(userId, payResultVO.getRedCode());
						}
						// 充值活动编号不为空，发送活动mq消息，2017-09-19 被decheng要求去掉
						// if (!ObjectUtil.isBlank(transRecharge.getActivityCode())) {
						String activityPage = redisUtil.getString(CacheConstants.P_CORE_RECHARGE_ORDER_PAGE + transRecharge.getTransRechargeCode());
						messageProvider.sendRechargeActivityMessage(transRecharge.getActivityCode(), transRecharge.getTransRechargeCode(), ObjectUtil.isBlank(activityPage) ? "0" : activityPage);
						// }

						// 往当日渠道限额中添加金额
						payCoreService.addDayLimitAmount(transRecharge);
					}
				} else {
					// 充值失败，更新充值、交易流水等记录（2017-08-09 14点30去掉失败的交易流水的添加）
					payCoreService.modifyFailTransRecord(transRecharge, payNotifyResult);
				}
				return payResult;// 返回支付结果信息
			} else {
				logger.info(channelTypeName + "【充值】获取充值流水【" + rechargeCode + "】的充值记录失败，无法更新充值记录和添加交易记录");
				return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
			}
		} catch (Exception e) {
			logger.error("【充值】处理支付异步通知异常，通知参数【" + params.toString() + "】异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@Override
	public ResultBO<?> rechargeNotifyMock(PayNotifyMockVO payNotifyMockVO) throws Exception {
		logger.info("支付异步通知参数：" + payNotifyMockVO.toString());
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		try {
			TransRechargeBO transRecharge = transRechargeService.findRechargeByTransCode(payNotifyMockVO.getTransCode());
			if (ObjectUtil.isBlank(transRecharge)) {
				logger.info("【支付】获取充值流水【" + payNotifyMockVO.getTransCode() + "】的充值记录失败，无法更新充值记录和添加交易记录");
				return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
			}
			if (transRecharge.getTransStatus().equals(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey())) {
				return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_STATUS_FINISHED_ERROR_SERVICE);
			}

			// 验证充值状态是否已更新
			ResultBO<?> resultBO = payCoreService.checkTransRechargeStatus(payNotifyMockVO.getTransCode());
			if (resultBO.isError()) {
				return resultBO;
			}

			payNotifyResult.setPayId(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
			payNotifyResult.setOrderAmt(Double.parseDouble(payNotifyMockVO.getTransAmount()));
			payNotifyResult.setThirdTradeNo(ObjectUtil.isBlank(payNotifyMockVO.getThirdTradeNo()) ? DateUtil.getNow(DateUtil.DATE_FORMAT_NUM) : payNotifyMockVO.getThirdTradeNo());
			payNotifyResult.setTradeTime(ObjectUtil.isBlank(payNotifyMockVO.getThirdTradeNo()) ? DateUtil.getNow() : payNotifyMockVO.getTradeTime());
			PayStatusEnum payStatusEnum = null;
			if (StringUtil.isBlank(payNotifyMockVO.getPayStatus())) {
				payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
			} else {
				if (payNotifyMockVO.getPayStatus().equals("1")) {
					payStatusEnum = PayStatusEnum.PAYMENT_SUCCESS;
				} else {
					payStatusEnum = PayStatusEnum.PAYMENT_FAILURE;
				}
			}
			payNotifyResult.setStatus(payStatusEnum);// 支付状态
			payNotifyResult.setOrderCode(payNotifyMockVO.getTransCode());
			if (payNotifyResult.getStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
				// 验证充值金额与到账户金额是否一致，不一致返回错误
				resultBO = PayUtil.validateRechargeAmount(payNotifyResult.getOrderAmt(), transRecharge.getRechargeAmount());
				if (resultBO.isError()) {
					logger.error("充值金额【" + transRecharge.getRechargeAmount() + "】与到账金额【" + payNotifyResult.getOrderAmt() + "】不匹配");
					return resultBO;
				}

				/********** 充值成功，更新充值、钱包、彩金红包、交易流水等记录******************/
				resultBO = payCoreService.modifyRechargeSuccessTransRecord(transRecharge, payNotifyResult);
				if (resultBO.isOK()) {
					// 3、添加订单支付结果信息到缓存
					redisUtil.addString(CacheConstants.P_CORE_PAY_STATUS_RESULT + transRecharge.getUserId() + "_" + transRecharge.getTransRechargeCode(), "success", CacheConstants.FIFTEEN_MINUTES);
					PayResultVO payResultVO = (PayResultVO) resultBO.getData();

					Integer userId = transRecharge.getUserId();
					// 发送充值消息
					messageProvider.sendRechargeMessage(userId, transRecharge.getTransRechargeCode());
					// 使用了红包，才发送红包消息
					if (!ObjectUtil.isBlank(payResultVO.getRedCode())) {
						// 发送得到彩金红包消息
						messageProvider.sendRedMessage(userId, payResultVO.getRedCode());
					}
					// 充值活动编号不为空，发送活动mq消息
					String activityPage = redisUtil.getString(CacheConstants.P_CORE_RECHARGE_ORDER_PAGE + transRecharge.getTransRechargeCode());
					messageProvider.sendRechargeActivityMessage(transRecharge.getActivityCode(), transRecharge.getTransRechargeCode(), ObjectUtil.isBlank(activityPage) ? "0" : activityPage);

					// 往当日渠道限额中添加金额
					payCoreService.addDayLimitAmount(transRecharge);
				}
			} else {
				// 充值失败，更新充值、交易流水等记录
				resultBO = payCoreService.modifyFailTransRecord(transRecharge, payNotifyResult);
				if (resultBO.isOK()) {
					redisUtil.addString(CacheConstants.P_CORE_PAY_STATUS_RESULT + transRecharge.getUserId() + "_" + transRecharge.getTransRechargeCode(), "fail", CacheConstants.FIFTEEN_MINUTES);
				}
			}
			return resultBO;
		} catch (

		Exception e) {
			logger.error("【支付】处理支付异步通知异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@Override
	public ResultBO<?> updateRecharge(CmsRechargeVO cmsRecharge) {
		try {
			ResultBO<?> resultBO = PayUtil.validateCmsRecharge(cmsRecharge);
			if (resultBO.isError()) {
				logger.info("cms人工充值失败：" + resultBO.getMessage());
				return resultBO;
			}
			// 判断操作类型
			Short rechargeType = cmsRecharge.getRechargeType();
			CmsRechargeTypeEnum cmsRechargeTypeEnum = PayConstants.CmsRechargeTypeEnum.getEnum(rechargeType);
			logger.info("cms开始人工【" + cmsRechargeTypeEnum.getValue() + "】充值，参数【" + cmsRecharge.toString() + "】");
			// 构造充值流水
			TransRechargePO transRecharge = createRechargeRecord(cmsRecharge);

			Integer userId = cmsRecharge.getUserId();
			if (rechargeType.equals(CmsRechargeTypeEnum.CASH.getKey())) {
				// 操作现金账户、更新钱包金额、添加交易流水
				resultBO = payCoreService.modifyUserWalletCash(transRecharge, cmsRecharge);
				// 发送充值消息
				messageProvider.sendRechargeMessage(userId, transRecharge.getTransRechargeCode());
			} else {
				// 操作彩金红包账户，增加红包交易流水、添加交易流水
				resultBO = payCoreService.modifyUserWalletRed(transRecharge, cmsRecharge);
				OperateCouponBO operateCouponBO = (OperateCouponBO) resultBO.getData();
				// 发送红包消息
				messageProvider.sendRedMessage(userId, operateCouponBO.getRedCode());
			}
			return resultBO;
		} catch (Exception e) {
			logger.error("cms给用户【" + cmsRecharge.getUserId() + "】人工充值异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@Override
	public ResultBO<?> updateWalletToBuyRedColorForCms(CmsRechargeVO cmsRecharge) throws Exception {
		long startTime = System.currentTimeMillis();
		logger.debug("updateWalletToBuyRedColorForCms开始时间：" + startTime);
		logger.info("CMS调用，更新钱包及购买红包参数：" + cmsRecharge.toString());
		ResultBO<?> resultBO = TransUtil.validateUserTransRecordByOrderCode(cmsRecharge);
		if (resultBO.isError()) {
			logger.info("CMS发起活动送彩金，参数验证不过：" + cmsRecharge);
			return resultBO;
		}
		TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(cmsRecharge.getRechargeCode());
		if (ObjectUtil.isBlank(transRechargeBO)) {
			logger.info("未获取到充值【" + cmsRecharge.getRechargeCode() + "】记录");
			return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
		}
		if (!transRechargeBO.getTransStatus().equals(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey())) {
			logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录交易状态【" + transRechargeBO.getTransStatus() + "】错误");
			return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
		}
		// 2018-05-16 ，liuqiong建议去掉判断
		/*if (ObjectUtil.isBlank(transRechargeBO.getActivityCode())) {
			logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录未参加活动");
			return ResultBO.err(MessageCodeConstants.FOOTBALL_FIRST_ACTIVITY_NOT_EXIST);
		}
		if (!transRechargeBO.getActivityCode().equals(cmsRecharge.getActivityCode())) {
			logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录参加活动与Cms传入活动不符");
			return ResultBO.err(MessageCodeConstants.FOOTBALL_FIRST_ACTIVITY_NOT_EXIST);
		}*/
		TransUserBO transUserBO = transUserService.findTransUserBy(transRechargeBO.getTransRechargeCode(), PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());
		// 2018-04-26 ，liuqiong建议去掉判断
		/*if (!ObjectUtil.isBlank(transUserBO)) {//
			if (transUserBO.getTransType().equals(PayConstants.TransTypeEnum.ACTIVITY_CONSUME.getKey()) || transUserBO.getTransType().equals(PayConstants.TransTypeEnum.ACTIVITY_GIVE.getKey())) {
				logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录已参加活动");
				return ResultBO.err(MessageCodeConstants.FIRST_REC_SEND_HAS_SEND_BY_FHONE);
			}
		}*/
		// 支出账户中的金额，购买。 这个充值金额是根据充值红包来的，充100送20，就会有120做为彩金红包。
		// resultBO = userWalletService.updateUserWalletBySplit(cmsRecharge.getUserId(), cmsRecharge.getRechargeAmount(), PayConstants.MoneyFlowEnum.OUT.getKey(),
		// PayConstants.WalletSplitTypeEnum.TWENTY_EIGHTY_WINNING.getKey());

		// 扣除账户金额，加彩金红包，一次操作
		resultBO = userWalletService.updateUserWalletCommon(cmsRecharge.getUserId(), cmsRecharge.getRechargeAmount(), PayConstants.MoneyFlowEnum.OUT.getKey(), cmsRecharge.getRechargeAmount(), PayConstants.MoneyFlowEnum.IN.getKey());

		if (resultBO.isError()) {
			logger.info("CMS送彩金，操作账户余额失败：" + resultBO.getMessage());
			return resultBO;
		}
		UserWalletPO uwp = (UserWalletPO) resultBO.getData();
		Double nowRedAmount = uwp.getEffRedBalance();// 当前账户上所剩红包金额
		// 1、添加账户消费流水
		cmsRecharge.setOrderInfo(Constants.RECHARGE_BUY_RED_REMARK_INFO);// 购买红包
		uwp.setEffRedBalance(MathUtil.sub(uwp.getEffRedBalance(), cmsRecharge.getRechargeAmount()));
		TransUserPO transUserPOSub = transUserService.addTransCostRecord(cmsRecharge, transRechargeBO, uwp);
		// transUserLogService.addTransLogRecord(transUserPO);

		// 3、生成彩金红包
		String redCode = OrderNoUtil.getOrderNo(NumberCode.COUPON);
		transRechargeBO.setRedCode(redCode);
		OperateCouponPO operateCouponPO = operateCouponService.addOperateCoupon(cmsRecharge, transRechargeBO);

		// 2、生成彩金红包交易流水
		cmsRecharge.setOrderInfo(Constants.RED_REMARK_ORDER_INFO);// 生成红包
		uwp.setEffRedBalance(nowRedAmount);
		TransUserPO transUserPOAdd = transUserService.addTransRecord(cmsRecharge, transRechargeBO, uwp);

		// 合并充值记录到一条，供前端展示，取的是现金交易金额
		addActivityTransUser(transUserBO, transUserPOSub, transUserPOAdd);

		// 4、彩金红包生成记录
		addTransRed(cmsRecharge, operateCouponPO);

		// 加彩金
		// userWalletService.updateUserWalletBySplit(cmsRecharge.getUserId(), cmsRecharge.getRechargeAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		long endTime = System.currentTimeMillis();
		logger.debug("updateWalletToBuyRedColorForCms结束时间：" + endTime + "，耗时：" + (endTime - startTime));
		return ResultBO.ok();
	}

	// 首充活动，充值的钱全部转成彩金，送的钱也是彩金，会有2条流水，
	@Override
	public ResultBO<?> updateWalletFirstRechargeForCms(CmsRechargeVO cmsRecharge) throws Exception {
		long startTime = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("updateWalletToBuyRedColorForCms开始时间：" + startTime);
		}
		logger.info("CMS调用，首充更新钱包及购买红包参数：" + cmsRecharge.toString());
		ResultBO<?> resultBO = TransUtil.validateUserTransRecordByOrderCode(cmsRecharge);
		if (resultBO.isError()) {
			logger.info("CMS发起活动送彩金，参数验证不过：" + resultBO.getMessage());
			return resultBO;
		}
		TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(cmsRecharge.getRechargeCode());
		if (ObjectUtil.isBlank(transRechargeBO)) {
			logger.info("未获取到充值【" + cmsRecharge.getRechargeCode() + "】记录");
			return ResultBO.err(MessageCodeConstants.QUERY_RECHARGE_RECORD_FAIL_ERROR_SERVICE);
		}
		if (!transRechargeBO.getTransStatus().equals(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey())) {
			logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录交易状态【" + transRechargeBO.getTransStatus() + "】错误");
			return ResultBO.err(MessageCodeConstants.PAY_TRADE_STATUS_ERROR_SERVICE);
		}
		// 如果充值记录中没有活动编号，则需求更新
		if (ObjectUtil.isBlank(transRechargeBO.getActivityCode())) {
			TransRechargePO transRechargePO = new TransRechargePO();
			transRechargePO.setId(transRechargeBO.getId());
			transRechargePO.setActivityCode(cmsRecharge.getActivityCode());
			int num = transRechargeService.updateRecharge(transRechargePO);
			if (num <= 0) {
				logger.info("将活动编号【" + cmsRecharge.getActivityCode() + "】更新到【" + cmsRecharge.getRechargeCode() + "】失败");
				return ResultBO.err(MessageCodeConstants.UPDATE_MESSAGE_FAIL);
			}
		} else {
			logger.info("充值【" + cmsRecharge.getRechargeCode() + "】记录已参加活动");
			return ResultBO.err(MessageCodeConstants.FIRST_REC_SEND_HAS_SEND_BY_FHONE);
		}

		Double rechargeAmount = transRechargeBO.getArrivalAmount();// 充值金额（账户扣除等额的现金转成彩金）

		// 扣除账户金额，加彩金红包，一次操作
		resultBO = userWalletService.updateUserWalletCommon(cmsRecharge.getUserId(), rechargeAmount, PayConstants.MoneyFlowEnum.OUT.getKey(), rechargeAmount, PayConstants.MoneyFlowEnum.IN.getKey());

		if (resultBO.isError()) {
			logger.info("CMS送彩金，操作账户余额失败：" + resultBO.getMessage());
			return resultBO;
		}
		UserWalletPO uwp = (UserWalletPO) resultBO.getData();
		Double nowRedAmount = uwp.getEffRedBalance();// 当前账户上所剩红包金额
		// 1、添加账户消费流水
		cmsRecharge.setOrderInfo(Constants.RECHARGE_BUY_RED_REMARK_INFO);// 购买红包
		uwp.setEffRedBalance(MathUtil.sub(nowRedAmount, rechargeAmount));
		transUserService.addActivityConsume(cmsRecharge, transRechargeBO, uwp);

		// 2、 现金转彩金红包，先生成彩金红包，再添加交易流水
		addCashToRedTransUser(cmsRecharge, transRechargeBO, uwp);

		// 3、活动送的红包，先生成彩金红包，再添加交易流水
		// addSendTransUser(cmsRecharge, transRechargeBO, giveAmount, uwp, nowRedAmount);

		long endTime = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("updateWalletToBuyRedColorForCms结束时间：" + endTime + "，耗时：" + (endTime - startTime));
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 现金转彩金，加流水
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param transRechargeBO
	* @param rechargeAmount
	* @param uwp
	* @param nowRedAmount
	* @throws Exception
	* @time: 2018年3月6日 上午11:56:16
	* @return: void 
	*/
	public void addCashToRedTransUser(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, UserWalletPO uwp) throws Exception {
		OperateCouponPO operateCouponPO2 = operateCouponService.addRechargeToRed(cmsRecharge.getActivityCode(), transRechargeBO.getUserId(), transRechargeBO.getArrivalAmount());
		/**彩金红包生成记录*/
		uwp.setEffRedBalance(MathUtil.add(uwp.getEffRedBalance(), transRechargeBO.getArrivalAmount()));
		cmsRecharge.setRechargeAmount(transRechargeBO.getArrivalAmount());
		cmsRecharge.setOrderInfo(Constants.ACTIVITY_SEND);// 生成红包

		/**彩金红包生成记录*/
		addTransRed(cmsRecharge, operateCouponPO2);
		transRechargeBO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));
		transUserService.addTransRecord(cmsRecharge, transRechargeBO, uwp);
	}

	/**  
	* 方法说明: 活动赠送加彩金
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param transRechargeBO
	* @param rechargeAmount
	* @param uwp
	* @param nowRedAmount
	* @throws Exception
	* @time: 2018年3月6日 上午11:55:44
	* @return: void 
	*/
	@SuppressWarnings("unused")
	private void addSendTransUser(CmsRechargeVO cmsRecharge, TransRechargeBO transRechargeBO, Double giveAmount, UserWalletPO uwp, Double nowRedAmount) throws Exception {
		transRechargeBO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));
		cmsRecharge.setRechargeAmount(giveAmount);
		OperateCouponPO operateCouponPO1 = operateCouponService.addOperateCoupon(cmsRecharge, transRechargeBO);
		/**彩金红包生成记录*/
		addTransRed(cmsRecharge, operateCouponPO1);

		// 生成活动送的彩金红包交易流水
		uwp.setEffRedBalance(nowRedAmount);// 剩余红包金额：账户上的总金额减去充值的金额
		TransUserPO transUserPOAdd = transUserService.addTransRecord(cmsRecharge, transRechargeBO, uwp);
		transUserLogService.addTransLogRecord(transUserPOAdd);
	}

	/**  
	* 方法说明: 添加活动流水
	* @auth: xiongJinGang
	* @param transUserBO
	* @param transUserPOSub
	* @param transUserPOAdd
	* @throws Exception
	* @time: 2017年11月22日 下午12:23:03
	* @return: void 
	*/
	private void addActivityTransUser(TransUserBO transUserBO, TransUserPO transUserPOSub, TransUserPO transUserPOAdd) throws Exception {
		Double leaveAmount = MathUtil.sub(transUserBO.getCashAmount(), transUserPOSub.getCashAmount());// 充值金额减去买红包所用的金额
		if (MathUtil.compareTo(leaveAmount, 0d) < 0) {
			leaveAmount = 0d;
		}
		Double showAmount = MathUtil.add(leaveAmount, transUserPOAdd.getTransAmount());// 展示金额 = 买红包剩余金额+红包到账金额
		Double serviceAmount = ObjectUtil.isBlank(transUserBO.getServiceCharge()) ? 0d : transUserBO.getServiceCharge();// 手续费
		showAmount = MathUtil.sub(showAmount, serviceAmount);

		TransUserPO transUserInfo = new TransUserPO();
		transUserInfo.setCashAmount(leaveAmount);//
		transUserInfo.setTotalCashBalance(transUserPOSub.getTotalCashBalance());
		transUserInfo.setTotalRedBalance(transUserPOAdd.getTotalRedBalance());
		transUserInfo.setTransCode(transUserBO.getTransCode());
		transUserInfo.setTransAmount(showAmount);// 需要给前端展示的金额不包括手续费，这里需要减除
		transUserInfo.setRedCode(transUserPOAdd.getRedCode());// 红包编号
		transUserInfo.setRedTransAmount(transUserPOAdd.getRedTransAmount());// 红包交易金额
		transUserLogService.updateTransUser(transUserInfo);
	}

	/**
	 * 方法说明: 创建人工充值记录
	 * @auth: xiongJinGang
	 * @param cmsRecharge
	 * @time: 2017年7月6日 下午5:25:10
	 * @return: TransRechargePO
	 */
	private TransRechargePO createRechargeRecord(CmsRechargeVO cmsRecharge) {
		TransRechargePO transRechargePO = new TransRechargePO();
		transRechargePO = new TransRechargePO();
		transRechargePO.setUserId(cmsRecharge.getUserId());// 用户Id
		transRechargePO.setCreateBy(cmsRecharge.getOperator());// 创建人
		transRechargePO.setRemark(Constants.RECHARGE_REMARK_INFO);// 充值描述
		transRechargePO.setRechargeAmount(cmsRecharge.getRechargeAmount());// 充值金额
		transRechargePO.setArrivalAmount(cmsRecharge.getRechargeAmount());// 到账金额

		transRechargePO.setPayType(PayConstants.PayTypeEnum.HAND_PAYMENT.getKey());// 人工充值
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 默认其它类型
		transRechargePO.setPayType(PayConstants.PayTypeEnum.HAND_PAYMENT.getKey());// 其它支付
		transRechargePO.setRechargeBank(0);// 银行Id，人工充值没有银行
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 其它银行类型
		transRechargePO.setRechargeRemark(cmsRecharge.getRechargeRemark());// 充值描述
		transRechargePO.setRechargePlatform(PayConstants.TakenPlatformEnum.WEB.getKey());// 充值平台
		transRechargePO.setRedAmount(0d);//
		transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transRechargePO.setTransRechargeCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRechargePO.setServiceCharge(0.0);// 服务费
		transRechargePO.setChannelId(PayConstants.ChannelEnum.UNKNOWN.getKey());// 渠道ID，前端传递的渠道ID
		transRechargePO.setRechargeChannel(PayConstants.PayChannelEnum.ARTIFICIAL_RECHARGE.getKey());// 人工充值
		transRechargePO.setTransTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setThirdTransNum(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
		transRechargePO.setResponseTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setUpdateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setTransEndTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setTakenStatus(PayConstants.RechargeTakenStatusEnum.NOT_ALLOW.getKey());// 即买即付，默认不可提
		transRechargePO.setSwitchStatus(PayConstants.ChangeEnum.NO.getKey());// 是否切换，0不切换，1切换
		transRechargePO.setRechargeType(PayConstants.RechargeTypeEnum.RECHARGE.getKey());// 充值
		// transRechargePO.setRedCode("");// 红包编号
		// transRechargePO.setChannelCode(channelType);
		return transRechargePO;
	}

	private TransRechargePO createAgentRechargeRecord(AgentPayVO agentPayVO) {
		TransRechargePO transRechargePO = new TransRechargePO();
		transRechargePO = new TransRechargePO();
		transRechargePO.setUserId(agentPayVO.getUserId());// 用户Id
		// transRechargePO.setCreateBy(cmsRecharge.getOperator());// 创建人
		String remark = agentPayVO.getRemark();
		if (StringUtils.isBlank(agentPayVO.getRemark())) {
			remark = "第三方渠道充值";
		}
		transRechargePO.setRemark(remark);// 充值描述
		transRechargePO.setRechargeAmount(agentPayVO.getAmount());// 充值金额
		transRechargePO.setArrivalAmount(agentPayVO.getAmount());// 到账金额
		transRechargePO.setChannelCode(PayConstants.PayChannelEnum.AGENT_RECHARGE.getType() + "_WEB");
		// transRechargePO.setOrderCode(agentPayVO.getAgentTradeNo());
		transRechargePO.setPayType(PayConstants.PayTypeEnum.CHANNEL_PAYMENT.getKey());// 代理系统充值
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 默认其它类型
		transRechargePO.setRechargeBank(0);// 银行Id，人工充值没有银行
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 其它银行类型
		transRechargePO.setRechargePlatform(PayConstants.TakenPlatformEnum.AGENT.getKey());// 充值平台
		transRechargePO.setRedAmount(0d);//
		transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transRechargePO.setTransRechargeCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRechargePO.setServiceCharge(0.0);// 服务费
		String channelId = agentPayVO.getChannelId();
		if (ObjectUtil.isBlank(channelId)) {
			channelId = PayConstants.ChannelEnum.UNKNOWN.getKey();
		}
		transRechargePO.setChannelId(channelId);// 渠道ID，前端传递的渠道ID
		transRechargePO.setRechargeChannel(PayConstants.PayChannelEnum.AGENT_RECHARGE.getKey());// 人工充值
		transRechargePO.setTransTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setThirdTransNum(agentPayVO.getAgentTradeNo()); // 第三方充值流水号
		transRechargePO.setResponseTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setUpdateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setTransEndTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setTakenStatus(PayConstants.RechargeTakenStatusEnum.ALLOW.getKey());// 代理充值，改成可以提
		transRechargePO.setSwitchStatus(PayConstants.ChangeEnum.NO.getKey());// 是否切换，0不切换，1切换
		transRechargePO.setRechargeType(PayConstants.RechargeTypeEnum.RECHARGE.getKey());// 充值
		transRechargePO.setActivityCode(agentPayVO.getActivityCode());// 活动编号
		return transRechargePO;
	}

	private UnifiedPayService getServiceImpl(String channelType) {
		ChannelTypeEnum channel = PayUtil.getChannelType(channelType);
		return realPayServices.get(channel.getChannel());
	}

	/**
	 * 方法说明: 调用充值
	 * 
	 * @auth: xiongJinGang
	 * @param rechargeParam
	 * @param userInfo
	 * @time: 2017年4月10日 下午5:06:54
	 * @return: ResultBO<?>
	 */
	private ResultBO<?> callRecharge(RechargeParamVO rechargeParam, UserInfoBO userInfo, OperateCouponBO operateCouponBO) {
		final String transCode = OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN);
		rechargeParam.setTransCode(transCode);

		/***************获取具体的支付渠道***************/
		// 根据银行ID获取银行的支付渠道
		List<PayChannelBO> payChannelList = payChannelService.findChannelByBankIdUseCache(rechargeParam.getBankId());
		// 未获取到可用支付渠道
		if (ObjectUtil.isBlank(payChannelList)) {
			logger.info("获取用户【" + userInfo.getId() + "】银行【" + rechargeParam.getBankId() + "】充值渠道为空");
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}
		PayBankcardBO payBankcardBO = rechargeParam.getPayBankcardBO();
		ChannelParamVO channelParam = new ChannelParamVO(rechargeParam.getPlatform(), rechargeParam.getRechargeAmount(), rechargeParam.getPayBankBO(), payBankcardBO, payChannelList, PayConstants.RechargeTypeEnum.RECHARGE.getKey(),
				rechargeParam.getChannelId(), rechargeParam.getAppId());
		if (!ObjectUtil.isBlank(rechargeParam.getAppId()) && !ObjectUtil.isBlank(rechargeParam.getOpenId())
				&& (rechargeParam.getPlatform().equals(TakenPlatformEnum.WAP.getKey()) || rechargeParam.getPlatform().equals(TakenPlatformEnum.JSAPI.getKey()))) {
			channelParam.setPlatform(PayConstants.TakenPlatformEnum.JSAPI.getKey());
		}
		ResultBO<?> resultBO = payCoreService.getPayChannel(channelParam);
		if (resultBO.isError()) {
			return resultBO;
		}
		PayTypeResultVO payTypeResultVO = (PayTypeResultVO) resultBO.getData();
		String channelType = payTypeResultVO.getPayTypeName();
		if (ObjectUtil.isBlank(channelType)) {
			if (!ObjectUtil.isBlank(payBankcardBO)) {
				String card = payBankcardBO.getCardcode();
				String bankCard = "卡号：" + card;
				logger.info("根据用户【" + userInfo.getId() + "，银行卡" + payBankcardBO.getCardcode() + "，" + bankCard + "】未匹配上具体的支付渠道");
			}
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}

		/***************拼装统一的api支付请求参数***************/
		UnifiedPayService payService = null;
		try {
			payService = getServiceImpl(channelType);
			if (ObjectUtil.isBlank(payService)) {
				logger.info("筛选给用户【" + userInfo.getId() + "】的支付渠道【" + channelType + "】没有对接");
				return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
			}
		} catch (Exception e) {
			logger.error("筛选给用户【" + userInfo.getId() + "】的支付渠道【" + channelType + "】没有对接", e);
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CHANNEL_NOT_FOUND_ERROR_SERVICE);
		}
		PaymentInfoBO paymentInfo = getRechargeParam(rechargeParam, userInfo, transCode, channelType);
		// 支付方式 1:网银支付(借记卡) 2:快捷支付(借记卡) 3:快捷支付(信用卡)8:网银支付(信用卡)9:B2B企业网银支付 10：支付宝 11：微信
		paymentInfo.setPayType(payTypeResultVO.getPayType());
		// 渠道类型转换
		PayUtil.platformChange(channelType, paymentInfo);
		// 调用具体的支付
		resultBO = payService.pay(paymentInfo);

		/***************获取支付请求返回***************/
		if (resultBO.isOK()) {
			PayReqResultVO payReqResultVO = (PayReqResultVO) resultBO.getData();
			payReqResultVO.setTransCode(transCode);
			payReqResultVO.setChannel(payTypeResultVO.getPayType());// 微信还是支付宝
			// 以字节数组流返回
			if (PayConstants.PayReqResultEnum.LINK.getKey().equals(payReqResultVO.getType()) && !StringUtil.isBlank(payReqResultVO.getFormLink())) {
				// 以字节数组流返回
				payReqResultVO.setQrStream(CodeUtil.getQrCode(payReqResultVO.getFormLink()));
			}
			resultBO = ResultBO.ok(payReqResultVO);

			// 转账汇款的，只是做一个跳转，不添加记录
			if (!payReqResultVO.getTradeChannel().equals(PayConstants.PayChannelEnum.TRANSFER_RECHARGE.getKey())) {
				// 添加充值记录到数据库
				ResultBO<?> resultBO1 = addRechargeRecord(rechargeParam, userInfo, operateCouponBO, transCode, payTypeResultVO);
				// 记录当前充值是否是从活动页面过来的，存入redis
				redisUtil.addString(CacheConstants.P_CORE_RECHARGE_ORDER_PAGE + transCode, String.valueOf(rechargeParam.getActivity()), CacheConstants.ONE_HOURS);
				// 充值记录加入到延时队列，过期自动获取支付结果并关闭
				Boolean isOpenDelayQueue = Boolean.parseBoolean(openDelayQueue);
				if (isOpenDelayQueue) {
					// 2、 把订单插入到待收货的队列和redis
					ThreadPoolUtil.execute(new Runnable() {
						@Override
						public void run() {
							Long endTime = DateUtil.getNowAddMinute(DateUtil.getNowDate(), Constants.RECHARGE_EFFECTIVE_TIME);
							// 1 插入到待收货队列
							CloseRechargeDelay dshOrder = new CloseRechargeDelay(transCode, endTime);
							delayService.add(dshOrder);
							// 2插入到redis
							redisUtil.addObj(CacheConstants.P_CORE_RECHARGE_ORDER + transCode, dshOrder, CacheConstants.TWO_HOURS);
							logger.info("充值记录【" + transCode + "】存入延时队列和redis中，过期自动消费");
						}
					});
				}
				return resultBO1.isOK() ? resultBO : resultBO1;
			}
		}
		return resultBO;

	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param rechargeParam
	* @param userInfo
	* @param operateCouponBO
	* @param transCode
	* @param channelType
	* @return
	* @time: 2017年12月13日 下午3:18:59
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> addRechargeRecord(RechargeParamVO rechargeParam, UserInfoBO userInfo, OperateCouponBO operateCouponBO, final String transCode, PayTypeResultVO payTypeResultVO) {
		// 根据支付渠道的code获取支付渠道key
		String channelType = payTypeResultVO.getPayTypeName();
		PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByType(payTypeResultVO.getChannelCode());
		// 记录充值记录
		TransRechargeVO transRecharge = new TransRechargeVO(rechargeParam);
		transRecharge.setChannelCode(channelType);
		transRecharge.setRechargeChannel(payChannelEnum.getKey());
		transRecharge.setTransCode(transCode);
		// 计算充值金额（减去相应的手续费，目前手续费为0）
		transRecharge.setRechargeAmount(MathUtil.calCounterFee(rechargeParam.getRechargeAmount(), 0.0));
		transRecharge.setUserId(userInfo.getId());
		transRecharge.setChannelId(rechargeParam.getChannelId());// 渠道ID
		// 使用了充值红包
		if (!ObjectUtil.isBlank(operateCouponBO) && operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey())) {
			transRecharge.setRechargeRemark(MessageFormat.format(Constants.RECHARGE_REMARK, operateCouponBO.getMinSpendAmount(), operateCouponBO.getRedValue()));
		}
		transRecharge.setRechargeType(PayConstants.RechargeTypeEnum.RECHARGE.getKey());
		transRecharge.setTakenStatus(PayConstants.RechargeTakenStatusEnum.ALLOW.getKey());// 充值默认可提
		transRecharge.setPayChannelId(payTypeResultVO.getPayChannelId());
		// 1、保存充值记录
		ResultBO<?> resultBO1 = transRechargeService.addRechargeTrans(transRecharge, userInfo);
		return resultBO1;
	}

	/**
	 * 方法说明: 组装支付对象
	 * 
	 * @auth: xiongJinGang
	 * @param payParam
	 * @param userInfo
	 * @param transCode
	 * @param channelType
	 * @time: 2017年4月10日 下午4:23:42
	 * @return: PaymentInfoBO
	 */
	private PaymentInfoBO getRechargeParam(RechargeParamVO rechargeParam, UserInfoBO userInfo, String transCode, String channelType) {
		PaymentInfoBO paymentInfo = new PaymentInfoBO(rechargeParam, userInfo, transCode);
		// 拼装第三方接口支付需要的参数
		paymentInfo.setValidOrder(validTime);
		// paymentInfo.setInfoOrder(transCode);// 支付请求
		if (!ObjectUtil.isBlank(rechargeParam.getReturnUrl())) {
			String return_url = PayUtil.getReturnUrl(rechargeParam.getReturnUrl(), transCode);
			return_url = return_url.replace("=", "_");
			paymentInfo.setAttach("recharge," + return_url);// 充值，在回调的时候，根据这个参数判断是调用充值还是支付
			paymentInfo.setUrlReturn(PayUtil.getReturnUrl(rechargeParam.getReturnUrl(), transCode));
		}
		paymentInfo.setNotifyUrl(PayUtil.getPayMethod(channelType, notifyUrl));
		paymentInfo.setRegisterTime(userInfo.getRegisterTime());

		// 易宝的支付跳到
		if (channelType.equals(PayConstants.ChannelTypeEnum.YEEPAY_FAST.name())) {
			paymentInfo.setUrlReturn(returnUrl);
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.NOWPAY_WAP.name())) {
			// 1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
			String urlr = PayUtil.getPayMethod(transCode, "{noOrder}", nowRechargeReturnUrl);
			urlr = PayUtil.getPayMethod(paymentInfo.getPayPlatform().toString(), "{platform}", urlr);
			paymentInfo.setUrlReturn(urlr);
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.PALMPAY_WAP.name())) {
			// 掌易付支付，带token过去
			String url = PayUtil.getReturnUrl(rechargeParam.getReturnUrl(), transCode);
			paymentInfo.setUrlReturn(url + "&token=" + rechargeParam.getToken());
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_FAST.name()) || channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_WEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.LIANLIAN_WAP.name())) {
			// 1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
			String urlr = PayUtil.getPayMethod(transCode, "{noOrder}", lianRechargeReturnUrl);
			urlr = PayUtil.getPayMethod(paymentInfo.getPayPlatform().toString(), "{platform}", urlr);
			String isActivity = "0";// 不是活动充值
			if (!ObjectUtil.isBlank(rechargeParam.getHdCode())) {
				isActivity = "1";// 是活动充值
			}
			urlr = PayUtil.getPayMethod(isActivity, "{isActivity}", urlr);
			paymentInfo.setUrlReturn(urlr);
		} else if (channelType.equals(PayConstants.ChannelTypeEnum.SANDPAY_WEB.name())) {// 六度支付
		} else if (rechargeParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WEB.getKey())
				|| rechargeParam.getPlatform().equals(PayConstants.TakenPlatformEnum.WAP.getKey())
						&& (channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.DIVINEPAY_CARDWAP.name()))
				|| channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_WEB.name()) || channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_APP.name()) || channelType.equals(PayConstants.ChannelTypeEnum.NATIONAL_WAP.name())) {
			// 1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
			String urlr = PayUtil.getPayMethod(transCode, "{noOrder}", lianRechargeReturnUrl);
			urlr = PayUtil.getPayMethod(paymentInfo.getPayPlatform().toString(), "{platform}", urlr);
			String isActivity = "0";// 不是活动充值
			if (!ObjectUtil.isBlank(rechargeParam.getHdCode())) {
				isActivity = "1";// 是活动充值
			}
			urlr = PayUtil.getPayMethod(isActivity, "{isActivity}", urlr);
			paymentInfo.setUrlReturn(urlr);
		}

		String name = "2Ncai-" + paymentInfo.getNoOrder();
		paymentInfo.setNameGoods(name);
		return paymentInfo;
	}

	/**  
	* 方法说明: 添加红包交易流水
	* @auth: xiongJinGang
	* @param cmsRecharge
	* @param operateCouponPO
	* @throws Exception
	* @time: 2017年8月21日 下午5:19:01
	* @return: void 
	*/
	public void addTransRed(CmsRechargeVO cmsRecharge, OperateCouponPO operateCouponPO) throws Exception {
		// 生成彩金红包交易记录
		TransRedPO transRed = new TransRedPO();
		transRed.setRedCode(operateCouponPO.getRedCode());
		transRed.setRedTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRed.setUserId(cmsRecharge.getUserId());
		transRed.setTransType(PayConstants.TransTypeEnum.RECHARGE.getKey());
		transRed.setOrderInfo(Constants.RED_REMARK_RECHARGE_INFO);// 充值赠送
		transRed.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());
		transRed.setCreateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRed.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易状态 0：交易失败；1：交易成功；
		transRed.setTransAmount(cmsRecharge.getRechargeAmount());
		transRed.setAftTransAmount(cmsRecharge.getRechargeAmount());// 红包交易后金额
		logger.debug("生成红包交易流水参数【" + transRed.toString() + "】");
		transRedService.addTransRed(transRed);
	}

}
