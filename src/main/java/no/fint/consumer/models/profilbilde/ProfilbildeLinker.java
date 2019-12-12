package no.fint.consumer.models.profilbilde;

import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.profilbilde.ProfilbildeResource;
import no.fint.model.resource.profilbilde.ProfilbildeResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class ProfilbildeLinker extends FintLinker<ProfilbildeResource> {

    public ProfilbildeLinker() {
        super(ProfilbildeResource.class);
    }

    @Override
    public ProfilbildeResources toResources(Collection<ProfilbildeResource> collection) {
        ProfilbildeResources resources = new ProfilbildeResources();
        collection.stream().map(this::toResource).forEach(resources::addResource);
        resources.addSelf(Link.with(self()));
        return resources;

    }

    @Override
    public void mapLinks(FintLinks resource) {
        super.mapLinks(resource);
    }

    @Override
    public String getSelfHref(ProfilbildeResource profilbilde) {
        return createHrefWithId(profilbilde.getSystemId().getIdentifikatorverdi(), "systemid");
    }
}
