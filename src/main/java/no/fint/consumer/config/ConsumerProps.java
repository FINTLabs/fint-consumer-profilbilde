package no.fint.consumer.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@Component
public class ConsumerProps {

    @Value("${fint.consumer.override-org-id:false}")
    private boolean overrideOrgId;

    @Value("${fint.consumer.default-client:FINT}")
    private String defaultClient;

    @Value("${fint.consumer.default-org-id:fint.no}")
    private String defaultOrgId;

    @Value("${fint.consumer.status.created:false}")
    private boolean useCreated;

    private Set<String> assets;

    @Autowired
    private void setupOrgs(@Value("${fint.events.orgIds:}") String[] orgs) {
        assets = new HashSet<>(Arrays.asList(orgs));
    }

    public String[] getOrgs() {
        return assets.toArray(new String[0]);
    }

}
