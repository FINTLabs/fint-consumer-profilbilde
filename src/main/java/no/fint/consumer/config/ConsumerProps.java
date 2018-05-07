package no.fint.consumer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class ConsumerProps {

    @Value("${fint.consumer.override-org-id:false}")
    private boolean overrideOrgId;

    @Value("${fint.consumer.default-client:FINT}")
    private String defaultClient;

    @Value("${fint.consumer.default-org-id:fint.no}")
    private String defaultOrgId;

    @Value("${fint.events.orgIds:fint.no}")
    private String[] orgs;

    @Value("${fint.thumbor.url}")
    private String thumborUrl;

    public static final String CACHE_INITIALDELAY_AVATAR = "${fint.consumer.image.initialDelay.avatar:60000}";
    public static final String CACHE_FIXEDRATE_AVATAR = "${fint.consumer.image.fixedRate.avatar:900000}";

}

