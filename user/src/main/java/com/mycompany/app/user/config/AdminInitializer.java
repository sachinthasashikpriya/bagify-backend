package com.mycompany.app.user.config;

import com.mycompany.app.user.entity.User;
import com.mycompany.app.user.entity.Role;
import com.mycompany.app.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        // Check if the admin already exists
        if (!userRepository.existsByEmail("admin@bagmarket.com")) {

            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@bagmarket.com");
            admin.setPassword(passwordEncoder.encode("Admin@123"));
            admin.setPhone("0000000000");
            admin.setAddress("Head Office");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);

            userRepository.save(admin);

            System.out.println("✅ Default admin created");
        } else {
            System.out.println("ℹ Admin already exists");
        }
    }
}
