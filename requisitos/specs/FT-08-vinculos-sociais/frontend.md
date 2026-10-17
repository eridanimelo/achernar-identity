# Frontend Spec — FT-08 Vínculos sociais
`NEW` · `APPROVED` (documental) · SCR-17 Vínculos sociais (seção de `/account/security`), SCR-11 (reautenticação com provedor).

SCR-17: por provedor (Google, Facebook) mostra estado “Vinculado em <data>” ou “Não vinculado”. Ações: *Vincular* (reautenticação → intenção → navegação ao provedor → retorno com `result`); *Desvincular* (diálogo de confirmação; explica se for o último método e desabilita com dica “Defina uma senha antes”). Provedores não habilitados na instalação não aparecem.
Retorno: `?result=linked` ⇒ aviso de sucesso e atualização da lista; `?error=already_linked` ⇒ “Esta conta do provedor já está vinculada a outro usuário”; demais erros mapeados como em SCR-16.
SCR-11 com provedor: botão “Confirmar com Google/Facebook” (apenas provedores vinculados); após `result=reauthenticated` repete a ação original.
Acessibilidade: botões com nome do provedor, foco devolvido após o diálogo, estados anunciados.
Rastreabilidade: SCR-17 → CA-FT08-01…03, 06, 07 · SCR-11 → CA-FT08-10…12.
