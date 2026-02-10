package com.potfoliomoedas.portfolio.dto;

import java.util.List;

public record CoinGeckoSearchResponse(List<CoinThumb> coins) {

    // Record interno para mapear cada moeda da lista
    public record CoinThumb(
            String id,
            String name,
            String symbol,
            String thumb // URL da imagemzinha
    ) {}
}