/**
 * Utility for measuring the memory impact of an object and its graph.
 */
module com.volkhart.memory {
	requires java.instrument;
	requires org.jetbrains.annotations;
	exports com.volkhart.memory;
}