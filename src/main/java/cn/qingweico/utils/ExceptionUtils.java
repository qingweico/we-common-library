package cn.qingweico.utils;

/**
 * @author zqw
 * @date 2025/9/6
 */
public class ExceptionUtils {
    /**
     * 获取异常的根本原因消息
     */
    public static String getRootCauseMessage(Throwable e) {
        Throwable rootCause = getRootCause(e);
        return rootCause.getMessage();
    }

    /**
     * 获取异常的根本原因
     */
    public static Throwable getRootCause(Throwable e) {
        Throwable cause = e;
        while (cause != null && cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * 获取完整的异常链消息
     */
    public static String getFullExceptionChain(Throwable e) {
        StringBuilder sb = new StringBuilder();
        Throwable cause = e;
        int level = 1;

        while (cause != null) {
            if (level > 1) {
                sb.append(" -> ");
            }
            sb.append("[").append(cause.getClass().getSimpleName()).append("] ");
            sb.append(cause.getMessage());

            cause = cause.getCause();
            level++;

            // 防止无限循环
            if (level > 20) {
                break;
            }
        }
        return sb.toString();
    }

}
