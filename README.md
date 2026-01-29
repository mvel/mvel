# MVEL

MVFLEX Expression Language (MVEL) is a hybrid dynamic/statically typed, embeddable Expression Language and runtime for the Java Platform.

## Overview

MVEL3 is a complete rewrite of MVEL. Instead of interpreting expressions at runtime (as MVEL2 does), MVEL3 transpiles MVEL expressions into Java source code, compiles them in-memory via javac, and executes the resulting bytecode.

> **Alpha Notice:** This is an alpha release. APIs may change in future releases.

### MVEL3 and MVEL2

This is the MVEL version 3.x codebase, developed in the `main` branch.

MVEL2 is maintained separately in the [`mvel2` branch](https://github.com/mvel/mvel/tree/mvel2). If you contribute to MVEL2, please submit PRs to the `mvel2` branch.

## How to Build

**Prerequisites**: JDK 17+, Maven 3.8.7+

MVEL3 requires a [javaparser-mvel](https://github.com/mvel/javaparser-mvel) fork with MVEL-specific AST nodes. Build it first:
```bash
git clone https://github.com/mvel/javaparser-mvel.git
cd javaparser-mvel
mvn clean install
```

Then build MVEL3:
```bash
git clone https://github.com/mvel/mvel.git
cd mvel
mvn clean install
```

## Usage

MVEL3 provides a fluent builder API with three context types: **Map**, **List**, and **POJO**.

### Map Context

Variables are passed via `Map<String, Object>` and extracted by name.

```java
Map<String, Type<?>> types = new HashMap<>();
types.put("x", Type.type(int.class));
types.put("y", Type.type(int.class));

Evaluator<Map<String, Object>, Void, Integer> evaluator =
    MVEL.<Object>map(Declaration.from(types))
        .<Integer>out(Integer.class)
        .expression("x + y")
        .imports(Collections.emptySet())
        .compile();

Map<String, Object> vars = new HashMap<>();
vars.put("x", 3);
vars.put("y", 5);

Integer result = evaluator.eval(vars); // returns 8
```

### POJO Context

Variables are extracted from a POJO via getter methods.

```java
CompilerParameters<Person, Void, Boolean> params =
    MVEL.<Person>pojo(Person.class,
         Declaration.of("age", int.class))
        .<Boolean>out(Boolean.class)
        .expression("age > 20")
        .imports(Collections.emptySet())
        .classManager(new ClassManager())
        .build();

Evaluator<Person, Void, Boolean> evaluator = new MVEL().compile(params);

Person person = new Person("John", 25);
Boolean result = evaluator.eval(person); // returns true
```

### List Context

Variables are extracted from a `List` by positional index.

```java
Declaration[] decls = new Declaration[]{
    new Declaration("a", int.class),
    new Declaration("b", int.class)
};

Evaluator<List<Object>, Void, Integer> evaluator =
    new MVEL().compileListExpression(
        "a + b",
        Integer.class,
        Collections.emptySet(),
        decls
    );

Integer result = evaluator.eval(List.of(3, 5)); // returns 8
```

### Expressions vs Blocks

- **Expression**: A single expression that returns its value directly.
  ```java
  .expression("x + y")
  ```
- **Block**: Multiple statements. Use `return` to specify the return value.
  ```java
  .block("a = 4; b = 5; int c = 6; return a + b + c;")
  ```
  Variables assigned in a block are written back to the context (Map entries are updated, POJO setters are called).

### Generics

Use `Type.type()` to specify generic types:

```java
types.put("names", Type.type(List.class, "<String>"));
types.put("people", Type.type(List.class, "<" + Person.class.getCanonicalName() + ">"));
```

### Imports

Pass a `Set<String>` of fully qualified class names for types used in expressions:

```java
Set<String> imports = new HashSet<>();
imports.add("java.util.List");
imports.add(Person.class.getCanonicalName());
```
