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

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(emailService, "remetente", "sistema@coinmonitor.com");
    }

    @Test
    @DisplayName("Deve montar o email corretamente e enviar")
    void enviarEmailTexto_Sucesso() {
        String destinatario = "usuario@teste.com";
        String titulo = "Bem-vindo";
        String corpo = "Olá!";

        emailService.enviarEmailTexto(destinatario, titulo, corpo);
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage mensagemEnviada = messageCaptor.getValue();

        assertEquals("sistema@coinmonitor.com", mensagemEnviada.getFrom());
        assertEquals(destinatario, mensagemEnviada.getTo()[0]);
        assertEquals(titulo, mensagemEnviada.getSubject());
        assertEquals(corpo, mensagemEnviada.getText());
    }

    @Test
    @DisplayName("Não deve quebrar a aplicação se o envio falhar (Cai no Catch)")
    void enviarEmailTexto_ErroNoEnvio() {
        doThrow(new RuntimeException("Servidor SMTP fora do ar"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() ->
                emailService.enviarEmailTexto("a", "b", "c")
        );

        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}