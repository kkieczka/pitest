/*
 * Copyright 2015 Jason Fehr
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.maven.report;

import java.io.File;
import java.util.Locale;

import org.apache.maven.doxia.siterenderer.Renderer;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.pitest.maven.report.generator.ReportGenerationContext;
import org.pitest.maven.report.generator.ReportGenerationManager;
import org.pitest.util.PitError;

/**
 * Generates a report of the pit mutation testing.
 * 
 * @goal report
 * @phase site
 */
public class PitReportMojo extends AbstractMavenReport {

	private static final String SITE_REPORTS_FOLDER = "pit-reports";
	
	//TODO what about the exportLineCoverage option?
	//TODO add getters and setters for each field
	
    /**
     * @component
     * @required
     * @readonly
     */
    private Renderer siteRenderer;
    
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * When set indicates that generation of the site report should be skipped.
     * 
     * @parameter default-value="false"
     */
    private boolean skip;
    
    /**
     * Base directory where all pit reports are written to by the mutationCoverage goal.  If 
     * timestampedReports is true (the default), then the actual reports will be contained in a 
     * subdirectory within this directory.  If timestampedReports is false, the actual reports 
     * will be in this directory. 
     * 
     * @parameter default-value="${project.build.directory}/pit-reports"
     *            expression="${reportsDirectory}"
     */
    private File reportsDirectory;
    
    private ReportGenerationManager reportGenerationManager;
    
    public PitReportMojo() {
    	super();
    	
    	this.reportGenerationManager = new ReportGenerationManager();
    }
    
	public String getOutputName() {
		return SITE_REPORTS_FOLDER + File.separator + "index";
	}

	public String getName(Locale locale) {
		//TODO internationalize this (and test it)
		return "PIT Test Report";
	}

	public String getDescription(Locale locale) {
		//TODO internationalize this (and test it)
		return "Report of the pit test coverage";
	}

	@Override
	protected Renderer getSiteRenderer() {
		return this.siteRenderer;
	}

	@Override
	protected String getOutputDirectory() {
		return this.reportsDirectory.getAbsolutePath();
	}

	@Override
	protected MavenProject getProject() {
		return this.project;
	}

	@Override
	protected void executeReport(Locale locale) throws MavenReportException {
		this.getLog().debug("PitReportMojo - starting");
		
		if(!this.reportsDirectory.exists()){
			throw new PitError("could not find reports directory [" + this.reportsDirectory + "]");
		}
		
		if(!this.reportsDirectory.canRead()){
			throw new PitError("reports directory [" + this.reportsDirectory + "] not readable");
		}
		
		if(!this.reportsDirectory.isDirectory()){
			throw new PitError("reports directory [" + this.reportsDirectory + "] is actually a file, it must be a directory");
		}
		
		this.reportGenerationManager.generateSiteReport(this.buildReportGenerationContext(locale));
		
		this.getLog().debug("PitReportMojo - ending");
	}
	
	@Override
	public boolean canGenerateReport() {
		return !skip;
	}
	
	public boolean isExternalReport() {
	    return true;
	}
	
	public boolean isSkip() {
		return skip;
	}

	public File getReportsDirectory() {
		return reportsDirectory;
	}

	private ReportGenerationContext buildReportGenerationContext(Locale locale) {
		return new ReportGenerationContext(locale, this.getSink(), reportsDirectory, new File(this.getReportOutputDirectory().getAbsolutePath() + File.separator + SITE_REPORTS_FOLDER), this.getLog());
	}
	
}
