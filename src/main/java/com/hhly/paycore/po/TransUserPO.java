package com.hhly.paycore.po;

import java.io.Serializable;
import java.util.Date;

/**
 * @desc 用户交易流水
 * @author xiongjingang
 * @date 2017年3月1日 下午12:17:07
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class TransUserPO implements Serializable {

	private static final long serialVersionUID = -2592177709083909661L;
	/**
	 *自增长ID
	 */
	private Integer id;
	/**
	 *用户表主键ID
	 */
	private Integer userId;
	/**
	 *固定规则的流水ID
	 */
	private String transCode;
	/**
	 *关联订单表的订单编号
	 */
	private String orderCode;
	/**
	 *1：充值；2：购彩；3：返奖；4：退款；5：提款；6：其它
	 */
	private Short transType;
	/**
	 *订单信息说明；有相应文档。
	 */
	private String orderInfo;
	/**
	 *交易结束时间
	 */
	private Date transEndTime;
	/**
	 *付款成功时入库时间。
	 */
	private Date transTime;
	/**
	 *第三方交易返回的时间
	 */
	private Date thirdTransTime;
	/**
	 *总的交易流水表trans_user，该字段表示【交易总金额；现金金额+红包金额+服务费】。给用户的交易流水表【trans_user_log】,该字段表示【交易总金额；现金金额+红包金额-服务费】，为什么要减，是供前端直接展示时使用
	 */
	private Double transAmount;
	/**
	 *实付现金金额
	 */
	private Double cashAmount;
	/**
	 *现金总余额
	 */
	private Double totalCashBalance;
	/**
	 *所用的红包消费总金额
	 */
	private Double redTransAmount;
	/**
	 *0：交易失败；1：交易成功；
	 */
	private Short transStatus;
	/**
	 *渠道表ID；取渠道ID，订单来源
	 */
	private String channelId;
	/**
	 *第三方返回的订单编号
	 */
	private String thirdTransId;
	/**
	 *服务费
	 */
	private Double serviceCharge = 0d;
	/**
	 *更新时间
	 */
	private Date updateTime;
	/**
	 *创建时间
	 */
	private Date createTime;
	/**
	 * 使用80%金额
	 */
	private Double amount80 = 0d;
	/**
	 * 使用20%金额
	 */
	private Double amount20 = 0d;
	/**
	 * 使用中奖金额
	 */
	private Double amountWin = 0d;
	/**
	 * 使用红包编号 2017-07-12 添加，用户撤单生成相应的红包
	 */
	private String redCode;
	/**
	 * 流水编号，可以是充值编号、提款编号等，存的是自己内部其它交易表的编号
	 */
	private String tradeCode;
	private String remark;// 描述

	/**
	 * 代理编码
	 */
	private String agentCode;
	private Short sourceType = 0;// 交易来源，默认0， 1直接充值、2即买即付、3人工充值、4代理充值 2017-11-08号添加
	/**
	 * 剩余红包总余额
	 */
	private Double totalRedBalance;
	private Short awardFlag;// 是否重置开派奖标记：0是正常派奖，1是重置派奖

	public TransUserPO() {
		super();
	}

	public TransUserPO(Integer userId, String transCode, Short transType, String orderInfo) {
		super();
		this.userId = userId;
		this.transCode = transCode;
		this.transType = transType;
		this.orderInfo = orderInfo;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public String getTransCode() {
		return transCode;
	}

	public void setTransCode(String transCode) {
		this.transCode = transCode;
	}

	public String getOrderCode() {
		return orderCode;
	}

	public void setOrderCode(String orderCode) {
		this.orderCode = orderCode;
	}

	public Short getTransType() {
		return transType;
	}

	public void setTransType(Short transType) {
		this.transType = transType;
	}

	public String getOrderInfo() {
		return orderInfo;
	}

	public void setOrderInfo(String orderInfo) {
		this.orderInfo = orderInfo;
	}

	public Date getTransEndTime() {
		return transEndTime;
	}

	public void setTransEndTime(Date transEndTime) {
		this.transEndTime = transEndTime;
	}

	public Date getTransTime() {
		return transTime;
	}

	public void setTransTime(Date transTime) {
		this.transTime = transTime;
	}

	public Date getThirdTransTime() {
		return thirdTransTime;
	}

	public void setThirdTransTime(Date thirdTransTime) {
		this.thirdTransTime = thirdTransTime;
	}

	public Double getTransAmount() {
		return transAmount;
	}

	public void setTransAmount(Double transAmount) {
		this.transAmount = transAmount;
	}

	public Double getCashAmount() {
		return cashAmount;
	}

	public void setCashAmount(Double cashAmount) {
		this.cashAmount = cashAmount;
	}

	public Double getRedTransAmount() {
		return redTransAmount;
	}

	public void setRedTransAmount(Double redTransAmount) {
		this.redTransAmount = redTransAmount;
	}

	public Short getTransStatus() {
		return transStatus;
	}

	public void setTransStatus(Short transStatus) {
		this.transStatus = transStatus;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getThirdTransId() {
		return thirdTransId;
	}

	public void setThirdTransId(String thirdTransId) {
		this.thirdTransId = thirdTransId;
	}

	public Double getServiceCharge() {
		return serviceCharge;
	}

	public void setServiceCharge(Double serviceCharge) {
		this.serviceCharge = serviceCharge;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

	public Double getTotalCashBalance() {
		return totalCashBalance;
	}

	public void setTotalCashBalance(Double totalCashBalance) {
		this.totalCashBalance = totalCashBalance;
	}

	public Double getAmount80() {
		return amount80;
	}

	public void setAmount80(Double amount80) {
		this.amount80 = amount80;
	}

	public Double getAmount20() {
		return amount20;
	}

	public void setAmount20(Double amount20) {
		this.amount20 = amount20;
	}

	public Double getAmountWin() {
		return amountWin;
	}

	public void setAmountWin(Double amountWin) {
		this.amountWin = amountWin;
	}

	public String getRedCode() {
		return redCode;
	}

	public void setRedCode(String redCode) {
		this.redCode = redCode;
	}

	public String getTradeCode() {
		return tradeCode;
	}

	public void setTradeCode(String tradeCode) {
		this.tradeCode = tradeCode;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getAgentCode() {
		return agentCode;
	}

	public void setAgentCode(String agentCode) {
		this.agentCode = agentCode;
	}

	public Short getSourceType() {
		return sourceType;
	}

	public void setSourceType(Short sourceType) {
		this.sourceType = sourceType;
	}

	public Double getTotalRedBalance() {
		return totalRedBalance;
	}

	public void setTotalRedBalance(Double totalRedBalance) {
		this.totalRedBalance = totalRedBalance;
	}

	public Short getAwardFlag() {
		return awardFlag;
	}

	public void setAwardFlag(Short awardFlag) {
		this.awardFlag = awardFlag;
	}

}