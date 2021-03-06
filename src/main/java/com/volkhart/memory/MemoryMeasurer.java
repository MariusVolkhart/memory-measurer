package com.volkhart.memory;

import org.jetbrains.annotations.NotNull;

import java.lang.instrument.Instrumentation;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * A tool for quantitatively measuring the memory footprint of an arbitrary object graph. In a
 * nutshell, the user gives a root object, and this class recursively and reflectively explores the
 * object's references.
 *
 * <p>
 * This class can only be used if the containing jar has been given to the Java VM as an agent, as
 * follows: {@code -javaagent:path/to/measurer.jar}
 */
public final class MemoryMeasurer {
  private static final Instrumentation instrumentation = InstrumentationGrabber.instrumentation();

  /*
   * The bare minimum memory footprint of an enum value, measured empirically. This should be
   * subtracted for any enum value encountered, since it is static in nature.
   */
  private static final long costOfBareEnumConstant = instrumentation.getObjectSize(DummyEnum.CONSTANT);

  private enum DummyEnum {
    CONSTANT;
  }

  private MemoryMeasurer() {
    // No instances
  }

  /**
   * Measures the memory footprint, in bytes, of an object graph. The object graph is defined by a
   * root object and whatever objects can be reached through that, excluding static fields,
   * {@code Class} objects, and fields defined in {@code enum}s (all these are considered shared
   * values, which should not contribute to the cost of any single object graph).
   *
   * @param rootObject the root object that defines the object graph to be measured.
   * @return the memory footprint, in bytes, of the object graph.
   */
  public static long measureBytes(@NotNull Object rootObject) {
    return measureBytes(rootObject, o -> true);
  }

  /**
   * Measures the memory footprint, in bytes, of an object graph. The object graph is defined by a
   * root object and whatever objects can be reached through that, excluding static fields,
   * {@code Class} objects, and fields defined in {@code enum}s (all these are considered shared
   * values, which should not contribute to the cost of any single object graph), and any object for
   * which the user-provided predicate returns {@code false}.
   *
   * @param rootObject the root object that defines the object graph to be measured.
   * @param objectAcceptor a predicate that returns {@code true} for objects to be explored (and
   *        treated as part of the object graph), or {@code false} to forbid the traversal to
   *        traverse the given object.
   * @return the memory footprint, in bytes, of the object graph.
   */
  public static long measureBytes(@NotNull Object rootObject, @NotNull Predicate<Object> objectAcceptor) {
    Objects.requireNonNull(objectAcceptor, "predicate");

    Predicate<Chain> completePredicate =
        new ObjectExplorer.AtMostOncePredicate().and(ObjectExplorer.notEnumFieldsOrClasses)
            .and(chain -> objectAcceptor.test(ObjectExplorer.chainToObject.apply(chain)));

    return ObjectExplorer.exploreObject(rootObject, new MemoryMeasurerVisitor(completePredicate));
  }

  private static class MemoryMeasurerVisitor implements ObjectVisitor<Long> {
    private long memory;
    @NotNull
    private final Predicate<Chain> predicate;

    MemoryMeasurerVisitor(@NotNull Predicate<Chain> predicate) {
      this.predicate = predicate;
    }

    @Override
    public Traversal visit(Chain chain) {
      if (predicate.test(chain)) {
        Object o = chain.getValue();
        memory += instrumentation.getObjectSize(o);
        if (Enum.class.isAssignableFrom(o.getClass())) {
          memory -= costOfBareEnumConstant;
        }
        return Traversal.EXPLORE;
      }
      return Traversal.SKIP;
    }

    @Override
    public Long result() {
      return memory;
    }
  }
}
