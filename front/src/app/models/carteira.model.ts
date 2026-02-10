export interface Moeda {
  coinId: string;
  quantidade: number;
  logo: string;
  
  precoAtualBRL: number;
  seuSaldoEmBRL: number;
  
  precoAtualUSD: number;
  seuSaldoEmUSD: number;
  
  precoAtualEUR: number;
  seuSaldoEmEUR: number;
}

export interface CarteiraResponse {

  usuarioEmail: string;   
  usuarioNome: string;     
  seuSaldoTotalBRL: number;    
  seuSaldoTotalUSD: number;
  seuSaldoTotalEUR: number;
  moedas: Moeda[];
} 

