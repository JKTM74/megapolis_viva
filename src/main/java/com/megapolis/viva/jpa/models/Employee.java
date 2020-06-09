package com.megapolis.viva.jpa.models;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.List;

/**
 * Сущность сотрудников из тезиса
 */

@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Builder @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Employee implements Serializable {
    @Id
    private String id;

    private String surname;
    private String name;
    private String patronymic;

    @Column
    @Type(type = "jsonb")
    private List<InstitutionThesis> institutions;

    private String position;
    private String city;
    private String login;
    private Boolean isActive;

    @Override
    public String toString() {
        return name;
    }

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class InstitutionThesis implements Serializable {
        private String id;
        private String name;

        @Override
        public String toString() {
            return name;
        }
    }
}
