package no.fint.consumer.api;

import lombok.Data;

import java.util.List;

@Data
public class ApiService {
    private String collectionUrl;
    private List<String> oneUrl;
    private String cacheSizeUrl;
    private String lastUpdatedUrl;
}
