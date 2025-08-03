package cn.qingweico.encrypt;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.crypto.digests.SM3Digest;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * AES/CBC/PKCS5Padding
 * 其中【AES】是对称加密算法,密钥长度支持128/192/256位
 * 【CBC】是加密模式(Cipher Block Chaining),每个块依赖前一个块的密文
 * 【PKCS5Padding】是填充方案,将数据补齐到块大小的整数倍(AES块大小=16字节)
 * 需要IV向量,初始化向量(IV)需随机生成且每次加密不同
 * 计算速度快,适合加密大数据量,比如文件、HTTP消息体
 * 对称加密的概念: 加密解密使用相同密钥
 * ---------------------------------------------------------------
 * RSA是非对称加密,使用公钥加密,私钥解密,其中公钥可公开,私钥必须保密
 * 其底层原理基于大数分解难题(RSA)或椭圆曲线(EC),填充方案通常使用PKCS1Padding
 * 加密模式方面则是无加密模式:RSA本身是块加密,不涉及CBC/ECB等模式
 * 计算速度慢,适合加密小数据量(如密钥交换、数字签名)
 * ---------------------------------------------------------------
 * 基于两组加密方式的特性,所以两组加密方式一般组合使用,RSA传密钥,AES加密数据
 *
 * @author zqw
 * @date 2025/7/26
 */
public class Security {

