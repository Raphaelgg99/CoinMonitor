package com.potfoliomoedas.portfolio.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potfoliomoedas.portfolio.dto.GoogleLoginDTO;
import com.potfoliomoedas.portfolio.dto.Login;
import com.potfoliomoedas.portfolio.dto.Sessao;
import com.potfoliomoedas.portfolio.exception.InvalidCredentialsException;
import com.potfoliomoedas.portfolio.service.LoginService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false) // Desliga o Spring Security para testar só a lógica do controller
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoginService loginService;

    // --- TESTES DO LOGIN PADRÃO (EMAIL/SENHA) ---

    @Test
    @DisplayName("POST /login - Sucesso: Deve retornar token (200 OK)")
    void loginPadrao_Sucesso() throws Exception {
        // 1. ARRANGE
        // Assumindo que Login é um Record: new Login(email, senha)
        // Se for classe normal, use setters.
        Login loginRequest = new Login("teste@email.com", "123456");

        // Sessão compatível com seu Service (apenas email e token)
        Sessao sessaoMock = new Sessao("teste@email.com", "TOKEN_JWT_VALIDO");

        when(loginService.logar(any(Login.class))).thenReturn(sessaoMock);

        // 2. ACT
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))

                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("teste@email.com"))
                .andExpect(jsonPath("$.token").value("TOKEN_JWT_VALIDO"))
                // Garante que não estamos inventando campos
                .andExpect(jsonPath("$.validade").doesNotExist());
    }

    @Test
    @DisplayName("POST /login - Erro: Credenciais Inválidas (400 Bad Request)")
    void loginPadrao_ErroCredenciais() throws Exception {
        // 1. ARRANGE
        Login loginRequest = new Login("errado@email.com", "senhaerrada");

        // O Service lança InvalidCredentialsException
        when(loginService.logar(any(Login.class)))
                .thenThrow(new InvalidCredentialsException("Credenciais inválidas"));

        // 2. ACT
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))

                // 3. ASSERT
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    @DisplayName("POST /login - Erro: Email não verificado (400 Bad Request)")
    void loginPadrao_ErroNaoVerificado() throws Exception {
        Login loginRequest = new Login("naoverificado@email.com", "123456");

        // O Service lança RuntimeException genérica para validação
        when(loginService.logar(any(Login.class)))
                .thenThrow(new RuntimeException("Seu email ainda não foi verificado. Verifique sua caixa de entrada."));

        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    // --- TESTES DO LOGIN GOOGLE ---

    @Test
    @DisplayName("POST /google - Sucesso: Retorna Sessão (200 OK)")
    void loginGoogle_Sucesso() throws Exception {
        // 1. ARRANGE
        GoogleLoginDTO googleDto = new GoogleLoginDTO();
        googleDto.token = "TOKEN_GOOGLE_VALIDO";

        Sessao sessaoMock = new Sessao("usuario@gmail.com", "TOKEN_JWT_GERADO");

        // O Service retorna a sessão se o token for válido
        when(loginService.logarComGoogle("TOKEN_GOOGLE_VALIDO")).thenReturn(sessaoMock);

        // 2. ACT
        mockMvc.perform(post("/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(googleDto)))

                // 3. ASSERT
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("usuario@gmail.com"))
                .andExpect(jsonPath("$.token").value("TOKEN_JWT_GERADO"));
    }

    @Test
    @DisplayName("POST /google - Erro: Token Inválido (400 Bad Request)")
    void loginGoogle_Erro() throws Exception {
        GoogleLoginDTO googleDto = new GoogleLoginDTO();
        googleDto.token = "TOKEN_FALSO";

        // O Service lança IllegalArgumentException se o token Google for ruim
        when(loginService.logarComGoogle(anyString()))
                .thenThrow(new IllegalArgumentException("Token do Google inválido"));

        mockMvc.perform(post("/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(googleDto)))
                .andExpect(status().isBadRequest());
    }
}
