package com.fileservice.minioservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FgaRelation {
    private String object;
    private String relation;
    private String user;
}
