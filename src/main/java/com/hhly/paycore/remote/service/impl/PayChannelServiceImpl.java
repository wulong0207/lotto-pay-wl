package com.hhly.paycore.remote.service.impl;

import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.remote.service.IPayChannelService;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.OperateMarketChannelService;
import com.hhly.paycore.service.PayBankLimitService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.PayChannelLimitService;
import com.hhly.paycore.service.PayChannelMgrService;
import com.hhly.paycore.service.PayChannelService;
import com.hhly.skeleton.base.common.PayEnum;
import com.hhly.skeleton.base.common.PayEnum.EntranceEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.UserPayTypeBO;
import com.hhly.skeleton.pay.channel.bo.PayChannelBO;
import com.hhly.skeleton.pay.vo.ToRechargeParamVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】支付渠道管理
 * @author xiongJinGang
 * @date 2017年12月13日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iPayChannelService")
public class PayChannelServiceImpl implements IPayChannelService {
	private static final Logger logger = LoggerFactory.getLogger(PayChannelServiceImpl.class);

	@Resource
	private BankcardService BankcardService;
	@Resource
	private PayChannelService payChannelService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private PayBankService payBankService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private PayBankLimitService bankLimitService;
	@Resource
	private PayChannelLimitService payChannelLimitService;
	@Resource
	private PayChannelMgrService payChannelMgrService;
	@Resource
	private OperateMarketChannelService operateMarketChannelService;

	/**
	 * 获取用户支付方式列表
	 *
	 * @param token
	 * @return
	 */
	@Override
	public List<UserPayTypeBO> findUserPayTypes(ToRechargeParamVO toRechargeParamVO) {
		if (logger.isDebugEnabled()) {
			logger.debug("跳转充值请求参数：" + toRechargeParamVO.toString());
		}
		UserInfoBO userInfoBO = userUtil.getUserByToken(toRechargeParamVO.getToken());
		if (ObjectUtil.isBlank(userInfoBO)) {
			return null;
		}
		String orderBy = toRechargeParamVO.getPlatform();
		// 如果平台为wap，全部转换成h5，因为平台的定义和支付渠道中的平台定义不一样，一个是Wap，一个是H5
		if (!ObjectUtil.isBlank(orderBy) && PayConstants.TakenPlatformEnum.WAP.getPlatForm().equals(orderBy)) {
			orderBy = PayEnum.EntranceEnum.H5.getValue();
		}
		Integer userId = userInfoBO.getId();
		String browserType = ObjectUtil.isBlank(toRechargeParamVO.getBrowserType()) ? "" : toRechargeParamVO.getBrowserType();
		StringBuffer key = new StringBuffer(CacheConstants.P_CORE_USER_PAY_CHANNEL).append(userId).append(orderBy).append(browserType);
		List<UserPayTypeBO> all = redisUtil.getObj(key.toString(), new ArrayList<UserPayTypeBO>());
		if (ObjectUtil.isBlank(all)) {
			all = new ArrayList<UserPayTypeBO>();

			// 用户可用银行卡列表
			List<PayBankcardBO> userBankList = BankcardService.findUserBankListFromCache(userId);
			// 第三方支付列表
			List<PayBankBO> thirdPayBankList = payBankService.findBankListByPlatFromCache(orderBy, PayConstants.PayBankPayTypeEnum.THIRD.getKey());
			// 判断渠道是否为马甲
			boolean isMajia = operateMarketChannelService.isMajia(toRechargeParamVO.getChannelId());
			// 以下是拼装第三方支付
			fillThirdChannel(orderBy, userInfoBO, all, thirdPayBankList, toRechargeParamVO, isMajia);
			// 以下是拼装银行卡支付
			fillBankChannel(orderBy, userInfoBO, all, userBankList, toRechargeParamVO.getRechargeType(), isMajia);
			if (!ObjectUtil.isBlank(all)) {
				// 按照sort排下序
				Collections.sort(all, new Comparator<UserPayTypeBO>() {
					@Override
					public int compare(UserPayTypeBO o1, UserPayTypeBO o2) {
						return o1.getSort().compareTo(o2.getSort());
					}
				});
				redisUtil.addObj(key.toString(), all, CacheConstants.ONE_WEEK);// 存一周
			}
		}
		return all;
	}

