package com.hhly.paycore.paychannel.yeepay.utils;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: yingjie.wang
 * @since : 2015-10-10 17:16
 */

public class ClearDataUtils {

    public static final String CHARSET = "UTF-8";

    //超时时间设置为1小时
    public static final int TIMEOUT = 60 * 60 * 1000;

    //get请求
    public static InputStream httpGet(String url, String merchantaccount, String data, String encryptkey) {

        if (url == null || url.equals("")) {
            return null;
        }

        InputStream result = null;

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;

        //超时设置
        RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(TIMEOUT).setSocketTimeout(TIMEOUT).build();

        try {

            List<NameValuePair> pairs = new ArrayList<NameValuePair>();
            pairs.add(new BasicNameValuePair("merchantaccount", merchantaccount));
            pairs.add(new BasicNameValuePair("data", data));
            pairs.add(new BasicNameValuePair("encryptkey", encryptkey));
            url = url + "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs, CHARSET));

            httpGet = new HttpGet(url);
            httpGet.setConfig(requestConfig);
            response = httpClient.execute(httpGet);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                httpGet.abort();
                throw new RuntimeException("HttpClient,error status code :" + statusCode);
            }

            HttpEntity entity = response.getEntity();
            result = entity.getContent();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // 关闭连接,释放资源
                if (response != null) {
                    response.close();
                }
                if (httpClient != null) {
                    httpClient.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static String getPathOfClearData(String url, String merchantaccount, String data, String encryptkey, String path) {

        String time = String.valueOf(System.currentTimeMillis());
        String fileName = "clearData" + "_" + time + ".txt";
        String filePath = path + File.separator + fileName;

        InputStream responseStream = httpGet(url, merchantaccount, data, encryptkey);

        try {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            file.createNewFile();

            FileOutputStream orderFile = new FileOutputStream(filePath);
            BufferedInputStream bis = new BufferedInputStream(responseStream);

            byte[] by = new byte[1024];
            while (true) {
                int i = bis.read(by);
                if (i == -1) {
                    break;
                }
                orderFile.write(by, 0, i);
            }
            orderFile.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return filePath;
    }
}