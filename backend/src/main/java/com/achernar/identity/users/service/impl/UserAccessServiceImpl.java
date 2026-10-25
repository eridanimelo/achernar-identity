package com.achernar.identity.users.service.impl;

import com.achernar.identity.shared.exception.ApiException;
import com.achernar.identity.users.entity.UserEntity;
import com.achernar.identity.users.exception.UserNotFoundException;
import com.achernar.identity.users.repository.UserRepository;
import com.achernar.identity.users.service.UserAccessService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAccessServiceImpl implements UserAccessService {
    private final UserRepository users;

    public UserAccessServiceImpl(UserRepository users) {
        this.users = users;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void lockAdmin(UUID actor) {
        users.adminLock();
        var user = users.lock(actor)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Administração não permitida."));
        if (!user.status().equals("ACTIVE") || !user.verified() || !user.profiles().contains("ADMIN"))
            throw new ApiException(HttpStatus.FORBIDDEN, "Administração não permitida.");
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void revokeProfile(UUID id) {
        users.revokeProfile(id);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public UserEntity lockTarget(UUID actor, UUID target) {
        if (actor.equals(target))
            throw new ApiException(HttpStatus.CONFLICT, "Você não pode alterar seu próprio acesso pela administração.");
        return users.lock(target).filter(u -> !u.status().equals("DELETED"))
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado."));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public void protect(UserEntity user, List<String> codes, String status, boolean newEmail) {
        if (user.profiles().contains("ADMIN") && user.status().equals("ACTIVE") && user.verified()
                && (!codes.contains("ADMIN") || !status.equals("ACTIVE") || newEmail) && users.activeAdmins() <= 1)
            throw new ApiException(HttpStatus.CONFLICT, "É necessário manter um administrador ativo e confirmado.");
    }
}
