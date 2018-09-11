package com.hhly.paycore.paychannel.yeepay2.config;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.yeepay.g3.sdk.yop.client.AbstractClient;
import com.yeepay.g3.sdk.yop.client.YopResponse;
import com.yeepay.g3.sdk.yop.config.AppSDKConfig;
import com.yeepay.g3.sdk.yop.encrypt.CertTypeEnum;
import com.yeepay.g3.sdk.yop.encrypt.DigestAlgEnum;
import com.yeepay.g3.sdk.yop.encrypt.DigitalSignatureDTO;
import com.yeepay.g3.sdk.yop.exception.YopClientException;
import com.yeepay.g3.sdk.yop.http.HttpUtils;
import com.yeepay.g3.sdk.yop.unmarshaller.JacksonJsonMarshaller;
import com.yeepay.g3.sdk.yop.utils.DateUtils;
import com.yeepay.g3.sdk.yop.utils.DigitalEnvelopeUtils;
import com.yeepay.g3.sdk.yop.utils.Exceptions;
import com.yeepay.g3.sdk.yop.utils.RSAKeyUtils;
import com.yeepay.shade.com.google.common.base.Joiner;
import com.yeepay.shade.com.google.common.collect.Lists;
import com.yeepay.shade.com.google.common.collect.Maps;
import com.yeepay.shade.com.google.common.collect.Sets;
import com.yeepay.shade.org.apache.commons.lang3.StringUtils;
import com.yeepay.shade.org.apache.http.client.methods.HttpUriRequest;
import com.yeepay.shade.org.apache.http.client.methods.RequestBuilder;

public class YopClient3 extends AbstractClient {
	protected static final Logger LOGGER = Logger.getLogger(YopClient3.class);

	private static final Set<String> defaultHeadersToSign = Sets.newHashSet();
	private static final Joiner headerJoiner = Joiner.on('\n');
	private static final Joiner signedHeaderStringJoiner = Joiner.on(';');
	private static final String EXPIRED_SECONDS = "1800";

	protected static String richRequest(String methodOrUri, YopRequest request) {
		String serverRoot = request.getAppSDKConfig().getServerRoot();

		String path = methodOrUri;
		if (StringUtils.startsWith(methodOrUri, serverRoot)) {
			path = StringUtils.substringAfter(methodOrUri, serverRoot);
		}

		if (!StringUtils.startsWith(path, "/rest/v")) {
			throw new YopClientException("Unsupported request method.");
		}

		request.setParam("v", StringUtils.substringBetween(methodOrUri, "/rest/v", "/"));
		request.setParam("method", methodOrUri);
		return serverRoot + path;
	}

	public static YopResponse postRsa(String apiUri, YopRequest request) throws IOException {
		String contentUrl = richRequest(apiUri, request);
		sign(apiUri, request);

		RequestBuilder requestBuilder = RequestBuilder.post().setUri(contentUrl);
		for (Map.Entry entry : request.getHeaders().entrySet()) {
			requestBuilder.addHeader((String) entry.getKey(), (String) entry.getValue());
		}
		for (Map.Entry entry : request.getParams().entries()) {
			requestBuilder.addParameter((String) entry.getKey(), (String) entry.getValue());
		}

		HttpUriRequest httpPost = requestBuilder.build();
		YopResponse response = fetchContentByApacheHttpClient(httpPost);
		handleRsaResult(response, request.getAppSDKConfig());
		return response;
	}

	private static void sign(String apiUri, YopRequest request) {
		String appKey = request.getAppSDKConfig().getAppKey();
		String timestamp = DateUtils.formatCompressedIso8601Timestamp(System.currentTimeMillis());

		Map headers = request.getHeaders();
		if (!headers.containsKey("x-yop-request-id")) {
			String requestId = UUID.randomUUID().toString();
			headers.put("x-yop-request-id", requestId);
		}
		headers.put("x-yop-date", timestamp);

		String authString = "yop-auth-v2/" + appKey + "/" + timestamp + "/" + "1800";

		Set headersToSignSet = new HashSet();
		headersToSignSet.add("x-yop-request-id");
		headersToSignSet.add("x-yop-date");

		headers.put("x-yop-appkey", appKey);
		headersToSignSet.add("x-yop-appkey");

		String canonicalURI = HttpUtils.getCanonicalURIPath(apiUri);

		String canonicalQueryString = HttpUtils.getCanonicalQueryString(request.getParams(), true);

		SortedMap headersToSign = getHeadersToSign(headers, headersToSignSet);

		String canonicalHeader = getCanonicalHeaders(headersToSign);
		String signedHeaders = "";
		if (headersToSignSet != null) {
			signedHeaders = signedHeaderStringJoiner.join(headersToSign.keySet());
			signedHeaders = signedHeaders.trim().toLowerCase();
		}

		String canonicalRequest = authString + "\nPOST\n" + canonicalURI + "\n" + canonicalQueryString + "\n" + canonicalHeader;
		PrivateKey isvPrivateKey;
		if (StringUtils.length(request.getSecretKey()) > 128)
			try {
				isvPrivateKey = RSAKeyUtils.string2PrivateKey(request.getSecretKey());
			} catch (NoSuchAlgorithmException e) {
				throw Exceptions.unchecked(e);
			} catch (InvalidKeySpecException e) {
				throw Exceptions.unchecked(e);
			}
		else {
			isvPrivateKey = request.getAppSDKConfig().getIsvPrivateKey();
		}
		if (null == isvPrivateKey) {
			throw new YopClientException("Can't init ISV private key!");
		}

		DigitalSignatureDTO digitalSignatureDTO = new DigitalSignatureDTO();
		digitalSignatureDTO.setPlainText(canonicalRequest);
		digitalSignatureDTO.setCertType(CertTypeEnum.RSA2048);
		digitalSignatureDTO.setDigestAlg(DigestAlgEnum.SHA256);
		digitalSignatureDTO = DigitalEnvelopeUtils.sign(digitalSignatureDTO, isvPrivateKey);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("canonicalRequest:" + canonicalRequest);
			LOGGER.debug("signature:" + digitalSignatureDTO.getSignature());
		}

