package no.fint.consumer.models.profilbilde;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.event.SynchronousEvents;
import no.fint.consumer.exceptions.*;
import no.fint.consumer.status.StatusCache;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.Operation;
import no.fint.event.model.Status;
import no.fint.model.profilbilde.ProfilbildeActions;
import no.fint.model.resource.profilbilde.ProfilbildeResource;
import no.fint.model.resource.profilbilde.ProfilbildeResources;
import no.fint.relations.FintRelationsMediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
//import org.springframework.util.StringUtils;
import org.apache.commons.lang3.StringUtils;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.naming.NameNotFoundException;
import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = RestEndpoints.PROFILBILDE, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class ProfilbildeController {
    @Autowired
    private ProfilbildeCacheService cacheService;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private ProfilbildeLinker linker;

    @Autowired
    private ConsumerProps props;

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private StatusCache statusCache;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SynchronousEvents synchronousEvents;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/last-updated")
    public Map<String, String> getLastUpdated(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (cacheService == null) {
            throw new CacheDisabledException("Anmerkninger cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        String lastUpdated = Long.toString(cacheService.getLastUpdated(orgId));
        return ImmutableMap.of("lastUpdated", lastUpdated);
    }

    @GetMapping("/cache/size")
    public ImmutableMap<String, Integer> getCacheSize(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (cacheService == null) {
            throw new CacheDisabledException("Anmerkninger cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        return ImmutableMap.of("size", cacheService.getCacheSize(orgId));
    }

//    @PostMapping("/cache/rebuild")
//    public void rebuildCache(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
//        if (props.isOverrideOrgId() || orgId == null) {
//            orgId = props.getDefaultOrgId();
//        }
//        cacheService.rebuildCache(orgId);
//    }

    @GetMapping
    public ProfilbildeResources getProfilbilde(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(required = false) Long sinceTimeStamp,
            @RequestParam(defaultValue = "0") int size,
            @RequestParam(defaultValue = "0") int offset,
            HttpServletRequest request) {

        if (cacheService == null) {
            throw new CacheDisabledException("Profilbilde cache is disabled.");
        }
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.debug("OrgId: {}, Client: {}", orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, ProfilbildeActions.GET_ALL_PROFILBILDE, client);
        event.setOperation(Operation.READ);
        if (StringUtils.isNotBlank(request.getQueryString())) {
            event.setQuery("?" + request.getQueryString());
        }
        fintAuditService.audit(event);
        fintAuditService.audit(event, Status.CACHE);

        Stream<ProfilbildeResource> resources;
        if (size > 0 && offset >= 0 && sinceTimeStamp > 0) {
            resources = cacheService.streamSliceSince(orgId, sinceTimeStamp, offset, size);
        } else if (size > 0 && offset >= 0) {
            resources = cacheService.streamSlice(orgId, offset, size);
        } else if (sinceTimeStamp > 0) {
            resources = cacheService.streamSince(orgId, sinceTimeStamp);
        } else {
            resources = cacheService.streamAll(orgId);
        }

        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        return linker.toResources(resources, offset, size, cacheService.getCacheSize(orgId));
//        if (props.isOverrideOrgId() || orgId == null) {
//            orgId = props.getDefaultOrgId();
//        }
//        if (client == null) {
//            client = props.getDefaultClient();
//        }
//        log.info("OrgId: {}, Client: {}", orgId, client);
//
//        Event event = new Event(orgId, Constants.COMPONENT, ProfilbildeActions.GET_ALL_PROFILBILDE, client);
//        fintAuditService.audit(event);
//
//        fintAuditService.audit(event, Status.CACHE);
//
//        List<ProfilbildeResource> profilbilde;
//        if (sinceTimeStamp == null) {
//            profilbilde = cacheService.getAll(orgId);
//        } else {
//            profilbilde = cacheService.getAll(orgId, sinceTimeStamp);
//        }
//
//        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);
//
//        return linker.toResources(profilbilde);
    }

    @GetMapping("/systemid/{id:.+}")
    public ResponseEntity<?> getProfilbildeBySystemId(
            @PathVariable String id,
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam String s,
            @RequestParam(required = false, defaultValue = "0,0,0,0") String r,
            @RequestParam(required = false, defaultValue = "jpeg") String t) {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.info("SystemId: {}, OrgId: {}, Client: {}, s: {}, t: {}", id, orgId, client, s, t);

        Event event = new Event(orgId, Constants.COMPONENT, ProfilbildeActions.GET_PROFILBILDE, client);
        event.setQuery(id);
        fintAuditService.audit(event);

        fintAuditService.audit(event, Status.CACHE);

        Optional<ProfilbildeResource> profilbilde = cacheService.getProfilbildeBySystemId(orgId, id);

        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        if (profilbilde.isPresent()) {
            ProfilbildeResource profilbildeResource = profilbilde.get();
            if ("json".equalsIgnoreCase(t)) {
                return ResponseEntity.ok(profilbildeResource);
            }
            if (!s.contains("x"))
                s = s + "x" + s;
            HttpHeaders headers = new HttpHeaders();
            if (!StringUtils.isEmpty(profilbildeResource.getAutorisasjon()))
                headers.set(HttpHeaders.AUTHORIZATION, profilbildeResource.getAutorisasjon());
            headers.set(HeaderConstants.ORG_ID, orgId);
            headers.set(HeaderConstants.CLIENT, client);
            return restTemplate.exchange("/{s}/filters:round_corner({r}):format({t})/{file}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class,
                    s, r, t,
                    profilbildeResource.getFilnavn());
        } else {
            throw new EntityNotFoundException(id);
        }
    }

    @PostMapping
    public ResponseEntity postProfilbilde(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestBody ProfilbildeResource body,
            @RequestParam String s,
            @RequestParam(required = false, defaultValue = "0,0,0,0") String r,
            @RequestParam(required = false, defaultValue = "jpeg") String t) {
        log.info("postProfilbilde, OrgId: {}, Client: {}, s: {}, t: {}", orgId, client, s, t);
        log.trace("Body: {}", body);
        linker.mapLinks(body);
        Optional<ProfilbildeResource> result = cacheService.getProfilbildeByLink(orgId, body);

        if (result.isPresent()) {
            URI location = UriComponentsBuilder
                    .fromUriString(linker.getSelfHref(result.get()))
                    .queryParam("s", s)
                    .queryParam("t", t)
                    .queryParam("r", r)
                    .build()
                    .toUri();
            return ResponseEntity.status(HttpStatus.SEE_OTHER).location(location).build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }


    //
    // Exception handlers
    //
    @ExceptionHandler(UpdateEntityMismatchException.class)
    public ResponseEntity handleUpdateEntityMismatch(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity handleEntityNotFound(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(CreateEntityMismatchException.class)
    public ResponseEntity handleCreateEntityMismatch(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e));
    }

    @ExceptionHandler(EntityFoundException.class)
    public ResponseEntity handleEntityFound(Exception e) {
        return ResponseEntity.status(HttpStatus.FOUND).body(ErrorResponse.of(e));
    }

    @ExceptionHandler(NameNotFoundException.class)
    public ResponseEntity handleNameNotFound(Exception e) {
        return ResponseEntity.badRequest().body(ErrorResponse.of(e));
    }

    @ExceptionHandler(UnknownHostException.class)
    public ResponseEntity handleUnkownHost(Exception e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ErrorResponse.of(e));
    }

}
