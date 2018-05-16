package no.fint.consumer.models.avatar;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import no.fint.cache.CacheService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.event.model.Event;
import no.fint.model.avatar.AvatarActions;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.avatar.AvatarResource;
import no.fint.relations.FintResourceCompatibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AvatarCacheService extends CacheService<AvatarResource> {

    public static final String MODEL = "avatar";

    @Value("${fint.consumer.compatibility.fintresource:true}")
    private boolean checkFintResourceCompatibility;

    @Autowired
    private FintResourceCompatibility fintResourceCompatibility;

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private ConsumerProps props;

    @Autowired
    private AvatarLinker linker;

    private JavaType javaType;

    private ObjectMapper objectMapper;

    public AvatarCacheService() {
        super(MODEL, AvatarActions.GET_ALL_AVATAR);
        objectMapper = new ObjectMapper();
        javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, AvatarResource.class);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @PostConstruct
    public void init() {
        Arrays.stream(props.getOrgs()).forEach(this::createCache);
    }

    @Scheduled(initialDelayString = ConsumerProps.CACHE_INITIALDELAY_AVATAR, fixedRateString = ConsumerProps.CACHE_FIXEDRATE_AVATAR)
    public void populateCacheAll() {
        Arrays.stream(props.getOrgs()).forEach(this::populateCache);
    }

    public void rebuildCache(String orgId) {
        flush(orgId);
        populateCache(orgId);
    }

    private void populateCache(String orgId) {
        log.info("Populating Avatar cache for {}", orgId);
        Event event = new Event(orgId, Constants.COMPONENT, AvatarActions.GET_ALL_AVATAR, Constants.CACHE_SERVICE);
        consumerEventUtil.send(event);
    }

    public Optional<AvatarResource> getAvatarBySystemId(String orgId, String systemId) {
        return getOne(orgId, (resource) -> Optional
                .ofNullable(resource)
                .map(AvatarResource::getSystemId)
                .map(Identifikator::getIdentifikatorverdi)
                .map(_id -> _id.equals(systemId))
                .orElse(false));
    }

    @Override
    public void onAction(Event event) {
        List<AvatarResource> data;
        if (checkFintResourceCompatibility && fintResourceCompatibility.isFintResourceData(event.getData())) {
            log.info("Compatibility: Converting FintResource<AvatarResource> to AvatarResource ...");
            data = fintResourceCompatibility.convertResourceData(event.getData(), AvatarResource.class);
        } else {
            data = objectMapper.convertValue(event.getData(), javaType);
        }
        data.forEach(linker::toResource);
        update(event.getOrgId(), data);
        log.info("Updated cache for {} with {} elements", event.getOrgId(), data.size());
    }

    public Optional<AvatarResource> getAvatarByLink(String orgId, AvatarResource body) {
        log.debug("Trying to find {} ...", body);
        return getOne(orgId, avatarResource -> avatarResource.getLinks().entrySet().stream()
                .filter(e -> body.getLinks().keySet().contains(e.getKey())).anyMatch(entry -> {
                    log.debug("Checking {}", entry);
                    return entry.getValue().stream().anyMatch(body.getLinks().get(entry.getKey())::contains);
        }));
    }
}
