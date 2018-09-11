package com.hhly.paycore.common;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.MathUtil;

/**
 * @desc 合买工具类
 * @author xiongJinGang
 * @date 2018年4月28日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class OrderGroupUtil {

	/**
	 * 合买进度大于等于90%，开始进行拆票
	 */
	public static final Double BUY_TOGETHER_PROGRESS = Double.valueOf("90");
	
	/**
	 * 合买进度大于等于百分比0.9，开始进行拆票
	 */
	public static final Double BUY_TOGETHER_PERCENT_PROGRESS = Double.valueOf("0.9");

	/**  
	* 方法说明: 验证钱包金额是否够扣
	* @auth: xiongJinGang
	* @param totalAmount
	* @param needSubAmount
	* @throws Exception
	* @time: 2018年5月4日 下午3:13:04
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateUserWalletBalance(Double totalAmount, Double needSubAmount) throws Exception {
		// 判断总现金余额中的金额是否大于当前的订单金额
		if (MathUtil.compareTo(totalAmount, needSubAmount) < 0) {
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		return ResultBO.ok(needSubAmount);
	}
}
