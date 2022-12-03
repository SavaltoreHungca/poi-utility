package com.zxslsoft.general.utility.poi;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DocxUtil {
    /**
     * @param map value 支持的类型有 str、 {@link Img}、{@link BiConsumer<XWPFDocument, XWPFParagraph>}
     *            key 以 IMG_ 开头则表示插入图片 {@link Img}
     *            为 INSERT_ 则表示插入自定义内容 {@link BiConsumer<XWPFDocument, XWPFParagraph>}
     */
    public static void replaceContent(XWPFDocument document, Map<String, Object> map) {
        for (XWPFParagraph paragraph : new ArrayList<>(document.getParagraphs())) {
            List<XWPFRun> runs = paragraph.getRuns();

            Map<String, List<RunSet>> joinedRun = findJoinedRun(runs);
            replaceStr(document, paragraph, joinedRun, map);
        }

        Iterator<XWPFTable> itTable = document.getTablesIterator();
        while (itTable.hasNext()) {
            XWPFTable table = itTable.next();
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : new ArrayList<>(Utils.nullSafe(cell.getParagraphs()))) {
                        Map<String, List<RunSet>> joinedRun = findJoinedRun(paragraph.getRuns());
                        replaceStr(document, paragraph, joinedRun, map);
                    }
                }
            }
        }
    }

    private static void replaceStr(XWPFDocument document, XWPFParagraph paragraph, Map<String, List<RunSet>> finds, Map<String, Object> repMap) {
        if (Utils.isEmpty(finds)) {
            return;
        }

        finds.forEach((k, v)->{
            for (RunSet item : v) {
                Map<String, RunSet> find = Utils.asMap(
                        k, item
                );
                find.forEach((name, runset) -> {
                    StringBuilder text = new StringBuilder();
                    XWPFRun firstRun = null;
                    int i = 0;
                    for (XWPFRun run : runset.runs) {
                        text.append(run.getText(run.getTextPosition()));
                        if (i == 0) {
                            firstRun = run;
                        } else {
                            paragraph.removeRun(runset.startPosi + 1);
                        }
                        i++;
                    }
                    assert firstRun != null;
                    if (repMap.containsKey(name)) {
                        if (name.startsWith("IMG_")) {
                            firstRun.setText(text.toString().replace(rs(name), ""), 0);
                            Img img = (Img) repMap.get(name);
                            insertImg(paragraph, img);
                        } else if (name.startsWith("INSERT_")) {
                            firstRun.setText(text.toString().replace(rs(name), ""), 0);
                            BiConsumer<XWPFDocument, XWPFParagraph> consumer = (BiConsumer) repMap.get(name);
                            consumer.accept(document, paragraph);
                            if (Utils.isEmptyString(paragraph.getText())) {
                                document.removeBodyElement(document.getPosOfParagraph(paragraph));
                            }
                        } else {
                            firstRun.setText(text.toString().replace(rs(name), repMap.get(name).toString()), 0);
                        }
                    } else {
                        firstRun.setText(text.toString(), 0);
                    }
                });
            }
        });

    }

    private static Map<String, List<RunSet>> findJoinedRun(Collection<XWPFRun> runs) {
        Map<String, List<RunSet>> joinedRun = new HashMap<>();

        int runPosi = 0;
        boolean found = false;
        boolean foundStart = false;
        boolean firstFound = false;
        RunSet runSet = null;
        StringBuilder key = new StringBuilder();
        XWPFRun beforeRun = null;
        for (XWPFRun run : runs) {
            String str = run.getText(run.getTextPosition());
            if (str == null) {
                continue;
            }
            for (int i = 0; i < str.length(); i++) {
                if (str.charAt(i) == '$') {
                    foundStart = true;
                    continue;
                }
                if (str.charAt(i) == '{' && foundStart) {
                    foundStart = false;
                    firstFound = true;
                    found = true;
                    runSet = new RunSet();
                    if (i > 0) {
                        runSet.startPosi = runPosi;
                    } else {
                        runSet.startPosi = runPosi - 1;
                        runSet.runs.add(beforeRun);
                    }
                    runSet.runs.add(run);
                    continue;
                }
                if (foundStart) {
                    foundStart = false;
                    continue;
                }
                if (str.charAt(i) == '}' && found) {
                    firstFound = false;
                    foundStart = false;
                    found = false;

                    if (!runSet.runs.contains(run)) {
                        runSet.runs.add(run);
                    }
                    joinedRun.computeIfAbsent(key.toString(), _k -> new ArrayList<>())
                            .add(runSet);
                    key = new StringBuilder();
                    continue;
                }

                if (found) {
                    key.append(str.charAt(i));
                }
            }
            if (!firstFound && found && !runSet.runs.contains(run)) {
                runSet.runs.add(run);
            }
            if (firstFound) {
                firstFound = false;
            }
            beforeRun = run;
            runPosi++;
        }
        return joinedRun;
    }

    private static class RunSet {
        int startPosi;
        List<XWPFRun> runs = new ArrayList<>();
    }

    private static String rs(String name) {
        return String.format("${%s}", name);
    }

    public static void insertImg(XWPFParagraph newPara, Img img) {
        try {
            newPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun newParaRun = newPara.createRun();
            newParaRun.addPicture(new ByteArrayInputStream(img.data), img.type, img.name, Units.toEMU(img.width), Units.toEMU(img.height));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class Img {

        /**
         * @param type   {@link XWPFDocument#PICTURE_TYPE_PNG}
         * @param width  int
         * @param height int
         * @param name   {@link Units#toEMU}
         * @param data   byte[]
         */
        public Img(int type, int width, int height, String name, byte[] data) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.name = name;
            this.data = data;
        }

        public int type;
        public int width;
        public int height;
        public String name;
        public byte[] data;
    }

    public static XmlCursor insertParagraph(XWPFDocument d, XmlCursor cursor, Consumer<XWPFParagraph> consumer) {
        XWPFParagraph paragraph = d.insertNewParagraph(cursor);
        consumer.accept(paragraph);
        cursor = paragraph.getCTP().newCursor();
        cursor.toNextSibling();
        XWPFParagraph p = d.insertNewParagraph(cursor);
        return p.getCTP().newCursor();
    }

    public static XmlCursor insertTable(XWPFDocument d, XmlCursor cursor, Consumer<XWPFTable> consumer) {
        XWPFTable table = d.insertNewTbl(cursor);


        CTTblPr tblPr = table.getCTTbl().addNewTblPr();
        tblPr.addNewJc().setVal(STJc.CENTER);
        CTTblWidth width = tblPr.addNewTblW();
        width.setType(STTblWidth.AUTO);
        width.setW(new BigInteger("0"));
        tblPr.setTblW(width);
        table.setWidth("100%");

        consumer.accept(table);
        cursor = table.getCTTbl().newCursor();
        cursor.toNextSibling();
        XWPFParagraph p = d.insertNewParagraph(cursor);
        return p.getCTP().newCursor();
    }

    public static void initTableSize(XWPFTable table, int rows, int cols){
        for (int i = 0; i < rows; i++) {
            if (i == 0) for (int j = 1; j < cols; j++) {
                table.getRow(i).createCell();
            }
            else {
                table.createRow();
            }
        }
    }

    public static XmlCursor insertTable(XWPFDocument d, XmlCursor cursor, List<List<String>> data) {
        return insertTable(d, cursor, table -> {
            CTPageSz pgSz = getPageSize(d, table).getPgSz();
            String cellW = pgSz.getW().longValue() / data.get(0).size() + "";

            initTableSize(table, data.size(), data.get(0).size());

            for (int i = 0; i < data.size(); i++) {
                List<String> header = data.get(i);
                XWPFTableRow row = table.getRow(i);
                for (int j = 0; j < header.size(); j++) {
                    XWPFTableCell cell = row.getCell(j);
                    XWPFParagraph paragraph = cell.addParagraph();
                    cell.setWidth(cellW);
                    XWPFRun r =  paragraph.createRun();
                    r.setText(header.get(j));
                    if(i==0){
                        r.setBold(true);
                    }
                    cell.setText("");
                }
            }
        });
    }

    public static CTSectPr getPageSize(XWPFDocument d, IBodyElement currentNode) {
        Integer currentNodePosi = null;
        CTSectPr rlt = null;
        int index = 0;
        for (IBodyElement bodyElement : d.getBodyElements()) {
            if (bodyElement.equals(currentNode)) {
                currentNodePosi = index;
            }
            if (currentNodePosi != null
                    && currentNodePosi <= index
                    && bodyElement instanceof XWPFParagraph) {
                XWPFParagraph p = ((XWPFParagraph) bodyElement);
                if (p.getCTP() != null && p.getCTP().getPPr() != null && p.getCTP().getPPr().getSectPr() != null) {
                    rlt = p.getCTP().getPPr().getSectPr();
                    break;
                }
            }
            index++;
        }
        if (rlt == null) rlt = d.getDocument().getBody().getSectPr();
        return rlt;
    }

    public static void setPageSize(XWPFDocument d, int w, int h){
        CTSectPr pr = getPageSize(d, d.getLastParagraph());
        XWPFParagraph paragraph = d.createParagraph();
        paragraph.getCTP().addNewPPr();
        paragraph.getCTP().getPPr().addNewSectPr();
        paragraph.getCTP().getPPr().getSectPr().addNewPgSz();
        paragraph.getCTP().getPPr().getSectPr().getPgSz().setW(pr.getPgSz().getW());
        paragraph.getCTP().getPPr().getSectPr().getPgSz().setH(pr.getPgSz().getH());

        if(d.getDocument().getBody().getSectPr() == null){
            d.getDocument().getBody().addNewSectPr();
        }

        if(d.getDocument().getBody().getSectPr().getPgSz() == null){
            d.getDocument().getBody().getSectPr().addNewPgSz();
        }

        d.getDocument().getBody().getSectPr().getPgSz().setW(new BigInteger("" + w));
        d.getDocument().getBody().getSectPr().getPgSz().setH(new BigInteger("" + h));
    }

    public static void createTableRow(XWPFTable table, Consumer<XWPFTableRow> consumer) {
        consumer.accept(table.createRow());
    }
}