package com.hhly.paycore.service;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;

/**
 * @author YiJian
 * @version 1.0
 * @desc
 * @date 2017/5/24.
 * @company 益彩网络科技有限公司
 */
public interface CancellationRefundService {
    /**
     * 撤单统一入口
     * @param CancellationRefundBO
     * @return
     */
    public ResultBO<?> doCancellation(OrderCancelMsgModel orderCancelMsgModel) throws Exception;
}
