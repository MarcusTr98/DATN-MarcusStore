package com.fpoly.marcusstore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.fpoly.marcusstore.repository.auth.UserRepository;

@SpringBootApplication
public class MarcusStoreApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MarcusStoreApiApplication.class, args);
	}

	// @Bean
	// public CommandLineRunner initData(UserRepository userRepository,
	// PasswordEncoder passwordEncoder) {
	// return args -> {
	// // 3 tài khoản đã seeding
	// java.util.List<String> usernames = java.util.Arrays.asList("admin", "staff1",
	// "khach1");

	// for (String username : usernames) {
	// userRepository.findByUsername(username).ifPresent(user -> {
	// // Mã hóa và lưu đè mật khẩu
	// user.setPasswordHash(passwordEncoder.encode("123456"));
	// userRepository.save(user);
	// System.out.println("Đã cập nhật mật khẩu chuẩn BCrypt cho 3 tài khoản
	// seeding");
	// });
	// }
	// };
	// }
}
