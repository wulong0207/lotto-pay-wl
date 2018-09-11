package com.hhly.pay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.log4j.Logger;

/**
 * RSA签名公共类
 * @author shmily
 */
public class RSAUtil {

	private static Logger log = Logger.getLogger(RSAUtil.class);
	private static RSAUtil instance;

	private RSAUtil() {

	}

	public static RSAUtil getInstance() {
		if (null == instance)
			return new RSAUtil();
		return instance;
	}

	/**
	 * 公钥、私钥文件生成
	 * @param keyPath：保存文件的路径
	 * @param keyFlag：文件名前缀
	 */
	private void generateKeyPair(String key_path, String name_prefix) {
		java.security.KeyPairGenerator keygen = null;
		try {
			keygen = java.security.KeyPairGenerator.getInstance("RSA");
		} catch (NoSuchAlgorithmException e1) {
			log.error(e1.getMessage());
		}
		SecureRandom secrand = new SecureRandom();
		secrand.setSeed("21cn".getBytes()); // 初始化随机产生器
		keygen.initialize(1024, secrand);
		KeyPair keys = keygen.genKeyPair();
		PublicKey pubkey = keys.getPublic();
		PrivateKey prikey = keys.getPrivate();

		String pubKeyStr = Base64.getBASE64(pubkey.getEncoded());
		String priKeyStr = Base64.getBASE64(prikey.getEncoded());
		File file = new File(key_path);
		if (!file.exists()) {
			file.mkdirs();
		}
		try {
			// 保存私钥
			FileOutputStream fos = new FileOutputStream(new File(key_path + name_prefix + "_RSAKey_private.txt"));
			fos.write(priKeyStr.getBytes());
			fos.close();
			// 保存公钥
			fos = new FileOutputStream(new File(key_path + name_prefix + "_RSAKey_public.txt"));
			fos.write(pubKeyStr.getBytes());
			fos.close();
		} catch (IOException e) {
			log.error(e.getMessage());
		}
	}

	/**
	 * 读取密钥文件内容
	 * @param key_file:文件路径
	 * @return
	 */
	private static String getKeyContent(String key_file) {
		File file = new File(key_file);
		BufferedReader br = null;
		InputStream ins = null;
		StringBuffer sReturnBuf = new StringBuffer();
		try {
			ins = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(ins, "UTF-8"));
			String readStr = null;
			readStr = br.readLine();
			while (readStr != null) {
				sReturnBuf.append(readStr);
				readStr = br.readLine();
			}
		} catch (IOException e) {
			return null;
		} finally {
			if (br != null) {
				try {
					br.close();
					br = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (ins != null) {
				try {
					ins.close();
					ins = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}
		return sReturnBuf.toString();
	}

	/**
	 * 签名处理
	 * @param prikeyvalue：私钥文件
	 * @param sign_str：签名源内容
	 * @return
	 */
	public static String sign(String prikeyvalue, String sign_str) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.getBytesBASE64(prikeyvalue));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey myprikey = keyf.generatePrivate(priPKCS8);
			// 用私钥对信息生成数字签名
			java.security.Signature signet = java.security.Signature.getInstance("MD5withRSA");
			signet.initSign(myprikey);
			signet.update(sign_str.getBytes("UTF-8"));
			byte[] signed = signet.sign(); // 对信息的数字签名
			return new String(org.apache.commons.codec.binary.Base64.encodeBase64(signed));
		} catch (java.lang.Exception e) {
			log.error("签名失败," + e.getMessage());
		}
		return null;
	}

	/**
	 * 签名验证
	 * @param pubkeyvalue：公钥
	 * @param oid_str：源串
	 * @param signed_str：签名结果串
	 * @return
	 */
	public static boolean checksign(String pubkeyvalue, String oid_str, String signed_str) {
		try {
			X509EncodedKeySpec bobPubKeySpec = new X509EncodedKeySpec(Base64.getBytesBASE64(pubkeyvalue));
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PublicKey pubKey = keyFactory.generatePublic(bobPubKeySpec);
			byte[] signed = Base64.getBytesBASE64(signed_str);// 这是SignatureData输出的数字签名
			java.security.Signature signetcheck = java.security.Signature.getInstance("MD5withRSA");
			signetcheck.initVerify(pubKey);
			signetcheck.update(oid_str.getBytes("UTF-8"));
			return signetcheck.verify(signed);
		} catch (java.lang.Exception e) {
			log.error("签名验证异常," + e.getMessage());
		}
		return false;
	}

