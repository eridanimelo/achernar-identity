package com.achernar.identity.users;

import com.achernar.identity.shared.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class UserAccessService {
    private final UserRepository users;

    public UserAccessService(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockAdmin(UUID actor) {
        users.adminLock();
        var user = users.lock(actor)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Administração não permitida."));
        if (!user.status().equals("ACTIVE") || !user.verified() || !user.profiles().contains("ADMIN"))
            throw new ApiException(HttpStatus.FORBIDDEN, "Administração não permitida.");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void revokeProfile(UUID id) {
        users.revokeProfile(id);
    }
}
