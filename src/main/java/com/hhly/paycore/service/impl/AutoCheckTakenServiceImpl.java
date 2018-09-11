/**    
* @Title: AutoCheckTakenServiceImpl.java  
* @Package com.hhly.paycore.service.impl  
* @Description: TODO
* @author xiongJinGang 
* @date 2018年3月7日 上午10:18:47  
* @version V1.0    
*/
package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.hhly.paycore.dao.DicDataDetailMapper;
import com.hhly.paycore.dao.TransTakenMapper;
import com.hhly.paycore.dao.TransUserMapper;
import com.hhly.paycore.po.TransTakenPO;
import com.hhly.paycore.po.TransUserPO;
import com.hhly.paycore.service.AutoCheckTakenService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.common.OrderEnum.NumberCode;
import com.hhly.skeleton.base.constants.Constants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.UserTransMoneyFlowEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.MathUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.base.util.OrderNoUtil;
import com.hhly.skeleton.lotto.base.dic.bo.DicDataDetailBO;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.bo.TransUserBO;

/**
 * @desc 自动审核提款
 * @author xiongJinGang
 * @date 2018年3月7日
 * @company 益彩网络科技公司
 * @version 1.0
 */
@Service
public class AutoCheckTakenServiceImpl implements AutoCheckTakenService {
	private static final Logger logger = LoggerFactory.getLogger(AutoCheckTakenServiceImpl.class);

	private static final Integer PAGE_NUM = 20;
	@Resource
	private DicDataDetailMapper dicDataDetailMapper;
	@Resource
	private TransTakenMapper transTakenMapper;
	@Resource
	private TransUserMapper transUserMapper;
	@Resource
	private MessageProvider messageProvider;

	@Override
	public ResultBO<?> autoCheckForQuartz() throws Exception {
		logger.info("自动审核提款记录开始");
		DicDataDetailBO dataDetailBO = findDataDetail();
		if (ObjectUtil.isBlank(dataDetailBO)) {
			logger.error("未获取到自动提款的基础数据");
			return ResultBO.err(MessageCodeConstants.BASE_DATA_INFO_NOT_CONFIG);
		}

		while (true) {
			// 需要自动审核的提款列表
			List<TransTakenBO> takenList = findTakenList(dataDetailBO);
			if (ObjectUtil.isBlank(takenList)) {
				logger.error("未获取到需要自动审核的提款记录");
				return ResultBO.err(MessageCodeConstants.AUTO_CHECK_DATA_NOT_FOUND);
			}
			List<TransTakenBO> newTakenList = new CopyOnWriteArrayList<>(takenList);
			TransUserPO transUserPO = null;
			List<TransUserPO> updateUserList = new ArrayList<>();

			for (TransTakenBO transTakenBO : newTakenList) {
				TransUserBO transUserBO = findTransUserList(transTakenBO);
				if (ObjectUtil.isBlank(transUserBO)) {
					messageProvider.sendAutoCheckMessage(transTakenBO.getTransTakenCode());
					logger.info("提款记录【" + transTakenBO.getTransTakenCode() + "】的交易流水，不满足自动审核条件，发送报警信息成功");
					newTakenList.remove(transTakenBO);
					continue;
				}
				transUserPO = new TransUserPO();
				transUserPO.setId(transUserBO.getId());
				transUserPO.setTradeCode(transTakenBO.getTransTakenCode());
				transUserPO.setTransCode(transUserBO.getTransCode());
				transUserPO.setTransStatus(PayConstants.UserTransStatusEnum.AUDIT_SUCCESS.getKey());
				transUserPO.setTransType(transUserBO.getTransType());
				updateUserList.add(transUserPO);
			}

			List<TransTakenPO> updateTakenList = null;
			if (!ObjectUtil.isBlank(newTakenList)) {
				TransTakenPO transTakenPO = null;
				updateTakenList = new ArrayList<>();
				for (TransTakenBO transTakenBO : newTakenList) {
					transTakenPO = new TransTakenPO(transTakenBO);
					transTakenPO.setTransStatus(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey());
					transTakenPO.setReviewBy(Constants.SYSTEM_OPERATE);
					transTakenPO.setReviewTime(DateUtil.getNowDate());
					String batchCode = OrderNoUtil.getOrderNo(NumberCode.SEND_BATCH);// 批次号
					transTakenPO.setBatchNum(batchCode);
					updateTakenList.add(transTakenPO);
				}
			}
			// 批量更新
			updateTakenUserByBatch(updateUserList, updateTakenList);
			if (takenList.size() < PAGE_NUM) {
				break;
			}
		}
		logger.info("自动审核提款记录结束");
		return ResultBO.ok();
	}

