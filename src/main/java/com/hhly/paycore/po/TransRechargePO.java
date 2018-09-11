package com.hhly.paycore.po;

import java.io.Serializable;
import java.util.Date;

import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.pay.vo.RechargeUpdateVO;
import com.hhly.skeleton.user.bo.UserInfoBO;

/**
 * @desc 用户充值管理
 * @author xiongjingang
 * @date 2017年3月1日 下午3:40:27
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class TransRechargePO implements Serializable {
	private static final long serialVersionUID = -7368002328260963842L;
	/**
	 *自增长主键ID
	 */
	private Integer id;
	/**
	*自定义用户充值流水编号
	*/
	private String transRechargeCode;
	/**
	*1：支付宝充值；2：微信支付；3：练练支付；4：百度支付；5：人工充值 ；6：代理系统充值
	*/
	private Short rechargeChannel;
	/**
	*1：快捷支付；2：网银支付；3：其它支付；
	*/
	private Short payType;
	/**
	*1：农商银行；2：光大银行；3：交通银行；4：平安银行；5：农业银行；5：中信银行；6：广发银行；7：华夏银行；8：浦发银行；9：民生银行；10：建设银行；11：中国银行；12：工商银行；13：邮储银行；14：招商银行；15：兴业银行；
	*/
	private Integer rechargeBank;
	/**
	*1：储蓄卡；2：信用卡；3：其它
	*/
	private Short bankCardType;
	/**
	*银行卡号
	*/
	private String bankCardNum;
	/**
	*交易时间
	*/
	private Date transTime;
	/**
	*交易结束时间
	*/
	private Date transEndTime;
	/**
	*第三方交易时间
	*/
	private Date thirdTransTime;
	/**
	*请求响应时间
	*/
	private Date responseTime;
	/**
	*用户ID
	*/
	private Integer userId;
	/**
	*充值金额
	*/
	private Double rechargeAmount;
	/**
	*到账金额
	*/
	private Double arrivalAmount;
	/**
	*服务费
	*/
	private Double serviceCharge;
	/**
	*充值描述
	*/
	private String rechargeRemark;
	/**
	*1：进行中；2：交易成功；3：交易失败；4：订单已关闭；
	*/
	private Short transStatus;
	/**
	*1：本站WEB；2：本站WAP；3：Android客户端；4：IOS客户端；5：未知；
	*/
	private Short rechargePlatform;
	/**
	*市场渠道ID
	*/
	private String channelId;
	/**
	*第三方流水号
	*/
	private String thirdTransNum;
	/**
	*红包金额
	*/
	private Double redAmount;
	/**
	*补单人
	*/
	private String supplementBy;
	/**
	*修改时间
	*/
	private Date modifyTime;
	/**
	*修改人
	*/
	private String modifyBy;
	/**
	*创建人
	*/
	private String createBy;
	/**
	*更新时间
	*/
	private Date updateTime;
	/**
	*创建时间
	*/
	private Date createTime;
	/**
	*描述
	*/
	private String remark;
	/**
	*订单号
	*/
	private String orderCode;
	/**
	 * 红包code
	 */
	private String redCode;
	/**
	 *支付渠道编号
	 */
	private String channelCode;
	/**
	 * 实际进入用户钱包中的金额
	 */
	private Double inWallet;
	private Short switchStatus;// 网银和快捷切换状态:0不切换;1切换

	private String activityCode;// 活动编号
	/**
	 * 是否可提款状态:0不可提、1可提 、2已提
	 */
	private Short takenStatus;
	/**
	 *  0充值、1即买即付，默认0
	 */
	private Short rechargeType;
	private Integer payChannelId;// 支付渠道Id
	private Double groupAmount;// 合买余额支付金额

	public TransRechargePO() {
		super();
	}

	public TransRechargePO(UserInfoBO userInfo) {
		this.userId = userInfo.getId();// 用户Id
		this.createBy = userInfo.getRealName();// 创建人
		this.transStatus = PayConstants.TransStatusEnum.TRADE_UNDERWAY.getKey();// 进行中
	}

	public TransRechargePO(RechargeUpdateVO rechargeUpdate) {
		this.arrivalAmount = rechargeUpdate.getArrivalAmount();
		this.transStatus = rechargeUpdate.getTransStatus();
		this.thirdTransNum = rechargeUpdate.getThirdTransNum();
	}

	public TransRechargePO(Integer id, Short takenStatus) {
		super();
		this.id = id;
		this.takenStatus = takenStatus;
	}

	public TransRechargePO(String transRechargeCode, Short takenStatus) {
		super();
		this.transRechargeCode = transRechargeCode;
		this.takenStatus = takenStatus;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTransRechargeCode() {
		return transRechargeCode;
	}

	public void setTransRechargeCode(String transRechargeCode) {
		this.transRechargeCode = transRechargeCode;
	}

	public Short getRechargeChannel() {
		return rechargeChannel;
	}

	public void setRechargeChannel(Short rechargeChannel) {
		this.rechargeChannel = rechargeChannel;
	}

	public Short getPayType() {
		return payType;
	}

	public void setPayType(Short payType) {
		this.payType = payType;
	}

	public Integer getRechargeBank() {
		return rechargeBank;
	}

	public void setRechargeBank(Integer rechargeBank) {
		this.rechargeBank = rechargeBank;
	}

	public Short getBankCardType() {
		return bankCardType;
	}

	public void setBankCardType(Short bankCardType) {
		this.bankCardType = bankCardType;
	}

	public String getBankCardNum() {
		return bankCardNum;
	}

	public void setBankCardNum(String bankCardNum) {
		this.bankCardNum = bankCardNum;
	}

	public Date getTransTime() {
		return transTime;
	}

	public void setTransTime(Date transTime) {
		this.transTime = transTime;
	}

	public Date getTransEndTime() {
		return transEndTime;
	}

	public void setTransEndTime(Date transEndTime) {
		this.transEndTime = transEndTime;
	}

	public Date getThirdTransTime() {
		return thirdTransTime;
	}

	public void setThirdTransTime(Date thirdTransTime) {
		this.thirdTransTime = thirdTransTime;
	}

	public Date getResponseTime() {
		return responseTime;
	}

	public void setResponseTime(Date responseTime) {
		this.responseTime = responseTime;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Double getRechargeAmount() {
		return rechargeAmount;
	}

	public void setRechargeAmount(Double rechargeAmount) {
		this.rechargeAmount = rechargeAmount;
	}

	public Double getArrivalAmount() {
		return arrivalAmount;
	}

	public void setArrivalAmount(Double arrivalAmount) {
		this.arrivalAmount = arrivalAmount;
	}

	public Double getServiceCharge() {
		return serviceCharge;
	}

	public void setServiceCharge(Double serviceCharge) {
		this.serviceCharge = serviceCharge;
	}

	public String getRechargeRemark() {
		return rechargeRemark;
	}

	public void setRechargeRemark(String rechargeRemark) {
		this.rechargeRemark = rechargeRemark;
	}

	public Short getTransStatus() {
		return transStatus;
	}

	public void setTransStatus(Short transStatus) {
		this.transStatus = transStatus;
	}

	public Short getRechargePlatform() {
		return rechargePlatform;
	}

	public void setRechargePlatform(Short rechargePlatform) {
		this.rechargePlatform = rechargePlatform;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getThirdTransNum() {
		return thirdTransNum;
	}

	public void setThirdTransNum(String thirdTransNum) {
		this.thirdTransNum = thirdTransNum;
	}

	public Double getRedAmount() {
		return redAmount;
	}

	public void setRedAmount(Double redAmount) {
		this.redAmount = redAmount;
	}

	public String getSupplementBy() {
		return supplementBy;
	}

	public void setSupplementBy(String supplementBy) {
		this.supplementBy = supplementBy;
	}

	public Date getModifyTime() {
		return modifyTime;
	}

	public void setModifyTime(Date modifyTime) {
		this.modifyTime = modifyTime;
	}

	public String getModifyBy() {
		return modifyBy;
	}

	public void setModifyBy(String modifyBy) {
		this.modifyBy = modifyBy;
	}

	public String getCreateBy() {
		return createBy;
	}

	public void setCreateBy(String createBy) {
		this.createBy = createBy;
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

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getOrderCode() {
		return orderCode;
	}

	public void setOrderCode(String orderCode) {
		this.orderCode = orderCode;
	}

	public String getRedCode() {
		return redCode;
	}

	public void setRedCode(String redCode) {
		this.redCode = redCode;
	}

	public String getChannelCode() {
		return channelCode;
	}

	public void setChannelCode(String channelCode) {
		this.channelCode = channelCode;
	}

	public Double getInWallet() {
		return inWallet;
	}

	public void setInWallet(Double inWallet) {
		this.inWallet = inWallet;
	}

	public Short getSwitchStatus() {
		return switchStatus;
	}

	public void setSwitchStatus(Short switchStatus) {
		this.switchStatus = switchStatus;
	}

	public String getActivityCode() {
		return activityCode;
	}

	public void setActivityCode(String activityCode) {
		this.activityCode = activityCode;
	}

	public Short getTakenStatus() {
		return takenStatus;
	}

	public void setTakenStatus(Short takenStatus) {
		this.takenStatus = takenStatus;
	}

	public Short getRechargeType() {
		return rechargeType;
	}

	public void setRechargeType(Short rechargeType) {
		this.rechargeType = rechargeType;
	}

	public Integer getPayChannelId() {
		return payChannelId;
	}

	public void setPayChannelId(Integer payChannelId) {
		this.payChannelId = payChannelId;
	}

	public Double getGroupAmount() {
		return groupAmount;
	}

	public void setGroupAmount(Double groupAmount) {
		this.groupAmount = groupAmount;
	}

	@Override
	public String toString() {
		return "TransRechargePO [id=" + id + ", transRechargeCode=" + transRechargeCode + ", rechargeChannel=" + rechargeChannel + ", payType=" + payType + ", rechargeBank=" + rechargeBank + ", bankCardType=" + bankCardType + ", bankCardNum="
				+ bankCardNum + ", transTime=" + transTime + ", transEndTime=" + transEndTime + ", thirdTransTime=" + thirdTransTime + ", responseTime=" + responseTime + ", userId=" + userId + ", rechargeAmount=" + rechargeAmount + ", arrivalAmount="
				+ arrivalAmount + ", serviceCharge=" + serviceCharge + ", rechargeRemark=" + rechargeRemark + ", transStatus=" + transStatus + ", rechargePlatform=" + rechargePlatform + ", channelId=" + channelId + ", thirdTransNum=" + thirdTransNum
				+ ", redAmount=" + redAmount + ", supplementBy=" + supplementBy + ", modifyTime=" + modifyTime + ", modifyBy=" + modifyBy + ", createBy=" + createBy + ", updateTime=" + updateTime + ", createTime=" + createTime + ", remark=" + remark
				+ ", orderCode=" + orderCode + ", redCode=" + redCode + ", channelCode=" + channelCode + ", inWallet=" + inWallet + "]";
	}

}