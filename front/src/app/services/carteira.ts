import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarteiraResponse } from '../models/carteira.model'; 
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CarteiraService {

  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) { }

  getCarteira(): Observable<CarteiraResponse> {
    const token = localStorage.getItem('token');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<CarteiraResponse>(`${this.apiUrl}/usuario/carteira`, { headers });
  }

  adicionarMoeda(coinId: string, quantidade: number, logo: string): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    const body = {
      coinId: coinId,
      quantidade: quantidade,
      logo: logo
     };
    return this.http.post(`${this.apiUrl}/usuario/carteira/adicionar`, body, { headers });
  }

  deletarMoeda(coinId: string): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    const body = { coinId };
    return this.http.delete(`${this.apiUrl}/usuario/carteira/${coinId}`, {headers} );
  }

  editarQuantidade(coinId: string, novaQuantidade: number): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    const body = {
      coinId: coinId,
      quantidade: novaQuantidade
     };
    return this.http.put(`${this.apiUrl}/usuario/carteira`, body, { headers });
}

buscarMoedasCoinGecko(query: string): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get(`${this.apiUrl}/usuario/carteira/buscar-moeda?query=${query}`, { headers });
  }

buscarHistorico(coinId: string, dias: string, currency: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<any[]>(
      `${this.apiUrl}/usuario/carteira/historico/${coinId}?dias=${dias}&currency=${currency}`,
      { headers }
    );
  } 

  analisarCarteiraUsuario(): Observable<any>{ 
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<any>(
      `${this.apiUrl}/usuario/carteira/analise-ia`,
      { headers }
    );

  }
}
