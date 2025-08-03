package cn.qingweico.model;

import lombok.Getter;

import java.io.Serial;

/**
 * 自定义业务异常类, 用于处理业务逻辑中的异常情况
 *
 * @author zqw
 * @date 2025/8/1
 */
public class BusinessException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 412362892343246790L;

    /**
     * 异常码值
     * 请自行实现 {@link IErrorCode}
     */
    @Getter
    protected IErrorCode errorCode;
    /**
     * 异常信息
     */
    protected String msg;

    /**
     * 仅包含错误码
     *
     * @param errorCode 错误码接口实现
     */
    public BusinessException(IErrorCode errorCode) {
        this.errorCode = errorCode;
        this.msg = errorCode.getMsg();
    }

    /**
     * 含错误码和自定义消息
     *
     * @param errorCode 错误码接口实现
     * @param msg       自定义异常信息
     */
    public BusinessException(IErrorCode errorCode, String msg) {
        super(msg);
        this.errorCode = errorCode;
        this.msg = msg;
    }

    /**
     * 包含错误码、自定义消息和原始异常
     *
     * @param errorCode 错误码接口实现
     * @param msg       自定义异常信息
     * @param e         原始异常
     */
    public BusinessException(IErrorCode errorCode, String msg, Throwable e) {
        super(msg, e);
        this.errorCode = errorCode;
        this.msg = msg;
    }

    /**
     * 包含错误码和原始异常
     *
     * @param errorCode 错误码接口实现
     * @param e         原始异常
     */
    public BusinessException(IErrorCode errorCode, Throwable e) {
        super(errorCode.getMsg(), e);
        this.errorCode = errorCode;
        this.msg = errorCode.getMsg();
    }

    /**
     * 仅包含自定义消息
     *
     * @param msg 自定义异常信息
     */
    public BusinessException(String msg) {
        super(msg);
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        return this.msg;
    }
}
