package no.fint.consumer.models.avatar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import no.fint.audit.FintAuditService;
import no.fint.consumer.config.Constants;
import no.fint.consumer.config.ConsumerProps;
import no.fint.consumer.event.ConsumerEventUtil;
import no.fint.consumer.exceptions.*;
import no.fint.consumer.utils.RestEndpoints;
import no.fint.event.model.Event;
import no.fint.event.model.HeaderConstants;
import no.fint.event.model.Status;
import no.fint.model.avatar.AvatarActions;
import no.fint.model.resource.avatar.AvatarResource;
import no.fint.model.resource.avatar.AvatarResources;
import no.fint.relations.FintRelationsMediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.naming.NameNotFoundException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@CrossOrigin
@RestController
@RequestMapping(value = RestEndpoints.PROFILBILDE, produces = {FintRelationsMediaType.APPLICATION_HAL_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE})
public class AvatarController {
    @Autowired
    private AvatarCacheService cacheService;

    @Autowired
    private FintAuditService fintAuditService;

    @Autowired
    private AvatarLinker linker;

    @Autowired
    private ConsumerProps props;

    @Autowired
    private ConsumerEventUtil consumerEventUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/last-updated")
    public Map<String, String> getLastUpdated(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        String lastUpdated = Long.toString(cacheService.getLastUpdated(orgId));
        return ImmutableMap.of("lastUpdated", lastUpdated);
    }

    @GetMapping("/cache/size")
    public ImmutableMap<String, Integer> getCacheSize(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        return ImmutableMap.of("size", cacheService.getAll(orgId).size());
    }

    @PostMapping("/cache/rebuild")
    public void rebuildCache(@RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId) {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        cacheService.rebuildCache(orgId);
    }

    @GetMapping
    public AvatarResources getAvatar(
            @RequestHeader(name = HeaderConstants.ORG_ID, required = false) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT, required = false) String client,
            @RequestParam(required = false) Long sinceTimeStamp) {
        if (props.isOverrideOrgId() || orgId == null) {
            orgId = props.getDefaultOrgId();
        }
        if (client == null) {
            client = props.getDefaultClient();
        }
        log.info("OrgId: {}, Client: {}", orgId, client);

        Event event = new Event(orgId, Constants.COMPONENT, AvatarActions.GET_ALL_AVATAR, client);
        fintAuditService.audit(event);

        fintAuditService.audit(event, Status.CACHE);

        List<AvatarResource> avatar;
        if (sinceTimeStamp == null) {
            avatar = cacheService.getAll(orgId);
        } else {
            avatar = cacheService.getAll(orgId, sinceTimeStamp);
        }

        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        return linker.toResources(avatar);
    }

    @GetMapping("/systemid/{id:.+}")
    public ResponseEntity<?> getAvatarBySystemId(
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

        Event event = new Event(orgId, Constants.COMPONENT, AvatarActions.GET_AVATAR, client);
        event.setQuery(id);
        fintAuditService.audit(event);

        fintAuditService.audit(event, Status.CACHE);

        Optional<AvatarResource> avatar = cacheService.getAvatarBySystemId(orgId, id);

        fintAuditService.audit(event, Status.CACHE_RESPONSE, Status.SENT_TO_CLIENT);

        if (avatar.isPresent()) {
            AvatarResource avatarResource = avatar.get();
            if ("json".equalsIgnoreCase(t)) {
                return ResponseEntity.ok(avatarResource);
            }
            if (!s.contains("x"))
                s = s + "x" + s;
            HttpHeaders headers = new HttpHeaders();
            if (!StringUtils.isEmpty(avatarResource.getAutorisasjon()))
                headers.set(HttpHeaders.AUTHORIZATION, avatarResource.getAutorisasjon());
            headers.set(HeaderConstants.ORG_ID, orgId);
            headers.set(HeaderConstants.CLIENT, client);
            return restTemplate.exchange("/{s}/filters:round_corner({r}):format({t})/{file}",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    byte[].class,
                    s, r, t,
                    avatarResource.getFilnavn());
        } else {
            throw new EntityNotFoundException(id);
        }
    }

    @PostMapping
    public ResponseEntity postAvatar(
            @RequestHeader(name = HeaderConstants.ORG_ID) String orgId,
            @RequestHeader(name = HeaderConstants.CLIENT) String client,
            @RequestBody AvatarResource body,
            @RequestParam(required = false) String s,
            @RequestParam(required = false, defaultValue = "jpeg") String t) {
        log.info("postAvatar, OrgId: {}, Client: {}, s: {}, t: {}", orgId, client, s, t);
        log.trace("Body: {}", body);
        linker.mapLinks(body);
        Optional<AvatarResource> result = cacheService.getAvatarByLink(orgId, body);

        if (result.isPresent()) {
            URI location = UriComponentsBuilder.fromUriString(linker.getSelfHref(result.get())).queryParam("s", s).queryParam("t", t).build().toUri();
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
