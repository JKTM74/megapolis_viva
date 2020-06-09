package com.megapolis.viva.jpa.models;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Сущность шаблона для создания задачи в тезисе
 */

@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Template {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    Theme theme;
    String city;
    @ManyToOne
    @JoinColumn(name = "institution_id")
    InstitutionForm institutionForm;

    @Column
    @Type(type = "jsonb")
    List<EmployeesWithRole> employeesWithRole;

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class EmployeesWithRole implements Serializable {
        private String role;
        private Set<Employee> employees;
    }

    public enum Role {
        CONTROLLER("Controller", "Контролёр"),
        EXECUTOR("Executor", "Исполнитель"),
        OBSERVER("Observer", "Наблюдатель");

        private String value;
        private String localizedValue;

        Role(String value, String localizedValue) {
            this.value = value;
            this.localizedValue = localizedValue;
        }

        public String getValue() {
            return value;
        }

        public String getLocalizedValue() {
            return localizedValue;
        }
    }
}
