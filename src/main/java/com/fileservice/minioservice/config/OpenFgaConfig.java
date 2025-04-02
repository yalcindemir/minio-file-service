package com.fileservice.minioservice.config;

import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.configuration.ClientConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFgaConfig {

    @Value("${openfga.api-url}")
    private String apiUrl;

    @Value("${openfga.store-id}")
    private String storeId;

    @Value("${openfga.authorization-model-id}")
    private String authorizationModelId;

    @Bean
    public OpenFgaClient openFgaClient() {
        ClientConfiguration configuration = new ClientConfiguration()
                .apiUrl(apiUrl)
                .storeId(storeId)
                .authorizationModelId(authorizationModelId);

        return new OpenFgaClient(configuration);
    }
}
