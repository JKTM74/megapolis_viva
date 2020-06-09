package com.megapolis.viva.api.v1.dto;

import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor @NoArgsConstructor @Builder @Getter @Setter
public class ThesisTaskIdDto {
    private List<String> id;
}
