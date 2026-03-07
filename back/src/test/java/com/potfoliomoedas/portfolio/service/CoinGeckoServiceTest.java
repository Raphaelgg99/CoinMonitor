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
        String id = "bitcoin";
        String dias = "7";
        String moeda = "usd";

        List<Number> ponto1 = List.of(1620000000000L, 50000.0);
        List<Number> ponto2 = List.of(1620000001000L, 51000.0);
        List<List<Number>> dadosApi = List.of(ponto1, ponto2);
        Map<String, Object> respostaApi = Map.of("prices", dadosApi);

        // ✅ Mockando exchange() que é o que o service realmente usa
        when(restTemplate.exchange(
                anyString(),
                eq(org.springframework.http.HttpMethod.GET),
                any(org.springframework.http.HttpEntity.class),
                eq(Map.class)
        )).thenReturn(org.springframework.http.ResponseEntity.ok(respostaApi));

        List<List<Number>> resultado1 = service.buscarHistorico(id, dias, moeda);
        List<List<Number>> resultado2 = service.buscarHistorico(id, dias, moeda);

        assertEquals(2, resultado1.size());
        assertEquals(2, resultado2.size());

        // ✅ API chamada só 1 vez — segunda vez veio do cache
        verify(restTemplate, times(1)).exchange(
                anyString(),
                eq(org.springframework.http.HttpMethod.GET),
                any(org.springframework.http.HttpEntity.class),
                eq(Map.class)
        );
    }

    @Test
    @DisplayName("Deve retornar lista vazia se a API falhar")
    void deveTratarErroDaApi() {
        // ✅ Mockando exchange() lançando exceção
        when(restTemplate.exchange(
                anyString(),
                eq(org.springframework.http.HttpMethod.GET),
                any(org.springframework.http.HttpEntity.class),
                eq(Map.class)
        )).thenThrow(new RuntimeException("API fora do ar"));

        List<List<Number>> resultado = service.buscarHistorico("bitcoin", "7", "usd");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}