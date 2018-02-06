package org.pitest.junit.android;


import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.analysis.IClassCoverage;
//import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.IPackageCoverage;
//import org.jacoco.core.analysis.ISourceFileCoverage;
//import org.jacoco.core.internal.analysis.LineImpl;
import org.jacoco.core.tools.ExecFileLoader;
import org.pitest.util.Log;
import sun.pitest.CodeCoverageStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * Created by Krystian Kieczka on 2018-01-28.
 */
public class JacocoCoverageReportGenerator {

  private static final Logger LOG = Log.getLogger();

  private void emitCoverageInfo(String className) {

    // make sure that given class is loaded (and thus present in CodeCoverageStore)
    try {
      Class.forName(className.replace('/', '.'));
    } catch (ClassNotFoundException e) {
      LOG.warning("Could not load class " + className);
    }

    int id = CodeCoverageStore.getClassIdByName(className);
    if (id != -1) {
      int probesCount = CodeCoverageStore.getProbesCount(id);
      if (probesCount != -1) {
        boolean[] probes = new boolean[probesCount - 1]; // first probe means an overall class hit
        for (int i = 0; i < probesCount - 1; i++) {
          probes[i] = true;
        }
        CodeCoverageStore.visitProbes(id, 0, probes);
      } else {
        LOG.warning("No probes for class " + className);
      }
    } else {
      LOG.warning("Class " + className + " not present in CodeCoverageStore");
    }

  }

  public void generateReport(InputStream coverageFile, Path classFilesDir) {
    try {
      ExecFileLoader execFileLoader = new ExecFileLoader();
      execFileLoader.load(coverageFile);

      CoverageBuilder coverageBuilder = new CoverageBuilder();
      Analyzer analyzer = new Analyzer(execFileLoader.getExecutionDataStore(), coverageBuilder);
      analyzer.analyzeAll(classFilesDir.toFile());
      IBundleCoverage bundleCoverage = coverageBuilder.getBundle("test");

      Collection<IPackageCoverage> packages = bundleCoverage.getPackages();
      for (IPackageCoverage pc : packages) {
        for (IClassCoverage cc : pc.getClasses()) {
          if (cc.getInstructionCounter().getCoveredCount() > 0) {
            LOG.fine("Emitting coverage info for " + cc.getName());
            emitCoverageInfo(cc.getName());
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();
    }


  }

}
