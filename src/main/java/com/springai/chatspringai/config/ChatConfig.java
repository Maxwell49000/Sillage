package com.springai.chatspringai.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration CORS (Cross-Origin Resource Sharing) pour l'application.
 *
 * Permet aux clients front-end (localhost:8000 et localhost:3000) d'accéder
 * aux endpoints de l'API depuis des origines différentes en incluant les
 * credentials (cookies, tokens, etc.).
 */
@Configuration
public class ChatConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public ChatConfig(@Value("${app.cors.allowed-origins:http://localhost:8000,http://localhost:3000}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toArray(String[]::new);
    }

    /**
     * Configure les mappings CORS globalement.
     *
     * - Autorise toutes les routes (/**)
     * - Accepte les origines localhost:8000 et localhost:3000
     * - Autorise GET, POST, PUT, DELETE, OPTIONS
     * - Inclut les credentials (authentification)
     * - Cache la préflight pour 3600 secondes (1 heure)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(allowedOrigins)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
    

