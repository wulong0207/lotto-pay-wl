package com.hhly.paycore.controller;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.hhly.paycore.service.AutoCheckTakenService;
import com.hhly.paycore.service.TaskService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;

@RestController
@RequestMapping("/pay")
public class PayController {

	private static final Logger logger = Logger.getLogger(PayController.class);

	@Resource
	private TaskService taskService;
	@Resource
	private AutoCheckTakenService autoCheckTakenService;

	/**  
	* 方法说明: 定时关闭充值状态
	* @auth: xiongJinGang
	* @time: 2018年3月8日 上午10:26:36
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/closeRechargeStatus", method = { RequestMethod.POST, RequestMethod.GET })
	public ResultBO<?> closeRechargeStatus() {
		try {
			return taskService.closeRechargeStatus();
		} catch (Exception e) {
			logger.error("定时关闭充值记录状态异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	/**  
	* 方法说明: 定时审核提款记录
	* @auth: xiongJinGang
	* @time: 2018年3月8日 上午10:26:53
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/autoCheckTaken", method = { RequestMethod.POST, RequestMethod.GET })
	public ResultBO<?> autoCheckTaken() {
		try {
			return autoCheckTakenService.autoCheckForQuartz();
		} catch (Exception e) {
			logger.error("定时审核提款记录异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}
}
