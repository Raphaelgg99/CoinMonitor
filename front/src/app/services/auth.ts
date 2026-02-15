import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {

  private apiUrl = 'https://coinmonitor.onrender.com';

  constructor(private http: HttpClient, private router: Router) { }

  loginGoogle(token: string) {
  return this.http.post(`${this.apiUrl}/google`, { token: token });
  }

  login(loginData: any) {

    const payload = {
      email: loginData.email,
      senha: loginData.senha
    };

    return this.http.post(`${this.apiUrl}/login`, payload).pipe(
      tap((response: any) => {
        if (response.token) {
          console.log("Login realizado! Token:", response.token);
          localStorage.setItem('token', response.token);
          localStorage.setItem('user_email', response.email);
          this.router.navigate(['/dashboard']);
        }
      })
    );
  }

  register(userData: any) {
    const payload = {
      nome: userData.name,
      email: userData.email,
      senha: userData.senha
    };

    return this.http.post(`${this.apiUrl}/usuario/criarusuario`, payload)
  }

  verifyEmail(payload: { email: string, codigo: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/usuario/verificar`, payload);
  }

  resendCode(email: string): Observable<any> {
  const payload = { email: email };

  return this.http.post(`${this.apiUrl}/usuario/reenviar`, payload);
}
}