	/**  
	* 方法说明: 拼装银行卡支付
	* @auth: xiongJinGang
	* @param platForm 平台
	* @param userInfoBO
	* @param all
	* @param userBankList
	* @time: 2017年12月9日 下午2:18:21
	* @return: void 
	*/
	private void fillBankChannel(String platForm, UserInfoBO userInfoBO, List<UserPayTypeBO> all, List<PayBankcardBO> userBankList, Short transType, boolean isMajia) {
		UserPayTypeBO returnBO = null;
		// 用户的银行列表
		for (PayBankcardBO payBankCardBO : userBankList) {
			if (PayConstants.RechargeTypeEnum.RECHARGE.getKey().equals(transType)) {
				// 信用卡不能进行充值
				if (payBankCardBO.getBanktype().equals(PayConstants.BankCardTypeEnum.CREDIT.getKey())) {
					continue;
				}
			}

			// 根据用户银行卡信息中的银行ID查找银行信息
			PayBankBO payBank = payBankService.findSigleBankFromCache(payBankCardBO.getBankid());
			if (ObjectUtil.isBlank(payBank)) {
				continue;
			}
			// 银行状态为不可用，则不使用
			if (PayConstants.BankStatusEnum.DISABLE.getKey().equals(payBank.getStatus())) {
				continue;
			}

			// 获取可用的支付渠道
			List<PayChannelBO> channelList = getPayChannel(null, payBankCardBO.getBankid(), payBankCardBO.getBanktype(), platForm, isMajia);
			// 如果是马甲包，支付渠道为空，继续下一个
			if ((ObjectUtil.isBlank(channelList) || channelList.size() == 0) && isMajia) {
				continue;
			}

			returnBO = new UserPayTypeBO();
			Short bankStatus = payBank.getStatus();// 银行状态
			// 没有可用的渠道或者银行状态为关闭，将该支付渠道设置成不可用
			if (ObjectUtil.isBlank(channelList) || PayConstants.BankStatusEnum.DISABLE.getKey().equals(bankStatus)) {
				bankStatus = PayConstants.BankStatusEnum.DISABLE.getKey();
				returnBO.setReason(Constants.PAY_CHANNEL_STOP_USE_TIP);// 没有获取到可用的支付渠道
				if (logger.isInfoEnabled()) {
					logger.info("银行【" + payBank.getcName() + "】没有可用的支付渠道！银行卡ID" + payBank.getId() + "，银行类型：" + payBankCardBO.getBanktype() + "，平台：" + platForm);
				}
			}
			returnBO.setBankId(payBankCardBO.getBankid());
			returnBO.setBankName(payBank.getcName());
			returnBO.setBankType(payBankCardBO.getBanktype());// 银行卡类型:1储蓄卡;2信用卡
			// 格式化银行卡号变为 **1234
			returnBO.setCardCode(StringUtil.hideHeadString(payBankCardBO.getCardcode()));
			// 是否开启快捷支付为空，默认0（未开通）
			returnBO.setOpenBank(ObjectUtil.isBlank(payBankCardBO.getOpenbank()) ? 0 : payBankCardBO.getOpenbank());
			returnBO.setBankCardId(payBankCardBO.getId());// 银行卡ID
			returnBO.setOverdue(payBankCardBO.getOverdue());// 有效时间
			returnBO.setFlag(bankStatus);// 银行状态 0停用 1可用
			returnBO.setsLogo(payBank.getsLogo());// 小图标
			returnBO.setbLogo(payBank.getbLogo());// 大图标
			// 银行卡类型:1储蓄卡;2信用卡;3第三方支付。判断是否为信用卡，信用卡判断有效期
			if (payBankCardBO.getBanktype().equals(PayConstants.BankCardTypeEnum.CREDIT.getKey()) && !ObjectUtil.isBlank(payBankCardBO.getOverdue())) {
				try {
					boolean overdueValidate = DateUtil.validateCredCardOver(payBankCardBO.getOverdue());
					if (!overdueValidate) {
						returnBO.setReason(Constants.CREDIT_EXPIRED_TIPS);
						returnBO.setFlag(PayConstants.BankStatusEnum.EXPIRED.getKey());
					}
				} catch (ParseException e) {
					logger.error("信用卡【" + payBankCardBO.getCardcode() + "】有效期【" + payBankCardBO.getOverdue() + "】格式错误", e);
				}
			}
			// 判断支付渠道是否可用
			checkChannel(channelList, returnBO);
			setUserPayTypeSort(userInfoBO, returnBO, payBank, null);
			all.add(returnBO);
		}
	}

