package no.fint.consumer.models.avatar

import no.fint.consumer.config.FintTestConfiguration
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.Link
import no.fint.model.resource.avatar.AvatarResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [AvatarCacheService.class, AvatarLinker.class, FintTestConfiguration.class])
class AvatarCacheServiceSpec extends Specification {

    @Autowired
    AvatarCacheService avatarCacheService

    @Autowired
    AvatarLinker linker

    void setup() {
        ArrayList<AvatarResource> resources = [
                new AvatarResource(systemId: new Identifikator(identifikatorverdi: 'A'), links: [
                        "personalressurs": [Link.with('${personalressurs}/ansattnummer/12345')]
                ]),
                new AvatarResource(systemId: new Identifikator(identifikatorverdi: 'B'), links: [
                        "person": [Link.with('${person}/fodselsnummer/123456')],
                        "personalressurs": [Link.with('${personalressurs}/ansattnummer/23456')]
                ]),
                new AvatarResource(systemId: new Identifikator(identifikatorverdi: 'C'), links: [
                        "person": [Link.with('${person}/fodselsnummer/234567')]
                ])
        ]
        linker.toResources(resources)
        avatarCacheService.createCache('mock.no')
        avatarCacheService.add('mock.no', resources)
    }

    def 'Get Avatar A by ansattnummer 12345 using keyword link'() {
        given:
        def res = new AvatarResource(links: [ "personalressurs": [Link.with('${personalressurs}/ansattnummer/12345')]])

        when:
        linker.mapLinks(res)
        def result = avatarCacheService.getAvatarByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'A'
    }

    def 'Get Avatar C by fodselsnummer 234567 using keyword link'() {
        given:
        def res = new AvatarResource(links: [ "person": [Link.with('${person}/fodselsnummer/234567')]])

        when:
        linker.mapLinks(res)
        def result = avatarCacheService.getAvatarByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'C'
    }

    def 'Get Avatar B by declaring both relations'() {
        given:
        def res = new AvatarResource(links: [
                "person": [Link.with('${person}/fodselsnummer/123456')],
                "personalressurs": [Link.with('${personalressurs}/ansattnummer/23456')]
        ])

        when:
        linker.mapLinks(res)
        def result = avatarCacheService.getAvatarByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'B'

    }

    def 'Get Avatar C by fodselsnummer 234567 using expanded link'() {
        given:
        def res = new AvatarResource(links: [ "person": [Link.with('https://api.felleskomponent.no/felles/person/fodselsnummer/234567')]])

        when:
        linker.mapLinks(res)
        def result = avatarCacheService.getAvatarByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'C'
    }

    def 'Get Avatar without links should not fail'() {
        given:
        def res = new AvatarResource()

        when:
        linker.mapLinks(res)
        def result = avatarCacheService.getAvatarByLink('mock.no', res)

        then:
        result
        !result.isPresent()
    }

}
