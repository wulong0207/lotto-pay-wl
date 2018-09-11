package com.hhly.paycore.service.impl;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hhly.paycore.common.TakenUtil;
import com.hhly.paycore.dao.BankcardMapper;
import com.hhly.paycore.po.PayBankcardPO;
import com.hhly.paycore.service.BankcardSegmentService;
import com.hhly.paycore.service.BankcardService;
import com.hhly.paycore.service.PayBankLimitService;
import com.hhly.paycore.service.PayBankService;
import com.hhly.paycore.service.UserInfoService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PayBankBO;
import com.hhly.skeleton.pay.bo.PayBankcardBO;
import com.hhly.skeleton.pay.bo.TransRechargeBO;
import com.hhly.skeleton.pay.vo.PayBankcardVO;
import com.hhly.skeleton.pay.vo.TakenBankCardVO;
import com.hhly.skeleton.pay.vo.TakenReqParamVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

/**
 * @version 1.0
 * @auth chenkangning
 * @date 2017/3/2.
 * @desc 用户银行卡管理接口实现类
 * @compay 益彩网络科技有限公司
 */
@Service("bankcardService")
public class BankcardServiceImpl implements BankcardService {
	private static final Logger logger = Logger.getLogger(BankcardServiceImpl.class);

	@Autowired
	private BankcardMapper bankcardMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	@Resource
	private PayBankService payBankService;
	@Resource
	private BankcardSegmentService bankcardSegmentService;
	@Resource
	private PayBankLimitService bankLimitService;
	@Resource
	private UserInfoService userInfoService;

	@Override
	public List<PayBankcardBO> findUserBankList(Integer userId) {
		PayBankcardVO payBankcardVO = new PayBankcardVO(userId);
		return bankcardMapper.selectBankCard(payBankcardVO);
	}

	@Override
	public List<PayBankcardBO> findUserBankListFromCache(Integer userId) {
		String key = CacheConstants.P_CORE_USER_BANK_CARD_LIST + userId;
		List<PayBankcardBO> list = redisUtil.getObj(key, new ArrayList<PayBankcardBO>());
		if (ObjectUtil.isBlank(list)) {
			list = findUserBankList(userId);
			if (!ObjectUtil.isBlank(list)) {
				redisUtil.addObj(key, list, CacheConstants.ONE_HOURS);
			}
		}
		return list;
	}

	@Override
	public void findBankByIdAndCheckName(Integer userId, TakenReqParamVO takenReqParamVO) {
		PayBankcardBO payBankcardBO = bankcardMapper.getUserBankById(userId, takenReqParamVO.getBankCardId());
		if (!ObjectUtil.isBlank(payBankcardBO)) {
			// 银行名称不相等，表示用户修改过，需要保存用户输入
			if (!ObjectUtil.isBlank(payBankcardBO.getBankname()) && !payBankcardBO.getBankname().equals(takenReqParamVO.getBankName())) {
				PayBankcardPO payBankcardPO = new PayBankcardPO();
				payBankcardPO.setId(payBankcardBO.getId());
				payBankcardPO.setBankname(takenReqParamVO.getBankName());
				bankcardMapper.updateBankName(payBankcardPO);
			}
		}
	}

	@Override
	public PayBankcardBO getSingleBankCard(Integer userId) {
		List<PayBankcardBO> list = findUserBankListFromCache(userId);
		PayBankcardBO payBankcard = null;
		if (!ObjectUtil.isBlank(list)) {
			int num = 0;
			for (PayBankcardBO payBankcardBO : list) {
				if (num == 0) {
					payBankcard = payBankcardBO;
				}
				// 是否有默认不为空并且是默认的，取默认的
				if (!ObjectUtil.isBlank(payBankcardBO.getIsdefault()) && Integer.valueOf(payBankcardBO.getIsdefault()).equals(PayConstants.IsDefaultEnum.TRUE.getKey())) {
					payBankcard = payBankcardBO;
					break;
				}
				num++;
			}
		}
		return payBankcard;
	}

	@Override
	public PayBankcardBO getBankCardByCardCodeFromCache(String cardCode, Integer userId) {
		List<PayBankcardBO> list = findUserBankListFromCache(userId);
		PayBankcardBO payBankcard = null;
		if (!ObjectUtil.isBlank(list)) {
			for (PayBankcardBO payBankcardBO : list) {
				if (payBankcardBO.getCardcode().equals(cardCode)) {
					return payBankcardBO;
				}
			}
		}
		return payBankcard;
	}

