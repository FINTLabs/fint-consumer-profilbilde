package no.fint.consumer.api;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApiServiceMapper {

    private static final String CACHE_SIZE_PATTERN = "/cache/size";
    private static final String LAST_UPDATE_PATTERN = "/last-updated";
    private static final String ONE_PATTERN = "/.*/\\{id\\}";
    private static final String COLLECTION_PATTERN = "";

    @Value("${fint.relations.default-base-url}")
    private String defaultBaseUrl;

    @Value("${server.context-path}")
    private String serverContextPath;

    public ApiService getApiService(List<RequestMappingInfo> requestMappingInfos) {

        ApiService apiService = new ApiService();


        apiService.setCacheSizeUrl(getUrl(getCacheSizeMapping(requestMappingInfos)));
        apiService.setLastUpdatedUrl(getUrl(getLastUpdatedMapping(requestMappingInfos)));
        apiService.setOneUrl(getUrls(getOneMapping(requestMappingInfos)));
        apiService.setCollectionUrl(getUrl(getCollectionMapping(requestMappingInfos)));

        return apiService;
    }

    private String getCacheSizeMapping(List<RequestMappingInfo> requestMappingInfos) {
        return getMapping(requestMappingInfos, CACHE_SIZE_PATTERN).get(0);


    }

    private String getLastUpdatedMapping(List<RequestMappingInfo> requestMappingInfos) {
        return getMapping(requestMappingInfos, LAST_UPDATE_PATTERN).get(0);
    }

    private List<String> getOneMapping(List<RequestMappingInfo> requestMappingInfos) {
        return getMapping(requestMappingInfos, ONE_PATTERN);
    }

    private String getCollectionMapping(List<RequestMappingInfo> requestMappingInfos) {
        return getMapping(requestMappingInfos, COLLECTION_PATTERN).get(0);
    }

    private List<String> getMapping(List<RequestMappingInfo> requestMappingInfos, String pattern) {
        List<String> allMappings = new ArrayList<>();
        requestMappingInfos.forEach(requestMappingInfo -> {
            List<String> mappings = requestMappingInfo.getPatternsCondition().getPatterns().stream().filter(s ->
                    s.matches(String.format("/%s%s", requestMappingInfo.getName().toLowerCase(), pattern))).collect(Collectors.toList());
            allMappings.addAll(mappings);
        });

        if (allMappings.size() > 0) {
            return allMappings;
        }
        return Lists.newArrayList("");
    }


    private String getUrl(String serviceUrl) {
        return String.format("%s%s%s", defaultBaseUrl, serverContextPath, serviceUrl);
    }

    private List<String> getUrls(List<String> serviceUrls) {
        List<String> urls = new ArrayList<>();
        serviceUrls.forEach(url -> urls.add(getUrl(url)));

        return urls;
    }
}
