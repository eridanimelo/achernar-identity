package com.eridani.achernar.identity.social;

import com.eridani.achernar.identity.security.CurrentUser;
import com.eridani.achernar.identity.shared.error.ApiException;
import com.eridani.achernar.identity.shared.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** FT-08: API-FT08-01…03. */
@RestController
public class SocialLinkController {

    public record IntentRequest(@JsonProperty("return_url") String returnUrl, @JsonProperty("state") String state) {
    }

    private final SocialLinkService service;

    public SocialLinkController(SocialLinkService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/account/social-links/{provider}/intent")
    public Map<String, Object> linkIntent(@PathVariable("provider") String slug, @RequestBody IntentRequest body) {
        return service.intent(CurrentUser.require(), provider(slug), body.returnUrl(), body.state(),
                SocialTransaction.Purpose.LINK);
    }

    @PostMapping("/api/v1/account/reauth/social/{provider}/intent")
    public Map<String, Object> reauthIntent(@PathVariable("provider") String slug, @RequestBody IntentRequest body) {
        return service.intent(CurrentUser.require(), provider(slug), body.returnUrl(), body.state(),
                SocialTransaction.Purpose.REAUTH);
    }

    @DeleteMapping("/api/v1/account/social-links/{provider}")
    public ResponseEntity<Void> unlink(@PathVariable("provider") String slug) {
        service.unlink(CurrentUser.require(), provider(slug));
        return ResponseEntity.noContent().build();
    }

    private static Provider provider(String slug) {
        return Provider.fromSlug(slug).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND));
    }
}
