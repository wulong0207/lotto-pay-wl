package com.hhly.paycore.common;

import java.util.List;

import org.apache.log4j.Logger;

import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.cms.ticketmgr.bo.TicketInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;

/**
 * @desc 派奖工具类
 * @author xiongJinGang
 * @date 2017年9月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SendPrizeUtil {
	private static Logger logger = Logger.getLogger(SendPrizeUtil.class);

	/**  
	* 方法说明: 验证订单
	* @auth: xiongJinGang
	* @param orderInfo
	* @param tickets
	* @throws Exception
	* @time: 2017年9月7日 下午5:18:00
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateOrder(OrderInfoBO orderInfo, List<TicketInfoBO> tickets) throws Exception {
		// 票投注金额
		Double ticketAmountSum = 0.0d;
		// 票税后奖金
		Double ticketAftBonusSum = 0.0d;
		for (TicketInfoBO ticketInfo : tickets) {
			ticketAmountSum = MathUtil.add(ticketAmountSum, ticketInfo.getTicketMoney());
			Double aftBouns = ObjectUtil.isBlank(ticketInfo.getAftBonus()) ? 0d : ticketInfo.getAftBonus();
			ticketAftBonusSum = MathUtil.add(ticketAftBonusSum, aftBouns);
		}
		// 订单投注金额
		Double orderAmount = orderInfo.getOrderAmount();
		Double aftBonus = orderInfo.getAftBonus();
		// 2.核对订单投注金额与票投注金额是否一致
		if (MathUtil.compareTo(ticketAmountSum, orderAmount) != 0) {
			logger.info("核对订单【" + orderInfo.getOrderCode() + "】投注金额与票投注金额不一致，票投注金额【" + ticketAmountSum + "】,订单投注金额【" + orderAmount + "】");
			return ResultBO.err(MessageCodeConstants.ORDER_AND_TICKET_BETTINGAMT_IS_NOT_SAME);
		}

		// 3.核对票中奖金额与订单中奖金额是否一致
		if (MathUtil.compareTo(ticketAftBonusSum, aftBonus) != 0) {
			logger.info("核对订单【" + orderInfo.getOrderCode() + "】票中奖金额与订单中奖金额不一致，票税后奖金【" + ticketAftBonusSum + "】,税后金额【" + aftBonus + "】");
			return ResultBO.err(MessageCodeConstants.ORDER_AND_TICKET_WINNINGAMT_IS_NOT_SAME);
		}
		return ResultBO.ok();
	}

}
