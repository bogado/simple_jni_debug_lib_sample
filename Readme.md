## Debugging does not work for instrumented tests: 

The android library in the directory `hello-libs` is a small test library with a native component and a single instrumented test, that fails.

This is an attmept to reproduce the issue I found on a much more complex library project that I'm working on with the simplest possible code. It is ruthly based in one the examples provided on the [ndk sample `hello-libs`](https://github.com/android/ndk-samples/tree/master/hello-libs). Much of the original code was removed and a single android test was added. In particular, and importantly, there's no application within this project.


### Current state

    * The project is an Android Library.
    * There are some instrumented android tests.
    * Tests run ok on their on.

### When I…

Attempt to debug the failed test with the latest stable android studio (3.5.3):

    * Android studio fails to start the native debugger.
    * Debugging java only works as expected.
    * Dual debugging also fails, including to hit java break points that work fine when run with the java-only debgger configuration.

#### Findings :

Investigating this issue, using the dual debugger configuration, I found the following :

    * The tests actully run, but no debuuger is able to connect to them.
        You can see the output of the tests on the console tab of the native debugger.
    * LLDB runnig on the host machine cannot start, and consequently cannot connect, the lldb server on the device.

Android studio will attempt to start the debbuger in two different fashions : 

    1. When the device is rooted, some older emulators after `abd root` is exectured, it run some scripts that install and run the server.
    2. With a non-rooted device it will run more or less the same things using the instrumentation.

In both cases the code will attempt to create a directory under the main application directory and install the lldb-server executable on that directory.

Since this exmample don't have an actual application the euristics used to find the application directory fails to figur eout the correct packacge to use. In this specific sample the main package for the library is `com.example.buildlibs` and the tests use the `com.example.buildlibs.test` as package name. The instrumented test apk is installed used the second value, but android studio is attempting to use the first one that is inexistent. 

When using a rooted device creating the directory on the correct place and making sure that all the correct persmissions are set makes the application debuggable again. Unfortunatly the latest emulators don't seem to be rooted anymore. 

### Evidences :

The following idea.log extracts were collected after attempting to debug on the second case as I don't have a working rooted emulator.


* The tests are installed and run fine, noticed that here it is using the correct package name :
```
2019-12-09 12:34:53,537 [thread 388]   INFO - a.run.tasks.AbstractDeployTask - Install successfully finished in 172 ms.. App restart successful without requiring a re-install. 
2019-12-09 12:34:53,538 [thread 392]   INFO -            #com.android.ddmlib - Running am instrument -w -r   -e debug true com.example.buildlibs.test/androidx.test.runner.AndroidJUnitRunner on Pixel_3a_API_29 [emulator-5554] 
2019-12-09 12:34:55,546 [entQueue-0]   INFO - s.ndk.run.lldb.ConnectLLDBTask - ABIs supported by app: [x86] 
2019-12-09 12:34:55,546 [entQueue-0]   INFO - s.ndk.run.lldb.ConnectLLDBTask - Launching AndroidTestRunConfigurationType:Hybrid native debug session on device: manufacturer=Google, model=Android SDK built for x86, API=29, codename=REL, ABIs=[x86] 
``` 

* The first sign of trouble, attempting to use `run-as` fails as the device don't know the package being used on the test :
```
2019-12-09 12:34:55,564 [entQueue-0]   WARN -            #com.android.ddmlib - execute: called 'echo $USER_ID' from the Event Dispatch Thread! 
2019-12-09 12:34:55,604 [entQueue-0]   WARN -            #com.android.ddmlib - execute: called 'run-as com.example.buildlibs getprop ro.product.model' from the Event Dispatch Thread! 
2019-12-09 12:34:55,644 [entQueue-0]   WARN - s.ndk.run.lldb.ConnectLLDBTask - run-as for the selected device appears to be broken, output was : run-as: unknown package: com.example.buildlibs 
```

