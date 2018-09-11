package com.hhly.paycore.service.impl;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.common.PayUtil;
import com.hhly.paycore.common.TakenUtil;
import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.dao.TransTakenMapper;
import com.hhly.paycore.po.TransRechargePO;
import com.hhly.paycore.po.TransTakenPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.po.UserWalletPO;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.TransRechargeService;
import com.hhly.paycore.service.TransTakenConfirmService;
import com.hhly.paycore.service.TransUserLogService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayChannelEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenOperateTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.TakenStatusEnum;
import com.hhly.skeleton.base.constants.TransContans;
import com.hhly.skeleton.base.mq.msg.MessageModel;
import com.hhly.skeleton.base.mq.msg.OperateNodeMsg;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.base.util.StringUtil;
import com.hhly.skeleton.base.util.TokenUtil;
import com.hhly.skeleton.pay.bo.PageBO;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.TakenAfterUserWallet;
import com.hhly.skeleton.pay.vo.TakenAmountInfoVO;
import com.hhly.skeleton.pay.vo.TakenBankCardVO;
import com.hhly.skeleton.pay.vo.TakenConfirmVO;
import com.hhly.skeleton.pay.vo.TakenCountValidateVO;
import com.hhly.skeleton.pay.vo.TakenFlowVO;
import com.hhly.skeleton.pay.vo.TakenProcessVO;
import com.hhly.skeleton.pay.vo.TakenRealAmountVO;
import com.hhly.skeleton.pay.vo.TakenRechargeCountVO;
import com.hhly.skeleton.pay.vo.TakenReqParamVO;
import com.hhly.skeleton.pay.vo.TakenUserInfoVO;
import com.hhly.skeleton.pay.vo.TakenUserWalletVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransTakenVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 提款交易实现类
 * @author xiongjingang
 * @date 2017年3月2日 上午10:51:55
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("transTakenConfirmService")
public class TransTakenConfirmServiceImpl implements TransTakenConfirmService {

	private static final Logger logger = LoggerFactory.getLogger(TransTakenConfirmServiceImpl.class);

	@Resource
	private TransTakenMapper transTakenMapper;
	@Resource
	private BankcardService bankcardService;
	@Resource
	private PayBankService payBankService;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private TransRechargeService transRechargeService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private TransUserLogService transUserLogService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private UserInfoService userInfoService;
	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Value("${taken.Service.rate}")
	private String takenRate;// 提款服务费率

	@Override
	public ResultBO<?> findTakenByCode(String token, String transCode) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();

