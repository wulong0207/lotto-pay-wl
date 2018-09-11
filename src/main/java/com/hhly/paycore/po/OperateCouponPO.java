package com.hhly.paycore.po;

import com.hhly.skeleton.pay.bo.OperateCouponBO;
import com.hhly.skeleton.pay.vo.OperateCouponVO;

import java.io.Serializable;
import java.util.Date;

/**
 * @desc 运营管理的优惠券详情（彩金）
 * @author xiongJinGang
 * @date 2017年3月21日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class OperateCouponPO implements Serializable {
	private static final long serialVersionUID = 8758520266400923107L;
	/**
	 * 自增Id
	 */
	private Integer id;
	/**
	 * 自定义编号
	 */
	private String redCode;
	/**
	 * 红包类别 1：实体；2：虚拟 
	 */
	private Short redCategory;
	/**
	 * 活动管理编号
	 */
	private String activityCode;
	/**
	 * 关联彩种运营管理ID，list 
	 */
	private String operateLotteryId;
	/**
	 * 激活截止时间 
	 */
	private Date activeEndTime;
	/**
	 * 用户获取红包的时间 
	 */
	private Date obtainTime;
	/**
	 * 获取时间+有效天数 = 红包过期时间。
	 */
	private Date redOverdueTime;
	/**
	 * 使用时间 
	 */
	private Date useTime;
	/**
	 * 1：充值红包；2：满减红包；3：彩金红包；4：加奖红包；5：大礼包；6：随机红包
	 */
	private Short redType;
	/**
	 * 红包名称；按照指定规则生成；根据红包类型，红包面额，最低消费生成红包名称。 
	 */
	private String redName;
	/**
	 * 红包实际金额 
	 */
	private Double redValue;
	/**
	 * 红包余额 
	 */
	private Double redBalance;
	/**
	 * 使用红包的条件。根据订单金额计算。 
	 */
	private Integer minSpendAmount;
	/**
	 * 所属用户。user_id 
	 */
	private Integer userId;
	/**
	 * 用户获取红包后的有效天数。 
	 */
	private Integer ectivityDay;
	/**
	 * 渠道ID，支持多个
	 */
	private String channelId;
	/**
	 * 可自定义的红包标签。 
	 */
	private String redLabel;
	/**
	 * 1：待激活；2：待派发；3：可使用；4：已过期；5：已作废；6：已使用 
	 */
	private String redStatus;
	/**
	 * 1：主站Web专用；2：主站Wap专用；3：合作版Wap专用；4：客户端专用；5：API接口专用；6：其它平台专用； 支持多个
	 */
	private String limitPlatform;
	/**
	 * lottery_id；多选，list；用逗号隔开 
	 */
	private String limitLottery;
	/**
	 * 自定义的说明。根据类型，面值，最低消费金额，平台，彩种，根据文档进行判断是否可用。规则为固定的。 
	 */
	private String useRule;
	/**
	 * 红包备注 
	 */
	private String redRemark;
	/**
	 * 修改时间 
	 */
	private Date modifyTime;
	/**
	 * 修改人 
	 */
	private String modifyBy;
	/**
	 * 创建人 
	 */
	private String createBy;
	/**
	 * 更新时间 
	 */
	private Date updateTime;
	/**
	 * 创建时间 
	 */
	private Date createTime;
	/**
	 * 修改备注
	 */
	private String remark;
	/**
	 * 来源类型:1：活动；2：券
	 */
	private Short redSource;
	/**
	 * 限制红包子玩法
	 */
	private String limitLotteryChild;
	/**
	 * 限制红包玩法类型
	 */
	private String limitLotteryChildType;
	/**
	 * 随机红包生成红包类型为：1.彩金红包2.满减红包3.充值红包4. 加奖红包
	 */
	private Short randomRedType;

	public OperateCouponPO() {
		super();
	}

	public OperateCouponPO(OperateCouponBO operateCouponBO) {
		super();
		this.redCode = operateCouponBO.getRedCode();
		this.redCategory = operateCouponBO.getRedCategory();
		this.activityCode = operateCouponBO.getActivityCode();
		this.operateLotteryId = operateCouponBO.getOperateLotteryId();
		this.activeEndTime = operateCouponBO.getActiveEndTime();
		this.obtainTime = operateCouponBO.getObtainTime();
		this.redOverdueTime = operateCouponBO.getRedOverdueTime();
		this.useTime = operateCouponBO.getUseTime();
		this.redType = operateCouponBO.getRedType();
		this.redName = operateCouponBO.getRedName();
		this.redValue = operateCouponBO.getRedValue();
		this.redBalance = operateCouponBO.getRedBalance();
		this.minSpendAmount = operateCouponBO.getMinSpendAmount();
		this.userId = operateCouponBO.getUserId();
		this.ectivityDay = operateCouponBO.getEctivityDay();
		this.channelId = operateCouponBO.getChannelId();
		this.redLabel = operateCouponBO.getRedLabel();
		this.redStatus = operateCouponBO.getRedStatus();
		this.limitPlatform = operateCouponBO.getLimitPlatform();
		this.limitLottery = operateCouponBO.getLimitLottery();
		this.useRule = operateCouponBO.getUseRule();
		this.redRemark = operateCouponBO.getRedRemark();
		this.modifyTime = operateCouponBO.getModifyTime();
		this.modifyBy = operateCouponBO.getModifyBy();
		this.createBy = operateCouponBO.getCreateBy();
		this.remark = operateCouponBO.getRemark();
		this.redSource = operateCouponBO.getRedSource();
		this.limitLotteryChild = operateCouponBO.getLimitLotteryChild();
		this.limitLotteryChildType = operateCouponBO.getLimitLotteryChildType();
		this.randomRedType = operateCouponBO.getRandomRedType();
	}

	public OperateCouponPO(OperateCouponVO vo) {
		super();
		this.id = vo.getId();
		this.redCode = vo.getRedCode();
		this.redCategory = vo.getRedCategory();
		this.activityCode = vo.getActivityCode();
		this.operateLotteryId = vo.getOperateLotteryId();
		this.activeEndTime = vo.getActiveEndTime();
		this.obtainTime = vo.getObtainTime();
		this.redOverdueTime = vo.getRedOverdueTime();
		this.useTime = vo.getUseTime();
		this.redType = vo.getRedType();
		this.redName = vo.getRedName();
		this.redValue = vo.getRedValue();
		this.redBalance = vo.getRedBalance();
		this.minSpendAmount = vo.getMinSpendAmount();
		this.userId = vo.getUserId();
		this.ectivityDay = vo.getEctivityDay();
		this.channelId = vo.getChannelId();
		this.redLabel = vo.getRedLabel();
		this.redStatus = vo.getRedStatus();
		this.limitPlatform = vo.getLimitPlatform();
		this.limitLottery = vo.getLimitLottery();
		this.useRule = vo.getUseRule();
		this.redRemark = vo.getRedRemark();
		this.modifyTime = vo.getModifyTime();
		this.modifyBy = vo.getModifyBy();
		this.createBy = vo.getCreateBy();
		this.updateTime = vo.getUpdateTime();
		this.createTime = vo.getCreateTime();
		this.remark = vo.getRemark();
		this.redSource = vo.getRedSource();
		this.limitLotteryChild = vo.getLimitLotteryChild();
		this.limitLotteryChildType = vo.getLimitLotteryChildType();
		this.randomRedType = vo.getRandomRedType();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getRedCode() {
		return redCode;
	}

	public void setRedCode(String redCode) {
		this.redCode = redCode;
	}

	public Short getRedCategory() {
		return redCategory;
	}

	public void setRedCategory(Short redCategory) {
		this.redCategory = redCategory;
	}

	public String getActivityCode() {
		return activityCode;
	}

	public void setActivityCode(String activityCode) {
		this.activityCode = activityCode;
	}

	public String getOperateLotteryId() {
		return operateLotteryId;
	}

	public void setOperateLotteryId(String operateLotteryId) {
		this.operateLotteryId = operateLotteryId;
	}

	public Date getActiveEndTime() {
		return activeEndTime;
	}

	public void setActiveEndTime(Date activeEndTime) {
		this.activeEndTime = activeEndTime;
	}

	public Date getObtainTime() {
		return obtainTime;
	}

	public void setObtainTime(Date obtainTime) {
		this.obtainTime = obtainTime;
	}

	public Date getRedOverdueTime() {
		return redOverdueTime;
	}

	public void setRedOverdueTime(Date redOverdueTime) {
		this.redOverdueTime = redOverdueTime;
	}

	public Date getUseTime() {
		return useTime;
	}

	public void setUseTime(Date useTime) {
		this.useTime = useTime;
	}

	public Short getRedType() {
		return redType;
	}

	public void setRedType(Short redType) {
		this.redType = redType;
	}

	public String getRedName() {
		return redName;
	}

	public void setRedName(String redName) {
		this.redName = redName;
	}

	public Double getRedValue() {
		return redValue;
	}

	public void setRedValue(Double redValue) {
		this.redValue = redValue;
	}

	public Double getRedBalance() {
		return redBalance;
	}

	public void setRedBalance(Double redBalance) {
		this.redBalance = redBalance;
	}

	public Integer getMinSpendAmount() {
		return minSpendAmount;
	}

	public void setMinSpendAmount(Integer minSpendAmount) {
		this.minSpendAmount = minSpendAmount;
	}

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Integer getEctivityDay() {
		return ectivityDay;
	}

	public void setEctivityDay(Integer ectivityDay) {
		this.ectivityDay = ectivityDay;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getRedLabel() {
		return redLabel;
	}

	public void setRedLabel(String redLabel) {
		this.redLabel = redLabel;
	}

	public String getRedStatus() {
		return redStatus;
	}

	public void setRedStatus(String redStatus) {
		this.redStatus = redStatus;
	}

	public String getLimitPlatform() {
		return limitPlatform;
	}

	public void setLimitPlatform(String limitPlatform) {
		this.limitPlatform = limitPlatform;
	}

	public String getLimitLottery() {
		return limitLottery;
	}

	public void setLimitLottery(String limitLottery) {
		this.limitLottery = limitLottery;
	}

	public String getUseRule() {
		return useRule;
	}

	public void setUseRule(String useRule) {
		this.useRule = useRule;
	}

	public String getRedRemark() {
		return redRemark;
	}

	public void setRedRemark(String redRemark) {
		this.redRemark = redRemark;
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

	public String getLimitLotteryChild() {
		return limitLotteryChild;
	}

	public void setLimitLotteryChild(String limitLotteryChild) {
		this.limitLotteryChild = limitLotteryChild;
	}

	public String getLimitLotteryChildType() {
		return limitLotteryChildType;
	}

	public void setLimitLotteryChildType(String limitLotteryChildType) {
		this.limitLotteryChildType = limitLotteryChildType;
	}

	public Short getRandomRedType() {
		return randomRedType;
	}

	public void setRandomRedType(Short randomRedType) {
		this.randomRedType = randomRedType;
	}

	public Short getRedSource() {
		return redSource;
	}

	public void setRedSource(Short redSource) {
		this.redSource = redSource;
	}

	@Override
	public String toString() {
		return "OperateCouponPO{" +
				"redCode='" + redCode + '\'' +
				", activityCode='" + activityCode + '\'' +
				", redName='" + redName + '\'' +
				", redValue=" + redValue +
				", redBalance=" + redBalance +
				", userId=" + userId +
				'}';
	}
}