package com.hhly.paycore.remote.service;

import java.util.List;
import java.util.Map;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.vo.OperateCouponQueryVO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;

public interface IOperateCouponService {
	/**  
	* 方法说明: 查找用户的彩金优惠券
	* @auth: xiongJinGang
	* @param token
	* @param redCode
	* @time: 2017年3月22日 上午9:53:58
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findUserCouponByRedCode(String token, String redCode);

	/**  
	* 方法说明: 查找用户的彩金红包列表
	* @auth: xiongJinGang
	* @param token
	* @time: 2017年3月29日 下午7:14:11
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findUserCouponList(String token);

	/**  
	* 方法说明: 查找当前平台，当前彩种，该用户可以使用的红包
	* @auth: xiongJinGang
	* @param operateCouponQuery
	* @time: 2017年4月6日 下午4:58:25
	* @return: ResultBO<?> 
	*/
	ResultBO<?> findCurPlatformCoupon(OperateCouponQueryVO operateCouponQuery);

	/**
	 * 获取用户红包状态数量，红包类别数量，红包余额
	 *
	 * @param token
	 * @return
	 */
	ResultBO<Map<String, Object>> findOperateCouponCount(String token, final OperateCouponVO vo);

	/**
	 * 获取用户红包列表
	 *
	 * @param token
	 * @return
	 */
	ResultBO<List<OperateCouponBO>> getUserCoupone(final OperateCouponVO vo, final String token);
}
