package cn.qingweico.model;

/**
 * 错误码基础接口, 定义统一的错误码规范
 *
 * @author zqw
 * @date 2025/8/1
 */
public interface IErrorCode {

    /**
     * 获取标准错误码
     *
     * @return 非空的错误码字符串
     */
    String getCode();

    /**
     * 获取可读的错误描述信息
     *
     * @return 非空的错误描述信息
     */
    String getMsg();
}