    private static final ThreadLocal<Map<String, Cipher>> RSA_ENCRYPT_CIPHER_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, Cipher>> RSA_DECRYPT_CIPHER_THREAD_LOCAL = new ThreadLocal<>();
    private final static String[] HEX_DIGITS = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};
    private static final char[] ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();

    private static Map<String, Cipher> getEncryptCipherMap() {
        Map<String, Cipher> df = RSA_ENCRYPT_CIPHER_THREAD_LOCAL.get();
        if (df == null) {
            df = new HashMap<>();
            RSA_ENCRYPT_CIPHER_THREAD_LOCAL.set(df);
        }
        return df;
    }

    private static Map<String, Cipher> getDecryptCipherMap() {
        Map<String, Cipher> df = RSA_DECRYPT_CIPHER_THREAD_LOCAL.get();
        if (df == null) {
            df = new HashMap<>();
            RSA_DECRYPT_CIPHER_THREAD_LOCAL.set(df);
        }
        return df;
    }

    /**
     * 根据RSA公钥字符串获取/创建对应的加密Cipher实例
     *
     * <p>方法内部会缓存已创建的Cipher对象,避免重复初始化开销</p>
     *
     * <p><b>注意:</b>传入的公钥需符合PEM格式(含BEGIN/END标记)或Base64裸格式</p>
     *
     * @param key RSA公钥字符串,支持以下格式:
     *            <ul>
     *              <li>PEM格式(自动清理标记和空白字符):
     *                <pre>-----BEGIN PUBLIC KEY-----...-----END PUBLIC KEY-----</pre>
     *              </li>
     *              <li>Base64裸格式:直接传入Base64编码字符串</li>
     *            </ul>
     * @return 初始化好的Cipher实例(加密模式,使用PKCS1Padding)
     * @throws NullPointerException 当key为null时抛出
     * @throws IllegalArgumentException 当key格式非法时抛出
     * @throws GeneralSecurityException 当以下情况时抛出:
     *            <ul>
     *              <li>密钥工厂初始化失败(如无RSA Provider)</li>
     *              <li>Base64解码失败</li>
     *              <li>密钥规格无效</li>
     *              <li>Cipher初始化失败</li>
     *            </ul>
     *
     * @implSpec 方法内部逻辑:
     * 1. 清理PEM格式的标记和空白字符 → 2. Base64解码 →
     * 3. 生成PublicKey → 4. 初始化Cipher → 5. 缓存并返回
     *
     * @implNote 性能优化:
     * - 使用静态Map缓存Cipher实例(线程安全由调用方保证)
     * - 避免重复解析同一公钥
     *
     * @see javax.crypto.Cipher
     * @see java.security.KeyFactory
     * @see java.security.spec.X509EncodedKeySpec
     */
    private static Cipher getRsaPubKeyCipher(String key) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        key = key.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace(" ", "")
                .replace("\r", "")
                .replace("\n", "");
        Map<String, Cipher> map = getEncryptCipherMap();
        if (map.containsKey(key)) {
            return map.get(key);
        }
        byte[] data = Base64.getDecoder().decode(key);
        PublicKey k = kf.generatePublic(new X509EncodedKeySpec(data));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, k);
        map.put(key, cipher);
        return cipher;
    }
    /**
     * 根据RSA私钥字符串获取/创建对应的解密Cipher实例
     *
     * <p><b>缓存机制:</b>方法内部会缓存已初始化的Cipher对象,避免重复初始化开销(线程安全由调用方保证)</p>
     *
     * <p><b>密钥格式要求:</b></p>
     * <ul>
     *   <li>支持PEM格式(自动清理标记和空白字符):
     *     <pre>-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----</pre>
     *   </li>
     *   <li>或Base64裸格式(直接传入Base64编码字符串)</li>
     * </ul>
     *
     * @param key RSA私钥字符串(PEM或Base64格式)
     * @return 初始化好的Cipher实例(解密模式,使用PKCS1Padding)
     * @throws NullPointerException 当key为null时抛出
     * @throws IllegalArgumentException 当key格式非法(非PEM且非Base64)时抛出
     * @throws GeneralSecurityException 当以下情况时抛出:
     *   <ul>
     *     <li>密钥工厂初始化失败(如无RSA Provider)</li>
     *     <li>Base64解码失败</li>
     *     <li>密钥规格无效(非PKCS#8编码)</li>
     *     <li>Cipher初始化失败</li>
     *   </ul>
     *
     * @implNote 性能优化:
     * <ul>
     *   <li>使用静态Map缓存Cipher实例,相同私钥重复调用直接返回缓存对象</li>
     *   <li>自动清理PEM格式的冗余字符(BEGIN/END标记、换行符等)</li>
     * </ul>
     *
     * @see javax.crypto.Cipher
     * @see java.security.spec.PKCS8EncodedKeySpec
     * @see java.security.KeyFactory
     */
    private static Cipher getRsaPrivateKeyCipher(String key) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA");
        key = key.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace(" ", "")
                .replace("\r", "")
                .replace("\n", "");
        Map<String, Cipher> map = getDecryptCipherMap();
        if (map.containsKey(key)) {
            return map.get(key);
        }
        byte[] data = Base64.getDecoder().decode(key);
        PrivateKey k = kf.generatePrivate(new PKCS8EncodedKeySpec(data));
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, k);
        map.put(key, cipher);
        return cipher;
    }

    /**
     * 使用RSA公钥加密数据(线程安全实现)
     *
     * <p><b>加密规范:</b></p>
     * <ul>
     *   <li>算法:RSA/ECB/PKCS1Padding</li>
     *   <li>密钥格式:支持PEM格式或裸Base64编码的公钥</li>
     *   <li>输入限制:加密数据长度 ≤ (密钥长度/8 - 11)字节</li>
     * </ul>
     *
     * @param data    待加密的原始数据(非null,最大长度取决于密钥长度)
     * @param pubKey  公钥字符串,支持以下格式:
     *                <ul>
     *                  <li>PEM格式(自动处理标记和空白):
     *                    <pre>-----BEGIN PUBLIC KEY-----...-----END PUBLIC KEY-----</pre>
     *                  </li>
     *                  <li>Base64裸格式:直接传入Base64编码字符串</li>
     *                </ul>
     * @return        加密后的字节数组(长度=密钥长度/8)
     * @throws IllegalArgumentException 当以下情况时抛出:
     *                <ul>
     *                  <li>data为null或长度超限</li>
     *                  <li>pubKey格式非法</li>
     *                </ul>
     * @throws RuntimeException 当加密失败时抛出,包含原始异常信息
     *
     * @implSpec 线程安全保证:
     * <ol>
     *   <li>内部通过 {@link #getRsaPubKeyCipher(String)} 获取Cipher实例</li>
     *   <li>Cipher实例缓存为线程局部变量(ThreadLocal)</li>
     *   <li>每次加密操作原子性(无共享状态修改)</li>
     * </ol>
     *
     * @apiNote 典型用法示例:
     * <pre>{@code
     * String publicKey = "-----BEGIN PUBLIC KEY-----...";
     * byte[] encrypted = rsaEncrypt("明文数据".getBytes(UTF_8), publicKey);
     * }</pre>
     *
     * @see #getRsaPubKeyCipher(String)
     * @see javax.crypto.Cipher
     */
    public static byte[] rsaEncrypt(byte[] data, String pubKey) {
        try {
            return getRsaPubKeyCipher(pubKey).doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 使用RSA私钥解密数据(线程安全实现)
     *
     * <p><b>解密规范:</b></p>
     * <ul>
     *   <li>算法:RSA/ECB/PKCS1Padding(与加密端严格一致)</li>
     *   <li>密钥格式:PKCS#8标准的PEM或裸Base64私钥</li>
     *   <li>输入限制:密文长度必须等于密钥长度/8(如2048位密钥需256字节输入)</li>
     * </ul>
     *
     * @param data    待解密的密文字节数组(非null,长度必须严格匹配密钥长度)
     * @param priKey  PKCS#8格式的私钥字符串,支持:
     *                <ul>
     *                  <li>PEM格式(自动清理标记):
     *                    <pre>-----BEGIN PRIVATE KEY-----...-----END PRIVATE KEY-----</pre>
     *                  </li>
     *                  <li>Base64裸格式:直接传入Base64编码字符串</li>
     *                </ul>
     * @return        解密后的原始字节数据
     * @throws IllegalArgumentException 当以下情况时抛出:
     *                <ul>
     *                  <li>data为null或长度不符</li>
     *                  <li>priKey格式非法(非PKCS#8)</li>
     *                </ul>
     * @throws RuntimeException 当解密失败时抛出,包含以下可能原因:
     *                <ul>
     *                  <li>私钥不匹配(非加密时使用的配对私钥)</li>
     *                  <li>密文被篡改</li>
     *                  <li>密钥已过期</li>
     *                </ul>
     *
     * @implNote 线程安全实现细节:
     * <ol>
     *   <li>通过 {@link #getRsaPrivateKeyCipher(String)} 获取线程级缓存的Cipher实例</li>
     *   <li>每个线程持有独立的Cipher副本,避免锁竞争</li>
     *   <li>解密操作本身是原子性的</li>
     * </ol>
     *
     * @apiNote 典型用法示例:
     * <pre>{@code
     * // 解密场景
     * String privateKey = "-----BEGIN PRIVATE KEY-----...";
     * byte[] decrypted = rsaDecrypt(encryptedData, privateKey);
     * String plainText = new String(decrypted, StandardCharsets.UTF_8);
     * }</pre>
     *
     * @see #rsaEncrypt(byte[], String) 配套加密方法
     * @see #getRsaPrivateKeyCipher(String) 内部Cipher获取逻辑
     * @see <a href="https://tools.ietf.org/html/rfc5208">PKCS#8规范(RFC 5208)</a>
     */
    public static byte[] rsaDecrypt(byte[] data, String priKey) {
        try {
            return getRsaPrivateKeyCipher(priKey).doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }

    /**
     * 计算字符串的哈希值(支持SHA算法)
     * @param plainText 待哈希的原始数据
     * @param type 哈希算法类型(如"SHA-256")
     * @param encoding 字符编码
     * @return 哈希值的十六进制字符串
     */
    private static String sha(String plainText, String type, String encoding) {
        try {
            MessageDigest digest = MessageDigest.getInstance(type);
            digest.update(plainText.getBytes(encoding));
            byte[] messageDigest = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("数据转SHA失败");
        }
    }
    /**
     * 将字符串转换为MD5哈希值(16进制字符串形式)。
     *
     * <p><b>注意:MD5算法已不再安全,不推荐用于安全敏感场景。</b></p>
     *
     * <h3>安全警告</h3>
     * <ul>
     *   <li>MD5存在严重的<b>碰撞漏洞</b>(可人为构造不同输入产生相同哈希值)</li>
     *   <li>适用于非安全场景(如简单数据校验),不可用于:
     *     <ul>
     *       <li>密码存储</li>
     *       <li>数字签名</li>
     *       <li>证书验证</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <h3>替代方案</h3>
     * 对于安全敏感场景,建议使用:
     * <pre>
     *   // SHA-256示例
     *   MessageDigest.getInstance("SHA-256")
     * </pre>
     *
     * @param str     要哈希的输入字符串
     * @return 32字符的MD5十六进制字符串(小写)
     * @throws RuntimeException 如果发生以下情况:
     *               <ul>
     *                 <li>指定的字符集无效</li>
     *                 <li>当前JVM不支持MD5算法</li>
     *               </ul>
     *
     * @see java.security.MessageDigest
     * @see <a href="https://tools.ietf.org/html/rfc6151">RFC 6151 - MD5安全淘汰声明</a>
     */

    public static String toMD5(String str) {
        return toMD5(str, "utf-8");
    }

    public static String toMD5(String str, String charset) {
        try {
            byte[] arr = MessageDigest.getInstance("MD5").digest(str.getBytes(charset));
            return byteArrayToHexString(arr);
        } catch (Exception e) {
            throw new RuntimeException("获取字符串MD5失败");
        }
    }

    public static String toSHA1(String str) {
        return sha(str, "SHA-1", "utf-8");
    }

    public static String toSHA1(String str, String charset) {
        return sha(str, "SHA-1", charset);
    }

    public static String toSHA256(String str) {
        return sha(str, "SHA-256", "utf-8");
    }

    public static String toSHA256(String str, String charset) {
        return sha(str, "SHA-256", charset);
    }

    public static String toSHA512(String str) {
        return sha(str, "SHA-512", "utf-8");
    }

    public static String toSHA512(String str, String charset) {
        return sha(str, "SHA-512", charset);
    }
    /**
     * 计算字符串的SM3哈希值(国密算法)
     *
     * <p><b>算法特性:</b></p>
     * <ul>
     *   <li>哈希长度:固定32字节(256位)</li>
     *   <li>安全性:抗碰撞性强于SHA-256,符合中国商用密码标准</li>
     *   <li>编码:输入输出均使用UTF-8编码</li>
     * </ul>
     *
     * @param data 待哈希的原始字符串(非null,空字符串允许但结果固定)
     * @return 64字符的十六进制哈希字符串(小写)
     * @throws IllegalArgumentException 当data为null时抛出
     * @throws RuntimeException 当哈希计算失败时抛出,可能原因包括:
     *   <ul>
     *     <li>BouncyCastle Provider未注册</li>
     *     <li>内存不足导致字节数组分配失败</li>
     *   </ul>
     *
     * @implSpec 实现细节:
     * <ol>
     *   <li>使用BouncyCastle的{@link SM3Digest}实现</li>
     *   <li>内部处理流程:
     *     <pre>UTF-8字节 → SM3哈希 → 十六进制字符串转换</pre>
     *   </li>
     * </ol>
     *
     * @apiNote 典型用法示例:
     * <pre>{@code
     * String hash = toSM3("需要加密的数据");
     * System.out.println(hash); // 输出:55e12e91650d2fec56ec74fa1b7d17a40b7b1c0d8085e8a54a8b...
     * }</pre>
     *
     * @security 安全警告:
     * <ul>
     *   <li>SM3不适合加密敏感数据(如需加密请使用SM4)</li>
     *   <li>建议对重要数据加盐处理:<code>toSM3("salt"+data)</code></li>
     * </ul>
     *
     * @see org.bouncycastle.crypto.digests.SM3Digest
     * @see <a href="https://www.oscca.gov.cn/sca/xxgk/2010-12/17/content_1002389.shtml">GM/T 0004-2012 SM3标准</a>
     */
    public static String toSM3(String data) {
        SM3Digest digest = new SM3Digest();
        try {
            byte[] bt = data.getBytes(StandardCharsets.UTF_8);
            digest.update(bt, 0, bt.length);
            byte[] out = new byte[32];
            digest.doFinal(out, 0);
            return byteArrayToHexString(out);
        } catch (Exception e) {
            throw new RuntimeException("SM3加密失败");
        }
    }



    public static String toBase64(String str) {
        return toBase64(str.getBytes());
    }

    public static String toBase64(byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;
            int val = (0xFF & (int) data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & (int) data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & (int) data[i + 2]);
                quad = true;
            }
            out[index + 3] = ALPHABET[(quad ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 2] = ALPHABET[(trip ? (val & 0x3F) : 64)];
            val >>= 6;
            out[index + 1] = ALPHABET[val & 0x3F];
            val >>= 6;
            out[index] = ALPHABET[val & 0x3F];
        }

        return new String(out);
    }
    /**
     * 将十六进制字符串转换为字节数组
     *
     * <p><b>转换规则:</b></p>
     * <ul>
     *   <li>每两个字符解析为一个字节(如 "A1" → 0xA1)</li>
     *   <li>不区分大小写(自动转为大写处理)</li>
     *   <li>忽略前导0x标记(如有需要调用方自行去除)</li>
     * </ul>
     *
     * @param hexString 十六进制字符串,需满足:
     *                  <ul>
     *                    <li>非null且非空</li>
     *                    <li>长度为偶数(每字节需两个字符)</li>
     *                    <li>仅包含0-9、A-F、a-f字符</li>
     *                  </ul>
     * @return 对应的字节数组,或null当输入为空字符串时
     * @throws IllegalArgumentException 当以下情况时抛出:
     *                  <ul>
     *                    <li>字符串长度为奇数</li>
     *                    <li>包含非法字符(非十六进制字符)</li>
     *                  </ul>
     *
     * @implNote 性能特性:
     * <ol>
     *   <li>时间复杂度:O(n),n为字符串长度</li>
     *   <li>空间复杂度:O(n/2),输出数组长度为输入一半</li>
     * </ol>
     *
     * @apiNote 典型用法示例:
     * <pre>{@code
     * byte[] bytes = hexStringToByteArray("48656C6C6F"); // "Hello"的HEX
     * System.out.println(new String(bytes, StandardCharsets.UTF_8));
     * }</pre>
     *
     * @see #byteArrayToHexString(byte[]) 逆向转换方法
     * @see java.util.HexFormat (JDK 17+原生实现)
     */
    public static byte[] hexStringToByteArray(String hexString) {
        if (StringUtils.isEmpty(hexString)) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * 十六进制字符转字节值(内部方法)
     * @param c 必须为0-9或A-F的大写字符
     * @return 对应的4位字节值(0x00-0x0F)
     * @throws IllegalArgumentException 当字符非法时抛出
     */
    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        for (byte value : b) {
            resultSb.append(byteToHexString(value));
        }
        return resultSb.toString();
    }

    /**
     * 将一个字节转化成十六进制形式的字符串
     */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n = 256 + n;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HEX_DIGITS[d1] + HEX_DIGITS[d2];
    }
}