	/**  
	* 方法说明: 批量更新流水及提款记录状态
	* @auth: xiongJinGang
	* @param updateUserList
	* @param updateTakenList
	* @time: 2018年3月8日 上午10:22:58
	* @return: void 
	*/
	public void updateTakenUserByBatch(List<TransUserPO> updateUserList, List<TransTakenPO> updateTakenList) {
		if (!ObjectUtil.isBlank(updateUserList)) {
			logger.info("自动审核成功，开始批量更新记录状态");
			transTakenMapper.updateTakenByBatch(updateTakenList);
			transUserMapper.updateTransUserByBatch(updateUserList);
		} else {
			logger.info("没有满足自动审核的提款记录");
		}
	}

	/**  
	* 方法说明: 计算交易流水的出入账
	* @auth: xiongJinGang
	* @param transTakenBO
	* @time: 2018年3月7日 下午6:59:02
	* @return: void 
	*/
	private TransUserBO findTransUserList(TransTakenBO transTakenBO) {
		// 根据提款编号，获取交易流水信息
		TransUserBO transUser = transUserMapper.findTransUserByTradeCode(transTakenBO.getTransTakenCode());
		// 上一个流水的前面20条流水
		List<TransUserBO> list = transUserMapper.findTransUserListForAutoCheck(transUser.getId(), PAGE_NUM, transUser.getUserId());
		if (!ObjectUtil.isBlank(list)) {
			Double totalCashBalance = 0d;
			for (int i = list.size() - 1; i >= 0; i--) {
				TransUserBO transUserBO = list.get(i);
				if (i == (list.size() - 1)) {
					totalCashBalance = transUserBO.getTotalCashBalance();
					continue;
				}
				UserTransMoneyFlowEnum userTransMoneyFlowEnum = PayConstants.UserTransMoneyFlowEnum.getTransTypeByKey(transUserBO.getTransType());
				// 如果是入账
				if (PayConstants.MoneyFlowEnum.IN.getKey().equals(userTransMoneyFlowEnum.getType())) {
					totalCashBalance = MathUtil.add(totalCashBalance, transUserBO.getCashAmount());
				} else {
					// 因为提款涉及到手续费，现金交易账户的金额已经扣除了手续费，用户钱包中实际支出的金额是 交易总金额
					if (PayConstants.TransTypeEnum.DRAWING.getKey().equals(transUserBO.getTransType())) {
						totalCashBalance = MathUtil.sub(totalCashBalance, transUserBO.getTransAmount());
					} else {
						totalCashBalance = MathUtil.sub(totalCashBalance, transUserBO.getCashAmount());
					}
				}
			}
			// 计算20条流水后的金额 与 最后一条的提款金额加上提款后的剩余金额一致，表示可以自动审核
			if (totalCashBalance.equals(MathUtil.add(transTakenBO.getExtractAmount(), transUser.getTotalCashBalance()))) {
				logger.info("编号【" + transTakenBO.getTransTakenCode() + "】的提款记录，最近20条交易流水正常，可以自动审核");
				return transUser;
			} else {
				logger.info("编号【" + transTakenBO.getTransTakenCode() + "】的提款记录，最近20条交易流水异常，不能自动审核");
			}
		}
		return null;
	}

	/**  
	* 方法说明: 
	* @auth: xiongJinGang
	* @param dataDetailBO
	* @time: 2018年3月7日 下午4:05:20
	* @return: void 
	*/
	private List<TransTakenBO> findTakenList(DicDataDetailBO dataDetailBO) {
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("transStatus", PayConstants.TakenStatusEnum.PENDING_AUDIT.getKey());
		paramMap.put("extractAmount", dataDetailBO.getDicDataValue());// 低于现在的提现金额
		paramMap.put("startTime", DateUtil.getBeforeOrAfterDate(-1, DateUtil.DEFAULT_FORMAT));// 创建的开始时间
		paramMap.put("endTime", DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 创建的结束时间
		paramMap.put("limit", PAGE_NUM);// 每次查询多少条
		return transTakenMapper.findWaitVerifyTakenList(paramMap);
	}

	/**  
	* 方法说明: 自动提款的基础数据
	* @auth: xiongJinGang
	* @time: 2018年3月7日 下午3:15:09
	* @return: DicDataDetailBO 
	*/
	private DicDataDetailBO findDataDetail() {
		List<DicDataDetailBO> list = dicDataDetailMapper.findDataList("1111");// 1111是基础数据的code
		if (!ObjectUtil.isBlank(list)) {
			for (DicDataDetailBO dicDataDetailBO : list) {
				// 包含 autoCheckTaken的表示为自动审核条件
				if (dicDataDetailBO.getDicDataName().contains("autoCheckTaken")) {
					return dicDataDetailBO;
				}
			}
		}
		return null;
	}

}