	@Override
	public PayBankcardBO findUserBankById(Integer userId, Integer bankCardId) {
		return bankcardMapper.getUserBankById(userId, bankCardId);
	}

	@Override
	public PayBankcardBO findUserBankByCode(Integer userId, String cardCode) {
		return bankcardMapper.getUserBankByCode(userId, cardCode);
	}

	@Override
	public ResultBO<?> findUserBankCardByCardId(Integer userId, Integer bankCardId) {
		PayBankcardBO payBankcard = null;
		try {
			payBankcard = bankcardMapper.getUserBankById(userId, bankCardId);
		} catch (Exception e1) {
			logger.error("获取用户" + userId + " 的银行卡" + bankCardId + "信息异常" + e1.getMessage());
			return ResultBO.err(MessageCodeConstants.SYS_ERROR_SYS);
		}
		if (!ObjectUtil.isBlank(payBankcard)) {
			// 已经被删除了
			if (payBankcard.getStatus().equals(PayConstants.WinStatusEnum.DISABLE.getKey())) {
				logger.info("用户【" + userId + "】银行卡【" + bankCardId + "】已删除，不能使用");
				return ResultBO.err(MessageCodeConstants.BANKCARD_IS_VALIDATION_SERVICE);
			}
			Short bankType = payBankcard.getBanktype();
			// 信用卡，要判断有效期
			if (PayConstants.BankCardTypeEnum.CREDIT.getKey().equals(bankType)) {
				String overdue = payBankcard.getOverdue();
				if (ObjectUtil.isBlank(overdue)) {
					logger.info("信用卡【" + payBankcard.getCardcode() + "】的有效期为空");
					return ResultBO.err(MessageCodeConstants.BANK_CARD_OVERDUE_IS_NULL_FIELD);
				}
				try {
					boolean isValidate = DateUtil.validateCreditCard(overdue);
					if (!isValidate) {
						logger.info("信用卡【" + payBankcard.getCardcode() + "】已过有效期【" + overdue + "】");
						return ResultBO.err(MessageCodeConstants.PAY_CREDIT_INVALID_ERROR_SERVICE);
					}
					// 返回银行卡信息
					return ResultBO.ok(payBankcard);
				} catch (Exception e) {
					logger.info("信用卡【" + payBankcard.getCardcode() + "】有效期【" + overdue + "】格式错误");
					// 比较出异常，说明信用卡存储格式有问题
					return ResultBO.err(MessageCodeConstants.PAY_CREDIT_FORMAT_ERROR_SERVICE);
				}
			}
			return ResultBO.ok(payBankcard);
		} else {
			logger.info("未获取到用户【" + userId + "】银行卡【" + bankCardId + "】信息");
			return ResultBO.err(MessageCodeConstants.PAY_BANKCARD_NOT_FOUND_SERVICE);
		}
	}

	/**
	 * 设置默认银行卡
	 *
	 * @param payBankcardVO 数据对象
	 * @return ResultBO
	 * @throws Exception
	 */
	@Override
	public ResultBO<?> updateDefault(PayBankcardVO payBankcardVO) throws Exception {
		int row, row2;
		PayBankcardPO payBankcardPO = new PayBankcardPO();
		BeanUtils.copyProperties(payBankcardVO, payBankcardPO);
		payBankcardPO.setUserId(payBankcardVO.getUserid());
		row = bankcardMapper.updateDefault(payBankcardPO);
		row2 = bankcardMapper.updateDisableDefault(payBankcardPO);
		if (row > 0 && row2 > 0) {
			return ResultBO.ok();
		} else {
			return ResultBO.err("updateDefault is error");
		}
	}

