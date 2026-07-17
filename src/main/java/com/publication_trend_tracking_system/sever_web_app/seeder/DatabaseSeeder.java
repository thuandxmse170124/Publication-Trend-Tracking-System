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

        // 3. Seed API Source (OpenAlex)
        if (apiSourceRepository.count() == 0) {
            log.info("Seeding OpenAlex API Source...");
            ApiSource openAlex = new ApiSource();
            openAlex.setSourceName("OpenAlex");
            openAlex.setBaseUrl("https://api.openalex.org");
            openAlex.setStatus("ACTIVE");
            apiSourceRepository.save(openAlex);
        }

        // 4. Start Massive Sync if DB has no papers
        if (paperRepository.count() < 10000) {
            log.info("No papers found in DB. Triggering massive data load from OpenAlex (Background Thread)...");
            apiSourceRepository.findAll().stream()
                    .filter(source -> "ACTIVE".equalsIgnoreCase(source.getStatus()) && "OpenAlex".equalsIgnoreCase(source.getSourceName()))
                    .findFirst()
                    .ifPresent(source -> {
                        // Run sync asynchronously so it doesn't block Spring Boot startup
                        CompletableFuture.runAsync(() -> {
                            try {
                                log.info("STARTING MASSIVE PRE-LOAD DATA for source: {}", source.getSourceName());
                                syncService.syncFromSource(source.getSourceId(), null, null, com.publication_trend_tracking_system.sever_web_app.enums.SyncTimeRange.ALL);
                                log.info("MASSIVE PRE-LOAD DATA COMPLETED for source: {}", source.getSourceName());
                            } catch (Exception e) {
                                log.error("Error during massive pre-load data: ", e);
                            }
                        });
                    });
        } else {
            log.info("Database already contains papers. Skipping massive pre-load.");
        }

        log.info("Database seed checks completed.");
    }
}
