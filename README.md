# Metadata Package

This package provides a set of classes and interfaces to manage, store, and interact with metadata within your application. It allows for flexible, typed key-value pair storage, ensuring type safety and easy retrieval of metadata.

---

## How It Works

### Core Components:

- **MetadataKey<T>**: Represents a key with a specific name and type, used to identify metadata entries.
- **MetadataType<T>**: Defines the data type for the metadata value; includes methods for codecs, renderers, and behavior.
- **MetadataHolder<T>**: Encapsulates a key-value pair, where the value matches the type defined by the key.
- **MetadataContainer**: A container class that holds multiple metadata entries; provides methods to add, retrieve, and check for metadata.

### Basic Usage:

- Define keys with `MetadataKey<T>`, associating a name with a `MetadataType<T>`.
- Store data with `MetadataContainer.put()`.
- Retrieve data with `MetadataContainer.get()`.
- Type safety is enforced by the key's `MetadataType<T>`.

---

## How to Add the Package as a Dependency

### 1. Build the Package

1. **Download the source code**:
   - Clone or download the repository containing the metadata package.

2. **Build the package**:
   - Use your build tool (e.g., Maven, Gradle, or `javac`) to compile the classes into a JAR.

   Example using `javac`:
```bash
javac -d out/ src/com/brandongcobb/metadata/*.java
   jar cf metadata.jar -C out/ .
```
### 2. Add the JAR to Your Project

- **If you're using Maven**:
```xml
<dependency>
  <groupId>com.brandongcobb</groupId>
  <artifactId>metadata</artifactId>
  <version>1.0.0</version>
  <scope>system</scope>
  <systemPath>metadata.jar</systemPath>
</dependency>
```
*Alternatively, deploy the JAR to your Maven repository or reference it as a local file.*

- **If you're using Gradle**:
- **If not using a build system**, include the JAR directly in your classpath.

### 3. Use in Your Code

Import the package classes:
```java
import com.brandongcobb.metadata.*;
```
Create and store metadata:
```java
MetadataContainer container = new MetadataContainer();
MetadataKey<String> key = new MetadataKey<>("example_key", new MetadataString());
container.put(key, "Sample Data");
```
Retrieve data:
```java
String value = container.get(key);
System.out.println("Metadata value: " + value);
```
---

## Example: Storing and Retrieving Metadata
```java
import com.brandongcobb.metadata.*;

public class ExampleUsage {
    public static void main(String[] args) {
        MetadataContainer container = new MetadataContainer();

        // Define a key
        MetadataKey<Integer> scoreKey = new MetadataKey<>("score", new MetadataInteger());

        // Store some data
        container.put(scoreKey, 42);

        // Retrieve the data
        Integer score = container.get(scoreKey);
        System.out.println("Score: " + score);
    }
}
```
---

## Summary

This metadata package provides a flexible and type-safe approach to managing metadata within applications, geared toward integration with frameworks like Vyrtuous. By defining keys with specific types, it reduces errors and enhances clarity.

**Get Started Today**:

- Build the package into a JAR.
- Add it as a dependency.
- Use the classes to store and retrieve metadata in your application with ease.

---
