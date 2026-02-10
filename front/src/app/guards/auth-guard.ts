import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

export const authGuard: CanActivateFn = (route, state) => {
  // Injeta o Roteador para poder mandar o usuário embora se precisar
  const router = inject(Router);
  
  // Pega o token salvo no navegador
  const token = localStorage.getItem('token');

  if (token) {
    // Se tem token, deixa passar
    return true;
  } else {
    // Se NÃO tem token, manda para a tela inicial (Login)
    router.navigate(['/']);
    return false;
  }
};