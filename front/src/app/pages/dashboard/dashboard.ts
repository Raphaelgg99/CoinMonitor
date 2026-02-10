import { Component, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; 

// 👇 1. IMPORTAÇÕES DO GRÁFICO (APEXCHARTS)
import {
  NgApexchartsModule,
  ApexAxisChartSeries,
  ApexChart,
  ApexXAxis,
  ApexDataLabels,
  ApexTooltip,
  ApexStroke,
  ApexTheme,
  ApexFill
} from "ng-apexcharts";

import { CarteiraService } from '../../services/carteira';
import { CarteiraResponse } from '../../models/carteira.model';

// Definição do Tipo para o Gráfico
export type ChartOptions = {
  series: ApexAxisChartSeries;
  chart: ApexChart;
  xaxis: ApexXAxis;
  stroke: ApexStroke;
  tooltip: ApexTooltip;
  dataLabels: ApexDataLabels;
  theme: ApexTheme;
  fill: ApexFill;
  yaxis: any;
};

@Component({
  selector: 'app-dashboard',
  standalone: true,
  // 👇 2. O MÓDULO DO GRÁFICO ENTRA AQUI
  imports: [CommonModule, FormsModule, NgApexchartsModule],
  templateUrl: './dashboard.html', // Verifique se o nome é dashboard.html ou dashboard.component.html
  styleUrls: ['./dashboard.css'] // Verifique se é css ou scss
})
export class DashboardComponent implements OnInit { 

  moedaSelecionada: 'BRL' | 'USD' | 'EUR' = 'BRL'; 

  sugestoesMoedas: any[] = [];
  mostrandoSugestoes: boolean = false;

  carteira: CarteiraResponse | null = null;
  isLoading = true;  
  
  // Variáveis do Formulário do Modal
  novaMoedaId: string = '';
  novaMoedaQtd: number | null = null;  
  novaMoedaLogo: string = '';

  // Variáveis para controle de Modais
  moedaParaExcluir: string | null = null; 
  moedaParaEditar: string | null = null;
  qtdParaEditar: number | null = null;

  // === VARIÁVEIS DO GRÁFICO 📊 ===
  public chartOptions: Partial<ChartOptions> | any;
  public moedaGraficoId: string = '';
  public carregandoGrafico: boolean = false; 
  public periodoSelecionado: string = '7'; 
  mostrarAvisoPrecos: boolean = true;

  constructor(
    private carteiraService: CarteiraService, 
    private cdr: ChangeDetectorRef
  ) {
    // === CONFIGURAÇÃO INICIAL DO GRÁFICO (DARK MODE) ===
    this.chartOptions = {
      series: [],
      chart: {
        type: "area",
        height: 350,
        background: '#212529',
        toolbar: { show: false }
      },
      dataLabels: { enabled: false },
      stroke: { curve: "smooth", width: 2, colors: ['#0dcaf0'] }, // Azul Ciano
      xaxis: {
        type: "datetime",
        labels: { style: { colors: '#fff' } }
      },
      yaxis: {
        labels: { 
          style: { colors: '#fff' },
          formatter: (value: number) => { return "R$ " + value.toFixed(2) } 
        }
      },
      tooltip: { theme: 'dark' },
      fill: {
        type: "gradient",
        gradient: {
          shadeIntensity: 1,
          opacityFrom: 0.7,
          opacityTo: 0.1,
          stops: [0, 100]
        }
      },
      theme: { mode: 'dark' }
    };
  }

  ngOnInit() {
    this.carregarDados();
  } 

  fecharAviso() {
    this.mostrarAvisoPrecos = false;
  }

  carregarDados() {
    this.carteiraService.getCarteira().subscribe({
      next: (dados) => {
        console.log('✅ Dados recebidos, atualizando tela...');
        this.carteira = dados;
        this.isLoading = false;
        this.cdr.detectChanges(); 
      },
      error: (err) => {
        console.error('❌ Erro:', err);
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    }); 
  } 

  abrirGrafico(coinId: string) {
    this.moedaGraficoId = coinId;
    this.periodoSelecionado = '7'; // Começa sempre com 7 dias
    this.carregarGraficoComPeriodo(this.periodoSelecionado); // Chama a função nova
  }

  carregarGraficoComPeriodo(dias: string) {
    this.periodoSelecionado = dias;
    this.carregandoGrafico = true;
    this.chartOptions.series = [];

    // 1. Descobre qual símbolo usar no gráfico
    let simbolo = 'R$ ';
    if (this.moedaSelecionada === 'USD') simbolo = '$ ';
    if (this.moedaSelecionada === 'EUR') simbolo = '€ ';

    // 2. Atualiza a formatação do Eixo Y dinamicamente
    this.chartOptions.yaxis = {
        labels: { 
          style: { colors: '#fff' },
          formatter: (value: number) => { return simbolo + value.toFixed(2) } 
        }
    };

    // 3. Chama o serviço passando a moeda selecionada (BRL, USD ou EUR)
    this.carteiraService.buscarHistorico(this.moedaGraficoId, dias, this.moedaSelecionada)
      .subscribe({
        next: (dados) => {
          this.chartOptions.series = [{
            name: `Preço (${this.moedaSelecionada})`, // Ex: Preço (USD)
            data: dados
          }];
          this.carregandoGrafico = false;
          // Força atualização das opções do gráfico para pegar o novo símbolo
          this.chartOptions = { ...this.chartOptions }; 
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error("Erro no gráfico", err);
          this.carregandoGrafico = false;
        }
      });
  }

  // --- ADICIONAR MOEDA ---
  adicionarMoeda() {
    if (!this.novaMoedaId || !this.novaMoedaQtd) {
      alert('Preencha todos os campos!');
      return;
    }

    this.carteiraService.adicionarMoeda(this.novaMoedaId.toLowerCase(), this.novaMoedaQtd, this.novaMoedaLogo)
      .subscribe({
        next: () => {
          alert('Moeda adicionada com sucesso! 🚀\n\nNota: Se o preço aparecer zerado, aguarde cerca de 5 minutos para a atualização automática.');
    
          this.novaMoedaId = '';
          this.novaMoedaQtd = null; 
          this.novaMoedaLogo = '';

          const btnFechar = document.getElementById('fecharModalBtn');
          btnFechar?.click();

          this.isLoading = true;
          this.carregarDados();
        },
        error: (err) => {
          console.error('Erro ao adicionar:', err);
          alert('Erro ao adicionar moeda. Verifique o ID (ex: bitcoin).');
        }
      });
  } 

  // --- EXCLUIR MOEDA ---
  prepararExclusao(coinId: string) {
    this.moedaParaExcluir = coinId;
  }

  confirmarExclusao() {
    if (this.moedaParaExcluir) {
      this.carteiraService.deletarMoeda(this.moedaParaExcluir).subscribe({
        next: () => {
          this.carregarDados();
          
          const btnFechar = document.getElementById('fecharModalExclusaoBtn');
          btnFechar?.click();
          
          this.moedaParaExcluir = null;
        },
        error: (err) => {
          console.error('Erro ao excluir:', err);
          alert('Erro ao excluir moeda.');
        }
      });
    }
  } 
  
  // --- EDITAR QUANTIDADE ---
  prepararEdicao(moeda: any) {
    this.moedaParaEditar = moeda.coinId;
    this.qtdParaEditar = moeda.quantidade; 
  }

  confirmarEdicao() {
    if (this.moedaParaEditar && this.qtdParaEditar !== null) {
      this.carteiraService.editarQuantidade(this.moedaParaEditar, this.qtdParaEditar)
        .subscribe({
          next: () => {
            alert('Quantidade atualizada com sucesso!');
            
            const btnFechar = document.getElementById('fecharModalEdicaoBtn');
            btnFechar?.click();
            
            this.carregarDados();
          },
          error: (err) => {
            console.error('Erro ao editar:', err);
            alert('Erro ao atualizar quantidade.');
          }
        });
    }
  } 

  // --- AUTOCOMPLETE ---
  buscarSugestoes(evento: any) {
    const query = evento.target.value;

    if (query.length > 0) {
      this.carteiraService.buscarMoedasCoinGecko(query).subscribe({
        next: (res: any) => {
          this.sugestoesMoedas = res || []; 
          this.mostrandoSugestoes = true;
        },
        error: (err) => {
          console.error('Erro ao buscar moeda', err);
        }
      });
    } else {
      this.sugestoesMoedas = [];
      this.mostrandoSugestoes = false;
    }
  } 

  selecionarSugestao(moeda: any) {
    this.novaMoedaId = moeda.id; 
    this.mostrandoSugestoes = false; 
    this.novaMoedaLogo = moeda.thumb; 
  }

  esconderSugestoes() {
    setTimeout(() => {
      this.mostrandoSugestoes = false;
    }, 200);
  }
}