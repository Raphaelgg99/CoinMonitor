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
      // Pega o token salvo
      const token = localStorage.getItem('token');

      // Monta o cabeçalho com o Token
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

    // Payload que o Java espera
    const payload = {
      nome: usuarioEditado.nome,    // <--- Corrigido de .name para .nome
      email: usuarioEditado.email,
      senha: usuarioEditado.senha   // Se vier vazio, o Java deve ignorar
    };

    return this.http.put(`${this.apiUrl}`,  payload, { headers })
  }

  // Adicione este método no seu AuthService (ou UsuarioService)
uploadFoto(file: File): Observable<any> {
  const formData = new FormData();
  formData.append('file', file);

  const token = localStorage.getItem('token');

    // ⚠️ SEGREDINHO:
    // Criamos o header SÓ com o Authorization.
    // Não coloque 'Content-Type': 'multipart/form-data', senão quebra o envio do arquivo!
    const headers = new HttpHeaders({
        'Authorization': `Bearer ${token}`
    });

  // Ajuste a rota '/usuario/foto' conforme seu Controller no Java
  // Se o seu Controller não tiver @RequestMapping("/usuario"), use apenas '/foto'
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
