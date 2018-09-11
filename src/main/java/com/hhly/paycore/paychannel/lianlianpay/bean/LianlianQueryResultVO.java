package com.hhly.paycore.paychannel.lianlianpay.bean;

/**
 * @desc 查询连连支付交易结果返回
 * @author xiongJinGang
 * @date 2017年9月9日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class LianlianQueryResultVO {
	private String oid_partner;// 商户编号
	private String dt_order;// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
	private String no_order;// 商户唯一订单号
	private String result_pay;// SUCCESS 成功WAITING 等待支付PROCESSING 银行支付处理中REFUND 退款FAILURE 失败支付结果以此为准，商户按此进行后续是否发货操作
	private String oid_paybill;// 连连支付支付单号2013051500001
	private String money_order;// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
	private String settle_date;// 清算日期 YYYYMMDD 支付成功后会有
	private String info_order;// 订单描述
	private String pay_type;// 支付方式 1：网银支付（借记卡）8：网银支付（信用卡）9：B2B 企业网银支付
	private String bank_code;// 银行编号 01020000
	private String sign_type;// 签名方式：RSA 或者 MD5
	private String sign;//
	private String ret_code;// 交易结果代码 0000表示成功
	private String ret_msg;// 交易结果描述 交易成功

	private String bank_name; // 银行名称 query_version 大于 1.0 版本时返回不参与签名
	private String memo; // query_version 大于 1.0 版本时返回支付失败的原因，如果多次支付的话返回最后一次的失败原因

	// {"ret_code":"8901","ret_msg":"没有记录"}

	public String getOid_partner() {
		return oid_partner;
	}

	public void setOid_partner(String oid_partner) {
		this.oid_partner = oid_partner;
	}

	public String getDt_order() {
		return dt_order;
	}

	public void setDt_order(String dt_order) {
		this.dt_order = dt_order;
	}

	public String getNo_order() {
		return no_order;
	}

	public void setNo_order(String no_order) {
		this.no_order = no_order;
	}

	public String getResult_pay() {
		return result_pay;
	}

	public void setResult_pay(String result_pay) {
		this.result_pay = result_pay;
	}

	public String getOid_paybill() {
		return oid_paybill;
	}

	public void setOid_paybill(String oid_paybill) {
		this.oid_paybill = oid_paybill;
	}

	public String getMoney_order() {
		return money_order;
	}

	public void setMoney_order(String money_order) {
		this.money_order = money_order;
	}

	public String getSettle_date() {
		return settle_date;
	}

	public void setSettle_date(String settle_date) {
		this.settle_date = settle_date;
	}

	public String getInfo_order() {
		return info_order;
	}

	public void setInfo_order(String info_order) {
		this.info_order = info_order;
	}

	public String getPay_type() {
		return pay_type;
	}

	public void setPay_type(String pay_type) {
		this.pay_type = pay_type;
	}

	public String getBank_code() {
		return bank_code;
	}

	public void setBank_code(String bank_code) {
		this.bank_code = bank_code;
	}

	public String getSign_type() {
		return sign_type;
	}

	public void setSign_type(String sign_type) {
		this.sign_type = sign_type;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

	public String getRet_code() {
		return ret_code;
	}

	public void setRet_code(String ret_code) {
		this.ret_code = ret_code;
	}

	public String getRet_msg() {
		return ret_msg;
	}

	public void setRet_msg(String ret_msg) {
		this.ret_msg = ret_msg;
	}

	public String getBank_name() {
		return bank_name;
	}

	public void setBank_name(String bank_name) {
		this.bank_name = bank_name;
	}

	public String getMemo() {
		return memo;
	}

	public void setMemo(String memo) {
		this.memo = memo;
	}

}
