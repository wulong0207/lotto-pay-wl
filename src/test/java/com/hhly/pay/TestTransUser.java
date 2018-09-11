package com.hhly.pay;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.dao.OrderFollowedInfoDaoMapper;
import com.hhly.paycore.jms.CloseRechargeDelay;
import com.hhly.paycore.remote.service.IPayService;
import com.hhly.paycore.remote.service.IRechargeService;
import com.hhly.paycore.remote.service.ITransUserLogService;
import com.hhly.paycore.service.MessageProvider;
import com.hhly.paycore.service.TransUserService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.CacheConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.UserConstants;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.lotto.base.ordercopy.bo.OrderFollowedInfoBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;
import com.hhly.skeleton.pay.vo.RefundParamVO;
import com.hhly.skeleton.user.bo.UserInfoBO;
import com.hhly.utils.RedisUtil;
import com.hhly.utils.UserUtil;

public class TestTransUser extends BaseTest {

	@Resource
	private TransUserService transUserService;
	@Resource
	private ITransUserLogService transUserLogService;
	@Resource
	private MessageProvider messageProvider;
	@Resource
	private IPayService payService;
	@Resource
	private IRechargeService rechargeService;

	@Resource
	private OrderFollowedInfoDaoMapper followedInfoDaoMapper;
	@Resource
	private RedisUtil redisUtil;
	@Resource
	private UserUtil userUtil;
	String key = "1login_user_info_key";

	public static void main(String[] args) throws Exception {
		String sevenAfterDay = DateUtil.getBeforeOrAfterYearForString(20, DateUtil.DATE_FORMAT) + " 23:59:59";
		System.out.println(sevenAfterDay);
	}

	/**  
	* 方法说明: 消息发送
	* @auth: xiongJinGang
	* @time: 2017年6月7日 上午10:48:55
	* @return: void 
	*/
	@Test
	public void messageSend() {
		// messageProvider.sendFlowMessage("D1705151804000100002,", null, 2);
		// messageProvider.sendMessage("split_queue", "33333,444444,55555,6666");
	}

	@Test
	public void subNoPayOrder() {
		redisUtil.delAllString("ORDER_NO_PAY_COUNT_C_550");
	}

	/**  
	* 方法说明: 退款
	* @auth: xiongJinGang
	* @time: 2017年6月7日 上午10:46:32
	* @return: void 
	*/
	@Test
	public void refundTest() {
		RefundParamVO refundParam = new RefundParamVO();
		// refundParam.setTradeNo("411706085784166666");
		refundParam.setTransCode("I17110409322608000004");
		refundParam.setRefundAmount(1d);
		refundParam.setRefundReason("不想要了");
		ResultBO<?> resultBO = payService.refund(refundParam);
		System.out.println(JSON.toJSONString(resultBO));
	}

	@Test
	public void tokenTest() {
		// 支付结果
		// redisUtil.addString("PAY_STATUS_RESULT_619_O1706031555411000010", "success", CacheConstants.FIFTEEN_MINUTES);
	}

	/**  
	* 方法说明: 清除redis
	* @auth: xiongJinGang
	* @time: 2017年6月7日 上午10:47:25
	* @return: void 
	*/
	@Test
	public void redisClearTest() {
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_BANK_LIST);
		redisUtil.delAllString(CacheConstants.P_CORE_USER_PAY_CHANNEL);
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_BANK_CHANNEL_SINGLE);
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_BANK_LIMIT_SINGLE);
		redisUtil.delAllString(CacheConstants.P_CORE_USER_BANK_CARD_LIST);
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_CHANNEL_LIMIT_LIST);
		redisUtil.delAllString(CacheConstants.P_CORE_PAY_CHANNEL_MGR_LIST);
	}

	/**  
	* 方法说明: 创建登录token
	* @auth: xiongJinGang
	* @time: 2017年6月7日 上午10:48:05
	* @return: void 
	*/
	@Test
	public void createLoginTokenTest() {
		try {
			redisUtil.delObj(key);
			System.out.println(redisUtil.getString(key));

			UserInfoBO userInfoBO = new UserInfoBO();
			userInfoBO.setId(54);
			// userInfoBO.setId(35);
			// userInfoBO.setId(1674);

			userInfoBO.setRealName("熊金刚");
			userInfoBO.setMobile("13428974866");
			userInfoBO.setIdCard("430181198406114375");
			userInfoBO.setAccountStatus((short) 1);

			userInfoBO.setUserPayId(22);
			userInfoBO.setMobileStatus(UserConstants.IS_TRUE);
			long time = 60 * 24 * 60 * 60 * 1000;
			userUtil.addUserCacheByToken(key, userInfoBO, time);
			UserInfoBO bb = userUtil.getUserByToken(key);
			System.out.println("用户ID：" + bb.getId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void createMobileCodeTest() {
		try {
			String key = "134289748669";
			redisUtil.addString(key, "666666", 88883333l);
			System.out.println("缓存：" + redisUtil.getString(key));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void updateRechargeTest() throws Exception {
		CmsRechargeVO cmsRecharge = new CmsRechargeVO();
		cmsRecharge.setUserId(1);
		cmsRecharge.setRechargeAmount(100d);
		cmsRecharge.setRechargeRemark("后台充值");
		cmsRecharge.setRechargeType(PayConstants.CmsRechargeTypeEnum.CASH.getKey());
		cmsRecharge.setOperator("金刚");
		ResultBO<?> resultBO = rechargeService.updateRecharge(cmsRecharge);
		System.out.println(JSON.toJSONString(resultBO));
	}

	@Test
	public void updateWalletFirstRechargeForCmsTest() throws Exception {
		CmsRechargeVO cmsRecharge = new CmsRechargeVO();
		cmsRecharge.setUserId(54);
		cmsRecharge.setRechargeAmount(15d);
		cmsRecharge.setRechargeCode("I18030712114716600040");
		cmsRecharge.setActivityCode("111111111");
		ResultBO<?> resultBO = rechargeService.updateWalletFirstRechargeForCms(cmsRecharge);
		System.out.println(JSON.toJSONString(resultBO));
	}

	@Test
	public void getKeys() throws Exception {
		String key = CacheConstants.P_CORE_RECHARGE_ORDER;
		Set<String> set = redisUtil.keys(key);
		for (String string : set) {
			System.out.println("键的列表：" + string);

			CloseRechargeDelay closeRechargeDelay = redisUtil.getObj(string, new CloseRechargeDelay());
			System.out.println(closeRechargeDelay.getRechargeCode());
			System.out.println(closeRechargeDelay.getExpireTime());
		}
	}

	@Test
	public void findOrderFollowMap() {
		Map<String, OrderFollowedInfoBO> orderFollowedInfoMap = followedInfoDaoMapper.findOrderFollowMap(272);
		for (String key : orderFollowedInfoMap.keySet()) {
			System.out.println(key);
		}
	}

	@Test
	public void getUserInfo() throws Exception {
		String token = "2a53f29320d7345f096503949d3a83f69";
		UserInfoBO userInfoBO = userUtil.getUserByToken(token);
		System.out.println(token + "的用户ID：" + userInfoBO.getId());
	}

	@Test
	public void findTransUserLogList() {
		// ResultBO<?> resultBO = transUserLogService.findTransUserByPage(transParamVO);
		// System.out.println("查询交易流水返回：" + JSON.toJSON(resultBO));
	}

}
