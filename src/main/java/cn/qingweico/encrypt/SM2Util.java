package cn.qingweico.encrypt;

import org.apache.commons.lang3.tuple.Pair;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.util.encoders.Hex;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;


/**
 * SM2(椭圆曲线公钥加密) 加密工具类
 *
 * @author zqw
 * @date 2025/9/5
 */

public class SM2Util {

    /**
     * 使用 SM2 公钥加密数据
     *
     * @param publicKeyStr 公钥(HEX)
     * @param dataHex      明文数据(HEX)
     * @return 密文数据(HEX)
     */
    public static String encrypt(String publicKeyStr, String dataHex) {
        byte[] result = encrypt(getECPublicKeyByPublicKeyHex(publicKeyStr), Hex.decode(dataHex));
        return Hex.toHexString(result);
    }

    /**
     * 使用 SM2 公钥加密数据(输入明文为 byte[])
     *
     * @param publicKeyStr 公钥(HEX)
     * @param data         明文数据
     * @return 密文数据
     */
    public static byte[] encrypt(String publicKeyStr, byte[] data) {
        return encrypt(getECPublicKeyByPublicKeyHex(publicKeyStr), data);
    }

    /**
     * SM2 公钥加密核心方法
     *
     * @param publicKey SM2公钥对象
     * @param data      明文数据 byte[]
     * @return 密文数据 byte[]
     */
    private static byte[] encrypt(BCECPublicKey publicKey, byte[] data) {
        //通过公钥对象获取公钥的基本域参数。
        ECParameterSpec ecParameterSpec = publicKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        //通过公钥值和公钥基本参数创建公钥参数对象
        ECPublicKeyParameters ecPublicKeyParameters = new ECPublicKeyParameters(publicKey.getQ(), ecDomainParameters);
        //根据加密模式实例化SM2公钥加密引擎
        SM2Engine sm2Engine = new SM2Engine();
        //初始化加密引擎
        sm2Engine.init(true, new ParametersWithRandom(ecPublicKeyParameters, new SecureRandom()));
        try {
            //通过加密引擎对字节数串行加密
            return sm2Engine.processBlock(data, 0, data.length);
        } catch (Exception e) {
            throw new RuntimeException("数据加密失败", e);
        }
    }

    /**
     * 使用 SM2 私钥解密数据
     *
     * @param privateKeyHex 私钥(HEX)
     * @param cipherDataHex 密文数据(HEX)
     * @return 解密后数据(HEX)
     */
    public static String decrypt(String privateKeyHex, String cipherDataHex) {
        byte[] result = decrypt(getBCECPrivateKeyByPrivateKeyHex(privateKeyHex), Hex.decode(cipherDataHex));
        return Hex.toHexString(result);
    }

    /**
     * 使用 SM2 私钥解密数据
     *
     * @param privateKeyHex 私钥 HEX 字符串
     * @param cipherDataHex 密文 HEX 字符串
     * @return 解密后的明文数据 HEX 字符串
     */
    public static String decryptWith04(String privateKeyHex, String cipherDataHex) {
        cipherDataHex = "04" + cipherDataHex;
        return decrypt(privateKeyHex, cipherDataHex);
    }

    /**
     * SM2 私钥解密核心方法
     *
     * @param bcecPrivateKey 私钥对象
     * @param cipherData     密文数据
     * @return 解密后的明文 byte[]
     */
    private static byte[] decrypt(BCECPrivateKey bcecPrivateKey, byte[] cipherData) {
        ECParameterSpec ecParameterSpec = bcecPrivateKey.getParameters();
        ECDomainParameters ecDomainParameters = new ECDomainParameters(ecParameterSpec.getCurve(),
                ecParameterSpec.getG(), ecParameterSpec.getN());
        ECPrivateKeyParameters ecPrivateKeyParameters = new ECPrivateKeyParameters(bcecPrivateKey.getD(),
                ecDomainParameters);
        SM2Engine sm2Engine = new SM2Engine();
        sm2Engine.init(false, ecPrivateKeyParameters);

        try {
            return sm2Engine.processBlock(cipherData, 0, cipherData.length);
        } catch (Exception e) {
            throw new RuntimeException("数据解密失败,系统错误", e);
        }
    }


