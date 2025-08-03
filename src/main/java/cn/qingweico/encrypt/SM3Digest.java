package cn.qingweico.encrypt;

import java.math.BigInteger;

/**
 * SM3 消息摘要算法实现
 * <p>实现中国国家密码管理局发布的SM3密码杂凑算法</p>
 *
 * @author zqw
 * @date 2025/8/2
 */
public class SM3Digest {
    // SM3输出长度(字节)
    private static final int BYTE_LENGTH = 32;
    // 分组长度(字节)
    private static final int BLOCK_LENGTH = 64;
    // 缓冲区长度
    private static final int BUFFER_LENGTH = 64 * 2;
    // 输入缓冲区
    private final byte[] xBuf = new byte[BUFFER_LENGTH];
    // 缓冲区偏移量
    private int xBufOff;
    // 当前哈希状态
    private byte[] iv = SM3.IV.clone();
    // 已处理分组计数
    private int cntBlock = 0;

    /**
     * 构造函数,初始化SM3摘要计算
     */
    public SM3Digest() {
        this.reset();
    }

    /**
     * 完成摘要计算并输出结果
     *
     * @param out    保存摘要结果的缓冲区
     * @param outOff 缓冲区偏移量
     * @return 摘要长度(固定为32字节)
     */
    public int doFinal(byte[] out, int outOff) {
        byte[] tmp = doFinal();
        System.arraycopy(tmp, 0, out, 0, tmp.length);
        return BYTE_LENGTH;
    }

    /**
     * 获取算法名称
     *
     * @return "SM3"
     */
    public String getAlgorithmName() {
        return "SM3";
    }

    /**
     * 获取摘要长度
     *
     * @return 摘要长度(字节)
     */
    public int getDigestSize() {
        return BYTE_LENGTH;
    }

    /**
     * 重置摘要计算状态
     */
    public void reset() {
        xBufOff = 0;
        cntBlock = 0;
        iv = SM3.IV;
    }


    /**
     * 更新消息摘要
     *
     * @param in    消息输入缓冲区
     * @param inOff 缓冲区偏移量
     * @param len   消息长度
     */
    public void update(byte[] in, int inOff, int len) {
        if (xBufOff + len > BUFFER_LENGTH) {
            int tmpLen = xBufOff + len - BUFFER_LENGTH;
            System.arraycopy(in, inOff, xBuf, xBufOff, BUFFER_LENGTH - xBufOff);
            doUpdate();
            xBufOff = 0;
            int i = 1;
            while (tmpLen > BUFFER_LENGTH) {
                tmpLen -= BUFFER_LENGTH;
                System.arraycopy(in, inOff + BUFFER_LENGTH * i, xBuf, xBufOff, BUFFER_LENGTH - xBufOff);
                doUpdate();
                xBufOff = 0;
                i++;
            }
            System.arraycopy(in, inOff + len - tmpLen, xBuf, xBufOff, tmpLen);
            xBufOff += tmpLen;

        } else if (xBufOff + len == BUFFER_LENGTH) {
            System.arraycopy(in, inOff, xBuf, xBufOff, len);
            doUpdate();
            xBufOff = 0;
        } else {
            System.arraycopy(in, inOff, xBuf, xBufOff, len);
            xBufOff += len;
        }
    }

    /**
     * 处理缓冲区中的数据
     */
    private void doUpdate() {
        byte[] b = new byte[BLOCK_LENGTH];
        for (int i = 0; i < BUFFER_LENGTH; i += BLOCK_LENGTH) {
            System.arraycopy(xBuf, i, b, 0, b.length);
            doHash(b);
        }
        cntBlock += BUFFER_LENGTH / BLOCK_LENGTH;
    }

    /**
     * 执行哈希计算
     *
     * @param B 512位(64字节)数据块
     */
    private void doHash(byte[] B) {
        iv = SM3.compress(iv, B);
    }

