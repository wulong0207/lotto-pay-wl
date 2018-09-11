package com.hhly.paycore.remote.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.user.bo.UserWalletBO;

import java.util.List;

/**
 * 
 * @desc 【对外暴露hession接口】 用户钱包业务
 * @author xiongjingang
 * @date 2017年3月16日 下午5:02:35
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface IUserWalletService {

	/**  
	* 方法说明: 根据用户token获取用户钱包记录
	* @auth: xiongJinGang
	* @param token
	* @time: 2017年4月11日 下午4:56:34
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> findUserWallet(String token);

	/**  
	* 方法说明: 根据用户ID查找用户钱包信息
	* @auth: xiongjingang
	* @param userId
	* @time: 2017年3月16日 下午5:02:35
	* @return: UserWalletBO 
	*/
	public UserWalletBO findUserWalletByUserId(Integer userId);

	/**  
	* 方法说明: 账户钱包收入和支出公用类
	* @auth: xiongJinGang
	* @param userId 用户Id
	* @param updateAmount 需要增加或者减去的总金额
	* @param operateType 操作类型，参见 PayConstants.MoneyFlowEnum  收入和支出
	* @param redAmount 红包金额（operateType为收入，传的是充值红包：本金+送的金额。支出时，传的是消费的彩金红包金额）
	* @param redOperateType 操作红包的类型，参见 PayConstants.MoneyFlowEnum  收入和支出
	* @throws Exception
	* @time: 2017年5月24日 下午7:31:50
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateUserWalletCommon(Integer userId, Double updateAmount, Short operateType, Double redAmount, Short redOperateType) throws Exception;

	/**  
	* 方法说明: 按比例，将updateAmount的金额分配到用户账户的各个字段上。
	* @auth: xiongJinGang
	* @param userId 用户ID，不能为空
	* @param updateAmount 需要增加或者减去的总金额，不能为空
	* @param operateType 操作类型，参见 PayConstants.MoneyFlowEnum  收入和支出，不能为空
	* @param splitType 金额分配方式，参见：PayConstants.WalletSplitTypeEnum，不能为空
	* @throws Exception
	* @time: 2017年5月26日 下午6:02:59
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateUserWalletBySplit(Integer userId, Double updateAmount, Short operateType, Short splitType) throws Exception;

	/**
	 * 方法说明: 提供给cms后台，【添加彩金红包金额，添加红包交易记录，添加交易流水】
	 * @auth: xiongJinGang
	 * @param list
	 * @param transType 交易类型 PayConstants.TransTypeEnum.
	 * @time: 2017年5月31日 下午4:34:59
	 * @return: ResultBO<?>
	 * @throws Exception
	 */
	ResultBO<?> addRedColorAmountByType(List<UserRedAddParamVo> list, Short transType) throws Exception;

	/**  
	* 方法说明: 作废彩金红包【提供给cms后台】
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年6月29日 下午3:30:54
	* @return: ResultBO<?> 
	*/
	ResultBO<?> subRedColorAmount(List<UserRedAddParamVo> list) throws Exception;

	/**  
	* 方法说明: 修改每个子账户的金额（用户撤单）
	* @auth: xiongJinGang
	* @param userId
	* @param amount80
	* @param amount20
	* @param amountWin
	* @param amountRed
	* @param operateType 操作类型，参见 PayConstants.MoneyFlowEnum  收入和支出，不能为空
	* @throws Exception
	* @time: 2017年7月12日 上午11:56:12
	* @return: ResultBO<?> 
	*/
	ResultBO<?> updateUserWalletCommon(Integer userId, Double amount80, Double amount20, Double amountWin, Double amountRed, Short operateType) throws Exception;

	/**  
	* 方法说明:添加中奖金额，添加交易流水【提供给cms后台】
	* @auth: xiongJinGang
	* @param userRedAddParam
	* @throws Exception
	* @time: 2017年8月11日 下午4:28:49
	* @return: ResultBO<?> 
	*/
	ResultBO<?> addWinAmountForCMS(UserRedAddParamVo userRedAddParam) throws Exception;

	/**
	 * 方法说明: 给账户添加彩金红包金额
	 * @auth: xiongJinGang
	 * @param list
	 * @throws Exception
	 * @time: 2017年11月8日 下午3:18:48
	 * @return: ResultBO<?>
	 */
	ResultBO<?> addRedColorAmount(List<UserRedAddParamVo> list) throws Exception;

	/**
	 * 方法说明: 提供给cms后台，【添加彩金红包金额，添加红包交易记录，添加交易流水】
	 * @auth: xiongJinGang
	 * @param list
	 * @param transType 交易类型 PayConstants.TransTypeEnum.
	 * @time: 2017年5月31日 下午4:34:59
	 * @return: ResultBO<?>
	 * @throws Exception
	 */
	ResultBO<?> addRedColorAmountByType2(List<OperateCouponVO> list, Short transType) throws Exception;

	/**  
	* 方法说明: 给账户添加彩金红包金额
	* @auth: xiongJinGang
	* @param list
	* @time: 2017年11月8日 下午3:18:48
	* @return: ResultBO<?> 
	*/
	ResultBO<?> addRedColorAmount2(List<OperateCouponVO> list) throws Exception;

}