		if (ObjectUtil.isBlank(transCode)) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_IS_NULL_FIELD);
		}
		TransTakenBO transTakenBO = null;
		try {
			transTakenBO = transTakenMapper.findUserTakenByCode(userId, transCode);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(ResultBO.getMsg(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE, e));
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		if (null == transTakenBO) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		transTakenBO.setBankCardNum(StringUtil.hideHeadString(transTakenBO.getBankCardNum()));
		// 渠道名称
		if (!ObjectUtil.isBlank(transTakenBO.getPayChannel())) {
			PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByKey(transTakenBO.getPayChannel());
			if (null != payChannelEnum) {
				transTakenBO.setPayChannelName(payChannelEnum.getValue());
			}
		}
		// 充值银行名称
		if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
			PayBankBO payBankBO = payBankService.findBankFromCache(transTakenBO.getTakenBank());
			if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
				transTakenBO.setTakenBankName(payBankBO.getcName());
			}
		}
		// 交易状态
		if (!ObjectUtil.isBlank(transTakenBO.getTransStatus())) {
			TakenStatusEnum transStatusEnum = PayConstants.TakenStatusEnum.getEnum(transTakenBO.getTransStatus());
			if (null != transStatusEnum) {
				transTakenBO.setTransStatusName(transStatusEnum.getValue());
			}
		}
		List<TakenFlowVO> flowList = new ArrayList<TakenFlowVO>();
		// 申请提款
		flowList.add(new TakenFlowVO(transTakenBO.getCreateTime(), TransContans.SUPPLY_SUCCESS_REMARK));
		// 审核通过、不通过
		if (!ObjectUtil.isBlank(transTakenBO.getReviewTime())) {
			if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey())) {
				flowList.add(new TakenFlowVO(transTakenBO.getReviewTime(), TransContans.AUDIT_FAIL_REMAR + "，" + transTakenBO.getTransFailInfo()));
			} else {
				flowList.add(new TakenFlowVO(transTakenBO.getReviewTime(), TransContans.AUDIT_SUCCESS_REMARK));

				// 银行处理中
				if (!ObjectUtil.isBlank(transTakenBO.getDealTime())) {
					// 处理成功或者失败
					if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {// 银行处理时间
						flowList.add(new TakenFlowVO(transTakenBO.getDealTime(), TransContans.TAKEN_FAIL_REMARK + "，" + transTakenBO.getTransFailInfo()));
					} else {
						flowList.add(new TakenFlowVO(transTakenBO.getDealTime(), TransContans.BANK_PROCESSING_REMARK));
						// 提款到账时间
						if (!ObjectUtil.isBlank(transTakenBO.getArrivalTime())) {// 到账时间
							flowList.add(new TakenFlowVO(transTakenBO.getArrivalTime(), TransContans.TAKEN_ARRIVAL_REMARK));
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
		transTakenBO.setFlowList(flowList);
		return ResultBO.ok(transTakenBO);
	}

	@Override
	public ResultBO<?> findTakenListByPage(TransParamVO transParamVO) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transParamVO.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		transParamVO.setUserId(userId);
		ResultBO<?> resultBO = TransUtil.validateCommonParam(transParamVO);
		if (resultBO.isError()) {
			return resultBO;
		}
		transParamVO = (TransParamVO) resultBO.getData();
		int count = transTakenMapper.findTakenListCount(transParamVO);
		if (count > 0) {
			PageBO pageBO = new PageBO(transParamVO.getShowCount(), count, transParamVO.getCurrentPage());
			List<TransTakenBO> list = transTakenMapper.findTakenListByPage(transParamVO);
			if (!ObjectUtil.isBlank(list)) {
				for (TransTakenBO transTakenBO : list) {
					// 渠道名称
					if (!ObjectUtil.isBlank(transTakenBO.getPayChannel())) {
						PayChannelEnum payChannelEnum = PayConstants.PayChannelEnum.getByKey(transTakenBO.getPayChannel());
						if (null != payChannelEnum) {
							transTakenBO.setPayChannelName(payChannelEnum.getValue());
						}
					}
					// 充值银行名称
					if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
						PayBankBO payBankBO = payBankService.findBankFromCache(transTakenBO.getTakenBank());
						if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
							transTakenBO.setTakenBankName(payBankBO.getcName());
						}
					}
					// 交易状态
					if (!ObjectUtil.isBlank(transTakenBO.getTransStatus())) {
						TakenStatusEnum transStatusEnum = PayConstants.TakenStatusEnum.getEnum(transTakenBO.getTransStatus());
						if (null != transStatusEnum) {
							transTakenBO.setTransStatusName(transStatusEnum.getValue());
						}
					}
				}
			}
			pageBO.setDataList(list);
			return ResultBO.ok(pageBO);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> addTransTaken(TransTakenVO transTakenVO) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transTakenVO.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		transTakenVO.setUserId(userId);
		// 验证必填项
		ResultBO<?> paramBo = TransUtil.validateAddTaken(transTakenVO);
		if (paramBo.isError()) {
			return paramBo;
		}
		UserInfoBO userInfoBO = userInfoService.findUserInfo(userId);
		TransTakenPO transTakenPO = new TransTakenPO();
		BeanUtils.copyProperties(transTakenPO, transTakenVO);
		transTakenPO.setTransTakenCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
		transTakenPO.setUserId(userInfo.getId());
		transTakenPO.setCreateBy(userInfoBO.getRealName());
		int num = transTakenMapper.addTakenTrans(transTakenPO);
		if (num <= 0) {
			logger.info("添加提款交易失败：" + transTakenVO.toString());
			return ResultBO.err(MessageCodeConstants.ADD_DATA_FAIL_SERVICE);
		}
		return ResultBO.ok();
	}

	@Override
	@SuppressWarnings("unchecked")
	public ResultBO<?> updateTakenStatusByBatch(List<TransTakenBO> list, Short operateType) throws Exception {
		TakenOperateTypeEnum takenOperateTypeEnum = PayConstants.TakenOperateTypeEnum.getEnum(operateType);
		logger.info("CMS批量操作用户提款开始，操作数量：" + list.size() + "，操作类型：" + takenOperateTypeEnum.getValue());
		if (ObjectUtil.isBlank(list)) {
			logger.debug("批量审核参数为空");
			return ResultBO.err(MessageCodeConstants.PARAM_IS_NULL_FIELD);
		}
		ResultBO<?> resultBO = TakenUtil.takenBOConvertTakenPO(list, operateType);
		if (resultBO.isError()) {
			return resultBO;
		}
		// 对象转换
		Map<String, List<TransTakenPO>> map = (Map<String, List<TransTakenPO>>) resultBO.getData();
		List<TransTakenPO> allList = map.get("allList");
		if (allList.size() != list.size()) {
			logger.info("CMS提交银行处理结果数量【" + list.size() + "】与实际成功和失败数量【" + allList.size() + "】不符");
			return ResultBO.err(MessageCodeConstants.PARAM_IS_FIELD);
		}
		// 批量更新提款记录
		transTakenMapper.updateTakenByBatch(allList);

		// 审核，状态都是一致的
		if (operateType.equals(PayConstants.TakenOperateTypeEnum.AUDIT.getKey())) {
			// list为1个或者多个时，状态都是一致的，要么审核通过，要么审核不通过
			updateTransUser(allList, list);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUBMIT.getKey())) {
			// 提交银行，状态改成银行处理中或者处理失败
			List<TransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.BANK_COMPLETE.getKey())) {
			// 收到银行处理结果，完成提款（处理成功、处理失败）
			List<TransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.CMS_COMPLETE.getKey())) {
			// CMS修改已到账
			updateTransUser(allList, list);
		} else if (operateType.equals(PayConstants.TakenOperateTypeEnum.SUCCESS_TO_FAIL.getKey())) {
			// CMS修改，银行处理成功改成银行处理失败
			List<TransTakenPO> failList = map.get("fail");
			updateTransUser(failList, list);
		}
		logger.info("CMS批量操作用户提款【" + takenOperateTypeEnum.getValue() + "】结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 提款前，验证用户提款次数，超过当天最多提款次数，弹出进行中的提款记录
	* @auth: xiongJinGang
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年11月4日 上午11:59:48
	* @return: ResultBO<?> 
	*/
	@Override
	public ResultBO<?> validateTakenCount(TakenReqParamVO takenReqParamVO) throws Exception {
		String token = takenReqParamVO.getToken();
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();// 用户Id
		// 验证用户提款次数

		int takenCount = findUserTakenTimes(userId);
		ResultBO<?> resultBO = TakenUtil.validateTakenTimes(takenCount);
		TakenCountValidateVO takenCountValidateVO = new TakenCountValidateVO();
		takenCountValidateVO.setCount(takenCount);
		if (resultBO.isError()) {
			// 银行当天的提款超过当日最多可提次数，查询出所有在进行中的提款记录
			logger.info("用户【" + userInfo.getId() + "】提款次数超限");
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("userId", userId);

			Short[] transStatus = { PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey(), PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey(), PayConstants.TakenStatusEnum.PENDING_AUDIT.getKey(),
					PayConstants.TakenStatusEnum.BANK_PROCESSING.getKey() };
			paramMap.put("transStatus", transStatus);

			List<TransTakenBO> list = transTakenMapper.getProcessTakenList(paramMap);
			if (!ObjectUtil.isBlank(list)) {
				List<TakenProcessVO> takenProcessList = new ArrayList<TakenProcessVO>();
				TakenProcessVO takenProcessVO = null;
				for (TransTakenBO transTakenBO : list) {
					takenProcessVO = new TakenProcessVO(transTakenBO);

					// 充值银行名称
					if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
						PayBankBO payBankBO = payBankService.findBankFromCache(transTakenBO.getTakenBank());
						if (!ObjectUtil.isBlank(transTakenBO.getTakenBank())) {
							takenProcessVO.setbImg(payBankBO.getbLogo());
							takenProcessVO.setsImg(payBankBO.getsLogo());
							takenProcessVO.setBankName(payBankBO.getcName());
						}
					}
					// 交易状态
					if (!ObjectUtil.isBlank(transTakenBO.getTransStatus())) {
						TakenStatusEnum transStatusEnum = PayConstants.TakenStatusEnum.getEnum(transTakenBO.getTransStatus());
						if (null != transStatusEnum) {
							takenProcessVO.setTakenStatusName(transStatusEnum.getValue());
						}
					}
					takenProcessList.add(takenProcessVO);
				}
				takenCountValidateVO.setList(takenProcessList);
				return ResultBO.ok(takenCountValidateVO);
			}
		} else {
			List<PayBankcardBO> list = bankcardService.findUserBankList(userId);
			if (!ObjectUtil.isBlank(list)) {
				for (PayBankcardBO payBankcardBO : list) {
					// 存在储蓄卡，跳出
					if (payBankcardBO.getBanktype().equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
						takenCountValidateVO.setHaveBankCard(PayConstants.IsDefaultEnum.TRUE.getKey());
						break;
					}
				}
			}
		}
		return ResultBO.ok(takenCountValidateVO);
	}

	@Override
	public ResultBO<?> takenCount(TakenReqParamVO takenReqParamVO) throws Exception {
		String token = takenReqParamVO.getToken();
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();// 用户Id
		// 验证用户提款次数
		ResultBO<?> resultBO = findUserTakenTimesAndValidate(userInfo.getId());
		if (resultBO.isError()) {
			logger.info("用户【" + userInfo.getId() + "】提款次数超限");
			return resultBO;
		}
		Double takenAmount = takenReqParamVO.getTakenAmount();// 需要提款的金额
		if (ObjectUtil.isBlank(takenAmount)) {
			logger.debug("用户【" + userId + "】提款金额为空");
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_IS_NULL_FIELD);
		}
		// 提款展示给前端的钱包余额信息
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userId);
		TakenUserWalletVO takenUserWallet = findUserWallet(userWalletBO);
		if (ObjectUtil.isBlank(takenUserWallet)) {
			logger.info("用户【" + userId + "】钱包账户为空，不能提款");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		// 可提款金额（总现金金额+中奖余额）
		Double totalAmount = takenUserWallet.getTotalCashBalance();
		// 账户余额不足
		if (MathUtil.compareTo(totalAmount, 0d) <= 0) {
			logger.info("用户【" + userId + "】钱包现金账户余额【" + totalAmount + "】不足，不能提款");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		logger.info("用户【" + userId + "】申请提款开始，账户金额【" + userWalletBO.toString() + "】，提款金额【" + takenReqParamVO.getTakenAmount() + "】，提款银行卡【" + takenReqParamVO.getBankCardId() + "】");
		TakenConfirmVO takenConfirmVO = new TakenConfirmVO(takenUserWallet);
		takenConfirmVO.setBankCardId(takenReqParamVO.getBankCardId());
		Double top20Balance = takenUserWallet.getTop20Balance();// 20%的余额
		Double needTakenAmount = takenReqParamVO.getTakenAmount();// 需要提款的金额
		Double winBalance = takenUserWallet.getWinningBalance();// 中奖余额
		Double top80Balance = takenUserWallet.getTop80Balance();// 80%的余额
		Double winAnd80Balance = MathUtil.add(winBalance, top80Balance);// 中奖金额+80%金额总和

		// 计算总现金金额 是否等于 20%+80%+中奖金额
		resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 需要提现的金额大于现金总金额，返回金额不足
		if (MathUtil.compareTo(needTakenAmount, totalAmount) > 0) {
			logger.info("用户【" + userId + "】需要提款的金额【" + needTakenAmount + "】大于现金总余额【" + totalAmount + "】，不能提款");
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_LARGER_ERROR_SERVICE, totalAmount);
		}

		List<TakenBankCardVO> takenBankList = bankcardService.getUserBankInfo(userId, takenReqParamVO.getBankCardId());// 用户指定的提款银行
		if (ObjectUtil.isBlank(takenBankList)) {
			logger.info("未获取到用户【" + userId + "】指定的提款银行【" + takenReqParamVO.getBankCardId() + "】信息");
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}

		// 20%未消费完成
		if (MathUtil.compareTo(top20Balance, 0) > 0) {
			// 提款金额大于等于中奖金额+80%金额
			if (MathUtil.compareTo(needTakenAmount, winAnd80Balance) > 0) {
				Double needSubServiceChargeAmount = MathUtil.sub(needTakenAmount, winAnd80Balance);
				// 原路退回的充值记录
				resultBO = roadRefundRechargeRecord(userId, takenConfirmVO, top20Balance, needSubServiceChargeAmount);
				if (resultBO.isError()) {
					logger.info("用户【" + userId + "】正常提款错误：" + resultBO.getMessage());
					return resultBO;
				}
				takenConfirmVO = (TakenConfirmVO) resultBO.getData();
				boolean existException = false;
				boolean existNotAllow = false;
				Double totalService = 0d;
				int existExceptionNum = 0;
				int existNotAllowNum = 0;
				for (TakenAmountInfoVO takenAmountInfo : takenConfirmVO.getList()) {
					if (!takenAmountInfo.getStatus().equals(PayConstants.TakenAmountStatusEnum.NOT_ALLOW.getKey())) {
						if (!existException) {
							existException = true;
						}
						existExceptionNum++;
					} else {
						if (!existNotAllow) {
							existNotAllow = true;
						}
						existNotAllowNum++;
					}
					totalService = MathUtil.add(totalService, takenAmountInfo.getServiceFeeDou());
				}
				String confirmTips = "";
				if (existException) {
					// 提款金额包含{0}笔 （{1}元） 未达最低消费金额,将原卡返还。原卡返还预计15个工作日内到账。未达最低消费提款内收取{2}%的手续费。
					confirmTips = MessageFormat.format(Constants.TAKEN_APPLY_AMOUNT_TIPS, existExceptionNum, takenConfirmVO.getTotalTop20Balance(), Double.parseDouble(takenRate) * 100);
				}
				// 提款描述
				if (existNotAllow) {
					if (ObjectUtil.isBlank(confirmTips)) {
						confirmTips = MessageFormat.format(Constants.TAKEN_NOT_ALLOW_AMOUNT_TIPS, existNotAllowNum);
					} else {
						confirmTips += "；" + MessageFormat.format(Constants.TAKEN_NOT_ALLOW_AMOUNT_TIPS, existNotAllowNum);
					}
				}

				takenConfirmVO.setConfirmTips(confirmTips);
				takenConfirmVO.setList(null);
				takenConfirmVO.setTakenUserWallet(null);
				takenConfirmVO.setActualTakenAmount(MathUtil.add(winAnd80Balance, takenConfirmVO.getTotalTop20Balance()));
				return ResultBO.ok(takenConfirmVO);
			}
		}
		return ResultBO.ok();
	}

	// 由于要发短信和验证短信，支付这边不引用短信sdk，所以提款第1，2步聚在user-core中处理
	// PC端 提款请求的第三步，移动端的第二步
	@Override
	public ResultBO<?> taken(TakenReqParamVO takenReqParamVO) throws Exception {
		String token = takenReqParamVO.getToken();
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();// 用户Id
		// 验证用户提款次数
		ResultBO<?> resultBO = findUserTakenTimesAndValidate(userId);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】提款次数超限");
			return resultBO;
		}
		// 验证参数是否为空
		resultBO = TakenUtil.validateTakenParam(takenReqParamVO, false);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】提款参数验证不通过，参数【" + takenReqParamVO.toString() + "】：" + resultBO.getMessage());
			return resultBO;
		}
		// 验证提款token
		resultBO = validateTakenToken(token);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】提款token超时：" + resultBO.getMessage());
			return resultBO;
		}
		// 提款展示给前端的钱包余额信息
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userId);
		TakenUserWalletVO takenUserWallet = findUserWallet(userWalletBO);
		if (ObjectUtil.isBlank(takenUserWallet)) {
			logger.info("用户【" + userId + "】钱包账户为空，不能提款");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		// 可提款金额（总现金金额+中奖余额）,如果中奖金额设置成不可用，totalAmount中不包括中奖金额
		Double totalAmount = takenUserWallet.getTotalCashBalance();
		// 账户余额不足
		if (MathUtil.compareTo(totalAmount, 0d) <= 0) {
			logger.info("用户【" + userId + "】钱包现金账户余额【" + totalAmount + "】不足，不能提款");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		logger.info("用户【" + userId + "】申请提款开始，账户金额【" + userWalletBO.toString() + "】，提款金额【" + takenReqParamVO.getTakenAmount() + "】，提款银行卡【" + takenReqParamVO.getBankCardId() + "】");
		TakenConfirmVO takenConfirmVO = new TakenConfirmVO(takenUserWallet);
		takenConfirmVO.setBankCardId(takenReqParamVO.getBankCardId());
		Double top20Balance = takenUserWallet.getTop20Balance();// 20%的余额
		Double needTakenAmount = takenReqParamVO.getTakenAmount();// 需要提款的金额
		Double winBalance = takenUserWallet.getWinningBalance();// 中奖余额
		Double top80Balance = takenUserWallet.getTop80Balance();// 80%的余额
		Double winAnd80Balance = MathUtil.add(winBalance, top80Balance);// 中奖金额+80%金额总和

		// 计算总现金金额 是否等于 20%+80%+中奖金额
		resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			return resultBO;
		}

		// 需要提现的金额大于现金总金额，返回金额不足
		if (MathUtil.compareTo(needTakenAmount, totalAmount) > 0) {
			logger.info("用户【" + userId + "】需要提款的金额【" + needTakenAmount + "】大于现金总余额【" + totalAmount + "】，不能提款");
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_LARGER_ERROR_SERVICE, totalAmount);
		}
		// 提款金额不能小于1元，因为低于10元，要扣除1元手续费
		if (MathUtil.compareTo(needTakenAmount, Constants.TOKEN_LOWEST_ONE_AMOUNT) < 0) {
			logger.info("用户【" + userId + "】需要提款的金额【" + needTakenAmount + "】小于【" + Constants.TOKEN_LOWEST_ONE_AMOUNT + "】，不能提款");
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_LOWEST_ERROR_SERVICE);
		}
		List<TakenBankCardVO> takenBankList = bankcardService.getUserBankInfo(userId, takenReqParamVO.getBankCardId());// 用户指定的提款银行
		if (ObjectUtil.isBlank(takenBankList)) {
			logger.info("未获取到用户【" + userId + "】指定的提款银行【" + takenReqParamVO.getBankCardId() + "】信息");
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}

		// 20%消费完成，可以正常提款
		if (MathUtil.compareTo(top20Balance, 0) <= 0) {
			if (MathUtil.compareTo(needTakenAmount, Constants.TOKEN_LOWEST_TEN_AMOUNT) <= 0) {
				// 正常提款，低于10元，收一元手续费
				resultBO = TakenUtil.oneYuanTakenApply(takenConfirmVO, needTakenAmount, takenBankList);
			} else {
				// 正常提款，免收手续费
				resultBO = TakenUtil.normalTakenApply(takenConfirmVO, needTakenAmount, takenBankList);
			}
			if (resultBO.isError()) {
				logger.info("用户【" + userId + "】正常提款错误：" + resultBO.getMessage());
				return resultBO;
			}
			takenConfirmVO = (TakenConfirmVO) resultBO.getData();
		} else {
			// 20%部分没有使用完，提款金额小于等于中奖金额+80%金额，有2种情况
			if (MathUtil.compareTo(needTakenAmount, winAnd80Balance) <= 0) {
				if (MathUtil.compareTo(needTakenAmount, Constants.TOKEN_LOWEST_TEN_AMOUNT) <= 0) {
					// 正常提款，低于10元，收一元手续费
					resultBO = TakenUtil.oneYuanTakenApply(takenConfirmVO, needTakenAmount, takenBankList);
				} else {
					// 正常提款，免收手续费
					resultBO = TakenUtil.normalTakenApply(takenConfirmVO, needTakenAmount, takenBankList);
				}
				if (resultBO.isError()) {
					logger.info("用户【" + userId + "】正常提款错误：" + resultBO.getMessage());
					return resultBO;
				}
				takenConfirmVO = (TakenConfirmVO) resultBO.getData();
			} else {
				if (MathUtil.compareTo(winAnd80Balance, 0d) > 0) {
					// 20%部分没有使用完，提款金额大于中奖金额+80%金额。中奖金额+80%是免手续费提取
					resultBO = TakenUtil.normalTakenApply(takenConfirmVO, winAnd80Balance, takenBankList);
					if (resultBO.isError()) {
						logger.info("用户【" + userId + "】正常提款错误：" + resultBO.getMessage());
						return resultBO;
					}
					takenConfirmVO = (TakenConfirmVO) resultBO.getData();
				}

				if (MathUtil.compareTo(needTakenAmount, winAnd80Balance) > 0) {
					// 提款金额减去中奖金额+80%，得到需要扣手续费金额
					Double needSubServiceChargeAmount = MathUtil.sub(needTakenAmount, winAnd80Balance);
					// 原路退回的充值记录
					resultBO = roadRefundRechargeRecord(userId, takenConfirmVO, top20Balance, needSubServiceChargeAmount);
					if (resultBO.isError()) {
						logger.info("用户【" + userId + "】正常提款错误：" + resultBO.getMessage());
						return resultBO;
					}
					takenConfirmVO = (TakenConfirmVO) resultBO.getData();
				}
			}
		}
		if (MathUtil.compareTo(takenConfirmVO.getTotalServiceFee(), takenConfirmVO.getActualTakenAmount()) >= 0) {
			logger.info("用户【" + userId + "】提款手续费【" + takenConfirmVO.getTotalServiceFee() + "】大于需要提款的金额【" + takenConfirmVO.getActualTakenAmount() + "】，不能提款");
			// return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_NOT_ENOUGH_ERROR_SERVICE);
		}

		// 需要减的总金额
		if (MathUtil.compareTo(totalAmount, needTakenAmount) < 0 || MathUtil.compareTo(totalAmount, takenConfirmVO.getActualTakenAmount()) < 0) {
			logger.info("用户【" + userId + "】账户现金总额【" + totalAmount + "】小于需要提款的金额【" + needTakenAmount + "】，不能提款");
			// 需要提款的金额，小于账户可提现金额，返回错误
			return ResultBO.err(MessageCodeConstants.TAKEN_AMOUNT_LARGER_ERROR_SERVICE);
		} else {
			// 提款后剩余金额
			takenUserWallet.setTakenAfterBalance(MathUtil.sub(totalAmount, takenConfirmVO.getActualTakenAmount()));
		}

		// takenConfirmVO.setTakenToken(takenReqParamVO.getTakenToken());
		String key = TakenUtil.makeTakenKey(takenReqParamVO);
		if (!ObjectUtil.isBlank(takenConfirmVO)) {
			// 验证请求参数有没有包含支行名称，存在则判断是否一致，不一致则需要修改
			bankcardService.findBankByIdAndCheckName(userId, takenReqParamVO);
			redisUtil.addObj(key, takenConfirmVO, CacheConstants.FIVE_MINUTES);
		}
		return ResultBO.ok(takenConfirmVO);
	}

	/**  
	* 方法说明: 原路退款的充值记录
	* @auth: xiongJinGang
	* @param userId
	* @param takenConfirmVO
	* @param top20Balance
	* @param needTakenAmount
	* @param takenBankList
	* @param needSubServiceChargeAmount
	* @throws NumberFormatException
	* @time: 2017年8月22日 下午4:32:21
	* @return: Double 
	*/
	private ResultBO<?> roadRefundRechargeRecord(Integer userId, TakenConfirmVO takenConfirmVO, Double top20Balance, Double needSubServiceChargeAmount) throws NumberFormatException {
		// 查询充值记录
		TakenRechargeCountVO takenRechargeCountVO = countRechargeRecord(userId, needSubServiceChargeAmount);
		List<TransRechargeBO> list = takenRechargeCountVO.getList();
		if (ObjectUtil.isBlank(list)) {
			logger.info("未获取到用户【" + userId + "】充值记录，无法提款");
			return ResultBO.err(MessageCodeConstants.TAKEN_RECHARGE_COUNT_ERROR_SERVICE);
		}
		// 总的需要提款的充值记录
		List<TakenAmountInfoVO> allList = takenConfirmVO.getList();
		if (ObjectUtil.isBlank(allList)) {
			allList = new ArrayList<TakenAmountInfoVO>();
		}

		// 需要收手续费的部分
		List<TakenAmountInfoVO> takenList = addNeedChargeTaken(list, needSubServiceChargeAmount, top20Balance);
		Double newServiceFee = 0d;
		Double part20Amount = 0d;// 20%部分的金额
		int subNum = 0;
		for (TakenAmountInfoVO takenAmount : takenList) {
			// 计算正常提取和异常提取总共的金额
			if (!takenAmount.getStatus().equals(PayConstants.TakenAmountStatusEnum.NOT_ALLOW.getKey())) {
				newServiceFee = MathUtil.add(newServiceFee, takenAmount.getServiceFeeDou());
				part20Amount = MathUtil.add(part20Amount, takenAmount.getTaken20Amount());
			} else {
				// 超过手续费的，不能提取
				subNum++;
			}
		}
		allList.addAll(takenList);
		takenConfirmVO.setList(allList);
		int num = allList.size();
		if ((num - subNum) == 0) {
			takenConfirmVO.setConfirmTips(Constants.TAKEN_NOT_ALLOW_TIPS);
		} else {
			takenConfirmVO.setConfirmTips(MessageFormat.format(Constants.TAKEN_SERVICE_AMOUNT_TIPS, (num - subNum), MathUtil.formatAmountToStr(newServiceFee)));
		}
		takenConfirmVO.setTotalServiceFee(newServiceFee);
		takenConfirmVO.setTotalTop20Balance(part20Amount);// 需要提的总的20%部分金额
		takenConfirmVO.setActualTakenAmount(MathUtil.add(takenConfirmVO.getActualTakenAmount(), part20Amount));
		return ResultBO.ok(takenConfirmVO);
	}

	@Override
	public ResultBO<?> takenConfirm(TakenReqParamVO takenReqParamVO) throws Exception {
		String token = takenReqParamVO.getToken();
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();// 用户Id
		logger.info("用户【" + userId + "】确认提款开始，参数：" + takenReqParamVO.toString());
		// 验证参数是否为空
		ResultBO<?> resultBO = TakenUtil.validateTakenParam(takenReqParamVO, true);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】确认提款参数验证不过：" + resultBO.getMessage() + "，参数【" + takenReqParamVO.toString() + "】");
			return resultBO;
		}
		// 验证提款token是否存在
		resultBO = validateTakenToken(token);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】提款token" + token + "已过期");
			return resultBO;
		}
		// 验证用户提款次数
		resultBO = findUserTakenTimesAndValidate(userId);
		if (resultBO.isError()) {
			logger.info("用户【" + userId + "】当天提款次数已达上限");
			return resultBO;
		}
		String key = TakenUtil.makeTakenKey(takenReqParamVO);
		TakenConfirmVO takenConfirmVO = redisUtil.getObj(key, new TakenConfirmVO());
		if (ObjectUtil.isBlank(takenConfirmVO)) {
			logger.info("未从redis中获取到用户【" + userId + "】提款记录，key：" + key);
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_TIME_OUT_ERROR_SERVICE);
		}
		TakenUserWalletVO takenUserWalletVO = takenConfirmVO.getTakenUserWallet();

		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userId);
		if (ObjectUtil.isBlank(userWalletBO)) {
			logger.info("用户【" + userId + "】暂无钱包");
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}
		Double totalCashBalance = userWalletBO.getTotalCashBalance();// 可用总现金金额
		// 现金金额有变动
		if (MathUtil.compareTo(totalCashBalance, takenUserWalletVO.getTotalCashBalance()) != 0) {
			logger.info("用户【" + userId + "】申请提款现金金额【" + takenUserWalletVO.getTotalCashBalance() + "】与当前现金金额【" + totalCashBalance + "】不等，不能提现");
			return ResultBO.err(MessageCodeConstants.WALLET_TOTAL_CASH_CHANGE_ERROR_SERVICE);
		}
		// 现金总金额与（20%、80%、中奖）之和是否相等
		resultBO = PayUtil.countTotalAmount(userWalletBO);
		if (resultBO.isError()) {
			return resultBO;
		}

		resultBO = TakenUtil.getRealTaken(takenReqParamVO, takenConfirmVO);
		if (resultBO.isError()) {
			logger.info("计算用户【" + userId + "】真实提款失败：" + resultBO.getMessage());
			return resultBO;
		}
		TakenRealAmountVO takenRealAmountVO = (TakenRealAmountVO) resultBO.getData();
		logger.info("用户【" + userId + "】实际提款信息：" + JSON.toJSONString(takenRealAmountVO));
		// 保存到数据库
		try {
			updateTakenOperate(takenRealAmountVO, userInfo, userWalletBO, takenReqParamVO);
		} catch (Exception e) {
			logger.error("批量添加用户【" + userId + "】提款记录异常：" + e.getMessage());
			return ResultBO.err(MessageCodeConstants.TAKEN_REQUEST_ERROR_SERVICE);
		}
		// 剩余实际金额
		Double leaveTotalAmount = MathUtil.sub(userWalletBO.getTotalCashBalance(), takenRealAmountVO.getRealTakenAmount());
		TakenAfterUserWallet takenAfterUserWallet = new TakenAfterUserWallet(leaveTotalAmount);
		redisUtil.delObj(key);// 清空缓存中的请求
		redisUtil.delString(TakenUtil.makeTakenTokenKey(token));// 当前一次请求结束，用户需要重新验证才能进行提款
		logger.info("用户【" + userId + "】提款结束，账户剩余金额：" + JSON.toJSONString(takenAfterUserWallet));
		// 返回提款后剩余钱包总额
		return ResultBO.ok(takenAfterUserWallet);
	}

	/****************************app申请提款请求接口（第一步）******************************/
	@Override
	public ResultBO<?> takenReqForApp(String token) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}

		Integer userId = userInfo.getId();// 用户Id
		// 提款展示给前端的钱包余额信息
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(userId);
		TakenUserWalletVO takenUserWallet = findUserWallet(userWalletBO);

		PayBankcardBO bankCard = bankcardService.getSingleBankCard(userId);
		// 2018-06-21 去掉银行卡为空的验证
		/*if (ObjectUtil.isBlank(bankCard)) {
			logger.info("用户【" + userId + "】申请提款失败，未绑定银行卡");
			// 未绑定卡
			return ResultBO.err(MessageCodeConstants.NOT_BIND_BANK_CARD);
		}*/

		TakenUserInfoVO takenUserInfoVO = new TakenUserInfoVO();
		takenUserInfoVO.setUserWallet(takenUserWallet);
		takenUserInfoVO.setIdNo(StringUtil.hideString(userInfo.getIdCard(), (short) 1));// 身份证号码
		takenUserInfoVO.setRealName(bankCard.getRealname());
		List<TakenBankCardVO> takenBankList = bankcardService.getUserBankInfo(userId, null);
		// 2018-06-21 去掉银行卡为空的验证
		/*if (ObjectUtil.isBlank(takenBankList)) {
			logger.info("用户【" + userId + "】申请提款失败：" + MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}*/
		// 用户最近支付id 等于存的id 默认显示它
		if (!ObjectUtil.isBlank(userInfo.getUserPayId()) && !ObjectUtil.isBlank(takenBankList)) {
			for (int i = takenBankList.size() - 1; i >= 0; i--) {
				TakenBankCardVO takenBankCard = takenBankList.get(i);
				// 银行ID一致，并且是储蓄卡，将其
				if (takenBankCard.getBankId().equals(userInfo.getUserPayId()) && takenBankCard.getBankType().equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
					TakenBankCardVO first = takenBankCard;
					takenBankList.remove(takenBankCard);
					takenBankList.add(0, first);// 设置成第一位
				}
			}
		}
		takenUserInfoVO.setBankList(takenBankList);
		String takenToken = TokenUtil.createTokenStr();
		redisUtil.addString(TakenUtil.makeTakenTokenKey(token), takenToken, CacheConstants.FIVE_MINUTES);
		return ResultBO.ok(takenUserInfoVO);
	}

	/**  
	* 方法说明: 验证提款token
	* @auth: xiongJinGang
	* @param token
	* @param takenReqParamVO
	* @time: 2017年4月25日 上午11:51:27
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> validateTakenToken(String token) {
		String takenKey = TakenUtil.makeTakenTokenKey(token);
		// 验证提款token是否存在
		String takenToken = redisUtil.getString(takenKey);
		if (ObjectUtil.isBlank(takenToken)) {
			return ResultBO.err(MessageCodeConstants.TAKEN_CONFIRM_TIME_OUT_ERROR_SERVICE);
		}
		redisUtil.expire(takenKey, CacheConstants.FIVE_MINUTES);// 验证通过，重新设置key的有效时间为5分钟
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 添加需要提款的充值记录
	* @auth: xiongJinGang
	* @param needBackList
	* @param takenAmount
	* @param top20BalanceInWallet
	* @time: 2017年8月22日 下午6:44:20
	* @return: List<TakenAmountInfoVO> 
	*/
	private List<TakenAmountInfoVO> addNeedChargeTaken(List<TransRechargeBO> needBackList, Double needSubServiceChargeAmount, Double total20BalanceInWallet) {
		List<TakenAmountInfoVO> list = new LinkedList<TakenAmountInfoVO>();
		TakenAmountInfoVO takenAmountInfo = null;
		int num = 1;
		boolean flag = false;
		Double top20Balance = 0d;// 充值记录中，总的20%金额
		if (!ObjectUtil.isBlank(needBackList)) {
			for (TransRechargeBO transRechargeBO : needBackList) {
				if (flag) {
					break;
				}
				// 总的充值金额已经大于等于提款金额了，跳出
				takenAmountInfo = new TakenAmountInfoVO();
				// 充值到钱包中的金额
				Double arrivalAmount = transRechargeBO.getArrivalAmount();
				if (!ObjectUtil.isBlank(transRechargeBO.getInWallet())) {
					arrivalAmount = transRechargeBO.getInWallet();
				}

				Double singleTop20Balance = MathUtil.mul(arrivalAmount, Constants.USER_WALLET_TWENTY_PERCENT);// 充值记录中，20%的金额
				top20Balance = MathUtil.add(top20Balance, singleTop20Balance);// 充值记录中，总的20%记录
				// 当前提款金额
				Double nowTakenAmount = singleTop20Balance;
				// 当前总的充值记录中，20%部分大于需要收手续费的20%部分，20%部分全部提
				if (MathUtil.compareTo(top20Balance, needSubServiceChargeAmount) >= 0) {
					// 总的充值记录中20%的金额，大于用户提款需要收手续费的金额
					nowTakenAmount = singleTop20Balance;
					// 总的提款金额大于钱包中20%部分金额
					if (MathUtil.compareTo(top20Balance, total20BalanceInWallet) >= 0) {
						// 实际可提金额=钱包中20%金额-这笔之前总的需要提的20%金额
						nowTakenAmount = MathUtil.sub(total20BalanceInWallet, MathUtil.sub(top20Balance, singleTop20Balance));
					}
					flag = true;
				}
				takenAmountInfo.setTaken20Amount(nowTakenAmount);// 提款20%部分金额
				takenAmountInfo.setTakenAmount(MathUtil.formatAmountToStr(nowTakenAmount));// 提款金额
				takenAmountInfo.setTakenAmountDou(nowTakenAmount);
				takenAmountInfo.setExceptionRemark(MessageFormat.format(Constants.TAKEN_EXCEPTION_REMARK, Double.parseDouble(takenRate) * 100));
				String rechargeDate = DateUtil.convertDateToStr(transRechargeBO.getCreateTime(), DateUtil.DEFAULT_FORMAT);
				// 充值记录中20%的和小于等于账户中20%的金额
				takenAmountInfo.setExceptionTips(MessageFormat.format(Constants.TAKEN_EXCEPTION_TIPS, rechargeDate, MathUtil.formatAmountToStr(arrivalAmount)));
				Double serviceFee = MathUtil.mul(arrivalAmount, Double.parseDouble(takenRate));
				takenAmountInfo.setServiceFee(MathUtil.formatAmountToStr(serviceFee));// 收手续费，带格式的
				takenAmountInfo.setServiceFeeDou(serviceFee);// 不带格式的手续费
				takenAmountInfo.setStatus(PayConstants.TakenAmountStatusEnum.EXCEPTION.getKey());// 异常提款
				// 单笔提款的手续费大于当前提款金额，不能提
				if (MathUtil.compareTo(serviceFee, nowTakenAmount) > 0) {
					takenAmountInfo.setStatus(PayConstants.TakenAmountStatusEnum.NOT_ALLOW.getKey());
					takenAmountInfo.setNotAllowTips(Constants.TAKEN_NOT_ALLOW_TIPS);
					takenAmountInfo.setTitle(Constants.TAKEN_CAN_NOT_APPLICATION);
				} else {
					takenAmountInfo.setBankId(transRechargeBO.getRechargeBank());
					takenAmountInfo.setRechargeCode(transRechargeBO.getTransRechargeCode());// 充值流水号
					takenAmountInfo.setRechargeId(transRechargeBO.getId());// 充值流水编号
					takenAmountInfo.setArrivalTime(MessageFormat.format(Constants.TAKEN_ARRIVAL_TIME, DateUtil.getBeforeOrAfterDate(15), "（15日内）"));// 15天的时间
					// 不等于第三方支付
					if (!transRechargeBO.getBankCardType().equals(PayConstants.BankCardTypeEnum.OTHER.getKey()) && !ObjectUtil.isBlank(transRechargeBO.getBankCardNum())) {
						takenAmountInfo.setBankCard(StringUtil.hideHeadString(transRechargeBO.getBankCardNum()));// 第三方支付存的是邮箱或者微信ID
					}

					takenAmountInfo.setFullBankCard(transRechargeBO.getBankCardNum());
					PayBankBO payBankBO = payBankService.findBankFromCache(Integer.parseInt(transRechargeBO.getRechargeBank() + ""));
					if (ObjectUtil.isBlank(payBankBO)) {
						// 根据银行卡ID未获取到银行
						logger.info("根据充值银行卡ID【" + transRechargeBO.getRechargeBank() + "】未获取到银行，充值记录：" + transRechargeBO.toString());
						continue;
					}
					takenAmountInfo.setBankName(payBankBO.getName());
					Double actualAmount = MathUtil.sub(nowTakenAmount, serviceFee);
					takenAmountInfo.setActualAmount(MathUtil.formatAmountToStr(actualAmount));// 实际到账金额
					takenAmountInfo.setTakenId(num);// 自定义编号
					takenAmountInfo.setTitle(MessageFormat.format(Constants.TAKEN_EXCEPTION_APPLICATION, num));
					takenAmountInfo.setServiceFeeTips(MessageFormat.format(Constants.TAKEN_NEED_CHARGE_TIPS, serviceFee));
					takenAmountInfo.setRechargeChannel(transRechargeBO.getRechargeChannel());// 充值渠道
				}
				list.add(takenAmountInfo);
				num++;
			}
		}
		return list;
	}

	/**  
	* 方法说明: 获取需要计算的充值记录
	* @auth: xiongJinGang
	* @param userId
	* @param top20BalanceInWallet
	* @time: 2017年8月22日 下午4:28:27
	* @return: TakenRechargeCountVO 
	*/
	private TakenRechargeCountVO countRechargeRecord(Integer userId, Double top20BalanceInWallet) {
		Integer currendPage = 1;// 从第一页开始
		TakenRechargeCountVO takenRechargeCountVO = new TakenRechargeCountVO();
		// 获取用户可提款的充值记录
		Integer totalCount = transRechargeService.findRechargeRecordCountForTaken(userId);
		PageBO pageBO = new PageBO(totalCount, currendPage);
		Double takenRateDou = Double.parseDouble(takenRate);// 提款费率
		for (int i = 0; i < pageBO.getTotalPage(); i++) {
			// 分页获取充值记录（过滤掉后台人工充值的）
			List<TransRechargeBO> list = transRechargeService.findRechargeRecordForTaken(userId, currendPage);
			TakenUtil.countRecharge(list, top20BalanceInWallet, takenRechargeCountVO, takenRateDou);
			if (!takenRechargeCountVO.isBigger()) {
				currendPage++;
			} else {
				break;
			}
		}
		return takenRechargeCountVO;
	}

	/**  
	* 方法说明: 获取用户钱包记录
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月19日 上午11:48:43
	* @return: TakenUserWalletVO 
	*/
	private TakenUserWalletVO findUserWallet(UserWalletBO userWalletBO) {
		TakenUserWalletVO takenUserWalletVO = null;
		if (!ObjectUtil.isBlank(userWalletBO)) {
			takenUserWalletVO = new TakenUserWalletVO();
			try {
				BeanUtils.copyProperties(takenUserWalletVO, userWalletBO);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return takenUserWalletVO;
	}

	/**  
	* 方法说明: 添加提款记录，更新钱包中的余额
	* @auth: xiongJinGang
	* @param needTakenList
	* @param userInfoBO
	* @param userWalletBO
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年4月24日 下午6:44:19
	* @return: UserWalletPO 
	*/
	private ResultBO<?> updateTakenOperate(TakenRealAmountVO takenRealAmountVO, UserInfoBO userInfoBO, UserWalletBO userWalletBO, TakenReqParamVO takenReqParamVO) throws Exception {
		// 1、添加提款记录
		logger.info("开始给用户【" + userWalletBO.getUserId() + "】添加提款记录，更新钱包中的余额，添加交易流水记录");
		List<TakenAmountInfoVO> needTakenList = takenRealAmountVO.getNeedTakenList();
		// 批量添加提款记录
		List<TransTakenPO> list = addTransTakenBatch(needTakenList, userInfoBO, takenReqParamVO);
		if (ObjectUtil.isBlank(list)) {
			logger.info("添加用户【" + userInfoBO.getId() + "】提款记录失败，请求参数：" + takenReqParamVO.toString());
			throw new RuntimeException("插入提款记录失败！");
		}

		// 2、更新钱包中的余额
		ResultBO<?> resultBO = userWalletService.updateUserWalletBySplit(userInfoBO.getId(), takenRealAmountVO.getRealTakenAmount(), PayConstants.MoneyFlowEnum.OUT.getKey(), PayConstants.WalletSplitTypeEnum.WINNING_EIGHTY_TWENTY.getKey());
		if (resultBO.isError()) {
			logger.info("更新用户【" + userInfoBO.getId() + "】钱包失败。" + resultBO.getMessage());
			return resultBO;
		}
		// 账户钱包信息
		UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();

		// 3、记录交易流水，一条提款记录一条交易流水
		List<TransUserPO> transUserList = addTransUserParam(list, takenRealAmountVO.getRealTakenAmount(), takenReqParamVO.getChannelId(), userWalletBO, userWalletPO);

		// 二个交易流水表中都需要加记录，一个是给cms查看，一个是给用户查看
		int num = transUserService.addTransUserByBatch(transUserList);
		int num2 = transUserLogService.addTransUserByBatch(transUserList);
		if (num <= 0 && num2 <= 0) {
			logger.info("添加用户【" + userInfoBO.getId() + "】提款流水失败，请求参数：" + takenReqParamVO.toString());
			throw new RuntimeException("添加提款流水失败！");
		}
		// 4、更新充值状态不可用
		updateRechargeTakenStatus(needTakenList);
		// 5、提款记录插入正常，钱包余额更新正常，发送提款消息，一条提款记录一条消息
		OperateNodeMsg operateNodeMsg = null;
		MessageModel messageModel = new MessageModel();
		messageModel.setKey(Constants.MSG_NODE_RESEND);
		messageModel.setMessageSource("lotto-pay");
		for (TransTakenPO transTakenPO : list) {
			operateNodeMsg = new OperateNodeMsg();
			operateNodeMsg.setNodeId(5);
			operateNodeMsg.setNodeData(userInfoBO.getId() + ";" + transTakenPO.getTransTakenCode());// 用户ID;充值交易号
			messageModel.setMessage(operateNodeMsg);
			messageProvider.sendMessage(Constants.QUEUE_NAME_MSG_QUEUE, messageModel);
		}

		return ResultBO.ok();
	}

	/**  
	* 方法说明: 批量添加提款记录
	* @auth: xiongJinGang
	* @param needTakenList
	* @param userInfoBO
	* @param takenReqParamVO
	* @throws Exception
	* @time: 2017年4月21日 下午12:04:49
	* @return: list 
	*/
	private List<TransTakenPO> addTransTakenBatch(List<TakenAmountInfoVO> needTakenList, UserInfoBO userInfoBO, TakenReqParamVO takenReqParamVO) throws Exception {
		List<TransTakenPO> list = new ArrayList<TransTakenPO>();
		if (ObjectUtil.isBlank(needTakenList)) {
			return list;
		}
		Integer hadTakenTimes = findUserTakenTimes(userInfoBO.getId());// 已经提款次数
		Short hadTakenTimesShort = ObjectUtil.isBlank(hadTakenTimes) ? (short) 0 : Short.parseShort(hadTakenTimes + "");
		Short nowTimes = (short) (hadTakenTimesShort + 1);
		TransTakenPO transTakenPO = null;

		for (TakenAmountInfoVO takenAmountInfo : needTakenList) {
			transTakenPO = new TransTakenPO();
			transTakenPO.setTransTakenCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_OUT));
			transTakenPO.setUserId(userInfoBO.getId());
			transTakenPO.setCreateBy(userInfoBO.getRealName());
			transTakenPO.setTakenBank(takenAmountInfo.getBankId());// 交易银行
			PayBankBO payBankBO = payBankService.findBankFromCache(takenAmountInfo.getBankId());
			if (ObjectUtil.isBlank(payBankBO)) {
				logger.error("获取用户【" + userInfoBO.getId() + "】提款银行【" + takenAmountInfo.getBankId() + "】为空");
				throw new RuntimeException("提款银行为空");
			}
			transTakenPO.setBankInfo(payBankBO.getcName());
			if (takenAmountInfo.getStatus().equals(PayConstants.TakenAmountStatusEnum.EXCEPTION.getKey())) {
				TransRechargeBO transRechargeBO = transRechargeService.findRechargeByTransCode(takenAmountInfo.getRechargeCode());
				transTakenPO.setBankCardNum(transRechargeBO.getBankCardNum());
				transTakenPO.setPayChannel(takenAmountInfo.getRechargeChannel());// 充值渠道
			} else {
				// 正常提款，走人工
				transTakenPO.setBankCardNum(takenAmountInfo.getFullBankCard());
				transTakenPO.setPayChannel(PayConstants.PayChannelEnum.ARTIFICIAL_RECHARGE.getKey());
			}
			Double takenAmount = MathUtil.formatAmountToDouble(takenAmountInfo.getTakenAmount());
			Double takenService = MathUtil.formatAmountToDouble(takenAmountInfo.getServiceFee());
			transTakenPO.setExtractAmount(takenAmount);// 提款金额
			transTakenPO.setServiceCharge(takenService);// 提款手续费
			transTakenPO.setTransStatus(PayConstants.TakenStatusEnum.PENDING_AUDIT.getKey());// 待审核
			transTakenPO.setTakenPlatform(takenReqParamVO.getPlatform());// 提款平台
			transTakenPO.setChannelId(takenReqParamVO.getChannelId());// 渠道ID，现在默认一个
			// 批次状态先不处理
			// transTakenPO.setBatchStatus(PayConstants.TakenBatchStatusEnum.OPERATE_FAIL.getKey());// 批次状态，现在默认失败
			// 实际到账金额小于等于0，设置为0
			Double realAmount = MathUtil.sub(takenAmount, takenService);
			if (MathUtil.compareTo(realAmount, 0) <= 0) {
				realAmount = 0d;
			}
			transTakenPO.setRealAmount(realAmount);// 实际到账金额
			transTakenPO.setTakenTimes(nowTimes);// 提款次数
			transTakenPO.setTakenType(takenAmountInfo.getStatus());// 提款分类:1正常提款 2原路返回
			transTakenPO.setTransRechargeCode(takenAmountInfo.getRechargeCode());// 充值编号
			int num = transTakenMapper.addTakenTrans(transTakenPO);
			if (num <= 0) {
				logger.error("保存用户【" + userInfoBO.getId() + "】提款记录异常，参数：" + takenAmountInfo.toString());
				throw new RuntimeException("保存提款记录失败");
			}
			list.add(transTakenPO);
		}
		return list;
	}

	/**  
	* 方法说明: 获取用户提款次数
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月24日 上午10:06:53
	* @return: Integer 
	*/
	private Integer findUserTakenTimes(Integer userId) {
		String today = DateUtil.getNow(DateUtil.DATE_FORMAT);
		return transTakenMapper.getUserTakenTimes(userId, today);
	}

	/**  
	* 方法说明: 验证用户提款次数
	* @auth: xiongJinGang
	* @param userId
	* @time: 2017年4月24日 上午10:19:52
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> findUserTakenTimesAndValidate(Integer userId) {
		return TakenUtil.validateTakenTimes(findUserTakenTimes(userId));
	}

	/**  
	* 方法说明: 生成批量添加交易记录参数
	* @auth: xiongJinGang
	* @param list
	* @param takenReqParamVO
	* @time: 2017年8月3日 下午4:05:04
	* @return: List<TransUserPO> 
	*/
	private List<TransUserPO> addTransUserParam(List<TransTakenPO> list, Double realTakenAmount, String channelId, UserWalletBO userWalletBO, UserWalletPO userWalletPO) {
		Short transType = PayConstants.TransTypeEnum.DRAWING.getKey();

		Double userWinAmount = userWalletPO.getUseWinBalance();// 使用中奖金额
		Double use80Amount = userWalletPO.getUse80Balance();// 使用80%金额
		Double use20Amount = userWalletPO.getUse20Balance();// 使用20%金额
		Double beforeTotalCashBalance = userWalletBO.getTotalCashBalance();// 更新账户余额前，现金总金额

		List<TransUserPO> transUserList = new ArrayList<TransUserPO>();
		TransUserPO transUserPO = null;
		for (TransTakenPO transTakenPO : list) {
			transUserPO = new TransUserPO();
			transUserPO.setChannelId(channelId);
			transUserPO.setOrderCode("");// 订单信息
			Short transStatus = PayConstants.UserTransStatusEnum.WAIT_AUDIT.getKey();// 默认交易成功
			transUserPO.setTransStatus(transStatus);
			Double transAmount = transTakenPO.getExtractAmount();// 实际提款金额
			transUserPO.setTransAmount(transAmount);// 交易总金额；现金金额+红包金额+服务费
			transUserPO.setCashAmount(transTakenPO.getRealAmount());// 现金金额
			transUserPO.setServiceCharge(transTakenPO.getServiceCharge());// 服务费

			// 充值记录
			transUserPO.setTransType(transType);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
			transUserPO.setUserId(transTakenPO.getUserId());
			String orderInfo = transTakenPO.getBankInfo();
			if (!ObjectUtil.isBlank(transTakenPO.getBankCardNum())) {
				orderInfo += "：" + StringUtil.hideHeadString(transTakenPO.getBankCardNum());
			}
			transUserPO.setOrderInfo(orderInfo);// 描述
			transUserPO.setRemark(Constants.ACCOUNT_TAKEN);// 后台描述，账户提款
			transUserPO.setRedTransAmount(0d);// 红包金额
			// 获取交易号
			transUserPO.setTransCode(OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType)));
			// 每次递减
			beforeTotalCashBalance = MathUtil.sub(beforeTotalCashBalance, transAmount);
			transUserPO.setTotalRedBalance(userWalletBO.getEffRedBalance());// 剩余红包总金额
			transUserPO.setTotalCashBalance(beforeTotalCashBalance);// 剩余总现金金额
			transUserPO.setRedCode(null);// 红包编号

			// 计算每个账户使用金额
			Double singleWinAmount = 0d;
			Double single80Amount = 0d;
			Double single20Amount = 0d;
			if (MathUtil.compareTo(userWinAmount, transAmount) > 0) {
				singleWinAmount = transAmount;
				userWinAmount = MathUtil.sub(userWinAmount, transAmount);// 使用完后，中奖账户剩余金额
			} else {
				// 剩余使用的中奖金额账户小于等于使用的金额
				singleWinAmount = userWinAmount;
				Double needSub80Amount = MathUtil.sub(transAmount, userWinAmount);// 使用的中奖账户不够扣，需要扣80%账上的钱
				userWinAmount = 0d;
				if (MathUtil.compareTo(use80Amount, needSub80Amount) > 0) {
					single80Amount = needSub80Amount;
					use80Amount = MathUtil.sub(use80Amount, needSub80Amount);
				} else {
					single80Amount = use80Amount;// 使用80%金额
					Double needSub20Amount = MathUtil.sub(needSub80Amount, use80Amount);// 使用80%账户不够扣，需要减20%账户上的钱
					use80Amount = 0d;
					if (MathUtil.compareTo(use20Amount, needSub20Amount) > 0) {
						single20Amount = needSub20Amount;
						use20Amount = MathUtil.sub(use20Amount, needSub20Amount);
					} else {
						single20Amount = needSub20Amount;
					}
				}
			}
			transUserPO.setAmount20(single20Amount);
			transUserPO.setAmount80(single80Amount);
			transUserPO.setAmountWin(singleWinAmount);
			transUserPO.setTradeCode(transTakenPO.getTransTakenCode());// 提款编号
			transUserList.add(transUserPO);
		}
		return transUserList;
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
	private void updateTransUser(List<TransTakenPO> list, List<TransTakenBO> takenList) throws Exception {
		if (!ObjectUtil.isBlank(list)) {
			// 给用户退款，通过第一个来判断状态
			TransTakenPO transTakenPO = list.get(0);
			// 审核不通过或者银行处理失败，需要退款到用户账户。// 1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
			if (transTakenPO.getTransStatus().equals(PayConstants.TakenStatusEnum.AUDIT_NOT_APPROVED.getKey()) || transTakenPO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
				logger.info("提款审核不通过或者银行处理失败，退款至用户钱包开始");
				Short transStatus = PayConstants.UserTransStatusEnum.WAIT_AUDIT.getKey();// 默认是查待审核的
				// 当前如果是银行处理失败，需要查询审核通过的交易记录
				if (transTakenPO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey())) {
					transStatus = PayConstants.UserTransStatusEnum.AUDIT_SUCCESS.getKey();
				}
				List<TransRechargePO> rechargeList = new ArrayList<TransRechargePO>();
				TransUserPO transUserPO = null;
				List<TransUserPO> transUserList = new ArrayList<TransUserPO>();
				for (TransTakenPO transTaken : list) {
					String takenCode = transTaken.getTransTakenCode();
					logger.info("提款编号【" + takenCode + "】审核不通过或者银行处理失败，退款至用户钱包");
					// 需要查询出待审核的交易记录，获取每个钱包账户扣的钱的比例，然后按比例退
					TransUserBO transUserBO = transUserService.findTransUserBy(takenCode, transStatus);

					if (!ObjectUtil.isBlank(transUserBO)) {
						Integer userId = transUserBO.getUserId();
						logger.info("退还用户【" + userId + "】【" + takenCode + "】金额组成80，20，win【" + transUserBO.getAmount80() + "|" + transUserBO.getAmount20() + "|" + transUserBO.getAmountWin() + "】");
						ResultBO<?> resultBO = userWalletService.updateUserWalletCommon(userId, transUserBO.getAmount80(), transUserBO.getAmount20(), transUserBO.getAmountWin(), 0d, PayConstants.MoneyFlowEnum.IN.getKey());

						// 添加审核不通过交易流水
						UserWalletPO userWalletPO = (UserWalletPO) resultBO.getData();
						// 获取提款记录
						TransTakenBO transTakenBO = transTakenMapper.findUserTakenByCode(userId, takenCode);
						if (!ObjectUtil.isBlank(transTakenBO)) {
							transUserPO = addRefundTransUser(transTakenBO, userWalletPO);
							transUserList.add(transUserPO);

							// 判断提款类型是否为异常提款
							if (!ObjectUtil.isBlank(transTakenBO.getTakenType()) && transTakenBO.getTakenType().equals(PayConstants.TakenAmountStatusEnum.EXCEPTION.getKey())) {
								TransRechargePO transRechargePO = new TransRechargePO(transTakenBO.getTransRechargeCode(), PayConstants.RechargeTakenStatusEnum.ALLOW.getKey());
								logger.debug("将充值记录【" + transTakenBO.getTransRechargeCode() + "】提款状态改成可提");
								rechargeList.add(transRechargePO);
							}
						} else {
							logger.error("获取用户【" + userId + "】提款记录【" + takenCode + "】为空");
						}
					} else {
						logger.info("未获取到交易记录，参数：提款编号【" + takenCode + "】，状态【" + transStatus + "】");
						throw new RuntimeException("获取提款的交易记录失败");
					}
				}

				// 修改充值记录状态为可提
				if (!ObjectUtil.isBlank(rechargeList)) {
					// 审核不通过，需要将充值流水中的提款状态改成可提
					transRechargeService.updateRechargeTakenStatusByBatch(rechargeList);
					logger.debug("批量更新充值提款状态到可提【" + rechargeList.size() + "】条");
				}
				if (!ObjectUtil.isBlank(transUserList)) {
					// 需要添加退款交易流水
					transUserService.addTransUserByBatch(transUserList);
					// 用户交易流水中添加退款流水
					transUserLogService.addTransUserByBatch(transUserList);
				}
			}
		}

		// 批量更新交易流水状态
		ResultBO<?> resultBO = TransUtil.takenBOTotransUserPO(takenList);
		List<TransUserPO> transList = (List<TransUserPO>) resultBO.getData();
		if (!ObjectUtil.isBlank(transList)) {
			int num = transUserService.updateTransUserByBatch(transList);
			// 用户交易流水中添加退款流水
			int num2 = transUserLogService.updateTransUserByBatch(transList);
			if (num <= 0 && num2 <= 0) {
				logger.info("批量更新CMS交易记录状态失败");
				throw new RuntimeException("更新提款交易记录状态失败");
			}
			logger.debug("处理提款完成，批量更新交易流水状态【" + takenList.size() + "】条");
		}
	}

	/**  
	* 方法说明: 封装退款交易流水记录
	* @auth: xiongJinGang
	* @param transTaken
	* @param userWalletPO
	* @time: 2017年9月7日 下午1:26:20
	* @return: TransUserPO 
	*/
	private TransUserPO addRefundTransUser(TransTakenBO transTaken, UserWalletPO userWalletPO) {
		Short transType = PayConstants.TransTypeEnum.REFUND.getKey();
		String transCode = OrderNoUtil.getOrderNo(PayUtil.getNoHeadByTradeType(transType));

		String orderInfo = "提款" + transTaken.getBankInfo();
		if (!ObjectUtil.isBlank(transTaken.getBankCardNum())) {
			orderInfo += "：" + StringUtil.hideHeadString(transTaken.getBankCardNum());
		}
		TransUserPO transUserPO = new TransUserPO(transTaken.getUserId(), transCode, transType, orderInfo);// 1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它；7：活动
		// 获取交易号
		transUserPO.setTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易成功
		transUserPO.setCashAmount(transTaken.getExtractAmount());
		// transUserPO.setThirdTransId("");
		transUserPO.setChannelId(transTaken.getChannelId());
		transUserPO.setAmount20(userWalletPO.getUse20Balance());
		transUserPO.setAmount80(userWalletPO.getUse80Balance());
		transUserPO.setAmountWin(userWalletPO.getWinningBalance());
		transUserPO.setTradeCode(transTaken.getTransTakenCode());
		transUserPO.setRedTransAmount(0d);
		transUserPO.setTotalRedBalance(userWalletPO.getEffRedBalance());// 剩余红包总金额
		transUserPO.setTotalCashBalance(userWalletPO.getTotalCashBalance());// 剩余总现金金额

		transUserPO.setServiceCharge(0d);
		// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setTransAmount(transTaken.getExtractAmount());// 交易总金额；现金金额+红包金额+服务费
		transUserPO.setRemark("审核不通过");
		return transUserPO;
	}

	/**  
	* 方法说明: 更新原路返回充值记录的提款状态
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年8月24日 下午12:09:41
	* @return: void 
	*/
	private void updateRechargeTakenStatus(List<TakenAmountInfoVO> list) throws Exception {
		if (!ObjectUtil.isBlank(list)) {
			TransRechargePO transRechargePO = null;
			for (TakenAmountInfoVO takenAmountInfoVO : list) {
				// 提款状态 1正常 2异常
				if (takenAmountInfoVO.getStatus().equals(PayConstants.TakenAmountStatusEnum.EXCEPTION.getKey())) {

					transRechargePO = new TransRechargePO(takenAmountInfoVO.getRechargeId(), PayConstants.RechargeTakenStatusEnum.FINISHED.getKey());
					int result = transRechargeService.updateRecharge(transRechargePO);
					if (result <= 0) {
						logger.info("更新充值记录【" + takenAmountInfoVO.getRechargeCode() + "】提款状态到可提失败");
						throw new RuntimeException("更新充值记录的提款状态失败");
					}
				}
			}
		}
	}
}
