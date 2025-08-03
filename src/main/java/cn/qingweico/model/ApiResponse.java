package cn.qingweico.model;

import lombok.Builder;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * API 响应 JSON 格式数据
 *
 * @author zqw
 * @date 2023/10/12
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ApiResponse<T> {
    private @Builder.Default String code = "200";
    private @Builder.Default String msg = "OK";
    private @Builder.Default boolean success = false;
    private @Builder.Default Long now = Instant.now().getEpochSecond();
    private T data;

    public ApiResponse(T data) {
        this.data = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok() {
        return ApiResponse.<T>builder()
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> error(String msg) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .success(false)
                .build();
    }

    public static <T> ApiResponse<T> success(String msg) {
        return ApiResponse.<T>builder()
                .msg(msg)
                .success(true)
                .build();
    }
}
