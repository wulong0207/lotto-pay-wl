package com.hhly.paycore.paychannel.juhepay.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;

import javax.crypto.Cipher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.paycore.sign.Base64;

/**
 * @desc 目前聚合支付的都是用的MD5签名，RSA签名目前没用，一旦启用，MD5签名的将会失效
 * @author xiongJinGang
 * @date 2017年10月11日
 * @company 益彩网络科技公司
 * @version 1.0
 */
public class RSA {
	public static final Logger logger = LoggerFactory.getLogger(RSA.class);

	public static final String SIGN_SHA1WITHRSA = "SHA1WithRSA";
	public static final String SIGN_SHA256WITHRSA = "SHA256WithRSA";

	// /**
	// * 秘钥长度: 1024
	// * @return Map<String, Object>
	// * @throws Exception
	// */
	// public static Map<String, Object> genKeyPair() throws Exception {
	// //随机生成密钥对
	// KeyPairGenerator keyPairGen = null;
	// keyPairGen = KeyPairGenerator.getInstance("RSA");
	//
	// // 初始化密钥对生成器，密钥大小为96-1024位
	// keyPairGen.initialize(1024, new SecureRandom());
	// // 生成一个密钥对，保存在keyPair中
	// KeyPair keyPair = keyPairGen.generateKeyPair();
	// // 得到公钥
	// RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
	// // 得到私钥
	// RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
	//
	// Map<String, Object> keyMap = new HashMap(2);
	// keyMap.put("RSAPublicKey", publicKey);
	// keyMap.put("RSAPrivateKey", privateKey);
	//
	// return keyMap;
	// }

	public static String getPrivateKey(Map<String, Object> keyMap) throws Exception {
		Key key = (Key) keyMap.get("RSAPrivateKey");
		return Base64.encode(key.getEncoded());
	}

	public static String getPublicKey(Map<String, Object> keyMap) throws Exception {
		Key key = (Key) keyMap.get("RSAPublicKey");
		return Base64.encode(key.getEncoded());
	}

