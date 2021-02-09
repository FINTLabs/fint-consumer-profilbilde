package no.fint.consumer.event

import no.fint.audit.FintAuditService
import no.fint.event.model.Event
import no.fint.event.model.Status
import spock.lang.Specification

class EventListenerSpec extends Specification {
    private EventListener eventListener
    private FintAuditService fintAuditService

    void setup() {
        fintAuditService = Mock()
        eventListener = new EventListener(
                cacheServices: [],
                fintAuditService: fintAuditService)
    }

    def "No exception is thrown when receiving event"() {
        when:
        eventListener.accept(new Event(corrId: '123'))

        then:
        noExceptionThrown()
        1 * fintAuditService.audit(_ as Event, _ as Status)
    }
}
