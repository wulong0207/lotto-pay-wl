package com.hhly.paycore.controller;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hhly.paycore.service.SendPrizeService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;

/**
 * @author YiJian
 * @date 2017年6月21日 下午5:50:10
 * @company 深圳益彩网络科技有限公司
 * @version v1.0
 */
@RestController
@RequestMapping("/trans/sendPrize")
public class SendPrizeController {

	private static final Logger logger = Logger.getLogger(SendPrizeController.class);

	@Resource
	private SendPrizeService sendPrizeService;

	/**  
	* 派奖
	* @param transParam
	* @time: 2017年3月7日 下午4:18:28
	* @return: Object 
	*/
	@RequestMapping(value = "/doSendPrize", method = { RequestMethod.POST, RequestMethod.GET })
	public ResultBO<?> doSendPrize(String orderCode) {
		try {
			return sendPrizeService.updateSendPrize(orderCode);
		} catch (Exception e) {
			logger.error("派奖异常，订单号：" + orderCode, e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	/**  
	* 方法说明: 重置派奖
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年9月8日 下午3:21:04
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/resetSendPrize", method = { RequestMethod.POST, RequestMethod.GET })
	public ResultBO<?> resetSendPrize(String orderCode) {
		try {
			return sendPrizeService.updateResetSendPrize(orderCode);
		} catch (Exception e) {
			logger.error("重置派奖异常，订单号：" + orderCode, e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}
}
