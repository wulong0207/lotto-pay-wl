package com.hhly.paycore.remote.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.TakenUtil;
import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.AgentTransTakenMapper;
import com.hhly.paycore.po.AgentTransLogPO;
import com.hhly.paycore.po.AgentTransTakenPO;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.remote.service.IAgentService;
import com.hhly.paycore.service.AgentCoreService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.OperateCouponService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.AgentConstants;
import com.hhly.skeleton.base.constants.AgentConstants.AgentTransTypeEnum;
import com.hhly.skeleton.base.constants.AgentConstants.AgentWalletTransTypeEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenOperateTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.UserTransStatusEnum;
import com.hhly.skeleton.base.constants.TransContans;
import com.hhly.skeleton.base.constants.UserConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.pay.agent.bo.AgentInfoBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransLogBO;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.agent.vo.AgentTransLogParamVO;
import com.hhly.skeleton.pay.agent.vo.ConfirmTakenResultVO;
import com.hhly.skeleton.pay.agent.vo.TransferAccountsVO;
import com.hhly.skeleton.pay.agent.vo.WithdrawApplyVO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PageBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.vo.AppTransUserVO;
import com.hhly.skeleton.pay.vo.TakenFlowVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

@Service("iAgentService")
public class AgentServiceImpl implements IAgentService {
	private static final Logger logger = LoggerFactory.getLogger(AgentServiceImpl.class);

	@Resource
	private AgentCoreService agentCoreService;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private OperateCouponService operateCouponService;
	@Resource
	private TransRedService transRedService;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private AgentTransTakenMapper agentTransTakenMapper;
	@Resource
	private TransRechargeService transRechargeService;

	@Override
	public ResultBO<?> applyWithdraw(WithdrawApplyVO withdrawApply) {
		UserInfoBO userInfo = userUtil.getUserByToken(withdrawApply.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		logger.info("用户【" + userInfo.getId() + "】申请提现参数：" + withdrawApply.toString());
		// 验证提现参数
		ResultBO<?> resultBO = checkParam(withdrawApply, userInfo, "1");
		if (resultBO.isError()) {
			logger.error("用户【" + userInfo.getId() + "】申请提现验证不过：" + resultBO.getMessage());
			return resultBO;
		}
		AgentInfoBO agentInfo = (AgentInfoBO) resultBO.getData();

		// 验证当月是否已经提过现
		resultBO = checkTakenCount(userInfo, agentInfo);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 计算税费
		Double taxAmount = countTaxAmount(withdrawApply.getAmount());
		if (MathUtil.compareTo(taxAmount, 0d) < 0) {
			return ResultBO.err(MessageCodeConstants.AGENT_TAX_IS_ERROR);
		}

		PayBankcardBO bankCard = bankcardService.findUserBankById(userInfo.getId(), withdrawApply.getBankCardId());
		if (ObjectUtil.isBlank(bankCard)) {
			logger.info("未获取到用户【" + userInfo.getId() + "】银行卡信息");
			return ResultBO.err(MessageCodeConstants.BANKCARD_IS_VALIDATION_SERVICE);
		}
		// 不是储蓄卡，不让提现
		if (!PayConstants.BankCardTypeEnum.BANK_CARD.getKey().equals(bankCard.getBanktype())) {
			logger.error("用户【" + userInfo.getId() + "】申请提现的银行卡类型错误");
			return ResultBO.err(MessageCodeConstants.AGENT_TAKEN_BANK_CARD_IS_ERROR);
		}

		ConfirmTakenResultVO confirmTakenResult = createConfirmResult(withdrawApply, userInfo, bankCard, agentInfo.getAgentWallet(), taxAmount);
		redisUtil.addObj(makeTakenKey(withdrawApply.getToken(), withdrawApply.getAmount()), confirmTakenResult, CacheConstants.FIVE_MINUTES);
		return ResultBO.ok(confirmTakenResult);
	}

	/**  
	* 方法说明: 验证用户当月提现次数
	* @auth: xiongJinGang
	* @param userInfo
	* @param agentInfo
	* @time: 2018年3月10日 下午4:21:16
	* @return: void 
	*/
	private ResultBO<?> checkTakenCount(UserInfoBO userInfo, AgentInfoBO agentInfo) {
		AgentTransTakenBO agentTransTakenBO = agentCoreService.findAgentLastTakenDetail(agentInfo.getId());
		if (!ObjectUtil.isBlank(agentTransTakenBO)) {
			// 当前月份
			String nowMonth = DateUtil.getNow(DateUtil.FORMAT_YYYYMM);
			String takenMonth = DateUtil.convertDateToStr(agentTransTakenBO.getCreateTime(), DateUtil.FORMAT_YYYYMM);
			if (nowMonth.equals(takenMonth)) {
				logger.error("用户【" + userInfo.getId() + "】当月已申请提现");
				return ResultBO.err(MessageCodeConstants.AGENT_TAKEN_COUNT_IS_ERROR);
			}
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> updateConfirmWithdraw(WithdrawApplyVO withdrawApply) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(withdrawApply.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		logger.info("用户【" + userInfo.getId() + "】确认提现参数：" + withdrawApply.toString());
		ConfirmTakenResultVO confirmTakenResult = redisUtil.getObj(makeTakenKey(withdrawApply.getToken(), withdrawApply.getAmount()), ConfirmTakenResultVO.class);
		if (ObjectUtil.isBlank(confirmTakenResult)) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_TIME_OUT_ERROR_SERVICE);
		}
		// 验证提现参数
		ResultBO<?> resultBO = checkParam(withdrawApply, userInfo, "2");
		if (resultBO.isError()) {
			logger.error("用户【" + userInfo.getId() + "】申请提现验证不过：" + resultBO.getMessage());
			return resultBO;
		}
		AgentInfoBO agentInfo = (AgentInfoBO) resultBO.getData();
		// 验证当月是否已经提过现
		resultBO = checkTakenCount(userInfo, agentInfo);
		if (resultBO.isError()) {
			return resultBO;
		}

		confirmTakenResult.setAgentId(agentInfo.getId());
		confirmTakenResult.setAgentCode(agentInfo.getAgentCode());

		resultBO = agentCoreService.updateAgentWallet(agentInfo.getId(), withdrawApply.getAmount(), AgentWalletTransTypeEnum.OUT);
		if (resultBO.isError()) {
			logger.error("更新代理【" + agentInfo.getId() + "】钱包失败：" + resultBO.getMessage());
			return resultBO;
		}

		// 添加提款记录
		PayBankcardBO bankCard = bankcardService.findUserBankById(userInfo.getId(), withdrawApply.getBankCardId());
		if (ObjectUtil.isBlank(bankCard)) {
			return ResultBO.err(MessageCodeConstants.BANKCARD_IS_VALIDATION_SERVICE);
		}
		String transTakenCode = OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT);
		confirmTakenResult.setTakenCode(transTakenCode);
		agentCoreService.addAgentTaken(withdrawApply, confirmTakenResult, agentInfo, bankCard);

		// 添加提款流水
		AgentTransLogPO agentTransLogPO = agentCoreService.addTransLog(confirmTakenResult);
		redisUtil.delObj(makeTakenKey(withdrawApply.getToken(), withdrawApply.getAmount()));
		return ResultBO.ok(agentTransLogPO.getTotalCashBalance());
	}

