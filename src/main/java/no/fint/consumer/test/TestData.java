package no.fint.consumer.test;

import lombok.extern.slf4j.Slf4j;
import no.fint.consumer.event.EventListener;
import no.fint.event.model.Event;
import no.fint.model.avatar.AvatarActions;
import no.fint.model.felles.kompleksedatatyper.Identifikator;
import no.fint.model.resource.Link;
import no.fint.model.resource.avatar.AvatarResource;
import no.fint.relations.config.FintRelationsProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("test")
public class TestData {

    @Autowired
    EventListener eventListener;

    @Autowired
    FintRelationsProps fintRelationsProps;

    @PostConstruct
    public void init() throws NoSuchAlgorithmException {
        eventListener.accept(createEvent(AvatarActions.GET_ALL_AVATAR, IntStream.range(50000, 50010).mapToObj(this::createAvatar).collect(Collectors.toList())));
    }

    private AvatarResource createAvatar(int i) {
        Identifikator id = new Identifikator();
        id.setIdentifikatorverdi(UUID.randomUUID().toString());
        AvatarResource avatarResource = new AvatarResource();
        avatarResource.setSystemId(id);
        avatarResource.setFilnavn(UriComponentsBuilder.fromHttpUrl("http://10.0.75.1:8080").path(String.format("IMG%06d.jpg", i)).build().toString());
        if (i % 2 == 0) {
            avatarResource.addPersonalressurs(Link.with("${administrasjon.personal.personalressurs}/ansattnummer/" + i));
        } else {
            avatarResource.addElev(Link.with("${utdanning.elev.elev}/elevnummer/" + i));
        }
        return avatarResource;
    }

    private Event createEvent(Enum action, List list) {
        Event result = new Event();
        result.setCorrId(UUID.randomUUID().toString());
        result.setAction(action);
        result.setOrgId("mock.no");
        result.setData(list);
        return result;
    }


}
