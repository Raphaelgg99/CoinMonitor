import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarteiraResponse } from '../models/carteira.model';

@Injectable({
  providedIn: 'root'
})
export class CarteiraService {

  private apiUrl = 'https://coinmonitor.onrender.com/usuario/carteira';

  constructor(private http: HttpClient) { }

  getCarteira(): Observable<CarteiraResponse> {
    const token = localStorage.getItem('token');

    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<CarteiraResponse>(this.apiUrl, { headers });
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
    return this.http.post(`${this.apiUrl}/adicionar`, body, { headers });
  }

  deletarMoeda(coinId: string): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    const body = { coinId };
    return this.http.delete(`${this.apiUrl}/${coinId}`, {headers} );
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
    return this.http.put(`${this.apiUrl}`, body, { headers });
}

buscarMoedasCoinGecko(query: string): Observable<any> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get(`${this.apiUrl}/buscar-moeda?query=${query}`, { headers });
  }

buscarHistorico(coinId: string, dias: string, currency: string): Observable<any[]> {
    const token = localStorage.getItem('token');
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<any[]>(
      `${this.apiUrl}/historico/${coinId}?dias=${dias}&currency=${currency}`,
      { headers }
    );
  }
}
