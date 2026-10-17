package com.eridani.achernar.identity.social;

/** Cliente OAuth do provedor (o backend do Identity é o cliente confidencial — social-login.md §1). */
public interface ProviderClient {

    Provider provider();

    /** Habilitado globalmente (credenciais presentes). */
    boolean configured();

    /**
     * @param callbackUrl URL de retorno registrada no provedor
     * @param forceReauth {@code prompt=login}/{@code auth_type=reauthenticate}
     * @param selectAccount {@code prompt=select_account} (LOGIN/LINK; apenas Google)
     */
    String authorizationUrl(String callbackUrl, String state, String nonce, String codeChallenge,
                            boolean forceReauth, boolean selectAccount);

    /** Troca o código (servidor→servidor, segredo + PKCE), valida e devolve a identidade; descarta os tokens. */
    ProviderIdentity fetchIdentity(String code, String codeVerifier, String nonce, String callbackUrl);
}
