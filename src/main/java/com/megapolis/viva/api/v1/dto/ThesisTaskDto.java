package com.megapolis.viva.api.v1.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor @NoArgsConstructor @Builder @Getter @Setter
public class ThesisTaskDto {
    private String name;
    private String description;
    private int priority;
    private String initiator;
    private List<String> executors;
    private List<String> observers;
    private List<String> controllers;
    private int startTaskType;
}
