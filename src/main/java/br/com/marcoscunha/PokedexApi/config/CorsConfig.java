package br.com.marcoscunha.PokedexApi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:4200",
                                "https://pokedex-sigma-plum.vercel.app",
                                "https://pokedex-git-main-marcos-projects-c01daa0e.vercel.app",
                                "https://pokedex-5bxg2jx36-marcos-projects-c01daa0e.vercel.app",
                                "https://pokedex-api.duckdns.org" // seu novo domínio
                        )
                        .allowedMethods("*")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }
        };
    }
}
