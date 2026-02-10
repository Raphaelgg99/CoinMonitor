package com.potfoliomoedas.portfolio.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_usuario")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String senha;

    @OneToMany(
            mappedBy = "usuario", // Mapeado pelo campo "usuario" na classe Moeda
            cascade = CascadeType.ALL, // Se apagar o usuário, apaga as moedas
            orphanRemoval = true // Remove moedas órfãs
    )
    private List<Moeda> carteira = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tab_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_id")
    private List<String> roles = new ArrayList<>();

    @Lob // Avisa ao banco que é um arquivo grande (BLOB)
    @Column(length = 10000000) // Garante espaço (ex: até 10MB, mas vamos limitar antes)
    private byte[] fotoPerfil;

    @Column(nullable = false)
    private boolean verificado = false;

    private String codigoVerificacao;

    private LocalDateTime validadeCodigo;
}
