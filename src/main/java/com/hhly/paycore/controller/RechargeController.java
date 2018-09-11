package com.hhly.paycore.controller;

import com.hhly.paycore.remote.service.IRechargeService;
import com.hhly.paycore.remote.service.IUserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.pay.vo.AgentPayVO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/recharge")
public class RechargeController {

	private static final Logger logger = Logger.getLogger(RechargeController.class);

	@Resource
	private IRechargeService rechargeService;

	@Autowired
	private IUserWalletService userWalletService;

	/**  
	* 方法说明: 代理充值
	* @auth: xiongJinGang
	* @param agentPay
	* @time: 2018年4月8日 上午10:37:05
	* @return: ResultBO<?> 
	*/
	@RequestMapping(value = "/agentRecharge", method = RequestMethod.POST)
	public ResultBO<?> agentRecharge(@RequestBody AgentPayVO agentPay) {
		try {
			// 充值类型不为空并且是充值现金
			if (null != agentPay.getRechargeType() && agentPay.getRechargeType().intValue() == 1) {
				return rechargeService.agentRechargeCash(agentPay);
			} else {
				// 充值红包
				return rechargeService.agentRecharge(agentPay);
			}
		} catch (Exception e) {
			logger.error("代理充值异常", e);
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
	}

	@RequestMapping(value = "/redColor", method = RequestMethod.POST)
	public ResultBO<?> addRedColorAmount(@RequestBody List<OperateCouponVO> reds) throws Exception {
		return userWalletService.addRedColorAmount2(reds);
	}

}
