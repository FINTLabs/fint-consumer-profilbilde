package no.fint.consumer.models.avatar;

import no.fint.model.resource.FintLinks;
import no.fint.model.resource.avatar.AvatarResource;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

@Component
public class AvatarLinker extends FintLinker<AvatarResource> {

    public AvatarLinker() {
        super(AvatarResource.class);
    }

    @Override
    public void mapLinks(FintLinks resource) {
        super.mapLinks(resource);
    }

    @Override
    public String getSelfHref(AvatarResource avatar) {
        return createHrefWithId(avatar.getSystemId().getIdentifikatorverdi(), "systemid");
    }
}
