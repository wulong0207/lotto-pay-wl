package com.hhly.paycore.po;

import java.io.Serializable;
import java.util.Date;

import com.hhly.skeleton.user.bo.UserWalletBO;

/**
 * @desc 会员钱包
 * @author xiongjingang
 * @date 2017年3月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class UserWalletPO implements Serializable {
	private static final long serialVersionUID = 4344410876325896873L;
	/**
	 * 自增Id
	 */
	private Integer id;
	/**
	 * 用户Id
	 */
	private Integer userId;
	/**
	 * 现金总余额
	 */
	private Double totalCashBalance;
	/**
	 * 中奖余额
	 */
	private Double winningBalance;
	/**
	 * 充值80%余额
	 */
	private Double top80Balance;
	/**
	 * 充值20%余额
	 */
	private Double top20Balance;
	/**
	 * 可用红包余额
	 */
	private Double effRedBalance;
	/**
	 * 待发红包余额
	 */
	private Double readyRedBalance;
	/**
	 * 0：禁用中奖余额；1：启用
	 */
	private Short status;
	/**
	 * 更新时间
	 */
	private Date updateTime;
	/**
	 * 创建时间
	 */
	private Date createTime;
	/**
	 * 总现金余额+中奖余额（status可用，才加中奖余额）
	 */
	private Double totalAmount;
	/**
	 * 版本号
	 */
	private Integer version;
	/**
	 *【参数传递】、使用80%金额
	 */
	private Double use80Balance = 0d;
	/**
	 *【参数传递】、使用20%金额
	 */
	private Double use20Balance = 0d;
	/**
	 *【参数传递】、使用中奖金额
	 */
	private Double useWinBalance = 0d;
	/**
	 *【参数传递】、原有总金额
	 */
	private Double oriTotalBalance = 0d;

	public UserWalletPO() {
		super();
	}

	public UserWalletPO(UserWalletBO userWalletBO) {
		super();
		this.id = userWalletBO.getId();
		this.userId = userWalletBO.getUserId();
		this.totalCashBalance = userWalletBO.getTotalCashBalance();
		this.winningBalance = userWalletBO.getWinningBalance();
		this.top80Balance = userWalletBO.getTop80Balance();
		this.top20Balance = userWalletBO.getTop20Balance();
		this.effRedBalance = userWalletBO.getEffRedBalance();
		this.readyRedBalance = userWalletBO.getReadyRedBalance();
		this.status = userWalletBO.getStatus();
		this.totalAmount = userWalletBO.getTotalAmount();
		this.version = userWalletBO.getVersion();
		this.oriTotalBalance = userWalletBO.getTotalCashBalance();
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

	public Double getTotalCashBalance() {
		return totalCashBalance;
	}

	public void setTotalCashBalance(Double totalCashBalance) {
		this.totalCashBalance = totalCashBalance;
	}

	public Double getWinningBalance() {
		return winningBalance;
	}

	public void setWinningBalance(Double winningBalance) {
		this.winningBalance = winningBalance;
	}

	public Double getTop80Balance() {
		return top80Balance;
	}

	public void setTop80Balance(Double top80Balance) {
		this.top80Balance = top80Balance;
	}

	public Double getTop20Balance() {
		return top20Balance;
	}

	public void setTop20Balance(Double top20Balance) {
		this.top20Balance = top20Balance;
	}

	public Double getEffRedBalance() {
		return effRedBalance;
	}

	public void setEffRedBalance(Double effRedBalance) {
		this.effRedBalance = effRedBalance;
	}

	public Double getReadyRedBalance() {
		return readyRedBalance;
	}

	public void setReadyRedBalance(Double readyRedBalance) {
		this.readyRedBalance = readyRedBalance;
	}

	public Short getStatus() {
		return status;
	}

	public void setStatus(Short status) {
		this.status = status;
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

	public Double getTotalAmount() {
		return totalAmount;
	}

	public void setTotalAmount(Double totalAmount) {
		this.totalAmount = totalAmount;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Double getUse80Balance() {
		return use80Balance;
	}

	public void setUse80Balance(Double use80Balance) {
		this.use80Balance = use80Balance;
	}

	public Double getUse20Balance() {
		return use20Balance;
	}

	public void setUse20Balance(Double use20Balance) {
		this.use20Balance = use20Balance;
	}

	public Double getUseWinBalance() {
		return useWinBalance;
	}

	public void setUseWinBalance(Double useWinBalance) {
		this.useWinBalance = useWinBalance;
	}

	public Double getOriTotalBalance() {
		return oriTotalBalance;
	}

	public void setOriTotalBalance(Double oriTotalBalance) {
		this.oriTotalBalance = oriTotalBalance;
	}

	@Override
	public String toString() {
		return "UserWalletPO [userId=" + userId + ", totalCashBalance=" + totalCashBalance + ", winningBalance=" + winningBalance + ", top80Balance=" + top80Balance + ", top20Balance=" + top20Balance + ", effRedBalance="
				+ effRedBalance + ", readyRedBalance=" + readyRedBalance + ", status=" + status + ", totalAmount=" + totalAmount + ", version=" + version + ", use80Balance=" + use80Balance + ", use20Balance="
				+ use20Balance + ", useWinBalance=" + useWinBalance + "]";
	}

}