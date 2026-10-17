package com.eridani.achernar.identity.account;

import com.eridani.achernar.identity.security.CurrentUser;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API-FT05-01: consultar a própria conta. */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> me() {
        return service.view(CurrentUser.require().accountId());
    }
}
