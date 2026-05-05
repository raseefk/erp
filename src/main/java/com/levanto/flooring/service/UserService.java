package com.levanto.flooring.service;

import com.levanto.flooring.entity.AppUser;
import com.levanto.flooring.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<AppUser> getAllUsers() {
        return userRepository.findAll();
    }

    public AppUser getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public AppUser getByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional
    public void createUser(AppUser user) {
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username is already taken");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = getByUsername(username);
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Transactional
    public void toggleStatus(Long id) {
        AppUser user = getById(id);
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
    }
}
