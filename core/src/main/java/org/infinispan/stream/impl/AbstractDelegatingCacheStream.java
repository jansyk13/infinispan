package org.infinispan.stream.impl;

import org.infinispan.CacheStream;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Delegate that forwards all the of the method calls to the underlying cache stream.  It is assumed that a CacheStream
 * is returned for all intermediate operations.
 */
public class AbstractDelegatingCacheStream<R> implements CacheStream<R> {
   protected CacheStream<?> underlyingStream;

   public AbstractDelegatingCacheStream(CacheStream<R> stream) {
      this.underlyingStream = stream;
   }

   private CacheStream<R> castStream(CacheStream stream) {
      return stream;
   }

   @Override
   public CacheStream<R> sequentialDistribution() {
      underlyingStream = underlyingStream.sequentialDistribution();
      return this;
   }

   @Override
   public CacheStream<R> parallelDistribution() {
      underlyingStream = underlyingStream.parallelDistribution();
      return this;
   }

   @Override
   public CacheStream<R> filterKeySegments(Set<Integer> segments) {
      underlyingStream = underlyingStream.filterKeySegments(segments);
      return this;
   }

   @Override
   public CacheStream<R> filterKeys(Set<?> keys) {
      underlyingStream = underlyingStream.filterKeys(keys);
      return this;
   }

   @Override
   public CacheStream<R> distributedBatchSize(int batchSize) {
      underlyingStream = underlyingStream.distributedBatchSize(batchSize);
      return this;
   }

   @Override
   public CacheStream<R> segmentCompletionListener(SegmentCompletionListener listener) {
      underlyingStream = underlyingStream.segmentCompletionListener(listener);
      return this;
   }

   @Override
   public CacheStream<R> disableRehashAware() {
      underlyingStream = underlyingStream.disableRehashAware();
      return this;
   }

   @Override
   public CacheStream<R> timeout(long timeout, TimeUnit unit) {
      underlyingStream = underlyingStream.timeout(timeout, unit);
      return this;
   }

   @Override
   public void forEach(Consumer<? super R> action) {
      castStream(underlyingStream).forEach(action);
   }

   @Override
   public void forEachOrdered(Consumer<? super R> action) {
      castStream(underlyingStream).forEachOrdered(action);
   }

   @Override
   public Object[] toArray() {
      return underlyingStream.toArray();
   }

   @Override
   public <A> A[] toArray(IntFunction<A[]> generator) {
      return underlyingStream.toArray(generator);
   }

   @Override
   public R reduce(R identity, BinaryOperator<R> accumulator) {
      return castStream(underlyingStream).reduce(identity, accumulator);
   }

   @Override
   public Optional<R> reduce(BinaryOperator<R> accumulator) {
      return castStream(underlyingStream).reduce(accumulator);
   }

   @Override
   public <U> U reduce(U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
      return castStream(underlyingStream).reduce(identity, accumulator, combiner);
   }

   @Override
   public <R1> R1 collect(Supplier<R1> supplier, BiConsumer<R1, ? super R> accumulator, BiConsumer<R1, R1> combiner) {
      return castStream(underlyingStream).collect(supplier, accumulator, combiner);
   }

   @Override
   public Iterator<R> iterator() {
      return castStream(underlyingStream).iterator();
   }

   @Override
   public Spliterator<R> spliterator() {
      return castStream(underlyingStream).spliterator();
   }

   @Override
   public boolean isParallel() {
      return underlyingStream.isParallel();
   }

   @Override
   public Stream<R> sequential() {
      underlyingStream = (CacheStream<?>) underlyingStream.sequential();
      return this;
   }

   @Override
   public Stream<R> parallel() {
      underlyingStream = (CacheStream<?>) underlyingStream.parallel();
      return this;
   }

   @Override
   public Stream<R> unordered() {
      underlyingStream = (CacheStream<?>) underlyingStream.unordered();
      return this;
   }

   @Override
   public Stream<R> onClose(Runnable closeHandler) {
      underlyingStream = (CacheStream<?>) underlyingStream.onClose(closeHandler);
      return this;
   }

   @Override
   public void close() {
      underlyingStream.close();
   }

   @Override
   public Stream<R> sorted() {
      underlyingStream = (CacheStream<?>) underlyingStream.sorted();
      return this;
   }

   @Override
   public Stream<R> sorted(Comparator<? super R> comparator) {
      underlyingStream = (CacheStream<?>) castStream(underlyingStream).sorted(comparator);
      return this;
   }

   @Override
   public Stream<R> peek(Consumer<? super R> action) {
      underlyingStream = (CacheStream<?>) castStream(underlyingStream).peek(action);
      return this;
   }

   @Override
   public Stream<R> limit(long maxSize) {
      underlyingStream = (CacheStream<?>) underlyingStream.limit(maxSize);
      return this;
   }

   @Override
   public Stream<R> skip(long n) {
      underlyingStream = (CacheStream<?>) underlyingStream.limit(n);
      return this;
   }

   @Override
   public Stream<R> filter(Predicate<? super R> predicate) {
      underlyingStream = (CacheStream<?>) castStream(underlyingStream).filter(predicate);
      return this;
   }

   @Override
   public <R1> Stream<R1> map(Function<? super R, ? extends R1> mapper) {
      underlyingStream = (CacheStream<?>) castStream(underlyingStream).map(mapper);
      return (Stream<R1>) this;
   }

   @Override
   public <R1> Stream<R1> flatMap(Function<? super R, ? extends Stream<? extends R1>> mapper) {
      underlyingStream = (CacheStream<?>) castStream(underlyingStream).flatMap(mapper);
      return (Stream<R1>) this;
   }

   @Override
   public Stream<R> distinct() {
      underlyingStream = (CacheStream<?>) underlyingStream.distinct();
      return this;
   }

   @Override
   public <R1, A> R1 collect(Collector<? super R, A, R1> collector) {
      return castStream(underlyingStream).collect(collector);
   }

   @Override
   public Optional<R> min(Comparator<? super R> comparator) {
      return castStream(underlyingStream).min(comparator);
   }

   @Override
   public Optional<R> max(Comparator<? super R> comparator) {
      return castStream(underlyingStream).max(comparator);
   }

   @Override
   public long count() {
      return underlyingStream.count();
   }

   @Override
   public boolean anyMatch(Predicate<? super R> predicate) {
      return castStream(underlyingStream).anyMatch(predicate);
   }

   @Override
   public boolean allMatch(Predicate<? super R> predicate) {
      return castStream(underlyingStream).allMatch(predicate);
   }

   @Override
   public boolean noneMatch(Predicate<? super R> predicate) {
      return castStream(underlyingStream).noneMatch(predicate);
   }

   @Override
   public Optional<R> findFirst() {
      return castStream(underlyingStream).findFirst();
   }

   @Override
   public Optional<R> findAny() {
      return castStream(underlyingStream).findAny();
   }

   @Override
   public IntStream mapToInt(ToIntFunction<? super R> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }

   @Override
   public LongStream mapToLong(ToLongFunction<? super R> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }

   @Override
   public DoubleStream mapToDouble(ToDoubleFunction<? super R> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }

   @Override
   public IntStream flatMapToInt(Function<? super R, ? extends IntStream> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }

   @Override
   public LongStream flatMapToLong(Function<? super R, ? extends LongStream> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }

   @Override
   public DoubleStream flatMapToDouble(Function<? super R, ? extends DoubleStream> mapper) {
      throw new UnsupportedOperationException("Primitive delegate is not yet supported!");
   }
}
