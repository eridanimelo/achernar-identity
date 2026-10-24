export interface AccessProfile {id:string;code:string;name:string;description:string;active:boolean;system:boolean;userCount:number;createdAt:string;}
export interface ProfileDraft {id?:string;code:string;name:string;description:string;active:boolean;}
export interface ProfileQuery {q:string;active:boolean|null;page:number;}
