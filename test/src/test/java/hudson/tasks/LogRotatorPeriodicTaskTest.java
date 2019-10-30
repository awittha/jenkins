/*
 * The MIT License
 *
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi
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
package hudson.tasks;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.FreeStyleProject;
import hudson.model.LogRotatorConfiguration;
import hudson.model.LogRotatorConfiguration.LogRotationPolicy;
import hudson.model.LogRotatorMapping;
import hudson.model.TaskListener;
import hudson.util.StreamTaskListener;
import jenkins.model.GlobalConfiguration;

public class LogRotatorPeriodicTaskTest {
	
	@Rule
    public JenkinsRule j = new JenkinsRule();
	
	protected LogRotator mockCustomRotator1;
	
	protected LogRotator mockCustomRotator2;
	
	protected LogRotator mockSpecificGlobalRotator;
	
	protected LogRotator mockCatchallGlobalRotator;
	
	protected LogRotatorMapping specificGlobalMapping;
	
	protected LogRotatorMapping catchallGlobalMapping;
	
	protected LogRotatorPeriodicTask task;
	
	protected StringWriter log;
	
	protected TaskListener listener;
	
	@Before
	public void setUp() throws IOException {
		
		mockCustomRotator1 = mock( LogRotator.class );
		
		mockCustomRotator2 = mock( LogRotator.class );
		
		mockSpecificGlobalRotator = mock( LogRotator.class );
		
		mockCatchallGlobalRotator = mock( LogRotator.class );
		
		specificGlobalMapping = new LogRotatorMapping( "rotator-.*", mockSpecificGlobalRotator );
		
		catchallGlobalMapping = new LogRotatorMapping( ".*", mockCatchallGlobalRotator );
		
		task = new LogRotatorPeriodicTask();
		
		log = new StringWriter();
		
        listener = new StreamTaskListener( log );
	}
	
	
	@Test
	public void customRotators_correctlyIgnored() throws IOException, InterruptedException {
		
		/*
		 * Test Setup
		 */
		
		// create a project with a LogRotator
		FreeStyleProject project = j.createFreeStyleProject();
		project.setBuildDiscarder( mockCustomRotator1 );
		
		// set global Log Rotation policy for jobs with a custom rotator:
		// do nothing
		LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
		assertNotNull( config );
		config.setPolicyForJobsWithCustomLogRotator( LogRotationPolicy.NONE );
		
		// this global LogRotator should match all jobs.
		// but, since the policy is "do nothing," it should NOT be used!
		config.setGlobalLogRotators(Arrays.asList( new LogRotatorMapping[] {
				catchallGlobalMapping
		}));
		
		/*
		 * Test Execution
		 */
		
		// apply LogRotation
		task.execute( listener );
		
		/*
		 * Test Validation
		 */
		
		// the custom rotator should not have been called.
		verify( mockCustomRotator1, never() )
		.perform( project );
		
		// the global rotator should not have been called.
		verify( mockCatchallGlobalRotator, never() )
		.perform( project );
	}
	
	@Test
	public void customRotators_correctlyUsed() throws IOException, InterruptedException {
		
		/*
		 * Test Setup
		 */
		
		// create a project with a LogRotator
		FreeStyleProject project = j.createFreeStyleProject();
		project.setBuildDiscarder( mockCustomRotator1 );
		
		// set global Log Rotation policy for jobs with a custom rotator:
		// use the custom rotator
		LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
		assertNotNull( config );
		config.setPolicyForJobsWithCustomLogRotator( LogRotationPolicy.CUSTOM );
		
		// this global LogRotator should match all jobs.
		// but, since the policy is "do nothing," it should NOT be used!
		config.setGlobalLogRotators(Arrays.asList( new LogRotatorMapping[] {
				catchallGlobalMapping
		}));
		
		/*
		 * Test Execution
		 */
		
		// apply LogRotation
		task.execute( listener );
		
		/*
		 * Test Validation
		 */
		
		// the custom rotator should have been called.
		verify( mockCustomRotator1 )
		.perform( project );
		
		// the global rotator should not have been called.
		verify( mockCatchallGlobalRotator, never() )
		.perform( project );
		
	}
	
	@Test
	public void customRotators_correctlyIgnoredInFavorOfGlobal() throws IOException, InterruptedException {
		
		/*
		 * Test Setup
		 */
		
		// create a project with a LogRotator
		FreeStyleProject project = j.createFreeStyleProject();
		project.setBuildDiscarder( mockCustomRotator1 );
		
		// set global Log Rotation policy for jobs with a custom rotator:
		// use the global rotators
		LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
		assertNotNull( config );
		config.setPolicyForJobsWithCustomLogRotator( LogRotationPolicy.GLOBAL );
		
		// this global LogRotator should match all jobs.
		// but, since the policy is "do nothing," it should NOT be used!
		config.setGlobalLogRotators(Arrays.asList( new LogRotatorMapping[] {
				catchallGlobalMapping
		}));
		
		/*
		 * Test Execution
		 */
		
		// apply LogRotation
		task.execute( listener );
		
		/*
		 * Test Validation
		 */
		
		// the custom rotator should not have been called.
		verify( mockCustomRotator1, never() )
		.perform( project );
		
		// the global rotator should have been called.
		verify( mockCatchallGlobalRotator )
		.perform( project );
		
	}
	
	@Test
	public void noRotator_correctlyIgnored() throws IOException, InterruptedException {
		
		/*
		 * Test Setup
		 */
		
		// create a project without a LogRotator
		FreeStyleProject project = j.createFreeStyleProject();
		
		// set global Log Rotation policy for jobs with a custom rotator:
		// do nothing
		LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
		assertNotNull( config );
		config.setPolicyForJobsWithCustomLogRotator( LogRotationPolicy.NONE );
		
		// this global LogRotator should match all jobs.
		// but, since the policy is "do nothing," it should NOT be used!
		config.setGlobalLogRotators(Arrays.asList( new LogRotatorMapping[] {
				catchallGlobalMapping
		}));
		
		/*
		 * Test Execution
		 */
		
		// apply LogRotation
		task.execute( listener );
		
		/*
		 * Test Validation
		 */
		
		// the global rotator should not have been called.
		verify( mockCatchallGlobalRotator, never() )
		.perform( project );
	}
	
	@Test
	public void noRotator_correctlyRotatedWithGlobal() throws IOException, InterruptedException {
		
		/*
		 * Test Setup
		 */
		
		// create a project without a LogRotator
		FreeStyleProject project = j.createFreeStyleProject();
		
		// set global Log Rotation policy for jobs with a custom rotator:
		// use the global log rotator
		LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
		assertNotNull( config );
		config.setPolicyForJobsWithCustomLogRotator( LogRotationPolicy.GLOBAL );
		
		// this global LogRotator should match all jobs.
		// but, since the policy is "do nothing," it should NOT be used!
		config.setGlobalLogRotators(Arrays.asList( new LogRotatorMapping[] {
				catchallGlobalMapping
		}));
		
		/*
		 * Test Execution
		 */
		
		// apply LogRotation
		task.execute( listener );
		
		/*
		 * Test Validation
		 */
		
		// the global rotator should not have been called.
		verify( mockCatchallGlobalRotator )
		.perform( project );
		
	}
	
	@Test
	public void globalRotators_checkedInOrder() {
		
	}
	
	@Test
	public void globalRotators_noRotationWhenNoneMatch() {
		
	}
	
}