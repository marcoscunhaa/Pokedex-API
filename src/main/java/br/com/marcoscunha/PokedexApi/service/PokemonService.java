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

    @Autowired
    private PokemonRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    // ===================== PROGRESS BAR =====================
    private void printProgressBar(int current, int total, String name, String status) {
        int barSize = 40;
        double progress = (double) current / total;

        int filled = (int) (progress * barSize);
        int empty = barSize - filled;

        String bar = "[" + "#".repeat(filled) + "-".repeat(empty) + "]";
        int percent = (int) (progress * 100);

        System.out.printf("\r%s %3d%% (%4d/%4d) - %-15s [%s]",
                bar, percent, current, total, name, status);

        System.out.flush();

        if (current == total) {
            System.out.println();
        }
    }

    // ===================== FETCH + SAVE =====================
    public Pokemon fetchAndSavePokemon(String pokemonIdentifier) {
        String url = baseApiUrl + "pokemon/" + pokemonIdentifier.toLowerCase();
        Map<String, Object> response = fetchFromApi(url);

        if (response == null) return null;

        Integer id = (Integer) response.get("id");
        String name = Optional.ofNullable((String) response.get("name"))
                .map(String::toLowerCase).orElse(null);

        if (id == null || name == null) return null;

        // ---------- 🔥 ANTI-DUPLICIDADE ABSOLUTA ----------
        Optional<Pokemon> existingById = repository.findById(Long.valueOf(id));
        if (existingById.isPresent()) return existingById.get();

        Optional<Pokemon> existingByName = repository.findByName(name);
        if (existingByName.isPresent()) return existingByName.get();
        // -----------------------------------------------------

        Pokemon pokemon = new Pokemon();
        pokemon.setId(Long.valueOf(id));
        pokemon.setName(name);
        pokemon.setHeight((int) response.get("height"));
        pokemon.setWeight((int) response.get("weight"));

        pokemon.setSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + id + ".png");

        pokemon.setType(parseNameList((List<Map<String, Object>>) response.get("types"), "type"));
        pokemon.setAbility(parseNameList((List<Map<String, Object>>) response.get("abilities"), "ability"));
        pokemon.setMove(parseNameList((List<Map<String, Object>>) response.get("moves"), "move"));
        pokemon.setStats(parseStats((List<Map<String, Object>>) response.get("stats")));

        // Species info
        Map<String, Object> speciesInfo = (Map<String, Object>) response.get("species");
        String speciesUrl = speciesInfo != null ? (String) speciesInfo.get("url") : null;

        if (speciesUrl != null) {
            Map<String, Object> speciesData = fetchFromApi(speciesUrl);

            if (speciesData != null) {
                Map<String, Object> generationInfo = (Map<String, Object>) speciesData.get("generation");
                if (generationInfo != null) pokemon.setGeneration((String) generationInfo.get("name"));

                pokemon.setEvolution(parseEvolution(speciesData));
                pokemon.setDescription(parseFlavorText(speciesData));
            } else {
                handleSpeciesFallback(pokemon);
            }

        } else {
            handleSpeciesFallback(pokemon);
        }

        return repository.save(pokemon);
    }

    public void fetchAndSaveAllPokemons() {
        int maxPokemon = 1025;
        String url = baseApiUrl + "pokemon?limit=" + maxPokemon + "&offset=0";
        Map<String, Object> response = fetchFromApi(url);

        if (response == null || !response.containsKey("results")) return;

        List<Map<String, String>> results = (List<Map<String, String>>) response.get("results");
        int total = results.size();
        int count = 0;

        System.out.println("\n=== INÍCIO DA IMPORTAÇÃO DE POKÉMONS ===\n");

        for (Map<String, String> pokemonData : results) {
            count++;
            String name = pokemonData.get("name");
            String status = "SALVANDO";

            try {
                fetchAndSavePokemon(name);
                status = "OK";
            } catch (Exception e) {
                status = "ERRO";
            }

            printProgressBar(count, total, name, status);

            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }

        System.out.println("\n=== IMPORTAÇÃO CONCLUÍDA ===\n");
    }

    // ===================== SPRITE BASE64 =====================
    public void convertAllSpritesToBase64() {
        List<Pokemon> pokemons = repository.findAll();
        int total = pokemons.size();
        int index = 0;

        System.out.println("\n=== CONVERSÃO DE SPRITES PARA BASE64 INICIADA ===\n");

        for (Pokemon pokemon : pokemons) {
            index++;
            String status = "OK";

            try {
                if (pokemon.getSpriteBase64() == null || pokemon.getSpriteBase64().isEmpty()) {
                    String base64 = downloadImageAsBase64(pokemon.getSprite());
                    pokemon.setSpriteBase64(base64);
                }
            } catch (Exception e) {
                status = "ERRO";
            }

            printProgressBar(index, total, pokemon.getName(), status);
        }

        repository.saveAll(pokemons);
        System.out.println("\n=== CONVERSÃO FINALIZADA ===\n");
    }

    private String downloadImageAsBase64(String imageUrl) {
        try (InputStream in = new URL(imageUrl).openStream()) {
            byte[] imageBytes = in.readAllBytes();
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            return "";
        }
    }

    // ===================== AUXILIARES =====================
    private Map<String, Object> fetchFromApi(String url) {
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (RestClientException e) {
            return null;
        }
    }

    private List<String> parseNameList(List<Map<String, Object>> dataList, String key) {
        List<String> names = new ArrayList<>();
        Optional.ofNullable(dataList).orElse(List.of()).forEach(item -> {
            Map<String, String> inner = (Map<String, String>) item.get(key);
            if (inner != null && inner.get("name") != null)
                names.add(inner.get("name").toLowerCase());
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

    private void handleSpeciesFallback(Pokemon pokemon) {
        pokemon.setEvolution(List.of(UNKNOWN));
        pokemon.setDescription(DESCRIPTION_NOT_FOUND);
    }

    private List<String> parseEvolution(Map<String, Object> speciesData) {
        Map<String, Object> evolutionChain = (Map<String, Object>) speciesData.get("evolution_chain");
        String url = evolutionChain != null ? (String) evolutionChain.get("url") : null;
        if (url == null) return List.of(UNKNOWN);

        Map<String, Object> evolutionData = fetchFromApi(url);
        if (evolutionData == null || evolutionData.get("chain") == null) return List.of(UNKNOWN);

        List<String> evolutionList = new ArrayList<>();
        extractEvolutionChain((Map<String, Object>) evolutionData.get("chain"), evolutionList);
        return evolutionList.isEmpty() ? List.of(UNKNOWN) : evolutionList;
    }

    private void extractEvolutionChain(Map<String, Object> node, List<String> evolutionList) {
        Map<String, String> species = (Map<String, String>) node.get("species");
        if (species != null && species.get("name") != null)
            evolutionList.add(species.get("name").toLowerCase());

        Optional.ofNullable((List<Map<String, Object>>) node.get("evolves_to"))
                .orElse(List.of())
                .forEach(next -> extractEvolutionChain(next, evolutionList));
    }

    private String parseFlavorText(Map<String, Object> speciesData) {
        List<Map<String, Object>> entries = (List<Map<String, Object>>) speciesData.get("flavor_text_entries");
        if (entries != null) {
            for (Map<String, Object> entry : entries) {
                Map<String, String> lang = (Map<String, String>) entry.get("language");
                if ("en".equalsIgnoreCase(Optional.ofNullable(lang).map(l -> l.get("name")).orElse(""))) {
                    String text = (String) entry.get("flavor_text");
                    if (text != null && !text.isEmpty()) {
                        return text.replaceAll("[\\n\\f]", " ").trim();
                    }
                }
            }
        }
        return DESCRIPTION_NOT_FOUND;
    }

    // ===================== BUSCAS =====================
    public List<Pokemon> getAllPokemons() {
        return repository.findAllByOrderByIdAsc();
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

        if (name != null && !name.isEmpty())
            result = result.stream().filter(p -> p.getName().contains(name.toLowerCase())).collect(Collectors.toList());

        if (types != null && !types.isEmpty())
            result = result.stream().filter(p -> types.stream().allMatch(t -> p.getType().contains(t.toLowerCase()))).collect(Collectors.toList());

        if (ability != null && !ability.isEmpty())
            result = result.stream().filter(p -> p.getAbility().contains(ability.toLowerCase())).collect(Collectors.toList());

        if (move != null && !move.isEmpty())
            result = result.stream().filter(p -> p.getMove().contains(move.toLowerCase())).collect(Collectors.toList());

        if (generation != null && !generation.isEmpty())
            result = result.stream().filter(p -> p.getGeneration() != null &&
                    p.getGeneration().toLowerCase().contains(generation.toLowerCase())).collect(Collectors.toList());

        return result;
    }

    // ===================== IMPORTAR VARIAÇÕES DE FORMA AUTOMÁTICA =====================
    public void importAllVariations() {

        int nextId = 1026; // começa o ID das variações
        int limit = 2000; // pega todas as forms possíveis
        String url = baseApiUrl + "pokemon-form?limit=" + limit;

        Map<String, Object> response = fetchFromApi(url);
        if (response == null) {
            System.out.println("ERRO ao acessar /pokemon-form");
            return;
        }

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

        // FILTRO: somente formas relevantes
        List<String> allowedPatterns = List.of(
                "mega", "primal", "origin", "therian", "sky", "unbound",
                "alola", "galar", "hisui", "paldea"
        );

        List<Map<String, Object>> filteredForms = results.stream()
                .filter(entry -> {
                    String name = ((String) entry.get("name")).toLowerCase();
                    return allowedPatterns.stream().anyMatch(name::contains);
                })
                .toList();

        int total = filteredForms.size();
        int count = 0;

        System.out.println("\n=== IMPORTANDO VARIAÇÕES OFICIAIS DA POKEAPI ===\n");

        for (Map<String, Object> formEntry : filteredForms) {
            count++;

            String formName = ((String) formEntry.get("name")).toLowerCase();
            String formUrl = (String) formEntry.get("url");

            String status = "OK";

            try {
                // Evita duplicidade
                if (repository.findByName(formName).isPresent()) {
                    printProgressBar(count, total, formName, "EXISTE");
                    continue;
                }

                // 1) baixa a estrutura da forma
                Map<String, Object> formData = fetchFromApi(formUrl);
                if (formData == null) {
                    printProgressBar(count, total, formName, "ERRO");
                    continue;
                }

                // 2) forma aponta para o Pokémon completo
                Map<String, Object> pokemonObj = (Map<String, Object>) formData.get("pokemon");
                if (pokemonObj == null || pokemonObj.get("url") == null) {
                    printProgressBar(count, total, formName, "ERRO");
                    continue;
                }

                // 3) baixa os dados reais do Pokémon
                Map<String, Object> fullData = fetchFromApi((String) pokemonObj.get("url"));
                if (fullData == null) {
                    printProgressBar(count, total, formName, "ERRO");
                    continue;
                }

                // === Criar variação ===
                Pokemon p = new Pokemon();
                p.setId((long) nextId++);
                p.setName(formName);

                p.setHeight((int) fullData.get("height"));
                p.setWeight((int) fullData.get("weight"));

                int spriteId = (int) fullData.get("id");
                p.setSprite("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/" + spriteId + ".png");

                p.setType(parseNameList((List<Map<String, Object>>) fullData.get("types"), "type"));
                p.setAbility(parseNameList((List<Map<String, Object>>) fullData.get("abilities"), "ability"));
                p.setMove(parseNameList((List<Map<String, Object>>) fullData.get("moves"), "move"));
                p.setStats(parseStats((List<Map<String, Object>>) fullData.get("stats")));

                // generation = herdada da espécie
                Map<String, Object> species = fetchFromApi((String) ((Map<String, Object>) fullData.get("species")).get("url"));
                if (species != null) {
                    Map<String, Object> gen = (Map<String, Object>) species.get("generation");
                    p.setGeneration(gen != null ? (String) gen.get("name") : "unknown");
                } else {
                    p.setGeneration("unknown");
                }

                p.setEvolution(List.of(formName));
                p.setDescription("Official alternate form: " + formName);

                repository.save(p);

            } catch (Exception e) {
                status = "ERRO";
            }

            printProgressBar(count, total, formName, status);

            try { Thread.sleep(120); } catch (InterruptedException ignored) {}
        }

        System.out.println("\n=== IMPORTAÇÃO COMPLETA: TODAS AS FORMAS ADICIONADAS ===\n");
    }



}
