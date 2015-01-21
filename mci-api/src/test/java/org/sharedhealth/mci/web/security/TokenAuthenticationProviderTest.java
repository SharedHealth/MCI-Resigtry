package org.sharedhealth.mci.web.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class TokenAuthenticationProviderTest {

    @Mock
    IdentityServiceClient identityServiceClient;

    @Mock
    Authentication authentication;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldAuthenticateAgainstIdentityService() throws Exception {
        UUID token = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(token.toString());
        when(identityServiceClient.authenticate(token.toString())).thenReturn(tokenAuthentication(token));
        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider
                (identityServiceClient);
        Authentication tokenAuthentication = authenticationProvider.authenticate(authentication);
        assertEquals("foo", tokenAuthentication.getName());
        assertEquals(token.toString(), tokenAuthentication.getPrincipal().toString());
        assertTrue(tokenAuthentication.getAuthorities().contains(new SimpleGrantedAuthority("MCI_USER")));
        assertTrue(tokenAuthentication.getAuthorities().contains(new SimpleGrantedAuthority("SHR_USER")));
        assertTrue(tokenAuthentication.isAuthenticated());
    }

    @Test(expected = BadCredentialsException.class)
    public void shouldRespond401OnException() throws Exception {
        UUID token = UUID.randomUUID();
        when(authentication.getPrincipal()).thenReturn(token.toString());
        when(identityServiceClient.authenticate(token.toString())).thenThrow(new BadCredentialsException("bar"));
        TokenAuthenticationProvider authenticationProvider = new TokenAuthenticationProvider
                (identityServiceClient);
        authenticationProvider.authenticate(authentication);
    }

    private TokenAuthentication tokenAuthentication(UUID token) {
        return new TokenAuthentication(new UserInfo("foo", Arrays.asList("SHR_USER", "MCI_USER")), token.toString());
    }
}