    /**
     * 完成最终哈希计算
     *
     * @return 摘要结果
     */
    private byte[] doFinal() {
        byte[] B = new byte[BLOCK_LENGTH];
        byte[] buffer = new byte[xBufOff];
        System.arraycopy(xBuf, 0, buffer, 0, buffer.length);
        byte[] tmp = SM3.padding(buffer, cntBlock);
        for (int i = 0; i < tmp.length; i += BLOCK_LENGTH) {
            System.arraycopy(tmp, i, B, 0, B.length);
            doHash(B);
            cntBlock++;
        }

        return iv;
    }

    /**
     * 计算SM2的ZA值
     *
     * @param x  公钥x坐标
     * @param y  公钥y坐标
     * @param id 用户ID
     * @return ZA哈希值
     */
    private byte[] getSM2Za(byte[] x, byte[] y, byte[] id) {
        byte[] tmp = Util.intToByte(id.length * 8);
        byte[] buffer = new byte[32 * 6 + 2 + id.length];
        buffer[0] = tmp[1];
        buffer[1] = tmp[0];
        byte[] a = Util.getA();
        byte[] b = Util.getB();
        byte[] gx = Util.getGx();
        byte[] gy = Util.getGy();
        int dPos = 2;
        System.arraycopy(id, 0, buffer, dPos, id.length);
        dPos += id.length;
        System.arraycopy(a, 0, buffer, dPos, 32);
        dPos += 32;
        System.arraycopy(b, 0, buffer, dPos, 32);
        dPos += 32;
        System.arraycopy(gx, 0, buffer, dPos, 32);
        dPos += 32;
        System.arraycopy(gy, 0, buffer, dPos, 32);
        dPos += 32;
        System.arraycopy(x, 0, buffer, dPos, 32);
        dPos += 32;
        System.arraycopy(y, 0, buffer, dPos, 32);
        SM3Digest digest = new SM3Digest();
        digest.update(buffer, 0, buffer.length);
        byte[] out = new byte[32];
        digest.doFinal(out, 0);
        return out;
    }

    /**
     * 添加SM2用户标识
     *
     * @param affineX 公钥x坐标
     * @param affineY 公钥y坐标
     * @param id      用户ID
     */
    public void addId(BigInteger affineX, BigInteger affineY, byte[] id) {
        byte[] x = Util.unsigned32ToByteArray(affineX);
        byte[] y = Util.unsigned32ToByteArray(affineY);
        byte[] tmp = getSM2Za(x, y, id);
        reset();
        System.arraycopy(tmp, 0, xBuf, xBufOff, 32);
        xBufOff = 32;
    }
}

/**
 * SM3算法核心实现类
 */
class SM3 {
    public static final byte[] IV = new BigInteger("7380166f4914b2b9172442d7da8a0600a96f30bc163138aae38dee4db0fb0e4e", 16).toByteArray();
    public static final int[] TJ = new int[64];

    static {
        for (int i = 0; i < 16; i++) {
            TJ[i] = 0x79cc4519;
        }
        for (int i = 16; i < 64; i++) {
            TJ[i] = 0x7a879d8a;
        }
    }

    /**
     * 压缩函数
     *
     * @param V 当前哈希状态
     * @param B 消息分组
     * @return 新的哈希状态
     */
    public static byte[] compress(byte[] V, byte[] B) {
        int[] v, b;
        v = convert(V);
        b = convert(B);
        return convert(compress(v, b));
    }

    /**
     * 字节数组转int数组(大端序)
     */
    private static int[] convert(byte[] arr) {
        int[] out = new int[arr.length / 4];
        byte[] tmp = new byte[4];
        for (int i = 0; i < arr.length; i += 4) {
            System.arraycopy(arr, i, tmp, 0, 4);
            out[i / 4] = bigEndianByteToInt(tmp);
        }
        return out;
    }

