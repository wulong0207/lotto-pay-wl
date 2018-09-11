package com.hhly.paycore.paychannel.yeepay2.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hhly.skeleton.base.exception.Assert;
import com.yeepay.g3.sdk.yop.YopServiceException;
import com.yeepay.g3.sdk.yop.client.YopConstants;
import com.yeepay.g3.sdk.yop.config.AppSDKConfig;
import com.yeepay.g3.sdk.yop.config.AppSDKConfigSupport;
import com.yeepay.g3.sdk.yop.config.ConfigUtils;
import com.yeepay.g3.sdk.yop.config.SDKConfig;
import com.yeepay.g3.sdk.yop.exception.YopClientException;
import com.yeepay.g3.sdk.yop.utils.JsonUtils;
import com.yeepay.shade.com.google.common.collect.ArrayListMultimap;
import com.yeepay.shade.com.google.common.collect.Multimap;

public class YopRequest {
	private static Logger logger = LoggerFactory.getLogger(YopRequest.class);

	private String locale = "zh_CN";

	private String signAlg = "SHA1";

	private Multimap<String, String> paramMap = ArrayListMultimap.create();

	private Multimap<String, Object> multiportFiles = ArrayListMultimap.create();

	private Map<String, String> headers = new HashMap<String, String>();

	private List<String> ignoreSignParams = new ArrayList(Arrays.asList(new String[] { "sign" }));
	private final AppSDKConfig appSDKConfig;
	private final String secretKey;

