package no.fint.consumer.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.hazelcast.config.*;
import no.fint.cache.CacheManager;
import no.fint.cache.FintCacheManager;
import no.fint.cache.HazelcastCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;

@Configuration
public class Config {

    @Value("${server.context-path:}")
    private String contextPath;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper.setDateFormat(new ISO8601DateFormat());
    }

    @Qualifier("linkMapper")
    @Bean
    public Map<String, String> linkMapper() {
        return LinkMapper.linkMapper(contextPath);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // Do any additional configuration here
        return builder.build();
    }
}
