import { Component, OnInit, ChangeDetectorRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

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
  imports: [CommonModule, FormsModule, NgApexchartsModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class DashboardComponent implements OnInit {

  moedaSelecionada: 'BRL' | 'USD' | 'EUR' = 'BRL';

  sugestoesMoedas: any[] = [];
  mostrandoSugestoes: boolean = false;

  carteira: CarteiraResponse | null = null;
  isLoading = true;

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

    this.chartOptions = {
      series: [],
      chart: {
        type: "area",
        height: 350,
        background: '#212529',
        toolbar: { show: false }
      },
      dataLabels: { enabled: false },
      stroke: { curve: "smooth", width: 2, colors: ['#0dcaf0'] },
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
    this.periodoSelecionado = '7';
    this.carregarGraficoComPeriodo(this.periodoSelecionado);
  }

  carregarGraficoComPeriodo(dias: string) {
    this.periodoSelecionado = dias;
    this.carregandoGrafico = true;
    this.chartOptions.series = [];

    let simbolo = 'R$ ';
    if (this.moedaSelecionada === 'USD') simbolo = '$ ';
    if (this.moedaSelecionada === 'EUR') simbolo = '€ ';

    this.chartOptions.yaxis = {
        labels: {
          style: { colors: '#fff' },
          formatter: (value: number) => { return simbolo + value.toFixed(2) }
        }
    };

    this.carteiraService.buscarHistorico(this.moedaGraficoId, dias, this.moedaSelecionada)
      .subscribe({
        next: (dados) => {
          this.chartOptions.series = [{
            name: `Preço (${this.moedaSelecionada})`,
            data: dados
          }];
          this.carregandoGrafico = false;
          this.chartOptions = { ...this.chartOptions };
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error("Erro no gráfico", err);
          this.carregandoGrafico = false;
        }
      });
  }

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
