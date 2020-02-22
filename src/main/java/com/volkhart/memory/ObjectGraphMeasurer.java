package com.volkhart.memory;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import com.volkhart.memory.ObjectExplorer.Feature;
import org.jetbrains.annotations.NotNull;

/**
 * A tool for qualitatively measuring the footprint ({@literal e.g.}, number of objects, references, primitives) of a
 * graph structure.
 */
public final class ObjectGraphMeasurer {
  private ObjectGraphMeasurer() {
    // No instances
  }

  /**
   * Measures the footprint of the specified object graph. The object graph is defined by a root
   * object and whatever objects can be reached through that, excluding static fields, {@code Class}
   * objects, and fields defined in {@code enum}s (all these are considered shared values, which
   * should not contribute to the cost of any single object graph).
   *
   * @param rootObject the root object of the object graph.
   * @return the footprint of the object graph.
   */
  public static Footprint measure(@NotNull Object rootObject) {
    return measure(rootObject, x -> true);
  }

  /**
   * Measures the footprint of the specified object graph. The object graph is defined by a root
   * object and whatever objects can be reached through that, excluding static fields, {@code Class}
   * objects, and fields defined in {@code enum}s (all these are considered shared values, which
   * should not contribute to the cost of any single object graph), and any object for which the
   * user-provided predicate returns {@code false}.
   *
   * @param rootObject the root object of the object graph.
   * @param objectAcceptor a predicate that returns {@code true} for objects to be explored (and
   *        treated as part of the footprint), or {@code false} to forbid the traversal to traverse
   *        the given object.
   * @return the footprint of the object graph.
   */
  public static Footprint measure(@NotNull Object rootObject, @NotNull Predicate<Object> objectAcceptor) {
    Objects.requireNonNull(objectAcceptor, "predicate");

    Predicate<Chain> completePredicate = ObjectExplorer.notEnumFieldsOrClasses
        .and(chain -> objectAcceptor.test(objectAcceptor.test(ObjectExplorer.chainToObject)))
        .and(new ObjectExplorer.AtMostOncePredicate());

    return ObjectExplorer.exploreObject(rootObject, new ObjectGraphVisitor(completePredicate),
        EnumSet.of(Feature.VISIT_PRIMITIVES, Feature.VISIT_NULL));
  }

  private static class ObjectGraphVisitor implements ObjectVisitor<Footprint> {
    private int objects;
    // -1 to account for the root, which has no reference leading to it
    private int references = -1;
    @NotNull
    private final Map<Class<?>, AtomicInteger> primitives = new HashMap<>();
    @NotNull
    private final Predicate<Chain> predicate;

    ObjectGraphVisitor(@NotNull Predicate<Chain> predicate) {
      this.predicate = predicate;
    }

    @Override
    public Traversal visit(Chain chain) {
      if (chain.isPrimitive()) {
        Class<?> element = chain.getValueType();
        AtomicInteger frequency = primitives.get(element);
        int occurrences = 1;
        if (frequency == null) {
          primitives.put(element, new AtomicInteger(occurrences));
        } else {
          long newCount = (long) frequency.get() + (long) occurrences;
          if (newCount > Integer.MAX_VALUE) {
              throw new IllegalStateException(String.format("too many occurrences: %s", newCount));
          }
          frequency.getAndAdd(occurrences);
        }
        return Traversal.SKIP;
      } else {
        references++;
      }
      if (predicate.test(chain) && chain.getValue() != null) {
        objects++;
        return Traversal.EXPLORE;
      }
      return Traversal.SKIP;
    }

    @Override
    public Footprint result() {
      return new Footprint(objects, references, Collections.unmodifiableMap(primitives));
    }
  }
}
