package com.hhly.paycore.paychannel.huichao.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.hhly.paycore.paychannel.huichao.config.HuiChaoConfig;
import com.hhly.skeleton.base.bo.ResultBO;
import com.hhly.skeleton.base.constants.MessageCodeConstants;
import com.hhly.skeleton.base.constants.PayConstants;
import com.hhly.skeleton.base.constants.PayConstants.PayStatusEnum;
import com.hhly.skeleton.base.constants.PayConstants.PayTypeThirdEnum;
import com.hhly.skeleton.base.util.DateUtil;
import com.hhly.skeleton.base.util.HttpUtil;
import com.hhly.skeleton.base.util.JaxbUtil;
import com.hhly.skeleton.base.util.ObjectUtil;
import com.hhly.skeleton.pay.bo.PaymentInfoBO;
import com.hhly.skeleton.pay.third.vo.HuiChaoOrderQueryReqVO;
import com.hhly.skeleton.pay.third.vo.HuiChaoOrderQueryResultListVO;
import com.hhly.skeleton.pay.third.vo.HuiChaoOrderQueryResultVO;
import com.hhly.skeleton.pay.vo.PayNotifyResultVO;
import com.hhly.skeleton.pay.vo.PayQueryParamVO;
import com.hhly.skeleton.pay.vo.PayQueryResultVO;

