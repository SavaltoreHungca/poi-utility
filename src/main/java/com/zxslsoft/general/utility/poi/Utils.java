package com.zxslsoft.general.utility.poi;

import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * 存放一般的工具，如基本类型，集合类型等
 */
@SuppressWarnings("all")
public class Utils {
    private static final Random random = new Random();

    /**
     * 根据字符的相似度获取匹配列表
     */
    public static List<String> getSimilarList(Collection<String> collection, String targetStr) {
        List<String> rlt = new ArrayList<>();
        Map<String, Double> similarityRate = new HashMap<>();
        for (String s : Utils.nullSafe(collection)) {
            double rate = Utils.strSimilarity(s, targetStr);
            if(targetStr.contains(s) || s.contains(targetStr)){
                rlt.add(s);
            }
            similarityRate.put(s, rate);
        }
        List<String> ans = new ArrayList<>(similarityRate.keySet());

        ans.sort((a, b) -> {
            double rateA = similarityRate.get(a);
            double rateB = similarityRate.get(b);

            double subtract = rateB - rateA;
            return subtract == 0 ? 0 : subtract > 0 ? 1 : -1;
        });
        for (int i = 0; i < 3 && i < ans.size(); i++) {
            if(!rlt.contains(ans.get(i))){
                rlt.add(ans.get(i));
            }
        }
        return rlt;
    }

    /**
     * 计算字符串的相似度
     */
    public static double strSimilarity(String A, String targetStr) {
        if (A == null && targetStr == null) {
            return 1;
        } else if (A == null || targetStr == null) {
            return 0;
        }
        return (targetStr.length() - strEditDistance(A, targetStr) + 0.0) / targetStr.length();
    }