	private static SDKConfig loadConfig(String configFile) {
		InputStream fis = null;
		SDKConfig config = null;
		try {
			fis = ConfigUtils.getInputStream(configFile);
			config = (SDKConfig) JsonUtils.loadFrom(fis, SDKConfig.class);

			if (null != fis)
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		} catch (Exception ex) {
			throw new YopServiceException(ex, "Errors occurred when loading SDK Config.");
		} finally {
			if (null != fis)
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		if (StringUtils.endsWith(config.getServerRoot(), "/")) {
			config.setServerRoot(StringUtils.substring(config.getServerRoot(), 0, -1));
		}
		return config;
	}

	private AppSDKConfig call() {
		AppSDKConfig appSDKConfig = new AppSDKConfig();
		try {
			SDKConfig sdkConfig = loadConfig("config/yop_sdk_config_default.json");
			appSDKConfig.setAppKey(sdkConfig.getAppKey());
			appSDKConfig.setAesSecretKey(sdkConfig.getAesSecretKey());
			appSDKConfig.setServerRoot(sdkConfig.getServerRoot());
			if ((sdkConfig.getYopPublicKey() != null) && (sdkConfig.getYopPublicKey().length >= 1)) {
				appSDKConfig.storeYopPublicKey(sdkConfig.getYopPublicKey());
			}
			if ((sdkConfig.getIsvPrivateKey() != null) && (sdkConfig.getIsvPrivateKey().length >= 1)) {
				appSDKConfig.storeIsvPrivateKey(sdkConfig.getIsvPrivateKey());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return appSDKConfig;
	}

	public YopRequest() {
		this.appSDKConfig = call();
		if (this.appSDKConfig == null) {
			throw new YopServiceException("Default SDKConfig not found.");
		}
		this.secretKey = null;
		init();
	}

	public YopRequest(String appKey) {
		this.appSDKConfig = call();
		if (this.appSDKConfig == null) {
			throw new YopServiceException("SDKConfig for appKey:" + appKey + " not found.");
		}
		this.secretKey = null;
		init();
	}

	public YopRequest(String appKey, String secretKey) {

		this.appSDKConfig = call();
		this.appSDKConfig.setAppKey(appKey);

		AppSDKConfig appSDKConfig = AppSDKConfigSupport.getConfigWithDefault(appKey);
		if (appSDKConfig == null) {
			throw new YopServiceException("SDKConfig not found.");
		}
		this.appSDKConfig.setServerRoot(appSDKConfig.getServerRoot());
		this.appSDKConfig.setYopPublicKey(appSDKConfig.getYopPublicKey());

		this.secretKey = secretKey;
		init();
	}

	public YopRequest(String appKey, String secretKey, String serverRoot) {
		this.appSDKConfig = new AppSDKConfig();
		this.appSDKConfig.setAppKey(appKey);
		if (StringUtils.endsWith(serverRoot, "/"))
			this.appSDKConfig.setServerRoot(StringUtils.substring(serverRoot, 0, -1));
		else {
			this.appSDKConfig.setServerRoot(serverRoot);
		}

		AppSDKConfig appSDKConfig = AppSDKConfigSupport.getConfigWithDefault(appKey);
		if (appSDKConfig == null) {
			throw new YopServiceException("SDKConfig not found.");
		}
		this.appSDKConfig.setYopPublicKey(appSDKConfig.getYopPublicKey());

		this.secretKey = secretKey;
		init();
	}

	private void init() {
		this.headers.put("User-Agent", YopConstants.USER_AGENT);
		this.paramMap.put("appKey", this.appSDKConfig.getAppKey());
		this.paramMap.put("locale", this.locale);
		this.paramMap.put("ts", String.valueOf(System.currentTimeMillis()));
	}

	public YopRequest setParam(String paramName, Object paramValue) {
		removeParam(paramName);
		addParam(paramName, paramValue, false);
		return this;
	}

	public YopRequest addParam(String paramName, Object paramValue) {
		addParam(paramName, paramValue, false);
		return this;
	}

	public YopRequest addParam(String paramName, Object paramValue, boolean ignoreSign) {
		Assert.hasText(paramName, "参数名不能为空");
		if ((paramValue == null) || (((paramValue instanceof String)) && (StringUtils.isBlank((String) paramValue))) || (((paramValue instanceof Collection)) && (((Collection) paramValue).isEmpty()))) {
			this.logger.warn("param " + paramName + "is null or empty，ignore it");
			return this;
		}

		if (StringUtils.equals("_file", paramName)) {
			addFile(paramValue);
			return this;
		}

		if (YopConstants.isProtectedKey(paramName)) {
			this.paramMap.put(paramName, paramValue.toString());
			return this;
		}
		Iterator localIterator;
		if ((paramValue instanceof Collection)) {
			for (localIterator = ((Collection) paramValue).iterator(); localIterator.hasNext();) {
				Object o = localIterator.next();
				if (o != null)
					this.paramMap.put(paramName, o.toString());
			}
		} else if (paramValue.getClass().isArray()) {
			int len = Array.getLength(paramValue);
			for (int i = 0; i < len; i++) {
				Object o = Array.get(paramValue, i);
				if (o != null)
					this.paramMap.put(paramName, o.toString());
			}
		} else {
			this.paramMap.put(paramName, paramValue.toString());
		}

		if (ignoreSign) {
			this.ignoreSignParams.add(paramName);
		}
		return this;
	}

	public List<String> getParam(String key) {
		return (List) this.paramMap.get(key);
	}

	public String getParamValue(String key) {
		return StringUtils.join(this.paramMap.get(key), ",");
	}

	public String removeParam(String key) {
		return StringUtils.join(this.paramMap.removeAll(key), ",");
	}

	public Multimap<String, String> getParams() {
		return this.paramMap;
	}

	public YopRequest addFile(Object file) {
		addFile("_file", file);
		return this;
	}

	public YopRequest addFile(String paramName, Object file) {
		if (((file instanceof String)) || ((file instanceof File)) || ((file instanceof InputStream)))
			this.multiportFiles.put(paramName, file);
		else {
			throw new YopClientException("Unsupported file object.");
		}
		return this;
	}

	public Multimap<String, Object> getMultiportFiles() {
		return this.multiportFiles;
	}

	public boolean hasFiles() {
		return (null != this.multiportFiles) && (this.multiportFiles.size() > 0);
	}

	public Map<String, String> getHeaders() {
		return this.headers;
	}

	public void addHeader(String name, String value) {
		this.headers.put(name, value);
	}

	public String getRequestId() {
		return (String) this.headers.get("x-yop-request-id");
	}

	public void setRequestId(String requestId) {
		this.headers.put("x-yop-request-id", requestId);
	}

	public void setRequestSource(String source) {
		this.headers.put("x-yop-request-source", source);
	}

	public List<String> getIgnoreSignParams() {
		return this.ignoreSignParams;
	}

	public void setLocale(String locale) {
		this.locale = locale;
		this.paramMap.put("locale", this.locale);
	}

	public String getLocale() {
		return this.locale;
	}

	public String getSignAlg() {
		return this.signAlg;
	}

	public void setSignAlg(String signAlg) {
		this.signAlg = signAlg;
	}

	@Deprecated
	public void setEncrypt(boolean encrypt) {
	}

	@Deprecated
	public void setSignRet(boolean signRet) {
	}

	public String getSecretKey() {
		return this.secretKey;
	}

	public String getAesSecretKey() {
		return this.secretKey == null ? this.appSDKConfig.getAesSecretKey() : this.secretKey;
	}

	public AppSDKConfig getAppSDKConfig() {
		return this.appSDKConfig;
	}

	public String toQueryString() {
		StringBuilder builder = new StringBuilder();
		String key;
		for (Iterator<String> localIterator1 = this.paramMap.keySet().iterator(); localIterator1.hasNext();) {
			key = (String) localIterator1.next();
			Collection<String> values = this.paramMap.get(key);
			for (String value : values) {
				builder.append(builder.length() == 0 ? "" : "&");
				builder.append(key);
				builder.append("=");
				builder.append(value);
			}
		}
		return builder.toString();
	}
}