	/**  
	* 方法说明: 第三方支付拼装
	* @auth: xiongJinGang
	* @param orderBy
	* @param userInfoBO
	* @param all
	* @param thirdPayBankList
	* @param returnBO
	* @time: 2017年12月8日 下午6:49:24
	* @return: UserPayTypeBO 
	*/
	private void fillThirdChannel(String orderBy, UserInfoBO userInfoBO, List<UserPayTypeBO> all, List<PayBankBO> thirdPayBankList, ToRechargeParamVO toRechargeParamVO, boolean isMajia) {
		UserPayTypeBO returnBO = null;
		if (!ObjectUtil.isBlank(thirdPayBankList)) {
			for (PayBankBO payBankBO : thirdPayBankList) {
				if (PayConstants.RechargeTypeEnum.PAY.getKey().equals(toRechargeParamVO.getRechargeType())) {
					// 充值卡或者转账汇款的，不能进行支付。153是pay_bank中配置的银行编码
					if (!ObjectUtil.isBlank(payBankBO.getCode()) && (payBankBO.getCode().equals(PayConstants.PayBankCodeEnum.CARD.getKey()) || payBankBO.getCode().equals("153"))) {
						continue;
					}
				}
				// 银行状态为不可用，则不使用
				if (PayConstants.BankStatusEnum.DISABLE.getKey().equals(payBankBO.getStatus())) {
					continue;
				}
				returnBO = new UserPayTypeBO();
				Short bankStatus = payBankBO.getStatus();// 银行状态
				// 获取可用的支付渠道（包括暂停在内的）
				List<PayChannelBO> channelList = getPayChannel(payBankBO, payBankBO.getId(), PayConstants.PayChannelCardTypeEnum.THIRD.getKey(), orderBy, isMajia);
				// 如果是马甲包，支付渠道为空，继续下一个
				if ((ObjectUtil.isBlank(channelList) || channelList.size() == 0) && isMajia) {
					continue;
				}
				// 没有可用的渠道或者银行状态为关闭，将该支付渠道设置成不可用
				if (ObjectUtil.isBlank(channelList) || PayConstants.BankStatusEnum.DISABLE.getKey().equals(bankStatus)) {
					bankStatus = PayConstants.BankStatusEnum.DISABLE.getKey();
					returnBO.setReason(Constants.PAY_CHANNEL_STOP_USE_TIP);// 没有获取到可用的支付渠道
				}
				returnBO.setBankId(payBankBO.getId());
				returnBO.setBankName(payBankBO.getcName());
				returnBO.setBankType(PayConstants.PayTypeEnum.THIRD_PAYMENT.getKey());// 默认第三方支付 银行卡类型:1储蓄卡;2信用卡;3第三方支付
				returnBO.setFlag(bankStatus);// 0停用 1可用
				returnBO.setsLogo(payBankBO.getsLogo());// 小图标
				returnBO.setbLogo(payBankBO.getbLogo());// 大图标
				returnBO.setBankCode(payBankBO.getCode());// 银行的唯一编码
				// bankStatus是可用状态时，才判断支付渠道是否存在维护及暂停
				checkChannel(channelList, returnBO);
				setUserPayTypeSort(userInfoBO, returnBO, payBankBO, toRechargeParamVO.getBrowserType());
				all.add(returnBO);
			}
		}
	}

