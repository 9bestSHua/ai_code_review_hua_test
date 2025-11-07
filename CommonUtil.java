package com.cbxsoftware.rest.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.cbxsoftware.rest.configuration.JacksonConfig;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Hibernate;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.SneakyThrows;
import com.cbxsoftware.rest.configuration.JacksonConfig;

@SuppressWarnings({"unused", "WeakerAccess"})
public class CommonUtil {

    private CommonUtil() {
    }

    public static <E> void forEachIndexed(final Iterable<? extends E> elements, final BiConsumer<? super E, Integer> action) {
        Objects.requireNonNull(action);
        if (elements == null) {
            return;
        }
        int index = 0;
        for (final E element : elements) {
            action.accept(element, index++);
        }
    }

    public static <T> T firstOrNull(final List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> List<T> tapFirst(final List<T> list, final Consumer<T> action) {
        if (!list.isEmpty()) {
            action.accept(list.get(0));
        }
        return list;
    }

    public static <T> ArrayList<T> insertToNewList(final Collection<T> list, final int index, final T value) {
        final ArrayList<T> newList = new ArrayList<>(list);
        newList.add(index, value);
        return newList;
    }

    @SafeVarargs
    public static <T> ArrayList<T> concatLists(final List<? extends T>... lists) {
        final ArrayList<T> result = new ArrayList<>();
        Stream.of(lists)
            .filter(Objects::nonNull)
            .flatMap(Stream::of)
            .forEach(result::addAll);
        return result;
    }

    @SafeVarargs
    public static <T> io.vavr.collection.List<T> concatListsVavr(final Iterable<T>... lists) {
        io.vavr.collection.List<T> result = io.vavr.collection.List.empty();
        for (final Iterable<T> list : lists) {
            result = result.appendAll(list);
        }
        return result;
    }

    public static String joinNonEmpty(final String separator, final String... words) {
        return StringUtils.join(io.vavr.collection.List.of(words).reject(StringUtils::isEmpty), separator);
    }

    public static Iterator<Integer> getCounter() {
        return getCounter(0, Integer.MAX_VALUE);
    }

    public static Iterator<Integer> getCounter(final int startFrom) {
        return getCounter(startFrom, Integer.MAX_VALUE);
    }

    public static Iterator<Integer> getCounter(final int startFrom, final int end) {
        return IntStream.range(startFrom, end).iterator();
    }

    public static <T> Stream<T> safeStream(final Collection<T> collection) {
        return collection == null ? Stream.empty() : collection.stream();
    }

    public static <T> Collection<T> safeCollection(final Collection<T> collection) {
        if (collection == null) {
            return Collections.emptyList();
        }
        return collection;
    }

    public static <T> List<T> safeList(final Collection<T> collection) {
        if (collection == null) {
            return Collections.emptyList();
        }
        return collection.stream().toList();
    }

    /**
     * usage:
     * <pre>
     *   List<Object> someList;
     *   List<SomeClass> casedList = someList.stream()
     *     .flatMap(filterCase(SomeClass.class))
     *     .collect(Collectors.toList());
     * </pre>
     */

    /**
     * There's no scenario that we have to know the exact type of
     * ModuleService<<? extends BaseHeaderEntity>> for client code.
     */
    @SuppressWarnings("S1452")
    public static <T, R> Function<T, Stream<R>> filterCase(final Class<R> caseTo) {
        //noinspection unchecked
        return obj -> caseTo.isAssignableFrom(obj.getClass()) ?
            Stream.of((R) obj) :
            Stream.empty();
    }

    /**
     * Prevent NPE when using vavr List.flatMap().
     * example:
     * <pre>
     *    // before
     *    someList
     *      .filter(Objects::nonNull)
     *      .filter(entry -> entry.getItems() != null)
     *      .flatMap(entry -> entry.getItems())
     *    // after
     *    someList.flatMap(safeFlatMap(entry -> entry.getItems()))
     * </pre>
     */

    /**
     * There's no scenario that we have to know the exact type of
     * ModuleService<<? extends BaseHeaderEntity>> for client code.
     */
    @SuppressWarnings("S1452")
    public static <T, R> Function<T, Iterable<R>> safeFlatMap(final Function<T, Iterable<R>> mapper) {
        return obj -> {
            if (obj != null) {
                final Iterable<R> value = mapper.apply(obj);
                if (value != null) {
                    return value; // Hadouken!!
                }
            }
            return Collections.emptyList();
        };
    }

    public static Optional<Class<?>> getListFieldType(final Field field) {
        if (!Iterable.class.isAssignableFrom(field.getType())) {
            return Optional.empty();
        }

        final Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) genericType;
            final Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            if (typeArgument instanceof TypeVariable) {
                return Optional.of((Class<?>) ((TypeVariable<?>) typeArgument).getGenericDeclaration());
            } else if (typeArgument instanceof Class) {
                return Optional.of((Class<?>) typeArgument);
            } else if (typeArgument instanceof ParameterizedType) {
                final ParameterizedType argumentType = (ParameterizedType) typeArgument;
                if (argumentType.getActualTypeArguments()[0] instanceof Class) {
                    return Optional.of((Class<?>) argumentType.getActualTypeArguments()[0]);
                }
                return Optional.of(typeArgument.getClass());
            }
        }
        return Optional.empty();
    }

    public static Class<?> getListFieldTypeOrThrow(final Field field) {
        return getListFieldType(field)
            .orElseThrow(() -> new RuntimeException("Cannot get list genericType of " +
                field.getDeclaringClass().getName() + " -> " + field.getName() + "."));
    }

    public static Class<?> getFieldActualType(Class<?> objClass, final String fieldId) throws NoSuchFieldException {
        Field field = objClass.getDeclaredField(fieldId);
        return getListFieldType(field).orElseGet(field::getType);
    }

    public static Class<?> getFieldActualType(final Field field) {
        return getListFieldType(field).orElseGet(field::getType);
    }

    /**
     * Get list type by first element in list.
     * .
     * Note: List may contain different type of element in runtime, for example
     * <code>
     * ArrayList<Integer> intList = new ArrayList<>();
     * intList.add(1);
     * ((List) intList).add("abc");
     * // intList = [1, abc]
     * </code>
     *
     * @param list Will throw exception if provided list is empty.
     */
    public static <T> Class<T> getListElementType(@Nonnull final List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            throw new IllegalArgumentException("List is empty.");
        }
        final T firstElement = list.get(0);
        // noinspection unchecked
        if (firstElement == null) {
            return null;
        }
        return (Class<T>) (Hibernate.unproxy(firstElement)).getClass();
    }

    public static <T> Option<Class<T>> getListElementTypeOption(@Nullable final List<T> list) {
        if (CollectionUtils.isEmpty(list)) {
            return Option.none();
        }
        return Option.of(getListElementType(list));
    }

    public static Map<String, Object> objToMap(final Object obj) {
        final ObjectMapper mapper = new ObjectMapper();
        // noinspection unchecked
        return mapper.convertValue(obj, Map.class);
    }

    public static <T> Comparator<T> createEmptyComparator() {
        return Comparator.comparingInt(type -> 0);
    }

    @SafeVarargs
    public static <R> Either<String, R> collectLeft(final Supplier<Either<String, R>>... eitherSuppliers) {
        final Iterator<Integer> counter = CommonUtil.getCounter(1);
        final ArrayList<String> errorList = new ArrayList<>();
        for (final Supplier<Either<String, R>> eitherSupplier : eitherSuppliers) {
            final Either<String, R> either = eitherSupplier.get();
            if (either.isRight()) {
                return either;
            } else {
                errorList.add(either.getLeft());
            }
        }
        final String errors = errorList.stream()
            .map(error -> counter.next() + "." + error)
            .collect(Collectors.joining(" | "));
        return Either.left(errors);
    }

    public static <T, R> List<R> getMissingData(final List<R> expect,
                                                final List<T> entities, final Function<T, R> requiredPropGetter) {
        final List<R> actual = entities.stream()
            .map(requiredPropGetter)
            .collect(Collectors.toList());
        return expect.stream()
            .filter(item -> !actual.contains(item))
            .collect(Collectors.toList());
    }

    public static <T, S> io.vavr.collection.List<T> orderIn(final io.vavr.collection.List<T> list,
                                                            final io.vavr.collection.List<S> byThisOrder,
                                                            final Function<T, S> keySelector) {
        return io.vavr.collection.List.ofAll(orderIn(list.toJavaList(), byThisOrder.toJavaList(), keySelector));
    }

    /**
     * Sort list by pass-in list key, and put all other after at end, e.g.:
     * list = [{1, 'a'}, {2, 'b'}, {3, 'c'}, {4, 'd'}, {5, 'e'}]
     * list = orderIn(list, [2, 3, 1], item => item[0])
     * list == [{2, 'b'}, {3, 'c'}, {1, 'a'}, {4, 'd'}, {5, 'e'}]
     */
    public static <T, S> List<T> orderIn(final List<T> list, final List<S> byThisOrder,
                                         final Function<T, S> keySelector) {
        if (CollectionUtils.isEmpty(list)) {
            return list;
        }
        final Map<S, T> keyMap = list.stream()
            .collect(Collectors.toMap(keySelector, Function.identity()));
        final List<T> newList = byThisOrder.stream()
            .map(keyMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        newList.addAll(
            list.stream()
                .filter(item -> !newList.contains(item))
                .collect(Collectors.toList())
        );
        return newList;
    }

    /**
     * Remove duplicated value by keySelector.
     *
     * @param mergeFunction return true if pass-in entry is the target entry
     */
    public static <K, V> List<V> uniqueBy(final List<V> list,
                                          final Function<V, K> keySelector,
                                          final Predicate<V> mergeFunction
    ) {
        final LinkedHashMap<K, V> map = new LinkedHashMap<>();
        for (final V entry : list) {
            map.compute(
                keySelector.apply(entry),
                (key, existedEntry) -> {
                    final boolean shouldReplaceEntry =
                        existedEntry == null || mergeFunction.test(entry) && !mergeFunction.test(existedEntry);
                    return shouldReplaceEntry ? entry : existedEntry;
                });
        }
        return new ArrayList<>(map.values());
    }

    public static List<String> getStaticFinalStringFieldValues(final Class<?> type) {
        return Stream.of(type.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .filter(field -> Modifier.isFinal(field.getModifiers()))
            .filter(field -> field.getType().equals(String.class))
            .map(field -> Try.of(() -> (String) field.get(null)))
            .filter(Try::isSuccess)
            .map(Try::get)
            .collect(Collectors.toList());
    }

    public static Integer toInteger(final Object value) {
        final int result = NumberUtils.toInt(value == null ? "" : value.toString(), Integer.MIN_VALUE);
        return result == Integer.MIN_VALUE ? null : result;
    }

    public static <T> List<T> toEmptyList(final List<T> list) {
        if (list == null) {
            return new ArrayList<>();
        }
        list.clear();
        return list;
    }

    public static <T> Optional<T> firstOptional(@Nullable final List<T> list) {
        return CollectionUtils.isEmpty(list) ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    /**
     * Read json file under the "/src/resources" directory.
     *
     * @return Optional.empty() if file not found, or throw exceptions if other error occur.
     */
    @SneakyThrows
    public static <T> Optional<T> readJson(final String resourcePath, final Class<T> valueType) {
        final ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        try {
            final InputStream resource2 = new ClassPathResource(resourcePath).getInputStream();
            final T value = objectMapper.readValue(resource2, valueType);
            return Optional.of(value);
        } catch (final FileNotFoundException e) {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public static <T> Optional<T> readJson(final InputStream inputStream, final Class<T> valueType) {
        final ObjectMapper objectMapper = SpringUtils.getBean(ObjectMapper.class);
        try {
            final T value = objectMapper.readValue(inputStream, valueType);
            return Optional.of(value);
        } catch (final FileNotFoundException e) {
            return Optional.empty();
        }
    }

    public static <T> List<T> flattenIterableItems(final Iterable<T> entries, final Function<T, Iterable<T>> childEntriesGetter) {
        final List<T> resultList = IterableUtils.toList(entries);
        for (final T entry : entries) {
            final Iterable<T> childEntries = childEntriesGetter.apply(entry);
            if (IterableUtils.isEmpty(childEntries)) {
                continue;
            }
            for (final T childEntry : childEntries) {
                resultList.add(childEntry);
            }
            resultList.addAll(flattenIterableItems(childEntries, childEntriesGetter));
        }
        return resultList;
    }

    /**
     * Combine two list with given product function.
     */
    public static <T, U, R> List<R> cartesianProductList(final List<T> list1, final List<U> list2, final BiFunction<T, U, R> productBy) {
        final List<R> results = new ArrayList<>();
        for (final T entry1 : list1) {
            for (final U entry2 : list2) {
                final R result = productBy.apply(entry1, entry2);
                if (result != null) {
                    results.add(result);
                }
            }
        }
        return results;
    }

    public static List<Method> getGetterMethods(final Object obj) {
        return getGetterMethods(obj.getClass());
    }

    public static List<Method> getGetterMethods(final Class<?> cls) {
        final List<Method> getters = Stream.of(cls.getMethods())
            .filter(method -> Modifier.isPublic(method.getModifiers()))
            .filter(method -> isGetterMethodName(method.getName()))
            .collect(Collectors.toList());
        return getters;
    }

    public static boolean isGetterMethodName(final String methodName) {
        return methodName.startsWith("get") &&
            !methodName.equals("getClass") &&
            !methodName.equals("getHibernateLazyInitializer") &&
            !methodName.equals("getInvocationHandler") &&
            !methodName.equals("getProxyClass");
    }

    public static String getterNameToFiledName(final String getterMethodName) {
        final String fieldNameInCapitalize = StringUtils.removeStart(getterMethodName, "get");
        return StringUtils.uncapitalize(fieldNameInCapitalize);
    }

    /**
     * A equal function which tolerate null input to compare two object.
     *
     * @return true if two objects are equal, false if two objects are not equal.
     */
    public static boolean safeEqual(final Object o1, final Object o2) {
        if (o1 == o2) {
            return true;
        } else if (o1 != null && o2 != null) {
            return o1.equals(o2);
        } else {
            return false;
        }
    }

    public static <V> BinaryOperator<V> takeFirst() {
        return (first, second) -> first;
    }

    public static <T> Map<String, Object> beanToMap(final T bean) {
        final Map<String, Object> map = new HashMap<>();
        if (bean == null) {
            return map;
        }
        final BeanMap beanMap = BeanMap.create(bean);
        for (final Object key : beanMap.keySet()) {
            map.put(ObjectUtils.toString(key, ""), beanMap.get(key));
        }
        return map;
    }

    public static <T> void initNullList(final T entity) {
        CommonUtil.safeStream(FieldValueUtil.getAccessors(entity, List.class, 1))
            .filter(listAccessor -> listAccessor.get() == null)
            .forEach(listAccessor -> listAccessor.set(new ArrayList<>()));
    }

    public static <T, K> Map<K, T> mapBy(final List<T> list,
                                         final Function<? super T, ? extends K> keyMapper) {
        return safeStream(list).collect(Collectors.toMap(keyMapper, Function.identity(), takeFirst()));
    }

    public static <T, K> Map<K, List<T>> groupBy(final List<T> list,
                                                 final Function<? super T, ? extends K> keyMapper) {
        return safeStream(list).collect(Collectors.groupingBy(keyMapper, LinkedHashMap::new, Collectors.toList()));
    }

    public static Class<?> getNestListFieldTypeOrThrow(final Field field) {
        return getNestListFieldType(field)
            .orElseThrow(() -> new RuntimeException("Cannot get list genericType of " +
                field.getDeclaringClass().getName() + " -> " + field.getName() + "."));
    }

    public static Optional<Class<?>> getNestListFieldType(final Field field) {
        if (!Iterable.class.isAssignableFrom(field.getType())) {
            return Optional.empty();
        }

        final Type genericType = field.getGenericType();
        return getGenericType(genericType);
    }

    public static Class<?> getInterfaceGenericClazz(final Class<?> interfaceClass, final Integer number) {
        final Type[] genericInterfaces = interfaceClass.getGenericInterfaces();

        for (final Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof final ParameterizedType parameterizedType) {

                final Type[] typeArguments = parameterizedType.getActualTypeArguments();
                final Type yType = typeArguments[number];
                if (yType instanceof Class) {
                    return (Class<?>) yType;
                }
            }
        }
        throw new RuntimeException("cannot found generic clazz!");
    }

    public static Class<?> getParentGenericClazz(final Class<?> currentClazz, final Integer number) {
        ParameterizedType genericSuperclass = (ParameterizedType) currentClazz.getGenericSuperclass();
        return (Class<?>) genericSuperclass.getActualTypeArguments()[number];
    }

    private static Optional<Class<?>> getGenericType(final Type genericType) {
        if (genericType instanceof final ParameterizedType parameterizedType) {
            final Type typeArgument = parameterizedType.getActualTypeArguments()[0];
            if (typeArgument instanceof final TypeVariable<?> typeVariable) {
                return Optional.of((Class<?>) typeVariable.getGenericDeclaration());
            } else if (typeArgument instanceof final Class<?> clazz) {
                return Optional.of(clazz);
            } else if (typeArgument instanceof final ParameterizedType innerParameterizedType) {
                if (Iterable.class.isAssignableFrom(TypeFactory.rawClass(innerParameterizedType.getRawType()))) {
                    return Optional.of((Class<?>) innerParameterizedType.getActualTypeArguments()[0]);
                }
                return Optional.of(typeArgument.getClass());
            }
        }
        return Optional.empty();
    }

    public static <T> void setNonNull(Supplier<T> tSupplier, Consumer<T> tConsumer) {
        Optional.ofNullable(tSupplier.get())
                .ifPresent(tConsumer);
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    public static Map<String, Object> filterEntityFields(Object entity) {
        Map<String, Object> result = new HashMap<>();
        Class<?> clazz = entity.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(entity);
                if (value != null) {
                    if (value instanceof ArrayList) {
                        if (!((ArrayList<?>) value).isEmpty()) {
                            result.put(field.getName(), value);
                        }
                    } else {
                        result.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
            }
        }
        return result;
    }

}
