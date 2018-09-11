package com.hhly.paycore.jms;

import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * @desc 延时关闭未充值的交易
 * @author xiongJinGang
 * @date 2017年8月4日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class CloseRechargeDelay implements Delayed, Serializable {

	private static final long serialVersionUID = 6387479269210910013L;
	private String rechargeCode;// 交易号
	private long expireTime;// 到期时间
	private Short isAdd = 0;// 是否添加过（系统更新或者重启后，记录下是否重新加过），1是重新加载过

	public CloseRechargeDelay() {
		super();
	}

	public CloseRechargeDelay(String rechargeCode, long expireTime) {
		super();
		this.rechargeCode = rechargeCode;
		this.expireTime = expireTime;
	}

	public String getRechargeCode() {
		return rechargeCode;
	}

	public void setRechargeCode(String rechargeCode) {
		this.rechargeCode = rechargeCode;
	}

	public long getExpireTime() {
		return expireTime;
	}

	public void setExpireTime(long expireTime) {
		this.expireTime = expireTime;
	}

	public Short getIsAdd() {
		return isAdd;
	}

	public void setIsAdd(Short isAdd) {
		this.isAdd = isAdd;
	}

	@Override
	public int compareTo(Delayed delayed) {
		if (delayed == this) {
			return 0;
		}
		if (delayed instanceof CloseRechargeDelay) {
			CloseRechargeDelay otherRequest = (CloseRechargeDelay) delayed;
			long otherStartTime = otherRequest.getExpireTime();
			return (int) (this.expireTime - otherStartTime);
		}
		return 0;
	}

	@Override
	public long getDelay(TimeUnit unit) {
		return unit.convert(expireTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

}
