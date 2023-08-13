import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class StartTest {

    @ParameterizedTest()
    @ValueSource(strings = {"广州","深圳","佛山","湛江","茂名","清远"})
    public void test(String city){
        String sync = OkHttpUtils.getInstance()
                .addParam("appid", "23035354")
                .addParam("appsecret", "8YvlPNrz")
                .addParam("cityid", "0")
                .addParam("city", city)
                .addParam("ip", "")
                .addParam("callback", "0")
                .url("https://tianqiapi.com/free/day")
                .get()
                .sync();
        System.out.println(sync);

    }
}
