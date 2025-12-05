package br.com.marcoscunha.PokedexApi.service;

import br.com.marcoscunha.PokedexApi.model.Pokemon;
import br.com.marcoscunha.PokedexApi.repository.PokemonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PokemonService {

    @Value("${pokeapi.base-url:https://pokeapi.co/api/v2/}")
    private String baseApiUrl;

    private static final String UNKNOWN = "unknown";
    private static final String DESCRIPTION_NOT_FOUND = "Descrição não encontrada.";
    private static final int MAX_ID = 1026;

    @Autowired
    private PokemonRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    // ===============================
    //        FETCH E SALVAMENTO
    // ===============================
    public Pokemon fetchAndSavePokemon(String pokemonIdentifier) {
        return fetchAndSavePokemon(pokemonIdentifier, new HashSet<>(), null);
    }

    private Pokemon fetchAndSavePokemon(String pokemonIdentifier, Set<String> processing, Pokemon normalPokemon) {
        pokemonIdentifier = pokemonIdentifier.toLowerCase();

        if (processing.contains(pokemonIdentifier)) {
            return repository.findByName(pokemonIdentifier).orElse(null);
        }

        processing.add(pokemonIdentifier);

        System.out.println("➡ Buscando Pokémon: " + pokemonIdentifier);

        String url = baseApiUrl + "pokemon/" + pokemonIdentifier;
        Map<String, Object> response = fetchFromApi(url);

        if (response == null) {
            System.err.println("❌ Falha ao buscar: " + pokemonIdentifier);
            return null;
        }

        String name = (String) response.get("name");

        Optional<Pokemon> existing = repository.findByName(name);
        if (existing.isPresent()) {
            System.out.println("✔ Já existe no banco: " + name);
            return existing.get();
        }

        Pokemon pokemon = new Pokemon();
        pokemon.setName(name);
        pokemon.setHeight((int) response.get("height"));
        pokemon.setWeight((int) response.get("weight"));

        pokemon.setSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + name + ".png");

        pokemon.setType(parseNameList((List<Map<String, Object>>) response.get("types"), "type"));
        pokemon.setAbility(parseNameList((List<Map<String, Object>>) response.get("abilities"), "ability"));
        pokemon.setMove(parseNameList((List<Map<String, Object>>) response.get("moves"), "move"));
        pokemon.setStats(parseStats((List<Map<String, Object>>) response.get("stats")));

        // ============================
        //      SPECIES INFO
        // ============================
        Map<String, Object> speciesInfo = (Map<String, Object>) response.get("species");
        String speciesUrl = speciesInfo != null ? (String) speciesInfo.get("url") : null;

        if (speciesUrl != null) {
            Map<String, Object> speciesData = fetchFromApi(speciesUrl);

            if (speciesData != null) {
                Map<String, Object> generationInfo = (Map<String, Object>) speciesData.get("generation");
                if (generationInfo != null) {
                    pokemon.setGeneration((String) generationInfo.get("name"));
                }

                pokemon.setEvolution(parseEvolution(speciesData, pokemonIdentifier));

                // Descrição: se nula, usa do normal
                String description = parseFlavorText(speciesData);
                if ((description == null || description.isEmpty() || description.equals(DESCRIPTION_NOT_FOUND)) && normalPokemon != null) {
                    description = normalPokemon.getDescription();
                }
                pokemon.setDescription(description != null ? description : DESCRIPTION_NOT_FOUND);

            } else {
                handleSpeciesFallback(pokemon, normalPokemon);
            }
        } else {
            handleSpeciesFallback(pokemon, normalPokemon);
        }

        System.out.println("💾 Salvando no banco: " + name);
        Pokemon saved = repository.save(pokemon);

        return saved;
    }

    private void handleSpeciesFallback(Pokemon pokemon, Pokemon normalPokemon) {
        pokemon.setEvolution(List.of(UNKNOWN));
        if (normalPokemon != null && normalPokemon.getDescription() != null) {
            pokemon.setDescription(normalPokemon.getDescription());
        } else {
            pokemon.setDescription(DESCRIPTION_NOT_FOUND);
        }
    }

    public void fetchAndSaveAllPokemons() {
        String url = baseApiUrl + "pokemon?limit=100000&offset=0";
        Map<String, Object> response = fetchFromApi(url);
        if (response == null || !response.containsKey("results")) {
            System.err.println("❌ Resposta inválida da API.");
            return;
        }

        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");

        for (Map<String, String> pokeData : results) {
            String name = pokeData.get("name");

            // Busca o Pokémon normal
            Pokemon normal = fetchAndSavePokemon(name);
            if (normal == null) continue;

            // Verifica se o ID ultrapassa o limite
            if (normal.getId() != null && normal.getId() > MAX_ID) {
                System.out.println("⚠ ID máximo atingido (" + normal.getId() + "). Parando a busca.");
                break;
            }

            // =========================
            // Salva formas alternativas
            // =========================
            try {
                Map<String, Object> speciesData = fetchFromApi(baseApiUrl + "pokemon-species/" + normal.getName());
                if (speciesData == null) continue;

                List<Map<String, Object>> varieties = (List<Map<String, Object>>) speciesData.get("varieties");
                if (varieties == null) continue;

                for (Map<String, Object> variety : varieties) {
                    Map<String, Object> pokeInfo = (Map<String, Object>) variety.get("pokemon");
                    if (pokeInfo == null) continue;

                    String varietyName = ((String) pokeInfo.get("name")).toLowerCase();
                    boolean isDefault = (boolean) variety.getOrDefault("is_default", false);

                    if (!isDefault && repository.findByName(varietyName).isEmpty()) {
                        System.out.println("🔁 Salvando forma alternativa: " + varietyName);
                        fetchAndSavePokemon(varietyName, new HashSet<>(), normal); // herda descrição
                    }
                }
            } catch (Exception e) {
                System.err.println("Erro ao processar alternativas de " + normal.getName() + ": " + e.getMessage());
            }
        }

        System.out.println("✅ Todos os Pokémon e formas alternativas até o ID " + MAX_ID + " foram processados.");
    }

    // ===============================
    // MÉTODOS DE BUSCA, ADVANCED SEARCH, SPRITES
    // ===============================
    public List<Pokemon> getAllPokemons() {
        return repository.findAllByOrderByIdAsc();
    }

    public Page<Pokemon> getAllPaginated(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Pokemon findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public List<Pokemon> findByName(String name) {
        return repository.findByNameContainingIgnoreCaseOrderByIdAsc(name);
    }

    public List<Pokemon> findByType(String type) {
        return repository.findByTypeContainingIgnoreCaseOrderByIdAsc(type);
    }

    public List<Pokemon> findByAbility(String ability) {
        return repository.findByAbilityContainingIgnoreCaseOrderByIdAsc(ability);
    }

    public List<Pokemon> findByMove(String move) {
        return repository.findByMoveContainingIgnoreCaseOrderByIdAsc(move);
    }

    public List<Pokemon> advancedSearch(String name, List<String> types, String ability, String move, String generation) {
        List<Pokemon> result = getAllPokemons();

        if (name != null && !name.isEmpty()) {
            result = result.stream()
                    .filter(p -> p.getName().toLowerCase().contains(name.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (types != null && !types.isEmpty()) {
            result = result.stream()
                    .filter(p -> types.stream().allMatch(t ->
                            p.getType().stream().anyMatch(pt -> pt.equalsIgnoreCase(t))
                    ))
                    .collect(Collectors.toList());
        }

        if (ability != null && !ability.isEmpty()) {
            result = result.stream()
                    .filter(p -> p.getAbility().stream().anyMatch(a -> a.equalsIgnoreCase(ability)))
                    .collect(Collectors.toList());
        }

        if (move != null && !move.isEmpty()) {
            result = result.stream()
                    .filter(p -> p.getMove().stream().anyMatch(m -> m.equalsIgnoreCase(move)))
                    .collect(Collectors.toList());
        }

        if (generation != null && !generation.isEmpty()) {
            result = result.stream()
                    .filter(p -> p.getGeneration() != null && p.getGeneration().equalsIgnoreCase(generation))
                    .collect(Collectors.toList());
        }

        return result;
    }

    public void convertAllSpritesToBase64() {
        List<Pokemon> pokemons = repository.findAllByOrderByIdAsc();

        for (Pokemon pokemon : pokemons) {
            if (pokemon.getId() != null && pokemon.getId() > MAX_ID) {
                continue; // ignora Pokémon acima do limite
            }

            try {
                if (pokemon.getSpriteBase64() == null || pokemon.getSpriteBase64().isEmpty()) {
                    String base64 = downloadImageAsBase64(pokemon.getSprite());
                    pokemon.setSpriteBase64(base64);
                    System.out.println("Convertido: " + pokemon.getName());
                    Thread.sleep(1000);
                } else {
                    System.out.println("Já convertido: " + pokemon.getName());
                }
            } catch (Exception e) {
                System.err.println("Erro ao converter sprite do Pokémon '" + pokemon.getName() + "': " + e.getMessage());
            }
        }

        repository.saveAll(pokemons);
    }

    private String downloadImageAsBase64(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            byte[] imageBytes = in.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            System.err.println("Erro ao baixar/converter imagem: " + e.getMessage());
            return "";
        }
    }

    // ===============================
    //        MÉTODOS PRIVADOS
    // ===============================
    private Map<String, Object> fetchFromApi(String url) {
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            System.err.printf("Erro ao buscar dados da URL '%s': %s%n", url, e.getMessage());
            return null;
        }
    }

    private List<String> parseNameList(List<Map<String, Object>> dataList, String key) {
        List<String> names = new ArrayList<>();
        Optional.ofNullable(dataList).orElse(List.of()).forEach(item -> {
            Map<String, String> inner = (Map<String, String>) item.get(key);
            if (inner != null && inner.get("name") != null) {
                names.add(inner.get("name"));
            }
        });
        return names;
    }

    private Map<String, Integer> parseStats(List<Map<String, Object>> statsList) {
        Map<String, Integer> statMap = new HashMap<>();
        Optional.ofNullable(statsList).orElse(List.of()).forEach(statInfo -> {
            Map<String, String> stat = (Map<String, String>) statInfo.get("stat");
            Object baseStatObj = statInfo.get("base_stat");
            if (stat != null && stat.get("name") != null && baseStatObj instanceof Integer baseStat) {
                statMap.put(stat.get("name"), baseStat);
            }
        });
        return statMap;
    }

    private List<String> parseEvolution(Map<String, Object> speciesData, String pokemonName) {
        Map<String, String> evolutionChain = (Map<String, String>) speciesData.get("evolution_chain");
        String evolutionUrl = Optional.ofNullable(evolutionChain).map(e -> e.get("url")).orElse(null);

        if (evolutionUrl == null) return List.of(UNKNOWN);

        Map<String, Object> evolutionData = fetchFromApi(evolutionUrl);
        if (evolutionData == null || evolutionData.get("chain") == null) return List.of(UNKNOWN);

        List<String> evolutionList = new ArrayList<>();
        extractEvolutionChain((Map<String, Object>) evolutionData.get("chain"), evolutionList);
        return evolutionList.isEmpty() ? List.of(UNKNOWN) : evolutionList;
    }

    private void extractEvolutionChain(Map<String, Object> node, List<String> evolutionList) {
        Map<String, String> species = (Map<String, String>) node.get("species");
        if (species != null && species.get("name") != null) {
            evolutionList.add(species.get("name"));
        }
        Optional.ofNullable((List<Map<String, Object>>) node.get("evolves_to")).orElse(List.of())
                .forEach(next -> extractEvolutionChain(next, evolutionList));
    }

    private String parseFlavorText(Map<String, Object> speciesData) {
        List<Map<String, Object>> flavorTextEntries = (List<Map<String, Object>>) speciesData.get("flavor_text_entries");
        if (flavorTextEntries != null) {
            for (Map<String, Object> entry : flavorTextEntries) {
                Map<String, String> language = (Map<String, String>) entry.get("language");
                if ("en".equals(Optional.ofNullable(language).map(lang -> lang.get("name")).orElse(""))) {
                    String rawText = (String) entry.get("flavor_text");
                    if (rawText != null && !rawText.isEmpty()) {
                        return rawText.replaceAll("[\\n\\f]", " ").trim();
                    }
                }
            }
        }
        return DESCRIPTION_NOT_FOUND;
    }
}
