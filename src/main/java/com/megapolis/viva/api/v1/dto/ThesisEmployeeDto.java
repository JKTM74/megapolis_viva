package com.megapolis.viva.api.v1.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder @Getter @Setter
public class ThesisEmployeeDto {
    private String id;
    private City city;
    private String firstName;
    private String lastName;
    private String middleName;
    private String name;
    private User user;
    private List<InstitutionThesisDto> institutions;

    @Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
    public static class InstitutionThesisDto {
        private String id;
        private String name;
    }

    @AllArgsConstructor @NoArgsConstructor @Builder @Getter @Setter
    public static class City {
        private String id;
        private String name;
    }

    @AllArgsConstructor @NoArgsConstructor @Builder @Getter @Setter
    public static class User {
        private String id;
        private String active;
        private String deleteTs;
        private String departmentCode;
        private String login;
        private String name;
        private String position;
    }
}
