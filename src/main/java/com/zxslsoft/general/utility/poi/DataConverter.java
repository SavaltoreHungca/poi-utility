package com.zxslsoft.general.utility.poi;

import java.math.BigDecimal;

public class DataConverter {

    @SuppressWarnings("unchecked")
    public static <T> T convertString(String str, Class<T> clzz){
        if (Integer.class.equals(clzz)){
            return (T) new Integer(str);
        }
        if (Long.class.equals(clzz)){
            return (T) new Long(str);
        }
        if (String.class.equals(clzz)){
            return (T) str;
        }
        if (Character.class.equals(clzz)){
            return (T) new Character(str.toCharArray()[0]);
        }
        if (Byte.class.equals(clzz)){
            return (T) new Byte(str);
        }
        if (BigDecimal.class.equals(clzz)){
            return (T) new BigDecimal(str);
        }
        if(Boolean.class.equals(clzz)){
            return (T) Boolean.valueOf(str);
        }
        throw new RuntimeException(String.format("%s can't convert to type %s", str, clzz.getName()));
    }

}
