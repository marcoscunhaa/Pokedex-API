package br.com.marcoscunha.PokedexApi.config;

import br.com.marcoscunha.PokedexApi.service.PokemonService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupRunner {

    @Bean
    public CommandLineRunner run(PokemonService service) {
        return args -> {
            service.fetchAndSaveAllPokemons();
            service.convertAllSpritesToBase64();
        };
    }
}
