package com.hhly.paycore.paychannel.sandpay.rsp;

/**
 * @desc 六度支付返回结果
 * @author xiongJinGang
 * @date 2018年6月27日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class SandPayBody {
	private String clearDate;// 20180612,
	private String oriTradeNo;// 2018061210292704270430346020,
	private String payTime;// 20180612102928,
	private String orderStatus;// 1,
	private String totalAmount;// 000000000001,
	private String buyerPayAmount;// 000000000001,
	private String oriOrderCode;// 20180612516

	public String getClearDate() {
		return clearDate;
	}

	public void setClearDate(String clearDate) {
		this.clearDate = clearDate;
	}

	public String getOriTradeNo() {
		return oriTradeNo;
	}

	public void setOriTradeNo(String oriTradeNo) {
		this.oriTradeNo = oriTradeNo;
	}

	public String getPayTime() {
		return payTime;
	}

	public void setPayTime(String payTime) {
		this.payTime = payTime;
	}

	public String getOrderStatus() {
		return orderStatus;
	}

	public void setOrderStatus(String orderStatus) {
		this.orderStatus = orderStatus;
	}

	public String getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(String totalAmount) {
		this.totalAmount = totalAmount;
	}

	public String getBuyerPayAmount() {
		return buyerPayAmount;
	}

	public void setBuyerPayAmount(String buyerPayAmount) {
		this.buyerPayAmount = buyerPayAmount;
	}

	public String getOriOrderCode() {
		return oriOrderCode;
	}

	public void setOriOrderCode(String oriOrderCode) {
		this.oriOrderCode = oriOrderCode;
	}

}
