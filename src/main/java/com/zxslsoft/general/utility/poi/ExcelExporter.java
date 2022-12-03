package com.zxslsoft.general.utility.poi;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.zxslsoft.general.utility.poi.Utils.*;

/**
 * Excel 处理工具, 示例 看 该类的main方法
 */
public class ExcelExporter<T> {

    private List<String> sheetNames = new ArrayList<>();
    private final Workbook workbook = new XSSFWorkbook();
    private final List<Sheet> sheets = new ArrayList<>();
    private List<List<List<T>>> dataSet;
    private CellValueFormat cellValueFormat = ExcelUtils.DEFAULT_SET_VALUE_MANEL;
    private CellFormat cellFormat = null;
    private SheetFormat sheetFormat = null;
    private final Map<Integer, Set<Integer>> hiddenColumns = new HashMap<>();

    private boolean autoResizeColumnWidth = true; // 是否自动调整列宽


    /**
     * 设置数据集， 适用于多个 sheet 页
     */
    public ExcelExporter<T> setDataSet(List<List<List<T>>> dataSet) {
        this.dataSet = dataSet;
        return this;
    }

    /**
     * 设置数据集, 当仅有一个 sheet 页的时候适用
     */
    public ExcelExporter<T> setSingleSheetDataSet(List<List<T>> dataSet) {
        this.dataSet = new ArrayList<>();
        this.dataSet.add(dataSet);
        return this;
    }

    /**
     * 自动调整列宽
     */
    public ExcelExporter<T> setAutoResizeColumnWidth(boolean autoResize) {
        this.autoResizeColumnWidth = autoResize;
        return this;
    }

    /**
     * 设置多少个 sheet 的名字
     */
    public ExcelExporter<T> setSheetNames(String... names) {
        this.sheetNames = asList(names);
        return this;
    }

    /**
     * 决定如何设置单元格的值
     */
    public ExcelExporter<T> setCellValueFormat(CellValueFormat setCellValue) {
        this.cellValueFormat = setCellValue;
        return this;
    }

    /**
     * 决定每个单元格或多个单元格的样式
     */
    public ExcelExporter<T> setCellFormat(CellFormat cellFormat) {
        this.cellFormat = cellFormat;
        return this;
    }

    public ExcelExporter<T> setSheetFormat(SheetFormat sheetFormat) {
        this.sheetFormat = sheetFormat;
        return this;
    }

    /**
     * 设置隐藏列
     */
    public ExcelExporter<T> setHiddenColumns(Integer sheetIndex, Integer... columns) {
        this.hiddenColumns.computeIfAbsent(sheetIndex, _k -> new HashSet<>()).addAll(Utils.asSet(columns));
        return this;
    }