	@Override
	public ResultBO<?> updateTransAccount(TransferAccountsVO transferAccounts) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transferAccounts.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		// 验证请求参数
		ResultBO<?> resultBO = checkTransAccountParam(transferAccounts, userInfo);
		if (resultBO.isError()) {
			logger.error("用户【" + userInfo.getId() + "】申请提现验证不过：" + resultBO.getMessage());
			return resultBO;
		}
		// 会员信息
		UserInfoBO memberInfo = (UserInfoBO) resultBO.getData();
		// 自己不能给自己转账
		/*if (userInfo.getId().equals(memberInfo.getId())) {
			logger.info("会员账户【" + userInfo.getAccount() + "】不能给自己账户转账");
			return ResultBO.err(MessageCodeConstants.AGENT_TRANS_ACCOUNT_ACCOUNT_ERROR);
		}*/
		// 代理信息
		resultBO = checkAgentParam(transferAccounts, userInfo);
		if (resultBO.isError()) {
			logger.error("用户【" + userInfo.getId() + "】申请提现验证不过：" + resultBO.getMessage());
			return resultBO;
		}
		AgentInfoBO agentInfo = (AgentInfoBO) resultBO.getData();
		/**********代理的相关操作*********/
		Double updateAmount = Double.valueOf(transferAccounts.getRechargeAmount());
		// 更新账户中的金额
		resultBO = agentCoreService.updateAgentWallet(agentInfo.getId(), updateAmount, AgentConstants.AgentWalletTransTypeEnum.OUT);
		if (resultBO.isError()) {
			return resultBO;
		}

