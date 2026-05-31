package com.fpoly.marcusstore.security;

import com.fpoly.marcusstore.security.jwt.AuthTokenFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity // Cho phép dùng @PreAuthorize trên Controller
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); // Dùng BCrypt
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configure(http))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. Nhóm API mở tự do (Không cần Token)
                        .requestMatchers("/api/auth/**").permitAll() // Ngọc: Đăng nhập, Đăng ký, Quên MK
                        .requestMatchers("/api/public/**").permitAll() // Đức, Đạt, Huy: Lấy SP Home, Banner, Bài
                                                                       // viết SEO

                        // 2. Nhóm API dành cho Khách hàng đã đăng nhập
                        .requestMatchers("/api/user/**").authenticated() // Đạt, Đức: Checkout, Giỏ hàng, Wishlist, Đgiá

                        // 3. Nhóm API dành riêng cho Quản trị viên (Phân quyền RBAC)
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF") // Ngọc, Huy: Quản lý User,Thống
                                                                                       // kê
                        .requestMatchers("/api/admin/roles/**").hasRole("ADMIN") // Chỉ Admin mới được đổi quyền

                        // Khóa mọi request khác đi lạc
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}