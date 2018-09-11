package com.hhly.paycore.po;

import java.util.Date;

public class PayBankPO {
    /**
     * ID主键
     */
    private Long id;

    /**
     * 银行名称
     */
    private String name;

    /**
     * 银行简称
     */
    private String cName;

    /**
     * 银行状态:0停用,1可用
     */
    private Short status;

    /**
     * 支付类型:1银行卡,2第三方支付
     */
    private Short payType;

    /**
     * 银行大Logo
     */
    private String bLogo;

    /**
     * 银行小Logo
     */
    private String sLogo;

    /**
     * PC排序
     */
    private Integer orderPc;

    /**
     * H5排序
     */
    private Integer orderH5;

    /**
     * ANDROID排序
     */
    private Integer orderAndroid;

    /**
     * IOS排序
     */
    private Integer orderIos;

    /**
     * 备注
     */
    private String remark;

    /**
     * null
     */
    private String createBy;

    /**
     * null
     */
    private String modifyBy;

    /**
     * null
     */
    private Date modifyTime;

    /**
     * null
     */
    private Date updateTime;

    /**
     * null
     */
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public String getcName() {
        return cName;
    }

    public void setcName(String cName) {
        this.cName = cName;
    }

    public Short getStatus() {
        return status;
    }

    public void setStatus(Short status) {
        this.status = status;
    }

    public Short getPayType() {
        return payType;
    }

    public void setPayType(Short payType) {
        this.payType = payType;
    }

    public String getbLogo() {
        return bLogo;
    }

    public void setbLogo(String bLogo) {
        this.bLogo = bLogo;
    }

    public String getsLogo() {
        return sLogo;
    }

    public void setsLogo(String sLogo) {
        this.sLogo = sLogo;
    }

    public Integer getOrderPc() {
        return orderPc;
    }

    public void setOrderPc(Integer orderPc) {
        this.orderPc = orderPc;
    }

    public Integer getOrderH5() {
        return orderH5;
    }

    public void setOrderH5(Integer orderH5) {
        this.orderH5 = orderH5;
    }

    public Integer getOrderAndroid() {
        return orderAndroid;
    }

    public void setOrderAndroid(Integer orderAndroid) {
        this.orderAndroid = orderAndroid;
    }

    public Integer getOrderIos() {
        return orderIos;
    }

    public void setOrderIos(Integer orderIos) {
        this.orderIos = orderIos;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy == null ? null : createBy.trim();
    }

    public String getModifyBy() {
        return modifyBy;
    }

    public void setModifyBy(String modifyBy) {
        this.modifyBy = modifyBy == null ? null : modifyBy.trim();
    }

    public Date getModifyTime() {
        return modifyTime;
    }

    public void setModifyTime(Date modifyTime) {
        this.modifyTime = modifyTime;
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
}