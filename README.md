# MVEL
MVFLEX Expression Language (MVEL) is a hybrid dynamic/statically typed, embeddable Expression Language and runtime for the Java Platform.

## Document

http://mvel.documentnode.com/

## Requirements

- Java 11 or later

## How to build

```
git clone https://github.com/mvel/mvel.git
cd mvel
mvn clean install
```

## Test case development
- Test cases should extend `BaseMvelTest` (JUnit 4) or `BaseMvelTestCase` (JUnit 3) to ensure global MVEL configuration is reset between tests.

## Migrating to 2.6.0

Java 8 is no longer supported. **Java 11 or later is required.**

- `Make.Set._()` was renamed to `Make.Set._add()` since `_` is a reserved keyword since Java 9.