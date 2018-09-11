package com.hhly.paycore.remote.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.TransUtil;
import com.hhly.paycore.dao.TransUserLogMapper;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.remote.service.ITransUserLogService;
import com.hhly.paycore.service.OrderGroupContentService;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TransTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants.UserTransMoneyFlowEnum;
import com.hhly.skeleton.base.constants.PayConstants.UserTransStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.PageBO;
import com.hhly.skeleton.pay.bo.TransUserLogBO;
import com.hhly.skeleton.pay.vo.AppTransUserVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.skeleton.pay.vo.TransParamVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】 用户交易记录实现
 * @author xiongjingang
 * @date 2017年3月3日 下午2:42:12
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iTransUserLogService")
public class TransUserLogServiceImpl implements ITransUserLogService {
	public static final Logger logger = LoggerFactory.getLogger(TransTakenConfirmServiceImpl.class);

	@Resource
	private PayOrderUpdateService payOrderUpdateService;
	@Value("${recharge.Service.rate}")
	private String rechargeRate;// 充值服务费率
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Value("${taken.Service.rate}")
	private String takenRate;// 提款服务费率
	@Resource
	private TransUserLogMapper transUserLogMapper;
	@Resource
	private OrderGroupContentService orderGroupContentService;

	/**  
	* 方法说明: 添加交易记录【提供给客户查看】
	* @auth: xiongJinGang
	* @param transUserPO
	* @throws Exception
	* @time: 2017年11月9日 下午6:11:03
	* @return: int 
	*/
	@Override
	public int addTransLogRecord(TransUserPO transUserPO) throws Exception {
		return transUserLogMapper.addUserTrans(transUserPO);
	}

	@Override
	public int addTransUserByBatch(List<TransUserPO> list) throws Exception {
		return transUserLogMapper.addUserTransBatch(list);
	}

