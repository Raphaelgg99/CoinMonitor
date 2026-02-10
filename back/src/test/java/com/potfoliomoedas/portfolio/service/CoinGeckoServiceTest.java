package com.potfoliomoedas.portfolio.service;

import static org.junit.jupiter.api.Assertions.*;

import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinGeckoServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private MoedaRepository moedaRepository;

    @InjectMocks
    private CoinGeckoService service;

    @Test
    @DisplayName("Deve usar o CACHE na segunda chamada se for feita rapidamente")
    void deveUsarCacheParaGraficos() {
        // 1. ARRANGE
        String id = "bitcoin";
        String dias = "7";
        String moeda = "usd";

        // Simulando resposta da API: {"prices": [[1000, 50000], [1001, 51000]]}
        List<Number> ponto1 = List.of(1620000000000L, 50000.0);
        List<Number> ponto2 = List.of(1620000001000L, 51000.0);
        List<List<Number>> dadosApi = List.of(ponto1, ponto2);
        Map<String, Object> respostaApi = Map.of("prices", dadosApi);

        // Ensina o mock a devolver isso quando chamarem a URL
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(respostaApi);

        // 2. ACT

        // Primeira chamada (Deve ir na API)
        List<List<Number>> resultado1 = service.buscarHistorico(id, dias, moeda);

        // Segunda chamada imediata (Deve vir do Cache)
        List<List<Number>> resultado2 = service.buscarHistorico(id, dias, moeda);

        // 3. ASSERT
        assertEquals(2, resultado1.size());
        assertEquals(2, resultado2.size());

        // A prova real: O RestTemplate só pode ter sido chamado 1 VEZ!
        verify(restTemplate, times(1)).getForObject(anyString(), eq(Map.class));
    }

    @Test
    @DisplayName("Deve retornar cache antigo ou vazio se a API falhar")
    void deveTratarErroDaApi() {
        // 1. ARRANGE
        // Simula que a API lançou erro (ex: RuntimeException)
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("API fora do ar"));

        // 2. ACT
        List<List<Number>> resultado = service.buscarHistorico("bitcoin", "7", "usd");

        // 3. ASSERT
        // Deve retornar lista vazia e não quebrar o teste
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}