package no.fint.consumer.models.profilbilde;

import no.fint.model.resource.FintLinks;
import no.fint.model.resource.Link;
import no.fint.model.resource.profilbilde.ProfilbildeResource;
import no.fint.model.resource.profilbilde.ProfilbildeResources;
import no.fint.relations.FintLinker;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static java.util.Objects.isNull;
import static org.springframework.util.StringUtils.isEmpty;

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

    public int[] hashCodes(ProfilbildeResource profilbildeResource) {
        IntStream.Builder builder = IntStream.builder();
        if (!isNull(profilbildeResource.getSystemId()) && !isEmpty(profilbildeResource.getSystemId().getIdentifikatorverdi())) {
            builder.add(profilbildeResource.getSystemId().getIdentifikatorverdi().hashCode());
        }
        return IntStream.concat(
                builder.build(),
                profilbildeResource
                        .getLinks()
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .map(Link::getHref)
                        .mapToInt(Objects::hashCode))
                .toArray();
    }
}
