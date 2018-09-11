package com.hhly.paycore.service.impl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.hhly.paycore.common.OrderGroupUtil;
import com.hhly.paycore.dao.OrderGroupContentMapper;
import com.hhly.paycore.dao.OrderGroupMapper;
import com.hhly.paycore.dao.PayOrderUpdateMapper;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.OrderGroupService;
import com.hhly.paycore.service.PayCoreService;
import com.hhly.paycore.service.TransUserService;
import com.hhly.paycore.service.UserWalletService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.CancellationConstants;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.OrderGroupConstants;
import com.hhly.skeleton.base.constants.OrderGroupConstants.OrderGroupBuyTypeEnum;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayResultEnum;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.lotto.base.group.bo.OrderDetailGroupInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderBaseInfoBO;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.bo.OrderGroupBO;
import com.hhly.skeleton.pay.bo.OrderGroupContentBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransUserBO;
import com.hhly.skeleton.pay.vo.OrderGroupVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayParamVO;
import com.hhly.skeleton.pay.vo.PayReqResultVO;
import com.hhly.skeleton.pay.vo.ToPayEndTimeVO;
import com.hhly.skeleton.user.bo.UserWalletBO;
import com.hhly.utils.RedisUtil;

/**
 * @desc 合买
 * @author xiongJinGang
 * @date 2018年4月28日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("orderGroupService")
public class OrderGroupServiceImpl implements OrderGroupService {
	private static final Logger logger = LoggerFactory.getLogger(OrderGroupServiceImpl.class);

	@Resource
	private OrderGroupMapper orderGroupMapper;
	@Resource
	private OrderGroupContentMapper orderGroupContentMapper;
	@Resource
	private PayOrderUpdateMapper payOrderUpdateMapper;
	@Resource
	private MessageProvider messageProvider;// 发拆票MQ
	@Resource
	private PayCoreService payCoreService;
	@Resource
	private UserWalletService userWalletService;
	@Resource
	private TransUserService transUserService;
	@Resource
	private RedisUtil redisUtil;

	@Value("${lotto.order.group.url}")
	private String orderGroupUrl;// 合买订单URL

	@Override
	public OrderGroupBO findOrderGroupByOrderCode(String orderCode) {
		return orderGroupMapper.getOrderGroupByOrderCode(orderCode);
	}

	@Override
	public boolean isGreaterThan90(OrderGroupBO orderGroupBO) {
		// 合买总进度=保底比例+合买进度比例
		Double totalProgress = MathUtil.add(orderGroupBO.getGuaranteeRatio(), orderGroupBO.getProgress());
		if (MathUtil.compareTo(OrderGroupUtil.BUY_TOGETHER_PROGRESS, totalProgress) > 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isGreaterThan90(OrderInfoBO orderInfo, OrderGroupBO orderGroupBO) {
		// （进度+保底）/订单金额
		Double totalProgress = MathUtil.add(orderGroupBO.getProgressAmount(), orderGroupBO.getGuaranteeAmount());// 进度金额加保底金额
		Double progress = MathUtil.div(totalProgress, orderInfo.getOrderAmount());
		if (MathUtil.compareTo(OrderGroupUtil.BUY_TOGETHER_PERCENT_PROGRESS, progress) > 0) {
			return false;
		}
		return true;
	}

	@Override
	public ResultBO<?> updateBuyTogetherOrder(String orderCode, TransRechargeBO transRecharge, PayNotifyResultVO payNotifyResult) throws Exception {
		if (PayConstants.PayStatusEnum.PAYMENT_SUCCESS.equals(payNotifyResult.getStatus())) {
			// 获取订单信息
			OrderInfoBO orderInfo = payOrderUpdateMapper.getOrderInfo(orderCode);
			if (ObjectUtil.isBlank(orderInfo)) {
				logger.info("订单【" + orderCode + "】详情为空");
				return ResultBO.err(MessageCodeConstants.ORDER_GROUP_DETAIL_IS_NULL);
			}

			// 订单的支付状态等于待支付，表示是发起合买（最好判断支付金额与合买订单的保底金额+认购金额的和是否一致）
			Boolean firstPay = false;// 合买订单是否为第一次支付
			OrderGroupBO orderGroup = null;

			Double useBalance = ObjectUtil.isBlank(transRecharge.getGroupAmount()) ? 0d : transRecharge.getGroupAmount();// 使用余额金额
			Double realPayAmount = MathUtil.add(useBalance, payNotifyResult.getOrderAmt());// 实际支付的金额=使用余额金额+现金支付金额
			if (PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey().equals(orderInfo.getPayStatus()) || PayConstants.PayStatusEnum.BEING_PAID.getKey().equals(orderInfo.getPayStatus())) {
				firstPay = true;// 支付状态为待支付，表示是第一次发起支付
				orderGroup = orderGroupMapper.getOrderGroupByOrderCode(orderCode);
				Double needPayAmount = MathUtil.add(orderGroup.getGuaranteeAmount(), orderGroup.getMinBuyAmount());
				if (MathUtil.compareTo(realPayAmount, needPayAmount) != 0) {
					logger.info("订单【" + orderCode + "】实际支付的金额" + realPayAmount + "与发起合买金额" + needPayAmount + "不符");
				}
				realPayAmount = orderGroup.getMinBuyAmount();// 首次支付，只存最低认购金额
			}
			// 先验证一下用户钱包中的金额够不够扣除本次交易
			/*	UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(transRecharge.getUserId());
				if (MathUtil.compareTo(userWalletBO.getTotalCashBalance(), realPayAmount) < 0) {
					logger.info("用户【" + transRecharge.getUserId() + "】账户总余额：" + userWalletBO.getTotalCashBalance() + "不够扣除跟单【" + orderCode + "】金额：" + realPayAmount);
					return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
				}*/

			// 1、调用合买订单接口，完成下单
			OrderGroupContentBO orderGroupContentBO = null;
			// 需要参与合买的金额
			Double needBuyTogetherAmount = 0d;

			// 先验证用户合买支付有没有参与合买，参与过不再调用认购接口
			String togetherRecordKey = "pay_order_group_content_" + transRecharge.getTransRechargeCode();// 充值编号的key
			String json = redisUtil.getString(togetherRecordKey);
			if (StringUtils.isBlank(json)) {
				json = callOrderService(orderCode, realPayAmount, transRecharge.getUserId(), firstPay);
				redisUtil.addString(togetherRecordKey, json, CacheConstants.TWELVE_HOURS);
			} else {
				logger.info(transRecharge.getTransRechargeCode() + "已经参与过合买，不再调用合买接口合买，直接用之前的合买记录进行流水添加");
			}
			if (!ObjectUtil.isBlank(json)) {
				// 2、判断接口返回的结果
				ResultBO<?> resultBO = JSON.parseObject(json, ResultBO.class);
				// 3、返回成功
				if (resultBO.isOK()) {
					JSONObject jsonObject = (JSONObject) resultBO.getData();
					orderGroupContentBO = jsonObject.toJavaObject(OrderGroupContentBO.class);
					needBuyTogetherAmount = orderGroupContentBO.getBuyAmount();
					// 如果是首次支付，调用接口传过去的金额没有包含保底金额；调用接口后，返回的金额也是不包含保底金额的，需要加上保底金额
					if (firstPay) {
						needBuyTogetherAmount = MathUtil.add(needBuyTogetherAmount, orderGroup.getGuaranteeAmount());
					}
				} else {
					logger.info("订单【" + orderCode + "】调用接口记录合买失败，充值金额转成充值金额");
				}
			}

			ResultBO<?> resultBO = payCoreService.modifyBuyTogetherToRecharge(transRecharge, payNotifyResult, orderInfo, needBuyTogetherAmount, orderGroup, orderGroupContentBO);
			sendOrderGroupMQ(orderCode, transRecharge.getTransRechargeCode(), orderInfo, firstPay, resultBO);
			return ResultBO.ok();
		} else {
			logger.info("订单【" + orderCode + "】当前交易状态【" + payNotifyResult.getStatus() + "】不是支付成功，不做处理");
		}
		return ResultBO.err();
	}

	/**  
	* 方法说明: 调用订单服务接口
	* @auth: xiongJinGang
	* @param orderCode
	* @param buyAmount
	* @param userId
	* @throws IOException
	* @throws URISyntaxException
	* @time: 2018年5月4日 下午5:50:47
	* @return: String 
	*/
	public String callOrderService(String orderCode, Double buyAmount, Integer userId, boolean firstPay) {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("orderCode", orderCode);
		paramMap.put("buyAmount", String.valueOf(buyAmount));
		paramMap.put("userId", String.valueOf(userId));
		if (firstPay) {
			paramMap.put("flag", "1");
		}
		String json = null;
		try {
			String reqJson = JSONObject.toJSONString(paramMap);
			logger.info("订单【" + orderCode + "】调用合买订单服务接口请求参数：" + reqJson);
			json = HttpUtil.doPostJson(orderGroupUrl + "orderGroup/addOrderGroupContent", reqJson, false);
			logger.info("订单【" + orderCode + "】调用合买订单服务接口返回：" + json);
		} catch (Exception e) {
			logger.error("订单【" + orderCode + "】调用合买订单服务接口异常", e);
		}
		return json;
	}

	@Override
	public ResultBO<?> updateBuyTogetherOrderByLocal(String orderCode, PayParamVO payParam, OrderBaseInfoBO orderBaseInfoBO, ToPayEndTimeVO toPayEndTimeVO) throws Exception {
		// 获取订单信息
		OrderInfoBO orderInfo = payOrderUpdateMapper.getOrderInfo(orderCode);
		if (ObjectUtil.isBlank(orderInfo)) {
			logger.info("订单【" + orderCode + "】详情为空");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_DETAIL_IS_NULL);
		}
		// 订单的支付状态等于待支付，表示是发起合买（最好判断支付金额与合买订单的保底金额+认购金额的和是否一致）
		Boolean firstPay = false;// 合买订单是否为第一次支付
		OrderGroupBO orderGroup = null;
		Double realPayAmount = payParam.getUseBalance();
		if (PayConstants.PayStatusEnum.WAITTING_PAYMENT.getKey().equals(orderInfo.getPayStatus()) || PayConstants.PayStatusEnum.BEING_PAID.getKey().equals(orderInfo.getPayStatus())) {
			firstPay = true;// 支付状态为待支付，表示是第一次发起支付
			OrderDetailGroupInfoBO orderDetailGroupInfoBO = orderBaseInfoBO.getOrderDetailGroupInfoBO();
			// realPayAmount = MathUtil.add(orderDetailGroupInfoBO.getGuaranteeAmount(), orderDetailGroupInfoBO.getGroupAmount());
			realPayAmount = orderDetailGroupInfoBO.getGroupAmount();// 首次支付，只存最低认购金额
			orderGroup = orderGroupMapper.getOrderGroupByOrderCode(orderCode);
		}

		// 先验证一下用户钱包中的金额够不够扣除本次交易
		UserWalletBO userWalletBO = userWalletService.findUserWalletByUserId(payParam.getUserId());
		if (MathUtil.compareTo(userWalletBO.getTotalCashBalance(), realPayAmount) < 0) {
			logger.info("用户【" + payParam.getUserId() + "】账户总余额：" + userWalletBO.getTotalCashBalance() + "不够扣除跟单【" + orderCode + "】金额：" + realPayAmount);
			return ResultBO.err(MessageCodeConstants.PAY_USER_WALLET_ERROR_SERVICE);
		}

		// 1、调用合买订单接口，完成下单
		// 根据订单号和用户ID获取最后一笔交易的记录
		OrderGroupContentBO orderGroupContentBO = null;
		// 需要参与合买的金额
		Double needBuyTogetherAmount = 0d;
		String json = callOrderService(orderCode, realPayAmount, payParam.getUserId(), firstPay);
		if (!ObjectUtil.isBlank(json)) {
			// 2、判断接口返回的结果
			ResultBO<?> resultBO = JSON.parseObject(json, ResultBO.class);
			// 3、返回成功，判断合买进度是否大于90%
			if (resultBO.isOK()) {
				JSONObject jsonObject = (JSONObject) resultBO.getData();
				orderGroupContentBO = jsonObject.toJavaObject(OrderGroupContentBO.class);
				// orderGroupContentBO = orderGroupContentMapper.getUserLastOrderGroupContentByOrderCode(orderCode, transRecharge.getUserId());
				needBuyTogetherAmount = orderGroupContentBO.getBuyAmount();
				// 如果是首次支付，调用接口传过去的金额没有包含保底金额；调用接口后，返回的金额也是不包含保底金额的，需要加上保底金额
				if (firstPay) {
					needBuyTogetherAmount = MathUtil.add(needBuyTogetherAmount, orderGroup.getGuaranteeAmount());
				}
			} else {
				logger.info("订单【" + orderCode + "】调用接口记录合买失败，充值金额转成充值金额");
			}
		}

		// 如果需要参与合买的金额小于等于0，返回错误
		if (MathUtil.compareTo(needBuyTogetherAmount, 0d) <= 0) {
			logger.info("订单【" + orderCode + "】参与合买失败，不扣减账户余额");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_BUY_FAIL);
		}
		ResultBO<?> resultBO = payCoreService.modifyBuyTogetherForLocal(payParam, orderInfo, toPayEndTimeVO, needBuyTogetherAmount, orderGroup, orderGroupContentBO);
		if (resultBO.isOK()) {
			sendOrderGroupMQ(orderCode, null, orderInfo, firstPay, resultBO);
			// 添加订单支付结果信息到缓存，余额支付成功
			redisUtil.addString(CacheConstants.P_CORE_PAY_STATUS_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), PayResultEnum.BALANCE_SUCCESS.getKey(), CacheConstants.ONE_HOURS);
			redisUtil.addObj(CacheConstants.P_CORE_PAY_STATUS_OBJ_RESULT + payParam.getUserId() + "_" + payParam.getTransCode(), resultBO.getData(), CacheConstants.ONE_HOURS);
			PayReqResultVO payReqResult = new PayReqResultVO();
			payReqResult.setType(PayConstants.PayReqResultEnum.SHOW.getKey());
			payReqResult.setTransCode(payParam.getTransCode());// 交易码
			return ResultBO.ok(payReqResult);
		}
		return resultBO;
	}

	/**  
	* 方法说明: 发送合买订单MQ
	* @auth: xiongJinGang
	* @param orderCode
	* @param transRecharge
	* @param orderInfo
	* @param firstPay
	* @param resultBO
	* @time: 2018年5月3日 下午4:51:48
	* @return: void 
	*/
	public void sendOrderGroupMQ(String orderCode, String transRechargeCode, OrderInfoBO orderInfo, Boolean firstPay, ResultBO<?> resultBO) {
		if (resultBO.isOK()) {
			OrderGroupBO orderGroupBO = findOrderGroupByOrderCode(orderCode);
			// 大于90%，并且订单状态为待拆票，发送拆票MQ
			if (isGreaterThan90(orderInfo, orderGroupBO) && orderInfo.getOrderStatus().equals(CancellationConstants.OrderStatusEnum.PENDINGTICKET.getKey())) {
				logger.info("订单【" + orderCode + "】，充值交易号【" + transRechargeCode + "】达到合买的90%，开始发送拆单消息");
				// 1、发出票MQ
				messageProvider.sendMessage(Constants.QUEUE_NAME_FOR_ORDER, orderCode + "#1");
				// 2、发等待出票MQ
				messageProvider.sendOrderFlowMessage(orderCode, null, Short.valueOf("2"), Short.valueOf("1"));
				// 3、合买订单，支付成功，未支付订单拿这个减一
				String lottoCode = String.valueOf(orderInfo.getLotteryCode());
				if (lottoCode.length() > 3) {
					lottoCode = lottoCode.substring(0, 3);
				}
				redisUtil.incr(CacheConstants.getNoPayOrderGroupCacheKey(orderInfo.getUserId(), Integer.parseInt(lottoCode)), -1);
			}
			// 如果是发起合买的支付，发“招募中”的MQ。
			if (firstPay) {
				messageProvider.sendOrderFlowMessage(orderCode, null, Short.valueOf("20"), Short.valueOf("1"));
			}
		}
	}

	@Override
	public ResultBO<?> updateOrderGroupByPlatform(OrderGroupVO orderGroupVO) throws Exception {
		// 根据订单号获取合买订单信息
		OrderGroupBO orderGroup = orderGroupMapper.getOrderGroupByOrderCode(orderGroupVO.getOrderCode());
		// 如果合买状态不是招募中，返回错误
		if (!orderGroup.getGrpbuyStatus().equals(OrderGroupConstants.OrderGroupStatusEnum.RECRUITMENT.getKey())) {
			logger.info("合买订单【" + orderGroupVO.getOrderCode() + "】状态：" + orderGroup.getGrpbuyStatus() + "不是招募中，不能操作");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_STATUS_ERROR);
		}
		// 获取订单信息
		OrderInfoBO orderInfo = payOrderUpdateMapper.getOrderInfo(orderGroupVO.getOrderCode());
		if (ObjectUtil.isBlank(orderInfo)) {
			logger.info("订单【" + orderGroupVO.getOrderCode() + "】详情为空");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_DETAIL_IS_NULL);
		}

		// 如果进度大于90%，才可以平台垫10%部分
		if (!isGreaterThan90(orderInfo, orderGroup)) {
			logger.info("合买订单【" + orderGroupVO.getOrderCode() + "】进度小于90%，平台不能垫付");
			return ResultBO.err(MessageCodeConstants.ORDER_GROUP_PAY_PROGRESS_ERROR);
		}

		OrderGroupContentBO orderGroupContentBO = orderGroupContentMapper.getOrderGroupContentByOrderCodeAndType(orderGroupVO.getOrderCode(), OrderGroupBuyTypeEnum.WEBSITE_GUARANTEE.getKey());
		if (!ObjectUtil.isBlank(orderGroupContentBO)) {
			String buyCode = orderGroupContentBO.getBuyCode();// 认购单号
			TransUserBO transUserBO = transUserService.getTransUserByType(buyCode, PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey(), PayConstants.TransTypeEnum.LOTTERY.getKey());
			if (!ObjectUtil.isBlank(transUserBO)) {
				logger.info("合买订单【" + orderGroupVO.getOrderCode() + "】已存在网站保底，不能重复添加");
				return ResultBO.err(MessageCodeConstants.ORDER_GROUP_WEB_GUARANTEE_REPEAT);
			}
		}

		return payCoreService.modifyPlatformGuarantee(orderInfo, orderGroupVO, orderGroup);
	}
}
