package org.pitest.mutationtest.execute;

import org.pitest.mutationtest.engine.Mutant;
import org.pitest.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by Krystian Kieczka on 2018-02-03.
 */
public class AndroidPackageUpdater {

  private static final Logger LOG = Log.getLogger();

  private Mutant mutatedClass;
  private Path originalClassFilePath;
  private byte[] originalClass;

  public AndroidPackageUpdater(Mutant mutatedClass) {
    this.mutatedClass = mutatedClass;
    this.originalClass = null;
  }

  public void updatePackage() {

    // 1. write class bytes to original class file

    String classesPath = System.getenv("PITEST_ANDROID_CLASSFILES_PATH");
    if (classesPath == null) {
      throw new RuntimeException("No class files path set");
    }
    String classNameWithSlashes = mutatedClass.getDetails().getId().getClassName().asInternalName();
    originalClassFilePath = Paths.get(classesPath, classNameWithSlashes + ".class");

    if (Files.exists(originalClassFilePath)) {
      try {
        originalClass = Files.readAllBytes(originalClassFilePath);

        Files.write(originalClassFilePath, mutatedClass.getBytes());

      } catch (IOException e) {
        throw new RuntimeException(e);
      }

    } else {
      LOG.warning("Resolved class file name could not be found");
    }

    // 2. invoke app building (and uploading) process skipping java files compilation
    // todo: just push prepared apk to /data/app/<pkg_name_with_suffix>/base.apk to make process faster?

    String cwd = System.getProperty("user.dir");
    if (cwd == null) {
      throw new RuntimeException("Could not determine current working directory");
    }

    Path gradlewPath = Paths.get(cwd, "../gradlew"); // fixme
    if (Files.notExists(gradlewPath)) {
      throw new RuntimeException("Could not find gradle wrapper in current working directory. "
              + "Pitest should be run from app project's main directory.");
    }

    // ./gradlew installDebug -x javaPreCompileDebug -x compileDebugJavaWithJavac

    ArrayList<String> gradlewProcessParams = new ArrayList<>(Arrays.asList(
            gradlewPath.toString(), "installDebug", "-x", "javaPreCompileDebug", "-x", "compileDebugJavaWithJavac"));
    ProcessBuilder pb = new ProcessBuilder(gradlewProcessParams);
    pb.redirectErrorStream(true);

    try {
      LOG.info("Repackaging and reinstalling the app...");
      Process gradlewProcess = pb.start();
      gradlewProcess.waitFor();

      if (gradlewProcess.exitValue() != 0) {
        LOG.warning("Gradle wrapper exited with code " + gradlewProcess.exitValue());
        BufferedReader br = new BufferedReader(new InputStreamReader(gradlewProcess.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          output.append(line).append('\n');
        }
        LOG.warning(output.toString());
      }
    } catch (IOException | InterruptedException ex) {
      ex.printStackTrace();
    }
  }

  public void restoreOriginalClass() {
    if (originalClass != null) {
      try {
        Files.write(originalClassFilePath, originalClass);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
