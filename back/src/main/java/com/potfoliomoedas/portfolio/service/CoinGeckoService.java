package com.potfoliomoedas.portfolio.service;

import com.potfoliomoedas.portfolio.dto.CoinGeckoSearchResponse;
import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoinGeckoService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MoedaRepository moedaRepository;

    @Value("${coingecko.api.key:}")
    private String apiKey;

    private Map<String, Map<String, Number>> CACHE_PRECOS = new ConcurrentHashMap<>();
    private Map<String, GraficoCache> CACHE_GRAFICOS = new ConcurrentHashMap<>();

    private Map<String, Long> ULTIMA_ATUALIZACAO = new ConcurrentHashMap<>();

    private static class GraficoCache {
        List<List<Number>> dados;
        long ultimaAtualizacao;

        public GraficoCache(List<List<Number>> dados) {
            this.dados = dados;
            this.ultimaAtualizacao = System.currentTimeMillis();
        }

        public boolean isExpirado() {
            return (System.currentTimeMillis() - ultimaAtualizacao) > 3600000;
        }
    }

    private <T> T getComChave(String url, Class<T> responseType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.set("x-cg-demo-api-key", apiKey);
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("⚠️ Erro na requisição CoinGecko (" + url + "): " + e.getMessage());
            return null;
        }
    }

    public Map<String, Map<String, Number>> buscarPrecosEmLote(List<String> ids) {
        return CACHE_PRECOS;
    }

    @Scheduled(fixedRate = 1200000)
    public void atualizarPrecosAutomaticamente() {
        System.out.println("🤖 Robô: Iniciando atualização com API Key...");

        List<String> moedasNoBanco = moedaRepository.findDistinctCoinIds();
        if (moedasNoBanco == null || moedasNoBanco.isEmpty()) return;

        String idsParam = String.join(",", moedasNoBanco);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idsParam + "&vs_currencies=brl,usd,eur";

        Map respostaApi = getComChave(url, Map.class);

        if (respostaApi != null && !respostaApi.isEmpty()) {
            CACHE_PRECOS.putAll(respostaApi);
            System.out.println("✅ Robô: Preços atualizados com sucesso.");
        }
    }

    public void atualizarPrecoUnico(String coinId) {
        long agora = System.currentTimeMillis();
        long ultimaVez = ULTIMA_ATUALIZACAO.getOrDefault(coinId, 0L);

        if (CACHE_PRECOS.containsKey(coinId) && (agora - ultimaVez) < 120000) {
            return;
        }

        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=brl,usd,eur";

        Map respostaApi = getComChave(url, Map.class);

        if (respostaApi != null && !respostaApi.isEmpty()) {
            CACHE_PRECOS.putAll(respostaApi);
            ULTIMA_ATUALIZACAO.put(coinId, agora);
        }
    }

    public List<List<Number>> buscarHistorico(String coinId, String dias, String currency) {
        String chaveCache = coinId.toLowerCase() + "_" + dias + "_" + currency.toLowerCase();
        GraficoCache cache = CACHE_GRAFICOS.get(chaveCache);

        if (cache != null && !cache.isExpirado()) {
            return cache.dados;
        }

        String url = "https://api.coingecko.com/api/v3/coins/" + coinId +
                "/market_chart?vs_currency=" + currency.toLowerCase() + "&days=" + dias;

        Map resposta = getComChave(url, Map.class);

        if (resposta != null && resposta.containsKey("prices")) {
            List<List<Number>> dadosNovos = (List<List<Number>>) resposta.get("prices");
            CACHE_GRAFICOS.put(chaveCache, new GraficoCache(dadosNovos));
            return dadosNovos;
        }

        if (cache != null) return cache.dados;
        return List.of();
    }

    public List<CoinGeckoSearchResponse.CoinThumb> buscarMoedasNaCoinGecko(String query) {
        String url = "https://api.coingecko.com/api/v3/search?query=" + query;

        CoinGeckoSearchResponse resposta = getComChave(url, CoinGeckoSearchResponse.class);

        if (resposta != null && resposta.coins() != null) {
            return resposta.coins();
        }
        return List.of();
    }

    public String buscarUrlLogo(String coinId) {
        // Cuidado: Esse método busca muitas moedas. O ideal é salvar o logo no banco na hora de criar.
        List<CoinGeckoSearchResponse.CoinThumb> resultados = buscarMoedasNaCoinGecko(coinId);
        if (resultados != null) {
            for (CoinGeckoSearchResponse.CoinThumb coin : resultados) {
                if (coin.id().equalsIgnoreCase(coinId)) {
                    return coin.thumb();
                }
            }
        }
        return null;
    }

    @PostConstruct
    public void aoIniciar() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                atualizarPrecosAutomaticamente();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
