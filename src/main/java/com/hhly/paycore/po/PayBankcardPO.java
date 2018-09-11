package com.hhly.paycore.po;

import com.hhly.skeleton.base.constants.UserConstants;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户银行信息（对表）
 * @desc
 * @author chenkangning
 * @date 2017年3月2日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PayBankcardPO implements Serializable {

	private static final long serialVersionUID = -4382741329075913981L;
    
    public PayBankcardPO () {
        //默认值设置
        this.status = UserConstants.BankCardStatusEnum.EFFECTIVE.getKey();
    }
    
    /**
	 * ID主键
	 */
	private Integer id;

	/**
	 * 用户id
	 */
	private Integer userId;

	/**
	 * 真实姓名
	 */
	private String realname;

	/**
	 * 银行id
	 */
	private Integer bankid;

	/**
	 * 开户行名称
	 */
	private String bankname;

	/**
	 * 是默认卡 0：否，1：是
	 */
	private Short isdefault;

	/**
	 * 是否开启快捷支付 0：否，1：是
	 */
	private Short openbank;

	/**
	 * 省份
	 */
	private String province;

	/**
	 * 城市
	 */
	private String city;

	/**
	 * 银行卡号
	 */
	private String cardcode;

	/**
	 * 银行卡类型:1储蓄卡;2信用卡
	 */
	private Short banktype;

	/**
	 * 针对信用卡(有效期,年,月字串)
	 */
	private String overdue;

	/**
	 * 是否绑定 0：否，1：是
	 */
	private Short bindflag;

	/**
	 * 针对信用卡(安全码)
	 */
	private String safecode;

	/**
	 *  ip地址
	 */
	private String ip;

	/**
	 * null
	 */
	private String ext;

	/**
	 * 更新时间
	 */
	private Date updateTime;

	/**
	 * 创建时间
	 */
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date createTime;

	/**
	 * 卡状态:0:删除;1:有效
	 */
	private Integer status;
	 /**
     * 是否绑定 0：否，1：是
     */
    private Short bindFlag;

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

	public String getRealname () {
		return realname;
	}

	public void setRealname (String realname) {
		this.realname = realname;
	}

	public Integer getBankid() {
		return bankid;
	}

	public void setBankid(Integer bankid) {
		this.bankid = bankid;
	}

	public String getBankname() {
		return bankname;
	}

	public void setBankname(String bankname) {
		this.bankname = bankname;
	}

	public Short getIsdefault() {
		return isdefault;
	}

	public void setIsdefault(Short isdefault) {
		this.isdefault = isdefault;
	}

	public Short getOpenbank() {
		return openbank;
	}

	public void setOpenbank(Short openbank) {
		this.openbank = openbank;
	}

	public String getProvince() {
		return province;
	}

	public void setProvince(String province) {
		this.province = province;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getCardcode() {
		return cardcode;
	}

	public void setCardcode(String cardcode) {
		this.cardcode = cardcode;
	}

	public Short getBanktype() {
		return banktype;
	}

	public void setBanktype(Short banktype) {
		this.banktype = banktype;
	}

	public String getOverdue() {
		return overdue;
	}

	public void setOverdue(String overdue) {
		this.overdue = overdue;
	}

	public Short getBindflag() {
		return bindflag;
	}

	public void setBindflag(Short bindflag) {
		this.bindflag = bindflag;
	}

	public String getSafecode() {
		return safecode;
	}

	public void setSafecode(String safecode) {
		this.safecode = safecode;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getExt() {
		return ext;
	}

	public void setExt(String ext) {
		this.ext = ext;
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

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Short getBindFlag() {
		return bindFlag;
	}

	public void setBindFlag(Short bindFlag) {
		this.bindFlag = bindFlag;
	}

	@Override
	public String toString() {
		return "PayBankCardPO [userId=" + userId + ", realname=" + realname + ", bankid=" + bankid + ", bankname=" + bankname + ", isdefault=" + isdefault + ", openbank=" + openbank + ", province=" + province + ", city="
				+ city + ", cardcode=" + cardcode + ", banktype=" + banktype + ", overdue=" + overdue + ", bindflag=" + bindflag + ", safecode=" + safecode + ", ip=" + ip + ", ext=" + ext + ", updateTime=" + updateTime
				+ ", createTime=" + createTime + ", status=" + status + "]";
	}

}
