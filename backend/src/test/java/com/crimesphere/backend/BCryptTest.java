package com.crimesphere.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BCryptTest {
    @Test
    void testBCrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String generatedHash = encoder.encode("Demo@123");
        System.out.println("Generated BCrypt Hash for Demo@123: " + generatedHash);
        assertTrue(encoder.matches("Demo@123", generatedHash));

        // Check the hash from V5 migration
        String v5Hash = "$2a$10$vI8aWBnW3fID.ZQ4/zo1G.qH5rI2uB3.m3z6v7e8r9t0y1u2i3o4p";
        boolean v5Matches = false;
        try {
            v5Matches = encoder.matches("Demo@123", v5Hash);
        } catch (Exception e) {
            System.out.println("v5Hash threw exception: " + e.getMessage());
        }
        System.out.println("v5Hash matches Demo@123: " + v5Matches);
    }
}
