package org.example.evaluation.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.Proxy;

@Configuration
public class RestClientConfig {

    @Bean
    @Primary
    public RestClient customRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        factory.setProxy(Proxy.NO_PROXY);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    @Qualifier("aiRestClient")
    public RestClient aiRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(180000);
        factory.setProxy(Proxy.NO_PROXY);

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
