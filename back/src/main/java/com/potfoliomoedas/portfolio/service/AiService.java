package com.potfoliomoedas.portfolio.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.potfoliomoedas.portfolio.model.Moeda;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String analisarCarteira(List<Moeda> moedasDoUsuario) {
        RestTemplate restTemplate = new RestTemplate();

        ObjectMapper mapper = new ObjectMapper();
        String carteiraEmJson = "";
        try {
            carteiraEmJson = mapper.writeValueAsString(moedasDoUsuario);
        } catch (Exception e) {
            System.err.println("Erro ao converter moedas para texto: " + e.getMessage());
            carteiraEmJson = "Carteira vazia ou erro na leitura.";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        String promptSistema = "Você é um consultor financeiro sênior especialista em criptomoedas. " +
                "Analise o seguinte portfólio de um usuário: " + carteiraEmJson + ". " +
                "Responda ESTRITAMENTE em formato JSON com as seguintes chaves: " +
                "\"score_risco\" (um número inteiro de 0 a 100 indicando o risco da carteira), " +
                "\"analise_curta\" (um parágrafo de 3 linhas com um insight sobre a diversificação), e " +
                "\"sugestao_rebalanceamento\" (uma lista com 2 ou 3 strings de dicas práticas). " +
                "Não escreva nada além do JSON.";

        Map<String, Object> body = Map.of(
                "model", "gpt-3.5-turbo", // Modelo rápido e barato (pode usar gpt-4o-mini se preferir)
                "messages", List.of(
                        Map.of("role", "system", "content", promptSistema)
                ),
                "temperature", 0.7 // 0.7 deixa ela criativa, mas não muito maluca
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {

            ResponseEntity<Map> response = restTemplate.postForEntity(OPENAI_URL, request, Map.class);

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");

            return (String) message.get("content");

        } catch (Exception e) {
            System.err.println("Erro ao chamar a OpenAI: " + e.getMessage());

            return "{\"score_risco\": 0, \"analise_curta\": \"Serviço de IA temporariamente indisponível.\", \"sugestao_rebalanceamento\": []}";
        }
    }
}