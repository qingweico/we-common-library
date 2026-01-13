package cn.qingweico.encrypt;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * RSA 解密工具类
 *
 * @author zqw
 * @date 2025/8/2
 */
public class RsaDecrypt {
    static final String BASE_64_STR = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    Map<String, Integer> base64IndexMap = new HashMap<>();
    Logger log = LoggerFactory.getLogger(RsaDecrypt.class);

    /**
     * 构造函数,初始化Base64字符索引映射表
     */
    public RsaDecrypt() {
        for (int i = 0; i < BASE_64_STR.length(); i++) {
            base64IndexMap.put(String.valueOf(BASE_64_STR.charAt(i)), i);
        }
    }

    /**
     * RSA解密主方法
     *
     * @param cipher     Base64编码的密文
     * @param privateKey Base64编码的私钥(格式为"模数,私钥指数")
     * @return 解密后的明文
     * @throws IllegalArgumentException 如果输入参数无效
     * @throws RuntimeException         如果解密过程中发生错误
     */
    public String decrypt(String cipher, String privateKey) {
        int[] decArray = cipherToDecArray(cipher);
        String binaryString = decArrayToBinString(decArray);
        String[] binArray = dealBinStringToBinArray(binaryString);
        int[] enDecArray = binArrayToDecArray(binArray);
        int[] rsaArray = rsaArray(enDecArray, privateKey);
        return ascArrayToString(rsaArray);
    }

    /**
     * 将密文字符串转换为Base64索引十进制数组
     *
     * @param cipher Base64编码的密文
     * @return Base64字符对应的十进制索引数组
     * @throws RuntimeException 如果包含非Base64字符
     */
    public int[] cipherToDecArray(String cipher) {
        int[] array = new int[cipher.length()];
        for (int i = 0; i < cipher.length(); i++) {
            String s = String.valueOf(cipher.charAt(i));
            array[i] = getBase64Index(s);
        }
        return array;
    }

    /**
     * 将二进制字符串数组转换为十进制数组
     *
     * @param binArray 二进制字符串数组(每个元素表示一个8位二进制数)
     * @return 十进制整数数组
     */
    public int[] binArrayToDecArray(String[] binArray) {
        int[] decArray = new int[binArray.length];
        for (int i = 0; i < binArray.length; i++) {
            String string2 = Integer.valueOf(binArray[i], 2).toString();
            decArray[i] = Integer.parseInt(string2);
        }
        return decArray;
    }

    /**
     * 获取Base64字符对应的索引值
     *
     * @param str Base64字符
     * @return 字符对应的索引值(0 - 63)
     * @throws RuntimeException 如果字符不是有效的Base64字符
     */
    public int getBase64Index(String str) {
        Integer index = base64IndexMap.get(str);
        if (index == null) {
            throw new RuntimeException("not found '" + str + "' base64Index");
        } else {
            return index;
        }
    }

    /**
     * 将Base64索引十进制数组转换为二进制字符串
     *
     * @param decArray Base64索引十进制数组
     * @return 拼接后的二进制字符串(每个索引转换为6位二进制)
     */
    public String decArrayToBinString(int[] decArray) {
        StringBuilder sb = new StringBuilder();
        for (int i : decArray) {
            String str = "00000000" + Integer.toBinaryString(i);
            String substring = str.substring(str.length() - 6);
            sb.append(substring);
        }
        return sb.toString();
    }

    /**
     * RSA解密运算
     *
     * @param cryptAscii 密文ASCII值
     * @param pow        私钥指数(d)
     * @param divisor    模数(n)
     * @return 解密后的明文ASCII值
     */
    public int rsaDecrypt(int cryptAscii, int pow, int divisor) {
        // 对密文ASCII值做幂运算
        BigDecimal powResult = BigDecimal.valueOf(cryptAscii).pow(pow);
        // 再对密运算结果对divisor取余数
        BigDecimal remainder = powResult.remainder(BigDecimal.valueOf(divisor));
        return remainder.intValue();
    }

    /**
     * Base64解码
     *
     * @param str Base64编码字符串
     * @return 解码后的原始字符串(UTF - 8编码)
     * @throws RuntimeException 如果输入为空或解码失败
     */
    public String getFromBase64(String str) {
        String result = null;
        try {
            byte[] b = Base64.getDecoder().decode(str);
            result = new String(b, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return result;
    }

    /**
     * 对密文数组进行RSA解密
     *
     * @param array      密文ASCII值数组
     * @param privateKey Base64编码的私钥(格式为"模数,私钥指数")
     * @return 解密后的明文ASCII值数组
     * @throws RuntimeException 如果私钥格式无效
     */
    public int[] rsaArray(int[] array, String privateKey) {
        if (privateKey == null || privateKey.isEmpty()) {
            throw new RuntimeException("privateKey is null");
        }
        // 将私钥从base64反解出来
        String string = getFromBase64(privateKey);
        String[] split = string.split(",");
        if (split.length != 2) {
            throw new RuntimeException("privateKey '" + privateKey + "' fromBase64: '" + string + "' error");
        }

        for (int i = 0; i < array.length; i++) {
            array[i] = rsaDecrypt(array[i], Integer.parseInt(split[1]), Integer.parseInt(split[0]));
        }
        return array;
    }

    /**
     * 将ASCII值数组转换为字符串
     *
     * @param array ASCII值数组
     * @return 对应的字符串
     */
    public String ascArrayToString(int[] array) {
        StringBuilder clearText = new StringBuilder();
        for (int j : array) {
            clearText.append((char) Integer.parseInt(String.valueOf(j)));
        }
        return clearText.toString();
    }

    /**
     * 将二进制字符串分割为8位一组的数组
     *
     * @param str 二进制字符串
     * @return 8位二进制字符串数组(最后不足8位的部分会被丢弃)
     */
    public String[] dealBinStringToBinArray(String str) {
        int count = str.length() / 8;
        String[] st = new String[count];
        for (int i = 0; i < count; i++) {
            String substring = str.substring(0, 8);
            str = str.substring(8);
            st[i] = substring;
        }
        return st;
    }

    /**
     * 生成 RSA 密钥对
     *
     * @param keySize 密钥长度
     * @return KeyPair 对象
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException, NoSuchProviderException {
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGen.initialize(keySize, new SecureRandom());
        return keyPairGen.generateKeyPair();
    }
}
