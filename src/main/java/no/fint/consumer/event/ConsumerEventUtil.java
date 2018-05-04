package no.fint.consumer.event;

import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.event.model.Event;
import no.fint.event.model.Status;
import no.fint.event.model.health.Health;
import no.fint.events.FintEvents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
public class ConsumerEventUtil {

    @Autowired
    private FintEvents fintEvents;

    @Autowired
    private FintAuditService fintAuditService;

    public Optional<Event<Health>> healthCheck(Event<Health> event) {
        fintAuditService.audit(event);

        fintAuditService.audit(event, Status.DOWNSTREAM_QUEUE);

        log.info("Sending health check event {} to {}", event.getAction(), event.getOrgId());
        Event<Health> response = fintEvents.sendHealthCheck(event);
        if (response == null) {
            return Optional.empty();
        } else {
            fintAuditService.audit(response, Status.SENT_TO_CLIENT);
            return Optional.of(response);
        }
    }

    public void send(Event event) {
        fintAuditService.audit(event);

        fintAuditService.audit(event, Status.DOWNSTREAM_QUEUE);

        log.info("Sending event {} to {}", event.getAction(), event.getOrgId());
        fintEvents.sendDownstream(event);
        fintAuditService.audit(event, Status.SENT_TO_CLIENT);
    }
}
