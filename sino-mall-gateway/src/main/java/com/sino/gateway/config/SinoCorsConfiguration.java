package com.sino.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SinoCorsConfiguration {

    /**
     * 跨域web过滤器
     *
     * @return {@link CorsWebFilter}
     */

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 配置跨域
        corsConfiguration.addAllowedHeader("*");  //设置允许跨域请求头
        corsConfiguration.addAllowedMethod("*");  //设置允许跨域请求方法
        // 允许访问的客户端域名
        List<String> allowedOriginPatterns = new ArrayList<>();
        allowedOriginPatterns.add("*");
        // corsConfiguration.addAllowedOrigin("*"); //springboot 2.4x  不能使用*号设置允许的Origin
        corsConfiguration.setAllowedOriginPatterns(allowedOriginPatterns); //设置允许跨域请求来源
        corsConfiguration.setAllowCredentials(true);  //是否允许携带cookie
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
