package cn.qingweico.encrypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Base64;

/**
 * AES加解密工具类
 *
 * @author zqw
 * @date 2025/7/26
 */
public class AESSecurity {
    private static final String KEY_ALGORITHM = "AES";
    // 默认的加密算法
    private static final String DEFAULT_CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";
    private static final String CBC_CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";


    private final ThreadLocal<Cipher> encryptCipher = new ThreadLocal<>();
    private final ThreadLocal<Cipher> decryptCipher = new ThreadLocal<>();

    private final byte[] key;
    private byte[] iv;


    public AESSecurity(String keyStr) {
        this.key = keyStr.getBytes(StandardCharsets.UTF_8);
    }

    public AESSecurity(byte[] key) {
        this.key = key;
    }

    public AESSecurity(byte[] key, byte[] iv) {
        this.key = key;
        this.iv = iv;
    }

    /**
     * 获取解密Cipher实例
     *
     * @return 初始化好的解密Cipher
     */
    private Cipher getEncryptCipher() {
        try {
            Cipher cipher = encryptCipher.get();
            if (cipher == null) {
                Security.addProvider(new BouncyCastleProvider());
                if (iv == null) {
                    cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
                    // 创建密码器
                    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key));
                    // 初始化为加密模式的密码器
                    encryptCipher.set(cipher);
                } else {
                    cipher = Cipher.getInstance(CBC_CIPHER_ALGORITHM, "BC");
                    AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
                    parameters.init(new IvParameterSpec(iv));
                    // 创建密码器
                    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(key), parameters);
                    // 初始化为加密模式的密码器

                    encryptCipher.set(cipher);
                }
            }
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("创建加密器失败", e);
        }
    }

    private Cipher getDecryptCipher() {
        try {
            Cipher cipher = decryptCipher.get();
            if (cipher == null) {
                Security.addProvider(new BouncyCastleProvider());
                if (iv == null) {
                    cipher = Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
                    // 创建密码器
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key));
                    // 初始化为加密模式的密码器
                    decryptCipher.set(cipher);
                } else {
                    cipher = Cipher.getInstance(CBC_CIPHER_ALGORITHM, "BC");
                    AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
                    parameters.init(new IvParameterSpec(iv));
                    // 创建密码器
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(key), parameters);
                    // 初始化为加密模式的密码器

                    decryptCipher.set(cipher);
                }
            }
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("创建解密器失败", e);
        }
    }


    /**
     * AES 加密操作
     *
     * @param content 待加密内容 加密方法会先将其进行getByte操作
     * @return 返回Base64转码后的加密数据
     */
    public String encrypt(String content) {
        try {
            Cipher cipher = getEncryptCipher();
            byte[] byteContent = content.getBytes(StandardCharsets.UTF_8);
            byte[] result = cipher.doFinal(byteContent);
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception ex) {
            throw new RuntimeException("信息加密失败", ex);
        }
    }

    /**
     * 对字节数组进行AES加密
     *
     * @param byteContent 待加密的字节数组
     * @return 加密后的字节数组
     * @throws RuntimeException 如果加密过程中发生错误
     */
    public byte[] encrypt(byte[] byteContent) {
        try {
            Cipher cipher = getEncryptCipher();
            return cipher.doFinal(byteContent);
        } catch (Exception ex) {
            throw new RuntimeException("信息加密失败", ex);
        }
    }

    /**
     * AES 解密操作
     *
     * @param content 待解密内容(Base64格式)
     * @return 解密后的数据的new String()后的结果
     */
    public String decrypt(String content) {
        try {
            Cipher cipher = getDecryptCipher();
            byte[] result = cipher.doFinal(Base64.getDecoder().decode(content));
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("信息解密失败", ex);
        }
    }

    /**
     * 对字节数组进行AES解密
     *
     * @param byteContent 待解密的字节数组
     * @return 解密后的字节数组
     * @throws RuntimeException 如果解密过程中发生错误
     */
    public byte[] decrypt(byte[] byteContent) {
        try {
            Cipher cipher = getDecryptCipher();
            return cipher.doFinal(byteContent);
        } catch (Exception ex) {
            throw new RuntimeException("信息解密失败", ex);
        }
    }

    /**
     * 生成AES加密密钥
     *
     * @param bytes 密钥字节数组(必须为16字节)
     * @return SecretKeySpec对象
     * @throws Exception 如果密钥长度不是16字节
     */
    private SecretKeySpec getSecretKey(final byte[] bytes) throws Exception {
        if (bytes.length != 16) {
            throw new RuntimeException("AES密钥长度必须为16");
        }
        return new SecretKeySpec(bytes, KEY_ALGORITHM);
    }
}
