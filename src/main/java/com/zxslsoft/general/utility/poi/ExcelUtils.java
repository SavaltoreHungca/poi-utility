package com.zxslsoft.general.utility.poi;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFDataValidation;
import org.apache.poi.xssf.usermodel.XSSFDataValidationConstraint;
import org.apache.poi.xssf.usermodel.XSSFDataValidationHelper;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import java.lang.reflect.Field;
import java.util.*;

import static com.zxslsoft.general.utility.poi.Utils.*;

public class ExcelUtils {

    static Map<Class<?>, Map<String, Field>> headerNameCache = new HashMap<>();

    public static String getCellPosition(Cell cell){
        return CellReference.convertNumToColString(cell.getColumnIndex()) + (cell.getRow().getRowNum() + 1);
    }

    public static void setErrorTips(Workbook workbook, String str, Cell r, IndexedColors backgroundColor){
        if(Utils.isEmptyString(getCellValue(r))) return;
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFillForegroundColor(backgroundColor.index);
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        r.setCellStyle(cellStyle);
        ExcelUtils.setPrompt(r, str);
    }

    public static Cell safeGetCell(Row row, Integer index){
        try{
            return row.getCell(index);
        }catch (NullPointerException e){
            return null;
        }
    }

    public static void setErrorTips(Workbook workbook, String str, Cell r){
        setErrorTips(workbook, str, r, IndexedColors.RED);
    }

    // 接收导入excel的DTO设置值
    public static void setValue(Object dto, String headerName, String value) {
        Map<String, Field> nameMap = getHeaderMap(dto.getClass());
        Field field = nameMap.get(headerName);
        if (null != field) {
            try {
                field.setAccessible(true);
                field.set(dto, DataConverter.convertString(value, field.getType()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static List<Integer> getNotEmptyColIndex(Row row, Set<Integer> skipCol) {
        List<Integer> ans = new ArrayList<>();

        skipCol = Utils.nullSafe(skipCol);

        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext()) {
            Cell cell = cellIterator.next();

            if (!skipCol.contains(cell.getColumnIndex()) && !Utils.isEmptyString(new DataFormatter().formatCellValue(cell))) {
                ans.add(cell.getColumnIndex());
            }
        }
        return ans;
    }

    // 获取DTO对应excel标题的字段
    @SuppressWarnings("all")
    public static <T> List<String> getDtoHeaders(Class<T> clazz) {
        Map<String, Field> nameMap = getHeaderMap(clazz);
        List<String> ans = new ArrayList<>(nameMap.keySet());
        ans.sort((k1, k2) -> {
            int k1_order = (int) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, nameMap.get(k1), "order");
            int k2_order = (int) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, nameMap.get(k2), "order");
            return k1_order - k2_order;
        });
        return ans;
    }

    // 获取excel标题和DTO对应的字段
    private static <T> Map<String, Field> getHeaderMap(Class<T> clazz) {
        return headerNameCache.computeIfAbsent(clazz, k -> {
            List<Field> fields = ReflectUtils.getFieldsAnnotatedWith(clazz, ExcelHeader.class);
            return Utils.getIdMap(fields, field ->
                    (String) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, field, "value"));
        });
    }

    // 为excel某列设置下拉选项值
    public static void setOptionalList(XSSFSheet sheet, Collection<String> options, int firstRow, int lastRow, int firstCol, int lastCol) {
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);
        XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint) dvHelper
                .createExplicitListConstraint(options.toArray(new String[]{}));
        CellRangeAddressList addressList = null;
        XSSFDataValidation validation = null;