	/**  
	* 方法说明: 检查并拼装参数
	* @auth: xiongJinGang
	* @param transParamVO
	* @time: 2017年11月9日 上午10:30:20
	* @return: ResultBO<?> 
	*/
	private ResultBO<?> checkQueryParam(TransParamVO transParamVO) {
		UserInfoBO userInfo = userUtil.getUserByToken(transParamVO.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		transParamVO.setUserId(userInfo.getId());
		ResultBO<?> resultBO = TransUtil.validateCommonParam(transParamVO);
		if (resultBO.isError()) {
			return resultBO;
		}
		transParamVO = (TransParamVO) resultBO.getData();

		// 交易类型
		Short tradeType = transParamVO.getTradeType();
		// 资金流向
		Short moneyFlow = transParamVO.getMoneyFlow();
		// 如果查询条件中，选择了收入或者支出，判断收入支付是否有对应的值
		if (!ObjectUtil.isBlank(moneyFlow)) {
			// 验证资金流向的值是否存在
			Object[] tradeTypes = UserTransMoneyFlowEnum.getTransTypeByFlowType(moneyFlow);
			// 根据传入参数，未获取到资金流向的具体交易类型
			if (ObjectUtil.isBlank(tradeTypes)) {
				return ResultBO.err(MessageCodeConstants.TRANS_MONEY_FLOW_FIELD);
			}
			if (ObjectUtil.isBlank(tradeType)) {
				transParamVO.setTradeTypes(tradeTypes);
			} else {
				if (!TransTypeEnum.containsKey(tradeType)) {
					return ResultBO.err(MessageCodeConstants.PAY_TRADE_TYPE_ERROR_SERVICE);
				}
				// 验证资金流向和交易类型是否匹配，不匹配返回错误
				UserTransMoneyFlowEnum userTransMoneyFlowEnum = UserTransMoneyFlowEnum.getTransTypeByKey(tradeType);
				if (!userTransMoneyFlowEnum.getType().equals(moneyFlow)) {
					return ResultBO.err(MessageCodeConstants.PAY_MONEYFLOW_NOT_SUIT_ERROR_SERVICE);
				}
			}
		}

		// 获取元素
		int count = transUserLogMapper.findUserTransListCount(transParamVO);
		if (count <= 0) {
			return ResultBO.ok();
		}
		transParamVO.setTotalNum(count);
		return ResultBO.ok(transParamVO);
	}

	@Override
	public ResultBO<?> findAppTransUserByPage(TransParamVO transParamVO) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(transParamVO.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		transParamVO.setUserId(userId);

		int count = transUserLogMapper.findUserTransListCount(transParamVO);
		if (count > 0) {
			PageBO pageBO = new PageBO(transParamVO.getShowCount(), count, transParamVO.getCurrentPage());
			List<TransUserLogBO> list = transUserLogMapper.findUserTransListByPage(transParamVO);
			pageBO.setDataList(getAppTransUserList(list));
			return ResultBO.ok(pageBO);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> findTransUserByCode(String token, String transCode) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		if (ObjectUtil.isBlank(transCode)) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_IS_NULL_FIELD);
		}
		TransUserLogBO transUserBO = null;
		try {
			transUserBO = transUserLogMapper.findTransUserByCode(userId, transCode);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(ResultBO.getMsg(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE, e));
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		if (null == transUserBO) {
			return ResultBO.err(MessageCodeConstants.TRANS_CODE_ERROR_SERVICE);
		}
		// 添加符号
		UserTransMoneyFlowEnum userTransMoneyFlowEnum = PayConstants.UserTransMoneyFlowEnum.getTransTypeByKey(transUserBO.getTransType());
		String symbol = userTransMoneyFlowEnum.getType().equals(PayConstants.MoneyFlowEnum.IN.getKey()) ? "+" : "-";
		transUserBO.setShowAmount(symbol + String.format("%.2f", transUserBO.getTransAmount()));// 保留小数点后2位

		// 交易状态
		UserTransStatusEnum transStatusEnum = PayConstants.UserTransStatusEnum.getEnum(transUserBO.getTransStatus());
		if (null != transStatusEnum) {
			transUserBO.setTransStatusName(transStatusEnum.getValue());
		}

		// 充值详情，填充充值服务费率
		if (transUserBO.getTransType().equals(PayConstants.TransTypeEnum.RECHARGE.getKey())) {
			transUserBO.setServiceRate(ObjectUtil.isBlank(rechargeRate) ? 0d : Double.valueOf(rechargeRate));
		}
		// 提款详情，填充提款费率
		if (transUserBO.getTransType().equals(PayConstants.TransTypeEnum.DRAWING.getKey())) {
			transUserBO.setServiceRate(ObjectUtil.isBlank(takenRate) ? 0d : Double.valueOf(takenRate));
		}
		return ResultBO.ok(transUserBO);
	}

	@Override
	public ResultBO<?> findTransUserByPage(TransParamVO transParamVO) throws Exception {
		// 1、检查并拼装参数
		ResultBO<?> resultBO = checkQueryParam(transParamVO);
		// 如果返回错误或者返回成功，对象为空，直接返回
		if (resultBO.isError() || (resultBO.isOK() && ObjectUtil.isBlank(resultBO.getData()))) {
			return resultBO;
		}
		PageBO pageBO = new PageBO(transParamVO.getShowCount(), transParamVO.getTotalNum(), transParamVO.getCurrentPage());
		List<TransUserLogBO> list = transUserLogMapper.findUserTransListByPage(transParamVO);
		// 4、将排序后的交易流水分页输出
		List<TransUserLogBO> resultList = rearrangeTransList(transParamVO, list);
		pageBO.setDataList(resultList);
		return ResultBO.ok(pageBO);
	}

	/**
	 * 方法说明: 交易记录按月份分组
	 * 
	 * @auth: xiongJinGang
	 * @param list
	 * @time: 2017年4月26日 下午4:11:00
	 * @return: List<AppTransUserVO>
	 */
	private List<AppTransUserVO> getAppTransUserList(List<TransUserLogBO> list) {
		List<AppTransUserVO> appList = new ArrayList<AppTransUserVO>();
		if (!ObjectUtil.isBlank(list)) {
			String nowMonth = DateUtil.getNow(DateUtil.FORMAT_CHINESE_YYYYMM);// 当月
			String today = DateUtil.getNow(DateUtil.DATE_FORMAT_NO_LINE);// 当天
			String yesteday = DateUtil.getBeforeOrAfterDate(-1, DateUtil.DATE_FORMAT_NO_LINE);// 明天
			String beforeYesteday = DateUtil.getBeforeOrAfterDate(-2, DateUtil.DATE_FORMAT_NO_LINE);// 后天

			Map<String, List<TransUserLogBO>> map = new LinkedHashMap<String, List<TransUserLogBO>>();
			for (TransUserLogBO transUserBO : list) {
				// 添加符号
				UserTransMoneyFlowEnum userTransMoneyFlowEnum = PayConstants.UserTransMoneyFlowEnum.getTransTypeByKey(transUserBO.getTransType());
				String symbol = userTransMoneyFlowEnum.getType().equals(PayConstants.MoneyFlowEnum.IN.getKey()) ? "+" : "-";
				transUserBO.setShowAmount(symbol + String.format("%.2f", transUserBO.getTransAmount()));// 保留小数点后2位

				// 交易状态
				UserTransStatusEnum transStatusEnum = PayConstants.UserTransStatusEnum.getEnum(transUserBO.getTransStatus());
				if (null != transStatusEnum) {
					transUserBO.setTransStatusName(transStatusEnum.getValue());
				}
				// 购彩和返奖，要加2个参数给移动端
				if (transUserBO.getTransType().equals(PayConstants.TransTypeEnum.LOTTERY.getKey()) || transUserBO.getTransType().equals(PayConstants.TransTypeEnum.RETURN_AWARD.getKey())
						|| transUserBO.getTransType().equals(PayConstants.TransTypeEnum.REFUND.getKey())) {
					if (!ObjectUtil.isBlank(transUserBO.getOrderCode())) {
						// 购彩并且是订单号不为空，并且以合买订单头开头，查询合买订单详情
						if (transUserBO.getOrderCode().startsWith(OrderEnum.NumberCode.ORDER_GROUP_BUYCODE.getCode())) {
							OrderGroupContentBO orderGroupContent = orderGroupContentService.findOrderGroupContentByBuyCodeFromCache(transUserBO.getOrderCode());
							if (!ObjectUtil.isBlank(orderGroupContent)) {
								transUserBO.setOrderCode(orderGroupContent.getOrderCode());
							}
						}

						PayOrderBaseInfoVO payOrderBaseInfoVO = payOrderUpdateService.findOrderBoth(transUserBO.getOrderCode());
						if (!ObjectUtil.isBlank(payOrderBaseInfoVO)) {
							transUserBO.setBuyType(payOrderBaseInfoVO.getBuyType());
							transUserBO.setLotteryCode(payOrderBaseInfoVO.getLotteryCode());
							if (!ObjectUtil.isBlank(payOrderBaseInfoVO.getOrderAddCode())) {
								transUserBO.setOrderCode(payOrderBaseInfoVO.getOrderAddCode());
								transUserBO.setBuyType(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey());
							}
						}
					}
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
					List<TransUserLogBO> transList = map.get(yearMonth);
					transList.add(transUserBO);
					map.put(yearMonth, transList);
				} else {
					List<TransUserLogBO> appTransList = new ArrayList<TransUserLogBO>();
					appTransList.add(transUserBO);
					map.put(yearMonth, appTransList);
				}
			}
			if (map.size() > 0) {
				AppTransUserVO appTransUserVO = null;

				for (Map.Entry<String, List<TransUserLogBO>> transMap : map.entrySet()) {
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

	/**  
	* 方法说明: 重新整理交易流水
	* @auth: xiongJinGang
	* @param oldShowCount
	* @param list
	* @time: 2017年11月9日 上午10:22:57
	* @return: List<TransUserLogBO> 
	*/
	private List<TransUserLogBO> rearrangeTransList(TransParamVO transParamVO, List<TransUserLogBO> list) {
		List<TransUserLogBO> resultList = new ArrayList<TransUserLogBO>();
		for (TransUserLogBO transUserBO : list) {
			// 购彩并且是订单号不为空，并且以合买订单头开头，查询合买订单详情
			if (!ObjectUtil.isBlank(transUserBO.getOrderCode()) && transUserBO.getOrderCode().startsWith(OrderEnum.NumberCode.ORDER_GROUP_BUYCODE.getCode())) {
				OrderGroupContentBO orderGroupContent = orderGroupContentService.findOrderGroupContentByBuyCodeFromCache(transUserBO.getOrderCode());
				if (!ObjectUtil.isBlank(orderGroupContent)) {
					transUserBO.setOrderCode(orderGroupContent.getOrderCode());
				}
			}
			// 添加符号
			UserTransMoneyFlowEnum userTransMoneyFlowEnum = PayConstants.UserTransMoneyFlowEnum.getTransTypeByKey(transUserBO.getTransType());
			String symbol = userTransMoneyFlowEnum.getType().equals(PayConstants.MoneyFlowEnum.IN.getKey()) ? "+" : "-";
			transUserBO.setShowAmount(symbol + String.format("%.2f", transUserBO.getTransAmount()));// 保留小数点后2位

			// 交易状态
			UserTransStatusEnum transStatusEnum = PayConstants.UserTransStatusEnum.getEnum(transUserBO.getTransStatus());
			if (null != transStatusEnum) {
				transUserBO.setTransStatusName(transStatusEnum.getValue());
			}
			resultList.add(transUserBO);
		}
		return resultList;
	}

	@Override
	public int updateTransUserByBatch(List<TransUserPO> list) throws Exception {
		return transUserLogMapper.updateTransUserByBatch(list);
	}

	@Override
	public int updateTransUser(TransUserPO transUserPO) throws Exception {
		return transUserLogMapper.updateTransUserInfo(transUserPO);
	}

}
