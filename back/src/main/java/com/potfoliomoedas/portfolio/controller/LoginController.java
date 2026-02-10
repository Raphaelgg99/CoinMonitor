package com.potfoliomoedas.portfolio.controller;

import com.potfoliomoedas.portfolio.dto.GoogleLoginDTO;
import com.potfoliomoedas.portfolio.dto.Login;
import com.potfoliomoedas.portfolio.dto.Sessao;
import com.potfoliomoedas.portfolio.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@RestController
public class LoginController {

    @Autowired
    LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<Object> logar(@RequestBody Login login){
        try {
            // Tenta logar
            Sessao sessao = loginService.logar(login);
            return ResponseEntity.ok(sessao);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<Sessao> loginGoogle(@RequestBody GoogleLoginDTO body) throws GeneralSecurityException, IOException {
        try {
            // Chama o serviço que faz toda a mágica (valida, cria user, gera token)
            Sessao sessao = loginService.logarComGoogle(body.token);
            return ResponseEntity.ok(sessao);
        }
        catch (Exception e) {
            // Se o token for falso ou der erro, retorna 400 Bad Request
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }
}