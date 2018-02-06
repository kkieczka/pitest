package org.pitest.junit.android;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;
import org.pitest.testapi.Description;
import org.pitest.testapi.TestUnit;
import org.pitest.testapi.TestUnitFinder;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Krystian Kieczka on 2018-01-18.
 */
public class AndroidJUnitTestUnitFinder implements TestUnitFinder {

  public static final String ANDROID_JUNIT_CLASS = "android.support.test.runner.AndroidJUnit4";

  @Override
  public List<TestUnit> findTestUnits(Class<?> clazz) {
    final RunWith runWith = clazz.getAnnotation(RunWith.class);

    if (runWith == null) {
      return Collections.emptyList();
    }

    Class<? extends Runner> runnerClass;
    try {
      runnerClass = runWith.value();
      if (runnerClass.getSimpleName().equals("AndroidJUnit4")) {
        return getTestsFromClass(clazz);
      }
      return Collections.emptyList();

    } catch (TypeNotPresentException ex) {
      // expected in case of AndroidJUnit4, as it is not in classpath
      // (and probably should not be, since this class is used on device, not locally)
      if (ex.typeName().equals(ANDROID_JUNIT_CLASS)) {
        return getTestsFromClass(clazz);
      }
      return Collections.emptyList();
    }
  }

  private List<TestUnit> getTestsFromClass(Class<?> clazz) {
    ArrayList<TestUnit> list = new ArrayList<>();
    for (Method m : clazz.getMethods()) {
      if (m.getAnnotation(Test.class) != null) {
        list.add(new AndroidJUnitTestUnit(new Description(m.getName(), clazz)));
      }
    }
    return list;
  }

}
