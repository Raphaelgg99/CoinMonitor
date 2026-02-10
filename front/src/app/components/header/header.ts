import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth'; 
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';  
import { UserService } from '../../services/user'; 

declare var bootstrap: any; // 👈 Adicione isso aqui!

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, GoogleSigninButtonModule, RouterModule], 
  templateUrl: './header.html', 
  styleUrls: ['./header.css']
})
export class HeaderComponent implements OnInit {

  // Variáveis de Login e Registro
  loginData = { email: '', senha: '' }; 
  showPassword = false; 
  registerData = { name: '', email: '', senha: '', confirmPassword: '' };
  
  // Variáveis do Perfil (Configurações)
  userData = { nome: '', email: '', isGoogleAccount: false }; 
  
  // Variáveis de Edição de Senha
  editandoSenha = false; 
  novaSenha = '';        
  confirmNovaSenha = '';  

  showNovaSenha = false;     // 👈 NOVO: Controla o primeiro campo
  showConfirmSenha = false;  // 👈 NOVO: Controla o campo de confirmação 

  editandoNome = false;
  nomeBackup = ''; 

  // 👇 NOVAS VARIÁVEIS PARA A VERIFICAÇÃO
  showVerificationStep = false; // Controla se mostra o form ou o código
  verificationCode = '';        // Guarda o código digitado 

  isLoading = false 

  // Variáveis do Timer
  timerValue: number = 120; // 120 segundos = 2 minutos
  timerInterval: any;
  canResend: boolean = false;  

  fotoPerfilUrl: string | null = null;

  
  constructor(
    private authService: AuthService, 
    public router: Router, 
    private socialAuthService: SocialAuthService, 
    private userService: UserService, 
    private cd: ChangeDetectorRef
  ) {}  
  
  // --- Lógica de Visualização ---

  ehPaginaDashboard(): boolean {
    return this.router.url.includes('/dashboard');
  } 

  togglePassword() {
    this.showPassword = !this.showPassword;
  } 
  
  toggleEditarNome() {
    this.editandoNome = !this.editandoNome;
    
    if (this.editandoNome) {
      // 1. Entrou no modo edição: Salva o nome atual como backup
      this.nomeBackup = this.userData.nome;
    } else {
      // 2. Cancelou: Restaura o nome original
      this.userData.nome = this.nomeBackup;
    }
  }

 toggleEditarSenha() {
    this.editandoSenha = !this.editandoSenha;
    // Se cancelou (editandoSenha virou false), limpa os campos
    if (!this.editandoSenha) {
      this.novaSenha = '';
      this.confirmNovaSenha = '';
      // Reseta a visibilidade por segurança
      this.showNovaSenha = false;    // 👈 NOVO
      this.showConfirmSenha = false; // 👈 NOVO
    }
  } 

  // 👇👇👇 ADICIONE ESSAS DUAS FUNÇÕES NOVAS 👇👇👇
  toggleNovaSenhaVisibility() {
    this.showNovaSenha = !this.showNovaSenha;
  }

