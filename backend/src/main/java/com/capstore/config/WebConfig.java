package com.capstore.config;

import com.capstore.service.LocalStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final ObjectProvider<LocalStorageService> localStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        LocalStorageService storage = localStorage.getIfAvailable();
        if (storage != null) {
            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(storage.root().toUri().toString())
                    .setCachePeriod(86400);
        }
    }
}
