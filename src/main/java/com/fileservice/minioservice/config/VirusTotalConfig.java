package com.fileservice.minioservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class VirusTotalConfig {

    @Value("${virustotal.api-key}")
    private String apiKey;

    @Value("${virustotal.api-url:https://www.virustotal.com/api/v3}")
    private String apiUrl;

    @Value("${virustotal.enabled:true}")
    private boolean enabled;

    @Value("${virustotal.scan-timeout:60000}")
    private int scanTimeout;

    @Bean
    public RestTemplate virusTotalRestTemplate() {
        return new RestTemplate();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getScanTimeout() {
        return scanTimeout;
    }
}
