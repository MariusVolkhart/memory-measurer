package com.volkhart.memory;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The footprint of an object graph.
 */
public final class Footprint {
	private final int objectCount;
	private final int referenceCount;
	private final Map<Class<?>, AtomicInteger> primitives;

	private static final Set<Class<?>> primitiveTypes = Set.of(boolean.class, byte.class, char.class,
			short.class, int.class, float.class, long.class, double.class);

	/**
	 * Constructs a Footprint, by specifying the number of objects, references, and primitives
	 * (represented as a {@link Map}).
	 *
	 * @param objectCount the number of objects
	 * @param referenceCount the number of references
	 * @param primitives the number of primitives (represented by the respective primitive classes,
	 *        e.g. {@code int.class} etc)
	 */
	Footprint(int objectCount, int referenceCount, Map<Class<?>, AtomicInteger> primitives) {
		assert (objectCount >= 0);
		assert (referenceCount >= 0);
		assert (primitiveTypes.containsAll(primitives.keySet()));

		this.objectCount = objectCount;
		this.referenceCount = referenceCount;
		this.primitives = Collections.unmodifiableMap(primitives);
	}

	/**
	 * Get the number of objects of this footprint.
	 *
	 * @return the number of objects of this footprint.
	 */
	public int getObjectCount() {
		return objectCount;
	}

	/**
	 * Get the number of references of this footprint.
	 *
	 * @return the number of references of this footprint.
	 */
	public int getReferenceCount() {
		return referenceCount;
	}

	/**
	 * Returns the number of primitives of this footprint (represented by the respective primitive
	 * classes, {@literal e.g.} {@code int.class} etc).
	 */
	public Set<Map.Entry<Class<?>, AtomicInteger>> getPrimitives() {
		return Collections.unmodifiableSet(primitives.entrySet());
	}

	@Override
	public String toString() {
		return "Footprint {objects=" + objectCount + ", references=" + referenceCount + ", primitives=" + primitives + "}";
	}
}