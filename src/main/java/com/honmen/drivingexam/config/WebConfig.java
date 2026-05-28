package com.honmen.drivingexam.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/v1/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(false)
            .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dataRoot = resolveDataRoot();
        registry.addResourceHandler("/images/subject1/**")
            .addResourceLocations(dataRoot.resolve("subject1").resolve("images").toUri().toString());
        registry.addResourceHandler("/images/subject4/**")
            .addResourceLocations(dataRoot.resolve("subject4").resolve("images").toUri().toString());
        registry.addResourceHandler("/images/**")
            .addResourceLocations(
                dataRoot.resolve("subject1").resolve("images").toUri().toString(),
                dataRoot.resolve("subject4").resolve("images").toUri().toString()
            );
        registry.addResourceHandler("/videos/subject4/**")
            .addResourceLocations(dataRoot.resolve("subject4").resolve("videos").toUri().toString());
        registry.addResourceHandler("/videos/**")
            .addResourceLocations(dataRoot.resolve("subject4").resolve("videos").toUri().toString());
    }

    private Path resolveDataRoot() {
        Path current = Paths.get("").toAbsolutePath();
        for (Path candidate : new Path[] {current, current.getParent()}) {
            if (candidate != null
                && Files.exists(candidate.resolve("subject1").resolve("questions.json"))
                && Files.exists(candidate.resolve("subject4").resolve("questions.json"))) {
                return candidate;
            }
        }
        return current;
    }
}
