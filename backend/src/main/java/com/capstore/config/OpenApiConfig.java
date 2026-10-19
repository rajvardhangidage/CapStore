package com.capstore.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    OpenAPI capStoreOpenAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the access token returned by /api/v1/auth/login.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    OpenApiCustomizer publicEndpointsCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    boolean publicEndpoint = path.startsWith("/api/v1/auth/")
                            || path.equals("/api/v1/payments/webhook")
                            || (method == io.swagger.v3.oas.models.PathItem.HttpMethod.GET
                                && (path.startsWith("/api/v1/products")
                                    || path.startsWith("/api/v1/categories")
                                    || path.equals("/api/v1/banners")
                                    || path.equals("/api/v1/config")
                                    || path.startsWith("/uploads/")));
                    if (publicEndpoint) operation.setSecurity(List.of());
                }));
    }
}
