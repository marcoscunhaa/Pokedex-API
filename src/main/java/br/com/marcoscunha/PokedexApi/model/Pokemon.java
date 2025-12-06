package br.com.marcoscunha.PokedexApi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Setter
@Getter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pokemons")
public class Pokemon {

    @Id
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false)
    private int height;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false, length = 255)
    private String sprite;

    @Lob
    @Column(name = "sprite_base64", columnDefinition = "TEXT")
    private String spriteBase64;

    @ElementCollection
    @CollectionTable(name = "pokemon_types", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "type", length = 50)
    private List<String> type;

    @ElementCollection
    @CollectionTable(name = "pokemon_abilities", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "ability", length = 100)
    private List<String> ability;

    @ElementCollection
    @CollectionTable(name = "pokemon_moves", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "move", length = 100)
    private List<String> move;

    // Correção para Map<String, Integer> - define coluna da chave e coluna do valor
    @ElementCollection
    @CollectionTable(name = "pokemon_stats", joinColumns = @JoinColumn(name = "pokemon_id"))
    @MapKeyColumn(name = "stat_name", length = 100)
    @Column(name = "stat_value")
    private Map<String, Integer> stats;

    @ElementCollection
    @CollectionTable(name = "pokemon_evolutions", joinColumns = @JoinColumn(name = "pokemon_id"))
    @Column(name = "evolution", length = 100)
    private List<String> evolution;

    @Column(nullable = true, length = 50)
    private String generation;
}
