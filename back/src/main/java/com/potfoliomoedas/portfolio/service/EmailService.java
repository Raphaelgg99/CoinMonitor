package com.potfoliomoedas.portfolio.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EmailService {

    // AQUI ESTÁ A MUDANÇA MÁGICA:
    // O Spring vai buscar essa chave lá nas variáveis do Render
    @Value("${BREVO_API_KEY}")
    private String apiKey;

    // Remetente (Pode ficar fixo ou virar variavel também, mas fixo não é perigoso)
    private final String REMETENTE_EMAIL = "raphaelprix99@gmail.com";
    private final String REMETENTE_NOME = "CoinMonitor";

    public void enviarEmailTexto(String destinatario, String titulo, String mensagem) {
        try {
            System.out.println("🚀 Tentando enviar e-mail via API HTTP para: " + destinatario);

            String jsonBody = String.format(
                    "{\"sender\":{\"name\":\"%s\",\"email\":\"%s\"},\"to\":[{\"email\":\"%s\"}],\"subject\":\"%s\",\"textContent\":\"%s\"}",
                    REMETENTE_NOME,
                    REMETENTE_EMAIL,
                    destinatario,
                    titulo,
                    mensagem.replace("\n", "\\n").replace("\"", "\\\"")
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("accept", "application/json")
                    .header("api-key", apiKey) // <--- Usa a variável injetada
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                System.out.println("✅ Sucesso! E-mail enviado. ID: " + response.body());
            } else {
                System.out.println("❌ Falha na API Brevo: " + response.body());
            }

        } catch (Exception e) {
            System.out.println("❌ Erro grave no envio: ");
            e.printStackTrace();
        }
    }
}