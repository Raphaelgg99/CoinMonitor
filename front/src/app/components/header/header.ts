import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { SocialAuthService, GoogleSigninButtonModule } from '@abacritt/angularx-social-login';
import { UserService } from '../../services/user';

declare var bootstrap: any; //

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, FormsModule, GoogleSigninButtonModule, RouterModule],
  templateUrl: './header.html',
  styleUrls: ['./header.css']
})
export class HeaderComponent implements OnInit {


  loginData = { email: '', senha: '' };
  showPassword = false;
  registerData = { name: '', email: '', senha: '', confirmPassword: '' };

  userData = { nome: '', email: '', isGoogleAccount: false };

  editandoSenha = false;
  novaSenha = '';
  confirmNovaSenha = '';

  showNovaSenha = false;
  showConfirmSenha = false;

  editandoNome = false;
  nomeBackup = '';

  showVerificationStep = false;
  verificationCode = '';

  isLoading = false

  timerValue: number = 120;
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


  ehPaginaDashboard(): boolean {
    return this.router.url.includes('/dashboard');
  }

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  toggleEditarNome() {
    this.editandoNome = !this.editandoNome;

    if (this.editandoNome) {
      this.nomeBackup = this.userData.nome;
    } else {
      this.userData.nome = this.nomeBackup;
    }
  }

 toggleEditarSenha() {
    this.editandoSenha = !this.editandoSenha;
    if (!this.editandoSenha) {
      this.novaSenha = '';
      this.confirmNovaSenha = '';
      this.showNovaSenha = false;
      this.showConfirmSenha = false;
    }
  }

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

    const dadosParaAtualizar = {
        nome: this.userData.nome,
        email: this.userData.email,
        senha: this.editandoSenha ? this.novaSenha : null
    };

    console.log('Enviando atualização:', dadosParaAtualizar);
    this.userService.update(dadosParaAtualizar).subscribe({
        next: (res) => {
            console.log('Sucesso:', res);
            alert('Perfil atualizado com sucesso!');
            document.getElementById('closeSettingsModal')?.click();
            if (this.editandoSenha) this.toggleEditarSenha();
            if (this.editandoNome) this.editandoNome = false;
            localStorage.setItem('username', this.userData.nome);
        },
        error: (err) => {
            console.error('Erro ao atualizar:', err);
            const msg = err.error?.message || 'Erro ao atualizar dados.';
            alert(msg);
        }
    });
  }

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
    this.isLoading = true;
      this.authService.login(this.loginData).subscribe({
        next: (res) => {
            this.isLoading = false;
            document.getElementById('closeLoginModal')?.click();
            this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          const mensagemErro = err.error?.message || 'Erro ao fazer login';

          if (mensagemErro.includes("Seu email ainda não foi verificado")) {

              alert(mensagemErro);
              document.getElementById('closeLoginModal')?.click();

              this.registerData.email = this.loginData.email;
              this.showVerificationStep = true;
              this.verificationCode = '';

              setTimeout(() => {
                  const registerModal = new bootstrap.Modal(document.getElementById('registerModal')!);
                  registerModal.show();

                  this.iniciarTimer();
              }, 500);

          } else {
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

  onVerifyCode() {
    if (this.verificationCode.length < 4) {
      alert('Código inválido.');
      return;
    }

    const payload = {
        email: this.registerData.email,
        codigo: this.verificationCode
    };
    this.authService.verifyEmail(payload).subscribe({
        next: (res) => {
            alert('Conta verificada com sucesso!');

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

iniciarTimer() {
  this.timerValue = 20;
  this.canResend = false;

  if (this.timerInterval) clearInterval(this.timerInterval);

  this.timerInterval = setInterval(() => {
    if (this.timerValue > 0) {
      this.timerValue--;
      this.cd.detectChanges();
    } else {
      this.canResend = true;
      clearInterval(this.timerInterval);
    }
  }, 1000);
}

get tempoFormatado() {
  const min = Math.floor(this.timerValue / 60);
  const sec = this.timerValue % 60;
  return `${min.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
}

onReenviar() {
  if (!this.canResend) return;

  this.isLoading = true;

  this.authService.resendCode(this.registerData.email).subscribe({
    next: (res) => {
      alert('Novo código enviado com sucesso!');
      this.iniciarTimer();
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
        if (file.size > 5 * 1024 * 1024) {
            alert('A imagem deve ter no máximo 5MB');
            return;
        }

        this.enviarFotoParaService(file);
    }
}

enviarFotoParaService(file: File) {
    this.userService.uploadFoto(file).subscribe({
        next: (res) => {
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
    const confirmacao = confirm("Tem certeza que deseja excluir sua conta permanentemente? Essa ação não pode ser desfeita.");

    if (confirmacao) {
        this.userService.deleteUser().subscribe({
            next: (res) => {
                console.log('Sucesso:', res);
                alert('Sua conta foi excluída com sucesso.');

                document.getElementById('closeSettingsModal')?.click();

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


