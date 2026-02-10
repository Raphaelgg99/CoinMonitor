export interface UsuarioResponseDTO{
    id: string, 
    nome: string, 
    email: string, 
    carteira: MoedaDTO[], 
    isGoogleAccount: boolean;
} 
export interface MoedaDTO{
    coinId: string, 
    quantidade: number
}