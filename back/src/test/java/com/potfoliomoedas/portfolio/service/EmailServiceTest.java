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

    // Captura o objeto "carta" que foi passado para o correio
    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @BeforeEach
    void setup() {
        // Injeta o valor da variável @Value na força bruta (pois não tem Spring aqui)
        ReflectionTestUtils.setField(emailService, "remetente", "sistema@coinmonitor.com");
    }

    @Test
    @DisplayName("Deve montar o email corretamente e enviar")
    void enviarEmailTexto_Sucesso() {
        // 1. ARRANGE
        String destinatario = "usuario@teste.com";
        String titulo = "Bem-vindo";
        String corpo = "Olá!";

        // 2. ACT
        emailService.enviarEmailTexto(destinatario, titulo, corpo);

        // 3. ASSERT
        // Verifica se o javaMailSender.send() foi chamado e captura o argumento
        verify(javaMailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage mensagemEnviada = messageCaptor.getValue();

        // Confere se os dados da mensagem estão certos
        assertEquals("sistema@coinmonitor.com", mensagemEnviada.getFrom());
        assertEquals(destinatario, mensagemEnviada.getTo()[0]);
        assertEquals(titulo, mensagemEnviada.getSubject());
        assertEquals(corpo, mensagemEnviada.getText());
    }

    @Test
    @DisplayName("Não deve quebrar a aplicação se o envio falhar (Cai no Catch)")
    void enviarEmailTexto_ErroNoEnvio() {
        // 1. ARRANGE
        // Simula que o servidor de email caiu (lança erro)
        doThrow(new RuntimeException("Servidor SMTP fora do ar"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        // 2. ACT & ASSERT
        // O método tem um try-catch que engole o erro e printa no console.
        // Então o teste garante que nenhuma exceção explode na cara do usuário.
        assertDoesNotThrow(() ->
                emailService.enviarEmailTexto("a", "b", "c")
        );

        // Confirma que tentou enviar, mesmo dando erro
        verify(javaMailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}