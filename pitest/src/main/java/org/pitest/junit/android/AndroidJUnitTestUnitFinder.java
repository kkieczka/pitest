package org.pitest.junit.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.pitest.testapi.Description;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitFinder;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Krystian Kieczka on 2018-01-18.
 */
public class AndroidJUnitTestUnitFinder implements TestUnitFinder {

  public static final String ANDROID_JUNIT_CLASS = "android.support.test.runner.AndroidJUnit4";

  private static String[] instrumentedTestsPaths = null;

  static {
    String instrumentedTestsPathsStr = System.getenv("PITEST_ANDROID_INSTRUMENTED_TESTS_PATH");
    if (instrumentedTestsPathsStr != null && !instrumentedTestsPathsStr.isEmpty()) {
      instrumentedTestsPaths = instrumentedTestsPathsStr.split(",");
    }

  }

  @Override
  public List<TestUnit> findTestUnits(Class<?> clazz) {
    final RunWith runWith = clazz.getAnnotation(RunWith.class);

    if (runWith == null) {

      if (instrumentedTestsPaths == null) {
        return Collections.emptyList();
      }

      CodeSource classSource = clazz.getProtectionDomain().getCodeSource();
      if (classSource == null || classSource.getLocation() == null) {
        return Collections.emptyList();
      }
      String classLocation = classSource.getLocation().getPath();
      for (String path : instrumentedTestsPaths) {
        if (classLocation.startsWith(path)) {
          return getTestsFromClass(clazz, false);
        }
      }
      return Collections.emptyList();
    }

    Class<? extends Runner> runnerClass;
    try {
      runnerClass = runWith.value();
      if (runnerClass.getSimpleName().equals("AndroidJUnit4")) {
        return getTestsFromClass(clazz, true);
      }
      return Collections.emptyList();

    } catch (TypeNotPresentException ex) {
      // expected in case of AndroidJUnit4, as it is not in classpath
      // (and probably should not be, since this class is used on device, not locally)
      if (ex.typeName().equals(ANDROID_JUNIT_CLASS)) {
        return getTestsFromClass(clazz, true);
      }
      return Collections.emptyList();
    }
  }

  // based on android.support.test.internal.runner.TestLoader
  private boolean isTestMethod(Method m, boolean runnerPresent) {
    if (m.getAnnotation(Test.class) != null) {
      return true;
    }

    if (m.getAnnotations().length != 0) {
      return false;
    }

    if (m.getParameterTypes().length == 0 && m.getReturnType().equals(Void.TYPE)
        && Modifier.isPublic(m.getModifiers())) {
      return runnerPresent || m.getName().startsWith("test");
    }
    return m.getParameterTypes().length == 0 && m.getName().startsWith("test")
            && m.getReturnType().equals(Void.TYPE);
  }

  private List<TestUnit> getTestsFromClass(Class<?> clazz, boolean runnerPresent) {
    ArrayList<TestUnit> list = new ArrayList<>();
    for (Method m : clazz.getDeclaredMethods()) {
      if (isTestMethod(m, runnerPresent)) {
        list.add(new AndroidJUnitTestUnit(new Description(m.getName(), clazz)));
      }
    }
    return list;
  }

}
