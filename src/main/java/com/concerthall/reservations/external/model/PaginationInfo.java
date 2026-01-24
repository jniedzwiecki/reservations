package com.concerthall.reservations.external.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInfo {
    private Integer page;
    private Integer size;
    private Long totalElements;
    private Integer totalPages;
}
