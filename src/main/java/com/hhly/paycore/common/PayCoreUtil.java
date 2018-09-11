package com.hhly.paycore.common;

import java.util.List;

import com.hhly.paycore.po.UserWalletPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.BatchPayOrderVO;
import com.hhly.skeleton.pay.vo.PayChildWalletVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayResultVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;

/**
 * @desc 支付核心类的工具类
 * @author xiongJinGang
 * @date 2017年11月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PayCoreUtil {
	/**  
	* 方法说明: 组装充值交易记录
	* @auth: xiongJinGang
	* @param payParam
	* @param userId
	* @time: 2017年11月9日 下午4:40:20
	* @return: TransRechargeBO 
	*/
	public static TransRechargeBO packageTransRecharge(PayParamVO payParam, Integer userId) {
		TransRechargeBO transRechargeBO = new TransRechargeBO(payParam);
		transRechargeBO.setUserId(userId);// 设置用户Id
		transRechargeBO.setRedAmount(payParam.getUseRedAmount());// 使用红包金额
		transRechargeBO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transRechargeBO.setChannelId(payParam.getChannelId());// 渠道ID
		transRechargeBO.setTransRechargeCode(payParam.getTransCode());
		// 计算充值金额（减去相应的手续费，目前手续费为0）
		transRechargeBO.setRechargeAmount(payParam.getPayAmount());// 充值金额
		transRechargeBO.setArrivalAmount(MathUtil.calCounterFee(payParam.getPayAmount(), 0.0));// 到账金额
		transRechargeBO.setActivityCode(payParam.getActivityCode());
		transRechargeBO.setRedCode(payParam.getRedCode());// 红包编号
		return transRechargeBO;
	}

	/**  
	* 方法说明: 拼装充值参数（用于合买）
	* @auth: xiongJinGang
	* @param channelId
	* @param userId
	* @time: 2018年5月4日 下午3:25:30
	* @return: TransRechargeBO 
	*/
	public static TransRechargeBO packageTransRecharge(String channelId, Integer userId) {
		TransRechargeBO transRechargeBO = new TransRechargeBO();
		transRechargeBO.setUserId(userId);// 设置用户Id
		transRechargeBO.setChannelId(channelId);
		transRechargeBO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		return transRechargeBO;
	}

	/**  
	* 方法说明: 组装购买彩金红包交易参数
	* @auth: xiongJinGang
	* @param transRecharge
	* @param uwp
	* @throws Exception
	* @time: 2017年7月21日 下午4:44:09
	* @return: PayChildWalletVO 
	*/
	public static PayChildWalletVO packageBuyRedTransRecord(TransRechargeBO transRecharge, UserWalletPO uwp) throws Exception {
		PayChildWalletVO pcw = new PayChildWalletVO(transRecharge.getUserId(), 0d, uwp.getUse20Balance(), uwp.getUse80Balance(), uwp.getUseWinBalance(), 0d, uwp.getTotalCashBalance(), uwp.getEffRedBalance());
		Short transType = PayConstants.TransTypeEnum.DEDUCT.getKey();// 其它（购买红包）
		// payChildWallet.setRedCode(redCode);
		// payChildWallet.setOrderCode(orderInfo.getOrderCode());
		pcw.setOperateRemark(Constants.RECHARGE_BUY_RED_REMARK_INFO);
		pcw.setOrderInfo(Constants.RECHARGE_BUY_RED_REMARK_INFO);
		pcw.setRedAmount(0d);
		pcw.setTransType(transType);
		pcw.setTradeCode(transRecharge.getTransRechargeCode());
		pcw.setTotalCashAmount(uwp.getTotalCashBalance());
		pcw.setTradeAmount(transRecharge.getRechargeAmount());// 交易金额
		pcw.setChannelId(transRecharge.getChannelId());
		return pcw;
	}

	/**  
	* 方法说明: 组装支付结果
	* @auth: xiongJinGang
	* @param payParam
	* @param toPayEndTimeVO
	* @param orderBaseList
	* @param transRecharge
	* @param operateCouponBO
	* @return
	* @time: 2017年11月9日 下午6:05:49
	* @return: PayResultVO 
	*/
	public static ResultBO<?> packagePayResult(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO, List<PayOrderBaseInfoVO> orderBaseList, TransRechargeBO transRecharge, OperateCouponBO operateCouponBO) {
		PayResultVO payResultVO = new PayResultVO(operateCouponBO, toPayEndTimeVO, transRecharge);
		BatchPayOrderVO batchPayOrderVO = PayUtil.getOrderInfo(orderBaseList);
		payResultVO.setOrderCode(batchPayOrderVO.getOrderCodes());
		payResultVO.setBuyType(batchPayOrderVO.getBuyTypes());
		payResultVO.setPayAmount(payParam.getUseBalance());
		return ResultBO.ok(payResultVO);
	}

	/**  
	* 方法说明: 合买支付详情
	* @auth: xiongJinGang
	* @param payParam
	* @param toPayEndTimeVO
	* @param orderBaseList
	* @param transRecharge
	* @param operateCouponBO
	* @time: 2018年5月17日 下午4:40:14
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> packageGroupPayResult(PayParamVO payParam, ToPayEndTimeVO toPayEndTimeVO, List<PayOrderBaseInfoVO> orderBaseList, TransRechargeBO transRecharge, OperateCouponBO operateCouponBO) {
		PayResultVO payResultVO = new PayResultVO(operateCouponBO, toPayEndTimeVO, transRecharge);
		payResultVO.setOrderCode(payParam.getOrderCode());
		// payResultVO.setBuyType(payParam.getBuyType());
		payResultVO.setPayAmount(payParam.getBuyAmount());
		return ResultBO.ok(payResultVO);
	}
}
