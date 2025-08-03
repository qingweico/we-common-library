package cn.qingweico.encrypt;

import org.apache.commons.codec.binary.Base64;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base64 编码解码工具类
 *
 * @author zqw
 * @date 2022/8/27
 */
public class Base64Convert {

    private static final Base64 BASE64 = new Base64();


    /**
     * 将输入流转换为Base64编码字符串
     *
     * @param in 输入流对象
     * @return Base64编码后的字符串
     * @throws IOException 当读取输入流发生错误时抛出
     * @apiNote 方法会自动关闭输入流
     * @see InputStream#available()
     */
    public static String ioToBase64(InputStream in) throws IOException {
        String strBase64;
        try {
            byte[] bytes = new byte[in.available()];
            in.read(bytes);
            strBase64 = BASE64.encodeToString(bytes);
        } finally {
            if (in != null) {
                in.close();
            }
        }
        return strBase64;
    }

    /**
     * 将字节数组转换为Base64编码字符串
     *
     * @param bytes 待编码的字节数组
     * @return Base64编码后的字符串
     * <pre>{@code
     * byte[] data = "hello".getBytes();
     * String base64 = Base64Convert.byteToBase64(data);
     * }</pre>
     */
    public static String byteToBase64(byte[] bytes) {
        String strBase64;
        strBase64 = BASE64.encodeToString(bytes);
        return strBase64;
    }

    /**
     * 将Base64字符串解码为原始字节数组
     *
     * @param strBase64 合法的Base64编码字符串
     * @return 解码后的原始字节数组
     */
    public static byte[] base64ToByte(String strBase64) {
        return BASE64.decode(strBase64);
    }
}
