/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Martin Eigenbrodt
 * Copyright (c) 2019 Intel Corporation
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import static java.util.logging.Level.INFO;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

/**
 * @author awitt
 * @since 2.203
 *
 */
@Extension
public class LogRotatorConfiguration extends GlobalConfiguration {
	
	private static final Logger LOGGER = Logger.getLogger( LogRotatorConfiguration.class.getName() );
	
	public static final boolean DEFAULT_ENABLE_ROTATION = true;
	protected boolean enableRotation = DEFAULT_ENABLE_ROTATION;
	
	public static final int DEFAULT_UPDATE_INTERVAL_HOURS = 24;
	protected int updateIntervalHours = DEFAULT_UPDATE_INTERVAL_HOURS;
	
	protected long lastRotated;
	
	public static final LogRotationPolicy DEFAULT_POLICY_FOR_JOBS_WITH_ROTATORS = LogRotationPolicy.CUSTOM;
	protected LogRotationPolicy policyForJobsWithCustomLogRotator;
	
	public static final LogRotationPolicy DEFAULT_POLICY_FOR_JOBS_WITHOUT_ROTATORS = LogRotationPolicy.NONE;
	protected LogRotationPolicy policyForJobsWithoutCustomLogRotator;
	
	protected List<LogRotatorMapping> globalLogRotators = new ArrayList<LogRotatorMapping>();
	
	@DataBoundConstructor
	public LogRotatorConfiguration() {
		load();
	}
	
	public boolean isEnableRotation() {
		return enableRotation;
	}
	
	@DataBoundSetter
	public void setEnableRotation(boolean enableRotation) {
		this.enableRotation = enableRotation;
		
		save();
	}

	public int getUpdateIntervalHours() {
		return updateIntervalHours;
	}

	@DataBoundSetter
	public void setUpdateIntervalHours(int updateIntervalHours) {
		this.updateIntervalHours = updateIntervalHours;
		
		save();
	}

	public long getLastRotated() {
		return lastRotated;
	}

	/**
	 * Not a {@link DataBoundSetter}; this should only be set by internal code,
	 * never by an external user or process.
	 * 
	 * @param lastRotated The timestamp of the last time log rotation rules were applied.
	 */
	public void setLastRotated(long lastRotated) {
		this.lastRotated = lastRotated;
		
		save();
	}

	public LogRotationPolicy getPolicyForJobsWithCustomLogRotator() {
		return policyForJobsWithCustomLogRotator;
	}

	@DataBoundSetter
	public void setPolicyForJobsWithCustomLogRotator(LogRotationPolicy policyForJobsWithCustomLogRotator) {
		LOGGER.log( INFO, "Got rotation policy for jobs w/ custom log rotator as ENUM: " + policyForJobsWithCustomLogRotator );
		this.policyForJobsWithCustomLogRotator = policyForJobsWithCustomLogRotator;
		
		save();
	}
	
	@DataBoundSetter
	public void setPolicyForJobsWithCustomLogRotator(String policyForJobsWithCustomLogRotator) {
		LOGGER.log( INFO, "Got rotation policy for jobs w/ custom log rotator as STRING: " + policyForJobsWithCustomLogRotator );
		this.policyForJobsWithCustomLogRotator = LogRotationPolicy.valueOf( policyForJobsWithCustomLogRotator );
		
		save();
	}

	public LogRotationPolicy getPolicyForJobsWithoutCustomLogRotator() {
		return policyForJobsWithoutCustomLogRotator;
	}

	@DataBoundSetter
	public void setPolicyForJobsWithoutCustomLogRotator(LogRotationPolicy policyForJobsWithoutCustomLogRotator) {
		LOGGER.log( INFO, "Got rotation policy for jobs w/out custom log rotator as ENUM: " + policyForJobsWithoutCustomLogRotator );
		this.policyForJobsWithoutCustomLogRotator = policyForJobsWithoutCustomLogRotator;
		
		save();
	}
	
	@DataBoundSetter
	public void setPolicyForJobsWithoutCustomLogRotator(String policyForJobsWithoutCustomLogRotator) {
		LOGGER.log( INFO, "Got rotation policy for jobs w/out custom log rotator as STRING: " + policyForJobsWithoutCustomLogRotator );
		this.policyForJobsWithoutCustomLogRotator = LogRotationPolicy.valueOf( policyForJobsWithoutCustomLogRotator );
		
		save();
	}

	public List<LogRotatorMapping> getGlobalLogRotators() {
		return globalLogRotators;
	}

	@DataBoundSetter
	public void setGlobalLogRotators(List<LogRotatorMapping> globalLogRotators) {
		this.globalLogRotators = globalLogRotators;
		
		save();
	}
	
	/**
	 * How to rotate the logs of a given job.
	 * 
	 * @author awitt
	 * @since 2.203
	 */
	public static enum LogRotationPolicy {
		NONE,   /* do nothing */
		CUSTOM, /* use the job's own LogRotator */
		GLOBAL  /* apply a globally-defined LogRotator */
	}

}
