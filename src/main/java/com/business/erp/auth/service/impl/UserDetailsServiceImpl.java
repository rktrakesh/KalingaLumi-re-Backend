package com.business.erp.auth.service.impl;

import com.business.erp.auth.service.LoginIdentifierService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LoginIdentifierService loginIdentifierService;
    private final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("UserDetailsServiceImpl:loadUserByUsername :: resolving login identifier");
        try {
            return loginIdentifierService.loadForAuthentication(username);
        } catch (UsernameNotFoundException exception) {
            log.warn("UserDetailsServiceImpl:loadUserByUsername :: invalid or ambiguous identifier");
            throw exception;
        }
    }
}
