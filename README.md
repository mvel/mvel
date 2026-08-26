# MVEL
MVFLEX Expression Language (MVEL) is a hybrid dynamic/statically typed, embeddable Expression Language and runtime for the Java Platform.

## Document

http://mvel.documentnode.com/

## How to build

```
git clone https://github.com/mvel/mvel.git
cd mvel
mvn clean install
```

## Test case development
- Test cases should extend `BaseMvelTest` (JUnit 4) or `BaseMvelTestCase` (JUnit 3) to ensure global MVEL configuration is reset between tests.