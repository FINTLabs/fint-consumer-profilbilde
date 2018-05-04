package no.fint.consumer.event

import no.fint.audit.FintAuditService
import no.fint.event.model.Event
import no.fint.event.model.Status
import no.fint.events.FintEvents
import spock.lang.Specification

class ConsumerEventUtilSpec extends Specification {
    private ConsumerEventUtil consumerEventUtil
    private FintEvents fintEvents
    private FintAuditService fintAuditService

    void setup() {
        fintEvents = Mock(FintEvents)
        fintAuditService = Mock(FintAuditService)
        consumerEventUtil = new ConsumerEventUtil(fintEvents: fintEvents, fintAuditService: fintAuditService)
    }

    def "Send and receive Event"() {
        given:
        def event = new Event(orgId: 'rogfk.no', corrId: '123')

        when:
        def response = consumerEventUtil.healthCheck(event)

        then:
        2 * fintAuditService.audit(_ as Event, _ as Status)
        1 * fintEvents.sendHealthCheck(event) >> event
        response.isPresent()
    }
}
