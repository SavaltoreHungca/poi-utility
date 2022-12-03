package com.zxslsoft.general.utility.poi;

import java.lang.annotation.*;

/**
 * 用于 {@link ExcelHeader}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ValueMap {
    String key();
    String value();
}


