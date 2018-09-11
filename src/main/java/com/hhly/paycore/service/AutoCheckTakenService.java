/**    
* @Title: AutoCheckTakenService.java  
* @Package com.hhly.paycore.service  
* @Description: TODO
* @author xiongJinGang 
* @date 2018年3月7日 上午10:17:34  
* @version V1.0    
*/
package com.hhly.paycore.service;

import com.hhly.skeleton.base.bo.ResultBO;

/**
 * @desc 自动审核提款
 * @author xiongJinGang
 * @date 2018年3月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface AutoCheckTakenService {

	/**  
	* 方法说明: 通过任务调度，自动审核提款
	* @auth: xiongJinGang
	* @throws Exception
	* @time: 2018年3月7日 上午10:18:16
	* @return: ResultBO<?> 
	*/
	public ResultBO<?> autoCheckForQuartz() throws Exception;
}
