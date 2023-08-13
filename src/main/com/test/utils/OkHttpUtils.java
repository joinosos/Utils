package com.test.utils;

import com.alibaba.fastjson2.JSON;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class OkHttpUtils {

    private static OkHttpUtils instance = null;
    private String CHARSET = "UTF-8";
    private static final int TIME_OUT = 60;

    private static OkHttpClient okHttpClient = null;
    private static volatile Semaphore semaphore = null;
    private String url;
    private Request.Builder request;
    private LinkedHashMap<String, String> headMap = null;
    private LinkedHashMap<String, String> bodyMap = null;

    private OkHttpUtils() {
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT, TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .cookieJar(new MyCookieJar())
                .build();
    }

    public static OkHttpUtils getInstance() {
        if (instance == null) {
            instance = new OkHttpUtils();
        }
        return instance;
    }

    /**
     * 用于异步请求时，控制访问线程数，返回结果 * * @return
     */
    private static Semaphore getSemaphoreInstance() {
        synchronized (OkHttpUtils.class) {
            if (semaphore == null) {
                semaphore = new Semaphore(0);
            }
        }
        return semaphore;
    }

    public OkHttpUtils addHeader(String key, String value) {
        if (headMap == null) {
            headMap = new LinkedHashMap<>(16);
        }
        headMap.put(key, value);
        if (headMap.containsValue("Content-Type")) {
            CHARSET = headMap.get("Content-Type");
            if (CHARSET.contains("charset")) {
                CHARSET = CHARSET.substring(CHARSET.indexOf("=") + 1);
            }
        }
        return this;
    }

    public OkHttpUtils addParam(String key, String value) {
        if (bodyMap == null) {
            bodyMap = new LinkedHashMap<>(16);
        }
        bodyMap.put(key, value);
        return this;
    }

    public OkHttpUtils addHeaders(HashMap<String, String> map) {
        if (headMap == null) {
            headMap = new LinkedHashMap<>(16);
        }
        //提取编码方式
        headMap.putAll(map);
        if (headMap.containsValue("Content-Type")) {
            CHARSET = headMap.get("Content-Type");
            if (CHARSET.contains("charset")) {
                CHARSET = CHARSET.substring(CHARSET.indexOf("=") + 1);
            }
        }
        return this;
    }

    public OkHttpUtils addParams(HashMap<String, String> map) {
        if (bodyMap == null) {
            bodyMap = new LinkedHashMap<>(16);
        }
        bodyMap.putAll(map);
        return this;
    }

    /**
     * 添加url * * @param url * @return
     */
    public OkHttpUtils url(String url) {
        this.url = url;
        return this;
    }

    /**
     * 初始化get方法
     * @return
     */
    public OkHttpUtils get() {
        request = new Request.Builder().get();
        StringBuilder urlBuilder = new StringBuilder(url);
        if (bodyMap != null) {
            urlBuilder.append("?");
            try {
                for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
                    urlBuilder.append(URLEncoder.encode(entry.getKey(), CHARSET)).
                            append("=").
                            append(URLEncoder.encode(entry.getValue(), CHARSET)).
                            append("&");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            urlBuilder.deleteCharAt(urlBuilder.length() - 1);
        }
        request.url(urlBuilder.toString());
        return this;
    }

    /**
     * 初始化post方法
     * @param isJsonPost true等于json的方式提交数据
     * @return
     */
    public OkHttpUtils post(boolean isJsonPost) {
        RequestBody requestBody;
        if (isJsonPost) {
            String json = "";
            if (bodyMap != null) {
                json = JSON.toJSONString(bodyMap);
            }
            requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), json);
        } else {
            FormBody.Builder formBody = new FormBody.Builder();
            if (bodyMap != null) {
                bodyMap.forEach(formBody::add);
            }
            requestBody = formBody.build();
        }
        request = new Request.Builder().post(requestBody).url(url);
        return this;
    }

    /**
     * 同步请求 * * @return
     */
    public String sync(String... filePath) {
        setHeader(request);
        try {
            Response response = okHttpClient.newCall(request.build()).execute();
            if (filePath.length<=0) {
                return string(response.body().byteStream());
            } else {
                down(response.body().byteStream(),filePath[0]);
                return "下载文件成功";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "请求失败：" + e.getMessage();
        }
    }

    /**
     * 异步请求，有返回值
     */
    public String async(String... filePath) {
        StringBuilder buffer = new StringBuilder("");
        setHeader(request);
        okHttpClient.newCall(request.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                buffer.append("请求出错：").append(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (filePath.length<=0){
                    buffer.append(string(response.body().byteStream()));
                }else {
                    down(response.body().byteStream(),filePath[0]);
                    buffer.append("下载文件成功");
                }
                getSemaphoreInstance().release();
            }
        });
        try {
            getSemaphoreInstance().acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    /**
     * 为request添加请求头 * * @param request
     */
    private void setHeader(Request.Builder request) {
        if (headMap != null) {
            try {
                for (Map.Entry<String, String> entry : headMap.entrySet()) {
                    request.addHeader(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 下载文件
     * @param input
     * @param filePath
     * @throws IOException
     */
    private void down(InputStream input,String filePath) throws IOException {
        File file = new File(filePath);
        //跟目录是否存在
        if (file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }else {
            //跟目录存在,判断文件名是否存在
            if (file.exists()){
                file.delete();
            }
        }
        //下载文件
        FileOutputStream output=new FileOutputStream(file);
        byte[] bytes=new byte[10240];
        while (input.read(bytes)!=-1){
            output.write(bytes);
            output.flush();
            input.close();
        }
        output.close();
        input.close();
    }

    /**
     * 读取字符串
     * @param input
     * @return
     */
    private String string(InputStream input) throws IOException {
        StringBuffer buffer=new StringBuffer();
        InputStreamReader inReader=new InputStreamReader(input,CHARSET);
        BufferedReader reader=new BufferedReader(inReader);
        String line="";
        while ((line=reader.readLine())!=null){
            buffer.append(line);
        }
        inReader.close();
        reader.close();
        return buffer.toString();
    }

    /**
     * Cookie管理
     */
    class MyCookieJar implements CookieJar {

        List<Cookie> cookiesList = new ArrayList<>();

        @NotNull
        @Override
        public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
            return cookiesList;
        }

        @Override
        public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
            cookiesList.clear();
            cookiesList.addAll(list);
        }
    }
}
