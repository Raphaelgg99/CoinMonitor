import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router) {}

  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    
    // Deixa a requisição passar e observa a resposta
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        
        // Se o erro for 403 (Proibido) ou 401 (Não autorizado)
        // Significa que o token é inválido ou expirou
        if (error.status === 403 || error.status === 401) {
            console.warn('Token expirado ou inválido. Deslogando...');
            
            // 1. Limpa o token vencido do navegador
            localStorage.removeItem('token');
            localStorage.removeItem('username'); // ou qualquer outro dado
            
            // 2. Chuta o usuário para a Home (Login)
            this.router.navigate(['/']); 
            
            // Opcional: Fechar modais abertos se tiver usando Bootstrap via JS
            document.getElementById('closeSettingsModal')?.click();
        }

        // Passa o erro para frente (caso o componente queira mostrar algum alerta)
        return throwError(() => error);
      })
    );
  }
}