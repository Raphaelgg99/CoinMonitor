package com.potfoliomoedas.portfolio.dto;

import java.util.List;

public record Carteira(
        String usuarioEmail,
        String usuarioNome,
        Double seuSaldoTotalBRL,
        Double seuSaldoTotalUSD,
        Double seuSaldoTotalEUR,
        List<MoedaResponse> moedas // Um detalhamento de cada moeda
) {}