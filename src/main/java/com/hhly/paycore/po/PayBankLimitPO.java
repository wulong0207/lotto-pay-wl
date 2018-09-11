package com.hhly.paycore.po;

public class PayBankLimitPO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 银行ID
     */
    private Long bankId;

    /**
     * 单笔限额(元)
     */
    private String limitTime;

    /**
     * 每日限额(元)
     */
    private String limitDay;

    /**
     * 每月限额(元)
     */
    private String limitMonth;

    /**
     * 限额需要满足的条件
     */
    private String condition;

    /**
     * 备注
     */
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBankId() {
        return bankId;
    }

    public void setBankId(Long bankId) {
        this.bankId = bankId;
    }

    public String getLimitTime() {
        return limitTime;
    }

    public void setLimitTime(String limitTime) {
        this.limitTime = limitTime;
    }

    public String getLimitDay() {
        return limitDay;
    }

    public void setLimitDay(String limitDay) {
        this.limitDay = limitDay;
    }

    public String getLimitMonth() {
        return limitMonth;
    }

    public void setLimitMonth(String limitMonth) {
        this.limitMonth = limitMonth;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}