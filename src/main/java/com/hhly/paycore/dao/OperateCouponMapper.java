package com.hhly.paycore.dao;

import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.skeleton.pay.bo.DicOperateCouponOptionBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @desc 运营管理的优惠券详情Mapper
 * @author xiongJinGang
 * @date 2017年3月22日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface OperateCouponMapper {
	/**  
	* 方法说明: 根据红包code获取用户的相关记录
	* @auth: xiongJinGang
	* @param userId 用户ID
	* @param redCode 红包code
	* @time: 2017年3月22日 上午9:45:45
	* @return: OperateCouponBO 
	*/
	OperateCouponBO getUserCouponeByRedCode(@Param("userId") Integer userId, @Param("redCode") String redCode);

	/**  
	* 方法说明: 根据用户ID获取用户的彩金红包列表
	* @auth: xiongJinGang
	* @param userId 用户Id
	* @time: 2017年3月30日 上午9:50:24
	* @return: List<OperateCouponBO> 
	*/
	List<OperateCouponBO> getUserCouponeList(@Param("userId") Integer userId, @Param("redStatus") String redStatus);

	/**  
	* 方法说明: 获取用户当前平台可以使用的优惠券红包
	* @auth: xiongJinGang
	* @param paramMap
	* @time: 2017年4月6日 下午5:25:44
	* @return: List<OperateCouponBO> 
	*/
	List<OperateCouponBO> getUserCurPlatformCouponeList(OperateCouponVO operateCouponVO);

	/**  
	* 方法说明: 根据红包code获取红包记录
	* @auth: xiongJinGang
	* @param redCode 红包code
	* @time: 2017年3月27日 上午11:25:57
	* @return: OperateCouponBO 
	*/
	OperateCouponBO getCouponeByRedCode(String redCode);

	/**  
	* 方法说明: 更新彩金红包状态等记录
	* @auth: xiongJinGang
	* @time: 2017年3月27日 下午4:26:21
	* @return: int 
	*/
	int updateOperateCouponStatus(OperateCouponPO operateCouponPO);

	/**  
	* 方法说明: 仅更新红包状态和更新时间
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @time: 2018年1月10日 下午2:39:40
	* @return: int 
	*/
	int updateStatus(OperateCouponPO operateCouponPO);

	/**  
	* 方法说明: 添加红包
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @time: 2017年4月11日 下午2:09:47
	* @return: int 
	*/
	int addOperateCoupon(OperateCouponPO operateCouponPO);

	/**
	 * 获取用户红包余额
	 *
	 vo	 * @return
	 */
	Double getUserRedBalance(OperateCouponVO vo);

	List<DicOperateCouponOptionBO> findOperateCouponCountStatusByUserId(OperateCouponVO vo);

	List<DicOperateCouponOptionBO> findOperateCouponCountRedTypeByUserId(OperateCouponVO vo);

	int getUserCouponeCountByUserId(OperateCouponVO vo);

	List<OperateCouponBO> getUserCouponeByUserId(OperateCouponVO vo);

	int insertBatch(List<OperateCouponPO> poList);
}
