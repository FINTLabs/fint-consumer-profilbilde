package no.fint.consumer.event

import no.fint.consumer.status.StatusCache
import no.fint.event.model.Event
import spock.lang.Specification

class EventListenerSpec extends Specification {
    private EventListener eventListener
    private StatusCache statusCache

    void setup() {
        statusCache = Mock(StatusCache)
        eventListener = new EventListener(cacheServices: [], statusCache: statusCache)
    }

    def "No exception is thrown when receiving event"() {
        when:
        eventListener.accept(new Event(corrId: '123'))

        then:
        noExceptionThrown()
        1 * statusCache.containsKey(_ as String) >> false
    }
}
