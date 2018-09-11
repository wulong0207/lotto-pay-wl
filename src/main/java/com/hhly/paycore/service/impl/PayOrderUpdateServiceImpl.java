package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.dao.LotteryTypeDaoMapper;
import com.hhly.paycore.dao.PayOrderAddedMapper;
import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.po.PayOrderUpdatePO;
import com.hhly.paycore.service.PayOrderUpdateService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.lotto.base.lottery.bo.LotteryBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.bo.PayOrderAddBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;
import com.hhly.utils.RedisUtil;

@Service("payOrderUpdateService")
public class PayOrderUpdateServiceImpl implements PayOrderUpdateService {
	private static Logger logger = Logger.getLogger(PayOrderUpdateServiceImpl.class);
	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Resource
	private PayOrderAddedMapper payOrderAddedMapper;
	@Resource
	private LotteryTypeDaoMapper lotteryTypeDaoMapper;
	@Resource
	private RedisUtil redisUtil;

	@Override
	public int dealOrderPayStatus(PayOrderUpdatePO payOrderUpdatePO, PayOrderBaseInfoVO payOrderBaseInfoVO) throws Exception {
		Integer buyType = PayConstants.BuyTypeEnum.PURCHASING.getKey();// 默认一个代购
		if (!ObjectUtil.isBlank(payOrderBaseInfoVO)) {
			buyType = payOrderBaseInfoVO.getBuyType();
		}
		int num = 0;
		// 如果是追号计划，查追号计划表
		if (buyType.equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
			num = payOrderAddedMapper.updateOrderPayStatus(payOrderUpdatePO);
		} else {
			num = payOrderUpdateMapper.updateOrderPayStatus(payOrderUpdatePO);
		}
		if (num <= 0) {
			logger.info("更新订单【" + payOrderUpdatePO.getOrderCode() + "】支付状态失败");
			throw new Exception("更新订单支付状态失败");
		}
		return num;
	}

