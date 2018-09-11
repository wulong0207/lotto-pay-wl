package com.hhly.paycore.service;

import com.hhly.paycore.po.ThirdTransDetailPO;
import com.hhly.skeleton.pay.trans.bo.ThirdTransDetailBO;

/**
 * @desc 第三方商户推荐赛事账户交易明细
 * @author xiongJinGang
 * @date 2018年1月11日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface ThirdTransDetailService {

	/**  
	* 方法说明: 获取用户支付的方案详情
	* @auth: xiongJinGang
	* @param transDetailBO
	* @time: 2018年1月11日 下午5:20:46
	* @return: ThirdTransDetailPO 
	*/
	public ThirdTransDetailBO findUserDetail(ThirdTransDetailBO transDetailBO);

	/**  
	* 方法说明: 添加用户支付方案详情
	* @auth: xiongJinGang
	* @param transDetailBO
	* @time: 2018年1月11日 下午5:21:03
	* @return: int 
	*/
	public int addUserDetail(ThirdTransDetailBO transDetailBO);

	public int addUserDetail(ThirdTransDetailPO transDetailPO);
}
