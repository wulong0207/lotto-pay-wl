package com.hhly.paycore.paychannel.lianlianpay.enums;

/**
* 签名方式枚举
* @author guoyx
* @date:May 13, 2013 8:22:15 PM
* @version :1.0
*
*/
public class LianlianEnum {

	/**
	 * @desc 签名方式
	 * @author xiongJinGang
	 * @date 2017年9月12日
	 * @company 益彩网络科技公司
	 * @version 1.0
	 */
	public enum SignTypeEnum {
		RSA("RSA", "RSA签名"), MD5("MD5", "MD5签名");

		private final String code;
		private final String msg;

		SignTypeEnum(String code, String msg) {
			this.code = code;
			this.msg = msg;
		}

		public String getCode() {
			return code;
		}

		public String getMsg() {
			return msg;
		}

		public static boolean isSignType(String code) {
			for (SignTypeEnum s : SignTypeEnum.values()) {
				if (s.getCode().equals(code)) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * @desc 支付类型
	 * @author xiongJinGang
	 * @date 2017年9月12日
	 * @company 益彩网络科技公司
	 * @version 1.0
	 */
	public enum PayTypeEnum {
		BANK("BANK", "网银"), FAST("FAST", "快捷"), WAP("WAP", "WAP"), APP("APP", "APP");

		private final String code;
		private final String msg;

		PayTypeEnum(String code, String msg) {
			this.code = code;
			this.msg = msg;
		}

		public String getCode() {
			return code;
		}

		public String getMsg() {
			return msg;
		}
	}
}
