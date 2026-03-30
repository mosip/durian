package io.mosip.datashare.util;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import io.mosip.datashare.constant.ApiName;
import io.mosip.datashare.exception.ApiNotAccessibleException;

/**
 * @author Sowmya The Class RestUtil.
 */
@Component
public class RestUtil {

    @Autowired
    private Environment environment;

    @Autowired
    @Qualifier("selfTokenWebClient")
    private WebClient webClient;

    /**
     * Post api.
     */
    @SuppressWarnings("unchecked")
    public <T> T postApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
                         MediaType mediaType, Object requestType, Class<?> responseClass) throws ApiNotAccessibleException {
        String apiHostIpPort = environment.getProperty(apiName.name());
        if (apiHostIpPort == null) return null;

        String url = buildUrl(apiHostIpPort, pathsegments, queryParamName, queryParamValue);
        try {
            return (T) webClient.post()
                    .uri(url)
                    .contentType(mediaType != null ? mediaType : MediaType.APPLICATION_JSON)
                    .bodyValue(requestType)
                    .retrieve()
                    .bodyToMono(responseClass)
                    .block();
        } catch (Exception e) {
            throw new ApiNotAccessibleException(e);
        }
    }

    /**
     * Gets the api.
     */
    @SuppressWarnings("unchecked")
    public <T> T getApi(ApiName apiName, List<String> pathsegments, String queryParamName, String queryParamValue,
                        Class<?> responseType) throws ApiNotAccessibleException {
        String apiHostIpPort = environment.getProperty(apiName.name());
        if (apiHostIpPort == null) return null;

        String url = buildUrl(apiHostIpPort, pathsegments, queryParamName, queryParamValue);
        try {
            return (T) webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            throw new ApiNotAccessibleException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getApi(ApiName apiName, Map<String, String> pathsegments, Class<?> responseType) throws Exception {
        String apiHostIpPort = environment.getProperty(apiName.name());
        if (apiHostIpPort == null) return null;

        try {
            URI urlWithPath = UriComponentsBuilder.fromUriString(apiHostIpPort).build(pathsegments);
            return (T) webClient.get()
                    .uri(urlWithPath)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    private String buildUrl(String baseUrl, List<String> pathsegments, String queryParamName, String queryParamValue) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);
        if (pathsegments != null && !pathsegments.isEmpty()) {
            for (String segment : pathsegments) {
                if (segment != null && !segment.isEmpty()) {
                    builder.pathSegment(segment);
                }
            }
        }
        if (queryParamName != null && !queryParamName.isEmpty()) {
            String[] names = queryParamName.split(",");
            String[] values = queryParamValue.split(",");
            for (int i = 0; i < names.length; i++) {
                builder.queryParam(names[i], values[i]);
            }
        }
        return builder.toUriString();
    }
}
