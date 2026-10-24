package com.achernar.identity.auth;
import com.achernar.identity.users.UserDto;
import java.util.UUID;
public interface AuthService {
    void register(String name,String email,String password);
    UserDto login(String email,String password);
    void requestLink(String email,String kind);
    void consume(String token,String kind,String password);
    void logout(UUID id);
    void sendLink(UUID id,String kind);
}
