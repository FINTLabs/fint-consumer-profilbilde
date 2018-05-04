package no.fint.consumer.models.avatar;

import no.fint.model.resource.avatar.AvatarResource;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

@Component
public class AvatarLinker extends FintLinker<AvatarResource> {

    public AvatarLinker() {
        super(AvatarResource.class);
    }

    @Override
    public String getSelfHref(AvatarResource avatar) {
        return createHrefWithId(avatar.getSystemId().getIdentifikatorverdi(), "systemid");
    }
}
