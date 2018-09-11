package com.hhly.paycore.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.remoting.caucho.HessianServiceExporter;

import com.hhly.paycore.remote.service.IActivityPayService;
import com.hhly.paycore.remote.service.IAgentService;
import com.hhly.paycore.remote.service.IOperateCouponService;
import com.hhly.paycore.remote.service.IPayBankLimitService;
import com.hhly.paycore.remote.service.IPayChannelService;
import com.hhly.paycore.remote.service.IPayService;
import com.hhly.paycore.remote.service.IRechargeService;
import com.hhly.paycore.remote.service.ITransRechargeService;
import com.hhly.paycore.remote.service.ITransRedService;
import com.hhly.paycore.remote.service.ITransTakenConfirmService;
import com.hhly.paycore.remote.service.ITransUserLogService;
import com.hhly.paycore.remote.service.ITransUserService;
import com.hhly.paycore.remote.service.IUserWalletService;

/**
 * 暴露hession接口
 * @author wul687
 * 2018-07-26
 */
@Configuration
public class HessianExportConfig {
	@Autowired
	private ITransTakenConfirmService iTransTakenConfirmService;
	@Autowired
	private ITransRechargeService iTransRechargeService;
	@Autowired
	private ITransUserService iTransUserService;

	@Autowired
	private ITransUserLogService iTransUserLogService;
	@Autowired
	private IPayService iPayService;
	@Autowired
	private IRechargeService iRechargeService;
	@Autowired
	private IOperateCouponService iOperateCouponService;
	@Autowired
	private ITransRedService iTransRedService;
	@Autowired
	private IPayChannelService iPayChannelService;
	@Autowired
	private IUserWalletService iUserWalletService;
	@Autowired
	private IPayBankLimitService iPayBankLimitService;
	@Autowired
	private IActivityPayService iActivityPayService;
	@Autowired
	private IAgentService iAgentService;
	
	@Bean(name = "/remote/transTakenConfirmService")
	public HessianServiceExporter transTakenConfirm() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iTransTakenConfirmService);
		exporter.setServiceInterface(ITransTakenConfirmService.class);
		return exporter;
	}

	@Bean(name = "/remote/transRechargeService")
	public HessianServiceExporter transRechargeService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iTransRechargeService);
		exporter.setServiceInterface(ITransRechargeService.class);
		return exporter;
	}

	@Bean(name = "/remote/transUserService")
	public HessianServiceExporter transUserService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iTransUserService);
		exporter.setServiceInterface(ITransUserService.class);
		return exporter;
	}

	@Bean(name = "/remote/transUserLogService")
	public HessianServiceExporter transUserLogService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iTransUserLogService);
		exporter.setServiceInterface(ITransUserLogService.class);
		return exporter;
	}

	@Bean(name = "/remote/payService")
	public HessianServiceExporter payService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iPayService);
		exporter.setServiceInterface(IPayService.class);
		return exporter;
	}

	@Bean(name = "/remote/rechargeService")
	public HessianServiceExporter rechargeService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iRechargeService);
		exporter.setServiceInterface(IRechargeService.class);
		return exporter;
	}

	@Bean(name = "/remote/operateCouponService")
	public HessianServiceExporter operateCouponService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iOperateCouponService);
		exporter.setServiceInterface(IOperateCouponService.class);
		return exporter;
	}

	@Bean(name = "/remote/transRedService")
	public HessianServiceExporter transRedService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iTransRedService);
		exporter.setServiceInterface(ITransRedService.class);
		return exporter;
	}

	@Bean(name = "/remote/payChannelService")
	public HessianServiceExporter payChannelService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iPayChannelService);
		exporter.setServiceInterface(IPayChannelService.class);
		return exporter;
	}

	@Bean(name = "/remote/userWalletService")
	public HessianServiceExporter userWalletService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iUserWalletService);
		exporter.setServiceInterface(IUserWalletService.class);
		return exporter;
	}

	@Bean(name = "/remote/payBankLimitService")
	public HessianServiceExporter payBankLimitService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iPayBankLimitService);
		exporter.setServiceInterface(IPayBankLimitService.class);
		return exporter;
	}

	@Bean(name = "/remote/activityPayService")
	public HessianServiceExporter activityPayService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iActivityPayService);
		exporter.setServiceInterface(IActivityPayService.class);
		return exporter;
	}

	@Bean(name = "/remote/agentService")
	public HessianServiceExporter agentService() {
		HessianServiceExporter exporter = new HessianServiceExporter();
		exporter.setService(iAgentService);
		exporter.setServiceInterface(IAgentService.class);
		return exporter;
	}
}
