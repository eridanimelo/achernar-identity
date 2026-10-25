package com.achernar.identity.profiles.service;

import com.achernar.identity.profiles.domain.model.ProfileView;
import com.achernar.identity.profiles.domain.repository.ProfileRepository;
import com.achernar.identity.shared.exception.ApiException;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileResolver {
    private final ProfileRepository profiles;

    public ProfileResolver(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<UUID> resolve(List<String> codes) {
        if (codes == null || codes.isEmpty() || codes.size() > 20 || new HashSet<>(codes).size() != codes.size())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Selecione de 1 a 20 perfis, sem repetição.");
        List<ProfileView> found = profiles.resolve(codes);
        if (found.size() != codes.size())
            throw new ApiException(HttpStatus.BAD_REQUEST, "Todos os perfis devem existir e estar ativos.");
        return found.stream().map(ProfileView::id).toList();
    }
}
