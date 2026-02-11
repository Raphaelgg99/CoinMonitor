import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CarteiraResponse } from '../models/carteira.model';

@Injectable({
  providedIn: 'root'
})
export class CarteiraService {

  // Ajuste a URL conforme seu Controller Java (ex: @RequestMapping("/carteira"))
  private apiUrl = 'https://coinmonitor.onrender.com/usuario/carteira';

  constructor(private http: HttpClient) { }

  getCarteira(): Observable<CarteiraResponse> {
    // Pega o token salvo
    const token = localStorage.getItem('token');

    // Monta o cabeçalho com o Token
    const headers = new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });

    return this.http.get<CarteiraResponse>(this.apiUrl, { headers });
  }

  // Adicione isso na classe CarteiraService se ainda não tiver
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

   // Adicione isso na classe CarteiraService se ainda não tiver
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

    // Chama: localhost:8080/usuario/carteira/buscar-moeda?query=bitcoin
    return this.http.get(`${this.apiUrl}/buscar-moeda?query=${query}`, { headers });
  }

buscarHistorico(coinId: string, dias: string, currency: string): Observable<any[]> {
    // Pega o token (igual fizemos antes para corrigir o erro 401)
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
