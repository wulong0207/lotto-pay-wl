package com.hhly.paycore.dao;

import com.hhly.skeleton.cms.ticketmgr.bo.TicketInfoBO;

import java.util.List;

/**
 * @author YiJian
 * @version 1.0
 * @desc
 * @date 2017/6/1.
 * @company 益彩网络科技有限公司
 */
public interface TicketInfoMapper {

    List<TicketInfoBO> getTickets(TicketInfoBO ticketInfoBO);

    int updateTicketInfo(TicketInfoBO ticketInfoBO);
}
