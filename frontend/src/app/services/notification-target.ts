import { AppNotification, Role } from '../models/models';

/**
 * Mapeia uma notificacao para a rota de destino apropriada.
 * Considera entityType, type e o role do user efetivo.
 * Retorna null se nao ha alvo (nada pra navegar).
 */
export function notificationTarget(n: AppNotification, role: Role | undefined): string | null {
  if (!n.entityType) return null;

  switch (n.entityType) {
    case 'MenuPlan':
      // Nutri revisa pendentes; cliente vê seus cardápios
      if (n.type === 'MENU_SUBMITTED') return '/nutritionist/pending-menus';
      return role === 'NUTRITIONIST' ? '/nutritionist/pending-menus' : '/menus';

    case 'CompanyNutritionist':
      // Convite ou revoke de empresa — sempre vai pra tela de convites do nutri
      return '/nutritionist/company-invitations';

    case 'NutritionistClientLink':
      // Nutri: gerenciar clientes/pedidos. Cliente: seus convites/dashboard.
      if (role === 'NUTRITIONIST') return '/nutritionist/clients';
      return '/invitations';

    case 'Chat':
      // Fase 9G-1: notificacao de nova mensagem — abre thread direto.
      return n.entityId ? `/chat/${n.entityId}` : null;

    default:
      return null;
  }
}
