package com.fpoly.marcusstore.security;

import com.fpoly.marcusstore.security.jwt.AuthTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
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
import com.fpoly.marcusstore.security.OAuth2SuccessHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AuthTokenFilter authTokenFilter;

    @Autowired
    private OAuth2SuccessHandler oAuth2SuccessHandler;

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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("""
                                    {
                                      "code":401,
                                      "message":"Unauthorized",
                                      "data":null
                                    }
                                    """);
                        })
                        .accessDeniedHandler((request, response, ex) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("""
                                    {
                                      "code":403,
                                      "message":"Access Denied",
                                      "data":null
                                    }
                                    """);
                        }))
                .oauth2Login(oauth -> oauth
                        .successHandler(oAuth2SuccessHandler))
                .authorizeHttpRequests(auth -> auth
                        // 1. Nhóm API mở tự do (Không cần Token)
                        .requestMatchers("/api/auth/**").permitAll() // Đăng nhập, Đăng ký, Quên MK
                        .requestMatchers("/api/ws-endpoint/**", "/ws-endpoint/**").permitAll() // Marcus làm websocket
                        .requestMatchers("/oauth2/**","/login/oauth2/**").permitAll()

                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/client/**").permitAll()
                        .requestMatchers("/api/home/**").permitAll()
                        .requestMatchers("/api/admin/user/verify-email").permitAll() // Khách hàng nhập OTP xác thực
                                                                                     // email
                        .requestMatchers("/api/vnpay/**").permitAll() // Marcus test môi trường Ngrok webhook Vnpay

                        // Mở khóa toàn bộ nhánh GHN và mở endpoint báo lỗi của Spring Boot
                        .requestMatchers("/api/ghn/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // 2. Nhóm API dành cho Khách hàng đã đăng nhập
                        .requestMatchers("/api/user/**").authenticated() // Checkout, Giỏ hàng, Wishlist, Đgiá
                        // Thêm quyền cho nhánh finance-reports
                        .requestMatchers("/api/admin/finance-reports/**").hasAnyRole("ADMIN", "STAFF")
                        // Marcus thêm: dữ liệu toàn hệ thống chỉ ADMIN được tạo và tải.
                        .requestMatchers("/api/admin/backups/**").hasRole("ADMIN")
                        // 3. Nhóm API dành riêng cho Quản trị viên
                        .requestMatchers("/api/admin/roles/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF") // Quản lý User, Thống kê

                        // Khóa mọi request khác đi lạc
                        .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
