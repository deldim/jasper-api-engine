package com.bnpp.app.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.bnpp.app.model.ApiUser;
import com.bnpp.app.repository.UserRepository;
import com.bnpp.app.security.CustomUserDetails;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    	Optional<ApiUser> optionalUser = userRepository.findByUsername(username);
    	if (optionalUser.isPresent()) {
    	    return new CustomUserDetails(optionalUser.get());
    	} else {
    	    throw new UsernameNotFoundException("User not found: " + username);
    	}
    }
}