		AgentInfoBO agentInfoUpdate = (AgentInfoBO) resultBO.getData();
		// 会员的入账交易记录流水
		String memberTransUserCode = OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN);
		// 添加账户扣款记录
		AgentTransLogPO transLogPO = agentCoreService.addTransLog(agentInfoUpdate, updateAmount, transferAccounts.getRechargeAccount(), memberTransUserCode);

		/************会员的相关操作***************/
		logger.debug("开始给会员【" + memberInfo.getAccount() + "】生成彩金红包");
		// 添加代理充值记录
		TransRechargePO transRecharge = createAgentRechargeRecord(agentInfoUpdate, updateAmount, memberInfo.getId(), transLogPO.getTransCode());
		transRechargeService.addRechargeTrans(transRecharge);

		OperateCouponBO operateCouponBO = new OperateCouponBO();
		operateCouponBO.setUserId(transRecharge.getUserId());
		operateCouponBO.setActivityCode(transferAccounts.getActivityCode());
		// operateCouponBO.setChannelId(transferAccounts.getChannelId());//这里不能加渠道ID 代理充值产生的红包，不能写入渠道号，否则对应的渠道登录后看不到红包
		OperateCouponPO operateCouponPO = operateCouponService.addAgentRedColor(operateCouponBO, transRecharge.getRechargeAmount());
		// 添加彩金红包交易流水
		logger.debug("添加红包生成记录");
		operateCouponBO.setRedCode(operateCouponPO.getRedCode());
		operateCouponBO.setRedType(operateCouponPO.getRedType());
		transRedService.addTransRed(operateCouponBO, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), TransTypeEnum.RECHARGE.getKey(), 0d, transRecharge.getRechargeAmount(), Constants.RED_AGENT_RECHARGE, transRecharge.getOrderCode());
		logger.debug("开始更新用户钱包");
		resultBO = userWalletService.updateUserWalletBySplit(transRecharge.getUserId(), transRecharge.getArrivalAmount(), PayConstants.MoneyFlowEnum.IN.getKey(), PayConstants.WalletSplitTypeEnum.RED_COLOR.getKey());
		if (resultBO.isError()) {
			throw new RuntimeException("更新用户【" + transRecharge.getUserId() + "】彩金红包金额失败");
		}

		logger.debug("添加用户交易记录");
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
		TransUserPO transUserPO = transUserService.addTransRecord(transferAccounts, memberInfo, updateAmount, transRecharge.getTransRechargeCode(), operateCouponPO, userWalletPO);
		transUserLogService.addTransLogRecord(transUserPO);
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> findAppTransUserByPage(AgentTransLogParamVO agentTransLogParam) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(agentTransLogParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		// 获取代理用户信息
		AgentInfoBO agentInfo = findAgentInfo(agentTransLogParam.getToken());
		agentTransLogParam.setAgentId(agentInfo.getId());
		// 只查询6个月以内的
		Date nowDate = DateUtil.getNowDate();
		agentTransLogParam.setStartDate(DateUtil.addMonth(nowDate, -6));
		agentTransLogParam.setEndDate(nowDate);
		int count = agentCoreService.findAgentTransListCount(agentTransLogParam);
		if (count > 0) {
			PageBO pageBO = new PageBO(agentTransLogParam.getShowCount(), count, agentTransLogParam.getCurrentPage());
			List<AgentTransLogBO> list = agentCoreService.findAgentTransListByPage(agentTransLogParam);
			pageBO.setDataList(getAppTransUserList(list));
			return ResultBO.ok(pageBO);
		}
		return ResultBO.ok(new ArrayList<>());
	}

	/**  
	* 方法说明: 用户ID
	* @auth: xiongJinGang
	* @param userId
	* @time: 2018年3月7日 下午2:39:27
	* @return: AgentInfoBO 
	*/
	@Override
	public AgentInfoBO findAgentInfo(String token) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		return agentCoreService.findAgentInfo(userInfo.getId());
	}

	/**
	 * 方法说明: 交易记录按月份分组
	 * 
	 * @auth: xiongJinGang
	 * @param list
	 * @time: 2017年4月26日 下午4:11:00
	 * @return: List<AppTransUserVO>
	 */
	private List<AppTransUserVO> getAppTransUserList(List<AgentTransLogBO> list) {
		List<AppTransUserVO> appList = new ArrayList<AppTransUserVO>();
		if (!ObjectUtil.isBlank(list)) {
			String nowMonth = DateUtil.getNow(DateUtil.FORMAT_CHINESE_YYYYMM);// 当月
			String today = DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE);// 当天
			String yesteday = DateUtil.getBeforeOrAfterDate(-1, DateUtil.DATE_FORMAT_NO_LINE);// 明天
			String beforeYesteday = DateUtil.getBeforeOrAfterDate(-2, DateUtil.DATE_FORMAT_NO_LINE);// 后天

			Map<String, List<AgentTransLogBO>> map = new LinkedHashMap<String, List<AgentTransLogBO>>();
			for (AgentTransLogBO transUserBO : list) {
				// 添加符号
				AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.getTransTypeByKey(transUserBO.getTransType());
				String symbol = agentTransTypeEnum.getMoneyFlow().equals(AgentConstants.MoneyFlowEnum.IN.getKey()) ? "+" : "-";
				transUserBO.setShowAmount(symbol + String.format("%.2f", transUserBO.getTransAmount()));// 保留小数点后2位

				// 交易状态
				UserTransStatusEnum transStatusEnum = PayConstants.UserTransStatusEnum.getEnum(transUserBO.getTransStatus());
				if (null != transStatusEnum) {
					transUserBO.setTransStatusName(transStatusEnum.getValue());
				}

				String yearMonth = DateUtil.convertDateToStr(transUserBO.getCreateTime(), DateUtil.FORMAT_CHINESE_YYYYMM);// 年月
				String yearMonthDate = DateUtil.convertDateToStr(transUserBO.getCreateTime(), DateUtil.DATE_FORMAT_NO_LINE);// 年月日
				if (yearMonthDate.equals(today)) {
					transUserBO.setCreateTimeStr(Constants.APP_TRANS_USER_LIST_TODAY);
				} else if (yearMonthDate.equals(yesteday)) {
					transUserBO.setCreateTimeStr(Constants.APP_TRANS_USER_LIST_YESTEDAY);
				} else if (yearMonthDate.equals(beforeYesteday)) {
					transUserBO.setCreateTimeStr(Constants.APP_TRANS_USER_LIST_BEFORE_YESTEDAY);
				} else {
					transUserBO.setCreateTimeStr(DateUtil.convertDateToStr(transUserBO.getCreateTime(), DateUtil.FORMAT_CHINESE_DAY));
				}
				if (map.containsKey(yearMonth)) {
					List<AgentTransLogBO> transList = map.get(yearMonth);
					transList.add(transUserBO);
					map.put(yearMonth, transList);
				} else {
					List<AgentTransLogBO> appTransList = new ArrayList<AgentTransLogBO>();
					appTransList.add(transUserBO);
					map.put(yearMonth, appTransList);
				}
			}
			if (map.size() > 0) {
				AppTransUserVO appTransUserVO = null;

				for (Map.Entry<String, List<AgentTransLogBO>> transMap : map.entrySet()) {
					appTransUserVO = new AppTransUserVO();
					String yearMonth = transMap.getKey();
					if (yearMonth.equals(nowMonth)) {
						appTransUserVO.setMonth(Constants.APP_TRANS_USER_LIST_TITLE);
						appTransUserVO.setList(transMap.getValue());
					} else {
						appTransUserVO.setMonth(yearMonth);
						appTransUserVO.setList(transMap.getValue());
					}
					appList.add(appTransUserVO);
				}
			}
		}
		return appList;
	}

	@Override
	public ResultBO<?> findTransDetail(AgentTransLogParamVO agentTransLogParam) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(agentTransLogParam.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		AgentInfoBO agentInfo = findAgentInfo(agentTransLogParam.getToken());

		AgentTransLogBO agentTransLogBO = agentCoreService.findAgentTransLogDetail(agentInfo.getId(), agentTransLogParam.getTransCode());
		if (ObjectUtil.isBlank(agentTransLogBO)) {
			logger.error("未获取到代理【" + agentInfo.getId() + "】交易编号为" + agentTransLogParam.getTransCode() + "的数据");
			return ResultBO.err(MessageCodeConstants.DATA_NOT_EXIST);
		}
		// 如果是提现到银行，则获取提款记录；否则获取自身的交易详情
		if (agentTransLogBO.getTransType().equals(AgentConstants.AgentTransTypeEnum.TAKEN_TO_BANK.getKey()) && !ObjectUtil.isBlank(agentTransLogBO.getTradeCode())) {
			return getAgentTransTakenDetail(agentInfo, agentTransLogBO);
		} else {
			return getAgentTransLogDetail(agentTransLogBO);
		}
	}

	/**  
	* 方法说明: 获取代理交易详情
	* @auth: xiongJinGang
	* @param agentTransLogBO
	* @time: 2018年3月10日 下午6:03:09
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> getAgentTransLogDetail(AgentTransLogBO agentTransLogBO) {
		// 添加符号
		AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.getTransTypeByKey(agentTransLogBO.getTransType());
		String symbol = agentTransTypeEnum.getMoneyFlow().equals(PayConstants.MoneyFlowEnum.IN.getKey()) ? "+" : "-";
		agentTransLogBO.setShowAmount(symbol + String.format("%.2f", agentTransLogBO.getTransAmount()));// 保留小数点后2位

		// 交易状态
		UserTransStatusEnum transStatusEnum = PayConstants.UserTransStatusEnum.getEnum(agentTransLogBO.getTransStatus());
		if (null != transStatusEnum) {
			agentTransLogBO.setTransStatusName(transStatusEnum.getValue());
		}
		return ResultBO.ok(agentTransLogBO);
	}

	/**  
	* 方法说明: 获取提款交易详情
	* @auth: xiongJinGang
	* @param agentInfo
	* @param agentTransLogBO
	* @time: 2018年3月10日 下午6:02:15
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> getAgentTransTakenDetail(AgentInfoBO agentInfo, AgentTransLogBO agentTransLogBO) {
		AgentTransTakenBO agentTransTakenBO = agentCoreService.findAgentTakenDetail(agentInfo.getId(), agentTransLogBO.getTradeCode());
		agentTransTakenBO.setShowMoney("-" + agentTransTakenBO.getExtractAmount());
		// 交易状态
		if (!ObjectUtil.isBlank(agentTransTakenBO.getTransStatus())) {
			TakenStatusEnum transStatusEnum = PayConstants.TakenStatusEnum.getEnum(agentTransTakenBO.getTransStatus());
			if (null != transStatusEnum) {
				agentTransTakenBO.setTransStatusName(transStatusEnum.getValue());
			}
		}
		// 银行卡号
		if (!ObjectUtil.isBlank(agentTransTakenBO.getBankCardNum())) {
			agentTransTakenBO.setBankCardNum(StringUtil.hideHeadString(agentTransTakenBO.getBankCardNum()));
		}

		List<TakenFlowVO> flowList = new ArrayList<TakenFlowVO>();
		// 申请提款
		flowList.add(new TakenFlowVO(agentTransTakenBO.getCreateTime(), TransContans.SUPPLY_SUCCESS_REMARK));
		// 审核通过、不通过
		if (!ObjectUtil.isBlank(agentTransTakenBO.getReviewTime())) {
			if (agentTransTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey())) {
				flowList.add(new TakenFlowVO(agentTransTakenBO.getReviewTime(), TransContans.AUDIT_FAIL_REMAR + "，" + agentTransTakenBO.getTransFailInfo()));
			} else {
				flowList.add(new TakenFlowVO(agentTransTakenBO.getReviewTime(), TransContans.AUDIT_SUCCESS_REMARK));

				// 银行处理中
				if (!ObjectUtil.isBlank(agentTransTakenBO.getDealTime())) {
					// 处理成功或者失败
					if (agentTransTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {// 银行处理时间
						flowList.add(new TakenFlowVO(agentTransTakenBO.getDealTime(), TransContans.TAKEN_FAIL_REMARK + "，" + agentTransTakenBO.getTransFailInfo()));
					} else {
						flowList.add(new TakenFlowVO(agentTransTakenBO.getDealTime(), TransContans.BANK_PROCESSING_REMARK));
						// 提款到账时间
						if (!ObjectUtil.isBlank(agentTransTakenBO.getArrivalTime())) {// 到账时间
							flowList.add(new TakenFlowVO(agentTransTakenBO.getArrivalTime(), TransContans.TAKEN_ARRIVAL_REMARK));
						}
					}
				}
			}
		}

		if (!ObjectUtil.isBlank(flowList) && flowList.size() > 1) {
			// 按处理时间排序
			Collections.sort(flowList, new Comparator<TakenFlowVO>() {
				@Override
				public int compare(TakenFlowVO takenFlowV, TakenFlowVO takenFlowV2) {
					// 升序
					return takenFlowV2.getDealTime().compareTo(takenFlowV.getDealTime());
				}
			});
		}
		agentTransTakenBO.setFlowList(flowList);
		return ResultBO.ok(agentTransTakenBO);
	}

	@Override
	public ResultBO<?> updateAgentWalletAmount(Integer agentId, Double updateAmount) throws Exception {
		logger.info("给代理【" + agentId + "】返现：" + updateAmount + "开始");
		if (ObjectUtil.isBlank(agentId)) {
			return ResultBO.err(MessageCodeConstants.AGENT_INFO_NOT_FOUND_ERROR);
		}
		if (ObjectUtil.isBlank(updateAmount)) {
			return ResultBO.err(MessageCodeConstants.AGENT_UPDATE_AMOUNT_IS_NULL);
		}
		AgentTransLogBO agentTransLogBO = agentCoreService.findAgentLastTransDetail(agentId);
		if (!ObjectUtil.isBlank(agentTransLogBO)) {
			String takenTime = DateUtil.convertDateToStr(agentTransLogBO.getCreateTime(), DateUtil.FORMAT_YYYYMM);
			String nowTime = DateUtil.getNow(DateUtil.FORMAT_YYYYMM);
			if (takenTime.equals(nowTime)) {
				logger.info("当月已经给代理" + agentId + "返佣");
				return ResultBO.err(MessageCodeConstants.AGENT_HAD_BACK_AMOUNT);
			}
		}

		ResultBO<?> resultBO = agentCoreService.updateAgentWallet(agentId, updateAmount, AgentConstants.AgentWalletTransTypeEnum.IN);
		if (resultBO.isError()) {
			logger.error("给代理" + agentId + "返佣异常", resultBO.getMessage());
			return resultBO;
		}
		AgentInfoBO agentInfo = (AgentInfoBO) resultBO.getData();
		// 添加代理结算日志
		agentCoreService.addTransLog(agentInfo, updateAmount);
		logger.info("给代理【" + agentId + "】返现成功，当前账户总金额：" + agentInfo.getAgentWallet());
		return ResultBO.ok();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ResultBO<?> updateTakenStatusByBatch(List<AgentTransTakenBO> list, Short operateType) throws Exception {
		TakenOperateTypeEnum takenOperateTypeEnum = PayConstants.TakenOperateTypeEnum.getEnum(operateType);
		logger.info("CMS批量操作代理提款开始，操作数量：" + list.size() + "，操作类型：" + takenOperateTypeEnum.getValue());
		if (ObjectUtil.isBlank(list)) {
			logger.debug("批量审核参数为空");
			return ResultBO.err(MessageCodeConstants.PARAM_IS_NULL_FIELD);
		}
		ResultBO<?> resultBO = TakenUtil.takenBOConvertAgentTakenPO(list, operateType);
		if (resultBO.isError()) {
			return resultBO;
		}
		// 对象转换
		Map<String, List<AgentTransTakenPO>> map = (Map<String, List<AgentTransTakenPO>>) resultBO.getData();
		List<AgentTransTakenPO> allList = map.get("allList");
		if (allList.size() != list.size()) {
			logger.info("CMS提交银行处理结果数量【" + list.size() + "】与实际成功和失败数量【" + allList.size() + "】不符");
			return ResultBO.err(MessageCodeConstants.PARAM_IS_FIELD);
		}
		// 批量更新提款记录
		// agentCoreService.updateTakenByBatch(allList);

		// 审核，状态都是一致的
		if (operateType.equals(PayConstants.TakenOperateTypeEnum.AUDIT.getKey())) {
			// list为1个或者多个时，状态都是一致的，要么审核通过，要么审核不通过
			updateTransUser(allList, list, allList);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUBMIT.getKey())) {
			// 提交银行，状态改成银行处理中或者处理失败
			List<AgentTransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list, allList);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.BANK_COMPLETE.getKey())) {
			// 收到银行处理结果，完成提款（处理成功、处理失败）
			List<AgentTransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list, allList);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.CMS_COMPLETE.getKey())) {
			// CMS修改已到账
			updateTransUser(allList, list, allList);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUCCESS_TO_FAIL.getKey())) {
			// CMS修改，银行处理成功改成银行处理失败
			List<AgentTransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list, allList);
		}
		logger.info("CMS批量操作用户提款【" + takenOperateTypeEnum.getValue() + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 检验提现参数
	* @auth: xiongJinGang
	* @param withdrawApply
	* @param userInfo
	* @time: 2018年3月3日 下午3:32:43
	* @return: AgentInfoBO 
	*/
	private ResultBO<?> checkParam(WithdrawApplyVO withdrawApply, UserInfoBO userInfo, String type) {
		AgentInfoBO agentInfo = findAgentInfo(withdrawApply.getToken());
		// 未获取到代理信息
		if (ObjectUtil.isBlank(agentInfo)) {
			return ResultBO.err(MessageCodeConstants.AGENT_INFO_NOT_FOUND_ERROR);
		}
		if ("1".equals(type) && ObjectUtil.isBlank(withdrawApply.getBankCardId())) {
			return ResultBO.err(MessageCodeConstants.PAY_BANK_CARD_ID_IS_NULL_FIELD);
		}
		// 提现金额不能为空
		if (ObjectUtil.isBlank(withdrawApply.getAmount())) {
			return ResultBO.err(MessageCodeConstants.AGENT_TAKEN_AMOUNT_IS_NULL);
		}
		// 最低提现金额不能低于10元
		if (MathUtil.compareTo(withdrawApply.getAmount(), AgentConstants.LOWEST_TAKEN_AMOUNT) < 0) {
			return ResultBO.err(MessageCodeConstants.AGENT_LOWEST_TAKEN_AMOUNT_IS_ERROR);
		}

		// 代理钱包中的金额低于提现金额
		if (MathUtil.compareTo(withdrawApply.getAmount(), agentInfo.getAgentWallet()) > 0) {
			return ResultBO.err(MessageCodeConstants.AGENT_TAKEN_AMOUNT_IS_ERROR);
		}
		return ResultBO.ok(agentInfo);
	}

	/**  
	* 方法说明: 计算税费
	* 代理金额收税标准
	* 0<提现金额<=800 应纳税额=提现金额*0%
	* 800<提现金额<=4000 应纳税额=(提现金额-800)*20%
	* 4000<提现金额<=20000 应纳税额=提现金额*(1-20%)*20%
	* 20000<提现金额<=50000 应纳税额=提现金额*(1-20%)*30%-2000
	* 提现金额>50000 应纳税额=提现金额*(1-20%)*40%-7000
	* @auth: xiongJinGang
	* @param takenAmount
	* @param agentWallet
	* @time: 2018年3月3日 下午3:10:56
	* @return: Double 
	*/
	private Double countTaxAmount(Double takenAmount) {
		Double taxAmount = 0d;
		if (MathUtil.compareTo(takenAmount, AgentConstants.EIGHT_HUNDRED) > 0 && MathUtil.compareTo(takenAmount, AgentConstants.FOUR_THOUSAND) <= 0) {
			// 800<提现金额<=4000 应纳税额=(提现金额-800)*20%
			taxAmount = MathUtil.mul(MathUtil.sub(takenAmount, 800), AgentConstants.TWENTY_PERCENT);
		} else if (MathUtil.compareTo(takenAmount, AgentConstants.FOUR_THOUSAND) > 0 && MathUtil.compareTo(takenAmount, AgentConstants.TWENTY_THOUSAND) <= 0) {
			// 4000<提现金额<=20000 应纳税额=提现金额*(1-20%)*20%
			taxAmount = MathUtil.mul(MathUtil.mul(takenAmount, 0.8), AgentConstants.TWENTY_PERCENT);
		} else if (MathUtil.compareTo(takenAmount, AgentConstants.TWENTY_THOUSAND) > 0 && MathUtil.compareTo(takenAmount, AgentConstants.FIFTY_THOUSAND) <= 0) {
			// 20000<提现金额<=50000 应纳税额=提现金额*(1-20%)*30%-2000
			taxAmount = MathUtil.sub(MathUtil.mul(MathUtil.mul(takenAmount, 0.8), AgentConstants.THIRTY_PERCENT), 2000);
		} else if (MathUtil.compareTo(takenAmount, AgentConstants.FIFTY_THOUSAND) > 0) {
			// 提现金额>50000 应纳税额=提现金额*(1-20%)*40%-7000
			taxAmount = MathUtil.sub(MathUtil.mul(MathUtil.mul(takenAmount, 0.8), AgentConstants.FORTY_PERCENT), 7000);
		}
		return taxAmount;
	}

	/**  
	* 方法说明: 创建确认提款结果
	* @auth: xiongJinGang
	* @param withdrawApply
	* @param userInfo
	* @param takenAmount
	* @param agentWallet
	* @param taxAmount
	* @time: 2018年3月3日 下午3:07:51
	* @return: ConfirmTakenResultVO 
	*/
	private ConfirmTakenResultVO createConfirmResult(WithdrawApplyVO withdrawApply, UserInfoBO userInfo, PayBankcardBO bankCard, Double agentWallet, Double taxAmount) {
		ConfirmTakenResultVO confirmTakenResult = new ConfirmTakenResultVO();
		confirmTakenResult.setTakenAmount(withdrawApply.getAmount());
		// confirmTakenResult.setArrivalTime(arrivalTime);//预计到账时间，先不展示
		confirmTakenResult.setBankName(bankCard.getBankname());
		confirmTakenResult.setCardCode(StringUtil.hideHeadString(bankCard.getCardcode()));
		confirmTakenResult.setArrivalAmount(MathUtil.sub(withdrawApply.getAmount(), taxAmount));
		confirmTakenResult.setTax(taxAmount);
		confirmTakenResult.setFee(0d);
		confirmTakenResult.setBalance(MathUtil.sub(agentWallet, withdrawApply.getAmount()));
		return confirmTakenResult;
	}

	/**  
	* 方法说明: 代理确认提款缓存key
	* @auth: xiongJinGang
	* @param token
	* @param takenAmount
	* @time: 2018年3月3日 下午3:04:22
	* @return: String 
	*/
	private String makeTakenKey(String token, Double takenAmount) {
		StringBuffer sBuffer = new StringBuffer(CacheConstants.P_CORE_AGENT_TAKEN_RECORD).append(token).append(takenAmount);
		return sBuffer.toString();
	}

	/**  
	* 方法说明: 验证充值请求参数
	* @auth: xiongJinGang
	* @param transferAccounts
	* @param userInfo
	* @throws Exception
	* @time: 2018年3月5日 上午10:04:43
	* @return: void 
	*/
	private ResultBO<?> checkTransAccountParam(TransferAccountsVO transferAccounts, UserInfoBO userInfo) throws Exception {
		// 验证码校验
		if (StringUtil.isBlank(transferAccounts.getValidateCode())) {
			return ResultBO.err(MessageCodeConstants.VERIFYCODE_IS_NULL_FIELD);
		} else {
			String redisCode = redisUtil.getString(userInfo.getMobile() + UserConstants.MessageTypeEnum.OTHER_MSG.getKey());
			// 根据缓存判断验证码有效性
			if (ObjectUtil.isBlank(redisCode)) {
				return ResultBO.err(MessageCodeConstants.VERIFYCODE_ERROR_SERVICE);
			}
			if (!transferAccounts.getValidateCode().equals(redisCode)) {
				return ResultBO.err(MessageCodeConstants.VERIFYCODE_ERROR_SERVICE);
			}
		}
		// 充值账号不能为空
		if (ObjectUtil.isBlank(transferAccounts.getRechargeAccount())) {
			return ResultBO.err(MessageCodeConstants.AGENT_RECHARGE_ACCOUNT_IS_NULL);
		}
		// 真实姓名不能为空
		if (ObjectUtil.isBlank(transferAccounts.getRealName())) {
			return ResultBO.err(MessageCodeConstants.REALNAME_IS_NULL_FIELD);
		}
		// 充值金额不能为空
		if (ObjectUtil.isBlank(transferAccounts.getRechargeAmount())) {
			return ResultBO.err(MessageCodeConstants.PAY_RECHARGE_BALANCE_IS_NULL_FIELD);
		}
		// 转账金额不能低于10元
		if (MathUtil.compareTo(AgentConstants.LOWEST_TAKEN_AMOUNT, Double.valueOf(transferAccounts.getRechargeAmount())) > 0) {
			return ResultBO.err(MessageCodeConstants.AGENT_TRANS_AMOUNT_IS_ERROR);
		}
		// 根据账号获取会员信息
		UserInfoBO userInfoBO = userInfoService.findUserByAccountName(transferAccounts.getRechargeAccount());
		if (ObjectUtil.isBlank(userInfoBO)) {
			return ResultBO.err(MessageCodeConstants.AGENT_RECHARGE_ACCOUNT_IS_ERROR);
		}
		// 真实姓名不匹配
		if (!transferAccounts.getRealName().equals(userInfoBO.getRealName())) {
			return ResultBO.err(MessageCodeConstants.AGENT_RECHARGE_ACCOUNT_REALNAME_IS_ERROR);
		}
		return ResultBO.ok(userInfoBO);
	}

	/**  
	* 方法说明: 验证代理钱包
	* @auth: xiongJinGang
	* @param transferAccounts
	* @param userInfo
	* @throws Exception
	* @time: 2018年3月5日 上午11:19:55
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> checkAgentParam(TransferAccountsVO transferAccounts, UserInfoBO userInfo) throws Exception {
		AgentInfoBO agentInfo = findAgentInfo(transferAccounts.getToken());
		if (ObjectUtil.isBlank(agentInfo)) {
			return ResultBO.err(MessageCodeConstants.AGENT_INFO_NOT_FOUND_ERROR);
		}
		// 验证代理钱包中金额是否大于等于需要充值的金额
		if (MathUtil.compareTo(agentInfo.getAgentWallet(), Double.valueOf(transferAccounts.getRechargeAmount())) < 0) {
			return ResultBO.err(MessageCodeConstants.AGENT_RECHARGE_ACCOUNT_AMOUNT_IS_ERROR);
		}
		return ResultBO.ok(agentInfo);
	}

	private TransRechargePO createAgentRechargeRecord(AgentInfoBO agentPay, Double updateAmount, Integer memberId, String agentTradeNo) {
		TransRechargePO transRechargePO = new TransRechargePO();
		transRechargePO = new TransRechargePO();
		transRechargePO.setUserId(memberId);// 用户Id
		// transRechargePO.setCreateBy(cmsRecharge.getOperator());// 创建人
		transRechargePO.setRemark("代理系统充值");// 充值描述
		transRechargePO.setRechargeAmount(updateAmount);// 充值金额
		transRechargePO.setArrivalAmount(updateAmount);// 到账金额
		transRechargePO.setOrderCode(agentTradeNo);
		transRechargePO.setPayType(PayConstants.PayTypeEnum.AGENT_PAYMENT.getKey());// 代理系统充值
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 默认其它类型
		transRechargePO.setPayType(PayConstants.PayTypeEnum.AGENT_PAYMENT.getKey());// 其它支付
		transRechargePO.setRechargeBank(0);// 银行Id，人工充值没有银行
		transRechargePO.setBankCardType(PayConstants.BankCardTypeEnum.OTHER.getKey());// 其它银行类型
		transRechargePO.setRechargePlatform(PayConstants.TakenPlatformEnum.AGENT.getKey());// 充值平台
		transRechargePO.setRedAmount(0d);//
		transRechargePO.setTransStatus(PayConstants.TransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transRechargePO.setTransRechargeCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRechargePO.setServiceCharge(0.0);// 服务费
		transRechargePO.setChannelId(PayConstants.ChannelEnum.AGENT.getKey());// 渠道ID，前端传递的渠道ID
		transRechargePO.setRechargeChannel(PayConstants.PayChannelEnum.AGENT_RECHARGE.getKey());// 人工充值
		transRechargePO.setTransTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setThirdTransNum(agentTradeNo); // 第三方充值流水号
		transRechargePO.setResponseTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setUpdateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setTransEndTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRechargePO.setChannelCode(agentPay.getAgentCode()); // 代理编码
		transRechargePO.setTakenStatus(PayConstants.RechargeTakenStatusEnum.NOT_ALLOW.getKey());// 即买即付，默认不可提
		transRechargePO.setSwitchStatus(PayConstants.ChangeEnum.NO.getKey());// 是否切换，0不切换，1切换
		transRechargePO.setRechargeType(PayConstants.RechargeTypeEnum.RECHARGE.getKey());// 充值
		// transRechargePO.setRedCode("");// 红包编号
		// transRechargePO.setChannelCode(channelType);
		return transRechargePO;
	}

	/**  
	* 方法说明: 更新交易流水，退款给用户
	* @auth: xiongJinGang
	* @param list 处理失败的
	* @param takenList 所有申请的
	* @throws Exception
	* @time: 2017年8月7日 下午6:03:28
	* @return: void 
	*/
	@SuppressWarnings("unchecked")
	public void updateTransUser(List<AgentTransTakenPO> list, List<AgentTransTakenBO> takenList, List<AgentTransTakenPO> allList) throws Exception {
		agentTransTakenMapper.updateTakenByBatch(allList);
		if (!ObjectUtil.isBlank(list)) {
			// 给用户退款，通过第一个来判断状态
			AgentTransTakenPO transTakenPO = list.get(0);
			// 审核不通过或者银行处理失败，需要退款到用户账户。// 1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
			if (transTakenPO.getTransStatus().equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey()) || transTakenPO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
				logger.info("提款审核不通过或者银行处理失败，退款至用户钱包开始");
				List<AgentTransLogPO> transUserList = new ArrayList<AgentTransLogPO>();
				for (AgentTransTakenPO transTaken : list) {
					logger.info("提款编号【" + transTaken.getTransTakenCode() + "】审核不通过或者银行处理失败，退款【" + transTaken.getExtractAmount() + "】至用户钱包");
					AgentTransTakenBO agentTransTakenBO = agentCoreService.findAgentTakenDetailByCode(transTaken.getTransTakenCode());

					if (!ObjectUtil.isBlank(agentTransTakenBO)) {
						ResultBO<?> resultBO = agentCoreService.updateAgentWallet(agentTransTakenBO.getAgentId(), agentTransTakenBO.getExtractAmount(), AgentWalletTransTypeEnum.IN);
						AgentInfoBO agentInfo = (AgentInfoBO) resultBO.getData();

						addAgentTransUserList(transUserList, agentTransTakenBO, agentInfo);
					} else {
						logger.info("获取代理【" + transTaken.getAgentId() + "】提款编号【" + transTaken.getTransTakenCode() + "】详情失败");
						throw new RuntimeException("获取提款的交易记录失败");
					}
				}

				if (!ObjectUtil.isBlank(transUserList)) {
					// 需要添加退款交易流水
					agentCoreService.addTransLog(transUserList);
				}
			}
		}

		// 批量更新交易流水状态
		ResultBO<?> resultBO = TransUtil.agentTakenBOTotransUserPO(takenList);
		List<AgentTransLogPO> transList = (List<AgentTransLogPO>) resultBO.getData();
		if (!ObjectUtil.isBlank(transList)) {
			int num = agentCoreService.updateTransUserByBatch(transList);
			if (num <= 0) {
				logger.info("批量更新CMS交易记录状态失败");
				throw new RuntimeException("更新提款交易记录状态失败");
			}
			logger.debug("处理提款完成，批量更新交易流水状态【" + takenList.size() + "】条");
		}
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param transUserList
	* @param agentTransTakenBO
	* @param agentInfo
	* @time: 2018年3月10日 下午5:20:41
	* @return: void 
	*/
	private void addAgentTransUserList(List<AgentTransLogPO> transUserList, AgentTransTakenBO agentTransTakenBO, AgentInfoBO agentInfo) {
		AgentTransLogPO agentTransLogPO = new AgentTransLogPO();
		agentTransLogPO.setAgentId(agentInfo.getId());
		agentTransLogPO.setTransAmount(agentTransTakenBO.getExtractAmount());
		agentTransLogPO.setRealAmount(agentTransTakenBO.getExtractAmount());
		agentTransLogPO.setServiceCharge(0d);
		agentTransLogPO.setTaxCharge(0d);
		agentTransLogPO.setTradeCode(agentTransTakenBO.getTransTakenCode());
		agentTransLogPO.setTotalCashBalance(agentInfo.getAgentWallet());
		agentTransLogPO.setCreateBy(Constants.SYSTEM_OPERATE);
		AgentTransTypeEnum agentTransTypeEnum = AgentConstants.AgentTransTypeEnum.TAKEN_FAIL_BACK;
		agentTransLogPO.setRemark(agentTransTypeEnum.getValue());
		agentTransLogPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		agentTransLogPO.setTransInfo(agentTransTypeEnum.getValue());// 充值到账号：***3172
		agentTransLogPO.setTransStatus(AgentConstants.AgentTransStatusEnum.TRADE_SUCCESS.getKey());
		agentTransLogPO.setTransType(agentTransTypeEnum.getKey());
		transUserList.add(agentTransLogPO);
	}
}
