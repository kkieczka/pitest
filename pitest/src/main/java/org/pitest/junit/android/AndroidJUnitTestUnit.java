package org.pitest.junit.android;

import org.pitest.testapi.AbstractTestUnit;
import org.pitest.testapi.Description;
import org.pitest.testapi.ResultCollector;
import org.pitest.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Created by Krystian Kieczka on 2018-01-18.
 */
public class AndroidJUnitTestUnit extends AbstractTestUnit {

  private static final Logger LOG = Log.getLogger();

  // TODO
  private static final String ADB_BINARY = "/home/krystian/Development/SDKs/android-sdk-linux/platform-tools/adb";

  private static final String ADB_INSTRUMENT = "shell am instrument";
  private static final String ADB_PULL = "pull";
  private static final String ADB_COVERAGE_FILE_DEV_PATH = "/sdcard/coverage.ec";
  private static final String ADB_JUNIT_RUNNER = "android.support.test.runner.AndroidJUnitRunner";

  private static Path tmpDir = null;

  private class InstrumentedTestRunResult {
    /** flag indicating whether execution of this test was successful (regardless of test result) */
    boolean successfullyExecuted;
    /** test result */
    boolean testResult = false;
    /** if execution was not successful, the reason why (if possible to determine, null otherwise) */
    String failureCause = null;
    /** path to generated EMMA coverage file, valid on device/emulator */
    String coverageFilePath = null;

    InstrumentedTestRunResult(String failureCause) {
      this.successfullyExecuted = false;
      this.failureCause = failureCause;
    }
    InstrumentedTestRunResult(boolean testResult, String coverageFilePath) {
      this.successfullyExecuted = true;
      this.testResult = testResult;
      this.coverageFilePath = coverageFilePath;
    }
  }

  public AndroidJUnitTestUnit(Description description) {
    super(description);
  }

  private static Path getTmpDir() {
    if (tmpDir == null) {
      try {
        tmpDir = Files.createTempDirectory("pitest-android");
      } catch (IOException e) {
        throw new RuntimeException(e); // TODO can sth more be done in such case?
      }
    }
    return tmpDir;
  }

  private InstrumentedTestRunResult parseRunnerOutput(String str) {
    if (str.contains("INSTRUMENTATION_FAILED")) {
      String cause = "<cause unknown>";
      int pos;
      while ((pos = str.indexOf("INSTRUMENTATION_STATUS: ")) != -1) {

        int startIndex = pos + "INSTRUMENTATION_STATUS: ".length();
        int endIndex = str.indexOf('\n', startIndex);
        String keyVal = str.substring(startIndex, endIndex != -1 ? endIndex : str.length());

        if (keyVal.startsWith("Error=")) {
          cause = keyVal.substring("Error=".length());
          break;
        }

        str = str.substring(startIndex);
      }
      LOG.severe("Instrumentation invocation failed: " + cause);
      return new InstrumentedTestRunResult(cause);

    } else if (str.contains("INSTRUMENTATION_STATUS_CODE: 1")) {
      // only one testcase is run with every invocation of 'am instrument',
      // so the next INSTRUMENTATION_STATUS_CODE should contain the result
      // of the test
      int startPos = str.indexOf("INSTRUMENTATION_STATUS_CODE: 1") + "INSTRUMENTATION_STATUS_CODE: 1".length();
      int resultPos = str.indexOf("INSTRUMENTATION_STATUS_CODE: ", startPos);

      if (resultPos != -1) {
        resultPos += "INSTRUMENTATION_STATUS_CODE: ".length();
        String resultStr = str.substring(resultPos, str.indexOf('\n', resultPos));
        LOG.info("Test result: " + resultStr);

        // get coverage file path
        String covFilePath = null;
        if (str.contains("coverageFilePath")) {
          int covFileStartPos = str.indexOf("coverageFilePath=") + "coverageFilePath=".length();
          covFilePath = str.substring(covFileStartPos, str.indexOf('\n', covFileStartPos));
        }

        try {
          int result = Integer.parseInt(resultStr);
          if (result == 0) {
            return new InstrumentedTestRunResult(true, covFilePath);
          } else if (result < 0) {
            return new InstrumentedTestRunResult(false, covFilePath);
          } else {
            LOG.warning("Unexpected test result value: " + result);
            return new InstrumentedTestRunResult("Unexpected test result value: "  + result);
          }
        } catch (NumberFormatException ex) {
          LOG.severe("Could not parse result string (" + resultStr + ")");
          return new InstrumentedTestRunResult("Could not parse result string (" + resultStr + ")");
        }

      }
    }
    LOG.severe("Could not find test result in command output");
    return new InstrumentedTestRunResult("Could not find test result in command output");
  }

