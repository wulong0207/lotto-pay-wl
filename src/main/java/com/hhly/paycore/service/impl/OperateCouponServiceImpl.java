package com.hhly.paycore.service.impl;

import java.util.Date;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.OperateCouponMapper;
import com.hhly.paycore.dao.TransRedMapper;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.RedTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 彩金红包实   现类
 * @author xiongJinGang
 * @date 2017年3月29日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("operateCouponService")
public class OperateCouponServiceImpl implements OperateCouponService {
	private static final Logger logger = LoggerFactory.getLogger(OperateCouponServiceImpl.class);
	@Resource
	private OperateCouponMapper operateCouponMapper;
	@Resource
	private TransRedMapper transRedMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private TransRedService transRedService;

	@Override
	public OperateCouponBO findByRedCode(String redCode) {
		return operateCouponMapper.getCouponeByRedCode(redCode);
	}

	@Override
	public ResultBO<?> findCouponByRedCode(String redCode) {
		OperateCouponBO operateCouponBO = null;
		try {
			operateCouponBO = findByRedCode(redCode);
		} catch (Exception e) {
			logger.error("获取红包【" + redCode + "】详情异常。" + e.getMessage());
			return ResultBO.err(MessageCodeConstants.PAY_RED_CODE_ERROR_SERVICE);
		}
		if (ObjectUtil.isBlank(operateCouponBO)) {
			return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
		}
		return ResultBO.ok(operateCouponBO);
	}

	@Override
	public OperateCouponBO findUserCouponByRedCode(Integer userId, String redCode) {
		return operateCouponMapper.getUserCouponeByRedCode(userId, redCode);
	}

	@Override
	public int dealOperateCoupon(OperateCouponPO operateCouponPO) throws Exception {
		return operateCouponMapper.updateOperateCouponStatus(operateCouponPO);
	}

