package no.fint.consumer.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.cache.CacheService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.event.model.DefaultActions;
import no.fint.event.model.Event;
import no.fint.event.model.Status;
import no.fint.events.FintEventListener;
import no.fint.events.FintEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EventListener implements FintEventListener {

    @Autowired
    private List<CacheService> cacheServices;
    
	@Autowired
	private FintEvents fintEvents;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private ConsumerProps props;

    @PostConstruct
    public void init() {
        fintEvents.registerUpstreamSystemListener(this);
        if (cacheServices == null)
            cacheServices = Collections.emptyList();
    	for (String orgId : props.getOrgs()) {
    		fintEvents.registerUpstreamListener(orgId, this);
    	}
    	log.info("Upstream listeners registered.");
    }

    @Scheduled(initialDelayString = "${fint.consumer.register-delay:70000}", fixedDelay = Long.MAX_VALUE)
    public void registerOrgIds() {
        log.info("Bootstrapping orgId registration ...");
        Event event = new Event("", Constants.COMPONENT, DefaultActions.REGISTER_ORG_ID, Constants.COMPONENT_CONSUMER);
        fintEvents.sendDownstream(event);
    }

	@Override
	public void accept(Event event) {
        log.debug("Received event: {}", event);
        log.trace("Event data: {}", event.getData());
        if (event.isRegisterOrgId()) {
            if (props.getAssets().add(event.getOrgId())) {
                log.info("Registering orgId {} for {}", event.getOrgId(), event.getClient());
                fintEvents.registerUpstreamListener(event.getOrgId(), this);
                cacheServices.forEach(c -> c.createCache(event.getOrgId()));
            }
            return;
        } else if (event.isHealthCheck()) {
            log.debug("Ignoring health check.");
            return;
        }

        String action = event.getAction();
        List<CacheService> supportedCacheServices = cacheServices.stream().filter(cacheService -> cacheService.supportsAction(action)).collect(Collectors.toList());
        if (supportedCacheServices.size() > 0) {
            supportedCacheServices.forEach(cacheService -> cacheService.onAction(event));
            fintAuditService.audit(event, Status.CACHE);
        } else {
            log.warn("Unhandled event: {}", event);
        }
    }
	
}
