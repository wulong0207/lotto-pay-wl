package com.hhly.paycore.service;

import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransRedPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.TransRedVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;

/**
 * @desc 红包交易记录service
 * @author xiongJinGang
 * @date 2017年3月24日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransRedService {

	/**  
	* 方法说明: 添加红包交易记录
	* @auth: xiongJinGang
	* @param record
	* @time: 2017年3月24日 上午11:42:20
	* @return: int 
	* @throws Exception 
	*/
	int addTransRed(TransRedPO record) throws Exception;

	/**  
	* 方法说明: 添加彩金红包的生成记录
	* @auth: xiongJinGang
	* @param userRedAddParamVo
	* @throws Exception
	* @time: 2017年6月15日 下午7:46:40
	* @return: void 
	*/
	void addTransRed(UserRedAddParamVo userRedAddParamVo) throws Exception;

	/**  
	* 方法说明: 添加红包交易记录
	* @auth: xiongJinGang
	* @param operateCoupon 红包对象
	* @param status 状态  PayConstants.UserTransStatusEnum
	* @param transType 交易类型 TransTypeEnum.RECHARGE
	* @param transAmount 交易金额
	* @param aftTransAmount 交易后红包金额
	* @param orderInfo 说明
	* @param orderCode 订单号
	* @time: 2017年7月6日 下午5:50:27
	* @return: void 
	*/
	void addTransRed(OperateCouponBO operateCoupon, Short status, Short transType, Double transAmount, Double aftTransAmount, String orderInfo, String orderCode);

	/**  
	* 方法说明: 根据红包code获取用户红包交易记录 
	* @auth: xiongJinGang
	* @param token 用户登录token
	* @param redCode 红包code
	* @time: 2017年3月24日 上午11:42:32
	* @return: ResultBO<?
	*/
	ResultBO<?> findUserTransRedByCode(String token, Integer redCode);

	/** 
	* @Title: findUserTransRedByPage 
	* @Description: 分页查询用户红包交易
	*  @param vo
	*  @return
	 * @throws Exception 
	* @time 2017年5月6日 下午5:14:31
	*/
	ResultBO<?> findUserTransRedByPage(TransRedVO vo);

	/**  
	* 方法说明: 撤单生成红包
	* @auth: xiongJinGang
	* @param redCode 红包编号
	* @param redAmount 红包金额
	* @param userId
	* @param orderCode 订单号
	* @param redType 红包类型
	* @throws Exception
	* @time: 2017年7月12日 下午3:16:47
	* @return: int 
	*/
	int addTransRed(String redCode, Double redAmount, Integer userId, String orderCode, Short redType) throws Exception;

	/**  
	* 方法说明: 添加红包记录
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @param transRechargeBO
	* @throws Exception
	* @time: 2017年11月9日 下午5:33:32
	* @return: void 
	*/
	void addRedTransRecord(OperateCouponPO operateCouponPO, TransRechargeBO transRechargeBO) throws Exception;
}
