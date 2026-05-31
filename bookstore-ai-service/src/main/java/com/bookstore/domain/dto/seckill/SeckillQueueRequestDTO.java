package com.bookstore.domain.dto.seckill;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillQueueRequestDTO {

    private String requestId;
    private Long userId;
    private Long activityId;
    private Long addressId;
    private LocalDateTime timestamp;
}
