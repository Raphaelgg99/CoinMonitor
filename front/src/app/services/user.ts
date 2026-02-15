import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioResponseDTO } from '../models/usuario.model';

@Injectable({
  providedIn: 'root',
})
export class UserService {

  private apiUrl = 'https://coinmonitor.onrender.com/usuario';

  constructor(private http: HttpClient) { }

  getUser(): Observable<UsuarioResponseDTO> {
      const token = localStorage.getItem('token');
      const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

      return this.http.get<UsuarioResponseDTO>(this.apiUrl, { headers });
    }

  update(usuarioEditado: any): Observable<any> {

    const token = localStorage.getItem('token');

    const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });

    const payload = {
      nome: usuarioEditado.nome,
      email: usuarioEditado.email,
      senha: usuarioEditado.senha
    };

    return this.http.put(`${this.apiUrl}`,  payload, { headers })
  }

uploadFoto(file: File): Observable<any> {
  const formData = new FormData();
  formData.append('file', file);

  const token = localStorage.getItem('token');

    const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
    });

  return this.http.post(`${this.apiUrl}/foto`, formData, {headers});
}

deleteUser(): Observable<any>{
   const token = localStorage.getItem('token');

    const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
      });
  return this.http.delete(`${this.apiUrl}`, {headers});
}
}
