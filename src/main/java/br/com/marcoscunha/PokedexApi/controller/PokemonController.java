package br.com.marcoscunha.PokedexApi.controller;

import br.com.marcoscunha.PokedexApi.model.Pokemon;
import br.com.marcoscunha.PokedexApi.service.PokemonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/pokemons")
public class PokemonController {

    @Autowired
    private PokemonService service;

    @GetMapping("/search/advanced")
    public ResponseEntity<List<Pokemon>> searchAdvanced(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) String ability,
            @RequestParam(required = false) String move,
            @RequestParam(required = false) String generation
    ) {
        List<Pokemon> pokemons = service.advancedSearch(name, types, ability, move, generation);
        return ResponseEntity.ok(pokemons);
    }


    @GetMapping("")
    public ResponseEntity<List<Pokemon>> getAll() {
        List<Pokemon> pokemons = service.getAllPokemons();
        return ResponseEntity.ok(pokemons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pokemon> getById(@PathVariable Long id) {
        Optional<Pokemon> result = Optional.ofNullable(service.findById(id));
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/name")
    public ResponseEntity<List<Pokemon>> getByName(@RequestParam String name) {
        List<Pokemon> result = service.findByName(name);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/type")
    public ResponseEntity<List<Pokemon>> getByType(@RequestParam String type) {
        List<Pokemon> result = service.findByType(type);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/ability")
    public ResponseEntity<List<Pokemon>> getByAbility(@RequestParam String ability) {
        List<Pokemon> result = service.findByAbility(ability);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/move")
    public ResponseEntity<List<Pokemon>> getByMove(@RequestParam String move) {
        List<Pokemon> result = service.findByMove(move);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/import")
    public ResponseEntity<String> importAll() {
        service.fetchAndSaveAllPokemons();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Todos os Pokémons foram importados com sucesso!!");
    }

    @PostMapping("/convert-sprites")
    public ResponseEntity<String> convertSpritesToBase64() {
        service.convertAllSpritesToBase64();
        return ResponseEntity.ok("Sprites convertidas para base64 com sucesso.");
    }

}
