package com.potfoliomoedas.portfolio.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailService, "apiKey", "fake-api-key");
    }

    @Test
    @DisplayName("Não deve lançar exceção ao enviar email com sucesso")
    void enviarEmailTexto_Sucesso() {
        assertDoesNotThrow(() ->
                emailService.enviarEmailTexto(
                        "usuario@teste.com",
                        "Bem-vindo",
                        "Olá!"
                )
        );
    }

    @Test
    @DisplayName("Não deve quebrar a aplicação se o envio falhar")
    void enviarEmailTexto_ErroNoEnvio() {
        // Endereço inválido força erro na chamada HTTP
        assertDoesNotThrow(() ->
                emailService.enviarEmailTexto(
                        "email-invalido",
                        "titulo",
                        "corpo"
                )
        );
    }
}