	@Override
	public List<TakenBankCardVO> getUserBankInfo(Integer userId, Integer bankCardId) {
		List<PayBankcardBO> bankCardList = findUserBankList(userId);
		// 先不用缓存
		List<PayBankBO> bankList = payBankService.findAllBank();
		List<TakenBankCardVO> takenBankList = new ArrayList<TakenBankCardVO>();
		TakenBankCardVO takenBankCardVO = null;
		if (ObjectUtil.isBlank(bankCardList)) {
			logger.info("未获取到用户【" + userId + "】银行信息");
			return takenBankList;
		}
		for (PayBankcardBO bankcard : bankCardList) {
			// 是储蓄卡，才进行下一步
			if (bankcard.getBanktype().equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
				if (ObjectUtil.isBlank(bankCardId)) {// 获取所有的储蓄卡.
					for (PayBankBO payBankBO : bankList) {
						if (bankcard.getBankid().intValue() == payBankBO.getId().intValue()) {
							takenBankCardVO = new TakenBankCardVO();
							TakenUtil.setTakenBankCardVO(takenBankCardVO, payBankBO, bankcard);
							takenBankCardVO.setIsDefault(bankcard.getIsdefault());
							takenBankList.add(takenBankCardVO);
							break;
						}
					}
				} else {
					// 银行卡ID一致，获取银行简称
					if (bankcard.getId().equals(bankCardId)) {
						for (PayBankBO payBankBO : bankList) {
							if (bankcard.getBankid().equals(payBankBO.getId())) {
								takenBankCardVO = new TakenBankCardVO();
								TakenUtil.setTakenBankCardVO(takenBankCardVO, payBankBO, bankcard);
								takenBankCardVO.setIsDefault(bankcard.getIsdefault());
								takenBankList.add(takenBankCardVO);
								break;
							}
						}
						break;
					}
				}
			}
		}
		return takenBankList;
	}

	@Override
	public ResultBO<?> updateDefaultBank(TransRechargeBO transRecharge) throws Exception {
		// 把最近使用的银行卡设置成默认
		if (!ObjectUtil.isBlank(transRecharge.getBankCardNum())) {
			PayBankcardVO payBankcardVO = new PayBankcardVO(transRecharge.getUserId(), transRecharge.getBankCardNum());

			Short switchStatus = ObjectUtil.isBlank(transRecharge.getSwitchStatus()) ? 0 : transRecharge.getSwitchStatus();
			if (switchStatus.equals(PayConstants.ChangeEnum.YES.getKey())) {
				PayBankcardBO payBankcardBO = findUserBankByCode(transRecharge.getUserId(), transRecharge.getBankCardNum());
				// 原来开通了快捷，改成网银；原来开通的网银，改成快捷
				if (payBankcardBO.getOpenbank().equals(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey())) {
					payBankcardVO.setOpenbank(PayConstants.BandCardQuickEnum.NOT_OPEN.getKey());
				} else {
					payBankcardVO.setOpenbank(PayConstants.BandCardQuickEnum.HAD_OPEN.getKey());
				}
			}
			return updateDefault(payBankcardVO);
		}
		return ResultBO.ok();
	}

	@Override
	public ResultBO<?> findBankList(String token) throws Exception {
		UserInfoBO userInfo = userUtil.getUserByToken(token);
		if (ObjectUtil.isBlank(userInfo)) {
			return ResultBO.err(MessageCodeConstants.TOKEN_LOSE_SERVICE);
		}
		Integer userId = userInfo.getId();
		List<TakenBankCardVO> takenBankList = getUserBankInfo(userId, null);
		if (ObjectUtil.isBlank(takenBankList)) {
			logger.info("用户【" + userId + "】申请提款失败：" + MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
			return ResultBO.err(MessageCodeConstants.TAKEN_BANK_CARD_NOT_FOUNE_ERROR_SERVICE);
		}
		// 重新查询一次
		UserInfoBO userInfoBO = userInfoService.findUserInfo(userId);
		// 用户最近支付id 等于存的id 默认显示它
		if (!ObjectUtil.isBlank(userInfoBO.getUserPayId())) {
			for (int i = takenBankList.size() - 1; i >= 0; i--) {
				TakenBankCardVO takenBankCard = takenBankList.get(i);
				// 银行ID一致，并且是储蓄卡，将其
				if (takenBankCard.getBankId().equals(userInfoBO.getUserPayId()) && takenBankCard.getBankType().equals(PayConstants.BankCardTypeEnum.BANK_CARD.getKey())) {
					TakenBankCardVO first = takenBankCard;
					takenBankList.remove(takenBankCard);
					takenBankList.add(0, first);// 设置成第一位
				}
			}
		}
		return ResultBO.ok(takenBankList);
	}
}
