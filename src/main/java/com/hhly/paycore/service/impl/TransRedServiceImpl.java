package com.hhly.paycore.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.OperateCouponMapper;
import com.hhly.paycore.dao.TransRedMapper;
import com.hhly.paycore.po.OperateCouponPO;
import com.hhly.paycore.po.TransRedPO;
import com.hhly.paycore.service.TransRedService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.RedTypeEnum;
import com.hhly.skeleton.base.page.template.PageService;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.bo.PageBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.bo.TransRedBO;
import com.hhly.skeleton.pay.vo.TransRedVO;
import com.hhly.skeleton.pay.vo.UserRedAddParamVo;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

@Service("transRedService")
public class TransRedServiceImpl implements TransRedService {
	private static final Logger logger = Logger.getLogger(TransRedServiceImpl.class);
	@Resource
	private TransRedMapper transRedMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private PageService pageService;
	@Resource
	private OperateCouponMapper operateCouponMapper;

	@Override
	public int addTransRed(TransRedPO record) throws Exception {
		return transRedMapper.addTransRed(record);
	}

	@Override
	public int addTransRed(String redCode, Double redAmount, Integer userId, String orderCode, Short redType) throws Exception {
		TransRedPO transRed = new TransRedPO();
		transRed.setRedCode(redCode);
		transRed.setRedTransCode(OrderNoUtil.getOrderNo(OrderEnum.NumberCode.RUNNING_WATER_IN));
		transRed.setUserId(userId);
		transRed.setTransType(PayConstants.TransTypeEnum.REFUND.getKey());
		transRed.setOrderInfo(Constants.RED_REMARK_CANCEL_SEND);// 退款生成
		transRed.setRedType(redType);
		transRed.setCreateTime(DateUtil.convertStrToDate(DateUtil.getNow()));
		transRed.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易状态 0：交易失败；1：交易成功；
		transRed.setOrderCode(orderCode);//
		transRed.setTransAmount(redAmount);
		transRed.setAftTransAmount(redAmount);// 红包交易后金额
		logger.debug("生成红包交易流水参数【" + transRed.toString() + "】");
		return addTransRed(transRed);
	}

	@Override
	public void addTransRed(UserRedAddParamVo userRedAddParamVo) throws Exception {
		TransRedPO transRedPO = new TransRedPO();
		transRedPO.setRedCode(userRedAddParamVo.getRedCode());
		transRedPO.setRedTransCode(OrderNoUtil.getOrderNo(NumberCode.RUNNING_WATER_OUT));
		transRedPO.setUserId(userRedAddParamVo.getUserId());
		transRedPO.setOrderCode("");
		transRedPO.setTransStatus(userRedAddParamVo.getStatus());
		transRedPO.setTransAmount(userRedAddParamVo.getRedAmount());
		transRedPO.setAftTransAmount(userRedAddParamVo.getAfterRedAmount());
		transRedPO.setTransType(userRedAddParamVo.getTransType());
		transRedPO.setOrderInfo(userRedAddParamVo.getOrderInfo());
		transRedPO.setRedType(RedTypeEnum.RED_COLOR.getKey());// 彩金红包
		int num = transRedMapper.addTransRed(transRedPO);
		if (num <= 0) {
			logger.error("生成红包交易记录失败，参数：" + transRedPO.toString());
			throw new RuntimeException("生成红包交易记录失败");
		}
	}

	@Override
	public void addTransRed(OperateCouponBO operateCoupon, Short status, Short transType, Double transAmount, Double aftTransAmount, String orderInfo, String orderCode) {
		// 添加红包交易记录
		TransRedPO transRedPO = new TransRedPO();
		transRedPO.setRedTransCode(OrderNoUtil.getOrderNo(NumberCode.RUNNING_WATER_OUT));// 红包交易流水编号，自动生成
		transRedPO.setRedCode(operateCoupon.getRedCode());// 红包编号，红包包中的红包编号
		transRedPO.setUserId(operateCoupon.getUserId());// 用户ID
		transRedPO.setRedType(operateCoupon.getRedType());// 红包类型
		transRedPO.setOrderInfo(orderInfo);
		transRedPO.setTransStatus(status);
		if (!ObjectUtil.isBlank(orderCode) && orderCode.contains(",")) {
			orderCode = orderCode.substring(0, orderCode.indexOf(","));
		}
		transRedPO.setOrderCode(orderCode);// 订单号D1705201623260100087,1;
		transRedPO.setTransType(transType);// 交易类型
		transRedPO.setAftTransAmount(aftTransAmount);// 红包交易后金额
		transRedPO.setTransAmount(transAmount);
		int num = transRedMapper.addTransRed(transRedPO);
		if (num <= 0) {
			logger.error("生成红包交易记录失败，参数：" + transRedPO.toString());
			throw new RuntimeException("生成红包交易记录失败");
		}
	}

