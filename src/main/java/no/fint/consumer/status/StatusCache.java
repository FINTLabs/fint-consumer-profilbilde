package no.fint.consumer.status;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import no.fint.event.model.Event;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Objects;

@Component
public class StatusCache {

    private Cache<String, Event> cache;

    @Value("${no.fint.consumer.status-cache:expireAfterAccess=30m,expireAfterWrite=6h}")
    private String cacheSpec;

    @PostConstruct
    public void init() {
        cache = CacheBuilder.from(cacheSpec).build();
    }

    public boolean containsKey(String id) {
        return Objects.nonNull(cache.getIfPresent(id));
    }

    public Event get(String id) {
        return cache.getIfPresent(id);
    }

    public void put(String corrId, Event event) {
        cache.put(corrId, event);
    }

}
