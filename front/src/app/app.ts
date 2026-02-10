import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router'; 
import { HeaderComponent } from './components/header/header'; // (confira o caminho do arquivo)

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('carteira-crypto-front');
}
