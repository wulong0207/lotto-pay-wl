package com.hhly.paycore.service;

import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;

public interface OperateCouponService {

	/**  
	* 方法说明: 根据彩金红包编号查找红包详情
	* @auth: xiongJinGang
	* @param redCode
	* @time: 2017年3月27日 上午11:32:55
	* @return: OperateCouponBO 
	*/
	ResultBO<?> findCouponByRedCode(String redCode);

	/**  
	* 方法说明: 处理彩金红包信息（更新彩金红包状态及彩金）
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @time: 2017年3月27日 下午4:24:43
	* @return: int 
	* @throws Exception 
	*/
	int dealOperateCoupon(OperateCouponPO operateCouponPO) throws Exception;

	/**  
	* 方法说明: 添加彩金红包记录(充值送的彩金红包不限时间，不限彩种，不限平台等)
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @param addRedAmount 实际送的彩金红包金额
	* @time: 2017年4月11日 下午12:06:47
	* @return: OperateCouponPO 
	* @throws Exception 
	*/
	OperateCouponPO addRedColor(OperateCouponBO operateCouponBO, Double addRedAmount) throws Exception;

	/**  
	* 方法说明: 更新红包记录
	* @auth: xiongJinGang
	* @param transRechargeBO
	* @param operateInfo
	* @throws Exception
	* @time: 2017年5月13日 下午3:07:45
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateRedInfo(TransRechargeBO transRechargeBO, Short transType, String operateInfo) throws Exception;

	/**  
	* 方法说明: 生成彩金红包
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @throws Exception
	* @time: 2017年7月12日 下午2:57:10
	* @return: OperateCouponPO 
	*/
	OperateCouponPO addRedColor(OperateCouponBO operateCouponBO) throws Exception;

	/**  
	* 方法说明: 添加优惠券
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @throws Exception
	* @time: 2017年7月25日 下午7:48:18
	* @return: OperateCouponPO 
	*/
	OperateCouponPO addCoupon(OperateCouponPO operateCouponPO) throws Exception;

	/**
	 * 代理系统充值生成彩金红包
	 * @param operateCouponBO
	 * @param redAmount
	 * @return
	 * @throws Exception
	 * @author YiJian
	 * @date 2017年7月27日 上午10:21:46
	 */
	OperateCouponPO addAgentRedColor(OperateCouponBO operateCouponBO, Double redAmount) throws Exception;

	/**  
	* 方法说明: 添加优惠券
	* @auth: xiongJinGang
	* @param cmsRechargeVO
	* @param transRechargeBO
	* @throws Exception
	* @time: 2017年10月27日 上午11:14:39
	* @return: OperateCouponPO 
	*/
	OperateCouponPO addOperateCoupon(CmsRechargeVO cmsRechargeVO, TransRechargeBO transRechargeBO) throws Exception;

	/**  
	* 方法说明: 查找用户及红包编号查找红包信息
	* @auth: xiongJinGang
	* @param userId
	* @param redCode
	* @time: 2017年12月9日 下午3:55:50
	* @return: OperateCouponBO 
	*/
	OperateCouponBO findUserCouponByRedCode(Integer userId, String redCode);

	/**  
	* 方法说明: 根据红包编号获取红包信息
	* @auth: xiongJinGang
	* @param redCode
	* @time: 2018年1月10日 上午10:05:24
	* @return: OperateCouponBO 
	*/
	OperateCouponBO findByRedCode(String redCode);

	/**  
	* 方法说明: 更新优惠券状态及时间
	* @auth: xiongJinGang
	* @param operateCouponBO
	* @throws Exception
	* @time: 2018年1月10日 下午2:44:16
	* @return: int 
	*/
	int updateOperateCoupon(OperateCouponBO operateCouponBO) throws Exception;

	/**  
	* 方法说明: 根据红包编号更新红包状态及红包余额
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @throws Exception
	* @time: 2018年1月12日 上午10:17:21
	* @return: int 
	*/
	int updateOperateCoupon(OperateCouponPO operateCouponPO) throws Exception;

	/**  
	* 方法说明: 给用户生成彩金红包
	* @auth: xiongJinGang
	* @param activityCode 活动编号 可空
	* @param userId 用户ID 必传
	* @param amount 交易金额 必传
	* @time: 2018年3月6日 上午11:07:30
	* @return: OperateCouponPO 
	*/
	OperateCouponPO addRechargeToRed(String activityCode, Integer userId, Double amount);
}
