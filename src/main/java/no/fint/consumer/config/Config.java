package no.fint.consumer.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Map;

@Configuration
public class Config {

    @Value("${server.context-path:}")
    private String contextPath;

    @Value("${fint.thumbor.url}")
    private String thumborUrl;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        objectMapper.setDateFormat(new ISO8601DateFormat()).disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Qualifier("linkMapper")
    @Bean
    public Map<String, String> linkMapper() {
        return LinkMapper.linkMapper(contextPath);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        final Logger log = LoggerFactory.getLogger("no.fint.consumer.thumbor");
        return builder
                .rootUri(thumborUrl)
                .additionalInterceptors((request, body, execution) -> {
                    log.debug("{} {}", request.getMethod(), request.getURI());
                    return execution.execute(request, body);
                })
                .build();
    }
}
