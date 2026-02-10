import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';

// 1. Adicionei HTTP_INTERCEPTORS e withInterceptorsFromDi aqui 👇
import { provideHttpClient, withFetch, withInterceptorsFromDi, HTTP_INTERCEPTORS } from '@angular/common/http';

import { SocialLoginModule, SocialAuthServiceConfig } from '@abacritt/angularx-social-login';
import { GoogleLoginProvider } from '@abacritt/angularx-social-login'; 

// 2. Importe o seu Interceptor (CONFIRA SE O CAMINHO ESTÁ CERTO) 👇
import { AuthInterceptor } from './services/auth.interceptor'; 

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),

    // 3. Atualizei o HttpClient para aceitar Interceptores de Classe 👇
    provideHttpClient(
      withFetch(),
      withInterceptorsFromDi() 
    ),

    // 4. Registrei o seu Interceptor aqui 👇
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    },

    // --- Configurações do Google (Mantidas iguais) ---
    importProvidersFrom(SocialLoginModule),
    {
      provide: 'SocialAuthServiceConfig',
      useValue: {
        autoLogin: false,
        providers: [
          {
            id: GoogleLoginProvider.PROVIDER_ID,
            provider: new GoogleLoginProvider(
              '502941747532-no2fvc6ktbl45mkkr159vaaat85cl8dh.apps.googleusercontent.com'
            )
          }
        ],
        onError: (err) => {
          console.error(err);
        }
      } as SocialAuthServiceConfig,
    }
  ]
};