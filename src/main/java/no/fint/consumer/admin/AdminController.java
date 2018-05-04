package no.fint.consumer.admin;

import no.fint.cache.CacheManager;
import no.fint.cache.utils.CacheUri;
import no.fint.consumer.config.Constants;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.DefaultActions;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.health.Health;
import no.fint.event.model.health.HealthStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = RestEndpoints.ADMIN, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public class AdminController {

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private CacheManager<?> cacheManager;

    @GetMapping("/health")
    public ResponseEntity healthCheck(@RequestHeader(HeaderConstants.ORG_ID) String orgId,
                                      @RequestHeader(HeaderConstants.CLIENT) String client) {
        Event<Health> event = new Event<>(orgId, Constants.COMPONENT, DefaultActions.HEALTH, client);
        event.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.SENT_FROM_CONSUMER_TO_PROVIDER));
        Optional<Event<Health>> health = consumerEventUtil.healthCheck(event);

        if (health.isPresent()) {
            Event<Health> receivedHealth = health.get();
            receivedHealth.addData(new Health(Constants.COMPONENT_CONSUMER, HealthStatus.RECEIVED_IN_CONSUMER_FROM_PROVIDER));
            return ResponseEntity.ok(receivedHealth);
        } else {
            event.setMessage("No response from adapter");
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(event);
        }
    }

    @GetMapping("/organisations")
    public Collection<String> getOrganisations() {
        return cacheManager.getKeys();
    }

    @GetMapping("/organisations/{orgId:.+}")
    public Collection<String> getOrganization(@PathVariable String orgId) {
        return cacheManager.getKeys().stream().filter(key -> CacheUri.containsOrgId(key, orgId)).collect(Collectors.toList());
    }

}