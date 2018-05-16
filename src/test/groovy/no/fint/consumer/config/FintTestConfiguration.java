package no.fint.consumer.config;

import com.google.common.collect.ImmutableMap;
import no.fint.audit.FintAuditService;
import no.fint.cache.CacheManager;
import no.fint.cache.FintCacheManager;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.events.FintEvents;
import no.fint.relations.FintResourceCompatibility;
import no.fint.relations.config.FintRelationsProps;
import no.fint.relations.internal.FintLinkMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import spock.mock.DetachedMockFactory;
import spock.mock.MockFactory;

import java.util.Map;

public class FintTestConfiguration {

    MockFactory mockFactory = new DetachedMockFactory();

    @Qualifier("linkMapper")
    @Bean
    public Map<String, String> linkMapper() {
        return ImmutableMap.<String, String>builder()
                .put("personalressurs", "/administrasjon/personal/personalressurs")
                .put("person", "/felles/person")
                .build();
    }

    @Bean
    FintLinkMapper fintLinkMapper() {
        return new FintLinkMapper();
    }

    @Bean
    ConsumerProps consumerProps() {
        return new ConsumerProps();
    }

    @Bean
    FintRelationsProps fintRelationsProps() {
        return new FintRelationsProps();
    }

    @Bean
    CacheManager cacheManager() {
        return new FintCacheManager();
    }

    @Bean
    FintAuditService fintAuditService() {
        return mockFactory.Mock(FintAuditService.class);
    }

    @Bean
    FintEvents fintEvents() {
        return mockFactory.Mock(FintEvents.class);
    }

    @Bean
    ConsumerEventUtil consumerEventUtil() {
        return mockFactory.Mock(ConsumerEventUtil.class);
    }

    @Bean
    FintResourceCompatibility fintResourceCompatibility() {
        return mockFactory.Mock(FintResourceCompatibility.class);
    }
}
