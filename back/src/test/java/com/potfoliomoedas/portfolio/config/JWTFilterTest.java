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
    private JWTFilter jwtFilter;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve autenticar o usuário quando o token for válido")
    void doFilterInternal_TokenValido() throws ServletException, IOException, java.security.SignatureException {
        String tokenValido = "Bearer token.valido.aqui";

        when(request.getHeader("Authorization")).thenReturn(tokenValido);

        JWTObject tokenDecodificado = new JWTObject();
        tokenDecodificado.setSubject("usuario@email.com");
        tokenDecodificado.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        when(jwtCreator.parseToken(tokenValido)).thenReturn(tokenDecodificado);
        jwtFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "A autenticação não deveria ser nula");
        assertEquals("usuario@email.com", auth.getPrincipal());
        assertEquals(2, auth.getAuthorities().size());
    }

    @Test
    @DisplayName("Deve continuar a requisição (anônima) se não houver token")
    void doFilterInternal_SemToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);
        jwtFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve retornar 403 Forbidden se o token for inválido/falso")
    void doFilterInternal_TokenInvalido() throws ServletException, IOException, java.security.SignatureException {
        String tokenRuim = "Bearer token.falsificado";
        when(request.getHeader("Authorization")).thenReturn(tokenRuim);
        when(jwtCreator.parseToken(tokenRuim)).thenThrow(new SignatureException("Assinatura inválida"));
        jwtFilter.doFilter(request, response, filterChain);
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(filterChain, never()).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    @DisplayName("Deve retornar 403 se o token estiver malformado")
    void doFilterInternal_TokenMalformado() throws ServletException, IOException, java.security.SignatureException {
        String tokenLixo = "Bearer lixo";
        when(request.getHeader("Authorization")).thenReturn(tokenLixo);

        when(jwtCreator.parseToken(tokenLixo)).thenThrow(new MalformedJwtException("Token quebrado"));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}