    /**
     * 基础的数据集设置好了后，导出为 byte[] 数据
     */
    public byte[] export() {
        int sheetIndex = -1;
        for (List<List<T>> sheetData : this.dataSet) {
            sheetIndex++;
            if (sheetIndex < sheetNames.size()) {
                sheets.add(workbook.createSheet(sheetNames.get(sheetIndex)));
            } else {
                sheets.add(workbook.createSheet());
            }
            Sheet sheet = sheets.get(sheetIndex);
            sheet.createDrawingPatriarch();
            int rowIndex = 0;
            int maxColIndex = 0;
            for (List<T> rowList : nullSafe(sheetData)) {
                Row row = sheet.createRow(rowIndex);
                int colIndex = 0;
                for (Object cellValue : nullSafe(rowList)) {
                    Cell cell = row.createCell(colIndex);
                    String ret = this.cellValueFormat.accept(sheetIndex, rowIndex, colIndex,
                            workbook, sheet, row, cell, cellValue);
                    if (!isEmptyString(ret)) {
//                        if (Utils.isNumber(ret)){
//                            cell.setCellValue( ret.contains(".")?Double.parseDouble(ret):Integer.parseInt(ret));
//                        }else {
//                            cell.setCellValue(ret);
//                        }
                        cell.setCellValue(ret);
                    }
                    maxColIndex = Math.max(maxColIndex, colIndex);
                    colIndex++;
                }

                rowIndex++;
            }

            if (!isEmptyObject(cellFormat)) {
                rowIndex = 0;
                for (List<T> rowList : nullSafe(sheetData)) {
                    Row row = sheet.getRow(rowIndex);
                    int colIndex = 0;
                    for (Object cellValue : nullSafe(rowList)) {
                        Cell cell = row.getCell(colIndex);

                        if (hiddenColumns.get(sheetIndex) == null || !hiddenColumns.get(sheetIndex).contains(colIndex)) {
                            cellFormat.accept(sheetIndex, rowIndex, colIndex,
                                    workbook, sheet, row, cell, cellValue);
                        }

                        colIndex++;
                    }
                    rowIndex++;
                }
            }

            if (hiddenColumns.get(sheetIndex) != null) {
                hiddenColumns.get(sheetIndex).forEach(ite -> {
                    sheet.setColumnHidden(ite, true);
                });
            }

            if (autoResizeColumnWidth) {
                for (int i = 0; i <= maxColIndex; i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(255 * 256, sheet.getColumnWidth(i) * 17 / 10));
//                    sheet.setColumnWidth(i, sheet.getColumnWidth(i) * 17 / 10);
                }
            }

            if (sheetFormat != null) {
                sheetFormat.accept(workbook, sheet, sheetData.size() - 1, maxColIndex);
            }
        }

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            workbook.write(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 通过一个DTO来导出模板
     */
    public static <R> byte[] exportDtoTemplate(Class<R> clazz) {
        List<String> headers = ExcelUtils.getDtoHeaders(clazz);
        Map<String, Map<String, String>> optional = ExcelUtils.getHeaderOptionalValue(clazz);

        List<List<Object>> content = new ArrayList<>();
        List<Object> hs = new ArrayList<>();
        content.add(hs);
        for (int i = 0; i < headers.size(); i++) {
            String headerName = headers.get(i);
            hs.add(headerName);

            List<Object> columnDefaultValues = ExcelUtils.getColumnDefaultValues(clazz, headerName);
            for (int j = 0; j < columnDefaultValues.size(); j++) {
                while (content.size() <= j + 1) {
                    content.add(new ArrayList<>());
                }
                List<Object> row = content.get(j + 1);
                while (row.size() <= i - 1) {
                    row.add("");
                }
                row.add(columnDefaultValues.get(j));
            }
        }

        return new ExcelExporter<Object>()
                .setSingleSheetDataSet(content)
                .setCellFormat((int sheetIndex, int rowIndex, int colIndex,
                                Workbook workbook, Sheet sheet, Row row, Cell cell, Object value) -> {
                    if (rowIndex != 0) return;
                    String headerName = String.valueOf(value);
                    if (null != optional) {
                        Map<String, String> options = optional.get(headerName);
                        if (!Utils.isEmpty(options)) {
                            ExcelUtils.setOptionalList((XSSFSheet) sheet, options.keySet(), 1, 500, colIndex, colIndex);
                        }
                    }

                    if (ExcelUtils.isHiddenColumn(headerName, clazz)) {
                        sheet.setColumnHidden(colIndex, true);
                    }
                })
                .export();
    }

    public interface CellValueFormat {
        /**
         * 如果为该单元格设置图片则返回空
         */
        String accept(int sheetIndex, int rowIndex, int colIndex,
                      Workbook workbook, Sheet sheet, Row row, Cell cell, Object value);
    }

    public interface CellFormat {
        /**
         * 为单元格设置样式
         */
        void accept(int sheetIndex, int rowIndex, int colIndex,
                    Workbook workbook, Sheet sheet, Row row, Cell cell, Object value);
    }

    public interface SheetFormat {
        void accept(Workbook workbook, Sheet sheet, int maxRowIndex, int maxColIndex);
    }

    // 示例：导出图片
    @SuppressWarnings("all")
    public static void demo_export_pic() {
        byte[] pic = new byte[]{1}; // 某张图片
        List<List<Object>> dataSet = asList(
                asList("姓名", "得分"),
                asList("米修斯", "满分", pic, pic),
                asList("哈哈", "1")
        );
        byte[] data = new ExcelExporter()
                .setSheetNames("xixi")
                .setSingleSheetDataSet(dataSet)
                .setCellValueFormat(ExcelUtils.SET_PIC) // 可以不用设置
                .export();
        FileUtils.saveFile(data, FileUtils.getUserHome(), String.format("export%S.xlsx", new Date().getTime()));
    }

    // 示例：导出模板
    public static void demo_export_template() {
        byte[] data = ExcelExporter.exportDtoTemplate(TestExportTemplate.class);
        FileUtils.saveFile(data, FileUtils.getUserHome(), String.format("export%S.xlsx", new Date().getTime()));
    }


    public static class TestExportTemplate {

        @ExcelHeader(
                value = "名字",
                defaultValues = {"唐三", "小舞"},
                order = 0
        )
        private String name;

        @ExcelHeader(value = "年龄", order = 1, defaultValues = {"19", "NaN"})
        private Integer age;

        @ExcelHeader(
                value = "性别",
                valueMap = {
                        @ValueMap(key = "男", value = "man"),
                        @ValueMap(key = "女", value = "female")
                },
                defaultValues = {"男", "女"}
        )
        private String gender;

        @ExcelHeader(
                value = "所属区域",
                columnDefaultValues = CountryList.class,
                valueMapClass = CountryValueMap.class
        )
        private String region;

    }

    public static class CountryList implements ColumnDefaultValueInterface {
        @Override
        public List<Object> defaultValueList() {
            return asList("斗罗大陆", "星斗大森林");
        }
    }

    public static class CountryValueMap implements ValueMapInterface {
        @Override
        public Map<String, String> getValueMap() {
            return asMap(
                    "斗罗大陆", "Big Place",
                    "星斗大森林", "Very Big Place",
                    "小山村", "Village"
            );
        }
    }
}