    /**
     * 将 HEX 字符串私钥转换为 BCECPrivateKey 对象
     *
     * @param privateKeyHex 32字节十六进制私钥字符串
     * @return BCECPrivateKey SM2私钥对象
     */
    private static BCECPrivateKey getBCECPrivateKeyByPrivateKeyHex(String privateKeyHex) {
        //将十六进制私钥字符串转换为BigInteger对象
        BigInteger d = new BigInteger(privateKeyHex, 16);
        //通过私钥和私钥域参数集创建椭圆曲线私钥规范
        X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(d, new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN()));
        //通过椭圆曲线私钥规范,创建出椭圆曲线私钥对象(可用于SM2解密和签名)
        return new BCECPrivateKey("EC", ecPrivateKeySpec, BouncyCastleProvider.CONFIGURATION);
    }

    /**
     * 将 HEX 字符串公钥转换为 BCECPublicKey 对象
     *
     * @param pubKeyHex 64 或 65 字节 HEX 公钥字符串(如果公钥字符串为65字节且首个字节为0x04:表示该公钥为非压缩格式,操作时需要删除)
     * @return BCECPublicKey SM2公钥对象
     */
    private static BCECPublicKey getECPublicKeyByPublicKeyHex(String pubKeyHex) {
        //截取64字节有效的SM2公钥(如果公钥首个字节为0x04)
        if (pubKeyHex.length() > 128) {
            pubKeyHex = pubKeyHex.substring(pubKeyHex.length() - 128);
        }
        //将公钥拆分为x,y分量(各32字节)
        String stringX = pubKeyHex.substring(0, 64);
        String stringY = pubKeyHex.substring(stringX.length());
        //将公钥x、y分量转换为BigInteger类型
        BigInteger x = new BigInteger(stringX, 16);
        BigInteger y = new BigInteger(stringY, 16);
        //通过公钥x、y分量创建椭圆曲线公钥规范
        X9ECParameters x9ECParameters = GMNamedCurves.getByName("sm2p256v1");
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(x9ECParameters.getCurve().createPoint(x, y), new ECParameterSpec(x9ECParameters.getCurve(), x9ECParameters.getG(), x9ECParameters.getN()));
        //通过椭圆曲线公钥规范,创建出椭圆曲线公钥对象(可用于SM2加密及验签)
        return new BCECPublicKey("EC", ecPublicKeySpec, BouncyCastleProvider.CONFIGURATION);
    }

    /**
     * SM2算法生成密钥对
     *
     * @return KeyPair 密钥对信息(包含公钥和私钥)
     */
    public static KeyPair generateSm2KeyPair() {
        try {
            final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
            // 获取一个椭圆曲线类型的密钥对生成器
            final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
            SecureRandom random = new SecureRandom();
            // 使用SM2的算法区域初始化密钥生成器
            kpg.initialize(sm2Spec, random);
            // 获取密钥对
            return kpg.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("SM2公私钥对生成失败", e);
        }
    }

    /**
     * SM2算法生成密钥对
     *
     * @return 公钥(HEX) : {@link Pair#getLeft()}; 私钥(HEX) : {@link Pair#getRight()}
     */
    public static Pair<String, String> generateSm2Pair() {
        KeyPair keyPair = SM2Util.generateSm2KeyPair();
        String privateKeyHex = ((BCECPrivateKey) keyPair.getPrivate()).getD().toString(16);
        String publicKeyHex = Hex.toHexString(((BCECPublicKey) keyPair.getPublic()).getQ().getEncoded(false));
        return Pair.of(publicKeyHex, privateKeyHex);
    }
}
