# MVEL
MVFLEX Expression Language (MVEL) is a hybrid dynamic/statically typed, embeddable Expression Language and runtime for the Java Platform.

## MVEL3 and MVEL2

This is mvel version 3.x codebase, which is experimental.

mvel2 is still maintained in `mvel2` branch. If you contribute to mvel2, please submit PRs to `mvel2` branch.

## How to build

mvel3 requires a javaparser fork with MVEL support. You can clone the fork and build it as follows:
```
git clone https://github.com/mvel/javaparser-mvel.git
cd javaparser-mvel
mvn clean install
```
Then, you can clone and build mvel3 as follows:
```
git clone https://github.com/mvel/mvel.git
cd mvel
mvn clean install
```
