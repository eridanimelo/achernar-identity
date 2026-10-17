export interface SessionItem {
  id: string;
  client_id: string;
  application_name: string;
  created_at: string;
  last_used_at: string;
  ip_approx: string | null;
  user_agent_summary: string | null;
  amr: string;
  current: boolean;
}

export interface ChangePasswordBody {
  current_password?: string;
  new_password: string;
}