  toggleConfirmSenhaVisibility() {
    this.showConfirmSenha = !this.showConfirmSenha;
  }

  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    this.router.navigate(['/login']);
  }

  // --- Lógica do Modal de Configurações ---

  abrirConfiguracoes() {
    this.userService.getUser().subscribe({
      next: (dados: any) => {
        console.log('Perfil carregado:', dados); 
        // Mapeia os dados vindos do Java
        this.userData.nome = dados.nome || dados.name; 
        this.userData.email = dados.email; 
        this.userData.isGoogleAccount = dados.isGoogleAccount;  
        if (dados.fotoBase64) {
            this.fotoPerfilUrl = `data:image/jpeg;base64,${dados.fotoBase64}`;
        } else {
            this.fotoPerfilUrl = null; // Garante o ícone cinza se não tiver foto
        }
        this.cd.detectChanges(); 
      },
      error: (err) => {
        console.error('Erro ao buscar perfil', err);
      }
    });
  }

  salvarPerfil() {
    // 1. Validação de Senha (apenas se estiver editando)
    if (this.editandoSenha) {
        if (!this.novaSenha || !this.confirmNovaSenha) {
            alert("Por favor, preencha a senha e a confirmação.");
            return;
        }
        if (this.novaSenha !== this.confirmNovaSenha) {
            alert("As senhas não coincidem!");
            return;
        }
        if (this.novaSenha.length < 8) { 
            alert("A nova senha deve ter no mínimo 8 caracteres."); 
            return; 
        } 

        if (!/[A-Z]/.test(this.novaSenha)) { 
          alert("Precisa de letra Maiúscula."); 
          return; 
        }
        if (!/[a-z]/.test(this.novaSenha)) { 
          alert("Precisa de letra minúscula."); 
          return; 
        }
        if (!/[0-9]/.test(this.novaSenha)) { 
          alert("Precisa de número."); 
          return;
        }
        if (!/[!@#$%^&*(),.?":{}|<>]/.test(this.novaSenha)) { 
          alert("Precisa de caractere especial."); 
          return; 
        }
        if (!this.userData.nome || this.userData.nome.trim() === '') {
        alert('O nome não pode ficar vazio.');
        return; 
        }
    }

    // 2. Monta o objeto para enviar ao Backend
    const dadosParaAtualizar = {
        nome: this.userData.nome,
        email: this.userData.email,
        // Se estiver editando, manda a novaSenha. Se não, manda null.
        senha: this.editandoSenha ? this.novaSenha : null 
    };

    console.log('Enviando atualização:', dadosParaAtualizar);

    // 3. Chama o Serviço
    this.userService.update(dadosParaAtualizar).subscribe({
        next: (res) => {
            console.log('Sucesso:', res);
            alert('Perfil atualizado com sucesso!');
            
            // Fecha o modal via Javascript nativo
            document.getElementById('closeSettingsModal')?.click(); 
            
            
            // Reseta o estado da senha
            if (this.editandoSenha) this.toggleEditarSenha(); 
            if (this.editandoNome) this.editandoNome = false; // 👈 TRAVA O NOME
            
            // Atualiza o nome no localStorage para refletir no Dashboard imediatamente
            localStorage.setItem('username', this.userData.nome);
        },
        error: (err) => {
            console.error('Erro ao atualizar:', err);
            const msg = err.error?.message || 'Erro ao atualizar dados.';
            alert(msg);
        }
    });
  }

  // --- Lógica de Inicialização e Auth (Mantida igual) ---

  ngOnInit() {
      this.socialAuthService.authState.subscribe((user) => {
        console.log('Google Token recebido...');
         this.authService.loginGoogle(user.idToken).subscribe({
            next: (res: any) => {
                console.log('Login Google Sucesso!', res);
                localStorage.setItem('token', res.token); 
                const nomeUsuario = res.nome || res.sessao?.nome || user.name;
                localStorage.setItem('username', nomeUsuario);

                document.getElementById('closeLoginModal')?.click();
                document.getElementById('closeRegisterModal')?.click();
                this.router.navigate(['/dashboard']);
            },
            error: (err) => {
                console.error('Erro Google:', err);
                alert('Erro de conexão com o servidor.');
            }
         });
      });
  }

  onLogin() {
      if (!this.loginData.email || !this.loginData.senha) {
        alert('Preencha todos os campos!');
        return;
      }
      this.authService.login(this.loginData).subscribe({
        next: (res) => {
            // ... (seu código de sucesso atual) ...
            document.getElementById('closeLoginModal')?.click();
            this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          // Pega a mensagem de erro que veio do Java
          const mensagemErro = err.error?.message || 'Erro ao fazer login';

          // 👇 Verifica se é o caso da conta não verificada
          if (mensagemErro.includes("Seu email ainda não foi verificado")) {
              
              alert(mensagemErro); // "Um novo código foi enviado..."

              // 1. Fecha o Modal de Login
              document.getElementById('closeLoginModal')?.click();

              // 2. Prepara os dados para a tela de verificação
              // (Importante: passa o email que ele tentou logar para o formulário de validação)
              this.registerData.email = this.loginData.email;
              this.showVerificationStep = true;
              this.verificationCode = ''; // Limpa código anterior

              // 3. Abre o Modal de Registro (onde fica a verificação)
              // Precisamos de um pequeno delay para a troca de modais ficar suave
              setTimeout(() => {
                  const registerModal = new bootstrap.Modal(document.getElementById('registerModal')!);
                  registerModal.show();
                  
                  // Inicia o timer do código novo que acabou de ser enviado
                  this.iniciarTimer(); 
              }, 500);

          } else {
              // Erro comum (senha errada, etc)
              alert(mensagemErro);
          }
        }
      });
  }

  onRegister() { 
    // Validações
    const emailPadrao = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (this.registerData.senha !== this.registerData.confirmPassword) {
      alert('As senhas não conferem!'); return;
    }
    if (!this.registerData.name || !this.registerData.email || !this.registerData.senha) {
      alert('Preencha todos os campos!'); return;
    } 
    if (!emailPadrao.test(this.registerData.email)) {
        alert("Digite um e-mail válido."); return; 
    } 
    // Validações de força de senha
    if (this.registerData.senha.length < 8) { alert("Mínimo 8 caracteres."); return; } 
    if (!/[A-Z]/.test(this.registerData.senha)) { alert("Precisa de letra Maiúscula."); return; }
    if (!/[a-z]/.test(this.registerData.senha)) { alert("Precisa de letra minúscula."); return; }
    if (!/[0-9]/.test(this.registerData.senha)) { alert("Precisa de número."); return; }
    if (!/[!@#$%^&*(),.?":{}|<>]/.test(this.registerData.senha)) { alert("Precisa de caractere especial."); return; } 

    this.isLoading = true;

    this.authService.register(this.registerData).subscribe({
      next: (res) => {
        console.log('Pré-registro feito! Aguardando verificação.', res);
        
      this.showVerificationStep = true;  
      this.isLoading = false; 

      this.iniciarTimer();
      this.cd.detectChanges();
      },
      error: (err) => {
        const msg = err.error?.message || 'Erro ao criar conta.';
        alert(msg); 
        this.isLoading = false;
      }
    });
  }

  // 👇 NOVA FUNÇÃO: CHAMADA QUANDO CLICA EM "VALIDAR CÓDIGO"
  onVerifyCode() {
    if (this.verificationCode.length < 4) {
      alert('Código inválido.');
      return;
    }

    const payload = {
        email: this.registerData.email,
        codigo: this.verificationCode
    };

    // Você precisará criar esse método 'verifyEmail' no seu AuthService
    this.authService.verifyEmail(payload).subscribe({
        next: (res) => {
            alert('Conta verificada com sucesso!');
            
            // Agora sim fazemos o Login Automático
            const loginPayload = { 
                email: this.registerData.email, 
                senha: this.registerData.senha 
            };
            
            this.authService.login(loginPayload).subscribe({
                 next: () => {
                     document.getElementById('closeRegisterModal')?.click();
                     this.router.navigate(['/dashboard']);
                     // Reseta o estado para a próxima vez
                     this.showVerificationStep = false; 
                     this.verificationCode = '';
                 }
            });
        },
        error: (err) => {
            alert('Código incorreto ou expirado!');
        }
    });
  } 

  // Inicia ou Reinicia o Relógio
iniciarTimer() {
  this.timerValue = 20; // Reseta para 2 minutos
  this.canResend = false; // Trava o botão
  
  // Limpa timer anterior para não encavalar
  if (this.timerInterval) clearInterval(this.timerInterval);

  this.timerInterval = setInterval(() => {
    if (this.timerValue > 0) {
      this.timerValue--; 
      this.cd.detectChanges();
    } else {
      // Tempo acabou! Libera o botão
      this.canResend = true;
      clearInterval(this.timerInterval);
    }
  }, 1000);
}

// Formata os segundos para "01:59" (Bonito na tela)
get tempoFormatado() {
  const min = Math.floor(this.timerValue / 60);
  const sec = this.timerValue % 60;
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
}

// Ação do botão "Reenviar"
onReenviar() {
  if (!this.canResend) return; // Segurança extra

  this.isLoading = true; // Trava para não clicar mil vezes
  
  this.authService.resendCode(this.registerData.email).subscribe({
    next: (res) => {
      alert('Novo código enviado com sucesso!');
      this.iniciarTimer(); // Reinicia a contagem
      this.isLoading = false;
      this.cd.detectChanges();
    },
    error: (err) => {
      alert('Erro ao reenviar email.');
      this.isLoading = false;
      this.cd.detectChanges();
    }
  });
} 

onFileSelected(event: any) {
    const file: File = event.target.files[0];

    if (file) {
        if (file.size > 5 * 1024 * 1024) { // Limite de 5MB
            alert('A imagem deve ter no máximo 5MB');
            return;
        }

        // Chama o envio
        this.enviarFotoParaService(file);
    }
}

// 2. Método que chama o Service
enviarFotoParaService(file: File) {
    // Usa o authService que já está injetado no construtor
    this.userService.uploadFoto(file).subscribe({
        next: (res) => {
            // SUCESSO: Atualiza o preview na tela
            const reader = new FileReader();
            reader.onload = (e: any) => {
                this.fotoPerfilUrl = e.target.result; 
                this.cd.detectChanges();
            };
            reader.readAsDataURL(file);

            alert('Foto atualizada com sucesso!');
        },
        error: (err) => {
            console.error(err);
            alert('Erro ao enviar a foto. Tente novamente.');
        }
    });
  }  

  deletarPerfil(){
    // 1. Pergunta de Segurança (Evita cliques acidentais)
    const confirmacao = confirm("Tem certeza que deseja excluir sua conta permanentemente? Essa ação não pode ser desfeita.");

    if (confirmacao) {
        this.userService.deleteUser().subscribe({
            next: (res) => {
                console.log('Sucesso:', res);
                alert('Sua conta foi excluída com sucesso.');

                // 2. Fecha o modal
                document.getElementById('closeSettingsModal')?.click();

                // 3. 👇 O PULO DO GATO: Faz Logout imediato!
                this.logout(); 
            },
            error: (err) => {
                console.error('Erro ao deletar:', err);
                const msg = err.error?.message || 'Erro ao excluir usuário.';
                alert(msg);
            }
        });
    }
      
  }

}


