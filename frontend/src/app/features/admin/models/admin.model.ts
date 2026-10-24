import { IdentityUser } from '../../auth/models/auth.model';
export type ManagedUser=IdentityUser;
export interface UserChange{name?:string;email?:string;profiles?:string[];role?:'ADMIN'|'USER';status?:'ACTIVE'|'BLOCKED';}
export interface UserDraft{id?:string;name:string;email:string;profiles:string[];}
export interface AdminAction{id:string;change?:UserChange;remove?:boolean;}