	/**  
	* 方法说明: 设置用户支付方式排序
	* @auth: xiongJinGang
	* @param userInfoBO
	* @param returnBO
	* @param payBankBO
	* @time: 2017年12月9日 下午2:17:11
	* @return: void 
	*/
	private void setUserPayTypeSort(UserInfoBO userInfoBO, UserPayTypeBO returnBO, PayBankBO payBankBO, String browerType) {
		// 判断银行状态，打开的排第一位，关闭的排第二位
		if (PayConstants.BankStatusEnum.OPEN.getKey().equals(returnBO.getFlag())) {
			returnBO.setSort(1);
		} else {
			returnBO.setSort(2);
		}
		// 判断浏览器类型是否为空
		if (!ObjectUtil.isBlank(browerType) && !ObjectUtil.isBlank(payBankBO.getCode())) {
			if (PayConstants.BrowerTypeEnum.ALIPAY.getKey().equals(browerType) && PayConstants.PayBankCodeEnum.ALIPAY.getKey().equals(payBankBO.getCode())) {
				returnBO.setSort(0);// 浏览器类型是支付宝并且银行编码是支付宝，支付宝的支付方式排在第一位
			} else if (PayConstants.BrowerTypeEnum.WECHAT.getKey().equals(browerType) && PayConstants.PayBankCodeEnum.WECHAT.getKey().equals(payBankBO.getCode())) {
				returnBO.setSort(0);// 浏览器类型是微信并且银行编码是微信，微信的支付方式排在第一位
			} else if (PayConstants.BrowerTypeEnum.QQ.getKey().equals(browerType) && PayConstants.PayBankCodeEnum.QQ.getKey().equals(payBankBO.getCode())) {
				returnBO.setSort(0);// 浏览器类型是QQ并且银行编码是QQ，QQ的支付方式排在第一位
			}
		} else {
			// 用户最近支付id 等于存的id 默认显示它
			if (!ObjectUtil.isBlank(userInfoBO.getUserPayId())) {
				if (userInfoBO.getUserPayId().equals(payBankBO.getId())) {
					returnBO.setIsRecentlyPay(payBankBO.getId());// 最近使用支付方式id
					// 最近使用的银行可以使用,才放第一位
					if (PayConstants.BankStatusEnum.OPEN.getKey().equals(returnBO.getFlag())) {
						returnBO.setSort(0);
					}
				}
			}
		}
	}

