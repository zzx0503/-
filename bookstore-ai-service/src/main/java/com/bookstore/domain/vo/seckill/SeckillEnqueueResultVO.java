package com.bookstore.domain.vo.seckill;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeckillEnqueueResultVO {

    private String requestId;
    private String status;
}