/**
 * @desc 汇潮工具类
 * @author xiongJinGang
 * @date 2018年1月3日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class HuiChaoUtil {
	private static Logger logger = Logger.getLogger(HuiChaoUtil.class);
	public static final String SUCCESS = "0";// 成功标志

	/**  
	* 方法说明: 构建支付请求参数
	* @auth: xiongJinGang
	* @param paymentInfoBO
	* @time: 2017年9月16日 上午9:27:33
	* @return: Map<String,String> 
	*/
	public static Map<String, String> buildWebMapParam(PaymentInfoBO paymentInfoBO) {
		Map<String, String> map = new ConcurrentHashMap<String, String>();
		map.put("MerNo", HuiChaoConfig.HUICHAO_MER_NO);// 商户号
		map.put("BillNo", paymentInfoBO.getNoOrder());// 订单号
		map.put("Amount", paymentInfoBO.getMoneyOrder());// 金额 元
		map.put("ReturnURL", paymentInfoBO.getUrlReturn());// 同步地址
		map.put("AdviceURL", paymentInfoBO.getNotifyUrl());// 异步地址
		map.put("OrderTime", DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));// 请求时间 YYYYMMDDHHMMSS
		// 以下是可选参数
		if (!ObjectUtil.isBlank(paymentInfoBO.getBankSimpleCode())) {
			map.put("defaultBankNumber", paymentInfoBO.getBankSimpleCode());// 银行编码【可选】
		}
		// 1:网银支付(借记卡) 2:快捷支付(借记卡) 3:快捷支付(信用卡)8:网银支付(信用卡)
		if (!ObjectUtil.isBlank(paymentInfoBO.getPayType())) {
			PayTypeThirdEnum payTypeThirdEnum = PayConstants.PayTypeThirdEnum.getEnum(paymentInfoBO.getPayType());
			if (!ObjectUtil.isBlank(payTypeThirdEnum)) {
				String payType = null;
				switch (payTypeThirdEnum) {
				case BANK_DEBIT_CARD_PAYMENT:// 借记卡网银
					payType = "B2CDebit";
					break;
				case QUICK_DEBIT_CARD_PAYMENT:// 借记卡快捷支付
					payType = "noCard";
					break;
				case QUICK_CREDIT_CARD_PAYMENT:// 信用卡快捷支付
					payType = "quickPay";
					break;
				case BANK_CREDIT_CARD_PAYMENT:// 信用卡网银支付
					payType = "B2CCredit";
					break;
				default:
					break;
				}
				if (!ObjectUtil.isBlank(payType)) {
					map.put("payType", payType);// 支付方式【可选】
				}
			}
		}

		map.put("Remark", paymentInfoBO.getNameGoods());// 备注
		// map.put("products", paymentInfoBO.getNotifyUrl());//物品信息
		// String sign = BuildRequestFormUtil.createLinkString(map, HuiChaoConfig.HUICHAO_KEY, false);
		String needMd5Str = new StringBuffer("MerNo=").append(map.get("MerNo")).append("&BillNo=").append(map.get("BillNo")).append("&Amount=").append(map.get("Amount")).append("&OrderTime=").append(map.get("OrderTime")).append("&ReturnURL=")
				.append(map.get("ReturnURL")).append("&AdviceURL=").append(map.get("AdviceURL")).append("&").append(HuiChaoConfig.HUICHAO_KEY).toString();

		map.put("SignInfo", DigestUtils.md5Hex(needMd5Str).toUpperCase());// MD5签名结果
		return map;
	}

	/**  
	* 方法说明: 解析并验证异步通知
	* @auth: xiongJinGang
	* @param map
	* @time: 2018年1月3日 上午10:29:30
	* @return: ResultBO<?> 
	*/
	public static ResultBO<?> payNotify(Map<String, String> map) {
		if (ObjectUtil.isBlank(map)) {
			logger.info("汇潮支付异步通知结果为空");
			return ResultBO.err();
		}
		PayNotifyResultVO payNotifyResult = new PayNotifyResultVO();
		if (map.containsKey("SignInfo")) {
			String needMd5Str = new StringBuffer("MerNo=").append(map.get("MerNo")).append("&BillNo=").append(map.get("BillNo")).append("&OrderNo=").append(map.get("OrderNo")).append("&Amount=").append(map.get("Amount")).append("&Succeed=")
					.append(map.get("Succeed")).append("&").append(HuiChaoConfig.HUICHAO_KEY).toString();

			String md5Sign = DigestUtils.md5Hex(needMd5Str).toUpperCase();
			if (!map.get("SignInfo").equals(md5Sign)) {
				logger.info("汇潮支付异步通知，验证签名不通过");
				return ResultBO.err(MessageCodeConstants.THIRD_API_SIGN_ERROR);
			}
			// 状态码88：成功 其它为失败
			payNotifyResult.setOrderCode(map.get("BillNo"));// 商户唯一订单号
			payNotifyResult.setThirdTradeNo(map.get("OrderNo"));// 平台订单号
			payNotifyResult.setOrderAmt(Double.parseDouble(map.get("Amount")));// 该笔订单的资金总额，单位为 RMB-元。大于 0 的数字，精确到小数点后两位。如：49.65
			payNotifyResult.setTradeTime(DateUtil.getNow(DateUtil.DEFAULT_FORMAT));// 商户订单时间 格式：YYYYMMDDH24MISS 14 位数字，精确到秒
			payNotifyResult.setResponse("ok");
			if (StringUtils.isNotBlank(map.get("Succeed")) && "88".equals(map.get("Succeed"))) {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_SUCCESS);
			} else {
				payNotifyResult.setStatus(PayStatusEnum.PAYMENT_FAILURE);
			}
			return ResultBO.ok(payNotifyResult);
		}
		logger.error("汇潮支付异步通知，无SignInfo参数");
		return ResultBO.err();
	}

	public static ResultBO<?> queryResult(PayQueryParamVO payQueryParamVO) {
		try {
			HuiChaoOrderQueryReqVO huiChaoOrderQueryVO = new HuiChaoOrderQueryReqVO();
			huiChaoOrderQueryVO.setTx("1001");// 1001表示订单查询
			huiChaoOrderQueryVO.setMerCode(HuiChaoConfig.HUICHAO_MER_NO);
			huiChaoOrderQueryVO.setOrderNumber(payQueryParamVO.getTransCode());
			huiChaoOrderQueryVO.setSign(DigestUtils.md5Hex(HuiChaoConfig.HUICHAO_MER_NO + HuiChaoConfig.HUICHAO_KEY).toUpperCase());

			Map<String, String> paramMap = new ConcurrentHashMap<String, String>();
			String param = JaxbUtil.convertToXml(huiChaoOrderQueryVO);
			paramMap.put("requestDomain", com.hhly.paycore.paychannel.lianlianpay.security.Base64.getBASE64(param));

			String result = HttpUtil.doPost(HuiChaoConfig.HUICHAO_QUERY_URL, paramMap);
			if (!ObjectUtil.isBlank(result)) {
				logger.info("查询汇潮交易编号【" + payQueryParamVO.getTransCode() + "】返回：" + result);

				HuiChaoOrderQueryResultVO huiChaoOrderQueryResultVO = JaxbUtil.converyToJavaBean(result, HuiChaoOrderQueryResultVO.class);
				// 00：请求成功，11：查询IP未绑定，22：签名不匹配，33：请求类型不匹配，44：查询条件为空
				if (huiChaoOrderQueryResultVO.getResultCode().equals("00")) {
					List<HuiChaoOrderQueryResultListVO> list = huiChaoOrderQueryResultVO.getList();
					if (!ObjectUtil.isBlank(list)) {
						HuiChaoOrderQueryResultListVO huiChaoOrderQueryResultListVO = list.get(0);
						PayQueryResultVO payQueryResultVO = new PayQueryResultVO();
						payQueryResultVO.setTotalAmount(huiChaoOrderQueryResultListVO.getOrderAmount());
						payQueryResultVO.setTradeNo(DateUtil.getNow(DateUtil.DATE_FORMAT_NUM));
						payQueryResultVO.setArriveTime(huiChaoOrderQueryResultListVO.getOrderDate());
						payQueryResultVO.setOrderCode(huiChaoOrderQueryResultListVO.getOrderNumber());
						if (huiChaoOrderQueryResultListVO.getOrderStatus().equals("1")) {
							payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_SUCCESS);
						} else {
							payQueryResultVO.setTradeStatus(PayStatusEnum.PAYMENT_FAILURE);
						}
						return ResultBO.ok(payQueryResultVO);
					}
				} else {
					logger.info("查询汇潮支付【" + payQueryParamVO.getTransCode() + "】结果当前状态【" + huiChaoOrderQueryResultVO.getResultCode() + "】，状态说明【00：请求成功，11：查询IP未绑定，22：签名不匹配，33：请求类型不匹配，44：查询条件为空】");
					return ResultBO.err(MessageCodeConstants.NO_PAY_OVERDUE);
				}
			}
		} catch (Exception e) {
			logger.error("查询汇潮支付【" + payQueryParamVO.getTransCode() + "】结果异常", e);
		}
		return ResultBO.err();
	}
}