    /**
     * int数组转字节数组(大端序)
     */
    private static byte[] convert(int[] arr) {
        byte[] out = new byte[arr.length * 4];
        byte[] tmp;
        for (int i = 0; i < arr.length; i++) {
            tmp = bigEndianIntToByte(arr[i]);
            System.arraycopy(tmp, 0, out, i * 4, 4);
        }
        return out;
    }

    /**
     * 核心压缩函数
     */
    public static int[] compress(int[] V, int[] B) {
        int a, b, c, d, e, f, g, h;
        int ss1, ss2, tt1, tt2;
        a = V[0];
        b = V[1];
        c = V[2];
        d = V[3];
        e = V[4];
        f = V[5];
        g = V[6];
        h = V[7];

        int[][] arr = expand(B);
        int[] w = arr[0];
        int[] w1 = arr[1];

        for (int j = 0; j < 64; j++) {
            ss1 = (bitCycleLeft(a, 12) + e + bitCycleLeft(TJ[j], j));
            ss1 = bitCycleLeft(ss1, 7);
            ss2 = ss1 ^ bitCycleLeft(a, 12);
            tt1 = FFj(a, b, c, j) + d + ss2 + w1[j];
            tt2 = GGj(e, f, g, j) + h + ss1 + w[j];
            d = c;
            c = bitCycleLeft(b, 9);
            b = a;
            a = tt1;
            h = g;
            g = bitCycleLeft(f, 19);
            f = e;
            e = P0(tt2);

        }

        int[] out = new int[8];
        out[0] = a ^ V[0];
        out[1] = b ^ V[1];
        out[2] = c ^ V[2];
        out[3] = d ^ V[3];
        out[4] = e ^ V[4];
        out[5] = f ^ V[5];
        out[6] = g ^ V[6];
        out[7] = h ^ V[7];

        return out;
    }

    /**
     * 消息扩展函数
     */
    private static int[][] expand(byte[] B) {
        int W[] = new int[68];
        int W1[] = new int[64];
        // 前16个字
        byte[] tmp = new byte[4];
        for (int i = 0; i < B.length; i += 4) {
            System.arraycopy(B, i, tmp, 0, 4);
            W[i / 4] = bigEndianByteToInt(tmp);
        }

        for (int i = 16; i < 68; i++) {
            W[i] = P1(W[i - 16] ^ W[i - 9] ^ bitCycleLeft(W[i - 3], 15)) ^ bitCycleLeft(W[i - 13], 7) ^ W[i - 6];
        }

        for (int i = 0; i < 64; i++) {
            W1[i] = W[i] ^ W[i + 4];
        }

        return new int[][]{W, W1};
    }

    private static int[][] expand(int[] B) {
        return expand(convert(B));
    }

    private static byte[] bigEndianIntToByte(int num) {
        return back(Util.intToByte(num));
    }

    private static int bigEndianByteToInt(byte[] bytes) {
        return Util.byteToInt(back(bytes));
    }

    // 布尔函数FFj
    private static int FFj(int X, int Y, int Z, int j) {
        if (j >= 0 && j <= 15) {
            return FF1j(X, Y, Z);
        } else {
            return FF2j(X, Y, Z);
        }
    }

    // 布尔函数GGj
    private static int GGj(int X, int Y, int Z, int j) {
        if (j >= 0 && j <= 15) {
            return GG1j(X, Y, Z);
        } else {
            return GG2j(X, Y, Z);
        }
    }

    /***********************************************/
    // 逻辑位运算函数
    private static int FF1j(int X, int Y, int Z) {
        return X ^ Y ^ Z;
    }

    private static int FF2j(int X, int Y, int Z) {
        return ((X & Y) | (X & Z) | (Y & Z));
    }

    private static int GG1j(int X, int Y, int Z) {
        return X ^ Y ^ Z;
    }

    private static int GG2j(int X, int Y, int Z) {
        return (X & Y) | (~X & Z);
    }