		headers.put("Authorization", "YOP-RSA2048-SHA256 yop-auth-v2/" + appKey + "/" + timestamp + "/" + "1800" + "/" + signedHeaders + "/" + digitalSignatureDTO.getSignature());
	}

	private static void handleRsaResult(YopResponse response, AppSDKConfig appSDKConfig) {
		String stringResult = response.getStringResult();
		if (StringUtils.isNotBlank(stringResult)) {
			response.setResult(JacksonJsonMarshaller.unmarshal(stringResult, Object.class));
		}

		String sign = response.getSign();
		if (StringUtils.isNotBlank(sign))
			response.setValidSign(verifySignature(stringResult, sign, appSDKConfig));
	}

	public static boolean verifySignature(String result, String expectedSign, AppSDKConfig appSDKConfig) {
		String trimmedBizResult = result.replaceAll("[ \t\n]", "");

		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.trimToEmpty(trimmedBizResult));

		DigitalSignatureDTO digitalSignatureDTO = new DigitalSignatureDTO();
		digitalSignatureDTO.setCertType(CertTypeEnum.RSA2048);
		digitalSignatureDTO.setSignature(expectedSign);
		digitalSignatureDTO.setPlainText(sb.toString());
		try {
			DigitalEnvelopeUtils.verify(digitalSignatureDTO, appSDKConfig.getYopPublicKey());
		} catch (Exception e) {
			LOGGER.error("error verify sign", e);
			return false;
		}
		return true;
	}

	private static String getCanonicalHeaders(SortedMap<String, String> headers) {
		if (headers.isEmpty()) {
			return "";
		}

		List headerStrings = Lists.newArrayList();
		for (Map.Entry entry : headers.entrySet()) {
			String key = (String) entry.getKey();
			if (key == null) {
				continue;
			}
			String value = (String) entry.getValue();
			if (value == null) {
				value = "";
			}
			headerStrings.add(HttpUtils.normalize(key.trim().toLowerCase()) + ':' + HttpUtils.normalize(value.trim()));
		}
		Collections.sort(headerStrings);

		return headerJoiner.join(headerStrings);
	}

	private static SortedMap<String, String> getHeadersToSign(Map<String, String> headers, Set<String> headersToSign) {
		SortedMap ret = Maps.newTreeMap();
		Set tempSet;
		if (headersToSign != null) {
			tempSet = Sets.newHashSet();
			for (String header : headersToSign) {
				tempSet.add(header.trim().toLowerCase());
			}
			headersToSign = tempSet;
		}
		for (Map.Entry entry : headers.entrySet()) {
			String key = (String) entry.getKey();
			if ((entry.getValue() != null) && (!((String) entry.getValue()).isEmpty())
					&& (((headersToSign == null) && (isDefaultHeaderToSign(key))) || ((headersToSign != null) && (headersToSign.contains(key.toLowerCase())) && (!"Authorization".equalsIgnoreCase(key))))) {
				ret.put(key, entry.getValue());
			}
		}

		return ret;
	}

	private static boolean isDefaultHeaderToSign(String header) {
		header = header.trim().toLowerCase();
		return (header.startsWith("x-yop-")) || (defaultHeadersToSign.contains(header));
	}

	static {
		defaultHeadersToSign.add("Host".toLowerCase());
		defaultHeadersToSign.add("Content-Length".toLowerCase());
		defaultHeadersToSign.add("Content-Type".toLowerCase());
		defaultHeadersToSign.add("Content-MD5".toLowerCase());
	}
}