	public static void main(String[] args) {
		/*  // 商户（RSA）私钥 TODO 强烈建议将私钥
		String RSA_PRIVATE = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAMhKNA1Ws0H6PrZ8t1lQxhQjERj0hYf8QWBlF2DtlMajYU52WsiGIvid6iQQhJGc+aPNTf3MfWCWSHk2XRIYRpjoVPQ8Oz8sLF8j3pT3I2h2gDRNvO2xqX+x+jyFDMnAXm4uMyBYS9wabuhUchF5JkHT1A3rZZFYapPqMTj/zeEFAgMBAAECgYB+uPwwCFAIiYVOPqBe4U1CBmHV8TffLwpKLAvbptX/y/VQCHAt+Th9JqSyxsSpwLDuI4KZ9tzI1KzsDCpcvYFEMuoPNgwjZBFBsmTdXD+nxUTKVbTII6kITyzMMWDBnF8LxAicMKpYcRKaVOULCg/AHPGV32Efd4pH8cyJGcJ6TQJBAP+7+YygfcJLvxI9kk/2Se+dI//mX6WVh1V0RFgSl0cWry+xq9xTQofy0wU++TiXkA05aCJbwY0EjyodUOcpHkMCQQDIf3r3WVpW4Fx6t6B2geew4mllckFEHHDf0pXE5GWymccQHHxo6knFrzZ8F/97XwAIGTabNBXQiWd9G1DfEyMXAkEAow/84wpCpe0efEb+UDY+lqagGb+PJUne7UIhgfb4tr9kHQkxCF+egIj4vNOWndsmYwhDugS/uWc60iO3Pm4deQJAC3qA57hN27tsj/oDTcWSJiZQMmagJe4a6DV+LY+F4vu60clPthHzt0WYsPIOxllh/xSyc6A/v3ieXCM8Ngk6cQJBAJiX6nzlyLyHrHQ0jIdQ97bYtJTqh0ZC6bZ3PShCj3we/Cu+5v6L5Rmwx0s+OJ84OnWIopuuc5QwmOT53VRIntE=";
		// 银通支付（RSA）公钥
		String RSA_YT_PUBLIC = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDISjQNVrNB+j62fLdZUMYUIxEY9IWH/EFgZRdg7ZTGo2FOdlrIhiL4neokEISRnPmjzU39zH1glkh5Nl0SGEaY6FT0PDs/LCxfI96U9yNodoA0Tbztsal/sfo8hQzJwF5uLjMgWEvcGm7oVHIReSZB09QN62WRWGqT6jE4/83hBQIDAQAB";
		
		// RSAUtil.getInstance().generateKeyPair("D:\\CertFiles\\inpour\\",
		// "ll_yt");
		String sign = RSAUtil.sign(RSA_PRIVATE, "busi_partner=101001&dt_order=20130521175800&money_order=12.10&name_goods=%E5%95%86%E5%93%81%E5%90%8D%E7%A7%B0&notify_url=http%3A%2F%2Fwww.baidu.com&no_order=20130521175800&oid_partner=201103171000000000&sign_type=RSA");
		
		System.out.println(sign);
		System.out.println(RSAUtil.checksign(RSA_YT_PUBLIC, "busi_partner=101001&dt_order=20130521175800&money_order=12.10&name_goods=%E5%95%86%E5%93%81%E5%90%8D%E7%A7%B0&notify_url=http%3A%2F%2Fwww.baidu.com&no_order=20130521175800&oid_partner=201103171000000000&sign_type=RSA", sign));
		*/

		// 商户（RSA）私钥 TODO 强烈建议将私钥
		String RSA_PRIVATE = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAK1Vn0/pKwD2FF0GLuX1HmTQDTkwXIph810Q+QD9c3nFfNOZE5ePTnnloZlJ2YB2bBkIt5scOS1e2c1jPnJKw/iG9Kb1KD8dD4JAlFIQsfXy3PVhOZ4yMzjQTuRts3RFKMXFXGHwfOkRMi76qLtD2lLHPbgxp0354QKKXEmB+4xrAgMBAAECgYByz0DbvFS4qUYpq5vKw0Yjfk8T7z2Mh36byU81YatoH/Ajc8QvYkOXqAsWrny7gzTsjAKZYeNZcvcO/MpzB6SvVLEQVXLwY6tGYKl24QGNjkGjQ3r8HWHwMwExQrdOpMOlOZBjptGO8vwGNOUdrrb2IrEj0WCDdSCXSWop4gr0IQJBAN6d2WVTqSq3G05Y5vKtlFIM+SLwpPOBXCSE0jy0M1Gr7ohRmF0XRF9/wuAV+XxY1b9EeUQ5epuBxx0bolgCOlECQQDHU9qoLFoJjzfYFwK4A2MlGgYn98TVJJSg+iOf5/HxfJrdltThPc1J4R9kFpoCH0Mk9Srup1mN3oxtbUPYP6/7AkEAizHDxtmiwvSu/DQWY9MpFIzMEo7JdQCDrsnl8tLx67VHdrEeRcbQl635GchjsN6S9/9Gm+Qcx7ND3u1yevkZoQJACwsjTzP93Q+5SVilBurxIEob0zUQC7sWHQEe3iospnN/5Q0JSF1zNUeqxJHYAIwY3UBUG5rcBFczMpDeNGn65wJBAMRtGFohy7vZg9gpCKc+AQD7T/ZvVKZhipgU8dM/00XUJyZun9qOYzLbbhZIQ0g3lW3mTTMaBH0k4GlD00D4GZM=";
		// 银通支付（RSA）公钥
		String RSA_YT_PUBLIC = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtVZ9P6SsA9hRdBi7l9R5k0A05MFyKYfNdEPkA/XN5xXzTmROXj0555aGZSdmAdmwZCLebHDktXtnNYz5ySsP4hvSm9Sg/HQ+CQJRSELH18tz1YTmeMjM40E7kbbN0RSjFxVxh8HzpETIu+qi7Q9pSxz24MadN+eECilxJgfuMawIDAQAB";

		// RSAUtil.getInstance().generateKeyPair("D:\\CertFiles\\inpour\\",
		// "ll_yt");
		String sign = RSAUtil.sign(RSA_PRIVATE,
				"busi_partner=101001&dt_order=20130521175800&money_order=12.10&name_goods=%E5%95%86%E5%93%81%E5%90%8D%E7%A7%B0&notify_url=http%3A%2F%2Fwww.baidu.com&no_order=20130521175800&oid_partner=201103171000000000&sign_type=RSA");

		System.out.println(sign);
		System.out.println(RSAUtil.checksign(RSA_YT_PUBLIC,
				"busi_partner=101001&dt_order=20130521175800&money_order=12.10&name_goods=%E5%95%86%E5%93%81%E5%90%8D%E7%A7%B0&notify_url=http%3A%2F%2Fwww.baidu.com&no_order=20130521175800&oid_partner=201103171000000000&sign_type=RSA", sign));

	}
}
