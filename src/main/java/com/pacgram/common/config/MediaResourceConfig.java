package com.pacgram.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {
    private final Path mediaDirectory;

    public MediaResourceConfig(@Value("${pacgram.storage.location}") String location) {
        this.mediaDirectory = Path.of(location).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/**")
                .addResourceLocations(mediaDirectory.toUri().toString());
    }
}
