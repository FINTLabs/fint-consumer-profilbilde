package no.fint.consumer.models.avatar;

import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.avatar.AvatarResource;
import no.fint.model.resource.avatar.AvatarResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class AvatarLinker extends FintLinker<AvatarResource> {

    public AvatarLinker() {
        super(AvatarResource.class);
    }

    @Override
    public AvatarResources toResources(Collection<AvatarResource> collection) {
        AvatarResources resources = new AvatarResources();
        collection.stream().map(this::toResource).forEach(resources::addResource);
        resources.addSelf(Link.with(self()));
        return resources;

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
