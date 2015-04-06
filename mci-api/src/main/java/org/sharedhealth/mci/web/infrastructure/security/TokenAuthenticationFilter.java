package org.sharedhealth.mci.web.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.sharedhealth.mci.utils.HttpUtil.*;
import static org.springframework.util.StringUtils.isEmpty;


public class TokenAuthenticationFilter extends OncePerRequestFilter {
    private AuthenticationManager authenticationManager;
    private final static Logger logger = LoggerFactory.getLogger(TokenAuthenticationFilter.class);

    public TokenAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws ServletException, IOException {
        String token = httpRequest.getHeader(AUTH_TOKEN_KEY);
        String clientId = httpRequest.getHeader(CLIENT_ID_KEY);
        String email = httpRequest.getHeader(FROM_KEY);
        if (isEmpty(token) || isEmpty(clientId) || isEmpty(email)) {
            httpResponse.sendError(SC_UNAUTHORIZED, "Headers are incomplete");
            return;
        }

        logger.debug("Authenticating for client : {} with token: {} and email : {}", clientId, token, email);
        try {
            SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
            processTokenAuthentication(clientId, email, token);
            filterChain.doFilter(httpRequest, httpResponse);

        } catch (AuthenticationException ex) {
            logger.error(String.format("Access to user=%s with email=%s is denied.", clientId, email));
            SecurityContextHolder.clearContext();
            httpResponse.sendError(SC_UNAUTHORIZED, ex.getMessage());
        }
    }

    private void processTokenAuthentication(String clientId, String email, String token) throws AuthenticationException {
        UserAuthInfo userAuthInfo = new UserAuthInfo(clientId, email);
        Authentication authentication = authenticationManager.authenticate(new PreAuthenticatedAuthenticationToken(userAuthInfo, token));

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Unable to authenticate user");
        }
        logger.debug("User successfully authenticated");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