	/**
	 * RSA签名
	 *
	 * @param content
	 *            待签名数据
	 * @param privateKey
	 *            商户私钥
	 * @param input_charset
	 *            编码格式
	 * @return 签名值
	 */
	public static String sign(String content, String privateKey, String input_charset) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);

			java.security.Signature signature = java.security.Signature.getInstance(SIGN_SHA1WITHRSA);
			signature.initSign(priKey);
			signature.update(content.getBytes(input_charset));

			byte[] signed = signature.sign();
			return Base64.encode(signed);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * RSA签名
	 *
	 * @param content
	 *            待签名数据
	 * @param privateKey
	 *            商户私钥
	 * @param input_charset
	 *            编码格式
	 * @return 签名值
	 */
	public static String signRSA256(String content, String privateKey, String input_charset) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(Base64.decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			PrivateKey priKey = keyf.generatePrivate(priPKCS8);

			java.security.Signature signature = java.security.Signature.getInstance(SIGN_SHA256WITHRSA);
			signature.initSign(priKey);
			if (input_charset == null) {
				input_charset = "UTF-8";
			}
			logger.info("content:" + content);
			logger.info("privateKey:" + privateKey);
			logger.info("input_charset:" + input_charset);
			signature.update(content.getBytes(input_charset));

			byte[] signed = signature.sign();
			return Base64.encode(signed);
		} catch (Exception e) {
			logger.error("生成RSA签名异常", e);
		}
		return null;
	}

	/*public static String easyLinkSign(String content, String privateKey, String input_charset) {
		try {
			PKCS8EncodedKeySpec priPKCS8 = new PKCS8EncodedKeySpec(EasyLinkBase64.decode(privateKey));
			KeyFactory keyf = KeyFactory.getInstance("RSA");
			RSAPrivateKey priKey = (RSAPrivateKey) keyf.generatePrivate(priPKCS8);
			// RSAPrivateKey pbk = getPrivateKey("c:\\\\yilian.pfx", "11111111");
			logger.info("sign content:" + content);
			java.security.Signature signature = java.security.Signature.getInstance("MD5withRSA");
			signature.initSign(priKey);
			signature.update(content.getBytes("UTF-8"));
	
			byte[] signed = signature.sign();
			return EasyLinkBase64.encode(signed);
		} catch (Exception e) {
			logger.error("生成RSA签名异常", e);
			throw new PayCenterApiException(CodeConstant.ERROR_CODE_SYSTEM_ERROR);
		}
	}
	
	public static boolean easyLinkVerify(String data, String pub_key, String value) {
	
		try {
			byte[] bts_data = EasyLinkBase64.decode(data);
			byte[] bts_key = EasyLinkBase64.decode(pub_key);
	
			KeyFactory rsaKeyFac = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bts_key);
			RSAPublicKey pbk = (RSAPublicKey) rsaKeyFac.generatePublic(keySpec);
	
			Signature signetcheck = java.security.Signature.getInstance("MD5withRSA");
			signetcheck.initVerify(pbk);
			signetcheck.update(value.getBytes("UTF-8"));
	
			return signetcheck.verify(bts_data);
	
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}*/

	/**
	 * RSA验签名检查
	 *
	 * @param content
	 *            待签名数据
	 * @param sign
	 *            签名值
	 * @param public_key
	 *            平台公钥
	 * @param input_charset
	 *            编码格式
	 * @return 布尔值
	 */
	public static boolean verify(String content, String sign, String public_key, String input_charset) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] encodedKey = Base64.decode(public_key);
			if (encodedKey == null) {
				return false;
			}
			PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

			java.security.Signature signature = java.security.Signature.getInstance(SIGN_SHA1WITHRSA);

			signature.initVerify(pubKey);
			signature.update(content.getBytes(input_charset));

			boolean bverify = signature.verify(Base64.decode(sign));
			return bverify;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	/**
	 * RSA验签名检查
	 *
	 * @param content
	 *            待签名数据
	 * @param sign
	 *            签名值
	 * @param public_key
	 *            平台公钥
	 * @param input_charset
	 *            编码格式
	 * @return 布尔值
	 */
	public static boolean verifyRSA256(String content, String sign, String public_key, String input_charset) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] encodedKey = Base64.decode(public_key);
			if (encodedKey == null) {
				return false;
			}
			PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));

			java.security.Signature signature = java.security.Signature.getInstance(SIGN_SHA256WITHRSA);

			signature.initVerify(pubKey);
			signature.update(content.getBytes(input_charset));

			boolean bverify = signature.verify(Base64.decode(sign));
			return bverify;

		} catch (Exception e) {
			logger.error("校验签名发生异常", e.getMessage());
		}
		return false;
	}

	// /**
	// * 秘钥长度: 2048
	// * @return Map<String, Object>
	// * @throws Exception
	// */
	// public static Map<String, Object> genKeyPairRSA256() throws Exception {
	// //随机生成密钥对
	// KeyPairGenerator keyPairGen = null;
	// keyPairGen = KeyPairGenerator.getInstance("RSA");
	//
	// // 初始化密钥对生成器，密钥大小为1025-2048位
	// keyPairGen.initialize(2048, new SecureRandom());
	// // 生成一个密钥对，保存在keyPair中
	// KeyPair keyPair = keyPairGen.generateKeyPair();
	// // 得到公钥
	// RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
	// // 得到私钥
	// RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
	//
	// Map<String, Object> keyMap = new HashMap(2);
	// keyMap.put("RSAPublicKey", publicKey);
	// keyMap.put("RSAPrivateKey", privateKey);
	//
	// return keyMap;
	// }
	/*	public static String easyLinkDecrypt(String content, String private_key) throws Exception {
			PrivateKey prikey = getPrivateKey(private_key);
	
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, prikey);
	
			byte[] btSrc = cipher.doFinal(EasyLinkBase64.decode(content));
	
			return new String(btSrc, "UTF-8");
		}*/

	/**
	 * 解密
	 *
	 * @param content
	 *            密文
	 * @param private_key
	 *            商户私钥
	 * @param input_charset
	 *            编码格式
	 * @return 解密后的字符串
	 */
	public static String decrypt(String content, String private_key, String input_charset) throws Exception {
		PrivateKey prikey = getPrivateKey(private_key);

		Cipher cipher = Cipher.getInstance("RSA");
		cipher.init(Cipher.DECRYPT_MODE, prikey);

		InputStream ins = new ByteArrayInputStream(Base64.decode(content));
		ByteArrayOutputStream writer = new ByteArrayOutputStream();
		// rsa解密的字节大小最多是128，将需要解密的内容，按128位拆开解密
		byte[] buf = new byte[128];
		int bufl;

		while ((bufl = ins.read(buf)) != -1) {
			byte[] block = null;

			if (buf.length == bufl) {
				block = buf;
			} else {
				block = new byte[bufl];
				for (int i = 0; i < bufl; i++) {
					block[i] = buf[i];
				}
			}

			writer.write(cipher.doFinal(block));
		}

		return new String(writer.toByteArray(), input_charset);
	}

	/**
	 * 得到私钥
	 *
	 * @param key
	 *            密钥字符串（经过base64编码）
	 * @throws Exception
	 */
	public static PrivateKey getPrivateKey(String key) throws Exception {

		byte[] keyBytes;

		keyBytes = Base64.decode(key);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

		KeyFactory keyFactory = KeyFactory.getInstance("RSA");

		PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

		return privateKey;
	}

	/**
	 *
	 * @param content
	 * @param privateKey
	 * @param input_charset
	 * @return
	 * @throws Exception
	 */
	public static String signByPrivate(String content, String privateKey, String input_charset) throws Exception {
		PrivateKey privateKeyInfo = getPrivateKeyByPKCS8(privateKey);
		return signByPrivate(content, privateKeyInfo, input_charset);
	}

	/**
	 * 得到私钥
	 *
	 * @param key  密钥字符串（经过base64编码）
	 * @throws Exception
	 */
	public static PrivateKey getPrivateKeyByPKCS8(String key) {
		byte[] keyBytes = buildPKCS8Key(key);

		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

			return privateKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static byte[] buildPKCS8Key(String privateKey) {
		if (privateKey.contains("-----BEGIN PRIVATE KEY-----")) {
			return org.springframework.security.crypto.codec.Base64.decode(privateKey.replaceAll("-----\\w+ PRIVATE KEY-----", "").getBytes());
		} else if (privateKey.contains("-----BEGIN RSA PRIVATE KEY-----")) {
			final byte[] innerKey = org.springframework.security.crypto.codec.Base64.decode(privateKey.replaceAll("-----\\w+ RSA PRIVATE KEY-----", "").getBytes());
			final byte[] result = new byte[innerKey.length + 26];
			System.arraycopy(org.springframework.security.crypto.codec.Base64.decode("MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKY=".getBytes()), 0, result, 0, 26);
			System.arraycopy(BigInteger.valueOf(result.length - 4).toByteArray(), 0, result, 2, 2);
			System.arraycopy(BigInteger.valueOf(innerKey.length).toByteArray(), 0, result, 24, 2);
			System.arraycopy(innerKey, 0, result, 26, innerKey.length);
			return result;
		} else {
			return org.springframework.security.crypto.codec.Base64.decode(privateKey.getBytes());
		}
	}

	/**
	 *
	 * @param content
	 * @param privateKey
	 * @param input_charset
	 * @return
	 */
	private static String signByPrivate(String content, PrivateKey privateKey, String input_charset) {
		Signature signature;
		try {
			signature = Signature.getInstance(SIGN_SHA1WITHRSA);
			signature.initSign(privateKey);
			signature.update(content.getBytes(input_charset));

			return new String(org.springframework.security.crypto.codec.Base64.encode(signature.sign()));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException | SignatureException | InvalidKeyException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 *
	 * @param content
	 * @param sign
	 * @param publicKey
	 * @param input_charset
	 * @return
	 */
	public static boolean verifyX509(String content, String sign, String publicKey, String input_charset) {
		try {
			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			byte[] encodedKey = org.springframework.security.crypto.codec.Base64.decode(publicKey.getBytes());
			PublicKey pubKey = keyFactory.generatePublic(new X509EncodedKeySpec(encodedKey));
			return verify(content, sign, pubKey, input_charset);
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			logger.error("验签发生了异常", e);
		}
		return false;
	}

	public static boolean verify(String content, String sign, PublicKey publicKey, String inputCharset) {
		try {
			Signature signature = Signature.getInstance(SIGN_SHA1WITHRSA);
			signature.initVerify(publicKey);
			signature.update(content.getBytes(inputCharset));
			boolean bverify = signature.verify(org.springframework.security.crypto.codec.Base64.decode(sign.getBytes()));
			return bverify;
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException | SignatureException | InvalidKeyException e) {
			logger.error("验签发生了异常", e);
		}
		return false;
	}

	// 公钥加密
	public static String encrypt(String data, String pub_key) {
		try {

			KeyFactory rsaKeyFac = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(pub_key));
			RSAPublicKey pbk = (RSAPublicKey) rsaKeyFac.generatePublic(keySpec);

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
			cipher.init(Cipher.ENCRYPT_MODE, pbk);

			byte[] encDate = cipher.doFinal(data.getBytes("UTF-8"));

			return Base64.encode(encDate);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	public static void main(String args[]) throws Exception {
		// Map<String, Object> keyMap = genKeyPair();
		// System.out.println("公钥: \n" + getPublicKey(keyMap));
		// System.out.println();
		// System.out.println("私钥： \n" + getPrivateKey(keyMap));

		String charSet = "utf-8";
		String content = "body=商品&cpId=660002&cpOrderId=3453645675674576&mchCreateIp=127.0.0.1&nonceStr=sdfasdfasdfasdf&notifyUrl=http://hsfasd.com&serviceName=hhly.pay.weixin.native&totalFee=1";
		String privateKey = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCQjPQZXumNm9qBpA2CbTD9DPsM3lDI6M25e0MDk4rgPRrWWTquKQgc+wuB3krOzd1zGsUxMxmyZ3yXRj1QrB7FY1+NsZvrYFY1lh0gHIiLfdIGb4yPtkEKe7Lm5hTqTtTAJgkn9DNOVCsodbGxrbVvJWPXXisrwBJP7z03VdEn+S9w3g4ejq6pie69R7Wnt9rtHAvOVLXtwBLJuHYgst9v3gU+AsQjPqXvu+iP24SheSyMbI5D03Amhqt+IOAhHM8FUsG44AFFDYN/hvktdCehIbNrHcxxr++RKLn8TGpYqdzHLLGdDfxLCFMQwOD1YM0iQGJU/g1ggrmcS510EjHxAgMBAAECggEAC81mdNfaC/Tov2MMs1SFBnRT3zYbtHvFqFpXG2ilky+KDGxWrTeucOdShjUHOKF4bliGN0Er3PQ0KQnUIc0b6hP2DlStHWfNQizSjfemcnVAe8QJ3wYLg3acIdwxYvwyxMmWP8vhkIiwyWnqq046nSuZXlFTBwx0slAfWHBKVpATexhClsg5YZATJK2UleDiUEjscCHD3ZFa2uwHOTaAYaEob6wurH1BH2tKVz0YX0bcxFl46WnH9JMj1khXD5UROrtqh5vSheejF3pPk+zpppxr8hApYiimYV7IiH6W573TU0RL+WSUaAaINKwx9KEyYrq/7CXZX17FKTtxBj/dEQKBgQDQWTZFB1zjAzGeHcp83gHfkKwuX4Dq4oyWjnP1mFaNhUBPmBivcjiZ+Ev315PZxFz7jf9UF+d9AIHr4DXclLRF9/tn/s47cw4+q3OHnsXVGfJoxJ3fuHsqOs/QX3jDIcKEhB95fIo2RvWlMvzraJNpCuCEmbRYhS9TrGrt0ch07QKBgQCxnGFUf7B42KcHPvVKnGHqRgkpmA6XKujVD1rKDbVG29rlTm4z2h94GqD9Rt/q9BxVMyL29a8gJn4ODodLsnvrkUHbbIyfm6Q6AsABW5APGCQeLrMLwd0lsGzRufTWCRWatZoyWmBhYFB6tsFkvfniA4OGVHzGvS3E8z7frn40lQKBgDU7Fcp9HxZA8qRbKCjO79uWsucGL4HTb+fnbWkKCtMNgGjVizkIDb4I2h/wTC4PJVJ/7QQnN1WQk6EYH6rDs2tM+EaRq081Diak11eHETRo9K4fzdYi5BTeAcpTGh+AuJnyx7faQdWsO4aBiGz8wuzLgqFeQ9156aWPH6KzPmvhAoGALn3T0G2YQRndJv1o8f3bd5qbnWKrNxDJX7QkPM23zPOR6gJMwI/xOtwllgbuaC88F2VKWsDW9aB90Kom6j6vR2t/hLN2X/U62OCNaA2SuRDFq4zn20UE4W+8HG4D098O469zzIhxwCuZBXY/S2KbPXlT/V6yRseQaoSNybmAWb0CgYBnRh/jJV0E4pOmPAaEZf0ljzIInW6RmTblS0hnYX8d/O177q9xzJVgqklfMMIZPN5w+jnMiXfDfsADDlobO/BOdWr0vAy6ACEgOfAouDEP4zjLn2zFf2MvqM46jgkWiW/0lgcevEzi2J0PjYcEisMuloIDOdY9zpTEiS7WkwgIzQ==";
//		String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDUKKtVsew+HA0Jb8qFP8QU3Xyp14UuzqUZ0M7SLeeHrbJIsxKXapHxW8x1ILx1AG4djsp3Lv31CBZSroAoeaiXSYZMro5gBNSB08xqt50mSFOJhtksfoMEXAxY00G2FLAT2MK7gpntaiFSq09jT1Yu54fk2iIORyg3yJhWjDg4dQIDAQAB";
		System.out.println(signRSA256(content, privateKey, charSet));
		// System.out.println(verify(content,sign(content,privateKey,charSet),publicKey,charSet));

		// String sign = MD5.sign(content, "&key=134f3368f38a37e959af132c6973d71e", charSet).toUpperCase();
		// System.out.println("生成的签名：" + sign);
	}

}
