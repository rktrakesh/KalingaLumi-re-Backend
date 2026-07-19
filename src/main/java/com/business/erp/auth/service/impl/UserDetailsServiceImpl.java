package com.business.erp.auth.service.impl;

import com.business.erp.auth.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("UserDetailsServiceImpl:loadUserByUsername :: username={}", username);
        return userRepository.findByUsername(username).orElseThrow(() -> {
            log.warn("UserDetailsServiceImpl:loadUserByUsername :: User not found :: username={}", username);
            return new UsernameNotFoundException("User not found: " + username);
        });
    }
}
