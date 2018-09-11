package com.hhly.paycore.dao;

import java.util.List;

import com.hhly.skeleton.pay.channel.bo.ChannelRechargeBO;
import com.hhly.skeleton.task.order.vo.OrderChannelVO;
import org.apache.ibatis.annotations.Param;

import com.hhly.paycore.po.TransUserPO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransUserVO;

/**
 * @desc 用户交易信息Mapper
 * @author xiongjingang
 * @date 2017年3月1日 下午4:54:10
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface TransUserMapper {

	/**  
	* 方法说明: 分页查找用户交易信息
	* @param transParamVO
	* @time: 2017年3月6日 上午11:35:16
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> findUserTransListByPage(TransParamVO transParamVO);

	/**  
	* 方法说明: 每次获取固定数量的交易流水
	* @auth: xiongJinGang
	* @param transParamVO
	* @time: 2017年11月9日 上午11:47:30
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> findListByPage(TransParamVO transParamVO);

	/**  
	* 方法说明: 根据条件获取用户交易数量
	* @param transParamVO
	* @time: 2017年3月6日 上午11:35:34
	* @return: int 
	*/
	int findUserTransListCount(TransParamVO transParamVO);

	/**  
	* 方法说明: 获取用户的交易详情
	* @param userId
	* @param transCode
	* @time: 2017年3月7日 下午2:43:06
	* @return: TransUserBO 
	*/
	TransUserBO findTransUserByCode(@Param("userId") Integer userId, @Param("transCode") String transCode);

	/**  
	* 方法说明: 添加用户交易记录
	* @param transUserPO
	* @time: 2017年3月8日 下午3:23:28
	* @return: int 
	*/
	int addUserTrans(TransUserPO transUserPO);

	/**  
	* 方法说明: 批量添加购彩交易记录
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年5月11日 下午6:46:56
	* @return: int 
	*/
	int addUserTransBatch(List<TransUserPO> list);

	/** 
	* @Title: findTransUserByOrderCode 
	* @Description: 根据订单编号获取用户交易记录
	* @param userId
	* @param orderCode
	* @return TransUserBO
	* @time 2017年3月22日
	*/
	TransUserBO findTransUserByOrderCode(@Param("orderCode") String orderCode);

	/**  
	* 方法说明: 根据交易号及交易状态查询交易记录
	* @auth: xiongJinGang
	* @param tradeNo
	* @param tradeStatus
	* @time: 2017年8月7日 下午3:22:29
	* @return: TransUserBO 
	*/
	TransUserBO getTransUserBy(@Param("tradeCode") String tradeCode, @Param("transStatus") Short transStatus);

	TransUserBO getTransUserByType(@Param("tradeCode") String tradeCode, @Param("transStatus") Short transStatus, @Param("transType") Short transType);

	/** 
	* @Title: updateTransUserInfo 
	* @Description: 修改用户支付信息
	*  @param transUserPO
	*  @return
	* @time 下午2:38:22
	*/
	int updateTransUserInfo(TransUserPO transUserPO);

	/**  
	* 方法说明: 根据订单号[交易类型]获取用户的交易记录
	* @auth: xiongJinGang
	* @param transUserVO
	* @time: 2017年4月5日 下午4:50:45
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> getUserTransRecordByOrderCode(TransUserVO transUserVO);

	/**  
	* 方法说明: 根据订单号，交易类型，交易状态获取合买交易记录
	* @auth: xiongJinGang
	* @param list
	* @param transType
	* @param transStatus
	* @time: 2018年5月3日 下午5:32:53
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> getOrderGroupTransRecord(@Param("list") List<String> list, @Param("transType") Short transType, @Param("transStatus") Short transStatus);

	/**  
	* 方法说明: 批量更新交易流水状态（更新提款的状态）
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年8月7日 上午10:55:46
	* @return: int 
	*/
	int updateTransUserByBatch(@Param("list") List<TransUserPO> list);

	/**  
	* 方法说明: 根据交易编号获取交易记录
	* @auth: xiongJinGang
	* @param tradeCode
	* @time: 2018年3月7日 下午4:50:28
	* @return: TransUserBO 
	*/
	TransUserBO findTransUserByTradeCode(@Param("tradeCode") String tradeCode);

	/**  
	* 方法说明: 自动审核提款需要的交易流水
	* @auth: xiongJinGang
	* @param maxId 最大记录ID
	* @param limit 限制条数
	* @param userId 用户ID
	* @time: 2018年3月7日 下午4:57:29
	* @return: List<TransUserBO> 
	*/
	List<TransUserBO> findTransUserListForAutoCheck(@Param("maxId") Integer maxId, @Param("limit") Integer limit, @Param("userId") Integer userId);

	/**
	 * 查询渠道充值列表
	 * @author zhouyang
	 * @param vo
	 * @date 2018.6.8
	 * @return
	 */
	List<ChannelRechargeBO> queryChannelRechargeList(OrderChannelVO vo);

	int updateAwardFlagById(@Param("awardFlag") Short awardFlag, @Param("id") Integer id);
}
