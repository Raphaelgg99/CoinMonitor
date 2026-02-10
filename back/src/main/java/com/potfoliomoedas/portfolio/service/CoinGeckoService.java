package com.potfoliomoedas.portfolio.service;

import com.potfoliomoedas.portfolio.dto.CoinGeckoSearchResponse;
import com.potfoliomoedas.portfolio.repository.MoedaRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CoinGeckoService {

    // A injeção de dependência do RestTemplate permite fazer requisições HTTP para APIs externas, no caso, a API CoinGecko
    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private MoedaRepository moedaRepository;

    // Cache para armazenar os preços das moedas, com a chave sendo o nome da moeda e o valor sendo outro mapa com os preços em várias moedas
    private Map<String, Map<String, Number>> CACHE_PRECOS = new ConcurrentHashMap<>();

    // Cache para armazenar os gráficos históricos das moedas, com a chave sendo uma combinação de moeda, dias e moeda de comparação
    private Map<String, GraficoCache> CACHE_GRAFICOS = new ConcurrentHashMap<>();

    // Classe interna que serve para controlar os gráficos e verificar se estão expirados
    private static class GraficoCache {
        List<List<Number>> dados;  // Dados do gráfico (preços ao longo do tempo)
        long ultimaAtualizacao;    // Timestamp da última atualização do gráfico

        // Construtor que recebe os dados e inicializa o timestamp da última atualização
        public GraficoCache(List<List<Number>> dados) {
            this.dados = dados;
            this.ultimaAtualizacao = System.currentTimeMillis();
        }

        // Método para verificar se o gráfico expirou (se passaram mais de 5 minutos desde a última atualização)
        public boolean isExpirado() {
            return (System.currentTimeMillis() - ultimaAtualizacao) > 300000;
        }
    }

    // Método que retorna os preços das moedas armazenados em cache
    public Map<String, Map<String, Number>> buscarPrecosEmLote(List<String> ids) {
        return CACHE_PRECOS;
    }

    // Método agendado para atualizar os preços das moedas a cada 5 minutos
    @Scheduled(fixedRate = 300000)  // Define que o método será executado a cada 5 minutos (300000 ms)
    public void atualizarPrecosAutomaticamente() {
        System.out.println("🤖 Robô: Iniciando atualização de preços...");

        // Busca os IDs das moedas no banco de dados
        List<String> moedasNoBanco = moedaRepository.findDistinctCoinIds();
        if (moedasNoBanco == null || moedasNoBanco.isEmpty()) {
            return;
        }

        // Constrói a URL para chamar a API do CoinGecko com os IDs das moedas
        String idsParam = String.join(",", moedasNoBanco);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + idsParam + "&vs_currencies=brl,usd,eur";

        try {
            // Faz a requisição à API
            Map respostaApi = restTemplate.getForObject(url, Map.class);
            if (respostaApi != null && !respostaApi.isEmpty()) {
                // Atualiza o cache com os preços retornados pela API
                CACHE_PRECOS.putAll(respostaApi);
                System.out.println("✅ Robô: Preços atualizados.");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Robô: Erro ao atualizar preços (API ocupada).");
        }
    }

    // Método que atualiza o preço de uma moeda específica imediatamente
    public void atualizarPrecoUnico(String coinId) {
        System.out.println("⚡ Atualizando preço imediato para: " + coinId);
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=" + coinId + "&vs_currencies=brl,usd,eur";
        try {
            // Faz a requisição à API para obter o preço da moeda
            Map respostaApi = restTemplate.getForObject(url, Map.class);
            if (respostaApi != null && !respostaApi.isEmpty()) {
                // Atualiza o cache com o preço da moeda específica
                CACHE_PRECOS.putAll(respostaApi);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao atualizar moeda única: " + e.getMessage());
        }
    }

    // Método que busca o histórico de preços de uma moeda específica para um número de dias
    public List<List<Number>> buscarHistorico(String coinId, String dias, String currency) {

        // Cria a chave do cache composta pela moeda, dias e moeda de comparação
        String chaveCache = coinId.toLowerCase() + "_" + dias + "_" + currency.toLowerCase();

        // Tenta obter os dados do gráfico do cache
        GraficoCache cache = CACHE_GRAFICOS.get(chaveCache);

        // Se os dados estiverem no cache e não estiverem expirados, retorna os dados do cache
        if (cache != null && !cache.isExpirado()) {
            System.out.println("🚀 Cache Hit: Gráfico entregue da memória (" + chaveCache + ")");
            return cache.dados;
        }

        System.out.println("🌍 API Call: Buscando gráfico (" + chaveCache + ")");

        // Se não estiver no cache, faz a requisição à API para obter os dados do gráfico
        String url = "https://api.coingecko.com/api/v3/coins/" + coinId +
                "/market_chart?vs_currency=" + currency.toLowerCase() + "&days=" + dias;

        try {
            // Faz a requisição à API
            Map resposta = restTemplate.getForObject(url, Map.class);
            if (resposta != null && resposta.containsKey("prices")) {
                // Obtém os dados de preços da resposta da API
                List<List<Number>> dadosNovos = (List<List<Number>>) resposta.get("prices");

                // Armazena os novos dados no cache
                CACHE_GRAFICOS.put(chaveCache, new GraficoCache(dadosNovos));
                return dadosNovos;
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar gráfico: " + e.getMessage());
            // Caso ocorra um erro, retorna os dados do cache (se existirem)
            if (cache != null) return cache.dados;
        }
        return List.of();  // Retorna uma lista vazia caso não haja dados
    }

    // Método que busca moedas na API do CoinGecko com base em uma query
    public List<CoinGeckoSearchResponse.CoinThumb> buscarMoedasNaCoinGecko(String query) {
        String url = "https://api.coingecko.com/api/v3/search?query=" + query;
        try {
            // Faz a requisição à API e obtém a resposta
            CoinGeckoSearchResponse resposta = restTemplate.getForObject(url, CoinGeckoSearchResponse.class);
            if (resposta != null && resposta.coins() != null) {
                return resposta.coins();
            }
        } catch (Exception e) {
            System.err.println("Erro no Autocomplete: " + e.getMessage());
        }
        return List.of();  // Retorna uma lista vazia caso ocorra um erro
    }

    // Método que busca o logo de uma moeda
    public String buscarUrlLogo(String coinId) {
        try {
            // Chama o método de busca de moedas para encontrar a moeda pelo seu ID
            List<CoinGeckoSearchResponse.CoinThumb> resultados = buscarMoedasNaCoinGecko(coinId);
            if (resultados != null) {
                // Itera sobre os resultados para encontrar o logo da moeda
                for (CoinGeckoSearchResponse.CoinThumb coin : resultados) {
                    if (coin.id().equalsIgnoreCase(coinId)) {
                        return coin.thumb();  // Retorna o logo da moeda
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erro ao buscar logo: " + e.getMessage());
        }
        return null;  // Retorna null caso não encontre o logo
    }

    @PostConstruct
    public void aoIniciar() {
        // Roda em paralelo para não travar a subida do servidor
        new Thread(() -> {
            try {
                Thread.sleep(5000); // Espera 5s pro sistema estabilizar
                atualizarPrecosAutomaticamente();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
