package com.zxslsoft.general.utility.poi;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;

public class ExcelImporter {
    private Workbook workbook;

    // 设置需要跳过的 sheet 页 和 某个 sheet 页行
    private final Map<Integer, Set<Integer>> skipRows = new HashMap<>();
    private final Map<Integer, Set<Integer>> skipColumns = new HashMap<>();
    private final Map<Integer, List<Integer>> notEmptyCells = new HashMap<>();
    private List<Integer> walkSheets = Utils.asList(0);


    /**
     * 导入数字时。导入公式的计算结果而非公式
     * @param cell
     * @return
     */
    public static String getCellValue(Cell cell) {
        return ExcelUtils.getCellValue(cell);
    }


    public ExcelImporter setWorkbook(InputStream inputStream) {
        try {
            ZipSecureFile.setMinInflateRatio(-1.0d);
            this.workbook = WorkbookFactory.create(inputStream);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ExcelImporter setWalkSheets(Integer... index){
        this.walkSheets = Utils.asList(index);
        return this;
    }

    public ExcelImporter setSkipRows(Integer sheetNum, Integer... nums) {
        this.skipRows.put(sheetNum, Utils.asSet(nums));
        return this;
    }

    public ExcelImporter setSkipColumns(Integer sheetNum, Integer... nums) {
        this.skipColumns.put(sheetNum, Utils.asSet(nums));
        return this;
    }

    public ExcelImporter setWorkbook(File file) {
        try {
            this.workbook = WorkbookFactory.create(file);
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Workbook getWorkbook(){
        return this.workbook;
    }

    public static byte[] getWorkbookBytes(Workbook workbook){
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            workbook.write(byteArrayOutputStream);
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 遍历所有行, 遍历方式为 一行一行地遍历
    public ExcelImporter walkThrough(ExcelWalker walker) {
        Iterator<Sheet> sheetIterator = this.workbook.sheetIterator();

        int sheetIndex = -1;

        while (sheetIterator.hasNext()) {
            sheetIndex++;
            Sheet sheet = sheetIterator.next();
            if(!walkSheets.contains(sheetIndex)){
                continue;
            }
            Iterator<Row> rowIterator = sheet.rowIterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                List<Integer> notEmptyCols = ExcelUtils.getNotEmptyColIndex(row, skipColumns.computeIfAbsent(sheetIndex, _k -> new HashSet<>(0)));
                if (!notEmptyCols.isEmpty() && !skipRows.computeIfAbsent(sheetIndex, _k -> new HashSet<>(0)).contains(row.getRowNum())) {
                    notEmptyCells.put(row.getRowNum(), notEmptyCols);
                }
            }

            int maxRow = 0;
            int maxCol = 0;

            for(Integer k : notEmptyCells.keySet()){
                maxRow = Math.max(maxRow, k);
                maxCol = Math.max(Utils.max(notEmptyCells.get(k)), maxCol);
            }

            for (int rowNum = 0; rowNum <= maxRow; rowNum++) {
                Set<Integer> skipSet = skipRows.get(sheetIndex);
                if (!Utils.isEmpty(skipSet) && skipSet.contains(rowNum)) {
                    continue;
                }
                Row row = sheet.getRow(rowNum);
                if(row == null){
                    continue;
                }
                for (int colNum = 0; colNum <= maxCol; colNum++) {
                    if (skipColumns.get(sheetIndex) != null && skipColumns.get(sheetIndex).contains(colNum)) {
                        continue;
                    }
                    Cell cell = row.getCell(colNum);
                    if (cell == null) {
                        cell = row.createCell(colNum);
                    }
                    walker.accept(workbook, sheet, row, cell);
                }
            }
        }

        return this;
    }

    // 判断value 是否下拉选择框形式， 将值映射为实体的值
    private String convertValue(Map<String, Map<String, String>> headerOptionalValue, String headerName, String value) {
        Map<String, String> stringStringMap = headerOptionalValue.get(headerName);
        if (stringStringMap != null && stringStringMap.size() > 0) {
            String mapValue = stringStringMap.get(value);
            if (Utils.isEmptyString(mapValue)) {
                throw new RuntimeException("不存在键值映射关系!");
            }
            return mapValue;
        } else {
            return value;
        }

    }

    // 将获取到的值转换为实体类
    public <T> List<T> convertToList(Class<T> type, Consumer<T> consumer) {
        List<T> dtoList = new ArrayList<>();
        try {
            ValueContainer<Integer> currentRow = new ValueContainer<>();
            ValueContainer<T> dto = new ValueContainer<>(ReflectUtils.newInstance(type));
            // 获取下拉的字段 以及下拉键值映射
            Map<String, Map<String, String>> headerOptionalValue = ExcelUtils.getHeaderOptionalValue(type);
            this.setSkipRows(0, 0)
                    .walkThrough((workbook, sheet, row, cell) -> {
                        try {
                            if (currentRow.get() == null) {
                                currentRow.set(row.getRowNum());
                                dtoList.add(dto.get());
                                consumer.accept(dto.get());
                            }
                            if (currentRow.get() != row.getRowNum()) {
                                dto.set(ReflectUtils.newInstance(type));
                                dtoList.add(dto.get());
                                consumer.accept(dto.get());
                                currentRow.set(row.getRowNum());
                            }

                            String value = new DataFormatter().formatCellValue(cell);
                            String headerName = new DataFormatter().formatCellValue(
                                    sheet.getRow(0).getCell(cell.getColumnIndex()));
                            if (headerOptionalValue != null) {
                                value = convertValue(headerOptionalValue, headerName, value);
                            }
                            ExcelUtils.setValue(dto.get(), headerName, value);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 将获取到的值转换为实体类
    public <T> List<T> convertToList(Class<T> type) {
        return this.convertToList(type, dto -> {
        });
    }

    public List<Map<String, String>> convertToMap(Consumer<Map<String, String>> consumer) {
        List<Map<String, String>> dtoList = new ArrayList<>();
        try {
            ValueContainer<Integer> currentRow = new ValueContainer<>();
            ValueContainer<Map<String, String>> dto = new ValueContainer<>(new HashMap<>());
            this.setSkipRows(0, 0)
                    .walkThrough((workbook, sheet, row, cell) -> {
                        try {
                            if (currentRow.get() == null) {
                                currentRow.set(row.getRowNum());

                                dtoList.add(dto.get());
                                consumer.accept(dto.get());
                            }
                            if (currentRow.get() != row.getRowNum()) {
                                dto.set(new HashMap<>());
                                dtoList.add(dto.get());
                                consumer.accept(dto.get());
                                currentRow.set(row.getRowNum());
                            }

                            String headerName = ExcelUtils.getCellValue(sheet.getRow(0).getCell(cell.getColumnIndex()));
                            String value = ExcelUtils.getCellValue(cell);
                            dto.get().put(headerName, value);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
            return dtoList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, String>> convertToMap() {
        return this.convertToMap(_ite -> {
        });
    }

    /**
     * 判断有
     */
    public interface ExcelWalker {
        void accept(Workbook workbook, Sheet sheet, Row row, Cell cell);
    }

    public static void main(String[] args) {
        new ExcelImporter()
                .setWorkbook(new File("/Users/hgc/Downloads/黄贵川周报.xls"))
                .walkThrough((workbook, sheet, row, cell) -> {
                    System.out.println(new DataFormatter().formatCellValue(cell));
                });
    }
}
