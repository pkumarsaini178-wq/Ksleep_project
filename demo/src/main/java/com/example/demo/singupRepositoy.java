package com.example.demo;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface singupRepositoy extends JpaRepository<Entitysignup, Long> {
    List<Entitysignup> findByEmailAndPassword(String email, String password);

    List<Entitysignup> findByEmail(String email);

    long countByRole(String role);

    List<Entitysignup> findByRole(String role);
}
