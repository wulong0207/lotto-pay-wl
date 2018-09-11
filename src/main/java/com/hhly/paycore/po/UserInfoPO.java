package com.hhly.paycore.po;

import java.util.Date;


/**
 * 用户信息（对表）
 * @desc
 * @author zhouyang
 * @date 2017年2月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class UserInfoPO {
	
	/**
	 * 自增id（即帐号id）
	 */
	private Integer id;
	
	/**
	 * 账号综合平台id
	 */
	private String accountId;
	
	/**
	 * 帐户名
	 */
	private String account;
	
	/**
	 * 密码
	 */
	private String password;
	
	/**
	 * 密码随机码
	 */
	private String rCode;
	
	/**
	 * 用户昵称
	 */
	private String nickName;
	
	/**
	 * 头像URL
	 */
	private String headUrl;
	
	/**
	 * 头像状态	0：禁用，1：启用
	 */
	private Short headStatus;
	
	/**
	 * 手机号
	 */
	private String mobile;
	
	/**
	 * 手机号是否认证	0：未认证，1：已认证
	 */
	private Short mobileStatus;
	
	/**
	 * 是否开启手机号登录	0：禁用，1：启用
	 */
	private Short isMobileLogin;
	
	/**
	 * 电子邮箱
	 */
	private String email;

	/**
	 * 邮箱是否认证	0：未认证，1：已认证
	 */
	private Short emailStatus;
	
	/**
	 * 是否开启邮箱登录	0：禁用，1：启用
	 */
	private Short isEmailLogin;
	
	/**
	 * 账户状态
	 */
	private Short accountStatus;
	
	/**
	 * 姓名
	 */
	private String realName;
	
	/**
	 * 身份证号码
	 */
	private String idCard;
	
	/**
	 * 居住地址
	 */
	private String address;
	
	/**
	 * 性别
	 */
	private Short sex;
	
	/**
	 * 注册时间
	 */
	private Date registerTime;
	
	/**
	 * 登录时间
	 */
	private Date lastLoginTime;

	/**
	 * 账户注册渠道id
	 */
	private String channelId;
	
	/**
	 * ip地址
	 */
	private String ip;
	
	/**
	 * 帐号是否修改过 0：否，1：是
	 */
	private Short accountModify;
	
	/**
	 * 支付id
	 */
	private Integer userPayId;

	/**
	 * qqopenId
	 */
	private String qqOpenID;

	/**
	 * 新浪openId
	 */
	private String sinaBlogOpenID;

	/**
	 * 百度openId
	 */
	private String baiduOpenID;

	/**
	 * 微信openId
	 */
	private String wechatOpenID;

	/**
	 * 支付宝openId
	 */
	private String alipayOpenID;

	/**
	 * 京东openId
	 */
	private String jdOpenID;

	/**
	 * QQ昵称
	 */
	private String qqName;

	/**
	 * 微信昵称
	 */
	private String wechatName;
	
	/**APP免打扰时间段,例如：23:00-09:00*/
	private String appNotDisturb;
	
    /**APP推送:0不接收;1接收*/
	private Integer msgApp;
	private String userPayCardcode;//最后一次使用的银行卡号
   
    public Integer getMsgApp () {
        return msgApp;
    }
    
    public void setMsgApp (Integer msgApp) {
        this.msgApp = msgApp;
    }
    
    public String getAppNotDisturb () {
        return appNotDisturb;
    }
    
    public void setAppNotDisturb (String appNotDisturb) {
        this.appNotDisturb = appNotDisturb;
    }
    
    public UserInfoPO() {
		super();
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getAccount() {
		return account;
	}

	public void setAccount(String account) {
		this.account = account;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getrCode() {
		return rCode;
	}

	public void setrCode(String rCode) {
		this.rCode = rCode;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public String getHeadUrl() {
		return headUrl;
	}

	public void setHeadUrl(String headUrl) {
		this.headUrl = headUrl;
	}

	public Short getHeadStatus() {
		return headStatus;
	}

	public void setHeadStatus(Short headStatus) {
		this.headStatus = headStatus;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public Short getMobileStatus() {
		return mobileStatus;
	}

	public void setMobileStatus(Short mobileStatus) {
		this.mobileStatus = mobileStatus;
	}

	public Short getIsMobileLogin() {
		return isMobileLogin;
	}

	public void setIsMobileLogin(Short isMobileLogin) {
		this.isMobileLogin = isMobileLogin;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Short getEmailStatus() {
		return emailStatus;
	}

	public void setEmailStatus(Short emailStatus) {
		this.emailStatus = emailStatus;
	}

	public Short getIsEmailLogin() {
		return isEmailLogin;
	}

	public void setIsEmailLogin(Short isEmailLogin) {
		this.isEmailLogin = isEmailLogin;
	}

	public Short getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(Short accountStatus) {
		this.accountStatus = accountStatus;
	}

	public String getRealName() {
		return realName;
	}

	public void setRealName(String realName) {
		this.realName = realName;
	}

	public String getIdCard() {
		return idCard;
	}

	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Short getSex() {
		return sex;
	}

	public void setSex(Short sex) {
		this.sex = sex;
	}

	public Date getRegisterTime() {
		return registerTime;
	}

	public void setRegisterTime(Date registerTime) {
		this.registerTime = registerTime;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Short getAccountModify() {
		return accountModify;
	}

	public void setAccountModify(Short accountModify) {
		this.accountModify = accountModify;
	}

	public Integer getUserPayId() { return userPayId; }

	public void setUserPayId(Integer userPayId) {
		this.userPayId = userPayId;
	}

	public String getQqOpenID() {
		return qqOpenID;
	}

	public void setQqOpenID(String qqOpenID) {
		this.qqOpenID = qqOpenID;
	}

	public String getSinaBlogOpenID() {
		return sinaBlogOpenID;
	}

	public void setSinaBlogOpenID(String sinaBlogOpenID) {
		this.sinaBlogOpenID = sinaBlogOpenID;
	}

	public String getBaiduOpenID() {
		return baiduOpenID;
	}

	public void setBaiduOpenID(String baiduOpenID) {
		this.baiduOpenID = baiduOpenID;
	}

	public String getWechatOpenID() {
		return wechatOpenID;
	}

	public void setWechatOpenID(String wechatOpenID) {
		this.wechatOpenID = wechatOpenID;
	}

	public String getAlipayOpenID() {
		return alipayOpenID;
	}

	public void setAlipayOpenID(String alipayOpenID) {
		this.alipayOpenID = alipayOpenID;
	}

	public String getJdOpenID() {
		return jdOpenID;
	}

	public void setJdOpenID(String jdOpenID) {
		this.jdOpenID = jdOpenID;
	}

	public String getQqName() {
		return qqName;
	}

	public void setQqName(String qqName) {
		this.qqName = qqName;
	}

	public String getWechatName() {
		return wechatName;
	}

	public void setWechatName(String wechatName) {
		this.wechatName = wechatName;
	}

	public String getUserPayCardcode() {
		return userPayCardcode;
	}

	public void setUserPayCardcode(String userPayCardcode) {
		this.userPayCardcode = userPayCardcode;
	}
	
	
}
