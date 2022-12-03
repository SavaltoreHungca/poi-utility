package com.zxslsoft.general.utility.poi;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * 日期处理
 */
@SuppressWarnings("all")
public class DateUtils {

    private static final DateTimeFormatter dateTimeFormatter=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 将日期格式化到日
     */
    public static String format(Date date) {
        return format(date, "yyyy-MM-dd");
    }

    /**
     * 将日期全 格式化
     */
    public static String formatAll(Date date) {
        return format(date, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 将日期全 格式化
     */
    public static String formatAll(LocalDateTime dateTime) {
        return dateTimeFormatter.format(dateTime);
    }

    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    public static String convertFormat(String dateStr, String originalFormat, String newFormat) {
        return format(parse(dateStr, originalFormat), newFormat);
    }

    public static Date parseTry(String date){
        if(Utils.isEmptyString(date)) return null;
        Date ans = null;
        for(String format : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd",
                "yyyy MM dd", "yyyy MM-dd",
                "yyyy-MM dd", "yyyy-MM",
                "yyyy MM", "yyyy"}){
            try{
                ans = parse(date, format);
            }catch (Exception e){
                //pass
            }
            if(ans != null){
                break;
            }
        }
        return ans;
    }

    public static Date parse(String date, String pattern) {
        try {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.parse(date);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Date parse(Date date, String pattern) {
        return parse(format(date, "yyyy-MM-dd HH:mm:ss"), pattern);
    }

    /**
     * 生成日期范围, 包含传入的开始和结束时间
     */
    public static List<String> genDateRange(Date start, Date end) {
        start = parse(start, "yyyy-MM-dd");
        end = parse(end, "yyyy-MM-dd");
        List<String> ans = new ArrayList<>();
        if (start.getTime() <= end.getTime()) {
            ans.add(format(start));
            while (start.getTime() < end.getTime()) {
                start = add(start, Calendar.DATE, 1);
                ans.add(format(start));
            }
            if (!format(start).equals(format(end))) {
                ans.add(format(end));
            }
            return ans;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<String> genMonthRange(Date start, Date end) {
        start = parse(start, "yyyy-MM");
        end = parse(end, "yyyy-MM");
        List<String> ans = new ArrayList<>();
        if (start.getTime() <= end.getTime()) {
            ans.add(format(start, "yyyy-MM"));
            while (start.getTime() < end.getTime()) {
                start = add(start, Calendar.MONTH, 1);
                ans.add(format(start, "yyyy-MM"));
            }
            if (!format(start).equals(format(end))) {
                ans.add(format(end, "yyyy-MM"));
            }
            return ans;
        } else {
            return new ArrayList<>(0);
        }
    }

    public static List<String> genDateRange(String start, String end) {
        return genDateRange(parse(start, "yyyy-MM-dd"), parse(end, "yyyy-MM-dd"));
    }

    public static List<String> genMonthRange(String start, String end) {
        return genMonthRange(parse(start, "yyyy-MM"), parse(end, "yyyy-MM"));
    }

    public static Date lastTimeOfYear(Date date) {
        return parse(format(date, "yyyy") + "-12-31 23:59:59",
                "yyyy-MM-dd HH:mm:ss");
    }

    public static Date lastTimeOfMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int lastDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        calendar.set(Calendar.DAY_OF_MONTH, lastDay);
        return parse(format(calendar.getTime(), "yyyy-MM-dd") + " 23:59:59",
                "yyyy-MM-dd HH:mm:ss");
    }

    public static Date atFirstTimeOfMonth(Date date) {
        return parse(format(date, "yyyy-MM") + "-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
    }

    public static Date atFirstTimeOfYear(Date date) {
        return parse(format(date, "yyyy") + "-01-01 00:00:00",
                "yyyy-MM-dd HH:mm:ss");
    }

    public static Date add(String date, String format, int field, int amount) {
        return add(parse(date, format), field, amount);
    }

    public static Date add(Date date, int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(field, amount);
        return calendar.getTime();
    }

    public static Long add(Long date, int field, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(date));
        calendar.add(field, amount);
        return calendar.getTime().getTime();
    }

    /**
     * 计算距离现在多久，精确
     */
    public static String getTimeBeforeAccurate(Date date) {
        Date now = new Date();
        long l = now.getTime() - date.getTime();
        long day = l / (24 * 60 * 60 * 1000);
        long hour = (l / (60 * 60 * 1000) - day * 24);
        long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
        long s = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
        String r = "";
        if (day > 0) {
            r += day + "天";
        }
        if (hour > 0) {
            r += hour + "小时";
        }
        if (min > 0) {
            r += min + "分";
        }
        if (s > 0) {
            r += s + "秒";
        }
        r += "前";
        return r;
    }


    public static DateUnit getDateUnit(Date time) {
        DateUnit ans = new DateUnit();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time);
        ans.y = calendar.get(Calendar.YEAR);
        ans.m = calendar.get(Calendar.MONTH) + 1;
        ans.d = calendar.get(Calendar.DAY_OF_MONTH);
        ans.minutes = calendar.get(Calendar.MINUTE);
        ans.hour = calendar.get(Calendar.HOUR);
        ans.seconds = calendar.get(Calendar.SECOND);
        return ans;
    }

    public static DateUnit getDateUnit(String dateStr, String format) {
        return getDateUnit(parse(dateStr, format));
    }

    public static LocalDateTime toLocalDateTimeFromFormat_yM(String dateYM) {
        return toLocalDateTime(parse(dateYM, "yyyy-MM"));
    }

    /**
     *
     *
     * @param localDateTime
     * @return yyyy-MM 格式的
     */
    public static String getStrDateYM(LocalDateTime localDateTime) {
        if (localDateTime.getMonthValue() < 10) {
            return localDateTime.getYear() + "-0" + localDateTime.getMonthValue();
        }
        return localDateTime.getYear() + "-" + localDateTime.getMonthValue();
    }
    public static String getStrDateYM(LocalDate localDate) {
        if (localDate.getMonthValue() < 10) {
            return localDate.getYear() + "-0" + localDate.getMonthValue();
        }
        return localDate.getYear() + "-" + localDate.getMonthValue();
    }


    public static String identifyValueFormat(String value) {

        return "";
    }

    public static class DateUnit {
        public Integer y;
        public Integer m;
        public Integer d;
        public Integer hour;
        public Integer minutes;
        public Integer seconds;

        @Override
        public String toString() {
            return "DateUnit{" +
                    "y=" + y +
                    ", m=" + m +
                    ", d=" + d +
                    ", hour=" + hour +
                    ", minutes=" + minutes +
                    ", seconds=" + seconds +
                    '}';
        }

        public String getDateYM() {
            if (this.m < 10) {
                return this.y + "-0" + this.m;
            }
            return this.y + "-" + this.m;
        }

        public Date getDate(String format) {
            String dateStr = "";
            if (this.m < 10) {
                dateStr = this.y + "-0" + this.m;
            } else {
                dateStr = this.y + "-" + this.m;
            }
            return DateUtils.parse(dateStr, format);
        }
    }

    //上年同时
    public static Date lastYearOfNow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.YEAR, -1);
        return c.getTime();
    }

    // 上月同时
    public static Date lastMonthOfNow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.MONTH, -1);
        return c.getTime();
    }

    // 上日同时
    public static Date lastDayOfNow(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(Calendar.DATE, -1);
        return c.getTime();
    }

    public static Date getLastTimeOfDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return parse(format(calendar.getTime(), "yyyy-MM-dd") + " 23:59:59",
                "yyyy-MM-dd HH:mm:ss");
    }

    public static Date getFirstTimeOfDay(Date date) {
        return parse(format(date, "yyyy-MM-dd") + " 00:00:00", "yyyy-MM-dd HH:mm:ss");
    }

    public static String getLastM(String dateYM) {
        Date dateTime = DateUtils.parse(dateYM, "yyyy-MM");
        DateUtils.DateUnit lastMDateUnit = DateUtils.getDateUnit(DateUtils.add(dateTime, Calendar.MONTH, -1));
        return lastMDateUnit.getDateYM();
    }

    public static String getsameMLastY(String dateYM) {
        Date dateTime = DateUtils.parse(dateYM, "yyyy-MM");
        DateUtils.DateUnit sameMLastYDateUnit = DateUtils.getDateUnit(DateUtils.add(dateTime, Calendar.YEAR, -1));
        return sameMLastYDateUnit.getDateYM();
    }

    public static List<String> getMonthBetween(String minDate, String maxDate) throws ParseException, ParseException {
        ArrayList<String> result = new ArrayList<String>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");//格式化为年月

        Calendar min = Calendar.getInstance();
        Calendar max = Calendar.getInstance();

        min.setTime(sdf.parse(minDate));
        min.set(min.get(Calendar.YEAR), min.get(Calendar.MONTH), 1);

        max.setTime(sdf.parse(maxDate));
        max.set(max.get(Calendar.YEAR), max.get(Calendar.MONTH), 2);

        Calendar curr = min;
        while (curr.before(max)) {
            result.add(sdf.format(curr.getTime()));
            curr.add(Calendar.MONTH, 1);
        }

        return result;
    }


    public static final DateTimeFormatter DFY_MD_HMS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DFY_MD = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DFY_M = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * LocalDateTime 转时间戳
     *
     * @param localDateTime /
     * @return /
     */
    public static Long getTimeStamp(LocalDateTime localDateTime) {
        return localDateTime.toInstant(ZoneOffset.of("+8")).toEpochMilli();
        //   return localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    /**
     * 时间戳转LocalDateTime
     *
     * @param timeStamp /
     * @return /
     */
    public static LocalDateTime fromTimeStamp(Long timeStamp) {
        return LocalDateTime.ofEpochSecond(timeStamp, 0, OffsetDateTime.now().getOffset());
    }

    /**
     * LocalDateTime 转 Date
     * Jdk8 后 不推荐使用 {@link Date} Date
     *
     * @param localDateTime /
     * @return /
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * LocalDate 转 Date
     * Jdk8 后 不推荐使用 {@link Date} Date
     *
     * @param localDate /
     * @return /
     */
    public static Date toDate(LocalDate localDate) {
        return toDate(localDate.atTime(LocalTime.now(ZoneId.systemDefault())));
    }


    /**
     * Date转 LocalDateTime
     * Jdk8 后 不推荐使用 {@link Date} Date
     *
     * @param date /
     * @return /
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 日期 格式化
     *
     * @param localDateTime /
     * @param patten        /
     * @return /
     */
    public static String localDateTimeFormat(LocalDateTime localDateTime, String patten) {
        DateTimeFormatter df = DateTimeFormatter.ofPattern(patten);
        return df.format(localDateTime);
    }

    /**
     * 日期 格式化
     *
     * @param localDateTime /
     * @param df            /
     * @return /
     */
    public static String localDateTimeFormat(LocalDateTime localDateTime, DateTimeFormatter df) {
        return df.format(localDateTime);
    }

    /**
     * 日期格式化 yyyy-MM-dd HH:mm:ss
     *
     * @param localDateTime /
     * @return /
     */
    public static String localDateTimeFormatyMdHms(LocalDateTime localDateTime) {
        return DFY_MD_HMS.format(localDateTime);
    }

    /**
     * 日期格式化 yyyy-MM-dd
     *
     * @param localDateTime /
     * @return /
     */
    public String localDateTimeFormatyMd(LocalDateTime localDateTime) {
        return DFY_MD.format(localDateTime);
    }

    /**
     * 日期格式化 yyyy-MM
     *
     * @param localDateTime
     * @return
     */
    public static String localDateTimeFormatyM(LocalDateTime localDateTime) {
        return DFY_M.format(localDateTime);
    }

    /**
     * 字符串转 LocalDateTime ，字符串格式 yyyy-MM-dd
     *
     * @param localDateTime /
     * @return /
     */
    public static LocalDateTime parseLocalDateTimeFormat(String localDateTime, String pattern) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
        return LocalDateTime.from(dateTimeFormatter.parse(localDateTime));
    }

    /**
     * 字符串转 LocalDateTime ，字符串格式 yyyy-MM-dd
     *
     * @param localDateTime /
     * @return /
     */
    public static LocalDateTime parseLocalDateTimeFormat(String localDateTime, DateTimeFormatter dateTimeFormatter) {
        return LocalDateTime.from(dateTimeFormatter.parse(localDateTime));
    }

    /**
     * 获取年月的整数格式
     *
     * @param localDateTime
     * @return
     */
    public static Integer getDateYM(LocalDateTime localDateTime) {
        return localDateTime.getYear() * 100 + localDateTime.getMonthValue();
    }

    /**
     * 字符串转 LocalDateTime ，字符串格式 yyyy-MM-dd HH:mm:ss
     *
     * @param localDateTime /
     * @return /
     */
    public static LocalDateTime parseLocalDateTimeFormatyMdHms(String localDateTime) {
        return LocalDateTime.from(DFY_MD_HMS.parse(localDateTime));
    }

    /**
     * 根据日期获得当月有多少天
     *
     * @param date
     * @return
     */
    public static int getMonthDays(Date date) {
        Calendar currentDate = Calendar.getInstance();
        currentDate.setTime(date);
        //将时间设置为下一个月
        currentDate.add(Calendar.MONTH, 1);
        //将日期设置为下一个月的第一天
        currentDate.add(Calendar.DATE, 0 - currentDate.get(Calendar.DATE));
        //将时间设置为剪去一天 则为当月的最后一天
        currentDate.add(Calendar.DATE, -1);
        //获取当月最后一天号数 加 1 则为当月有多少天
        return currentDate.get(Calendar.DATE) + 1;
    }

    /**
     * 将时间转换为日期
     *
     * @param time
     * @return
     */
    public static String timeToString(Date time) {
        return timeToString(time, "yyyy-MM-dd HH:mm:ss");
    }
    /**
     * 将时间转换为日期
     *
     * @param time
     * @param format
     * @return
     */
    public static String timeToString(Date time, String format) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(format);

            String date = sdf.format(time);
            return date;
        } catch (Exception e) {
            return "";
        }
    }
    /**
     * 获取当月最开始的时间
     *
     * @param date
     * @return
     */
    public static Date getMonthStart(String date) {

        return getMonthStart(date + "-01 00:00:00", "yyyy-MM");
    }

    /**
     * 获取当月最开始的时间
     *
     * @param dateStr
     * @return
     */
    public static Date getMonthStart(String dateStr, String format) {
        return getMonthStart(parse(dateStr, format));
    }
    /**
     * 获取当月最开始的时间
     *
     * @param date
     * @return
     */
    public static Date getMonthStart(Date date) {
        Calendar currentDate = Calendar.getInstance();
        currentDate.setTime(date);
        currentDate.add(Calendar.DATE, 1 - currentDate.get(Calendar.DATE));
        currentDate.add(Calendar.HOUR_OF_DAY, 0 - currentDate.get(Calendar.HOUR_OF_DAY));
        currentDate.add(Calendar.MINUTE, 0 - currentDate.get(Calendar.MINUTE));
        currentDate.add(Calendar.SECOND, 0 - currentDate.get(Calendar.SECOND));
        currentDate.add(Calendar.MILLISECOND, 0 - currentDate.get(Calendar.MILLISECOND));
        return currentDate.getTime();
    }
    /**
     * 获取当月最后一刻
     *
     * @param date
     * @return
     */
    public static Date getMonthEnd(Date date) {
        Calendar currentDate = Calendar.getInstance();
        currentDate.setTime(getMonthStart(date));
        currentDate.add(Calendar.MONTH, 1);
        currentDate.add(Calendar.MILLISECOND, -1);
        return currentDate.getTime();
    }

    /**
     * 获取 i 天后的日期
     *
     * @param date
     * @param i
     * @return
     */
    public static Date getAfterDate(Date date, int type, int num) {
        Calendar currentDate = Calendar.getInstance();
        currentDate.setTime(date);
        currentDate.add(type, num);
        return currentDate.getTime();
    }

    public static void main(String[] args) {
        try {
            List<String> monthBetween = getMonthBetween("2020-1", "2021-10");
            System.out.println(monthBetween);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
