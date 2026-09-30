package com.crimesphere.backend.security;

import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PoliceUserDetailsService implements UserDetailsService {

    private final PoliceOfficerRepository policeOfficerRepository;

    public PoliceUserDetailsService(PoliceOfficerRepository policeOfficerRepository) {
        this.policeOfficerRepository = policeOfficerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String policeId) throws UsernameNotFoundException {
        PoliceOfficer officer = policeOfficerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new UsernameNotFoundException("Police Officer not found with ID: " + policeId));
        return new PoliceUserDetails(officer);
    }
}
