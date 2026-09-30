package com.crimesphere.backend.security;

import com.crimesphere.backend.entity.PoliceOfficer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class PoliceUserDetails implements UserDetails {

    private final PoliceOfficer officer;

    public PoliceUserDetails(PoliceOfficer officer) {
        this.officer = officer;
    }

    public PoliceOfficer getOfficer() {
        return officer;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = officer.getRole() != null ? officer.getRole() : "OFFICER";
        if (!role.startsWith("ROLE_")) {
            role = "ROLE_" + role;
        }
        return Collections.singletonList(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return officer.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return officer.getPoliceId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(officer.getIsActive());
    }
}
