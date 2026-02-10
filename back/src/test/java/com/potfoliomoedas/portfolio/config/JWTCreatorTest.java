package com.potfoliomoedas.portfolio.config;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class) // Habilita o uso de @Mock e @InjectMocks
class JWTCreatorTest {

    // 1. Mockamos a classe de configuração (ela não será real, nós ditaremos os valores)
    @Mock
    private SecurityConfig securityConfig;

    // 2. Injetamos o mock dentro do JWTCreator
    @InjectMocks
    private JWTCreator jwtCreator;

    // Constantes para usar nos testes
    private final String CHAVE_TESTE = "uma_chave_super_secreta_e_longa_para_o_algoritmo_hs512_funcionar_bem";
    private final String PREFIXO_TESTE = "Bearer";

    @Test
    @DisplayName("Deve gerar um token válido e conseguir fazer o parse de volta (Round-trip)")
    void deveGerarEAnalisarTokenComSucesso() throws java.security.SignatureException {
        // --- ARRANGE ---
        // Ensinamos o Mock a devolver valores fake quando perguntado
        when(securityConfig.getKEY()).thenReturn(CHAVE_TESTE);
        when(securityConfig.getPREFIX()).thenReturn(PREFIXO_TESTE);

        JWTObject jwtObjectEntrada = new JWTObject();
        jwtObjectEntrada.setSubject("usuario@teste.com");
        jwtObjectEntrada.setIssuedAt(new Date());
        jwtObjectEntrada.setExpiration(new Date(System.currentTimeMillis() + 3600000)); // +1 hora
        // Passamos roles sem o prefixo para testar se ele adiciona sozinho
        jwtObjectEntrada.setRoles(Arrays.asList("ADMIN", "USER"));

        // --- ACT (Gerar) ---
        String tokenGerado = jwtCreator.gerarToken(jwtObjectEntrada);

        // --- ASSERT (Gerar) ---
        Assertions.assertNotNull(tokenGerado);
        Assertions.assertTrue(tokenGerado.startsWith(PREFIXO_TESTE + " "), "Token deve começar com o prefixo 'Bearer '");

        // --- ACT (Parse - Caminho de volta) ---
        // Agora passamos o token gerado para o método parseToken para ver se ele lê corretamente
        JWTObject jwtObjectSaida = jwtCreator.parseToken(tokenGerado);

        // --- ASSERT (Parse) ---
        Assertions.assertEquals("usuario@teste.com", jwtObjectSaida.getSubject());

        // Verifica se as roles receberam o prefixo "ROLE_" automaticamente
        List<String> roles = jwtObjectSaida.getRoles();
        Assertions.assertTrue(roles.contains("ROLE_ADMIN"));
        Assertions.assertTrue(roles.contains("ROLE_USER"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar ler um token com assinatura inválida")
    void deveFalharComAssinaturaInvalida() {
        // --- ARRANGE ---
        when(securityConfig.getKEY()).thenReturn(CHAVE_TESTE);
        when(securityConfig.getPREFIX()).thenReturn(PREFIXO_TESTE);

        // 1. Geramos um token válido primeiro
        JWTObject obj = new JWTObject();
        obj.setSubject("hacker");
        obj.setRoles(List.of("USER"));
        obj.setIssuedAt(new Date());
        obj.setExpiration(new Date(System.currentTimeMillis() + 10000));

        String tokenOriginal = jwtCreator.gerarToken(obj);

        // 2. Simulamos um ataque: Alteramos o Mock para usar OUTRA chave na hora de ler
        // Isso simula o cenário onde o token foi assinado com uma chave diferente da esperada
        when(securityConfig.getKEY()).thenReturn("outra_chave_qualquer_que_nao_bate");

        // --- ACT & ASSERT ---
        Assertions.assertThrows(SignatureException.class, () -> {
            jwtCreator.parseToken(tokenOriginal);
        });
    }

    @Test
    @DisplayName("Deve lançar exceção se o token estiver malformado (lixo)")
    void deveFalharComTokenMalformado() {
        when(securityConfig.getKEY()).thenReturn(CHAVE_TESTE);
        when(securityConfig.getPREFIX()).thenReturn(PREFIXO_TESTE);

        // --- ACT & ASSERT ---
        // Passamos uma String aleatória
        Assertions.assertThrows(MalformedJwtException.class, () -> {
            jwtCreator.parseToken("Bearer token.aleatorio.quebrado");
        });
    }

    @Test
    @DisplayName("Lógica de Roles: Não deve adicionar ROLE_ se já existir")
    void naoDeveDuplicarPrefixoRole() throws java.security.SignatureException {
        // --- ARRANGE ---
        when(securityConfig.getKEY()).thenReturn(CHAVE_TESTE);
        when(securityConfig.getPREFIX()).thenReturn(PREFIXO_TESTE);

        JWTObject obj = new JWTObject();
        obj.setSubject("teste");
        obj.setIssuedAt(new Date());
        obj.setExpiration(new Date(System.currentTimeMillis() + 10000));
        // Passamos uma role que JÁ TEM o prefixo
        obj.setRoles(List.of("ROLE_GESTOR"));

        // --- ACT ---
        String token = jwtCreator.gerarToken(obj);
        JWTObject resultado = jwtCreator.parseToken(token);

        // --- ASSERT ---
        // Garante que não ficou "ROLE_ROLE_GESTOR"
        Assertions.assertEquals("ROLE_GESTOR", resultado.getRoles().get(0));
    }
}