	/**  
	* 方法说明: 检查渠道是否可用及设置最低、最高限额
	* @auth: xiongJinGang
	* @param channelList
	* @param userPayTypeBO
	* @time: 2017年12月12日 下午6:35:44
	* @return: void 
	*/
	private void checkChannel(List<PayChannelBO> channelList, UserPayTypeBO userPayTypeBO) {
		// 前面状态是可使用的，再判断所有支付渠道是否都不满足
		if (userPayTypeBO.getFlag().equals(PayConstants.BankStatusEnum.OPEN.getKey())) {
			boolean flag = false;// 是否有暂停时间
			Date minEndTime = null;// 设置一个最早结束时间
			Double leastAmount = 0d;// 单笔最低限额
			Double highestAmount = 0d;// 单笔最高限额

			for (PayChannelBO payChannelBO : channelList) {
				// 渠道可用，才进行下面的判断
				if (payChannelBO.getAvailable().equals(PayConstants.BankStatusEnum.OPEN.getKey())) {
					// 存在维护暂停时间，并且暂停开始时间及暂停结束时间都不为空，需要判断当前交易时间是否在维护时间内
					if (PayConstants.BankStatusEnum.OPEN.getKey().equals(payChannelBO.getPause()) && !ObjectUtil.isBlank(payChannelBO.getBeginTime()) && !ObjectUtil.isBlank(payChannelBO.getEndTime())) {
						Date stopBeginTime = payChannelBO.getBeginTime();
						Date stopEndTime = payChannelBO.getEndTime();
						if (ObjectUtil.isBlank(minEndTime)) {
							minEndTime = stopEndTime;// 赋值最晚维护时间
						} else {
							if (DateUtil.compare(minEndTime, stopEndTime) > 0) {
								minEndTime = stopEndTime;// 赋值最晚维护时间
							}
						}
						if (!ObjectUtil.isBlank(stopBeginTime) && !ObjectUtil.isBlank(stopEndTime)) {
							userPayTypeBO.setStartTime(stopBeginTime);
							userPayTypeBO.setEndTime(stopEndTime);
							Date nowDate = DateUtil.getNowDate(null);
							int bigBigin = DateUtil.compare(nowDate, stopBeginTime);
							int bigEnd = DateUtil.compare(nowDate, stopEndTime);
							// 当前时间在暂停使用时间内
							if (bigBigin >= 0 && bigEnd <= 0) {
								userPayTypeBO.setFlag(PayConstants.BankStatusEnum.DISABLE.getKey());// 不可用
							} else {
								flag = true;// 有暂停，但不在开始结束时间内，就跳出
							}
						} else {
							flag = true;// 没有暂停维护时间，设置成true
						}
					} else {
						flag = true;// 没有暂停维护时间，设置成true
						// 判断当日限额

					}
					/************只要flag为true，才可以准确的获取到最低最高支付金额**************/
					if (flag) {
						// 获取最低支付金额
						if (MathUtil.compareTo(leastAmount, 0) == 0 || MathUtil.compareTo(payChannelBO.getMinPay(), leastAmount) < 0) {
							leastAmount = payChannelBO.getMinPay();
						}
						// 获取最高支付金额
						if (MathUtil.compareTo(highestAmount, 0) == 0 || MathUtil.compareTo(payChannelBO.getMaxPay(), highestAmount) > 0) {
							highestAmount = payChannelBO.getMaxPay();
						}
					}
				}
			}
			if (flag) {
				userPayTypeBO.setFlag(PayConstants.BankStatusEnum.OPEN.getKey());
				userPayTypeBO.setMaxLimit(highestAmount.toString());
				userPayTypeBO.setMinLimit(leastAmount.toString());
				userPayTypeBO.setReason(null);
				userPayTypeBO.setStartTime(null);
				userPayTypeBO.setEndTime(null);
			} else {
				userPayTypeBO.setReason(MessageFormat.format(Constants.PAY_CHANNEL_STOP_USE, DateUtil.convertDateToStr(minEndTime, DateUtil.DEFAULT_FORMAT)));
			}

		}
	}

	/**  
	* 方法说明: 获取该银行卡是否能够使用
	* @auth: xiongJinGang
	* @param payBankBO 支付银行
	* @param bankId 银行id
	* @param bankType 银行类型
	* @param platForm 支付平台
	* @param isMajia 是否为马甲
	* @time: 2018年8月9日 上午11:10:18
	* @return: List<PayChannelBO> 
	*/
	private List<PayChannelBO> getPayChannel(PayBankBO payBankBO, Integer bankId, Short bankType, String platForm, boolean isMajia) {
		// 该银行所有可选的支付渠道
		List<PayChannelBO> payChannelBOs = payChannelService.findChannelByBankIdUseCache(bankId);
		Iterator<PayChannelBO> iterator = payChannelBOs.iterator();
		while (iterator.hasNext()) {
			PayChannelBO payChannelBO = iterator.next();
			if (payChannelBO.getCardType().equals(bankType)) {
				boolean flag = false;
				// 如果传过来的银行对象不为空,并且银行编号等于支付宝，全部统一用PC的扫码支付功能
				if (!ObjectUtil.isBlank(payBankBO) && !ObjectUtil.isBlank(payBankBO.getCode()) && payBankBO.getCode().equals(PayConstants.PayBankCodeEnum.ALIPAY.getKey())) {
					flag = checkChannelStatusForPlatform(platForm, payChannelBO, isMajia);
				} else {
					flag = checkChannelStatusForPlatformOld(platForm, payChannelBO, isMajia);
				}
				if (!flag) {
					iterator.remove();
				}
			} else {
				iterator.remove();
			}
		}
		return payChannelBOs;
	}

