package com.potfoliomoedas.portfolio.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.potfoliomoedas.portfolio.config.JWTCreator;
import com.potfoliomoedas.portfolio.config.JWTObject;
import com.potfoliomoedas.portfolio.config.SecurityConfig;
import com.potfoliomoedas.portfolio.dto.Login;
import com.potfoliomoedas.portfolio.dto.Sessao;
import com.potfoliomoedas.portfolio.dto.UsuarioResponseDTO;
import com.potfoliomoedas.portfolio.exception.InvalidCredentialsException;
import com.potfoliomoedas.portfolio.model.Usuario;
import com.potfoliomoedas.portfolio.repository.UsuarioRepository;
import com.potfoliomoedas.portfolio.service.user.UsuarioServiceUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service
public class LoginService {

    @Autowired
    private UsuarioRepository repository;
    @Autowired
    private BCryptPasswordEncoder encoder;
    @Autowired
    private JWTCreator jwtCreator;
    @Autowired
    private SecurityConfig securityConfig;
    @Autowired
    @Value("${google.client.id}")
    private String googleClientId;
    @Autowired
    private UsuarioServiceUser usuarioService;

    public Sessao logar(Login login) {
        Usuario usuario = repository.findByEmail(login.email())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciais inválidas"));

        boolean passwordOk = encoder.matches(login.senha(), usuario.getSenha());
        if (!passwordOk) {
            throw new InvalidCredentialsException("Credenciais inválidas");
        }
        if (!usuario.isVerificado()) {
            usuarioService.reenviarCodigo(usuario.getEmail());
            throw new RuntimeException("Seu email ainda não foi verificado. Verifique sua caixa de entrada.");
        }
        System.out.println("Roles associadas ao usuário: " + usuario.getRoles());

        JWTObject jwtObject = new JWTObject();
        jwtObject.setIssuedAt(new Date(System.currentTimeMillis()));
        jwtObject.setExpiration(new Date(System.currentTimeMillis() + securityConfig.getEXPIRATION()));
        jwtObject.setRoles(usuario.getRoles());
        jwtObject.setSubject(usuario.getEmail());

        String token = jwtCreator.gerarToken(jwtObject);

        return new Sessao(usuario.getEmail(), token);
    }

    public Sessao logarComGoogle(String tokenGoogle) throws GeneralSecurityException, IOException {

        GoogleIdToken idToken = validarTokenNoGoogle(tokenGoogle);

        if (idToken != null) {
            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String nome = (String) payload.get("name");
            String fotoUrl = (String) payload.get("picture");

            // 3. Verifica ou Cria Usuário
            Usuario usuario = repository.findByEmail(email).orElse(null);

            if (usuario == null) {
                usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setNome(nome);
                usuario.setFotoPerfil(baixarFotoGoogle(fotoUrl));
                usuario.setRoles(new ArrayList<>(List.of("USER")));
                usuario.setSenha("GOOGLE_AUTH_EXTERNO");
                usuario.setVerificado(true);// Senha dummy
                repository.save(usuario);
            }
            if (usuario.getFotoPerfil() == null || usuario.getSenha().equals("GOOGLE_AUTH_EXTERNO")) {
                usuario.setFotoPerfil(baixarFotoGoogle(fotoUrl));
                repository.save(usuario);
            }
            JWTObject jwtObject = new JWTObject();
            jwtObject.setIssuedAt(new Date(System.currentTimeMillis()));
            jwtObject.setExpiration(new Date(System.currentTimeMillis() + securityConfig.getEXPIRATION()));
            jwtObject.setRoles(usuario.getRoles());
            jwtObject.setSubject(usuario.getEmail());

            // 4. Gera o Token do seu sistema (JWT)
            String token = jwtCreator.gerarToken(jwtObject);

            return new Sessao(usuario.getEmail(), token);
        } else {
            throw new IllegalArgumentException("Token do Google inválido");
        }
    }

    // 2. Crie esse método NOVO no final da classe (pode ser protected para o teste ver)
    protected GoogleIdToken validarTokenNoGoogle(String token) throws GeneralSecurityException, IOException {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
        return verifier.verify(token);
    }

    protected byte[] baixarFotoGoogle(String urlFoto) {
        try {
            if (urlFoto == null || urlFoto.isBlank()) return null;

            URL url = new URL(urlFoto);
            try (InputStream in = url.openStream()) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            System.err.println("Erro ao baixar foto do Google: " + e.getMessage());
            return null;
        }
    }
}
