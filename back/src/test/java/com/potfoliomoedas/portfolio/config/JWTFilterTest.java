package com.potfoliomoedas.portfolio.config;

import static org.junit.jupiter.api.Assertions.*;

import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JWTFilterTest {

    @Mock
    private JWTCreator jwtCreator;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JWTFilter jwtFilter; // Injeta o Mock do jwtCreator no construtor do Filter

    @BeforeEach
    void setup() {
        // Garante que o contexto de segurança começa limpo
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        // Garante que limpamos o contexto após o teste para não afetar outros
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve autenticar o usuário quando o token for válido")
    void doFilterInternal_TokenValido() throws ServletException, IOException, java.security.SignatureException {
        // 1. ARRANGE
        String tokenValido = "Bearer token.valido.aqui";

        // Simula o Header vindo na requisição
        when(request.getHeader("Authorization")).thenReturn(tokenValido);

        // Cria o objeto que o Creator retornaria
        JWTObject tokenDecodificado = new JWTObject();
        tokenDecodificado.setSubject("usuario@email.com");
        tokenDecodificado.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        // Ensina o Creator a retornar esse objeto
        when(jwtCreator.parseToken(tokenValido)).thenReturn(tokenDecodificado);

        // 2. ACT
        // Chamamos o filtro manualmente (simulando o Spring)
        jwtFilter.doFilter(request, response, filterChain);

        // 3. ASSERT
        // Verifica se o filtro deixou a requisição passar adiante
        verify(filterChain, times(1)).doFilter(request, response);

        // O PULO DO GATO: Verificar se o usuário foi "logado" no contexto
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "A autenticação não deveria ser nula");
        assertEquals("usuario@email.com", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size()); // Tem que ter as 2 roles
    }

    @Test
    @DisplayName("Deve continuar a requisição (anônima) se não houver token")
    void doFilterInternal_SemToken() throws ServletException, IOException {
        // 1. ARRANGE
        // Header vem nulo
        when(request.getHeader("Authorization")).thenReturn(null);

        // 2. ACT
        jwtFilter.doFilter(request, response, filterChain);

        // 3. ASSERT
        // O filtro deve deixar passar, mas SEM autenticar ninguém
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden se o token for inválido/falso")
    void doFilterInternal_TokenInvalido() throws ServletException, IOException, java.security.SignatureException {
        // 1. ARRANGE
        String tokenRuim = "Bearer token.falsificado";
        when(request.getHeader("Authorization")).thenReturn(tokenRuim);

        // Simulamos que o Creator lança exceção ao tentar ler
        when(jwtCreator.parseToken(tokenRuim)).thenThrow(new SignatureException("Assinatura inválida"));

        // 2. ACT
        jwtFilter.doFilter(request, response, filterChain);

        // 3. ASSERT
        // Verifica se setou o status 403 na resposta
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());

        // Verifica se NÃO chamou o próximo filtro (bloqueou a requisição)
        // Nota: Seu código tem um "return" no catch, então o chain não é chamado.
        verify(filterChain, never()).doFilter(request, response);

        // Garante que ninguém foi logado
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve retornar 403 se o token estiver malformado")
    void doFilterInternal_TokenMalformado() throws ServletException, IOException, java.security.SignatureException {
        // 1. ARRANGE
        String tokenLixo = "Bearer lixo";
        when(request.getHeader("Authorization")).thenReturn(tokenLixo);

        when(jwtCreator.parseToken(tokenLixo)).thenThrow(new MalformedJwtException("Token quebrado"));

        // 2. ACT
        jwtFilter.doFilter(request, response, filterChain);

        // 3. ASSERT
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}