	/**
	 * 方法说明: 验证各个平台的支付状态是否可用
	 *2018-08-07 之前的正常配置
	 * @param platForm
	 * @param payChannelBO
	 * @auth: xiongJinGang
	 * @time: 2017年4月12日 上午10:23:28
	 * @return: boolean
	 */
	private boolean checkChannelStatusForPlatformOld(String platForm, PayChannelBO payChannelBO, boolean isMajia) {
		EntranceEnum entranceEnum = PayEnum.EntranceEnum.getEnum(platForm);
		Short appInvokeType = payChannelBO.getAppInvokeType();// app调用:1调用sdk，0调用h5

		if (!ObjectUtil.isBlank(entranceEnum)) {
			switch (entranceEnum) {
			case PC:
				return payChannelBO.getPc().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
			case H5:
				return payChannelBO.getH5().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
			case WECHAT:
				Short wechat = ObjectUtil.isBlank(payChannelBO.getWechat()) ? 0 : payChannelBO.getWechat();
				return wechat.equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
			case IOS:
				// 是马甲包并且配置的是走appsdk
				if (isMajia && !ObjectUtil.isBlank(appInvokeType)) {
					return false;
				} else {
					// 如果为空或者为马甲，调用H5进行支付
					if (ObjectUtil.isBlank(appInvokeType)) {
						return payChannelBO.getH5().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
					}
					return payChannelBO.getIos().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
				}
			case ANDROID:
				if (isMajia && !ObjectUtil.isBlank(appInvokeType)) {
					return false;
				} else {
					// 如果为空或者为马甲，调用H5进行支付
					if (ObjectUtil.isBlank(appInvokeType)) {
						return payChannelBO.getH5().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
					}
					return payChannelBO.getAndroid().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
				}
			default:
				return false;
			}
		}
		return false;
	}

	/**  
	* 方法说明: h5和app走扫码支付
	* @auth: xiongJinGang
	* @param platForm
	* @param payChannelBO
	* @param isMajia
	* @time: 2018年8月7日 下午2:30:09
	* @return: boolean 
	*/
	private boolean checkChannelStatusForPlatform(String platForm, PayChannelBO payChannelBO, boolean isMajia) {
		EntranceEnum entranceEnum = PayEnum.EntranceEnum.getEnum(platForm);
		Short appInvokeType = payChannelBO.getAppInvokeType();// app调用:1调用sdk，0调用h5

		if (!ObjectUtil.isBlank(entranceEnum)) {
			switch (entranceEnum) {
			case PC:
			case H5:
				return payChannelBO.getPc().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
			case WECHAT:
				Short wechat = ObjectUtil.isBlank(payChannelBO.getWechat()) ? 0 : payChannelBO.getWechat();
				return wechat.equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
			case IOS:
			case ANDROID:
				// 是马甲包并且配置的是走appsdk
				if (isMajia && !ObjectUtil.isBlank(appInvokeType)) {
					return false;
				} else {
					return payChannelBO.getPc().equals(Short.parseShort(PayEnum.IsStartEnum.START.getValue() + ""));
				}
			default:
				return false;
			}
		}
		return false;
	}

}
