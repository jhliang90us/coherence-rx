/*
 * File: RxNamedCache.java
 *
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates.
 *
 * You may not use this file except in compliance with the Universal Permissive
 * License (UPL), Version 1.0 (the "License.")
 *
 * You may obtain a copy of the License at https://opensource.org/licenses/UPL.
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */

package com.oracle.coherence.rx;


import com.tangosol.internal.util.processor.CacheProcessors;

import com.tangosol.net.AsyncNamedCache;
import com.tangosol.net.NamedCache;

import com.tangosol.net.cache.CacheMap;

import com.tangosol.util.Filter;
import com.tangosol.util.InvocableMap;

import com.tangosol.util.aggregator.Count;

import com.tangosol.util.filter.AlwaysFilter;

import com.tangosol.util.function.Remote;

import rx.Observable;

import java.util.Collection;
import java.util.Map;


/**
 * Reactive Extensions (RxJava) {@link NamedCache} API.
 *
 * @param <K> the type of the entry keys
 * @param <V> the type of the entry values
 *
 * @author Aleksandar Seovic  2015.02.15
 */
public interface RxNamedCache<K, V>
    {
    // ---- factory methods -------------------------------------------------

    /**
     * Factory method for RxNamedCache instance.
     *
     * @param <K>   the type of the entry keys
     * @param <V>   the type of the entry values
     * @param cache the NamedCache to create the wrapper for
     *
     * @return  the RxNamedCache instance for the given NamedCache
     */
    static <K, V> RxNamedCache<K, V> rx(NamedCache<K, V> cache)
        {
        return new RxNamedCacheImpl<>(cache.async());
        }


    /**
     * Factory method for RxNamedCache instance.
     *
     * @param <K>   the type of the entry keys
     * @param <V>   the type of the entry values
     * @param cache the AsyncNamedCache to create the wrapper for
     *
     * @return  the RxNamedCache instance for the given NamedCache
     */
    static <K, V> RxNamedCache<K, V> rx(AsyncNamedCache<K, V> cache)
        {
        return new RxNamedCacheImpl<>(cache);
        }

    // ---- CacheMap methods ------------------------------------------------

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     *
     * @param key the key whose associated value is to be returned
     *
     * @return  an {@link Observable} for the value to which the specified key is
     *          mapped
     */
    default Observable<V> get(K key)
        {
        return invoke(key, CacheProcessors.get());
        }

    /**
     * Get all the specified keys, if they are in the cache.
     * <p>
     * For each key that is in the cache, that key and its corresponding value
     * will be placed in the observable stream that is returned by this method.
     * The absence of a key in the returned stream indicates that it was not in
     * the cache, which may imply (for caches that can load behind the scenes)
     * that the requested data could not be loaded.
     *
     * @param colKeys a collection of keys that may be in the named cache
     *
     * @return  an {@link Observable} stream of map entries for the specified
     *          keys passed in <tt>colKeys</tt>
     */
    default Observable<? extends Map.Entry<? extends K, ? extends V>> getAll(Collection<? extends K> colKeys)
        {
        return invokeAll(colKeys, CacheProcessors.get()).filter(e -> e.getValue() != null);
        }

    /**
     * Associates the specified value with the specified key in this cache. If
     * the cache previously contained a mapping for this key, the old value is
     * replaced.
     * <p>
     * Invoking this method is equivalent to the following call:
     * <pre>
     *     put(oKey, oValue, CacheMap.EXPIRY_DEFAULT);
     * </pre>
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    default Observable<Void> put(K key, V value)
        {
        return put(key, value, CacheMap.EXPIRY_DEFAULT);
        }

    /**
     * Associates the specified value with the specified key in this cache. If
     * the cache previously contained a mapping for this key, the old value is
     * replaced. This variation of the {@link #put(Object oKey, Object oValue)}
     * method allows the caller to specify an expiry (or "time to live") for the
     * cache entry.
     *
     * @param key     key with which the specified value is to be associated
     * @param value   value to be associated with the specified key
     * @param cMillis the number of milliseconds until the cache entry will
     *                expire, also referred to as the entry's "time to live";
     *                pass {@link CacheMap#EXPIRY_DEFAULT} to use the cache's
     *                default time-to-live setting; pass {@link
     *                CacheMap#EXPIRY_NEVER} to indicate that the cache entry
     *                should never expire; this milliseconds value is <b>not</b>
     *                a date/time value, such as is returned from
     *                System.currentTimeMillis()
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    default Observable<Void> put(K key, V value, long cMillis)
        {
        return invoke(key, CacheProcessors.put(value, cMillis));
        }

    /**
     * Copies all of the mappings from the specified map to this map.
     *
     * @param map mappings to be added to this map
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> putAll(Map<? extends K, ? extends V> map)
        {
        return (Observable) invokeAll(map.keySet(), CacheProcessors.putAll(map)).filter(entry -> false);
        }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key key whose mapping is to be removed from the map
     *
     * @return an {@link Observable} that will emit the previous value
     *         associated with the <tt>key</tt>
     */
    default Observable<V> remove(K key)
        {
        return invoke(key, CacheProcessors.remove());
        }

    /**
     * Removes all of the mappings from the specified keys from this map, if
     * they are present in the cache.
     *
     * @param colKeys a collection of keys that may be in the named cache
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> removeAll(Collection<? extends K> colKeys)
        {
        return (Observable) invokeAll(colKeys, CacheProcessors.removeBlind()).filter(entry -> false);
        }

    /**
     * Removes all of the mappings that satisfy the specified filter from this
     * map.
     *
     * @param filter a Filter that determines the set of entries to remove
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> removeAll(Filter filter)
        {
        return (Observable) invokeAll(filter, CacheProcessors.removeBlind()).filter(entry -> false);
        }

    // ---- QueryMap methods ------------------------------------------------

    /**
     * Return an {@link Observable} which will emit all the keys contained in
     * this map.
     *
     * @return an {@link Observable} for all the keys for this map
     */
    default Observable<K> keySet()
        {
        return keySet(AlwaysFilter.INSTANCE);
        }

    /**
     * Return an {@link Observable} which will emit the keys for all the entries
     * contained in this map that satisfy the criteria expressed by the filter.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return an {@link Observable} which will emit the keys for entries that
     *         satisfy the specified criteria
     */
    default Observable<K> keySet(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.nop()).map(Map.Entry::getKey);
        }

    /**
     * Return an {@link Observable} which will emit all the entries contained
     * in this map.
     *
     * @return an {@link Observable} which will emit all entries in this map
     */
    default Observable<? extends Map.Entry<? extends K, ? extends V>> entrySet()
        {
        return entrySet(AlwaysFilter.INSTANCE);
        }

    /**
     * Return an {@link Observable} which will emit the entries contained in
     * this map that satisfy the criteria expressed by the filter.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return an {@link Observable} which will emit the entries that satisfy
     *         the specified criteria
     */
    default Observable<? extends Map.Entry<? extends K, ? extends V>> entrySet(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.get());
        }

    /**
     * Return an {@link Observable} which will emit all the values contained
     * in this map.
     *
     * @return an {@link Observable} which will emit all the values in this map
     */
    default Observable<V> values()
        {
        return values(AlwaysFilter.INSTANCE);
        }

    /**
     * Return an {@link Observable} which will emit the values for all the entries
     * contained in this map that satisfy the criteria expressed by the filter.
     *
     * @param filter the Filter object representing the criteria that the
     *               entries of this map should satisfy
     *
     * @return an {@link Observable} which will emit the values for entries that
     *         satisfy the specified criteria
     */
    default Observable<V> values(Filter filter)
        {
        return invokeAll(filter, CacheProcessors.get()).map(Map.Entry::getValue);
        }

    // ---- InvocableMap methods --------------------------------------------

    /**
     * Invoke the passed EntryProcessor against the Entry specified by the
     * passed key asynchronously, returning an {@link Observable} that will emit
     * the result of the invocation.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param key       the key to process; it is not required to exist within
     *                  the Map
     * @param processor the EntryProcessor to use to process the specified key
     *
     * @return an {@link Observable} that will emit the result of the invocation
     */
    <R> Observable<R> invoke(K key, InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against all the entries asynchronously,
     * returning an {@link Observable} that will emit the result of the invocation
     * for each entry.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return an {@link Observable} that will emit the result of the invocation
     *         for each entry
     */
    default <R> Observable<? extends Map.Entry<? extends K, ? extends R>>
    invokeAll(InvocableMap.EntryProcessor<K, V, R> processor)
        {
        return invokeAll(AlwaysFilter.INSTANCE, processor);
        }

    /**
     * Invoke the passed EntryProcessor against the entries specified by the
     * passed keys asynchronously, returning an {@link Observable} that will
     * emit the result of the invocation for each entry.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param collKeys  the keys to process; these keys are not required to
     *                  exist within the Map
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return an {@link Observable} that will emit the result of the invocation
     *         for each entry
     */
    <R> Observable<? extends Map.Entry<? extends K, ? extends R>>
    invokeAll(Collection<? extends K> collKeys, InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Invoke the passed EntryProcessor against the set of entries that are
     * selected by the given Filter asynchronously, returning an {@link
     * Observable} that will emit the result of the invocation for each entry.
     *
     * @param <R>       the type of value returned by the EntryProcessor
     * @param filter    a Filter that results in the set of keys to be
     *                  processed
     * @param processor the EntryProcessor to use to process the specified keys
     *
     * @return an {@link Observable} that will emit the result of the invocation
     *         for each entry
     */
    <R> Observable<? extends Map.Entry<? extends K, ? extends R>>
    invokeAll(Filter filter, InvocableMap.EntryProcessor<K, V, R> processor);

    /**
     * Perform an aggregating operation asynchronously against all the entries.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the specified entries of this Map
     *
     * @return an {@link Observable} that will emit the result of the aggregation
     */
    default <R> Observable<R> aggregate(InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator)
        {
        return aggregate(AlwaysFilter.INSTANCE, aggregator);
        }

    /**
     * Perform an aggregating operation asynchronously against the entries
     * specified by the passed keys.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param collKeys   the Collection of keys that specify the entries within
     *                   this Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the specified entries of this Map
     *
     * @return an {@link Observable} that will emit the result of the aggregation
     */
    <R> Observable<R> aggregate(Collection<? extends K> collKeys,
                                InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator);

    /**
     * Perform an aggregating operation asynchronously against the set of
     * entries that are selected by the given Filter.
     *
     * @param <R>        the type of value returned by the EntryAggregator
     * @param filter     the Filter that is used to select entries within this
     *                   Map to aggregate across
     * @param aggregator the EntryAggregator that is used to aggregate across
     *                   the selected entries of this Map
     *
     * @return an {@link Observable} that will emit the result of the aggregation
     */
    <R> Observable<R> aggregate(Filter filter,
                                InvocableMap.EntryAggregator<? super K, ? super V, R> aggregator);

    // ---- Map methods -----------------------------------------------------

    /**
     * Returns the number of entries in this cache.
     *
     * @return an {@link Observable} that will emit the number of entries
     *         in this cache
     */
    default Observable<Integer> size()
        {
        return aggregate(new Count<>());
        }

    /**
     * Returns <tt>true</tt> if this cache contains no entries.
     *
     * @return an {@link Observable} that will emit <tt>true</tt> if this cache
     *         contains no entries
     */
    default Observable<Boolean> isEmpty()
        {
        return size().map(size -> size == 0);
        }

    /**
     * Removes all of the mappings from this cache. The cache will be empty
     * after this operation completes.
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    default Observable<Void> clear()
        {
        return removeAll(AlwaysFilter.INSTANCE);
        }

    /**
     * Returns <tt>true</tt> if this cache contains a mapping for the specified key.
     *
     * @param key key whose presence in this cache is to be tested
     *
     * @return an {@link Observable} that will emit <tt>true</tt> if this cache
     *         contains a mapping for the specified key
     */
    default Observable<Boolean> containsKey(K key)
        {
        return invoke(key, InvocableMap.Entry::isPresent);
        }

    /**
     * Returns the value to which the specified key is mapped, or {@code
     * valueDefault} if this map contains no mapping for the key.
     *
     * @param key          the key whose associated value is to be returned
     * @param valueDefault the default mapping of the key
     *
     * @return an {@link Observable} that will emit the value to which the
     *         specified key is mapped, or {@code valueDefault} if this map
     *         contains no mapping for the key
     */
    default Observable<V> getOrDefault(K key, V valueDefault)
        {
        return invoke(key, CacheProcessors.getOrDefault()).map(opt -> opt.orElse(valueDefault));
        }

    /**
     * If the specified key is not already associated with a value (or is mapped
     * to {@code null}) associates it with the given value and returns {@code
     * null}, else returns the current value.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     *
     * @return an {@link Observable} that will emit the previous value associated
     *         with the specified key, or {@code null} if there was no mapping
     *         for the key.
     */
    default Observable<V> putIfAbsent(K key, V value)
        {
        return invoke(key, CacheProcessors.putIfAbsent(value));
        }

    /**
     * Removes the entry for the specified key only if it is currently mapped to
     * the specified value.
     *
     * @param key   key with which the specified value is associated
     * @param value value expected to be associated with the specified key
     *
     * @return an {@link Observable} that will emit {@code true} if the value
     *         was removed
     */
    default Observable<Boolean> remove(K key, V value)
        {
        return invoke(key, CacheProcessors.remove(value));
        }

    /**
     * Replaces the entry for the specified key only if it is currently mapped
     * to some value.
     *
     * @param key   key with which the specified value is associated
     * @param value value to be associated with the specified key
     *
     * @return an {@link Observable} that will emit the previous value associated
     *         with the specified key, or {@code null} if there was no mapping
     *         for the key.
     */
    default Observable<V> replace(K key, V value)
        {
        return invoke(key, CacheProcessors.replace(value));
        }

    /**
     * Replaces the entry for the specified key only if currently mapped to the
     * specified value.
     *
     * @param key      key with which the specified value is associated
     * @param oldValue value expected to be associated with the specified key
     * @param newValue value to be associated with the specified key
     *
     * @return an {@link Observable} that will emit {@code true} if the value
     *         was replaced
     */
    default Observable<Boolean> replace(K key, V oldValue, V newValue)
        {
        return invoke(key, CacheProcessors.replace(oldValue, newValue));
        }

    /**
     * Compute the value using the given mapping function and enter it into this
     * map (unless {@code null}), if the specified key is not already associated
     * with a value (or is mapped to {@code null}).
     * <p>
     * If the mapping function returns {@code null} no mapping is recorded. If
     * the function itself throws an (unchecked) exception, the exception is
     * rethrown, and no mapping is recorded.
     *
     * @param key             key with which the specified value is to be
     *                        associated
     * @param mappingFunction the function to compute a value
     *
     * @return an {@link Observable} that will emit the current (existing or
     *         computed) value associated with the specified key, or null if the
     *         computed value is null
     */
    default Observable<V> computeIfAbsent(K key, Remote.Function<? super K, ? extends V> mappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfAbsent(mappingFunction));
        }

    /**
     * Compute a new mapping given the key and its current mapped value, if the
     * value for the specified key is present and non-null.
     * <p>
     * If the function returns {@code null}, the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key               the key with which the specified value is to be
     *                          associated
     * @param remappingFunction the function to compute a value
     *
     * @return an {@link Observable} that will emit the new value associated
     *         with the specified key, or null if none
     */
    default Observable<V> computeIfPresent(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.computeIfPresent(remappingFunction));
        }

    /**
     * Compute a new mapping for the specified key and its current value.
     * <p>
     * If the function returns {@code null}, the mapping is removed (or remains
     * absent if initially absent). If the function itself throws an (unchecked)
     * exception, the exception is rethrown, and the current mapping is left
     * unchanged.
     *
     * @param key               the key with which the computed value is to be
     *                          associated
     * @param remappingFunction the function to compute a value
     *
     * @return an {@link Observable} that will emit the new value associated
     *         with the specified key, or null if none
     */
    default Observable<V> compute(K key, Remote.BiFunction<? super K, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.compute(remappingFunction));
        }

    /**
     * If the specified key is not already associated with a value or is
     * associated with null, associates it with the given non-null value.
     * Otherwise, replaces the associated value with the results of the given
     * remapping function, or removes if the result is {@code null}.
     * <p>
     * This method may be of use when combining multiple mapped values for a
     * key. For example, to either create or append a {@code String msg} to a
     * value mapping:
     * <pre>
     *   {@code map.merge(key, msg, String::concat)}
     * </pre>
     * <p>
     * If the function returns {@code null} the mapping is removed.  If the
     * function itself throws an (unchecked) exception, the exception is
     * rethrown, and the current mapping is left unchanged.
     *
     * @param key               key with which the resulting value is to be
     *                          associated
     * @param value             the non-null value to be merged with the
     *                          existing value associated with the key or, if no
     *                          existing value or a null value is associated
     *                          with the key, to be associated with the key
     * @param remappingFunction the function to recompute a value if present
     *
     * @return an {@link Observable} that will emit the new value associated
     *         with the specified key, or null if no value is associated with the key
     */
    default Observable<V> merge(K key, V value, Remote.BiFunction<? super V, ? super V, ? extends V> remappingFunction)
        {
        return invoke(key, CacheProcessors.merge(value, remappingFunction));
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries have been processed or the function
     * throws an exception.
     *
     * @param function the function to apply to each entry
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> replaceAll(Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return (Observable) replaceAll(AlwaysFilter.INSTANCE, function);
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries for the specified key set have been
     * processed or the function throws an exception.
     *
     * @param collKeys the keys to process; these keys are not required to exist
     *                 within the Map
     * @param function the function to apply to each entry
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> replaceAll(Collection<? extends K> collKeys,
                                        Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return (Observable) invokeAll(collKeys, CacheProcessors.replace(function)).filter(e -> false);
        }

    /**
     * Replace each entry's value with the result of invoking the given function
     * on that entry until all entries selected by the specified filter have
     * been processed or the function throws an exception.
     *
     * @param filter   the filter that should be used to select entries
     * @param function the function to apply to each entry
     *
     * @return an {@link Observable} that will be completed when the operation
     *         completes, but will not emit any values
     */
    @SuppressWarnings("unchecked")
    default Observable<Void> replaceAll(Filter filter,
                                        Remote.BiFunction<? super K, ? super V, ? extends V> function)
        {
        return (Observable) invokeAll(filter, CacheProcessors.replace(function)).filter(e -> false);
        }
    }
