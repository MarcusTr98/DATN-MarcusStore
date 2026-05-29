package com.fpoly.marcusstore.security;

import com.fpoly.marcusstore.entity.auth.User;
import com.fpoly.marcusstore.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndIsActiveTrue(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException("Không tìm thấy user hoặc tài khoản bị khóa: " + username));

        return CustomUserDetails.build(user);
    }
}