	/**  
	* 方法说明: 添加红包交易流水
	* @auth: xiongJinGang
	* @param operateCouponPO
	* @param transRechargeBO
	* @throws Exception
	* @time: 2017年8月12日 下午3:57:20
	* @return: void 
	*/
	@Override
	public void addRedTransRecord(OperateCouponPO operateCouponPO, TransRechargeBO transRechargeBO) throws Exception {
		// 添加红包交易流水
		TransRedPO transRedPO = new TransRedPO();
		transRedPO.setRedTransCode(OrderNoUtil.getOrderNo(NumberCode.RUNNING_WATER_OUT));// 红包交易流水编号，自动生成
		transRedPO.setRedCode(operateCouponPO.getRedCode());// 红包编号，红包中的红包编号
		transRedPO.setUserId(transRechargeBO.getUserId());// 用户ID
		transRedPO.setRedType(operateCouponPO.getRedType());// 红包类型
		transRedPO.setOrderInfo(Constants.ACTIVITY_SEND);// 后台发放
		transRedPO.setTransStatus(PayConstants.UserTransStatusEnum.TRADE_SUCCESS.getKey());// 交易状态 0：交易失败；1：交易成功；
		String orderCode = transRechargeBO.getOrderCode();
		if (!ObjectUtil.isBlank(orderCode) && orderCode.contains(",")) {
			orderCode = orderCode.substring(0, orderCode.indexOf(","));
		}
		transRedPO.setOrderCode(orderCode);// 订单号
		transRedPO.setTransType(PayConstants.TransTypeEnum.RECHARGE.getKey());// 交易类型
		transRedPO.setAftTransAmount(transRechargeBO.getRedAmount());// 红包交易后金额
		transRedPO.setTransAmount(transRechargeBO.getRedAmount());
		int num = transRedMapper.addTransRed(transRedPO);
		if (num <= 0) {
			logger.error("生成红包交易记录失败，参数：" + transRedPO.toString());
			throw new RuntimeException("生成红包交易记录失败");
		}
	}

	@Override
	public ResultBO<?> findUserTransRedByCode(String token, Integer redCode) {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		List<TransRedBO> list = transRedMapper.getUserTransRedByCode(userInfo.getId(), redCode);
		return ResultBO.ok(list);
	}

	@Override
	public ResultBO<?> findUserTransRedByPage(TransRedVO vo) {
		UserInfoBO userInfo = userUtil.getUserByToken(vo.getToken());
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		vo.setUserId(userInfo.getId());
		// 默认第一页
		if (ObjectUtil.isBlank(vo.getCurrentPage())) {
			vo.setCurrentPage(0);
		}
		// 默认每页10条数据
		if (ObjectUtil.isBlank(vo.getShowCount())) {
			vo.setShowCount(10);
		}
		if (!ObjectUtil.isBlank(vo.getRedCode())) {
			OperateCouponBO operateCouponBO = null;
			try {
				operateCouponBO = operateCouponMapper.getUserCouponeByRedCode(vo.getUserId(), vo.getRedCode());
				if (ObjectUtil.isBlank(operateCouponBO)) {
					return ResultBO.err(MessageCodeConstants.PAY_RED_DETAIL_NOT_FOUND_ERROR_SERVICE);
				}
			} catch (Exception e) {
				logger.error("获取用户ID【" + vo.getUserId() + "】的红包【" + vo.getRedCode() + "】详情异常。" + e.getMessage());
				return ResultBO.err(MessageCodeConstants.HESSIAN_ERROR_SYS);
			}
		}
		int count = transRedMapper.findUserTransRedListCount(vo);
		PageBO pageBO = new PageBO(vo.getShowCount(), count, vo.getCurrentPage());
		List<TransRedBO> list = transRedMapper.findUserTransRedListByPage(vo);
		pageBO.setDataList(list);
		return ResultBO.ok(pageBO);
	}
}
