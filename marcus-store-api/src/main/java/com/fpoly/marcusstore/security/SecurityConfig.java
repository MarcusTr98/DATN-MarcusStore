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
import org.springframework.http.HttpMethod;
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
                        // Marcus sửa: chỉ dữ liệu catalog thực sự công khai mới được anonymous
                        // truy cập. Profile, sổ địa chỉ, voucher cá nhân và tính phí GHN nằm
                        // dưới /api/client nhưng vẫn phải có JWT hợp lệ.
                        .requestMatchers(HttpMethod.GET, "/api/client/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/client/categories/**").permitAll()
                        .requestMatchers("/api/client/**").authenticated()
                        .requestMatchers("/api/home/**").permitAll()
                        .requestMatchers("/api/admin/user/verify-email").permitAll() // Khách hàng nhập OTP xác thực
                                                                                     // email
                        // Marcus sửa: chỉ IPN được VNPAY gọi từ bên ngoài. Không mở rộng toàn
                        // bộ namespace để tránh endpoint mới vô tình trở thành public.
                        .requestMatchers(HttpMethod.GET, "/api/vnpay/ipn").permitAll()

                        // Marcus sửa: webhook GHN public ở tầng network nhưng controller bắt
                        // buộc kiểm tra X-Verification-Token. Endpoint GHN khác không được mở.
                        .requestMatchers(HttpMethod.POST, "/api/ghn/webhook").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()
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
