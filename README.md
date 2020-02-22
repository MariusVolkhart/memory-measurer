# Memory Measurer

## Using
This library requires Java 11 or higher.

```java
long memory = MemoryMeasurer.measureBytes(new HashMap());
```

or

```java
Footprint footprint = ObjectGraphMeasurer.measure(new HashMap());
```

### Quick tip

- `mvn clean package` - to package jar
- To use the MemoryMeasurer (to measure the footprint of an object
graph in bytes), this parameter needs to be passed to th VM:
`-javaagent:path/to/object-explorer.jar`