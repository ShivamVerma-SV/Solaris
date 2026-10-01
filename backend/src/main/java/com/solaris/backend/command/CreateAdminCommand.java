package com.solaris.backend.command;

import com.solaris.backend.entity.User;
import com.solaris.backend.entity.UserRole;
import com.solaris.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Scanner;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class CreateAdminCommand implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (args.length == 0 || !args[0].equals("create-admin")) {
            return;
        }

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter Name : ");
        String name = scanner.nextLine().trim();
        System.out.print("Enter Email : ");
        String email = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

        if (name.length() < 2 || email.isBlank()) {
            System.out.println("A valid name and email are required.");
            return;
        }

        if (userRepository.existsByEmail(email)) {
            System.out.println("User with this email already exists.");
            return;
        }

        System.out.print("Password: ");
        String password = scanner.nextLine();

        if (password.length() < 8 || password.length() > 72) {
            System.out.println("Password must be between 8 and 72 characters.");
            return;
        }

        User admin = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.ADMIN)
                .build();

        userRepository.save(admin);

        System.out.println("Admin user created successfully.");
    }

}
