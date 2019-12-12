package no.fint.consumer.config;

import com.google.common.collect.ImmutableMap;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.model.resource.Link;
import no.fint.model.resource.profilbilde.ProfilbildeResource;

import java.util.Map;

public class LinkMapper {
    public static Map<String, String> linkMapper(String contextPath) {
        return ImmutableMap.<String, String>builder()
                .put(Link.getHrefPlaceholder(ProfilbildeResource.class), contextPath + RestEndpoints.PROFILBILDE)
                .put("administrasjon.personal.personalressurs", "/administrasjon/personal/personalressurs")
                .put("utdanning.elev.elev", "/utdanning/elev/elev")
                .build();
    }
}
