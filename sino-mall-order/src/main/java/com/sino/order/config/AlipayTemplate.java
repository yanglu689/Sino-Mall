package com.sino.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import com.sino.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "alipay")
@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private String app_id = "9021000129636230";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private String merchant_private_key = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDolQUAxCNa4gNqX5vDx+Ge8Nr4FG3jdGweT0+jf7N+QGOn/y85B4eJrRAv4lO1PRB8w6DYuq7ndo+zkhzZE7pe1tXbXN6AYj9Ng3m0lzzHhYlSa/qSZWHrNryOxGbeEHKxbpwBF39MNTPUyt1kkkbgnYjKCqes807KSK1YGDBRmqPQ4dhNBGwUgmyQxsdTPuG6BHI0zWzBu4m6BVfYEUyjvKqN46MugSDaaWMlOtNCIOtaGDD55X9KahQoCcf+bqpwPw+R+IoVu9obGDNY0/6H0SNlsi5raebsuD/D6YjZGC6ypcHfXnQJEsdFrPt06R7LUa4gyUQisM4722OTUUjnAgMBAAECggEBAMoF1wipVgR1WX4megh/MI18rNbb9++giuFxBr4ACItbprSgRgaFcce22d6d+xPsbMvSqX5X+eD37S6PdhtgtXv3pF8ctfBSZqYP08F3tMWiSnuba1WKVpmXPAXOt4OgSd+xMjUIkfxCIjlt1QysuoAUlmLZCniCByhSJK/mouHdhv25hU2wwEXTZmwvMCwWxzkbFYZ9xo8+jQnZheyu8GGGiOBydviM2xb5yyfuw5f2zINNIWylVmmGZOjInyEnVqKjqfDdqlSuSrETXdIlENAd+VbA3Mar7GJ+tMzCvQEwCXv9QZUfT0Daxztw6XZe4t+4eapWtuU69EeYieYd5QECgYEA+MXvbynglwtAnEE+fIZD9NnKOWS+oJ6pSLSMVZUBV2hPwHR0tGkBofMCb8Jz+PkBeJusTdPNBwgbLrGXa5M1JAUKu6qWm1TBNfEDQGboNMJ5JMTkesoNQDW+nNLKys5HGHKeDobNMYmXqlMJxsrcq4Hl5jcf0XQMePdw2cEKwkECgYEA71as2ryY/WrPFVxJrrOoTJ5zs1nHc4NibSmr6qkR5Vit4uGzHGBL/OmXvkkk01R0JNF2r50KINvxPQ0eWOKaCsR84XyyJ6h8OPmjoL2m86a3FUNahBSXczr+v5KVt8F0ttJ/BxRjTE0DwrMezI0obOzbOJHQg1AGOjgr5YEvcScCgYAIcyCo0/FO8BDnvceTjzPrsyINRQC8j+cMEyXZGlCLWX2r+cilABQQHiLtDNvHjx2frRnzIsiJ/pp9wYZ1HvDlIk61BRxMdlqBrIgBvf0RHbHjr9Ra31YH4ktxJC/DJ7J+gBYiRC3gCt5d6KgiWm2YXbcjVKf+A0URLsZkfwwQgQKBgQCvl/XFnWzZMA2ybD0a0XB+lhno0cuFMjV6RYJ97YDVtRQOSuyvuu7FJVRRJTTBX0q+4HalLSALL5Jz6jYDnGzLzZCZlnTWp8RLwMuN2NauF/hmvz2ffcQHpJbWUmNJp8NLhm/v78NSbYZ+yA+mn0GzeKr1e/VxAbsv55o9DWSTLQKBgAHFkwdPp6N5y2Ro5IE0HwmO5QG2VwNBprmBb/hHjvGLeAoQYh8YGQzUSM48TXK1EtJu5i9rXAv6Yu/a6rQvIakrZ0OLfjKRXMyYPTu3C6ASk4JNx2PtYJmISFhv7uBUvl+eSTvghwx6AaNFD4afF9SzXJd2LJAcIfO0WYfq9J1X";

    // private  String merchant_private_key = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCSgX/nTQ0lD+S8ObaM5LGZ1hiz18GXnNpqPLhJCym4xOpn35FNPHrPkDGEoMKrZ5LJeA4cZulckD8AtpvBCpeyIkrj/i1WVmSg10hVX67MlVets4UecCHZv2hKAN0/iId76kozdqrd7Csp/YgXPquN9Np0NFotggTrmiBANk+vcpTF9SCGrDq/isOoCvClfbvVJjApfLLOel3yECe5K/SZ8puiWILVm1NxEXAqJ8z0ipPZVGrXsT6Bo0pEyCPcEL0SqaC9WT0zdWQzdUknCzZV9W2wKjEXBJG9hqxay5kPaKm9leBatSkDAaDxH/N5g36HRfY7BmklwRZsp17lHinxAgMBAAECggEAfnnfck35WBKFc90a9D0F+Xlzr+ZGEV3uzKIIsb46UXFlrzC5HoVkvEWOCiJCjHiIpvbGr8xED43TZgk/IwLC/JxQLM0kVJGWo6fWoSVOIP2YSLNe620APBvaq3BdkFiMJfSYBB+g2J7mkIR39SE8Nvu3j3QWmYzSNJbE2spINnwTzNBL1OPaB5h3hSjyI07KaUcOjhTBF0EZl83NlBDsxmQvy0NmuOIWAcIXXvGoIbwkA774J3LhwL+VS4W2FpQj4FlxvDlPu24GeNWN7oO66T3Jp9bweO120ObhuKwZQosDGkJq0975zVSJX5QtUWHMM/QDPO8Pk24n2AoPcACQcQKBgQDS6kqD+sK8dDBpkmxYopA1gJJATnur0RHFZJb5webOhnEZnePhB1hhhGvKFcrdY2hcYeQiUZkHMsnWItNUe9E9ccp4++m6KKG0iV/BQda7zx1zMTTZUMvSbO282Q31YnQu7Yz6BSk4f/U5Qbu61AK53Tv1ejSAgQhXt1Pwq8KD7QKBgQCx0pkqW4+53tY2o4iPqFGjKYI2yk5bAH5etmOvW51OZ4Slsq/aUJKBVG6fOpRVKkiXulHhrp5csZH0/C7kaj4Hy7TjgUKSWvwlv7i7jgN0dq/bhVJz82y+N9pENWvy5J0I8Kt67XH+6JDEGWjlV58auifMRSx5mRJNn5pM6qrFlQKBgFyZWm/JV1fv1xVyoLjlXlTvBsbO7kMH/jpgqFwtAk1n/x3VEShJ1kayIbTOjotWSopMvCFJG9tqM+0cyxWLatkELXWifAIsNpqRuYWah1FbZD2fu+kxLNtM0a+YyCUUvZeg2cUnIOraWupxbp9e13eMpvdmWMiWXfhM18CRWEwdAoGAUwT0l076EhgUQJwm1JML0jY94eCfpmLbnNJgRe1qysEPr+B1s2IslA7cOqC5we0kyRmmwsuoibQpZYwbRG7JmRAk2pZtgzDRSbpxv7a0rDoBLmbXMOU0Hraqw2+Bf3v2SMc79/9FWnIvrC4EyBYZZPwGOpsNAZRSdEUQX9qrceUCgYB99OOtFFt1ixzyTCyUj3Fuiw7BsPhdI3nuMSoNTPIDNpzRBp/KFXyv/FNJ2CjTAsX3OR3D6KmEYihqUfrYeb0P5zoybcQLMxbXxK+ec6F2o6U2iqFIq0MKwHUqsb9X3pj4qE0ZHbFgRtIHnL2/QGV5PFJdmIZIBKZcvB8fW6ztDA==";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsFHf/XFLfcfHRre32ps68m/VKVciowEKs47d2FxJAf85aRf1I6kuEFMbkSwLpLoVMmbKlpRURYRQGZEDC+iEqZ/E/7lHvVyFvDe6a490xLpaOnBO5MQTkddNAQPpHAT7hBaPoKDy4uuEX22LNIy0sGeZSJS4iuxMyFSn4QmKWciccEl1go+Du3bEZet3Pj8k15XNYd2Ge3qX9NVzCULDafmrrpbx/Hm4E5qSnhumBGrm7kjrcwyjlVUDS6Yw0FCaU0pJ5z1ZhTjL47beECGCjEtAvBAEmgDoEJxwn3CrTNfMdmmmTtE6SBhYheKNNS9iGCIpHakxWF2syKgv+Oc9JwIDAQAB";
    // private String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjrEVFMOSiNJXaRNKicQuQdsREraftDA9Tua3WNZwcpeXeh8Wrt+V9JilLqSa7N7sVqwpvv8zWChgXhX/A96hEg97Oxe6GKUmzaZRNh0cZZ88vpkn5tlgL4mH/dhSr3Ip00kvM4rHq9PwuT4k7z1DpZAf1eghK8Q5BgxL88d0X07m9X96Ijd0yMkXArzD7jg+noqfbztEKoH3kPMRJC2w4ByVdweWUT2PwrlATpZZtYLmtDvUKG/sOkNAIKEMg3Rut1oKWpjyYanzDgS7Cg3awr1KPTl9rHCazk15aNYowmYtVabKwbGVToCAGK+qQ1gT3ELhkGnf3+h53fukNqRH+wIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    // private  String notify_url;
    private String notify_url = "http://x849ei.natappfree.cc/trade/notify";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    // private  String return_url;
    private String return_url = "http://member.sinomall.com/memberOrder.html";

    // 签名方式
    private String sign_type = "RSA2";

    // 字符编码格式
    private String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    // private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";
    private String gatewayUrl = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";

    private String timeout = "30m";

    public String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\"" + out_trade_no + "\","
                + "\"total_amount\":\"" + total_amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"body\":\"" + body + "\","
                + "\"timeout_express\":\"" + timeout + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应：" + result);

        return result;

    }


    // 商户appid
    private String APPID = "9021000129636230";
    // 私钥 pkcs8格式的
    private String RSA_PRIVATE_KEY = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQDolQUAxCNa4gNqX5vDx+Ge8Nr4FG3jdGweT0+jf7N+QGOn/y85B4eJrRAv4lO1PRB8w6DYuq7ndo+zkhzZE7pe1tXbXN6AYj9Ng3m0lzzHhYlSa/qSZWHrNryOxGbeEHKxbpwBF39MNTPUyt1kkkbgnYjKCqes807KSK1YGDBRmqPQ4dhNBGwUgmyQxsdTPuG6BHI0zWzBu4m6BVfYEUyjvKqN46MugSDaaWMlOtNCIOtaGDD55X9KahQoCcf+bqpwPw+R+IoVu9obGDNY0/6H0SNlsi5raebsuD/D6YjZGC6ypcHfXnQJEsdFrPt06R7LUa4gyUQisM4722OTUUjnAgMBAAECggEBAMoF1wipVgR1WX4megh/MI18rNbb9++giuFxBr4ACItbprSgRgaFcce22d6d+xPsbMvSqX5X+eD37S6PdhtgtXv3pF8ctfBSZqYP08F3tMWiSnuba1WKVpmXPAXOt4OgSd+xMjUIkfxCIjlt1QysuoAUlmLZCniCByhSJK/mouHdhv25hU2wwEXTZmwvMCwWxzkbFYZ9xo8+jQnZheyu8GGGiOBydviM2xb5yyfuw5f2zINNIWylVmmGZOjInyEnVqKjqfDdqlSuSrETXdIlENAd+VbA3Mar7GJ+tMzCvQEwCXv9QZUfT0Daxztw6XZe4t+4eapWtuU69EeYieYd5QECgYEA+MXvbynglwtAnEE+fIZD9NnKOWS+oJ6pSLSMVZUBV2hPwHR0tGkBofMCb8Jz+PkBeJusTdPNBwgbLrGXa5M1JAUKu6qWm1TBNfEDQGboNMJ5JMTkesoNQDW+nNLKys5HGHKeDobNMYmXqlMJxsrcq4Hl5jcf0XQMePdw2cEKwkECgYEA71as2ryY/WrPFVxJrrOoTJ5zs1nHc4NibSmr6qkR5Vit4uGzHGBL/OmXvkkk01R0JNF2r50KINvxPQ0eWOKaCsR84XyyJ6h8OPmjoL2m86a3FUNahBSXczr+v5KVt8F0ttJ/BxRjTE0DwrMezI0obOzbOJHQg1AGOjgr5YEvcScCgYAIcyCo0/FO8BDnvceTjzPrsyINRQC8j+cMEyXZGlCLWX2r+cilABQQHiLtDNvHjx2frRnzIsiJ/pp9wYZ1HvDlIk61BRxMdlqBrIgBvf0RHbHjr9Ra31YH4ktxJC/DJ7J+gBYiRC3gCt5d6KgiWm2YXbcjVKf+A0URLsZkfwwQgQKBgQCvl/XFnWzZMA2ybD0a0XB+lhno0cuFMjV6RYJ97YDVtRQOSuyvuu7FJVRRJTTBX0q+4HalLSALL5Jz6jYDnGzLzZCZlnTWp8RLwMuN2NauF/hmvz2ffcQHpJbWUmNJp8NLhm/v78NSbYZ+yA+mn0GzeKr1e/VxAbsv55o9DWSTLQKBgAHFkwdPp6N5y2Ro5IE0HwmO5QG2VwNBprmBb/hHjvGLeAoQYh8YGQzUSM48TXK1EtJu5i9rXAv6Yu/a6rQvIakrZ0OLfjKRXMyYPTu3C6ASk4JNx2PtYJmISFhv7uBUvl+eSTvghwx6AaNFD4afF9SzXJd2LJAcIfO0WYfq9J1X";
    // 服务器异步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    private String notify_url2 = "http://x849ei.natappfree.cc/trade/notify";
    // 页面跳转同步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问 商户可以自定义同步跳转地址
    private String return_url2 = "http://member.sinomall.com/memberOrder.html";
    // 请求网关地址
    private String URL = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    // 编码
    private String CHARSET = "UTF-8";
    // 返回格式
    private String FORMAT = "json";
    // 支付宝公钥
    private String ALIPAY_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjrEVFMOSiNJXaRNKicQuQdsREraftDA9Tua3WNZwcpeXeh8Wrt+V9JilLqSa7N7sVqwpvv8zWChgXhX/A96hEg97Oxe6GKUmzaZRNh0cZZ88vpkn5tlgL4mH/dhSr3Ip00kvM4rHq9PwuT4k7z1DpZAf1eghK8Q5BgxL88d0X07m9X96Ijd0yMkXArzD7jg+noqfbztEKoH3kPMRJC2w4ByVdweWUT2PwrlATpZZtYLmtDvUKG/sOkNAIKEMg3Rut1oKWpjyYanzDgS7Cg3awr1KPTl9rHCazk15aNYowmYtVabKwbGVToCAGK+qQ1gT3ELhkGnf3+h53fukNqRH+wIDAQAB";
    // 日志记录目录
    private String log_path = "/log";
    // RSA2
    private String SIGNTYPE = "RSA2";

    public String pay2(PayVo vo) throws AlipayApiException {
        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        // 订单名称，必填
        String subject = vo.getSubject();
        System.out.println(subject);
        // 付款金额，必填
        String total_amount = vo.getTotal_amount();
        // 商品描述，可空
        String body = vo.getBody();
        // 超时时间 可空
        String timeout_express = "2m";
        // 销售产品码 必填
        String product_code = "QUICK_WAP_WAY";
        /**********************/
        // SDK 公共请求类，包含公共请求参数，以及封装了签名与验签，开发者无需关注签名与验签
        //调用RSA签名方式
        AlipayClient client = new DefaultAlipayClient(URL, APPID, RSA_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGNTYPE);
        AlipayTradeWapPayRequest alipay_request = new AlipayTradeWapPayRequest();

        // 封装请求支付信息
        AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
        model.setOutTradeNo(out_trade_no);
        model.setSubject(subject);
        model.setTotalAmount(total_amount);
        model.setBody(body);
        model.setTimeoutExpress(timeout_express);
        model.setProductCode(product_code);
        alipay_request.setBizModel(model);
        // 设置异步通知地址
        alipay_request.setNotifyUrl(notify_url2);
        // 设置同步地址
        alipay_request.setReturnUrl(return_url2);

        // form表单生产
        // 调用SDK生成表单
        String result = client.pageExecute(alipay_request).getBody();
        return result;
    }
}
