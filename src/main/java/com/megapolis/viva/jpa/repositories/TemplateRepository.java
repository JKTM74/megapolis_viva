package com.megapolis.viva.jpa.repositories;

import com.megapolis.viva.jpa.models.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    @Query("select t from Template t where t.city = :city and " +
            "t.theme.name = :theme and " +
            "t.institutionForm.name = :institution")
    Optional<Template> getTemplateByCityAndThemeAndInstitution(
            @Param("city") String city,
            @Param("theme") String theme,
            @Param("institution") String institution
    );
}
