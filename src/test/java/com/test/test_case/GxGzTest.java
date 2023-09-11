package com.test.test_case;

import com.dome.OkHttpUtils;
import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


public class GxGzTest {


    @Test
    @Description("junit")
    @Step("测试")
    public void testJunit5(){
        System.out.println("testJunit5:hello world junit5");
    }

    @ParameterizedTest
    @Description("广州天气")
    @Step("城市")
    @ValueSource(strings = {"广州", "深圳", "佛山", "湛江", "茂名", "清远"})
    public void testGZ(String city) {
        String sync = OkHttpUtils.getInstance()
                .addHeader("Content-Type","application/json;charset=utf-8")
                .addParam("appid", "31392142")
                .addParam("appsecret", "uoIrS6l0")
                .addParam("city", city)
                .addParam("unescape", "1")
                .url("https://v0.yiketianqi.com/free/day")
                .get()
                .sync();
        System.out.println(city+":"+sync);
    }

    @ParameterizedTest
    @Description("广西天气")
    @Step("城市")
    @ValueSource(strings = {"南宁", "柳州", "桂林", "百色", "玉林", "贵港"})
    public void testGX(String city) {
        String sync = OkHttpUtils.getInstance()
                .addHeader("Content-Type","application/json")
                .addParam("appid", "31392142")
                .addParam("appsecret", "uoIrS6l0")
                .addParam("city", city)
                .addParam("unescape", "1")
                .url("https://v0.yiketianqi.com/free/day")
                .get()
                .sync();
        System.out.println(city+":"+sync);
    }
}