    // 置换函数P0
    private static int P0(int X) {
        return X ^ bitCycleLeft(X, 9) ^ bitCycleLeft(X, 17);
    }

    // 置换函数P1
    private static int P1(int X) {
        return X ^ bitCycleLeft(X, 15) ^ bitCycleLeft(X, 23);
    }

    /**
     * 消息填充
     *
     * @param in   最后一个分组
     * @param bLen 已处理分组数
     * @return 填充后的消息
     */
    public static byte[] padding(byte[] in, int bLen) {
        //第一bit为1 所以长度=8 * in.length+1 k为所补的bit k+1/8 为需要补的字节
        int k = 448 - (8 * in.length + 1) % 512;
        if (k < 0) {
            k = 960 - (8 * in.length + 1) % 512;
        }
        k += 1;
        byte[] pad = new byte[k / 8];
        pad[0] = (byte) 0x80;
        long n = in.length * 8L + bLen * 512L;
        //64/8 字节 长度
        //k/8 字节padding
        byte[] out = new byte[in.length + k / 8 + 64 / 8];
        int pos = 0;
        System.arraycopy(in, 0, out, 0, in.length);
        pos += in.length;
        System.arraycopy(pad, 0, out, pos, pad.length);
        pos += pad.length;
        byte[] tmp = back(Util.longToByte(n));
        System.arraycopy(tmp, 0, out, pos, tmp.length);

        return out;
    }

    /**
     * 字节数组逆序（大小端转换）
     * <p>将字节数组的顺序完全反转,用于大端序和小端序之间的转换</p>
     *
     * @param in 输入字节数组
     * @return 逆序后的字节数组
     * <pre>{@code
     * byte[] data = {0x01, 0x02, 0x03, 0x04};
     * byte[] reversed = back(data); // 结果为 {0x04, 0x03, 0x02, 0x01}
     * }</pre>
     */
    private static byte[] back(byte[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = in[out.length - i - 1];
        }

        return out;
    }

    /**
     * 整数位循环左移
     * <p>将整数的二进制表示循环左移指定位数,超出部分从右侧补位</p>
     *
     * @param n      要移位的整数
     * @param bitLen 循环左移的位数（自动模32处理）
     * @return 移位后的整数
     * @implNote 实现步骤：
     * <ol>
     *   <li>将整数转换为大端序字节数组</li>
     *   <li>先处理字节级移位（每次移动8位）</li>
     *   <li>再处理剩余位级移位（小于8位）</li>
     *   <li>将结果转换回整数</li>
     * </ol>
     * <pre>{@code
     * int num = 0b11000000_00000000_00000000_00000001; // -1073741823
     * int shifted = bitCycleLeft(num, 1); // 结果为 0b10000000_00000000_00000000_00000011
     * }</pre>
     */
    private static int bitCycleLeft(int n, int bitLen) {
        bitLen %= 32;
        byte[] tmp = bigEndianIntToByte(n);
        int byteLen = bitLen / 8;
        int len = bitLen % 8;
        if (byteLen > 0) {
            tmp = byteCycleLeft(tmp, byteLen);
        }

        if (len > 0) {
            tmp = bitSmall8CycleLeft(tmp, len);
        }

        return bigEndianByteToInt(tmp);
    }

    /**
     * 字节内位循环左移（辅助方法）
     *
     * @param in  输入字节数组
     * @param len 循环左移的位数（1-7）
     * @return 移位后的字节数组
     */
    private static byte[] bitSmall8CycleLeft(byte[] in, int len) {
        byte[] tmp = new byte[in.length];
        int t1, t2, t3;
        for (int i = 0; i < tmp.length; i++) {
            t1 = (byte) ((in[i] & 0x000000ff) << len);
            t2 = (byte) ((in[(i + 1) % tmp.length] & 0x000000ff) >> (8 - len));
            t3 = (byte) (t1 | t2);
            tmp[i] = (byte) t3;
        }
        return tmp;
    }

