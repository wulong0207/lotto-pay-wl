package com.hhly.paycore.common;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenPlatformEnum;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.vo.OperateCouponQueryVO;

/**
 * @desc 验证红包条件有效性
 * @author xiongJinGang
 * @date 2017年4月6日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class OperateCouponUtil {

	/**  
	* 方法说明: 验证查询条件有效性
	* @auth: xiongJinGang
	* @param operateCouponQuery
	* @time: 2017年4月6日 下午5:13:27
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateCouponQueryParam(OperateCouponQueryVO operateCouponQuery) {
		// 如果不是充值，并且彩种编号为空，返回错误
		if (!operateCouponQuery.getRedType().equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getUseType()) && ObjectUtil.isBlank(operateCouponQuery.getLottoryCode())) {
			return ResultBO.err(MessageCodeConstants.LOTTERY_CODE_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(operateCouponQuery.getPlatform())) {
			return ResultBO.err(MessageCodeConstants.PAY_PLATFORM_IS_NULL_FIELD);
		} else {
			TakenPlatformEnum takenPlatformEnum = PayConstants.TakenPlatformEnum.getByKey(operateCouponQuery.getPlatform());
			if (ObjectUtil.isBlank(takenPlatformEnum)) {
				return ResultBO.err(MessageCodeConstants.PAY_RED_LIMIT_PLATFORM_ERROR_SERVICE);
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证传过来的客户端是否有可用的红包优惠券，
	* @auth: xiongJinGang
	* @param limitPlat 存在与platform相同的平台，则不能用，返回false。
	* @param platform
	* @time: 2017年5月12日 下午8:23:30
	* @return: boolean 
	*/
	public static boolean validateLimitPlatform(String limitPlat, Short platform) {
		if (!ObjectUtil.isBlank(limitPlat)) {
			String[] limitPlats = limitPlat.split(",");
			for (String limit : limitPlats) {
				Short limitPlatShort = Short.parseShort(limit);
				if (limitPlatShort.equals(platform)) {
					return false;
				}
			}
		} else {
			// 没有限制的平台，返回true
			return true;
		}
		return true;
	}

	/**  
	* 方法说明: 验证红包使用渠道
	* @auth: xiongJinGang
	* @param limitChannelId 存在 channelId 的值 ，返回false，否则返回true
	* @param channelId
	* @time: 2017年7月11日 上午10:54:32
	* @return: boolean 
	*/
	public static boolean validateLimitChannel(String limitChannelId, String channelId) {
		if (!ObjectUtil.isBlank(limitChannelId)) {
			if (ObjectUtil.isBlank(channelId)) {
				return true;
			}
			String[] limitChannelIds = limitChannelId.split(",");
			for (String limitId : limitChannelIds) {
				if (limitId.equals(channelId)) {
					return false;
				}
			}
		} else {
			// 没有限制的渠道，返回true
			return true;
		}
		return true;
	}

	/**  
	* 方法说明: 验证采种编号
	* @auth: xiongJinGang
	* @param limitLottery
	* @param curLotteryCode
	* @time: 2017年5月17日 上午11:10:23
	* @return: boolean 
	*/
	public static boolean validateLotteryCode(String limitLottery, Integer curLotteryCode) {
		boolean flag = true;
		if (!ObjectUtil.isBlank(limitLottery)) {
			String currentLotteryCode = String.valueOf(curLotteryCode);// 订单中的彩种编号
			currentLotteryCode = currentLotteryCode.substring(0, 3);
			String[] limitLotterys = limitLottery.split(",");
			for (String lotteryCode : limitLotterys) {
				if (lotteryCode.equals(currentLotteryCode)) {
					flag = false;
					break;
				}
			}
		}
		return flag;
	}

}
