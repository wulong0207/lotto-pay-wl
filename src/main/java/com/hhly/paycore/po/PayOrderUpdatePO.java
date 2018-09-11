package com.hhly.paycore.po;

import java.util.Date;

import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.lotto.base.order.bo.OrderInfoBO;
import com.hhly.skeleton.pay.vo.PayOrderBaseInfoVO;

/**
 * @desc 支付订单的更新信息
 * @author xiongJinGang
 * @date 2017年3月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PayOrderUpdatePO {
	private Integer id;// 订单id
	private String orderCode;// 订单编号
	private Short payStatus;// 1：等待支付；2：支付成功；3：未支付过期；4：支付失败；5：用户取消；6：退款
	private String redCodeUsed;// 开奖后生成的优惠券中的红包编号ID(系统自动发放的红包编号ID)
	private String activitySource;// 活动表的活动ID
	private Date buyTime;// 购买时间
	private Short addStatus; // 追号状态
	private Short orderStatus; // 订单状态
	private String redCodeGet;// 开奖后生成的优惠券中的红包编号(系统自动发放的红包编号)

	public PayOrderUpdatePO() {
		super();
	}

	/**构造方法*/
	public PayOrderUpdatePO(PayOrderBaseInfoVO orderInfoBO) {
		super();
		this.id = orderInfoBO.getId();
		this.orderCode = orderInfoBO.getOrderCode();
		this.payStatus = orderInfoBO.getPayStatus();
	}

	/**支付成功的构造方法*/
	public PayOrderUpdatePO(OrderInfoBO orderInfoBO) {
		super();
		this.id = Integer.parseInt(orderInfoBO.getId() + "");
		this.orderCode = orderInfoBO.getOrderCode();
		this.payStatus = PayConstants.PayStatusEnum.PAYMENT_SUCCESS.getKey();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getOrderCode() {
		return orderCode;
	}

	public void setOrderCode(String orderCode) {
		this.orderCode = orderCode;
	}

	public Short getPayStatus() {
		return payStatus;
	}

	public void setPayStatus(Short payStatus) {
		this.payStatus = payStatus;
	}

	public String getRedCodeUsed() {
		return redCodeUsed;
	}

	public void setRedCodeUsed(String redCodeUsed) {
		this.redCodeUsed = redCodeUsed;
	}

	public String getActivitySource() {
		return activitySource;
	}

	public void setActivitySource(String activitySource) {
		this.activitySource = activitySource;
	}

	public Date getBuyTime() {
		return buyTime;
	}

	public void setBuyTime(Date buyTime) {
		this.buyTime = buyTime;
	}

	public Short getAddStatus() {
		return addStatus;
	}

	public void setAddStatus(Short addStatus) {
		this.addStatus = addStatus;
	}

	public Short getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(Short orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getRedCodeGet() {
		return redCodeGet;
	}

	public void setRedCodeGet(String redCodeGet) {
		this.redCodeGet = redCodeGet;
	}

}