    /**
     * 字节数组循环左移（辅助方法）
     *
     * @param in      输入字节数组
     * @param byteLen 循环左移的字节数
     * @return 移位后的字节数组
     */
    private static byte[] byteCycleLeft(byte[] in, int byteLen) {
        byte[] tmp = new byte[in.length];
        System.arraycopy(in, byteLen, tmp, 0, in.length - byteLen);
        System.arraycopy(in, 0, tmp, in.length - byteLen, byteLen);

        return tmp;
    }

}

class Util {
    // SM2椭圆曲线参数
    private static BigInteger p = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFF", 16);
    private static BigInteger a = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF00000000FFFFFFFFFFFFFFFC", 16);
    private static BigInteger b = new BigInteger("28E9FA9E9D9F5E344D5A9E4BCF6509A7F39789F515AB8F92DDBCBD414D940E93", 16);
    private static BigInteger n = new BigInteger("FFFFFFFEFFFFFFFFFFFFFFFFFFFFFFFF7203DF6B21C6052B53BBF40939D54123", 16);
    private static BigInteger Gx = new BigInteger("32C4AE2C1F1981195F9904466A39C9948FE30BBFF2660BE1715A4589334C74C7", 16);
    private static BigInteger Gy = new BigInteger("BC3736A2F4F6779C59BDCEE36B692153D0A9877CC62A474002DF32E52139F0A0", 16);

    // 获取曲线参数
    private static byte[] getP() {
        return unsigned32ToByteArray(p);
    }

    public static byte[] getA() {
        return unsigned32ToByteArray(a);
    }

    public static byte[] getB() {
        return unsigned32ToByteArray(b);
    }

    public static byte[] getN() {
        return unsigned32ToByteArray(n);
    }

    public static byte[] getGx() {
        return unsigned32ToByteArray(Gx);
    }

    public static byte[] getGy() {
        return unsigned32ToByteArray(Gy);
    }

    /**
     * 将int转换为字节数组(小端序)
     */
    public static byte[] intToByte(int num) {
        byte[] bytes = new byte[4];

        bytes[0] = (byte) (0xff & (num >> 0));
        bytes[1] = (byte) (0xff & (num >> 8));
        bytes[2] = (byte) (0xff & (num >> 16));
        bytes[3] = (byte) (0xff & (num >> 24));

        return bytes;
    }

    /**
     * 将字节数组(小端序)转换为int
     */
    public static int byteToInt(byte[] bytes) {
        int num = 0;
        int temp;
        temp = (0x000000ff & (bytes[0])) << 0;
        num = num | temp;
        temp = (0x000000ff & (bytes[1])) << 8;
        num = num | temp;
        temp = (0x000000ff & (bytes[2])) << 16;
        num = num | temp;
        temp = (0x000000ff & (bytes[3])) << 24;
        num = num | temp;

        return num;
    }

    /**
     * 将long转换为字节数组
     */
    public static byte[] longToByte(long num) {
        byte[] bytes = new byte[8];

        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (0xff & (num >> (i * 8)));
        }

        return bytes;
    }

    /**
     * 将大整数转换为32字节无符号数组
     */
    public static byte[] unsigned32ToByteArray(BigInteger n) {
        return unsignedXToByteArray(n, 32);
    }

    public static byte[] unsignedXToByteArray(BigInteger x, int length) {
        if (x == null) {
            return null;
        }

        byte[] tmp = new byte[length];
        int len = x.toByteArray().length;
        if (len > length + 1) {
            return null;
        }

        if (len == length + 1) {
            if (x.toByteArray()[0] != 0) {
                return null;
            } else {
                System.arraycopy(x.toByteArray(), 1, tmp, 0, length);
                return tmp;
            }
        } else {
            System.arraycopy(x.toByteArray(), 0, tmp, length - len, len);
            return tmp;
        }

    }
}
