package com.hhly.paycore.paychannel.huayi.util;

public enum ServiceType {
	
	ONECODE_COLL_PAY("onecode.coll.pay","一码收银"),
		
	PC_GATEWAY_PAY("pc.gateway.pay","PC端收银台"),	
		
	H5_GATEWAY_PAY("h5.gateway.pay","手机网页端公众号支付"),
	
	H5T_GATEWAY_PAY("h5_t.gateway.pay","原生H5支付"),
		
	MOBILE_PLUGIN_PAY("mobile.plugin.pay","手机支付插件收银台"),	
		
	QUERY_ORDER_STATUS("mch.query.orderstatus","交易结果查询"),
	
	MCH_REFUND("mch.refund","商户退款"),
	
	MCH_TRANSFER("mch.transfer","商户转账");
	
	private ServiceType(String type,String desc){
		this.desc = desc;
		this.type = type;
	}
	
	private String type;
	
	private String desc;

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDesc() {
		return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}
	
}
