package cn.qingweico.convert;

import java.sql.Date;
import java.sql.Time;

/**
 * @author zqw
 * @date 2025/11/7
 */
public class VarType2Class {
    public static <T> Class<?> change2Class(String type) {

        Class<?> clazz = null;

        switch (type) {
            case "String":
            case "string":
                clazz = String.class;
                break;
            case "int":
                clazz = int.class;
                break;
            case "Integer":
                clazz = Integer.class;
                break;
            case "float":
                clazz = float.class;
                break;
            case "Float":
                clazz = Float.class;
                break;
            case "double":
                clazz = double.class;
                break;
            case "Double":
                clazz = Double.class;
                break;
            case "long":
                clazz = long.class;
                break;
            case "Long":
                clazz = Long.class;
                break;
            case "Object":
                clazz = Object.class;
                break;
            case "Date":
            case "date":
                clazz = Date.class;
                break;
            case "Time":
            case "time":
                clazz = Time.class;
                break;
            case "String[]":
                clazz = String[].class;
                break;
            case "int[]":
                clazz = int[].class;
                break;
            case "float[]":
                clazz = float[].class;
                break;
            case "double[]":
                clazz = double[].class;
                break;
            default:
                break;
        }

        return clazz;
    }
}
