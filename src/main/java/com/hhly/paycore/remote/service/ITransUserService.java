package com.hhly.paycore.remote.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.pay.vo.TransUserVO;

/**
 * @author xiongjingang
 * @version 1.0
 * @desc 【对外暴露hession接口】 用户交易接口
 * @date 2017年3月3日 上午10:32:26
 * @company 益彩网络科技公司
 */
public interface ITransUserService {
	/**
	 * 方法说明: 根据订单号[交易类型]获取用户的交易记录
	 *
	 * @param transUser
	 * @throws Exception
	 * @auth: xiongJinGang
	 * @time: 2017年4月5日 下午5:08:53
	 * @return: ResultBO<?>
	 */
	ResultBO<?> findUserTransRecordByOrderCode(TransUserVO transUser) throws Exception;
}
