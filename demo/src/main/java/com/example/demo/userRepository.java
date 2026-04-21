package com.example.demo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface userRepository  extends JpaRepository<userEntity,Long>{

    List<userEntity> findByEmail(String email);
}