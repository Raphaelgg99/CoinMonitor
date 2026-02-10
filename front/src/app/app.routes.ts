import { Routes } from '@angular/router'; 
import { HomeComponent } from './pages/home/home'; 
import { DashboardComponent } from './pages/dashboard/dashboard'; 
import { authGuard } from './guards/auth-guard'; // <--- Importe o guarda aqui

export const routes: Routes = [
    // Quando o caminho for vazio (site raiz), abre a Home
    { path: '', component: HomeComponent },
    { 
    path: 'dashboard', component: DashboardComponent, canActivate: [authGuard]},
    { path: '**', redirectTo: '' }
];
