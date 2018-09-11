package com.hhly.paycore.remote.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.OperateCouponUtil;
import com.hhly.paycore.dao.OperateCouponMapper;
import com.hhly.paycore.dao.TransRedMapper;
import com.hhly.paycore.remote.service.IOperateCouponService;
import com.hhly.paycore.service.TransRedService;
import com.hhly.skeleton.base.bo.PagingBO;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OperateCouponEnum;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.page.IPageService;
import com.hhly.skeleton.base.page.ISimplePage;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.DicOperateCouponOptionBO;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.vo.OperateCouponQueryVO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @desc 【对外暴露hession接口】 彩金红包实   现类
 * @author xiongJinGang
 * @date 2017年3月29日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service("iOperateCouponService")
public class OperateCouponServiceImpl implements IOperateCouponService {
	private static final Logger logger = LoggerFactory.getLogger(OperateCouponServiceImpl.class);
	@Resource
	private OperateCouponMapper operateCouponMapper;
	@Resource
	private TransRedMapper transRedMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private TransRedService transRedService;
	@Autowired
	private IPageService pageService;

	@Override
	public ResultBO<?> findUserCouponByRedCode(String token, String redCode) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		OperateCouponBO operateCouponBO = null;
		try {
			operateCouponBO = operateCouponMapper.getUserCouponeByRedCode(userId, redCode);
		} catch (Exception e) {
			logger.error("获取用户ID【" + userInfo.getId() + "】的红包【" + redCode + "】详情异常。" + e.getMessage());
			return ResultBO.err(MessageCodeConstants.HESSIAN_ERROR_SYS);
		}
		if (ObjectUtil.isBlank(operateCouponBO)) {
			return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
		}
		return ResultBO.ok(operateCouponBO);
	}

	@Override
	public ResultBO<?> findUserCouponList(String token) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		List<OperateCouponBO> list = null;
		try {
			Integer userId = userInfo.getId();
			String redStatus = PayConstants.RedStatusEnum.NORMAL.getKey();// 只获取可以使用的红包
			list = operateCouponMapper.getUserCouponeList(userId, redStatus);
		} catch (Exception e) {
			logger.error("获取用户【" + userInfo.getMobile() + "】优惠券列表异常。" + e.getMessage());
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
		return ResultBO.ok(list);
	}

	@Override
	public ResultBO<?> findCurPlatformCoupon(OperateCouponQueryVO operateCoupon) {
		UserInfoBO userInfo = userUtil.getUserByToken(operateCoupon.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();

		ResultBO<?> resultBO = OperateCouponUtil.validateCouponQueryParam(operateCoupon);
		if (resultBO.isError()) {
			return resultBO;
		}
		// 获取用户可以使用的红包列表
		List<OperateCouponBO> list = null;
		OperateCouponVO operateCouponVO = new OperateCouponVO();
		String redType = operateCoupon.getRedType();// 红包类型(充值recharge、支付pay)
		try {
			operateCouponVO.setRedStatus(PayConstants.RedStatusEnum.NORMAL.getKey());
			// operateCouponVO.setLimitPlatform(redPlatformRelationEnum.getLimitPlatform());
			operateCouponVO.setUserId(userId);
			if (redType.equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getUseType())) {
				operateCouponVO.setRedType(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey());
			}
			list = operateCouponMapper.getUserCurPlatformCouponeList(operateCouponVO);
		} catch (Exception e) {
			logger.error("获取用户【" + userInfo.getMobile() + "】优惠券列表异常，请求参数【" + operateCouponVO.toString() + "】，异常：", e);
		}
		Collection<OperateCouponBO> couponList = new CopyOnWriteArrayList<OperateCouponBO>();
		// 不等于空，需要验证彩种是否可以用
		if (!ObjectUtil.isBlank(list)) {
			couponList.addAll(list);
			// 当前使用的编号不为空

			// 限制红包使用平台 RedLimitPlatformEnum
			for (OperateCouponBO operateCouponBO : couponList) {
				// 判断限制平台
				/*if (!ObjectUtil.isBlank(operateCouponBO.getLimitPlatform())) {
					// 验证优惠券在当前平台是否受限。1：主站Web专用；2：主站Wap专用；3：合作版Wap专用；4：客户端专用；5：API接口专用；6：其它平台专用；
					boolean validateFlag = OperateCouponUtil.validateLimitPlatform(operateCouponBO.getLimitPlatform(), operateCoupon.getPlatform());
					if (!validateFlag) {
						couponList.remove(operateCouponBO);
						continue;
					}
				}*/
				// 判断限制渠道
				if (!ObjectUtil.isBlank(operateCouponBO.getChannelId())) {
					boolean validateFlag = OperateCouponUtil.validateLimitChannel(operateCouponBO.getChannelId(), operateCoupon.getChannelId());
					if (!validateFlag) {
						couponList.remove(operateCouponBO);
						continue;
					}
				}

				// 判断是充值 还是 支付，如果是充值，只能使用充值红包；支付可以使用除充值红包外的其它红包
				Short rechargeRed = PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getKey();// 充值红包
				if (redType.equals(PayConstants.RedTypeEnum.RECHARGE_DISCOUNT.getUseType())) {
					// 红包类型不是充值红包，删除
					if (!operateCouponBO.getRedType().equals(rechargeRed)) {
						couponList.remove(operateCouponBO);
						continue;
					}
				} else {
					// 红包类型不是支付相关红包，删除
					if (operateCouponBO.getRedType().equals(rechargeRed)) {
						couponList.remove(operateCouponBO);
						continue;
					}
					// 彩票中的限制编码和订单中的彩票编码都不为空，才检验
					/*	if (!ObjectUtil.isBlank(operateCouponBO.getLimitLottery())) {
							String[] lotteryCodes = operateCouponBO.getLimitLottery().split(",");
							for (String lotteryCode : lotteryCodes) {
								// 如果红包中存在限制列表，需要删除
								if (operateCoupon.getLottoryCode().startsWith(lotteryCode)) {
									couponList.remove(operateCouponBO);
									break;
								}
							}
						}*/
				}
			}
		}
		return ResultBO.ok(couponList);
	}

	/**
	 * 获取用户红包列表
	 *
	 * @param token
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ResultBO getUserCoupone(final OperateCouponVO vo, final String token) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);

		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}

		vo.setUserId(userInfo.getId());
		// 默认第一页
		if (ObjectUtil.isBlank(vo.getPageIndex())) {
			vo.setPageIndex(0);
		}
		// 默认每页9条数据
		if (ObjectUtil.isBlank(vo.getPageSize())) {
			vo.setPageSize(9);
		}
		PagingBO bo = pageService.getPageData(vo, new ISimplePage() {
			@Override
			public int getTotal() {
				return operateCouponMapper.getUserCouponeCountByUserId(vo);
			}

			@Override
			public List getData() {
				return operateCouponMapper.getUserCouponeByUserId(vo);
			}
		});
		return ResultBO.ok(bo);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ResultBO findOperateCouponCount(String token, OperateCouponVO vo) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);

		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		vo.setUserId(userInfo.getId());
		Map<String, Object> result = new HashMap<>();
		List<DicOperateCouponOptionBO> redTypes = operateCouponMapper.findOperateCouponCountRedTypeByUserId(vo);
		List<DicOperateCouponOptionBO> statues = operateCouponMapper.findOperateCouponCountStatusByUserId(vo);

		List<DicOperateCouponOptionBO> redTypeResults = new ArrayList<>();
		List<DicOperateCouponOptionBO> statueResults = new ArrayList<>();
		for (OperateCouponEnum.RedTypeEnum redTypeEnum : OperateCouponEnum.RedTypeEnum.values()) {
			for (DicOperateCouponOptionBO bo : redTypes) {
				if (redTypeEnum.getType() == bo.getType()) {
					redTypeEnum.setTotal(bo.getTotal());
				}
			}
			redTypeResults.add(new DicOperateCouponOptionBO(redTypeEnum));
		}
		for (OperateCouponEnum.RedStatusEnum redStatusEnum : OperateCouponEnum.RedStatusEnum.values()) {
			for (DicOperateCouponOptionBO bo : statues) {
				if (redStatusEnum.getValue() == bo.getType()) {
					redStatusEnum.setTotal(bo.getTotal());
				}
			}
			statueResults.add(new DicOperateCouponOptionBO(redStatusEnum));
		}
		result.put("redTypes", redTypeResults);
		result.put("statues", statueResults);
		result.put("redBalance", operateCouponMapper.getUserRedBalance(vo));
		return ResultBO.ok(result);
	}

}