* Studio ignores that and attempts to install and run the server with the wrong package name :
```
2019-12-09 12:34:55,644 [entQueue-0]   WARN - s.ndk.run.lldb.ConnectLLDBTask - Non-rooted device, run-as not working, resorting to injector to start debug session 
2019-12-09 12:34:55,645 [entQueue-0]   INFO - s.ndk.run.lldb.ConnectLLDBTask - ABIs supported by app: [x86] 
2019-12-09 12:34:55,645 [entQueue-0]   INFO - s.ndk.run.lldb.ConnectLLDBTask - Found LLDB server: "/Applications/Android Studio.app/Contents/bin/lldb/android/x86/lldb-server" 
2019-12-09 12:34:55,645 [entQueue-0]   INFO - AndroidLLDBDriverConfiguration - LLDB framework file: /Applications/Android Studio.app/Contents/bin/lldb/lib/liblldb.7.0.0.dylib 
2019-12-09 12:34:55,676 [entQueue-0]   INFO - .idea.vim.command.CommandState - Push new state: INSERT:NONE 
2019-12-09 12:34:55,676 [entQueue-0]   INFO - ome.idea.vim.group.ChangeGroup - Reset caret to a non-block shape 
2019-12-09 12:34:55,677 [entQueue-0]   INFO - .idea.vim.command.CommandState - Push new state: INSERT:NONE 
2019-12-09 12:34:55,678 [ndExecutor]   INFO - ls.ndk.run.lldb.SessionStarter - Pushing files to device 
2019-12-09 12:34:55,684 [entQueue-0]   INFO - .idea.vim.command.CommandState - Push new state: INSERT:NONE 
2019-12-09 12:34:55,691 [entQueue-0]   INFO - run.AndroidLogcatOutputCapture - stopAll() 
2019-12-09 12:34:55,696 [ndExecutor]   INFO - ls.ndk.run.lldb.SessionStarter - Remote file /data/local/tmp/lldb-server is up-to-date. 
2019-12-09 12:34:55,701 [ndExecutor]   INFO - ls.ndk.run.lldb.SessionStarter - Remote file /data/local/tmp/start_lldb_server.sh is up-to-date. 
2019-12-09 12:34:55,701 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Loading driver 
2019-12-09 12:34:55,726 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Loading startup script: /Applications/Android Studio.app/Contents/bin/lldb/shared/stl_printers/load_script 
2019-12-09 12:34:55,908 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Loading startup script: /Applications/Android Studio.app/Contents/bin/lldb/shared/jobject_printers/jstring_reader.py 
2019-12-09 12:34:55,911 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Startup command: "settings set auto-confirm true" 
2019-12-09 12:34:55,912 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Startup command: "settings set plugin.symbol-file.dwarf.comp-dir-symlink-paths /proc/self/cwd" 
2019-12-09 12:34:55,912 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Startup command: "settings set plugin.jit-loader.gdb.enable-jit-breakpoint false" 
2019-12-09 12:34:55,912 [ndExecutor]   INFO - ldb.InjectorSessionStarterImpl - Starting LLDB server using code injection 
2019-12-09 12:34:55,915 [entQueue-0]   INFO - .idea.vim.command.CommandState - Push new state: INSERT:NONE 
2019-12-09 12:34:56,008 [agerThread]   INFO - ols.ndk.run.jdwp.JdwpConnector - Attached to process 
2019-12-09 12:34:58,230 [agerThread]   INFO - ols.ndk.run.jdwp.JdwpConnector - Process is paused 
2019-12-09 12:34:58,230 [agerThread]   INFO - ols.ndk.run.jdwp.JdwpConnector - Evaluating expression: java.io.File lldbBinDir = new java.io.File("/data/data/com.example.buildlibs/lldb/bin");
lldbBinDir.mkdirs();
java.io.File serverFile = new java.io.File("/data/data/com.example.buildlibs/lldb/bin/lldb-server");
if (!(serverFile.exists() && serverFile.lastModified()>=1539074920000L)){
  java.lang.ProcessBuilder pb1 = new java.lang.ProcessBuilder("sh", "-c","cat /data/local/tmp/lldb-server>/data/data/com.example.buildlibs/lldb/bin/lldb-server && chmod 700 /data/data/com.example.buildlibs/lldb/bin/lldb-server;");
  pb1.directory(lldbBinDir).start().waitFor();
}
java.io.File startScriptFile = new java.io.File("/data/data/com.example.buildlibs/lldb/bin/start_lldb_server.sh");
if (!(startScriptFile.exists() && startScriptFile.lastModified()>=1539074920000L)){
  java.lang.ProcessBuilder pb2 = new java.lang.ProcessBuilder("sh", "-c","cat /data/local/tmp/start_lldb_server.sh>/data/data/com.example.buildlibs/lldb/bin/start_lldb_server.sh && chmod 700 /data/data/com.example.buildlibs/lldb/bin/start_lldb_server.sh;");
  pb2.directory(lldbBinDir).start().waitFor();
}
java.lang.ProcessBuilder pb3 = new java.lang.ProcessBuilder("sh", "-c","/data/data/com.example.buildlibs/lldb/bin/start_lldb_server.sh /data/data/com.example.buildlibs/lldb unix-abstract /com.example.buildlibs.test-0 platform-1575923695645.sock \"lldb process:gdb-remote packets\"");
pb3.directory(lldbBinDir).start();
 
```

