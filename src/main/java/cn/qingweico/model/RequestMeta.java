package cn.qingweico.model;

import lombok.Builder;
import lombok.Data;

/**
 * @author zqw
 * @date 2026/1/30
 */
@Data
@Builder
public class RequestMeta {
    @Builder.Default
    private long epoch = System.currentTimeMillis();
    private String traceId;
}
