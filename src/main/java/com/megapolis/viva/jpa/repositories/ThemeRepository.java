package com.megapolis.viva.jpa.repositories;

import com.megapolis.viva.jpa.models.Theme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThemeRepository extends JpaRepository<Theme, Long> {
    public List<Theme> findAllByOrderByNameDesc();
}