  private void getCoverageInfo(String covFilePath, String pkgName) {

    // get coverage file contents using 'run-as' to allow reading the file without
    // root permissions
    //Path destDir = getTmpDir().toAbsolutePath();
    ArrayList<String> covProcessParams = new ArrayList<>();
    covProcessParams.add(ADB_BINARY);
    covProcessParams.add("shell");
    covProcessParams.add("run-as");
    covProcessParams.add(pkgName);
    covProcessParams.add("cat");
    covProcessParams.add(covFilePath);
    ProcessBuilder pb = new ProcessBuilder(covProcessParams);
    pb.redirectErrorStream(true);

    try {
      Process getCoverageProcess = pb.start();
      getCoverageProcess.waitFor();

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      InputStream is = getCoverageProcess.getInputStream();
      int nRead;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        bos.write(data, 0, nRead);
      }

      bos.flush();

      byte[] coverage = bos.toByteArray();
      // check if magic bytes are ok
      if (coverage[0] != 0x01 || coverage[1] != (byte)0xc0 || coverage[2] != (byte)0xc0) {
        LOG.warning("Output is not a valid coverage file");
        return;
      }

      ByteArrayInputStream bis = new ByteArrayInputStream(coverage);

//      BufferedReader br = new BufferedReader(new InputStreamReader(getCoverageProcess.getInputStream()));
//      byte[] data = br.read
//      StringBuilder output = new StringBuilder();
//      String line;
//      while ((line = br.readLine()) != null) {
//        output.append(line).append('\n');
//      }
      //LOG.info(output.toString());

//      Path covFileName = Paths.get(covFilePath).getFileName();
//      Path destFile = destDir.resolve(covFileName);
//      if (Files.notExists(destFile)) {
//        throw new RuntimeException("No " + covFileName.toString() + " in " + getTmpDir() + " after adb pull!");
//      }


      // get path to class files
      String classFilesPath = System.getenv("PITEST_ANDROID_CLASSFILES_PATH");
      if (classFilesPath == null) {
        throw new RuntimeException("No class files path set");
      }
      JacocoCoverageReportGenerator generator = new JacocoCoverageReportGenerator();
      generator.generateReport(bis, Paths.get(classFilesPath));

    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void execute(ResultCollector rc) {
    LOG.info("AndroidJUnitTestUnit execute()");

    rc.notifyStart(this.getDescription());

    // get tested app's package name left by Pitest Gradle plugin
    // TODO make it static?
    String testAppPkgName = System.getenv("PITEST_ANDROID_PKGNAME");
    if (testAppPkgName == null) {
      throw new RuntimeException("No package name set");
    }

    String testedAppPkgName = System.getenv("PITEST_ANDROID_TESTED_APP_ID");
    if (testedAppPkgName == null) {
      throw new RuntimeException("No test package name set");
    }
    String testRunner = System.getenv("PITEST_ANDROID_INSTRUMENTATION_RUNNER");
    if (testRunner == null) {
      testRunner = ADB_JUNIT_RUNNER;
    }

    // compose parameters for adb
    ArrayList<String> params = new ArrayList<>();
    params.add(ADB_BINARY);
//    params.add(ADB_INSTRUMENT);
    params.add("shell");
    params.add("am");
    params.add("instrument");
    params.add("-w"); // wait for completion
    params.add("-r"); // print raw results
    params.add("-e coverage true"); // generate EMMA coverage file
    //params.add("-e coverageFile " + ADB_COVERAGE_FILE_DEV_PATH); // specify EMMA coverage file location
    params.add("-e class " + getDescription().getFirstTestClass() + "#" + getDescription().getName());
    params.add(testAppPkgName + "/" + testRunner);

    ProcessBuilder pb = new ProcessBuilder(params);
    pb.redirectErrorStream(true);

    try {
      Process testProcess = pb.start();
      testProcess.waitFor();

      BufferedReader br = new BufferedReader(new InputStreamReader(testProcess.getInputStream()));
      StringBuilder output = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        output.append(line).append('\n');
      }

      InstrumentedTestRunResult result = parseRunnerOutput(output.toString());
      if (result.successfullyExecuted) {

        if (result.coverageFilePath != null) {
          getCoverageInfo(result.coverageFilePath, testedAppPkgName);
        } else {
          LOG.warning("Failed to get coverage info for " + getDescription().getName());
        }

        if (result.testResult) {
          rc.notifyEnd(this.getDescription());
        } else {
          rc.notifyEnd(this.getDescription(), new Exception("Test failed"));
        }

      } else {
        rc.notifyEnd(this.getDescription(), new Exception(result.failureCause != null ? result.failureCause : ""));
      }

    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
