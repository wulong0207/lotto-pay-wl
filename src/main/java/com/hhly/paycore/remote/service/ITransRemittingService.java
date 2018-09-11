package com.hhly.paycore.remote.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.cms.transmgr.bo.TransRemittingBO;

/**
 * @desc 提款汇款服务接口
 * @author xiongjingang
 * @date 2017年3月2日 上午10:44:46
 * @company 益彩网络科技公司
 * @version 1.0
 */
public interface ITransRemittingService {

	/**
     * 提交提款记录
	 * @param vo
     * @return
     */
    ResultBO<?> insert(TransRemittingBO vo);
}
