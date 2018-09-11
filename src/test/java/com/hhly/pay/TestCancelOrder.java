package com.hhly.pay;

import javax.annotation.Resource;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.hhly.paycore.service.CancellationRefundOrderGroupService;
import com.hhly.paycore.service.CancellationRefundService;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.mq.OrderCancelMsgModel;

public class TestCancelOrder extends BaseTest {

	@Resource
	private CancellationRefundService cancellationRefundService;
	@Resource
	private CancellationRefundOrderGroupService cancellationRefundOrderGroupService;
	String key = "1login_user_info_key";

	/**  
	* 方法说明: 代购撤单
	* @auth: xiongJinGang
	* @time: 2017年7月25日 下午7:13:33
	* @return: void 
	*/
	@Test
	public void testCancel() {
		try {
			Short orderType = 1;
			OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
			// 下面这个订单，使用的是彩金红包
			// cancellationRefundBO.setOrderCode("D1707011744470100052");// 订单编号号或追号计划编号
			// 下面这个订单，使用的是满减红包
			cancellationRefundBO.setOrderCode("D1706141711510100016");//

			cancellationRefundBO.setOrderType(orderType);// 订单类型；1-代购订单；2-追号计划
			// cancellationRefundBO.setRefundType();
			cancellationRefundBO.setRefundAmount("50");// 退款金额

			ResultBO<?> resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
			System.out.println(JSON.toJSONString(resultBO));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// 追号 【系统撤单】
	@Test
	public void testSysCancel() {
		try {
			Short orderType = 2;
			OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
			cancellationRefundBO.setOrderCode("JZ17120211403016600018");// 订单编号号或追号计划编号
			cancellationRefundBO.setOrderType(orderType);// 订单类型；1-代购订单；2-追号计划
			cancellationRefundBO.setRefundAmount("8");// 退款金额

			cancellationRefundBO.setIssueCode("");// 期号 （追号单期退款用到）
			cancellationRefundBO.setRefundType("1");// orderType=2时，分3种退款类型；1-中奖停追退款;2-单期撤单退款;3-用户撤单退款

			ResultBO<?> resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
			System.out.println(JSON.toJSONString(resultBO));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 追号【用户撤单】
	@Test
	public void testUserCancel() {
		try {
			Short orderType = 2;
			OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
			cancellationRefundBO.setOrderCode("JZ17082615262116600023");// 订单编号号或追号计划编号
			cancellationRefundBO.setOrderType(orderType);// 订单类型；1-代购订单；2-追号计划
			cancellationRefundBO.setRefundAmount("2");// 退款金额
			cancellationRefundBO.setIssueCode("");// 期号 （追号单期退款用到）
			cancellationRefundBO.setRefundType("3");// orderType=2时，分3种退款类型；1-中奖停追退款;2-单期撤单退款;3-用户撤单退款

			ResultBO<?> resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
			System.out.println(JSON.toJSONString(resultBO));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*##########################CMS	执行撤单#############################*/
	// 追号 【系统撤单】
	@Test
	public void testCMSSysCancel() {
		try {
			Short orderType = 2;// 订单类型；1-代购订单；2-追号计划
			String orderCode = "JZ17111515034116600004";// 订单号
			String refundAmount = "10,8";// 退款金额
			String refundType = "null";// 退款类型

			OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
			cancellationRefundBO.setOrderCode(orderCode);// 订单编号号或追号计划编号
			cancellationRefundBO.setOrderType(orderType);// 订单类型；1-代购订单；2-追号计划
			cancellationRefundBO.setRefundAmount(refundAmount);// 退款金额

			// cancellationRefundBO.setIssueCode("");// 期号 （追号单期退款用到）
			cancellationRefundBO.setRefundType(refundType);// orderType=2时，分3种退款类型；1-中奖停追退款;2-单期撤单退款;3-用户撤单退款

			ResultBO<?> resultBO = cancellationRefundService.doCancellation(cancellationRefundBO);
			System.out.println(JSON.toJSONString(resultBO));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*##########################合买撤单#############################*/
	/*合买退款类型，只有是合买订单退款时，才有值；代购、追号订单退款时为空
	1.未满员，合买流产退款处理（全退）
	2.未满员，平台保底认购账户处理（平台垫钱，扣平台账户的钱，然后加交易记录，http调用）
	3.满员，退发起人的保底账户金额的处理
	4.系统发起的合买退款处理（同第一条）
	5.合买单出票失败的退款处理（同第一条）*/
	@Test
	public void testOrderGroupCancel() {
		try {
			OrderCancelMsgModel cancellationRefundBO = new OrderCancelMsgModel();
			cancellationRefundBO.setOrderCode("H18053016091017200091");// 订单编号号或追号计划编号
			cancellationRefundBO.setOrderType((short) 3);// 订单类型；1-代购订单；2-追号计划; 3-合买订单
			cancellationRefundBO.setBuyTogetherRefundType((short) 5);
//			 cancellationRefundBO.setRefundAmount("3");
//			 cancellationRefundBO.setBuyCode("OG18052417023108201118");

			ResultBO<?> resultBO = cancellationRefundOrderGroupService.doCancellation(cancellationRefundBO);
			System.out.println(JSON.toJSONString(resultBO));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
