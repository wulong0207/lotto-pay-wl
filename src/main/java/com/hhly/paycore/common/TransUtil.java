package com.hhly.paycore.common;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.hhly.paycore.po.AgentTransLogPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.AgentConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.agent.bo.AgentTransTakenBO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.pay.vo.TransRechargeVO;
import com.hhly.skeleton.pay.vo.TransTakenVO;
import com.hhly.skeleton.pay.vo.TransUserVO;

/**
 * @desc 交易接口通用查询参数验证
 * @author xiongjingang
 * @date 2017年3月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class TransUtil {

	/**  
	* 方法说明: 验证查询日期
	* @param transParamVO
	* @time: 2017年3月7日 上午11:05:29
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateQueryDate(TransParamVO transParamVO) {
		// 时间比较
		Date startDate = transParamVO.getStartDate();
		Date endDate = transParamVO.getEndDate();

		if (ObjectUtil.isBlank(startDate)) {
			return ResultBO.err(MessageCodeConstants.TRANS_QUERY_STARTDATE_ERROR_SERVICE);
		} else {
			String startDateStr = DateUtil.convertDateToStr(startDate, DateUtil.DATE_FORMAT);
			startDateStr += " 00:00:00";
			startDate = DateUtil.convertStrToDate(startDateStr, DateUtil.DEFAULT_FORMAT);
		}
		// 结束时间为空，默认当前时间
		if (ObjectUtil.isBlank(endDate)) {
			String endDateStr = DateUtil.getNow();
			endDate = DateUtil.convertStrToDate(endDateStr);
		} else {
			String endDateStr = DateUtil.convertDateToStr(endDate, DateUtil.DATE_FORMAT);
			endDateStr += " 23:59:59";
			endDate = DateUtil.convertStrToDate(endDateStr, DateUtil.DEFAULT_FORMAT);
		}
		// 比较查询日期大小
		int num = DateUtil.compare(startDate, endDate);
		if (num >= 1) {
			return ResultBO.err(MessageCodeConstants.TRANS_QUERY_DATE_ERROR_SERVICE);
		}
		transParamVO.setStartDate(startDate);
		transParamVO.setEndDate(endDate);
		return ResultBO.ok(transParamVO);
	}

	/**  
	* 方法说明: 验证通用参数
	* @param validateService
	* @param transParamVO
	* @time: 2017年3月7日 下午2:28:57
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateCommonParam(TransParamVO transParamVO) {
		ResultBO<?> dateResult = validateQueryDate(transParamVO);
		if (dateResult.isError()) {
			return dateResult;
		}
		transParamVO = (TransParamVO) dateResult.getData();
		Short moneyFlow = transParamVO.getMoneyFlow();
		if (!PayConstants.MoneyFlowEnum.containsKey(moneyFlow)) {
			transParamVO.setMoneyFlow(null);
		}
		return ResultBO.ok(transParamVO);
	}

	/**  
	* 方法说明: 验证提款记录参数
	* @param transTaken
	* @time: 2017年3月9日 下午6:10:00
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateAddTaken(TransTakenVO transTaken) {
		if (ObjectUtil.isBlank(transTaken.getPayChannel())) {
			return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(transTaken.getTakenBank())) {
			return ResultBO.err(MessageCodeConstants.TRANS_TAKEN_BANK_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(transTaken.getTakenPlatform())) {
			return ResultBO.err(MessageCodeConstants.TRANS_TAKEN_PLATFORM_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(transTaken.getBankCardNum())) {
			return ResultBO.err(MessageCodeConstants.CARD_CODE_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(transTaken.getExtractAmount())) {
			return ResultBO.err(MessageCodeConstants.TRANS_AMOUNT_IS_NULL_FIELD);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证充值参数
	* @param transRecharge
	* @time: 2017年3月10日 上午10:53:40
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateAddRecharge(TransRechargeVO transRecharge) {
		Short rechargeChannel = transRecharge.getRechargeChannel();
		if (ObjectUtil.isBlank(rechargeChannel)) {
			return ResultBO.err(MessageCodeConstants.TRANS_RECHARGE_CHANNEL_IS_NULL_FIELD);
		} else {
			if (!PayConstants.PayChannelEnum.containsKey(rechargeChannel)) {
				return ResultBO.err(MessageCodeConstants.TRANS_PAY_CHANNEL_IS_ERROR_SERVICE);
			}
		}
		if (ObjectUtil.isBlank(transRecharge.getPayType())) {
			return ResultBO.err(MessageCodeConstants.TRANS_PAY_TYPE_IS_NULL_FIELD);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 验证查询基本参数
	* @auth: xiongJinGang
	* @param transUserVO
	* @time: 2017年4月5日 下午4:48:26
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateUserTransRecordByOrderCode(TransUserVO transUserVO) {
		// 订单编号为空
		if (ObjectUtil.isBlank(transUserVO.getOrderCode())) {
			return ResultBO.err(MessageCodeConstants.ORDER_CODE_IS_NULL_FIELD);
		}
		Short transType = transUserVO.getTransType();
		if (!ObjectUtil.isBlank(transType)) {
			if (!PayConstants.TransTypeEnum.containsKey(transType)) {
				return ResultBO.err(MessageCodeConstants.PAY_TRADE_TYPE_ERROR_SERVICE);
			}
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 提款记录BO转成交易流水PO
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2017年8月7日 下午12:21:46
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> takenBOTotransUserPO(List<TransTakenBO> list) throws Exception {
		List<TransUserPO> poList = new ArrayList<TransUserPO>();
		TransUserPO transUserPO = null;
		for (TransTakenBO transTakenBO : list) {
			// 银行处理成功，不修改交易数据信息
			if (transTakenBO.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey())) {
				continue;
			}
			transUserPO = new TransUserPO();
			transUserPO.setTradeCode(transTakenBO.getTransTakenCode());// 交易流水中的交易编号为提款记录中的提款编号
			transUserPO.setTransStatus(getTransUserStatusByTakenStatus(transTakenBO.getTransStatus()));
			transUserPO.setUpdateTime(DateUtil.getNowDate());
			transUserPO.setUserId(transTakenBO.getUserId());
			transUserPO.setTransType(PayConstants.TransTypeEnum.DRAWING.getKey());// 提款类型
			poList.add(transUserPO);
		}
		return ResultBO.ok(poList);
	}

	/**  
	* 方法说明: 代理的提款Bo转成交易流水PO
	* @auth: xiongJinGang
	* @param list
	* @throws Exception
	* @time: 2018年3月10日 下午5:46:54
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> agentTakenBOTotransUserPO(List<AgentTransTakenBO> list) throws Exception {
		List<AgentTransLogPO> poList = new ArrayList<AgentTransLogPO>();
		AgentTransLogPO agentTransPO = null;
		for (AgentTransTakenBO agentTransTaken : list) {
			// 银行处理成功，不修改交易数据信息
			if (agentTransTaken.getTransStatus().equals(PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey())) {
				continue;
			}
			agentTransPO = new AgentTransLogPO();
			agentTransPO.setTradeCode(agentTransTaken.getTransTakenCode());// 交易流水中的交易编号为提款记录中的提款编号
			agentTransPO.setTransStatus(getTransUserStatusByTakenStatus(agentTransTaken.getTransStatus()));
			agentTransPO.setUpdateTime(DateUtil.getNowDate());
			agentTransPO.setAgentId(agentTransTaken.getAgentId());
			agentTransPO.setTransType(AgentConstants.AgentTransTypeEnum.TAKEN_TO_BANK.getKey());// 提款类型
			poList.add(agentTransPO);
		}
		return ResultBO.ok(poList);
	}

	/**  
	* 方法说明: 验证活动信息
	* @auth: xiongJinGang
	* @param cmsRechargeVO
	* @time: 2017年8月21日 下午4:36:50
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> validateUserTransRecordByOrderCode(CmsRechargeVO cmsRechargeVO) {
		// 订单编号为空
		if (ObjectUtil.isBlank(cmsRechargeVO.getUserId())) {
			return ResultBO.err(MessageCodeConstants.USER_INFO_ERROR_SYS);
		}
		if (ObjectUtil.isBlank(cmsRechargeVO.getRechargeCode())) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(cmsRechargeVO.getRechargeAmount())) {
			return ResultBO.err(MessageCodeConstants.TRANS_AMOUNT_IS_NULL_FIELD);
		}
		if (ObjectUtil.isBlank(cmsRechargeVO.getActivityCode())) {
			return ResultBO.err(MessageCodeConstants.FOOTBALL_FIRST_ACTIVITY_NOT_EXIST);
		}
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 根据提款状态，提取交易流水状态
	* @auth: xiongJinGang
	* @param takenStatus
	* @time: 2017年8月7日 下午12:20:09
	* @return: Short 
	*/
	private static Short getTransUserStatusByTakenStatus(Short takenStatus) {
		TakenStatusEnum takenStatusEnum = PayConstants.TakenStatusEnum.getEnum(takenStatus);
		switch (takenStatusEnum) {
		case AUDIT_THROUGH:
			// 审核通过
			return PayConstants.UserTransStatusEnum.AUDIT_SUCCESS.getKey();
		case AUDIT_NOT_APPROVED:
			// 审核不通过
			return PayConstants.UserTransStatusEnum.AUDIT_FAIL.getKey();
		case BANK_HANDLING_SUCCESS:
			// 银行处理成功，交易流水的交易状态不变
			return PayConstants.UserTransStatusEnum.AUDIT_SUCCESS.getKey();
		case BANK_HANDLING_FAIL:
			// 银行处理失败
			return PayConstants.UserTransStatusEnum.TRADE_FAIL.getKey();
		case ARRIVAL_ACCOUNT:
			// 已到账
			return PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey();
		case PENDING_AUDIT:
			// 待审核
			return PayConstants.UserTransStatusEnum.WAIT_AUDIT.getKey();
		case BANK_PROCESSING:
			// 银行处理中
			return PayConstants.UserTransStatusEnum.WAIT_AUDIT.getKey();
		default:
			break;
		}
		return PayConstants.UserTransStatusEnum.WAIT_AUDIT.getKey();
	}
}
