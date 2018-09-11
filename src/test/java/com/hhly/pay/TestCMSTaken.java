package com.hhly.pay;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.junit.Test;

import com.hhly.paycore.remote.service.IRechargeService;
import com.hhly.paycore.service.SendPrizeService;
import com.hhly.paycore.service.TransTakenConfirmService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.TakenOperateTypeEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.pay.bo.TransTakenBO;
import com.hhly.skeleton.pay.vo.CmsRechargeVO;

public class TestCMSTaken extends BaseTest {

	@Resource
	private TransTakenConfirmService transTakenConfirmService;
	@Resource
	private IRechargeService rechargeService;
	@Resource
	private SendPrizeService sendPrizeService;
	String key = "login_user_info_key";
	String transCode = "O17102715383211900005";

	/**  
	* 方法说明: 提款审核
	* @auth: xiongJinGang
	* @time: 2017年9月8日 上午11:45:25
	* @return: void 
	*/
	@Test
	public void testAuditTaken() {
		try {
			List<TransTakenBO> list = new ArrayList<TransTakenBO>();
			TransTakenBO transTakenBO = new TransTakenBO();
			transTakenBO.setTransTakenCode(transCode);
			transTakenBO.setUserId(1);
			// 1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
			transTakenBO.setTransStatus(PayConstants.TakenStatusEnum.AUDIT_THROUGH.getKey());
			transTakenBO.setReviewBy("金刚");
			transTakenBO.setReviewTime(DateUtil.getNowDate());
			list.add(transTakenBO);
			// 当前操作
			Short operateType = TakenOperateTypeEnum.AUDIT.getKey();
			ResultBO<?> resultBO = transTakenConfirmService.updateTakenStatusByBatch(list, operateType);
			System.out.println("接口返回：" + resultBO.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 银行处理
	@Test
	public void testBankDeal() {
		try {
			List<TransTakenBO> list = new ArrayList<TransTakenBO>();
			TransTakenBO transTakenBO = new TransTakenBO();
			transTakenBO.setTransTakenCode(transCode);
			transTakenBO.setUserId(1);
			// 1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
			transTakenBO.setTransStatus(PayConstants.TakenStatusEnum.BANK_HANDLING_SUCCESS.getKey());
			transTakenBO.setReviewBy("金刚");
			transTakenBO.setReviewTime(DateUtil.getNowDate());
			list.add(transTakenBO);
			// 当前操作
			Short operateType = TakenOperateTypeEnum.BANK_COMPLETE.getKey();
			ResultBO<?> resultBO = transTakenConfirmService.updateTakenStatusByBatch(list, operateType);
			System.out.println("接口返回：" + resultBO.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 处理成功改成处理失败
	@Test
	public void testBankComplate() {
		try {
			List<TransTakenBO> list = new ArrayList<TransTakenBO>();
			TransTakenBO transTakenBO = new TransTakenBO();
			transTakenBO.setTransTakenCode(transCode);
			transTakenBO.setUserId(1);
			// 1审核通过; 2审核不通过; 3银行处理成功; 4银行处理失败; 5已到帐;6待审核;7银行处理中
			transTakenBO.setTransStatus(PayConstants.TakenStatusEnum.BANK_HANDLING_FAIL.getKey());
			transTakenBO.setReviewBy("金刚");
			transTakenBO.setReviewTime(DateUtil.getNowDate());
			list.add(transTakenBO);
			// 当前操作
			Short operateType = TakenOperateTypeEnum.SUCCESS_TO_FAIL.getKey();
			ResultBO<?> resultBO = transTakenConfirmService.updateTakenStatusByBatch(list, operateType);
			System.out.println("接口返回：" + resultBO.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**  
	* 方法说明: 测试重置开奖
	* @auth: xiongJinGang
	* @time: 2017年9月8日 上午11:49:05
	* @return: void 
	*/
	@Test
	public void testResetSendPrize() {
		try {
			ResultBO<?> resultBO = sendPrizeService.updateResetSendPrize("D1707011744470100052");
			System.out.println("接口返回：" + resultBO.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testHttps() throws Exception {
		String strUrl = "https://graph.qq.com/oauth2.0/me?access_token=7F719B72FC3BC9F1EBE629CF245F37A0&unionid=1";
		// List<String> ss = HttpUtils.URLGet(strUrl, null);
		// System.out.println(JSON.toJSONString(ss));

		System.out.println(HttpUtil.doGet(strUrl, null));
	}

	@Test
	public void testUpdateWalletToBuyRedColorForCms() throws Exception {
		CmsRechargeVO cmsRecharge = new CmsRechargeVO();
		cmsRecharge.setActivityCode("8888");
		cmsRecharge.setRechargeAmount(20d);
		cmsRecharge.setRechargeCode("I17112216045007100010");
		cmsRecharge.setUserId(1);
		ResultBO<?> resultBO = rechargeService.updateWalletToBuyRedColorForCms(cmsRecharge);
		System.out.println(resultBO.getMessage());
	}

}
