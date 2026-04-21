package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface EntityRepository extends JpaRepository<EntityFile,Integer> {
    
    List<EntityFile> findByName(String user_name);
    List<EntityFile> findByEmail(String email_id);
    
}
