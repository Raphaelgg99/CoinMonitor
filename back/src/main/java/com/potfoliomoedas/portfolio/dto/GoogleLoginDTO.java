package com.potfoliomoedas.portfolio.dto;

import lombok.Data;

public class GoogleLoginDTO {
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    private String token;
}