    /**
     * 计算字符串编辑距离
     */
    public static int strEditDistance(String A, String targetStr) {
        if (A.equals(targetStr)) {
            System.out.println(0);
            return 0;
        }
        //dp[i][j]表示源串A位置i到目标串B位置j处最低需要操作的次数
        int[][] dp = new int[A.length() + 1][targetStr.length() + 1];
        for (int i = 1; i <= A.length(); i++)
            dp[i][0] = i;
        for (int j = 1; j <= targetStr.length(); j++)
            dp[0][j] = j;
        for (int i = 1; i <= A.length(); i++) {
            for (int j = 1; j <= targetStr.length(); j++) {
                if (A.charAt(i - 1) == targetStr.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1];
                else {
                    dp[i][j] = Math.min(dp[i - 1][j] + 1,
                            Math.min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + 1));
                }
            }
        }
        return dp[A.length()][targetStr.length()];
    }

    public static <T> String strSetAdd(String original,
                                       T el,
                                       String regex) {
        Set<String> list = new HashSet<>(strsplit(original, regex));
        list.add(el.toString());
        return strjoin(regex.replaceAll("\\\\", ""), list);
    }

    /**
     * 在 （min, max） 内
     */
    public static double randomDouble(double min, double max) {

        return min + ((max - min) * random.nextDouble());
    }

    public static boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null || b == null) {
            return false;
        }
        if (a == b) return true;
        if (a.equals(b)) {
            return true;
        }
        return false;
    }

    public static <T> void sort(LinkedHashSet<T> set, Comparator<T> comparator) {
        List<T> list = new ArrayList<>(set);
        list.sort(comparator);
        set.clear();
        set.addAll(list);
    }

    /**
     * 在 [m,n] 内, 只有正数和零
     */
    public static int randomInt(int min, int max) {
        return Math.abs(random.nextInt()) % (max - min + 1) + min;
    }

    /**
     * 记录 throttle
     */
    private static final Map<String, Long> THROTTLE_RECORDS = new HashMap<>();


    public static <T> void fillCollection(Collection<T> collection, T t, Integer size) {
        while (collection.size() < size) {
            collection.add(t);
        }
    }

    public static <T> List<T> fillCollection(T t, Integer size) {
        List<T> collection = new ArrayList<>();
        while (collection.size() < size) {
            collection.add(t);
        }
        return collection;
    }

    public static void sleep(Long miles) {
        try {
            Thread.currentThread().sleep(miles);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 空指针安全
     */
    public static <T> List<T> nullSafe(List<T> collection) {
        if (collection == null) {
            return new ArrayList<>(0);
        } else {
            return collection;
        }
    }

    public static <T> Collection<T> nullSafe(Collection<T> collection) {
        if (collection == null) {
            return new ArrayList<>(0);
        } else {
            return collection;
        }
    }

    public static <T> Set<T> nullSafe(Set<T> collection) {
        if (collection == null) {
            return new HashSet<>(0);
        } else {
            return collection;
        }
    }

    public static <T, V> Map<T, V> nullSafe(Map<T, V> collection) {
        if (collection == null) {
            return new HashMap<>(0);
        } else {
            return collection;
        }
    }

    /**
     * 获取数组
     */
    public static <T> T[] asArray(T... objs) {
        if (isEmpty(objs)) {
            return (T[]) new ArrayList<T>(0).toArray();
        }
        return objs;
    }

    /**
     * 获取集合
     */
    public static <T> List<T> asList(T[] array, Predicate<T> filter) {
        if (array == null) {
            return new ArrayList<>(0);
        }
        List<T> result = new ArrayList<>();
        for (T item : array) {
            if (filter.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    @SuppressWarnings("all")
    public static <T> List<T> asList(T... array) {
        if (array == null) {
            return new ArrayList<>(0);
        }
        List<T> result = new ArrayList<>();
        for (T item : array) {
            result.add(item);
        }
        return result;
    }

    public static <T> Set<T> asSet(T... array) {
        if (array == null) {
            return new HashSet<>(0);
        }
        HashSet<T> result = new HashSet<>();
        for (T item : array) {
            result.add(item);
        }
        return result;
    }

    public static <K, V> Map<K, V> asMap(Object... array) {
        if (isEmpty(array)) return new HashMap<>();
        Map<K, V> ans = new HashMap<>();
        for (int i = 0; i < array.length; i += 2) {
            ans.put((K) array[i], (V) array[i + 1]);
        }
        return ans;
    }

    public static <K, V> Map<K, V> asLinkedHashMap(Object... array) {
        if (isEmpty(array)) return new LinkedHashMap<>();
        Map<K, V> ans = new LinkedHashMap<>();
        for (int i = 0; i < array.length; i += 2) {
            ans.put((K) array[i], (V) array[i + 1]);
        }
        return ans;
    }

    public static <K, V> Map<K, V> setMapDefaultValue(Map<K, V> map, V defaultValue) {
        for (K k : filter(map.keySet(), k -> map.get(k) == null)) {
            map.put(k, defaultValue);
        }
        return map;
    }

    static <K, V> Map<K, V> asLinkedMap(Object... array) {
        if (isEmpty(array)) return new HashMap<>();
        Map<K, V> ans = new LinkedHashMap<>();
        for (int i = 0; i < array.length; i += 2) {
            ans.put((K) array[i], (V) array[i + 1]);
        }
        return ans;
    }

    public static <T> boolean isEmpty(T[] array) {
        if (null == array || array.length == 0) {
            return true;
        }
        return false;
    }

    /**
     * 在集合中？
     */
    public static <T> boolean in(T v, Collection<T> list) {
        if (null == v || isEmpty(list)) {
            return false;
        }
        for (T item : list) {
            if (item.equals(v)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查找并返回
     */
    public static <T> T find(Collection<T> list, Predicate<T> predicate) {
        if (isEmpty(list)) {
            return null;
        }
        for (T i : list) {
            if (predicate.test(i)) {
                return i;
            }
        }
        return null;
    }

    /**
     * 返回第一条记录
     */
    public static <T> T getOne(Collection<T> list) {
        if (isEmpty(list)) {
            return null;
        }
        for (T i : list) {
            return i;
        }
        return null;
    }

    /**
     * 获取第一个
     */
    public static <T> T getOne(T[] list) {
        if (isEmpty(list)) {
            return null;
        }
        for (T i : list) {
            return i;
        }
        return null;
    }

    /**
     * 获取指定下标的元素
     */
    public static <T> T getFromCollection(Collection<T> collection, int index) {
        if (isEmpty(collection)) {
            return null;
        }
        if (collection.size() <= index) {
            return null;
        }
        int j = 0;
        for (T i : collection) {
            if (index == j) {
                return i;
            }
            j++;
        }
        return null;
    }

    /**
     * 空对象？
     */
    public static boolean isEmptyObject(Object o) {
        if (null == o) {
            return true;
        }
        if (Collection.class.isAssignableFrom(o.getClass())) {
            return isEmpty((Collection) o);
        }
        if (in(o.getClass(), ReflectUtils.ESSENTIAL_ARRAY_TYPE)) {
            return isEmpty((Object[]) o);
        }
        if (o instanceof String) {
            return isEmptyString((String) o);
        }

        Object obj = o;
        if (obj == null) {
            return true;
        } else if (obj instanceof CharSequence) {
            return ((CharSequence) obj).length() == 0;
        } else if (obj.getClass().isArray()) {
            return Array.getLength(obj) == 0;
        } else if (obj instanceof Collection) {
            return ((Collection) obj).isEmpty();
        } else {
            return obj instanceof Map ? ((Map) obj).isEmpty() : false;
        }
    }

    /**
     * 空字符串？
     */
    public static boolean isEmptyString(String str) {
        return str == null || "".equals(str) || (str != null && str.trim().equals(""));
    }

    /**
     * 将列表根据某个字段映射为字典
     */
    public static <K, V> Map<K, V> getIdMap(Collection<V> list, Function<V, K> function) {
        if (isEmpty(list)) {
            return new HashMap<>(0);
        }

        Map<K, V> ans = new HashMap<>();
        for (V i : list) {
            K k = function.apply(i);
            if (k instanceof String && isEmptyString((String) k)) {
                continue;
            }
            if (!isEmptyObject(k)) {
                ans.put(k, i);
            }
        }
        return ans;
    }

    /**
     * ids转换list
     *
     * @param ids
     * @return
     */
    public static List<Long> idsToList(String ids) {
        if (ids == null || ids.equals("")) return new ArrayList<Long>(0);
        return Arrays.stream(ids.split(",")).map(s -> Long.valueOf(s)).collect(Collectors.toList());
    }

    /**
     * 将列表根据某个字段映射为一对多
     */
    public static <K, V> Map<K, List<V>> getLinkedIdMap(Collection<V> list, Function<V, K> function) {
        if (isEmpty(list)) {
            return new HashMap<>(0);
        }

        Map<K, List<V>> ans = new HashMap<>();
        for (V i : list) {
            K k = function.apply(i);
            if (k instanceof String && isEmptyString((String) k)) {
                continue;
            }
            if (!isEmptyObject(k)) {
                ans.computeIfAbsent(k, __ -> new ArrayList<>()).add(i);
            }
        }
        return ans;
    }

    public static <K, V, A> void distribution(
            Map<K, V> map,
            Collection<A> collection,
            Function<A, K> key,
            BiConsumer<A, V> set
    ) {
        for (A i : Utils.nullSafe(collection)) {
            K k = key.apply(i);
            if (map.containsKey(k)) {
                set.accept(i, map.get(k));
            }
        }
    }

    public static <K, V, A> void linkedDistribution(
            Collection<V> gs,
            Function<V, K> gkey,
            Collection<A> collection,
            Function<A, K> key,
            BiConsumer<A, List<V>> set
    ) {
        Map<K, List<V>> map = Utils.getLinkedIdMap(gs, gkey);
        for (A i : Utils.nullSafe(collection)) {
            K k = key.apply(i);
            if (map.containsKey(k)) {
                set.accept(i, map.get(k));
            }
        }
    }

    public static <K, V, A> void singleDistribution(
            Collection<V> gs,
            Function<V, K> gkey,
            Collection<A> collection,
            Function<A, K> key,
            BiConsumer<A, V> set
    ) {
        Map<K, V> map = Utils.getIdMap(gs, gkey);
        for (A i : Utils.nullSafe(collection)) {
            K k = key.apply(i);
            if (map.containsKey(k)) {
                set.accept(i, map.get(k));
            }
        }
    }

    /**
     * Integer 转 Long
     */
    public static Long i2l(Integer integer) {
        if (null == integer) {
            return null;
        } else {
            return integer.longValue();
        }
    }

    /**
     * 指定毫秒数内不会将同一方法再执行
     */
    public static void throttle(Supplier supplier, String key, Long miles) {
        Long current = System.currentTimeMillis();
        Long previous = THROTTLE_RECORDS.computeIfAbsent(key, k -> current - miles);
        if (current - previous >= miles) {
            supplier.get();
            THROTTLE_RECORDS.put(key, current);
        }
    }

    /**
     * 合并两个集合
     */
    public static <T> List<T> merge(Collection<?>... collections) {
        List<T> ans = new ArrayList<>();
        if (isEmpty(collections)) {
            return ans;
        }
        for (Collection<?> collection : collections) {
            ans.addAll((Collection) collection);
        }
        return ans;
    }

    /**
     * 合并两个集合
     */
    public static <T> Set<T> mergeAsSet(Collection<?>... collections) {
        Set<T> ans = new HashSet<>();
        if (isEmpty(collections)) {
            return ans;
        }
        for (Collection<?> collection : collections) {
            ans.addAll((Collection) collection);
        }
        return ans;
    }

    /**
     * 将集合分为多个组
     *
     * @param arrays     需要被分组的集合
     * @param eachLength 分的每组的大小
     * @return 分好组的集合
     */
    public static <T> List<List<T>> splitList(List<T> arrays, int eachLength) {
        List<List<T>> result = new ArrayList<List<T>>();
        int size = arrays.size();
        int start = 0;
        int end = 0;

        int flag = size - eachLength;

        for (; start < size; start += eachLength) {
            if (end > flag) {
                end = size;
            } else {
                end = start + eachLength;
            }
            result.add(arrays.subList(start, end));
        }
        return result;
    }

    /**
     * 将集合分组，然后单独处理每个组
     * 这对于数据量很大的集合要进行数据库查询时，进行分批查询
     */
    public static <T> void splitProcess(List<T> list, int len, Consumer<List<T>> consumer) {
        for (List<T> i : nullSafe(splitList(list, len))) {
            consumer.accept(i);
        }
    }

    public static byte[] getBytes(String str) {
        if (isEmptyString(str)) {
            return new byte[0];
        }
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String getString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static boolean lowerCompare(String str1, String str2) {
        if (str1 == str2) {
            return true;
        }

        if ("".equals(str1) && "".equals(str2)) {
            return true;
        }

        if (isEmptyString(str1) || isEmptyString(str2)) {
            return false;
        }

        if (str1.toLowerCase().equals(str2.toLowerCase())) {
            return true;
        }

        return false;
    }

    public static boolean lowerCompareObj(Object str1, Object str2) {
        if (str1 == str2) {
            return true;
        }
        if (str1 == null) return false;
        if (str2 == null) return false;
        return lowerCompare(str1.toString(), str2.toString());
    }

    public static String lower(String str) {
        if (isEmptyString(str)) {
            return str;
        }
        return str.toLowerCase();
    }

    public static String upper(String str) {
        if (isEmptyString(str)) {
            return str;
        }
        return str.toUpperCase();
    }

    public static Properties mergeProperties(Properties prop1, Properties prop2) {
        Enumeration enumeration = prop2.propertyNames();
        while (enumeration.hasMoreElements()) {
            String key = (String) enumeration.nextElement();
            prop1.setProperty(key, prop2.getProperty(key));
        }
        return prop1;
    }

    public static <T> String strListAdd(String original,
                                        T el,
                                        String regex) {
        List<String> list = strsplit(original, regex);
        list.add(el.toString());
        return strjoin(regex.replaceAll("\\\\", ""), list);
    }

    public static <T, R> List<T> mapToList(Collection<R> collection, Function<R, T> function) {
        return nullSafe(collection).stream().map(function).collect(Collectors.toList());
    }

    public static <T, R> List<T> mapToList(R[] collection, Function<R, T> function) {
        return nullSafe(mapToList(asList(collection), function));
    }

    public static <T, R> Set<T> mapToSet(Collection<R> collection, Function<R, T> function) {
        return nullSafe(collection).stream().map(function).collect(Collectors.toSet());
    }

    public static <T, R> Set<T> mapToSet(R[] collection, Function<R, T> function) {
        return nullSafe(asList(collection)).stream().map(function).collect(Collectors.toSet());
    }

    public static <R> List<R> filter(Collection<R> collection, Predicate<? super R> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toList());
    }

    public static <R> List<R> filter(R[] collection, Predicate<? super R> predicate) {
        return asList(collection).stream().filter(predicate).collect(Collectors.toList());
    }

    public static <T, R> void mapRemove(Map<T, R> map, BiPredicate<T, R> predicate) {
        if (isEmpty(map)) {
            return;
        }
        List<T> shouldRemove = new ArrayList<>();
        map.forEach((k, v) -> {
            if (predicate.test(k, v)) {
                shouldRemove.add(k);
            }
        });
        for (T t : shouldRemove) {
            map.remove(t);
        }
    }

    public static <R> void collectionRemove(Collection<R> collection, Predicate<? super R> predicate) {
        if (isEmpty(collection)) {
            return;
        }
        List<R> shouldRemove = new ArrayList<>();
        collection.forEach(ite -> {
            if (predicate.test(ite)) {
                shouldRemove.add(ite);
            }
        });
        for (R t : shouldRemove) {
            collection.remove(t);
        }
    }

    /**
     * 拼接字符串
     */
    public static <T> String strjoin(String separator, Collection<T> collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return String.join(separator, mapToList(collection, T::toString));
    }

    /**
     * 拼接字符串
     */
    public static <T> String strjoin(String separator, T[] collection) {
        if (isEmpty(collection)) {
            return null;
        }
        return strjoin(separator, asList(collection));
    }

    public static List<String> strsplit(String str, String regex) {
        if (isEmptyString(str)) {
            return new ArrayList<>(0);
        }

        List<String> list = asList(str.split(regex));
        List<String> ans = new ArrayList<>();

        for (String s : list) {
            if (!isEmptyString(s)) {
                ans.add(s);
            }
        }
        return ans;
    }

    public static String[] splitstr(String str, String regex) {
        if (isEmptyString(str)) {
            return new String[]{};
        }
        return asList(str.split(regex)).toArray(new String[]{});
    }

    public static <T> T[] splitstr(String str, String regex, Class<T> clazz, Function<String, T> convertor) {
        List<T> ans = new ArrayList<>();
        if (isEmptyString(str)) {
            return (T[]) Array.newInstance(clazz, 0);
        }
        asList(str.split(regex)).forEach(ite -> {
            ans.add(convertor.apply(ite));
        });
        return ans.toArray((T[]) Array.newInstance(clazz, 0));
    }

    public static String toUnderLine(String param) {
        if (isEmptyString(param)) {
            return null;
        }
        int len = param.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = param.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                sb.append("_");
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }

    public static String toCamel(String param) {
        if (isEmptyString(param)) {
            return null;
        }
        String temp = param.toLowerCase();
        int len = temp.length();
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            char c = temp.charAt(i);
            if ('_' == c) {
                if (++i < len) {
                    sb.append(Character.toUpperCase(temp.charAt(i)));
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String getPatternPart(String str, String pattern) {
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(str);
            if (matcher.find()) {
                return matcher.group();
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static Integer max(Collection<Integer> collection) {
        Integer max = getOne(collection);
        if (max == null) {
            throw new RuntimeException("空集合");
        }
        for (Integer integer : collection) {
            max = Math.max(integer, max);
        }
        return max;
    }

    public static Long maxLong(Collection<Long> collection) {
        Long max = getOne(collection);
        if (max == null) {
            throw new RuntimeException("空集合");
        }
        for (Long integer : collection) {
            max = Math.max(integer, max);
        }
        return max;
    }

    public static Double doubleFormat(Double d) {
        if (null == d) {
            return 0d;
        }
        DecimalFormat df = new DecimalFormat("#.0000");
        return Double.valueOf(df.format(d));
    }

    public static String getExceptionStackString(Exception e) {
        try (
                StringWriter sw = new StringWriter();
                PrintWriter printWriter = new PrintWriter(sw, true);
        ) {
            e.printStackTrace(printWriter);
            return printWriter.toString();
        }catch (Exception e1){
            throw new RuntimeException(e1);
        }
    }

    public static boolean containsInstance(Collection<?> collection, Object element) {
        if (collection != null) {
            Iterator var2 = collection.iterator();

            while (var2.hasNext()) {
                Object candidate = var2.next();
                if (candidate == element) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean containsAny(Collection<?> source, Collection<?> candidates) {
        if (!isEmpty(source) && !isEmpty(candidates)) {
            Iterator var2 = candidates.iterator();

            Object candidate;
            do {
                if (!var2.hasNext()) {
                    return false;
                }

                candidate = var2.next();
            } while (!source.contains(candidate));

            return true;
        } else {
            return false;
        }
    }

    public static double objToDoubleValue(Object objStr) {
        return isEmptyObject(objStr) ? 0d : Double.valueOf(objStr.toString()).doubleValue();
    }

    public static Double average(Collection<Double> collection) {
        double sum = 0d;
        int count = 0;

        for (Double item : nullSafe(collection)) {
            if (!isEmptyObject(item) && item != 0.0d) {
                sum += item;
                count++;
            }
        }
        if (Double.isNaN(sum / count)) {
            return 0d;
        }
        return sum / count;
    }


    public static boolean hasUniqueObject(Collection<?> collection) {
        if (isEmpty(collection)) {
            return false;
        } else {
            boolean hasCandidate = false;
            Object candidate = null;
            Iterator var3 = collection.iterator();

            while (var3.hasNext()) {
                Object elem = var3.next();
                if (!hasCandidate) {
                    hasCandidate = true;
                    candidate = elem;
                } else if (candidate != elem) {
                    return false;
                }
            }

            return true;
        }
    }

    public static Class<?> findCommonElementType(Collection<?> collection) {
        if (isEmpty(collection)) {
            return null;
        } else {
            Class<?> candidate = null;
            Iterator var2 = collection.iterator();

            while (var2.hasNext()) {
                Object val = var2.next();
                if (val != null) {
                    if (candidate == null) {
                        candidate = val.getClass();
                    } else if (candidate != val.getClass()) {
                        return null;
                    }
                }
            }

            return candidate;
        }
    }

    private static class EnumerationIterator<E> implements Iterator<E> {
        private final Enumeration<E> enumeration;

        public EnumerationIterator(Enumeration<E> enumeration) {
            this.enumeration = enumeration;
        }

        public boolean hasNext() {
            return this.enumeration.hasMoreElements();
        }

        public E next() {
            return this.enumeration.nextElement();
        }

        public void remove() throws UnsupportedOperationException {
            throw new UnsupportedOperationException("Not supported");
        }
    }

    public static boolean contains(String shortStr, String longStr) {
        return longStr != null && longStr.contains(shortStr);
    }

    public static <T, K> Map<K, T> convertMap(List<T> from, Function<T, K> keyFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toMap(keyFunc, item -> item));
    }

    public static <T, K, V> Map<K, V> convertMap(List<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty).collect(Collectors.toMap(keyFunc, valueFunc));
    }

    public static <T, R> Stream<R> flatMapToStream(List<T> from, Function<T, List<R>> getListFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty)
                .map(getListFunc)
                .filter(Objects::nonNull)
                .flatMap(List::stream);
    }


    public static <T, K> Map<K, List<T>> convertMultiMap(List<T> from, Function<T, K> keyFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty).collect(Collectors.groupingBy(keyFunc,
                        Collectors.mapping(t -> t, Collectors.toList())));
    }

    public static <T, K> Map<K, List<T>> convertMultiMap(Stream<T> stream, Function<T, K> keyFunc) {
        return stream.collect(Collectors.groupingBy(keyFunc,
                Collectors.mapping(t -> t, Collectors.toList())));
    }

    public static <T, K, V> Map<K, List<V>> convertMultiMap(List<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty).collect(Collectors.groupingBy(keyFunc,
                        Collectors.mapping(valueFunc, Collectors.toList())));
    }

    public static <T, K> Map<K, Long> convertMultiMapCount(List<T> from, Function<T, K> keyFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty).collect(Collectors.groupingBy(keyFunc,
                        Collectors.mapping(t -> t, Collectors.counting())));
    }

    /** 找出不同的集合数据 */
    public  static Collection<?> getDifferent(Collection<?> prelist, Collection<?> curlist) {
        List<Object> diff = new ArrayList<>();
        Map<Object,Integer> map = new HashMap<>(curlist.size());
        for (Object item : curlist) {
            map.put(item, 1);
        }
        for (Object item : prelist) {
            if(map.get(item)!=null) {
                map.put(item, 2);
                continue;
            }
            diff.add(item);
        }
        for(Map.Entry<Object, Integer> entry:map.entrySet()) {
            if(entry.getValue()==1) {diff.add(entry.getKey());  }
        }
        return diff;
    }


    public static <T, K, V> Map<K, Long> convertMultiMapCount(List<T> from, Function<T, K> keyFunc, Function<T, V> valueFunc) {
        return Optional.ofNullable(from)
                .map(List::stream)
                .orElseGet(Stream::empty).collect(Collectors.groupingBy(keyFunc,
                        Collectors.mapping(valueFunc, Collectors.counting())));
    }

    public static <T, U, R> R mapToProperty(T t, Function<T, U> function, Function<U, R> function1) {
        return Optional.ofNullable(t)
                .map(function)
                .map(function1)
                .orElse(null);
    }

    public static boolean isNumber(Object number) {
        //先判断number不为空。
        if(number==null || "".equals(number))
            return false;
        int index = number.toString().indexOf(".");
        if (index < 0) {
            //判断number是否为数字。
            return StringUtils.isNumeric(number.toString());
        } else {
            String num1 = number.toString().substring(0, index);
            String num2 = number.toString().substring(index + 1);

            return StringUtils.isNumeric(num1) && StringUtils.isNumeric(num2);
        }
    }

    public static boolean isInteger(String str) {

        Pattern pattern = Pattern.compile("^[-+]?[d]*$");

        return pattern.matcher(str).matches();

    }

}
