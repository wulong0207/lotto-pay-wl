package com.hhly.paycore.paychannel.palmpay.bean;

/**
 * @desc 掌宜付异步回调
 * @author xiongJinGang
 * @date 2017年9月16日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class PalmNotifyResultVO {
	private String code;// 0 交易成功1 交易失败(保留留值，交易失败暂不不发回调通知)
	private String app_id;// 应⽤用ID
	private String pay_way;// 支付方式 1 微信2 支付宝3 QQ钱包9 银联
	private String out_trade_no;// 商户订单编号
	private String invoice_no;// 平台订单编号 ，平台⾃自动⽣生成，全局唯一
	private String up_invoice_no;// 银行或微信支付流水号，不不是所有通道或支付方式都提供
	private String money;// 交易易金金额，正整数，以分为单位
	private String qn;// 商户渠道代码
	private String sign;// 参数签名

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getApp_id() {
		return app_id;
	}

	public void setApp_id(String app_id) {
		this.app_id = app_id;
	}

	public String getPay_way() {
		return pay_way;
	}

	public void setPay_way(String pay_way) {
		this.pay_way = pay_way;
	}

	public String getOut_trade_no() {
		return out_trade_no;
	}

	public void setOut_trade_no(String out_trade_no) {
		this.out_trade_no = out_trade_no;
	}

	public String getInvoice_no() {
		return invoice_no;
	}

	public void setInvoice_no(String invoice_no) {
		this.invoice_no = invoice_no;
	}

	public String getUp_invoice_no() {
		return up_invoice_no;
	}

	public void setUp_invoice_no(String up_invoice_no) {
		this.up_invoice_no = up_invoice_no;
	}

	public String getMoney() {
		return money;
	}

	public void setMoney(String money) {
		this.money = money;
	}

	public String getQn() {
		return qn;
	}

	public void setQn(String qn) {
		this.qn = qn;
	}

	public String getSign() {
		return sign;
	}

	public void setSign(String sign) {
		this.sign = sign;
	}

}