	@Override
	public int updateOrderBatch(List<PayOrderBaseInfoVO> orderTotalList, PayNotifyResultVO payNotifyResult, TransRechargeBO transRecharge) throws Exception {
		List<PayOrderUpdatePO> addedCodeList = new ArrayList<PayOrderUpdatePO>();
		List<PayOrderUpdatePO> orderCodeList = new ArrayList<PayOrderUpdatePO>();
		PayOrderUpdatePO payOrderUpdatePO = null;
		PayStatusEnum payStatusEnum = payNotifyResult.getStatus();
		String tradeTime = payNotifyResult.getTradeTime();
		Date buyTime = DateUtil.getNowDate();// 当前时间
		if (!ObjectUtil.isBlank(tradeTime)) {
			try {
				buyTime = DateUtil.convertStrToDate(tradeTime, DateUtil.DEFAULT_FORMAT);
			} catch (Exception e) {
				buyTime = DateUtil.getNowDate();// 当前时间
			}
		}

		// 筛选出是代购订单还是追号计划订单
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : orderTotalList) {
			payOrderUpdatePO = new PayOrderUpdatePO(payOrderBaseInfoVO);
			payOrderUpdatePO.setActivitySource(payOrderBaseInfoVO.getActivityCode());// 活动表的活动ID
			payOrderUpdatePO.setBuyTime(buyTime);// 购买时间
			// payOrderUpdatePO.setAddStatus(payOrderBaseInfoVO.get); // 追号状态
			payOrderUpdatePO.setOrderStatus(payOrderBaseInfoVO.getOrderStatus()); // 订单状态

			// 红包编号不为空，设置红包编号（只有订单为单个支付时才可能会存在红包编号）
			if (!ObjectUtil.isBlank(transRecharge.getRedCode())) {
				payOrderUpdatePO.setRedCodeUsed(transRecharge.getRedCode());// 开奖后生成的优惠券中的红包编号ID(系统自动发放的红包编号ID)
			}
			if (payStatusEnum.equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS)) {
				payOrderUpdatePO.setPayStatus(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey());// 支付成功
			} else {
				payOrderUpdatePO.setPayStatus(PayConstants.PayStatusEnum.PAYMENT_FAILURE.getKey());// 支付失败
			}
			if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				// 追号计划订单编号列表
				addedCodeList.add(payOrderUpdatePO);
			} else {
				// 查询订单编号列表
				orderCodeList.add(payOrderUpdatePO);
			}
			logger.info("更新订单【" + payOrderBaseInfoVO.getOrderCode() + "】的支付时间：" + buyTime + "，支付状态：" + payStatusEnum.getValue() + "，订单状态：" + payOrderBaseInfoVO.getOrderStatus());
		}
		int num = 0;
		// 如果是追号计划，查追号计划表
		if (!ObjectUtil.isBlank(addedCodeList)) {
			num = payOrderAddedMapper.updateOrderBatch(addedCodeList);
			logger.info("批量更新追号计划订单状态返回：" + num);
		}

		if (!ObjectUtil.isBlank(orderCodeList)) {
			num = payOrderUpdateMapper.updateOrderBatch(orderCodeList);
			logger.info("批量更新代购订单状态返回：" + num);
		}
		return num;
	}

	@Override
	public int updateOrderPayingBatch(List<OrderBaseInfoBO> orderList) throws Exception {
		List<PayOrderUpdatePO> addedCodeList = new ArrayList<PayOrderUpdatePO>();
		List<PayOrderUpdatePO> orderCodeList = new ArrayList<PayOrderUpdatePO>();
		PayOrderUpdatePO payOrderUpdatePO = null;

		// 筛选出是代购订单还是追号计划订单
		for (OrderBaseInfoBO orderBaseInfoBO : orderList) {
			payOrderUpdatePO = new PayOrderUpdatePO();
			payOrderUpdatePO.setOrderCode(orderBaseInfoBO.getOrderCode());
			payOrderUpdatePO.setPayStatus(PayConstants.PayStatusEnum.BEING_PAID.getKey());// 支付中
			if (orderBaseInfoBO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				// 追号计划订单编号列表
				addedCodeList.add(payOrderUpdatePO);
			} else {
				// 查询订单编号列表
				orderCodeList.add(payOrderUpdatePO);
			}
		}
		int num = 0;
		// 如果是追号计划，查追号计划表
		if (!ObjectUtil.isBlank(addedCodeList)) {
			num = payOrderAddedMapper.updateOrderBatch(addedCodeList);
		}

		if (!ObjectUtil.isBlank(orderCodeList)) {
			num = payOrderUpdateMapper.updateOrderBatch(orderCodeList);
		}
		return num;
	}

	@Override
	public PayOrderBaseInfoVO findOrderInfo(String orderCode) {
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		try {
			OrderInfoBO orderInfoBO = payOrderUpdateMapper.getOrderInfo(orderCode);
			if (!ObjectUtil.isBlank(orderInfoBO)) {
				payOrderBaseInfoVO = new PayOrderBaseInfoVO(orderInfoBO);
			}
		} catch (Exception e) {
			logger.error("获取订单【" + orderCode + "】详情异常。" + e.getMessage());
		}
		return payOrderBaseInfoVO;
	}

	/**  
	* 方法说明: 根据订单号查找追号计划信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年4月26日 上午10:40:40
	* @return: PayOrderBaseInfoVO 
	*/
	public PayOrderBaseInfoVO findOrderAdded(String orderCode) throws Exception {
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		PayOrderAddBO payOrderAddBO = payOrderAddedMapper.getOrderInfo(orderCode);
		if (!ObjectUtil.isBlank(payOrderAddBO)) {
			LotteryBO lotteryBO = getLotteryInfo(String.valueOf(payOrderAddBO.getLotteryCode()));
			payOrderBaseInfoVO = new PayOrderBaseInfoVO(payOrderAddBO, lotteryBO.getLotteryName());
			payOrderBaseInfoVO.setLotteryIssue(payOrderAddBO.getIssueCode());
			payOrderBaseInfoVO.setOrderAmount(payOrderAddBO.getAddAmount());
			payOrderBaseInfoVO.setOrderCode(orderCode);
		}
		return payOrderBaseInfoVO;
	}

	/**  
	* 方法说明: 批量查询订单信息
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年5月11日 上午11:22:07
	* @return: List<PayOrderBaseInfoVO> 
	*/
	private List<PayOrderBaseInfoVO> findOrderInfoList(List<String> codeList) throws Exception {
		List<OrderInfoBO> list = payOrderUpdateMapper.getOrderList(codeList);
		List<PayOrderBaseInfoVO> payOrderList = new ArrayList<PayOrderBaseInfoVO>();
		if (!ObjectUtil.isBlank(list)) {
			PayOrderBaseInfoVO payOrderBaseInfoVO = null;
			for (OrderInfoBO orderInfoBO : list) {
				// 这里不用copyProperties，有性能问题
				payOrderBaseInfoVO = new PayOrderBaseInfoVO(orderInfoBO);
				payOrderList.add(payOrderBaseInfoVO);
			}
		}
		return payOrderList;
	}

	/**  
	* 方法说明: 批量查询追号计划表
	* @auth: xiongJinGang
	* @param orderCode
	* @time: 2017年5月11日 上午11:29:22
	* @return: List<PayOrderBaseInfoVO> 
	*/
	private List<PayOrderBaseInfoVO> findOrderAddedList(List<String> codeList) {
		List<PayOrderAddBO> list = payOrderAddedMapper.getOrderList(codeList);
		List<PayOrderBaseInfoVO> payOrderList = new ArrayList<PayOrderBaseInfoVO>();
		if (!ObjectUtil.isBlank(list)) {
			PayOrderBaseInfoVO payOrderBaseInfoVO = null;
			for (PayOrderAddBO payOrderAddBO : list) {
				LotteryBO lotteryBO = getLotteryInfo(String.valueOf(payOrderAddBO.getLotteryCode()));
				payOrderBaseInfoVO = new PayOrderBaseInfoVO(payOrderAddBO, lotteryBO.getLotteryName());
				payOrderList.add(payOrderBaseInfoVO);
			}
		}
		return payOrderList;
	}

	@Override
	public ResultBO<?> findOrderAndValidate(String orderCode, Integer buyType) throws Exception {
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		// 如果是追号计划，查追号计划表
		if (buyType.equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
			payOrderBaseInfoVO = this.findOrderAdded(orderCode);
		} else {
			payOrderBaseInfoVO = this.findOrderInfo(orderCode);
		}
		if (ObjectUtil.isBlank(payOrderBaseInfoVO)) {
			return ResultBO.err(MessageCodeConstants.TRANS_ORDER_CODE_IS_ERROR_SERVICE);
		}
		payOrderBaseInfoVO.setBuyType(buyType);// 设置购买类型
		if (ObjectUtil.isBlank(payOrderBaseInfoVO.getPayStatus())) {
			return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
		}
		if (payOrderBaseInfoVO.getPayStatus().equals(PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey())) {
			return ResultBO.err(MessageCodeConstants.ORDER_HAD_PAY);
		}
		// 判断订单是否可以支付，如果支付状态不是等待支付，返回错误
		if (ObjectUtil.isBlank(payOrderBaseInfoVO.getPayStatus()) || !Integer.valueOf(PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey()).equals(payOrderBaseInfoVO.getPayStatus().shortValue())
				|| !payOrderBaseInfoVO.getPayStatus().equals(Integer.valueOf(PayConstants.PayStatusEnum.BEING_PAID.getKey() + ""))) {
			return ResultBO.err(MessageCodeConstants.PAY_STATUS_ERROR_SERVICE);
		}
		return ResultBO.ok(payOrderBaseInfoVO);
	}

	@Override
	public PayOrderBaseInfoVO findOrderBoth(String orderCode) {
		PayOrderBaseInfoVO payOrderBaseInfoVO = null;
		try {
			// 如果是追号计划，查追号计划表
			payOrderBaseInfoVO = this.findOrderAdded(orderCode);
			if (ObjectUtil.isBlank(payOrderBaseInfoVO)) {
				payOrderBaseInfoVO = this.findOrderInfo(orderCode);
			}
		} catch (Exception e) {
			logger.error("获取订单【" + orderCode + "】详情异常！" + e.getMessage());
		}
		return payOrderBaseInfoVO;
	}

	@Override
	public ResultBO<?> findOrderAndValidate(List<PayOrderBaseInfoVO> list) throws Exception {
		List<String> addedCodeList = new ArrayList<String>();
		List<String> orderCodeList = new ArrayList<String>();
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : list) {
			if (payOrderBaseInfoVO.getBuyType().equals(PayConstants.BuyTypeEnum.TRACKING_PLAN.getKey())) {
				// 追号计划订单编号列表
				addedCodeList.add(payOrderBaseInfoVO.getOrderCode());
			} else {
				// 查询订单编号列表
				orderCodeList.add(payOrderBaseInfoVO.getOrderCode());
			}
		}
		List<PayOrderBaseInfoVO> orderTotalList = new ArrayList<PayOrderBaseInfoVO>();
		// 如果是追号计划，查追号计划表
		if (!ObjectUtil.isBlank(addedCodeList)) {
			List<PayOrderBaseInfoVO> addOrderList = this.findOrderAddedList(addedCodeList);
			if (!ObjectUtil.isBlank(addOrderList)) {
				orderTotalList.addAll(addOrderList);
			}
		}

		if (!ObjectUtil.isBlank(orderCodeList)) {
			List<PayOrderBaseInfoVO> orderInfoList = this.findOrderInfoList(orderCodeList);
			if (!ObjectUtil.isBlank(orderInfoList)) {
				orderTotalList.addAll(orderInfoList);
			}
		}

		if (ObjectUtil.isBlank(orderTotalList)) {
			logger.error("批量查询订单列表返回空，追号计划订单编号：" + JSON.toJSONString(addedCodeList) + "，订单编号：" + JSON.toJSONString(orderCodeList));
			return ResultBO.err(MessageCodeConstants.ORDER_IS_NOT_EXIST);
		}
		if (orderTotalList.size() != list.size()) {
			return ResultBO.err(MessageCodeConstants.ORDER_NOT_EXIST_OR_INVALILD);
		}
		return ResultBO.ok(orderTotalList);
	}

	/**  
	* 方法说明: 获取彩票信息
	* @auth: xiongJinGang
	* @param lotteryCodeStr
	* @time: 2017年4月27日 下午6:09:12
	* @return: LotteryBO 
	*/
	private LotteryBO getLotteryInfo(String lotteryCodeStr) {
		// 缓存中取
		// Map<Integer, LotteryBO> lotteryInfoMap = redisUtil.getObj(CacheConstants.LOTTERY_TYPE_CHILD_MAP, new HashMap<Integer, LotteryBO>());
		Integer lotteryCode = Integer.valueOf(lotteryCodeStr.substring(Constants.NUM_0, Constants.NUM_3));
		// LotteryBO lotteryBO = null;
		// if (!ObjectUtil.isBlank(lotteryInfoMap)) {
		// // 从缓存中获取
		// lotteryBO = lotteryInfoMap.get(lotteryCode);
		// } else {
		// 从数据库中读取
		LotteryBO lotteryBO = getLotteryInfoFromDb(lotteryCode);
		// }
		return lotteryBO;
	}

	private LotteryBO getLotteryInfoFromDb(Integer lotteryCode) {
		return lotteryTypeDaoMapper.findSingleFront(lotteryCode);
	}

	@Override
	public void subNoPayOrderNum(List<PayOrderBaseInfoVO> list) {
		for (PayOrderBaseInfoVO payOrderBaseInfoVO : list) {
			if (!ObjectUtil.isBlank(payOrderBaseInfoVO.getLotteryCode())) {
				String lottoCode = String.valueOf(payOrderBaseInfoVO.getLotteryCode());
				if (lottoCode.length() > 3) {
					lottoCode = lottoCode.substring(0, 3);
				}
				redisUtil.incr(CacheConstants.getNoPayOrderCacheKey(payOrderBaseInfoVO.getUserId(), Integer.parseInt(lottoCode)), -1);
			}
		}
	}
}
