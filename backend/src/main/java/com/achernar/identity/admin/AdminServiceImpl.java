package com.achernar.identity.admin;
import com.achernar.identity.users.*;
import com.achernar.identity.auth.*;
import com.achernar.identity.profiles.ProfileService;
import com.achernar.identity.shared.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service
public class AdminServiceImpl implements AdminService {
    private final UserRepository users;
    private final ActionRepository actions;
    private final AuthService auth;
    private final ProfileService profiles;
    private final UserAccessService access;
    private final Audit audit;
    public AdminServiceImpl(UserRepository users,ActionRepository actions,AuthService auth,ProfileService profiles,UserAccessService access,Audit audit){this.users=users;this.actions=actions;this.auth=auth;this.profiles=profiles;this.access=access;this.audit=audit;}
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public Page list(String query,int page,int size){long total=users.count(query);return new Page(users.list(query,page,size),page,size,total,(total+size-1)/size);}
    @Override @Transactional(propagation=Propagation.NOT_SUPPORTED)
    public UserDto get(UUID id){return UserDto.from(users.byId(id).filter(u->!u.status().equals("DELETED")).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Usuário não encontrado.")));}
    @Override @Transactional
    public UserDto create(UUID actor,String name,String email,List<String> codes){
        access.lockAdmin(actor);var ids=profiles.resolve(codes);UUID id=UUID.randomUUID();
        if(!users.create(id,UserNames.normalize(name),AuthServiceImpl.normalize(email),"invited","USER",false))throw new ApiException(HttpStatus.CONFLICT,"E-mail já cadastrado.");
        users.assign(id,ids);auth.sendLink(id,"INVITE");audit.record(actor,id,"ADMIN_CREATE");return UserDto.from(users.byId(id).orElseThrow());
    }
    @Override @Transactional
    public UserDto change(UUID actor,UUID target,String role,String status){return editInternal(actor,target,null,null,null,role,status);}
    @Override @Transactional
    public UserDto edit(UUID actor,UUID target,String name,String email,List<String> codes,String role,String status){return editInternal(actor,target,name,email,codes,role,status);}
    private UserDto editInternal(UUID actor,UUID target,String name,String email,List<String> codes,String role,String status){
        access.lockAdmin(actor);var user=target(actor,target);
        if(role!=null && codes!=null)throw new ApiException(HttpStatus.BAD_REQUEST,"Use profiles ou role legado, nunca ambos.");
        List<String> nextCodes=codes!=null?codes:role!=null?List.of(role):null;
        var ids=nextCodes==null?null:profiles.resolve(nextCodes);
        String nextStatus=status==null?user.status():status;
        String nextEmail=email==null?user.email():AuthServiceImpl.normalize(email);
        boolean emailChanged=!nextEmail.equals(user.email());
        protect(user,nextCodes==null?user.profiles():nextCodes,nextStatus,emailChanged);
        if(emailChanged && users.byEmail(nextEmail).isPresent())throw new ApiException(HttpStatus.CONFLICT,"E-mail já cadastrado.");
        if(ids!=null)users.assign(target,ids);
        try{users.update(target,name==null?user.name():UserNames.normalize(name),nextEmail,nextStatus,user.verified() && !emailChanged);}catch(org.springframework.dao.DuplicateKeyException e){throw new ApiException(HttpStatus.CONFLICT,"E-mail já cadastrado.");}
        if(emailChanged){actions.remove(target);auth.sendLink(target,user.passwordHash().equals("invited")?"INVITE":"VERIFY");}
        audit.record(actor,target,"ADMIN_CHANGE");return UserDto.from(users.byId(target).orElseThrow());
    }
    @Override @Transactional
    public void delete(UUID actor,UUID target){access.lockAdmin(actor);var user=target(actor,target);protect(user,user.profiles(),"DELETED",false);users.delete(target);actions.remove(target);audit.record(actor,target,"ADMIN_DELETE");}
    private UserEntity target(UUID actor,UUID target){
        if(actor.equals(target))throw new ApiException(HttpStatus.CONFLICT,"Você não pode alterar seu próprio acesso pela administração.");
        return users.lock(target).filter(u->!u.status().equals("DELETED")).orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Usuário não encontrado."));
    }
    private void protect(UserEntity user,List<String> codes,String status,boolean newEmail){
        if(user.profiles().contains("ADMIN") && user.status().equals("ACTIVE") && user.verified() && (!codes.contains("ADMIN") || !status.equals("ACTIVE") || newEmail) && users.activeAdmins()<=1)
            throw new ApiException(HttpStatus.CONFLICT,"É necessário manter um administrador ativo e confirmado.");
    }
}