        addressList = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        validation = (XSSFDataValidation) dvHelper.createValidation(
                dvConstraint, addressList);
        validation.setShowPromptBox(true);
        sheet.addValidationData(validation);
    }

    public static void setOptionalList(XSSFSheet sheet, int firstRow, int lastRow, int firstCol, int lastCol,
                                       XSSFSheet sourceSheet, Integer sourceFirstRow, Integer sourceLastRow, Integer sourceCol) {
        String colStr = CellReference.convertNumToColString(sourceCol);
        String strFormula = sourceSheet.getSheetName() + "!$" + colStr + "$" + (sourceFirstRow + 1) + ":$" + colStr + "$" + (sourceLastRow + 1);
        XSSFDataValidationConstraint constraint = new XSSFDataValidationConstraint(DataValidationConstraint.ValidationType.LIST, strFormula);
        CellRangeAddressList regions = new CellRangeAddressList(firstRow, lastRow, firstCol, lastCol);
        DataValidationHelper help = new XSSFDataValidationHelper(sheet);
        DataValidation validation = help.createValidation(constraint, regions);
        sheet.addValidationData(validation);
    }

    // 设置 DTO 某个字段 和 excel 某列的值映射关系
    @SuppressWarnings("all")
    public static <T> Map<String, Map<String, String>> getHeaderOptionalValue(Class<T> clazz) {
        Map<String, Field> headerMap = getHeaderMap(clazz);
        if (null == headerMap) return null;

        Map<String, Map<String, String>> ans = new HashMap<>();
        headerMap.forEach((k, v) -> {
            ValueMap[] valueMaps = (ValueMap[]) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, v, "valueMap");
            Class valueMapClass = (Class) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, v, "valueMapClass");
            if (!Utils.isEmpty(valueMaps)) {
                for (ValueMap value : valueMaps) {
                    ans.computeIfAbsent(k, key -> new HashMap<>()).put(value.key(), value.value());
                }
            } else if (!ValueMapInterface.class.equals(valueMapClass)) {
                try {
                    ValueMapInterface o = ReflectUtils.newInstance(valueMapClass);
                    ans.put(k, o.getValueMap());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        if (Utils.isEmpty(ans)) return null;

        return ans;
    }

    @SuppressWarnings("all")
    public static boolean isHiddenColumn(String headerName, Class<?> clazz) {
        return (boolean) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, getHeaderMap(clazz).get(headerName), "hidden");
    }

    // 导出excel模板时， 为其设置默认值
    public static List<Object> getColumnDefaultValues(Class<?> clazz, String headerName) {
        Map<String, Field> headerMap = getHeaderMap(clazz);
        try {
            String[] defaultValues = (String[]) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, headerMap.get(headerName), "defaultValues");
            if (!Utils.isEmpty(defaultValues)) {
                return asList(defaultValues);
            }
            Class<?> defaultValueClazz = (Class<?>) ReflectUtils.getAnotationValueOfField(ExcelHeader.class, headerMap.get(headerName), "columnDefaultValues");
            if (ColumnDefaultValueInterface.class.equals(defaultValueClazz)) {
                return new ArrayList<>(0);
            }
            ColumnDefaultValueInterface o = ReflectUtils.newInstance(defaultValueClazz);
            return o.defaultValueList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 将数据集的某一列设置为某个值
    public static <T> void setColumnValue(List<List<T>> dataSet, Integer columnIndex, List<T> columnData, AddType addType, Integer... skipRows) {
        Set<Integer> skipRowSet = Utils.asSet(skipRows);

        int rowLen;
        if (!Utils.isEmpty(skipRowSet)) {
//            int max = Utils.max(skipRowSet) + 1;
//            int needFill = max - skipRowSet.size();
//
//            if (needFill <= columnData.size()) {
//                rowLen = columnData.size();
//            } else {
//                rowLen = max + columnData.size() - needFill;
//            }
            int max = Utils.max(skipRowSet);
            rowLen = Math.max(max, columnData.size());
        } else {
            rowLen = columnData.size();
        }

        int capedIndex = 0;
        for (int i = 0; i < rowLen; i++) {
            if (skipRowSet.contains(i)) {
                continue;
            }
            List<T> row;
            try {
                row = dataSet.get(i);
            } catch (Exception e) {
                row = new ArrayList<>();
                Utils.fillCollection(dataSet, new ArrayList<>(), i + 1);
                dataSet.set(i, row);
            }
            switch (addType) {
                    case OVERRIDE: {
                        Utils.fillCollection(row, null, columnIndex + 1);
                        if (capedIndex < columnData.size()) {
                            row.set(columnIndex, columnData.get(capedIndex));
                            capedIndex++;
                        } else {
                            row.set(columnIndex, null);
                        }
                        break;
                    }
                    case INSERT_BEFORE: {
                        Utils.fillCollection(row, null, columnIndex);
                        if (capedIndex < columnData.size()) {
                            row.add(columnIndex, columnData.get(capedIndex));
                            capedIndex++;
                        } else {
                            row.add(columnIndex, null);
                        }
                        break;
                    }
                    case INSERT_AFTER: {
                        Utils.fillCollection(row, null, columnIndex + 1);
                        if (capedIndex < columnData.size()) {
                            row.add(columnIndex + 1, columnData.get(capedIndex));
                            capedIndex++;
                        } else {
                            row.add(columnIndex + 1, null);
                        }
                        break;
                    }
                    default:
                        // row.add(columnIndex + 1, null);
                        break;
                }
        }
    }

    public static boolean isDateCell(Cell cell) {
        return CellType.NUMERIC.equals(cell.getCellType()) && DateUtil.isCellDateFormatted(cell);
    }

    // 如果单元格的类型为日期，则将单元格内的日期格式设置为指定类型
    public static String getFormatStrDateCell(Workbook workbook, Cell cell, String dateFormat) {
        CellStyle cellStyle = workbook.createCellStyle();
        CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(dateFormat));
        cell.setCellStyle(cellStyle);
        return new DataFormatter().formatCellValue(cell);
    }

    public static Date getDate(Workbook workbook, Cell cell){
        String value = ExcelUtils.getFormatStrDateCell(workbook, cell, "yyyy-MM-dd HH:mm:ss");
        return  DateUtils.parseTry(value);
    }

    public static void setPrompt(Cell cell, String prompt){
        Sheet sheet = cell.getSheet();
        Row row = cell.getRow();
        CellRangeAddressList ranges = new CellRangeAddressList(row.getRowNum(),row.getRowNum(),
                cell.getColumnIndex(), cell.getColumnIndex());

        DataValidationHelper helper = sheet.getDataValidationHelper();
        DataValidationConstraint constraint = helper.createCustomConstraint("A1");


        DataValidation dataValidation = helper.createValidation(constraint, ranges);
        if(prompt.length() > 255){
            prompt = prompt.substring(0, 254);
        }
        dataValidation.createPromptBox("------------tips-------------", prompt);
        dataValidation.setShowPromptBox(true);
        sheet.addValidationData(dataValidation);
    }

    public static String getCellValue(Row row, Integer cellIndex){
        return getCellValue(safeGetCell(row, cellIndex));
    }

    /**
     * 导入数字时。导入公式的计算结果而非公式
     */
    public static String getCellValue(Cell cell) {
        if (cell == null){
            return "";
        }
        CellType cellType = cell.getCellTypeEnum();
        String cellValue = "";
        switch (cellType) {
            case NUMERIC:{
                cellValue = String.valueOf(cell.getNumericCellValue());
                if(isDateCell(cell)){
                    cellValue = DateUtils.format(cell.getDateCellValue(), "yyyy-MM-dd HH:mm:ss");
//                    return getFormatStrDateCell(cell.getSheet().getWorkbook(), cell, "yyyy-MM-dd HH:mm:ss");
                }
                break;
            }
            case FORMULA:
                try {
                    cellValue = cell.getStringCellValue();
                } catch (IllegalStateException e) {
                    cellValue = String.valueOf(cell.getNumericCellValue());
                }
                break;
            default:
                cellValue = new DataFormatter().formatCellValue(cell);
        }
        return cellValue.trim();
    }

    /**
     * 简单地统计遍历过的行
     * 以及获取正在遍历的行
     */
    public static class Traversal{
        Set<Integer> traversalRow = new HashSet<>(); // 遍历过的行
        List<Map<String, String>> list = new ArrayList<>();

        public Map<String, String> getMapRow(Row row){
            Map<String, String> map;
            if (traversalRow.contains(row.getRowNum())) {
                map = list.get(list.size() - 1);
            } else {
                traversalRow.add(row.getRowNum());
                map = new HashMap<>();
                list.add(map);
            }
            return map;
        }

        public List<Map<String, String>> getList(){
            return this.list;
        }
    }

    /**
     * 如果单元格仅仅包含图片和字符串，可以使用该方式
     */
    public static ExcelExporter.CellValueFormat SET_PIC = (int sheetIndex, int rowIndex, int colIndex,
                                                           Workbook workbook, Sheet sheet, Row row, Cell cell, Object value) -> {
        if (isEmptyObject(value)) {
            return null;
        }
        if (byte[].class.equals(value.getClass())) {
            int picIndex = workbook.addPicture((byte[]) value, Workbook.PICTURE_TYPE_JPEG);
            CreationHelper helper = workbook.getCreationHelper();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setRow1(rowIndex);
            anchor.setRow2(rowIndex + 1);
            anchor.setCol1(colIndex);
            anchor.setCol2(colIndex + 1);
            sheet.getDrawingPatriarch().createPicture(anchor, picIndex);
        } else {
            return value.toString();
        }
        return null;
    };

    public static ExcelExporter.CellValueFormat DEFAULT_SET_VALUE_MANEL = ((sheetIndex, rowIndex, colIndex, workbook, sheet, row, cell, value)
            -> isEmptyObject(value) ? null : value.toString());
}
