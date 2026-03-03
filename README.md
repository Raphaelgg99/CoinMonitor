# 🚀 CoinMonitor

> Uma aplicação Fullstack robusta para monitoramento de criptomoedas em tempo real, com autenticação segura e gestão de perfil de usuário.

🌐 **Acesso ao Projeto Ao Vivo:** [Clique aqui para acessar o CoinMonitor](https://coin-monitor-gamma.vercel.app/)

---

## 💡 Sobre o Projeto

O **CoinMonitor** é uma aplicação completa focada na **criação e gestão de portfólios de criptomoedas**. O sistema permite que cada usuário monte a sua própria carteira de investimentos, acompanhando o saldo, a valorização e o histórico dos seus ativos em tempo real. Toda a arquitetura foi desenhada com um forte foco em **Segurança** e **Experiência do Usuário (UX)**, garantindo proteção de dados, cálculos precisos e fluxos robustos de autenticação e verificação.

### ✨ Principais Funcionalidades
- **Consultor Financeiro Inteligente (IA):** Integração com a API da OpenAI (ChatGPT) para analisar a carteira do usuário. A Inteligência Artificial gera um *Score de Risco* (0 a 100) e fornece recomendações personalizadas de rebalanceamento do portfólio.
- **Gestão de Portfólio Personalizado:** O usuário cria e gerencia sua própria carteira, monitorando o lucro, o saldo total e o impacto de cada criptomoeda nos seus investimentos.
- **Dashboard em Tempo Real:** Consumo otimizado da API externa (CoinGecko) para exibição de valores atualizados e **gráficos de evolução** dos ativos.
- **Autenticação Segura:** Login tradicional (JWT) e integração com **Google Sign-In (OAuth2)**.
- **Verificação de Duas Etapas (Email):** Envio de código OTP via e-mail para ativação de conta.
- **Gestão de Perfil:** Atualização de dados, troca de senha segura, upload de foto de perfil (salva no banco de dados) e exclusão permanente de conta.
- **UX Resiliente:** Tratamento de *Cold Starts* (servidor dormindo) com feedback visual para o usuário durante requisições assíncronas.

---

## 🛠️ Tecnologias Utilizadas

**Frontend:**
- **Angular 21:** Standalone Components, RxJS, HttpClient
- **Bootstrap 5:** Estilização, Modais responsivos e UI/UX
- **Google Auth:** Integração nativa para login social
- **ApexCharts:** Renderização de gráficos financeiros interativos

**Backend:**
- **Java 17** & **Spring Boot 3**
- **Spring Security:** Filtros customizados e autenticação Stateless (JWT)
- **Spring Data JPA / Hibernate:** Mapeamento objeto-relacional
- **JavaMailSender:** Envio de códigos de verificação
- **OpenAI API & Jackson:** Integração com LLMs (GPT) e processamento avançado de JSON via `ObjectMapper`

**Qualidade & Testes:**
- **JUnit 5 e Mockito:** Para garantir a integridade das regras de negócio e serviços do backend.

**Banco de Dados & Infraestrutura:**
- **PostgreSQL:** Banco de dados relacional (Hospedado na nuvem)
- **Docker:** Conteinerização da aplicação para garantia de consistência entre ambientes de desenvolvimento e produção
- **Deploy Backend:** Render
- **Deploy Frontend:** Vercel

---

## 🧠 Desafios Técnicos Resolvidos

Durante o desenvolvimento, a arquitetura exigiu a solução de problemas complexos de nível de produção:

1. **Contorno de Rate Limiting e Otimização de API Externa:**
   - *Problema:* A API da CoinGecko possui limites severos de requisições por minuto no plano gratuito. Se cada acesso ou atualização do usuário fizesse uma chamada direta, a aplicação seria rapidamente bloqueada (Erro 429 - Too Many Requests).
   - *Solução:* Arquitetura baseada em **Cache** e processamento assíncrono. Criei um **Bot (Scheduler)** no Spring Boot que roda em background atualizando as cotações automaticamente em intervalos controlados. Quando o usuário acessa o dashboard, ele consome os dados oxigenados do cache interno do servidor em vez de bater na API externa, garantindo tempo de resposta quase imediato e zero bloqueios.

2. **Políticas de CORS e Segurança:**
   - *Solução:* Configuração estrita do Spring Security para permitir requisições seguras apenas do domínio da Vercel, protegendo os endpoints REST e liberando as rotas públicas de login/registro.

3. **Configuração de SMTP e Disparo de E-mails em Produção:**
   - *Problema:* O envio de e-mails contendo os códigos de verificação (OTP) funcionava perfeitamente no ambiente local, mas falhava no ambiente de produção (Render) devido a restrições de segurança de provedores de e-mail.
   - *Solução:* Configuração do protocolo TLS (`mail.smtp.starttls.enable=true`) no Spring Boot para garantir tráfego criptografado. Implementação de "Senhas de Aplicativo" (App Passwords) para autenticação segura, injetando as credenciais dinamicamente via variáveis de ambiente no Render, garantindo que nenhum dado sensível vazasse no repositório.

---

## 🚀 Como Executar o Projeto Localmente

### Pré-requisitos
Antes de começar, você precisará ter as seguintes ferramentas instaladas em sua máquina:
- **Node.js** (v20 ou superior)
- **Angular CLI** (v21+)
- **Java 17** (JDK) e **Maven**
- **PostgreSQL** rodando localmente 

### 1. Clonando o Repositório
```bash
git clone [https://github.com/Raphaelgg99/CoinMonitor.git](https://github.com/Raphaelgg99/CoinMonitor.git)
```

### 2. Configurando e Rodando o Backend (Spring Boot)
1. Navegue até a pasta do backend (ex: `cd back` ou `cd backend`).
2. Configure as variáveis de ambiente na sua máquina/IDE (ou diretamente no `application.properties` para testes locais):
   - Usuário, senha e URL do banco PostgreSQL local.
   - Chaves de API (OpenAI e CoinGecko).
3. Execute o projeto via terminal utilizando o Maven:
```bash
mvn spring-boot:run
```
*(O servidor iniciará na porta `8080` por padrão).*

### 3. Configurando e Rodando o Frontend (Angular)
1. Abra um novo terminal e navegue até a pasta do frontend (ex: `cd front` ou `cd frontend`).
2. Instale todas as dependências do projeto:
```bash
npm install
```
3. Inicie o servidor de desenvolvimento do Angular:
```bash
ng serve
```

### 4. Acessando a Aplicação
Com os dois servidores rodando, abra o seu navegador e acesse:
👉 **http://localhost:4200**
