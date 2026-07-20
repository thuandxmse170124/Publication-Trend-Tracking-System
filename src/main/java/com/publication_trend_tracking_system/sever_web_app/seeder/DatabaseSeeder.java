package com.publication_trend_tracking_system.sever_web_app.seeder;

import com.publication_trend_tracking_system.sever_web_app.entity.ApiSource;
import com.publication_trend_tracking_system.sever_web_app.entity.Role;
import com.publication_trend_tracking_system.sever_web_app.entity.User;
import com.publication_trend_tracking_system.sever_web_app.repository.ApiSourceRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.PaperRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.RoleRepository;
import com.publication_trend_tracking_system.sever_web_app.repository.UserRepository;
import com.publication_trend_tracking_system.sever_web_app.service.SyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.publication_trend_tracking_system.sever_web_app.enums.UserStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ApiSourceRepository apiSourceRepository;
    private final PaperRepository paperRepository;
    private final SyncService syncService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Checking database for essential seed data...");

        // 1. Seed Roles
        if (roleRepository.count() == 0) {
            log.info("Seeding roles...");
            Role adminRole = new Role();
            adminRole.setRoleName("ADMIN");
            roleRepository.save(adminRole);

            Role memberRole = new Role();
            memberRole.setRoleName("MEMBER");
            roleRepository.save(memberRole);
        }

        // 2. Seed Admin User
        if (userRepository.count() == 0) {
            log.info("Seeding default admin user...");
            Role adminRole = roleRepository.findByRoleName("ADMIN")
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
            
            User adminUser = new User();
            adminUser.setRole(adminRole);
            adminUser.setFullName("System Admin");
            adminUser.setEmail("admin@gmail.com");
            // Default password: 123 (Needs to match your standard encrypted password)
            adminUser.setPasswordHash(passwordEncoder.encode("123"));
            adminUser.setStatus(UserStatus.ACTIVE);
            adminUser.setCreatedAt(LocalDateTime.now());
            adminUser.setUpdatedAt(LocalDateTime.now());
            userRepository.save(adminUser);
        }

        // Force reset password for testing
        User existingAdmin = userRepository.findByEmail("admin@gmail.com").orElse(null);
        if (existingAdmin != null) {
            existingAdmin.setPasswordHash(passwordEncoder.encode("123456Aa@"));
            userRepository.save(existingAdmin);
            log.info("Successfully forced password reset for admin@gmail.com to 123456Aa@");
        }

        // 3. Seed API Source (OpenAlex)
        if (apiSourceRepository.count() == 0) {
            log.info("Seeding OpenAlex API Source...");
            ApiSource openAlex = new ApiSource();
            openAlex.setSourceName("OpenAlex");
            openAlex.setBaseUrl("https://api.openalex.org");
            openAlex.setStatus("ACTIVE");
            apiSourceRepository.save(openAlex);
        }

        // 4. (Removed Massive Sync logic)


        log.info("Database seed checks completed.");
    }
}
