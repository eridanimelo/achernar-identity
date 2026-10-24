package com.achernar.identity.users;
import com.achernar.identity.shared.ApiException;
import org.springframework.http.HttpStatus;
public final class UserNames {
    private UserNames() {}
    public static String normalize(String name) {
        String value=name.strip();
        if(value.length()<2 || value.length()>100) throw new ApiException(HttpStatus.BAD_REQUEST,"Nome deve ter entre 2 e 100 caracteres.");
        return value;
    }
}
