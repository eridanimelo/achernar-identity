package com.achernar.identity.account;

import com.achernar.identity.users.*;
import com.achernar.identity.shared.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AccountServiceImpl implements AccountService {
    private final UserRepository users;
    private final Audit audit;

    public AccountServiceImpl(UserRepository users, Audit audit) {
        this.users = users;
        this.audit = audit;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public UserDto me(UUID id) {
        return UserDto.from(
                users.byId(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Conta não encontrada.")));
    }

    @Override
    @Transactional
    public UserDto rename(UUID id, String name) {
        var user = users.lock(id).orElseThrow();
        if (!user.status().equals("ACTIVE"))
            throw new ApiException(HttpStatus.FORBIDDEN, "Conta indisponível.");
        users.rename(id, UserNames.normalize(name));
        audit.record(id, id, "RENAME");
        return UserDto.from(users.byId(id).orElseThrow());
    }

}
