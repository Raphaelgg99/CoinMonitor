import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  // URL Base do seu Back-end (Sem o /auth, baseado no seu Controller)
  private apiUrl = 'https://coinmonitor.onrender.com';

  constructor(private http: HttpClient, private router: Router) { }

  loginGoogle(token: string) {
  // Ajuste a URL se necessário
  return this.http.post(`${this.apiUrl}/google`, { token: token });
  }

  // === LOGIN ===
  login(loginData: any) {
    // O backend espera "senha", então está mapeado corretamente aqui
    const payload = {
      email: loginData.email,
      senha: loginData.senha // 'senha' já está correto
    };

    return this.http.post(`${this.apiUrl}/login`, payload).pipe(
      tap((response: any) => {
        if (response.token) { // Se houver um token na resposta, significa que o login foi bem-sucedido
          console.log("Login realizado! Token:", response.token);

          // Armazena o token e o email no localStorage para futuras requisições
          localStorage.setItem('token', response.token);
          localStorage.setItem('user_email', response.email);

          // Redireciona para o dashboard
          this.router.navigate(['/dashboard']);
        }
      })
    );
  }

  // Função de registro de um novo usuário
  register(userData: any) {
    // Monta o payload com os dados do novo usuário
    const payload = {
      nome: userData.name,       // O Java espera "nome"
      email: userData.email,     // O Java espera "email"
      senha: userData.senha      // Agora estamos usando 'senha' ao invés de 'password'
    };

    // Envia os dados para o backend para criar o usuário
    return this.http.post(`${this.apiUrl}/usuario/criarusuario`, payload)
  }

  verifyEmail(payload: { email: string, codigo: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/usuario/verificar`, payload);
  }

  resendCode(email: string): Observable<any> {
  // O Java espera um JSON: {"email": "..."}
  const payload = { email: email };

  return this.http.post(`${this.apiUrl}/usuario/reenviar`, payload);
}
}
