package com.crimesphere.backend.config;

import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final PoliceOfficerRepository officerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(PoliceOfficerRepository officerRepository, PasswordEncoder passwordEncoder) {
        this.officerRepository = officerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Ensure development officers have properly formatted BCrypt hashes for 'Demo@123'
        List<PoliceOfficer> officers = officerRepository.findAll();
        for (PoliceOfficer officer : officers) {
            boolean matchesDemo = officer.getPasswordHash() != null &&
                    (passwordEncoder.matches("Demo@123", officer.getPasswordHash()) || passwordEncoder.matches("password123", officer.getPasswordHash()));
            if (!matchesDemo) {
                officer.setPasswordHash(passwordEncoder.encode("Demo@123"));
                officerRepository.save(officer);
                logger.info("Initialized BCrypt password hash for officer: {}", officer.getPoliceId());
            }
        }
    }
}
