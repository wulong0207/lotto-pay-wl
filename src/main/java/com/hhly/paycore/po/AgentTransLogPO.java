package com.hhly.paycore.po;

import java.util.Date;

import com.hhly.skeleton.base.bo.BaseBO;
import com.hhly.skeleton.pay.agent.vo.ConfirmTakenResultVO;

@SuppressWarnings("serial")
public class AgentTransLogPO extends BaseBO {
	private Integer id;
	private Integer agentId;
	private String transCode;
	private Short transType;
	private String transInfo;
	private Date transEndTime;
	private Double transAmount;
	private Short transStatus;
	private Double serviceCharge;
	private Double taxCharge;
	private Double realAmount;// 实到金额
	private Double totalCashBalance;
	private String tradeCode;
	private String createBy;
	private Date createTime;
	private String updateBy;
	private Date updateTime;
	private String remark;

	public AgentTransLogPO() {
		super();
	}

	public AgentTransLogPO(ConfirmTakenResultVO confirmTakenResult) {
		super();
		this.agentId = confirmTakenResult.getAgentId();
		this.transAmount = confirmTakenResult.getTakenAmount();// 交易总金额
		this.serviceCharge = confirmTakenResult.getFee();
		this.taxCharge = confirmTakenResult.getTax();
		this.totalCashBalance = confirmTakenResult.getBalance();
		this.realAmount = confirmTakenResult.getArrivalAmount();
		this.createBy = confirmTakenResult.getAgentCode();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getAgentId() {
		return agentId;
	}

	public void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}

	public String getTransCode() {
		return transCode;
	}

	public void setTransCode(String transCode) {
		this.transCode = transCode;
	}

	public Short getTransType() {
		return transType;
	}

	public void setTransType(Short transType) {
		this.transType = transType;
	}

	public String getTransInfo() {
		return transInfo;
	}

	public void setTransInfo(String transInfo) {
		this.transInfo = transInfo;
	}

	public Date getTransEndTime() {
		return transEndTime;
	}

	public void setTransEndTime(Date transEndTime) {
		this.transEndTime = transEndTime;
	}

	public Double getTransAmount() {
		return transAmount;
	}

	public void setTransAmount(Double transAmount) {
		this.transAmount = transAmount;
	}

	public Short getTransStatus() {
		return transStatus;
	}

	public void setTransStatus(Short transStatus) {
		this.transStatus = transStatus;
	}

	public Double getServiceCharge() {
		return serviceCharge;
	}

	public void setServiceCharge(Double serviceCharge) {
		this.serviceCharge = serviceCharge;
	}

	public Double getTaxCharge() {
		return taxCharge;
	}

	public void setTaxCharge(Double taxCharge) {
		this.taxCharge = taxCharge;
	}

	public Double getTotalCashBalance() {
		return totalCashBalance;
	}

	public void setTotalCashBalance(Double totalCashBalance) {
		this.totalCashBalance = totalCashBalance;
	}

	public String getTradeCode() {
		return tradeCode;
	}

	public void setTradeCode(String tradeCode) {
		this.tradeCode = tradeCode;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public String getUpdateBy() {
		return updateBy;
	}

	public void setUpdateBy(String updateBy) {
		this.updateBy = updateBy;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Double getRealAmount() {
		return realAmount;
	}

	public void setRealAmount(Double realAmount) {
		this.realAmount = realAmount;
	}

}