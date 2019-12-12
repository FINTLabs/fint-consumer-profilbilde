package no.fint.consumer.models.profilbilde;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import no.fint.cache.CacheService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.event.model.Event;
import no.fint.model.profilbilde.ProfilbildeActions;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.profilbilde.ProfilbildeResource;
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
public class ProfilbildeCacheService extends CacheService<ProfilbildeResource> {

    public static final String MODEL = "profilbilde";

    @Value("${fint.consumer.compatibility.fintresource:true}")
    private boolean checkFintResourceCompatibility;

    @Autowired
    private FintResourceCompatibility fintResourceCompatibility;

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private ConsumerProps props;

    @Autowired
    private ProfilbildeLinker linker;

    private JavaType javaType;

    private ObjectMapper objectMapper;

    public ProfilbildeCacheService() {
        super(MODEL, ProfilbildeActions.GET_ALL_PROFILBILDE);
        objectMapper = new ObjectMapper();
        javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, ProfilbildeResource.class);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    @PostConstruct
    public void init() {
        Arrays.stream(props.getOrgs()).forEach(this::createCache);
    }

    @Scheduled(initialDelayString = ConsumerProps.CACHE_INITIALDELAY_PROFILBILDE, fixedRateString = ConsumerProps.CACHE_FIXEDRATE_PROFILBILDE)
    public void populateCacheAll() {
        Arrays.stream(props.getOrgs()).forEach(this::populateCache);
    }

    public void rebuildCache(String orgId) {
        flush(orgId);
        populateCache(orgId);
    }

    private void populateCache(String orgId) {
        log.info("Populating Profilbilde cache for {}", orgId);
        Event event = new Event(orgId, Constants.COMPONENT, ProfilbildeActions.GET_ALL_PROFILBILDE, Constants.CACHE_SERVICE);
        consumerEventUtil.send(event);
    }

    public Optional<ProfilbildeResource> getProfilbildeBySystemId(String orgId, String systemId) {
        return getOne(orgId, (resource) -> Optional
                .ofNullable(resource)
                .map(ProfilbildeResource::getSystemId)
                .map(Identifikator::getIdentifikatorverdi)
                .map(_id -> _id.equals(systemId))
                .orElse(false));
    }

    @Override
    public void onAction(Event event) {
        List<ProfilbildeResource> data;
        if (checkFintResourceCompatibility && fintResourceCompatibility.isFintResourceData(event.getData())) {
            log.info("Compatibility: Converting FintResource<ProfilbildeResource> to ProfilbildeResource ...");
            data = fintResourceCompatibility.convertResourceData(event.getData(), ProfilbildeResource.class);
        } else {
            data = objectMapper.convertValue(event.getData(), javaType);
        }
        data.forEach(linker::toResource);
        update(event.getOrgId(), data);
        log.info("Updated cache for {} with {} elements", event.getOrgId(), data.size());
    }

    public Optional<ProfilbildeResource> getProfilbildeByLink(String orgId, ProfilbildeResource body) {
        log.debug("Trying to find {} ...", body);
        return getOne(orgId, profilbildeResource -> profilbildeResource.getLinks().entrySet().stream()
                .filter(e -> body.getLinks().keySet().contains(e.getKey())).anyMatch(entry -> {
                    log.debug("Checking {}", entry);
                    return entry.getValue().stream().anyMatch(body.getLinks().get(entry.getKey())::contains);
        }));
    }
}
