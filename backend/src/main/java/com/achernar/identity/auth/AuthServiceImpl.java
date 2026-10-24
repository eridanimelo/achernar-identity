package com.achernar.identity.auth;
import com.achernar.identity.users.*;
import com.achernar.identity.mail.MailQueue;
import com.achernar.identity.shared.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final ActionRepository actions;
    private final MailQueue mail;
    private final PasswordEncoder passwords;
    private final Audit audit;
    private final String actionUrl;
    private final String dummy;
    public AuthServiceImpl(UserRepository users,ActionRepository actions,MailQueue mail,PasswordEncoder passwords,Audit audit,@Value("${app.action-url}")String actionUrl) {
        this.users=users;this.actions=actions;this.mail=mail;this.passwords=passwords;this.audit=audit;this.actionUrl=actionUrl;
        dummy=passwords.encode(UUID.randomUUID().toString());
    }
    public static String normalize(String email) {return email.strip().toLowerCase(Locale.ROOT);}
    public static String hash(String token) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException e) {throw new IllegalStateException(e);}
    }
    @Override @Transactional
    public void register(String name,String email,String password) {
        UUID id=UUID.randomUUID();
        if(users.create(id,UserNames.normalize(name),normalize(email),passwords.encode(password),"USER",false)) {
            send(users.byId(id).orElseThrow(),"VERIFY");audit.record(id,id,"REGISTER");
        }
    }
    @Override @Transactional
    public UserDto login(String email,String password) {
        var found=users.byEmail(normalize(email));
        String hash=found.filter(u->!u.passwordHash().equals("invited")).map(UserEntity::passwordHash).orElse(dummy);
        boolean match=passwords.matches(password,hash);
        if(!match || found.isEmpty()) throw new ApiException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos.");
        var user=users.lock(found.get().id()).orElseThrow();
        if(!user.status().equals("ACTIVE") || !user.passwordHash().equals(hash)) throw new ApiException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos.");
        if(!user.verified()) throw new ApiException(HttpStatus.FORBIDDEN,"Confirme seu e-mail antes de entrar.");
        if(user.profiles().isEmpty())throw new ApiException(HttpStatus.FORBIDDEN,"Sua conta não possui perfil ativo. Procure um administrador.");
        audit.record(user.id(),user.id(),"LOGIN");return UserDto.from(user);
    }
    @Override @Transactional
    public void requestLink(String email,String kind) {
        users.byEmail(normalize(email)).ifPresent(found -> {
            var user=users.lock(found.id()).orElseThrow();
            if(user.status().equals("ACTIVE") && (kind.equals("RESET")?user.verified():!user.verified())) send(user,kind.equals("VERIFY") && user.passwordHash().equals("invited")?"INVITE":kind);
        });
    }
    @Override @Transactional(propagation=Propagation.MANDATORY)
    public void sendLink(UUID id,String kind){send(users.byId(id).orElseThrow(),kind);}
    private void send(UserEntity user,String kind) {
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        actions.invalidate(user.id(),kind);
        actions.create(user.id(),hash(token),kind,Instant.now().plusSeconds(kind.equals("RESET")?1800:86400));
        String path=kind.equals("RESET")?"/auth/reset-password":kind.equals("INVITE")?"/auth/activate":"/auth/verify";
        mail.enqueue(user.email(),kind.equals("RESET")?"Redefina sua senha — Achernar":kind.equals("INVITE")?"Ative sua conta — Achernar":"Confirme seu e-mail — Achernar",
            "Olá, "+user.name()+".\n\nAbra o link e confirme a operação:\n"+actionUrl+path+"#token="+token+"\n\nSe você não solicitou, ignore esta mensagem.");
    }
    @Override @Transactional
    public void consume(String token,String kind,String password) {
        var action=actions.find(hash(token),kind).orElseThrow(AuthServiceImpl::invalidLink);
        var user=users.lock(action.userId()).orElseThrow(AuthServiceImpl::invalidLink);
        if(!user.status().equals("ACTIVE") || !actions.consume(action.id())) throw invalidLink();
        if(kind.equals("RESET") || kind.equals("INVITE")) {
            users.reset(user.id(),passwords.encode(password));actions.invalidate(user.id(),"RESET");
            if(kind.equals("INVITE"))users.verify(user.id());
        } else users.verify(user.id());
        audit.record(user.id(),user.id(),kind.equals("RESET")?"RESET_PASSWORD":kind.equals("INVITE")?"ACTIVATE_ACCOUNT":"VERIFY_EMAIL");
    }
    @Override @Transactional
    public void logout(UUID id) {users.revoke(id);audit.record(id,id,"LOGOUT");}
    private static ApiException invalidLink() {return new ApiException(HttpStatus.BAD_REQUEST,"Link inválido, expirado ou já utilizado. Solicite um novo.");}
}
