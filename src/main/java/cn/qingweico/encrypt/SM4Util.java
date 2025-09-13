package cn.qingweico.encrypt;

import cn.qingweico.supplier.RandomDataGenerator;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;

/**
 * SM4对称加密算法工具类
 *
 * @author zqw
 * @date 2025/9/5
 */

public class SM4Util {
    public static byte[] encrypt(byte[] key, byte[] data) throws Exception {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new SM4Engine(), new PKCS7Padding());
        cipher.init(true, new KeyParameter(key));
        byte[] out = new byte[cipher.getOutputSize(data.length)];
        int len = cipher.processBytes(data, 0, data.length, out, 0);
        cipher.doFinal(out, len);
        return out;
    }

    public static byte[] decrypt(byte[] key, byte[] data) throws Exception {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new SM4Engine(), new PKCS7Padding());
        cipher.init(false, new KeyParameter(key));
        byte[] out = new byte[cipher.getOutputSize(data.length)];
        int len = cipher.processBytes(data, 0, data.length, out, 0);
        len += cipher.doFinal(out, len);
        return Arrays.copyOf(out, len);
    }

    /**
     * SM4 的 key 必须是 16 字节
     *
     * @return 生成的随机 SM4 key, 32 位 Hex 字符串
     * @throws NoSuchAlgorithmException 如果 KeyGenerator 不支持 SM4
     * @throws NoSuchProviderException  如果 BouncyCastle 提供者不可用
     */
    public static String keyGenerator() throws NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyGenerator keyGen = KeyGenerator.getInstance("SM4", "BC");
        keyGen.init(128);
        SecretKey secretKey = keyGen.generateKey();
        byte[] keyBytes = secretKey.getEncoded();
        return Hex.toHexString(keyBytes);
    }

    public static void main(String[] args) throws Exception {
        String key = keyGenerator();
        String plain = RandomDataGenerator.address();
        System.out.println(plain);
        // 不要直接对32位Hex字符串进行getBytes调用, getBytes()会把每个字符当作一个字节, 32个字符就变成32字节不满足key长度要求
        // Hex字符串中每两个字符表示一个字节, 使用 Hex.decode处理
        byte[] encrypt = encrypt(Hex.decode(key), plain.getBytes());
        String encryptHex = Hex.toHexString(encrypt);
        System.out.println(encryptHex);
        System.out.println(new String(decrypt(Hex.decode(key), Hex.decode(encryptHex))));
    }
}
