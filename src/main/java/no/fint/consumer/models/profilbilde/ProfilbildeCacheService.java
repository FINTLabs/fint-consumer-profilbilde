package no.fint.consumer.models.profilbilde;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import no.fint.cache.CacheService;
import no.fint.cache.model.CacheObject;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.event.model.Event;
import no.fint.model.profilbilde.ProfilbildeActions;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.Link;
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
import java.util.stream.Collectors;

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
        props.getAssets().forEach(this::createCache);
    }

    @Scheduled(initialDelayString = Constants.CACHE_INITIALDELAY_PROFILBILDE, fixedRateString = Constants.CACHE_FIXEDRATE_PROFILBILDE)
    public void populateCacheAll() {
        props.getAssets().forEach(this::populateCache);
    }

    public void rebuildCache(String orgId) {
        flush(orgId);
        populateCache(orgId);
    }

    @Override
    public void populateCache(String orgId) {
        log.info("Populating Profilbilde cache for {}", orgId);
        Event event = new Event(orgId, Constants.COMPONENT, ProfilbildeActions.GET_ALL_PROFILBILDE, Constants.CACHE_SERVICE);
        consumerEventUtil.send(event);
    }

    public Optional<ProfilbildeResource> getProfilbildeBySystemId(String orgId, String systemId) {
        return getOne(orgId, systemId.hashCode(),
                (resource) -> Optional
                        .ofNullable(resource)
                        .map(ProfilbildeResource::getSystemId)
                        .map(Identifikator::getIdentifikatorverdi)
                        .map(systemId::equals)
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
        data.forEach(linker::mapLinks);
        updateCache(event.getOrgId(), data
                .stream()
                .map(i -> new CacheObject<>(i, linker.hashCodes(i)))
                .collect(Collectors.toList())
        );
        log.info("Updated cache for {} with {} cache objects", event.getOrgId(), data.size());
    }

    public Optional<ProfilbildeResource> getProfilbildeByLink(String orgId, ProfilbildeResource body) {
        log.debug("Trying to find {} ...", body);
        return body.getLinks()
                .values()
                .stream()
                .flatMap(List::stream)
                .map(Link::getHref)
                .map(link -> getOne(orgId, link.hashCode(),
                        e -> {
                            log.debug("Hashcode match: {}", e);
                            return e.getLinks()
                                    .values()
                                    .stream()
                                    .flatMap(List::stream)
                                    .map(Link::getHref)
                                    .anyMatch(link::equals);
                        }
                ))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findAny();
    }
}
