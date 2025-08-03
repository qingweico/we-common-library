package cn.qingweico.supplier;

import com.github.javafaker.Faker;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.math3.util.Precision;

import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Using faker to generate some random data, such as random number, address, name, etc.
 *
 * @author zqw
 * @date 2022/06/20
 */
public class RandomDataGenerator {
    private final static Random R = new Random();
    private final static ThreadLocalRandom TLR = ThreadLocalRandom.current();

    public static Faker faker = null;
    public static Faker localFaker = null;

    static {
        try {
            faker = new LazyInitializer<Faker>() {
                @Override
                protected Faker initialize() {
                    return new Faker();
                }
            }.get();
        } catch (ConcurrentException e) {
            //
        }
        try {
            localFaker = new LazyInitializer<Faker>() {
                @Override
                protected Faker initialize() {
                    return new Faker(Locale.CHINA);
                }
            }.get();
        } catch (ConcurrentException e) {
            //
        }
    }

    /**
     * 检查Faker是否初始化成功
     *
     * @throws IllegalStateException 如果Faker未正确初始化
     */
    private static void checkFakerInitialization() {
        if (faker == null || localFaker == null) {
            throw new IllegalStateException("Faker initialization failed");
        }
    }

    /**
     * 生成随机姓名
     *
     * @param isChinese 是否生成中文姓名
     * @return 随机生成的全名字符串
     */
    public static String name(boolean isChinese) {
        checkFakerInitialization();
        return isChinese ? localFaker.name().fullName() : faker.name().fullName();
    }

    /**
     * 生成随机英文姓名
     * <p>等效于调用 {@link #name(boolean) name(false)}</p>
     *
     * @return 随机生成的英文全名
     * @throws IllegalStateException 如果Faker未正确初始化
     * @see #name(boolean)
     */
    public static String name() {
        return name(false);
    }

    /**
     * 生成随机电话号码
     *
     * @param isChinese 是否生成中国格式电话号码
     * @return 随机生成的电话号码字符串
     */
    public static String phone(boolean isChinese) {
        String ret;
        if (isChinese) {
            ret = localFaker.phoneNumber().phoneNumber();
        } else {
            ret = faker.phoneNumber().phoneNumber();
        }
        return ret;
    }

    /**
     * 生成随机国际电话号码
     * <p>等效于调用 {@link #phone(boolean) phone(false)}</p>
     *
     * @return 随机生成的国际电话号码
     * @throws IllegalStateException 如果Faker未正确初始化
     * @see #phone(boolean)
     */
    public static String phone() {
        return phone(false);
    }


    /**
     * 生成随机地址信息
     *
     * @param isChinese 是否生成中文格式地址
     * @return 随机生成的完整地址字符串
     * @throws IllegalStateException 如果Faker未正确初始化
     */
    public static String address(boolean isChinese) {
        checkFakerInitialization();
        return isChinese ? localFaker.address().fullAddress() : faker.address().fullAddress();
    }

    /**
     * 生成随机国际格式地址
     *
     * @return 随机生成的国际格式地址字符串
     * @throws IllegalStateException 如果Faker未正确初始化
     */
    public static String address() {
        return address(false);
    }


    /**
     * 生成随机生日日期
     *
     * @param isChinese 是否生成适合中国地区的生日日期
     * @return 随机生成的生日日期对象
     * @throws IllegalStateException 如果Faker未正确初始化
     */
    public static Date birthday(boolean isChinese) {
        checkFakerInitialization();
        return isChinese ? localFaker.date().birthday() : faker.date().birthday();
    }

    /**
     * 生成随机国际格式生日日期
     *
     * @return 随机生成的生日日期对象
     * @throws IllegalStateException 如果Faker未正确初始化
     */
    public static Date birthday() {
        return birthday(false);
    }

    /**
     * 生成0-99范围内的随机整数
     *
     * @return 0-99范围内的随机整数
     */
    public static int rndInt() {
        return R.nextInt(100);
    }


    /**
     * 生成指定范围内的随机整数
     *
     * @param exclusive 随机数的上界(不包含)
     * @return 0到bound-1范围内的随机整数
     * @throws IllegalArgumentException 如果bound不是正数
     */
    public static int rndInt(int exclusive) {
        if (exclusive <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        return R.nextInt(exclusive);
    }

    /**
     * 生成0.0-1.0之间的随机双精度浮点数,并四舍五入保留两位小数
     *
     * @return 0.0-1.0范围内的随机双精度浮点数, 精度保留两位小数
     */
    public static double rndDouble() {
        double v = TLR.nextDouble();
        return Precision.round(v, 2);
    }

    /**
     * 生成0-bound范围的随机双精度浮点数,并四舍五入保留两位小数
     *
     * @param exclusive 随机数的上界(必须 > 0)
     * @return 0-bound范围内的随机数,精度保留两位小数
     * @throws IllegalArgumentException 如果bound ≤ 0
     */
    public static double rndDouble(double exclusive) {
        if (exclusive <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        double v = TLR.nextDouble(exclusive);
        return Precision.round(v, 2);
    }

    /**
     * 生成0.0f-1.0f范围内的随机单精度浮点数,四舍五入保留两位小数
     *
     * @return 范围在[0.00, 1.00]之间的随机float值
     */
    public static float rndFloat() {
        float v = TLR.nextFloat();
        return Precision.round(v, 2);
    }


    /**
     * 生成0-bound范围内的随机单精度浮点数
     *
     * @param exclusive 随机数的上界
     * @return 0-bound范围内的随机单精度浮点数
     * @throws IllegalArgumentException 如果bound不是正数
     */
    public static float rndFloat(int exclusive) {
        if (exclusive <= 0) {
            throw new IllegalArgumentException("Bound must be positive");
        }
        return TLR.nextFloat() * exclusive;
    }

    /**
     * 生成随机布尔值
     *
     * @return 随机布尔值
     */
    public static boolean rndBoolean() {
        return R.nextBoolean();
    }

    /**
     * 生成随机0或1字符串
     *
     * @return "0"或"1"的字符串
     */
    public static String zeroOrOne() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(2));
    }
}
