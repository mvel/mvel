mvel
====

MVEL (MVFLEX Expression Language)

Before the fix:

 Results :

 Tests in error: 
   testFunctionPointerAsParam(org.mvel2.tests.core.MutationsTests): 1 out of 1 threads terminated due to exception: Detailed Failure Report:  FIRST TEST: { squareRoot = Math.sqrt; new String(St...
   testFunctionPointerInAssignment(org.mvel2.tests.core.MutationsTests): 1 out of 1 threads terminated due to exception: Detailed Failure Report:  FIRST TEST: { squareRoot = Math.sqrt; i = squareRoo...
   testFunctionDefAndCall(org.mvel2.tests.core.FunctionsTest): 1 out of 1 threads terminated due to exception: [Error: not a statement, or badly formed structure] [Near : {... function hey...
   testFunctionDefAndCall2(org.mvel2.tests.core.FunctionsTest): [Error: not a statement, or badly formed structure]
 [Near : {... function heyFoo() { return 'Foobar'; }; ....}]
                      ^
 [Line: 2, Column: 8]
   testFunctionDefAndCall4(org.mvel2.tests.core.FunctionsTest): 1 out of 1 threads terminated due to exception: [Error: not a statement, or badly formed structure] [Near : {... function tes...
   testFunctionDefAndCall5(org.mvel2.tests.core.FunctionsTest): 1 out of 1 threads terminated due to exception: [Error: not a statement, or badly formed structure] [Near : {... function tes...
   testJIRA207(org.mvel2.tests.core.FunctionsTest): [Error: not a statement, or badly formed structure]
 [Near : {... } ....}]
             ^
 [Line: 6, Column: 1]
   testBranchesWithReturn(org.mvel2.tests.core.FunctionsTest): [Error: not a statement, or badly formed structure]
 [Near : {... return $b; ....}]
             ^
 [Line: 9, Column: 10]
   testDeepNestedLoopsInFunction(org.mvel2.tests.core.FunctionsTest): 1 out of 1 threads terminated due to exception: [Error: not a statement, or badly formed structure] [Near : {... i); } }; if ...
   testMVEL225(org.mvel2.tests.core.FunctionsTest): [Error: not a statement, or badly formed structure]
 [Near : {... def f() { int a=1;a++;return a; }; f(); ....}]
                 ^
 [Line: 1, Column: 36]
   testFunctionSemantics(org.mvel2.tests.core.FunctionsTest): 1 out of 1 threads terminated due to exception: [Error: not a statement, or badly formed structure] [Near : {... __0 = ''; 'b...

 Tests run: 1065, Failures: 0, Errors: 11, Skipped: 0

 [INFO] ------------------------------------------------------------------------
 [ERROR] BUILD FAILURE
 [INFO] ------------------------------------------------------------------------
 [INFO] There are test failures.

Please refer to /home/dev/work/mvel2/target/surefire-reports for the individual test results.
[INFO] ------------------------------------------------------------------------
[INFO] For more information, run Maven with the -e switch
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 46 seconds
[INFO] Finished at: Wed Jun 04 14:01:07 MDT 2014
[INFO] Final Memory: 32M/399M

