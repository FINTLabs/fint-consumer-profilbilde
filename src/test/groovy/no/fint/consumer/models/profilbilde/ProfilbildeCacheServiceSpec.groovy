package no.fint.consumer.models.profilbilde

import no.fint.cache.model.CacheObject
import no.fint.consumer.config.FintTestConfiguration
import no.fint.model.felles.kompleksedatatyper.Identifikator
import no.fint.model.resource.Link
import no.fint.model.resource.profilbilde.ProfilbildeResource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [ProfilbildeCacheService.class, ProfilbildeLinker.class, FintTestConfiguration.class])
class ProfilbildeCacheServiceSpec extends Specification {

    @Autowired
    ProfilbildeCacheService profilbildeCacheService

    @Autowired
    ProfilbildeLinker linker

    void setup() {
        ArrayList<ProfilbildeResource> resources = [
                new ProfilbildeResource(systemId: new Identifikator(identifikatorverdi: 'A'), links: [
                        "personalressurs": [Link.with('${personalressurs}/ansattnummer/12345')]
                ]),
                new ProfilbildeResource(systemId: new Identifikator(identifikatorverdi: 'B'), links: [
                        "person": [Link.with('${person}/fodselsnummer/123456')],
                        "personalressurs": [Link.with('${personalressurs}/ansattnummer/23456')]
                ]),
                new ProfilbildeResource(systemId: new Identifikator(identifikatorverdi: 'C'), links: [
                        "person": [Link.with('${person}/fodselsnummer/234567')]
                ])
        ]
        profilbildeCacheService.createCache('mock.no')
        profilbildeCacheService.addCache('mock.no', resources.each { linker.mapLinks(it) }.collect { new CacheObject<>(it, linker.hashCodes(it)) } )
    }

    def 'Get Profilbilde A by ansattnummer 12345 using keyword link'() {
        given:
        def res = new ProfilbildeResource(links: [ "personalressurs": [Link.with('${personalressurs}/ansattnummer/12345')]])

        when:
        linker.mapLinks(res)
        def result = profilbildeCacheService.getProfilbildeByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'A'
    }

    def 'Get Profilbilde C by fodselsnummer 234567 using keyword link'() {
        given:
        def res = new ProfilbildeResource(links: [ "person": [Link.with('${person}/fodselsnummer/234567')]])

        when:
        linker.mapLinks(res)
        def result = profilbildeCacheService.getProfilbildeByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'C'
    }

    def 'Get Profilbilde B by declaring both relations'() {
        given:
        def res = new ProfilbildeResource(links: [
                "person": [Link.with('${person}/fodselsnummer/123456')],
                "personalressurs": [Link.with('${personalressurs}/ansattnummer/23456')]
        ])

        when:
        linker.mapLinks(res)
        def result = profilbildeCacheService.getProfilbildeByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'B'

    }

    def 'Get Profilbilde C by fodselsnummer 234567 using expanded link'() {
        given:
        def res = new ProfilbildeResource(links: [ "person": [Link.with('https://api.felleskomponent.no/felles/person/fodselsnummer/234567')]])

        when:
        linker.mapLinks(res)
        def result = profilbildeCacheService.getProfilbildeByLink('mock.no', res)

        then:
        result
        result.isPresent()
        result.get().systemId.identifikatorverdi == 'C'
    }

    def 'Get Profilbilde without links should not fail'() {
        given:
        def res = new ProfilbildeResource()

        when:
        linker.mapLinks(res)
        def result = profilbildeCacheService.getProfilbildeByLink('mock.no', res)

        then:
        result
        !result.isPresent()
    }

}
