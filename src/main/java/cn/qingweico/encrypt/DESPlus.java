package cn.qingweico.encrypt;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.Key;

/**
 * DES 对称加密解密工具类
 *
 * @author zqw
 * @date 2025/7/26
 */
public class DESPlus {
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;

    /**
     * 将字节数组转换为十六进制字符串
     * <p>每个字节转换为两个十六进制字符,如 byte[]{8,18} 转换为 "0812"</p>
     *
     * @param arrByte 需要转换的字节数组
     * @return 十六进制格式的字符串
     * @see #hexStr2ByteArr(String) 互逆
     */
    public static String byteArr2HexStr(byte[] arrByte) {
        int iLen = arrByte.length;
        StringBuilder sb = new StringBuilder(iLen * 2);
        for (byte b : arrByte) {
            sb.append(String.format("%02x", b & 0xFF));
        }
        return sb.toString();
    }


    /**
     * 将十六进制字符串转换为字节数组
     * <p>每两个字符转换为一个字节,如 "0812" 转换为 byte[]{8,18}</p>
     *
     * @param strIn 需要转换的十六进制字符串
     * @return 转换后的字节数组
     * @see #byteArr2HexStr(byte[]) 互逆
     */
    public static byte[] hexStr2ByteArr(String strIn) {
        byte[] arrB = strIn.getBytes();
        int iLen = arrB.length;
        byte[] arrOut = new byte[iLen / 2];
        for (int i = 0; i < iLen; i = i + 2) {
            String strTmp = new String(arrB, i, 2);
            arrOut[i / 2] = (byte) Integer.parseInt(strTmp, 16);
        }
        return arrOut;
    }

    /**
     * 使用指定密钥构造DES加密解密器
     *
     * @param strKey 加密密钥字符串
     * @throws Exception 如果初始化加密器失败
     */
    public DESPlus(String strKey) throws Exception {
        Key key = getKey(strKey.getBytes());
        encryptCipher = Cipher.getInstance("DES");
        encryptCipher.init(Cipher.ENCRYPT_MODE, key);
        decryptCipher = Cipher.getInstance("DES");
        decryptCipher.init(Cipher.DECRYPT_MODE, key);
    }

    /**
     * 加密字节数组
     *
     * @param arrByte 需要加密的原始字节数组
     * @return 加密后的字节数组
     * @throws javax.crypto.IllegalBlockSizeException 如果数据长度不符合要求
     * @throws javax.crypto.BadPaddingException       如果填充错误
     */
    public byte[] encrypt(byte[] arrByte) throws Exception {
        return encryptCipher.doFinal(arrByte);
    }

    /**
     * 加密字符串
     *
     * @param strIn 需要加密的原始字符串
     * @return 十六进制格式的加密结果
     * @throws NullPointerException 如果输入字符串为null
     */
    public String encrypt(String strIn) throws Exception {
        return byteArr2HexStr(encrypt(strIn.getBytes()));
    }

    /**
     * 解密字节数组
     *
     * @param arrByte 需要解密的字节数组
     * @return 解密后的原始字节数组
     * @throws NullPointerException                   如果输入数组为null
     * @throws javax.crypto.IllegalBlockSizeException 如果数据长度不符合要求
     * @throws javax.crypto.BadPaddingException       如果填充错误
     */
    public byte[] decrypt(byte[] arrByte) throws Exception {
        return decryptCipher.doFinal(arrByte);
    }

    /**
     * 解密十六进制格式字符串
     *
     * @param strIn 需要解密的十六进制字符串
     * @return 解密后的原始字符串(UTF - 8编码)
     * @throws NullPointerException 如果输入字符串为null
     */
    public String decrypt(String strIn) throws Exception {
        return new String(decrypt(hexStr2ByteArr(strIn)), StandardCharsets.UTF_8);
    }

    /**
     * 从字节数组生成DES密钥
     * <p>密钥长度固定为8字节,不足补零,超长截取</p>
     *
     * @param arrByte 原始密钥字节数组
     * @return 生成的DES密钥
     * @throws NullPointerException 如果输入数组为null
     */
    private Key getKey(byte[] arrByte) throws Exception {
        byte[] arrB = new byte[8];
        System.arraycopy(arrByte, 0, arrB, 0, Math.min(arrByte.length, arrB.length));
        return new javax.crypto.spec.SecretKeySpec(arrB, "DES");
    }
}