	@Override
	public ResultBO<?> updateRedInfo(TransRechargeBO transRechargeBO, Short transType, String operateInfo) throws Exception {
		OperateCouponBO operateCouponBO = operateCouponMapper.getCouponeByRedCode(transRechargeBO.getRedCode());
		if (ObjectUtil.isBlank(operateCouponBO)) {
			logger.error("获取红包【" + transRechargeBO.getRedCode() + "】信息失败");
			return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
		}
		Double aftTransAmount = 0d;// 红包使用包金额
		Double transRedAmount = 0d;// 红包交易金额
		// 彩金红包
		if (operateCouponBO.getRedType().equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
			// 彩金红包余额
			Double redBlance = operateCouponBO.getRedBalance();
			int redMoney = MathUtil.compareTo(redBlance, transRechargeBO.getRedAmount());
			// 彩金红包的余额刚好等于红包的使用金额，设置为已使用
			if (redMoney == 0) {
				String redStatus = PayConstants.RedStatusEnum.ALREADY_USE.getKey();
				operateCouponBO.setRedStatus(redStatus);
			}
			// 计算红包金额（当前彩金红包余额=钱包中彩金余额-已使用红包金额）
			operateCouponBO.setRedBalance(MathUtil.sub(redBlance, transRechargeBO.getRedAmount()));

			// 2、更新彩金红包的余额及状态
			OperateCouponPO operateCouponPO = new OperateCouponPO(operateCouponBO);
			// ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
			// BeanUtils.copyProperties(operateCouponPO, operateCouponBO);
			operateCouponMapper.updateOperateCouponStatus(operateCouponPO);

			aftTransAmount = operateCouponBO.getRedBalance();// 红包余额
			transRedAmount = transRechargeBO.getRedAmount();// 当前红包的交易金额
		} else {
			// 普通红包
			OperateCouponPO operateCouponPO = new OperateCouponPO();
			operateCouponBO.setRedStatus(PayConstants.RedStatusEnum.ALREADY_USE.getKey());// 改成已使用
			ConvertUtils.register(new DateConverter(null), java.util.Date.class);// 添加这一行代码，重新注册一个转换器
			BeanUtils.copyProperties(operateCouponPO, operateCouponBO);
			operateCouponPO.setRedBalance(0d);// 2018-01-13新加，红包余额设置成0
			operateCouponMapper.updateOperateCouponStatus(operateCouponPO);

			RedTypeEnum redTypeEnum = PayConstants.RedTypeEnum.getEnum(operateCouponBO.getRedType());
			switch (redTypeEnum) {
			case RECHARGE_DISCOUNT: // 充值红包
				transRedAmount = Double.valueOf(operateCouponBO.getMinSpendAmount());
				break;
			case CONSUMPTION_DISCOUNT: // 满减红包，使用金额取面额
				transRedAmount = Double.valueOf(operateCouponBO.getRedValue());
				break;
			case BONUS_RED:// 加奖红包
				// 暂时未做
				break;
			case BIG_PACKAGE: // 大礼包
				// 暂时未做
				break;
			case RANDOM_RED:// 随机红包
				Short randomRedType = operateCouponBO.getRandomRedType();
				// 随机红包生成的红包类型不为空，并且生成的红包为彩金红包类型
				if (!ObjectUtil.isBlank(randomRedType) && randomRedType.equals(PayConstants.RedTypeEnum.RED_COLOR.getKey())) {
					addRedColor(operateCouponBO, operateCouponBO.getRedBalance());// 随机红包，送多少钱取 redBalance里面的值
				} else {
					logger.info("使用了随机红包，但需要生成的红包类型为空或者生成的不是彩金红包，暂时不操作");
				}
				break;
			default:
				break;
			}
		}
		// 订单号为空，取交易号；不为空，取订单号
		String orderCode = ObjectUtil.isBlank(transRechargeBO.getOrderCode()) ? transRechargeBO.getTransRechargeCode() : transRechargeBO.getOrderCode();
		// 添加红包交易记录
		transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), transType, transRedAmount, aftTransAmount, operateInfo, orderCode);
		return ResultBO.ok(operateCouponBO);
	}

	@Override
	public int updateOperateCoupon(OperateCouponBO operateCouponBO) throws Exception {
		OperateCouponPO operateCouponPO = new OperateCouponPO(operateCouponBO);
		return operateCouponMapper.updateOperateCouponStatus(operateCouponPO);
	}

	@Override
	public int updateOperateCoupon(OperateCouponPO operateCouponPO) throws Exception {
		return operateCouponMapper.updateOperateCouponStatus(operateCouponPO);
	}

	@Override
	public OperateCouponPO addRedColor(OperateCouponBO operateCouponBO, Double redAmount) throws Exception {
		OperateCouponPO operateCouponPO = new OperateCouponPO();
		Date nowDate = DateUtil.convertStrToDate(DateUtil.getNow());
		operateCouponPO.setUserId(operateCouponBO.getUserId());
		operateCouponPO.setOperateLotteryId(Constants.getActivityLotteryId());// 默认
		operateCouponPO.setRedName(PayConstants.RedTypeEnum.RED_COLOR.getValue());// 红包名称；按照指定规则生成；根据红包类型，红包面额，最低消费生成红包名称。
		operateCouponPO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));// 红包编号
		operateCouponPO.setRedCategory(PayConstants.RedCategoryEnum.VIRTUAL.getKey());// 红包类别 1：实体；2：虚拟
		operateCouponPO.setActivityCode(operateCouponBO.getRedCode());// 活动管理编号
		operateCouponPO.setObtainTime(nowDate);// 用户获取红包的时间
		operateCouponPO.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());// 1：充值优惠；2：消费折扣；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
		operateCouponPO.setRedValue(redAmount);// 红包实际金额
		operateCouponPO.setRedBalance(redAmount);// 红包余额
		operateCouponPO.setMinSpendAmount(0);// 使用红包的条件。彩金红包没有最低使用条件限制
		operateCouponPO.setEctivityDay(Constants.RED_COLOR_OVERDUE_TIME_30_DAYS);// 用户获取红包后的有效天数。20年
		// 2017-09-07 彩金红包去掉有效期
		// String sevenAfterDay = DateUtil.getBeforeOrAfterYearForString(Constants.RED_COLOR_OVERDUE_TIME, DateUtil.DATE_FORMAT)+" 23:59:59";
		// Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
		// operateCouponPO.setRedOverdueTime(redOverdueTime);// 过期时间
		operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 1：待激活；2：待派发；3：可使用；4：已过期；5：已作废；6：已使用
		// operateCouponPO.setLimitLottery("");// 限制彩种
		// operateCouponPO.setLimitPlatform(platform);// 默认为空，都可以使用
		// 描述
		String remark = ObjectUtil.isBlank(operateCouponBO.getRemark()) ? Constants.RED_REMARK_RECHARGE_INFO : operateCouponBO.getRemark();
		operateCouponPO.setRedRemark(remark);
		operateCouponPO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
		operateCouponPO.setRandomRedType(null);
		operateCouponPO.setCreateTime(nowDate);
		// operateCouponPO.setChannelId(operateCouponBO.getChannelId());// 渠道ID，默认为空，所以渠道都可以使用
		operateCouponPO.setOperateLotteryId("0");// 关联彩种运营管理ID，list 默认为0
		// operateCouponPO.setRedLabel(redLabel);// 可自定义的红包标签。
		// operateCouponPO.setLimitLottery(limitLottery);//lottery_id；多选，list；用逗号隔开
		// operateCouponPO.setUseRule(useRule);//自定义的说明。根据类型，面值，最低消费金额，平台，彩种，根据文档进行判断是否可用。规则为固定的。
		// operateCouponPO.setLimitLotteryChild(limitLotteryChild);//限制红包子玩法
		// operateCouponPO.setRandomRedType(randomRedType);//随机红包生成红包类型为：1.彩金红包2.满减红包3.充值红包4. 加奖红包
		Short redSource = ObjectUtil.isBlank(operateCouponBO.getRedSource()) ? 2 : operateCouponBO.getRedSource();
		operateCouponPO.setRedSource(redSource);// 来源类型:1：活动；2：券
		int num = operateCouponMapper.addOperateCoupon(operateCouponPO);
		if (num <= 0) {
			logger.error("生成彩金红包失败，参数：" + operateCouponPO.toString());
			throw new RuntimeException("生成彩金红包失败");
		}
		return operateCouponPO;
	}

	@Override
	public OperateCouponPO addRechargeToRed(String activityCode, Integer userId, Double amount) {
		OperateCouponPO operateCouponPO = new OperateCouponPO();
		Date nowDate = DateUtil.convertStrToDate(DateUtil.getNow());
		operateCouponPO.setUserId(userId);
		operateCouponPO.setRedName(PayConstants.RedTypeEnum.RED_COLOR.getValue());// 红包名称；按照指定规则生成；根据红包类型，红包面额，最低消费生成红包名称。
		operateCouponPO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));// 红包编号
		operateCouponPO.setRedCategory(PayConstants.RedCategoryEnum.VIRTUAL.getKey());// 红包类别 1：实体；2：虚拟
		operateCouponPO.setActivityCode(activityCode);// 活动管理编号
		operateCouponPO.setObtainTime(nowDate);// 用户获取红包的时间
		operateCouponPO.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());// 1：充值优惠；2：消费折扣；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
		operateCouponPO.setRedValue(amount);// 红包实际金额
		operateCouponPO.setRedBalance(amount);// 红包余额
		operateCouponPO.setMinSpendAmount(0);// 使用红包的条件。彩金红包没有最低使用条件限制
		operateCouponPO.setEctivityDay(Constants.RED_COLOR_OVERDUE_TIME);// 用户获取红包后的有效天数。20年
		operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 1：待激活；2：待派发；3：可使用；4：已过期；5：已作废；6：已使用
		operateCouponPO.setRedRemark(Constants.RED_REMARK_RECHARGE_INFO);
		operateCouponPO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
		operateCouponPO.setRandomRedType(null);
		operateCouponPO.setCreateTime(nowDate);
		operateCouponPO.setOperateLotteryId("0");// 关联彩种运营管理ID，list 默认为0
		operateCouponPO.setRedSource((short) 1);// 来源类型:1：活动；2：券
		int num = operateCouponMapper.addOperateCoupon(operateCouponPO);
		if (num <= 0) {
			logger.error("生成彩金红包失败，参数：" + operateCouponPO.toString());
			throw new RuntimeException("生成彩金红包失败");
		}
		return operateCouponPO;
	}

	@Override
	public OperateCouponPO addRedColor(OperateCouponBO operateCouponBO) throws Exception {
		OperateCouponPO operateCouponPO = new OperateCouponPO(operateCouponBO);
		if (ObjectUtil.isBlank(operateCouponPO.getOperateLotteryId())) {
			operateCouponPO.setOperateLotteryId(Constants.getActivityLotteryId());// 默认
		}
		int num = operateCouponMapper.addOperateCoupon(operateCouponPO);
		if (num <= 0) {
			logger.error("生成优惠券失败，参数：" + operateCouponPO.toString());
			throw new RuntimeException("生成优惠券失败");
		}
		return operateCouponPO;
	}

	@Override
	public OperateCouponPO addCoupon(OperateCouponPO operateCouponPO) throws Exception {
		if (ObjectUtil.isBlank(operateCouponPO.getOperateLotteryId())) {
			operateCouponPO.setOperateLotteryId(Constants.getActivityLotteryId());// 默认
		}
		int num = operateCouponMapper.addOperateCoupon(operateCouponPO);
		if (num <= 0) {
			logger.error("生成优惠券失败，参数：" + operateCouponPO.toString());
			throw new RuntimeException("生成优惠券失败");
		}
		return operateCouponPO;
	}

	@Override
	public OperateCouponPO addAgentRedColor(OperateCouponBO operateCouponBO, Double redAmount) throws Exception {
		OperateCouponPO operateCouponPO = new OperateCouponPO();
		Date nowDate = DateUtil.convertStrToDate(DateUtil.getNow());
		operateCouponPO.setUserId(operateCouponBO.getUserId());
		operateCouponPO.setOperateLotteryId(Constants.getActivityLotteryId());// 默认
		operateCouponPO.setRedName(PayConstants.RedTypeEnum.RED_COLOR.getValue());// 红包名称；按照指定规则生成；根据红包类型，红包面额，最低消费生成红包名称。
		operateCouponPO.setRedCode(OrderNoUtil.getOrderNo(NumberCode.COUPON));// 红包编号
		operateCouponPO.setRedCategory(PayConstants.RedCategoryEnum.VIRTUAL.getKey());// 红包类别 1：实体；2：虚拟
		operateCouponPO.setActivityCode(operateCouponBO.getActivityCode());// 活动管理编号
		operateCouponPO.setObtainTime(nowDate);// 用户获取红包的时间
		operateCouponPO.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());// 1：充值优惠；2：消费折扣；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
		operateCouponPO.setRedValue(redAmount);// 红包实际金额
		operateCouponPO.setRedBalance(redAmount);// 红包余额
		operateCouponPO.setMinSpendAmount(0);// 使用红包的条件。彩金红包没有最低使用条件限制
		operateCouponPO.setEctivityDay(Constants.RED_COLOR_OVERDUE_TIME_30_DAYS);// 用户获取红包后的有效天数。30
		// String sevenAfterDay = DateUtil.getBeforeOrAfterYearForString(Constants.RED_COLOR_OVERDUE_TIME, DateUtil.DATE_FORMAT) + " 23:59:59";
		// Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
		// operateCouponPO.setRedOverdueTime(redOverdueTime);// 过期时间
		operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 1：待激活；2：待派发；3：可使用；4：已过期；5：已作废；6：已使用
		// operateCouponPO.setLimitLottery("");// 限制彩种
		operateCouponPO.setRedRemark(Constants.CHANNEL_RECHARGE);
		operateCouponPO.setCreateBy(Constants.CHANNEL_RECHARGE);
		operateCouponPO.setRandomRedType(null);
		operateCouponPO.setCreateTime(nowDate);
		operateCouponPO.setChannelId(operateCouponBO.getChannelId());// 渠道ID
		// operateCouponPO.setRedLabel(redLabel);// 可自定义的红包标签。
		// operateCouponPO.setLimitLottery(limitLottery);//lottery_id；多选，list；用逗号隔开
		// operateCouponPO.setUseRule(useRule);//自定义的说明。根据类型，面值，最低消费金额，平台，彩种，根据文档进行判断是否可用。规则为固定的。
		// operateCouponPO.setLimitLotteryChild(limitLotteryChild);//限制红包子玩法
		// operateCouponPO.setRandomRedType(randomRedType);//随机红包生成红包类型为：1.彩金红包2.满减红包3.充值红包4. 加奖红包
		operateCouponPO.setRedSource((short) 2);// 来源类型:1：活动；2：券
		int num = operateCouponMapper.addOperateCoupon(operateCouponPO);
		if (num <= 0) {
			logger.error("生成彩金红包失败，参数：" + operateCouponPO.toString());
			throw new RuntimeException("生成彩金红包失败");
		}
		return operateCouponPO;
	}

	/**  
	* 方法说明: 充值活动、添加活动彩金
	* @auth: xiongJinGang
	* @param cmsRechargeVO
	* @throws Exception
	* @time: 2017年8月21日 下午5:09:40
	* @return: OperateCouponPO 
	*/
	@Override
	public OperateCouponPO addOperateCoupon(CmsRechargeVO cmsRechargeVO, TransRechargeBO transRechargeBO) throws Exception {
		OperateCouponPO operateCouponPO = new OperateCouponPO();
		Date nowDate = DateUtil.convertStrToDate(DateUtil.getNow());
		operateCouponPO.setUserId(cmsRechargeVO.getUserId());
		operateCouponPO.setOperateLotteryId(Constants.getActivityLotteryId());// 默认
		operateCouponPO.setRedName(PayConstants.RedTypeEnum.RED_COLOR.getValue());// 红包名称；按照指定规则生成；根据红包类型，红包面额，最低消费生成红包名称。
		operateCouponPO.setRedCode(transRechargeBO.getRedCode());// 红包编号
		operateCouponPO.setRedCategory(PayConstants.RedCategoryEnum.VIRTUAL.getKey());// 红包类别 1：实体；2：虚拟
		operateCouponPO.setActivityCode(cmsRechargeVO.getActivityCode());// 活动管理编号
		operateCouponPO.setObtainTime(nowDate);// 用户获取红包的时间
		operateCouponPO.setRedType(PayConstants.RedTypeEnum.RED_COLOR.getKey());// 1：充值优惠；2：消费折扣；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
		operateCouponPO.setRedValue(cmsRechargeVO.getRechargeAmount());// 红包实际金额
		operateCouponPO.setRedBalance(cmsRechargeVO.getRechargeAmount());// 红包余额
		operateCouponPO.setMinSpendAmount(0);// 使用红包的条件。彩金红包没有最低使用条件限制
		operateCouponPO.setEctivityDay(20 * 365);// 用户获取红包后的有效天数。20年
		// operateCouponPO.setRedOverdueTime(DateUtil.getBeforeOrAfterYear(20, DateUtil.DEFAULT_FORMAT));// 过期时间
		// String sevenAfterDay = DateUtil.getBeforeOrAfterDate(20 * 365, DateUtil.DATE_FORMAT) + " 23:59:59";
		// Date redOverdueTime = DateUtil.convertStrToDate(sevenAfterDay);
		// operateCouponPO.setRedOverdueTime(redOverdueTime);

		operateCouponPO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());// 1：待激活；2：待派发；3：可使用；4：已过期；5：已作废；6：已使用
		operateCouponPO.setRedRemark(Constants.RED_REMARK_RECHARGE_INFO);
		operateCouponPO.setCreateBy(Constants.RED_REMARK_SYSTEM_SEND);
		operateCouponPO.setRandomRedType(null);
		operateCouponPO.setCreateTime(nowDate);
		operateCouponPO.setRedSource(PayConstants.RedSourceEnum.ACTIVITY.getKey());// 来源类型:1：活动；2：券
		return addCoupon(operateCouponPO);
	}
}
