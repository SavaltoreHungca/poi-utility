package com.zxslsoft.general.utility.poi;

import java.lang.annotation.*;

/**
 * 使用 DTO 导出模板时，设置标题信息
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ExcelHeader {

    // 标题名
    String value();

    // 标题的排序
    int order() default Integer.MAX_VALUE;

    // 是否隐藏该列
    boolean hidden() default false;

    // 导入时，进行键值对映射
    // 导出时，设置下拉选项值
    // key 为excel中显示的值， value 为DTO中存的值
    ValueMap[] valueMap() default {};

    // 导入时， 进行值映射， 实现类的返回值类型为 Map
    // 导出时，设置下拉选项值
    Class<? extends ValueMapInterface> valueMapClass() default ValueMapInterface.class;

    // 导出模板时， 设定模板下拉可选值
    Class<? extends ColumnDefaultValueInterface> columnDefaultValues() default ColumnDefaultValueInterface.class;

    String[] defaultValues() default {};
}
