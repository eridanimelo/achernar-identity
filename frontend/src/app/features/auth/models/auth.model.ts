export interface IdentityUser{id:string;name:string;email:string;role:'USER'|'ADMIN';status:'ACTIVE'|'BLOCKED'|'DELETED';verified:boolean;createdAt:string;profiles:string[];assignedProfiles:string[];}
export type AuthMode='login'|'register'|'forgot-password'|'reset-password'|'verify'|'resend'|'activate';
export interface AuthFormData{name:string;email:string;password:string;}
export interface Message{message:string;}