* The instrumentation fails due to the fact that the folders mentioned on the shell code above do not exist:
```
2019-12-09 12:34:58,415 [agerThread]   INFO - ols.ndk.run.jdwp.JdwpConnector - Evaluation took 185 ms 
2019-12-09 12:34:58,415 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:34:58,420 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 1 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:34:58,924 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:34:58,929 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 2 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:34:59,432 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:34:59,437 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 3 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:34:59,942 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:34:59,948 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 4 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:00,452 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:00,458 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 5 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:00,959 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:00,967 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 6 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:01,470 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:01,476 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 7 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:01,978 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:01,984 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 8 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:02,486 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:02,493 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Failed to connect platform (attempt 9 of 10) - retrying.  Error was: failed to get reply to handshake packet 
2019-12-09 12:35:02,996 [ndExecutor]   INFO - ndk.run.lldb.AndroidLLDBDriver - Connecting to LLDB server: unix-abstract-connect://[emulator-5554]/com.example.buildlibs.test-0/platform-1575923695645.sock 
2019-12-09 12:35:03,002 [ndExecutor]   WARN - ndk.run.lldb.AndroidLLDBDriver - Giving up making LLDB connection after 10 attempts 
2019-12-09 12:35:03,002 [ndExecutor]   WARN - n.AndroidNativeAppDebugProcess - failed to get reply to handshake packet 
com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriverException: failed to get reply to handshake packet
	at com.jetbrains.cidr.execution.debugger.backend.lldb.LLDBDriver$ThrowIfNotValid.throwIfNeeded(LLDBDriver.java:164)
	at com.android.tools.ndk.run.lldb.AndroidLLDBDriver.connectPlatform(AndroidLLDBDriver.java:308)
	at com.android.tools.ndk.run.lldb.AndroidLLDBDriver.commonLoad(AndroidLLDBDriver.java:157)
	at com.android.tools.ndk.run.lldb.AndroidLLDBDriver.loadForAttach(AndroidLLDBDriver.java:212)
	at com.android.tools.ndk.run.AndroidNativeAppDebugProcess.doLoadTarget(AndroidNativeAppDebugProcess.java:203)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess.lambda$start$1(CidrDebugProcess.java:319)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess$VoidDebuggerCommand.call(CidrDebugProcess.java:690)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess$VoidDebuggerCommand.call(CidrDebugProcess.java:684)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$doExecuteCommand$2.invokeSuspend(CidrDebuggerCommandExecutor.kt:91)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$doExecuteCommand$2.invoke(CidrDebuggerCommandExecutor.kt)
	at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatchedOrReturn(Undispatched.kt:84)
	at kotlinx.coroutines.BuildersKt__Builders_commonKt.withContext(Builders.common.kt:142)
	at kotlinx.coroutines.BuildersKt.withContext(Unknown Source)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor.doExecuteCommand(CidrDebuggerCommandExecutor.kt:86)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor.executeCommand(CidrDebuggerCommandExecutor.kt:71)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$executeCommandAsync$1.invokeSuspend(CidrDebuggerCommandExecutor.kt:56)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(Dispatched.kt:236)
	at com.intellij.util.concurrency.BoundedTaskExecutor.doRun(BoundedTaskExecutor.java:220)
	at com.intellij.util.concurrency.BoundedTaskExecutor.access$100(BoundedTaskExecutor.java:26)
	at com.intellij.util.concurrency.BoundedTaskExecutor$2.lambda$run$0(BoundedTaskExecutor.java:198)
	at com.intellij.util.ConcurrencyUtil.runUnderThreadName(ConcurrencyUtil.java:224)
	at com.intellij.util.concurrency.BoundedTaskExecutor$2.run(BoundedTaskExecutor.java:194)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
2019-12-09 12:35:03,078 [ndExecutor]  ERROR - brains.cidr.execution.debugger - failed to get reply to handshake packet 
com.intellij.diagnostic.LogEventException: failed to get reply to handshake packet
	at com.android.tools.ndk.run.crash.CrashLoggingEvent.makeException(CrashLoggingEvent.java:102)
	at com.android.tools.ndk.run.AndroidNativeAppDebugProcess.createLoggingException(AndroidNativeAppDebugProcess.java:490)
	at com.android.tools.ndk.run.AndroidNativeAppDebugProcess.handleLaunchException(AndroidNativeAppDebugProcess.java:480)
	at com.android.tools.ndk.run.AndroidNativeAppDebugProcess.doLoadTarget(AndroidNativeAppDebugProcess.java:227)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess.lambda$start$1(CidrDebugProcess.java:319)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess$VoidDebuggerCommand.call(CidrDebugProcess.java:690)
	at com.jetbrains.cidr.execution.debugger.CidrDebugProcess$VoidDebuggerCommand.call(CidrDebugProcess.java:684)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$doExecuteCommand$2.invokeSuspend(CidrDebuggerCommandExecutor.kt:91)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$doExecuteCommand$2.invoke(CidrDebuggerCommandExecutor.kt)
	at kotlinx.coroutines.intrinsics.UndispatchedKt.startUndispatchedOrReturn(Undispatched.kt:84)
	at kotlinx.coroutines.BuildersKt__Builders_commonKt.withContext(Builders.common.kt:142)
	at kotlinx.coroutines.BuildersKt.withContext(Unknown Source)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor.doExecuteCommand(CidrDebuggerCommandExecutor.kt:86)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor.executeCommand(CidrDebuggerCommandExecutor.kt:71)
	at com.jetbrains.cidr.execution.debugger.CidrDebuggerCommandExecutor$executeCommandAsync$1.invokeSuspend(CidrDebuggerCommandExecutor.kt:56)
	at kotlin.coroutines.jvm.internal.BaseContinuationImpl.resumeWith(ContinuationImpl.kt:33)
	at kotlinx.coroutines.DispatchedTask.run(Dispatched.kt:236)
	at com.intellij.util.concurrency.BoundedTaskExecutor.doRun(BoundedTaskExecutor.java:220)
	at com.intellij.util.concurrency.BoundedTaskExecutor.access$100(BoundedTaskExecutor.java:26)
	at com.intellij.util.concurrency.BoundedTaskExecutor$2.lambda$run$0(BoundedTaskExecutor.java:198)
	at com.intellij.util.ConcurrencyUtil.runUnderThreadName(ConcurrencyUtil.java:224)
	at com.intellij.util.concurrency.BoundedTaskExecutor$2.run(BoundedTaskExecutor.java:194)
	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1149)
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:624)
	at java.lang.Thread.run(Thread.java:748)
2019-12-09 12:35:03,078 [ndExecutor]  ERROR - brains.cidr.execution.debugger - Android Studio 3.5.3  Build #AI-191.8026.42.35.6010548 
2019-12-09 12:35:03,078 [ndExecutor]  ERROR - brains.cidr.execution.debugger - JDK: 1.8.0_202-release; VM: OpenJDK 64-Bit Server VM; Vendor: JetBrains s.r.o 
2019-12-09 12:35:03,078 [ndExecutor]  ERROR - brains.cidr.execution.debugger - OS: Mac OS X 
2019-12-09 12:35:03,078 [ndExecutor]  ERROR - brains.cidr.execution.debugger - Last Action: Debug 
2019-12-09 12:35:06,007 [entQueue-0]   WARN - brains.cidr.execution.debugger - Cannot detach/abort. Forcing driver termination 
```

