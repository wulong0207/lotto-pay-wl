package com.hhly.paycore.paychannel.huayi.util;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import com.hhly.paycore.paychannel.huayi.config.HuayiConfig;
import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;

/** 
 */
@SuppressWarnings("restriction")
public class RSAUtils {

	/**
	 * 签名算法RSA
	 */
	public static final String KEY_ALGORITHM = "RSA";

	/**
	 * 签名算法SHA1WithRSA
	 */
	public static final String SIGNATURE_ALGORITHM = "SHA1WithRSA";

	/**
	 * 商户私钥
	 */
	private static PrivateKey sysPrivateKey = null;

	/**
	 * 商户公钥
	 */
	private static PublicKey sysPublicKey = null;

	/**
	 * 七分钱公钥
	 */
	private static PublicKey sevenPubKey = null;

	static {

		try {
			// 商户私钥
			// String privateKeyStr =
			// "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAKNeySFFp9DO4u8IuFgqSnD/Sme2uc37vx3BE9nGKS3GKkYK7Ypl7j08as5ouU/cleXomkCnoVXiYXEM5BJCF8vPvWvRPvtfiCmMZy3rFe/8d96RbshZ1d+HWSvpYYOOARY/wlEbJLrexi9DQXaRmi8+tXdOdeC5VArIDt2Xy1zjAgMBAAECgYEAhBSo9b50C8yICot5RsaCQtMTW8COfetvu2WTX/jm5/wTx2ckX0VDlLyY+WIPmHKVujgRJf6g4GfTMewjJGPNCg6OLQ1SqQdUTu4bK2yRwZQN51jncBw4Cloh5zqNHiFdBcBsnsYnzvXlvqWZE6umLb+TUV2z/nrH+nHZjwAU+UkCQQDSSPez+MxKDxWHZPGd3Sb1jUSULEynjowapxPgaLU5aEi5YbqzyxydDRtzNl+i44CzEnQgntj2Av4bsQQEB219AkEAxuLcgeNLdawlgMJHFkHVhHs8rucOOsABd1BvzYrGDndTDW6WocLB0r+VNu7Bi7OFIqgJmty+w5l1fWsZSHOB3wJBAI44U28hAenEsebUWqVOAR9p38vm+iEIw7Dy9vp7VrXw1d6rPj6DTGLbjokpaR0diNmTzX6ScGJSO9I2smSXMXECQHJHPn7ftKc7sBzpBOG0GFTUXwz8tL9rPixpqqRHHgCH7c/lAGaYOri23q0+yTVVhKViOoqiVnabwde75z4rKK8CQEtdL/Q7+3aULcp/mgVMxE6V5DFA/hhSGBQKCRLR2cwA2FIhhP+s5iJzItNGWQY1BvzMXpLc8vppE0/AoKbv020=";

			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);

			// 因为java里面不识别x509格式的私钥，所以必须转换为 pkcs8格式方可使用
			// java异常描述为： java.security.spec.InvalidKeySpecException: Only
			// RSAPrivate(Crt)KeySpec and PKCS8EncodedKeySpec supported for RSA
			// private keys
			byte[] keyBytes = Base64.decode(HuayiConfig.HUAYI_PRIVATE_KEY);
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(keyBytes);
			sysPrivateKey = keyFactory.generatePrivate(pkcs8KeySpec);

			// 商户公钥
			// String publicKeyStr =
			// "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCjXskhRafQzuLvCLhYKkpw/0pntrnN+78dwRPZxiktxipGCu2KZe49PGrOaLlP3JXl6JpAp6FV4mFxDOQSQhfLz71r0T77X4gpjGct6xXv/HfekW7IWdXfh1kr6WGDjgEWP8JRGyS63sYvQ0F2kZovPrV3TnXguVQKyA7dl8tc4wIDAQAB";

			byte[] pubkeyBytes = Base64.decode(HuayiConfig.HUAYI_PUBLIC_KEY);
			X509EncodedKeySpec pubkeySpec = new X509EncodedKeySpec(pubkeyBytes);
			sysPublicKey = keyFactory.generatePublic(pubkeySpec);

			// 七分钱公钥
			// String sevenPubKeyStr =
			// "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC4G+OSs43I0Ctw7nxxuJPauTFsPyjMKkfsvYEcw4wqbn82KQWbFiSTy7P5hul4wdoZaFW5lSnHug+lyjn64t0dtCsaViOWefWrpL1gWZNpOc9gk6qNhQ0120ikHLE1SLH//gVStf+TDeVtaW+4Uzs5J7+/shdvfgU5T4+gxBk9jQIDAQAB";
			byte[] sevenPubKeyBytes = Base64.decode(HuayiConfig.HUAYI_SEVEN_PUBLIC_KEY);
			X509EncodedKeySpec sevenPubKeySpec = new X509EncodedKeySpec(sevenPubKeyBytes);
			sevenPubKey = keyFactory.generatePublic(sevenPubKeySpec);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static PublicKey getSevenPayPubKey() {
		return sevenPubKey;
	}

	/**
	 * <p>
	 * 用私钥对信息生成数字签名
	 * </p>
	 * 
	 * @param data
	 *            已加密数据
	 * @param privateKey
	 *            私钥(BASE64编码)
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String sign(byte[] data, PrivateKey privateK) throws Exception {

		if (null == privateK) {
			privateK = sysPrivateKey;
		}

		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initSign(privateK);
		signature.update(data);
		return parseByte2HexStr(signature.sign());
	}

	/**
	 * <p>
	 * 校验数字签名
	 * </p>
	 * 
	 * @param data
	 *            已加密数据
	 * @param publicKey
	 *            公钥(BASE64编码)
	 * @param sign
	 *            数字签名
	 * 
	 * @return
	 * @throws Exception
	 * 
	 */
	public static boolean verify(byte[] data, PublicKey publicK, String sign) throws Exception {

		if (null == publicK) {
			publicK = sysPublicKey;
		}

		Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
		signature.initVerify(publicK);
		signature.update(data);
		return signature.verify(parseHexStr2Byte(sign));
	}

	/**
	 * 将二进制转换成16进制
	 * 
	 * @param buf
	 * @return
	 */
	public static String parseByte2HexStr(byte buf[]) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < buf.length; i++) {
			String hex = Integer.toHexString(buf[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			sb.append(hex.toUpperCase());
		}
		return sb.toString();
	}

	/**
	 * 将16进制转换为二进制
	 * 
	 * @param hexStr
	 * @return
	 */
	public static byte[] parseHexStr2Byte(String hexStr) {
		if (hexStr.length() < 1) {
			return null;
		}
		byte[] result = new byte[hexStr.length() / 2];
		for (int i = 0; i < hexStr.length() / 2; i++) {
			int high = Integer.parseInt(hexStr.substring(i * 2, i * 2 + 1), 16);
			int low = Integer.parseInt(hexStr.substring(i * 2 + 1, i * 2 + 2), 16);
			result[i] = (byte) (high * 16 + low);
		}
		return result;
	}

	public static void main(String[] args) throws Exception {
		RSAUtils.sign("你好".getBytes("UTF-8"), null);
	}
}
