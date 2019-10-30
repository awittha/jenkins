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
package hudson.tasks;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import hudson.Extension;
import hudson.model.AsyncPeriodicWork;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.LogRotatorConfiguration;
import hudson.model.LogRotatorMapping;
import hudson.model.TaskListener;
import jenkins.model.BuildDiscarder;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

/**
 * Periodic task that checks if it's time to rotate build logs.
 * 
 * @author awitt
 * @since 2.203
 *
 */
@Extension
public class LogRotatorPeriodicTask extends AsyncPeriodicWork {
    
    private static final Logger LOGGER = Logger.getLogger( LogRotatorPeriodicTask.class.getName() );
    
    public LogRotatorPeriodicTask() {
        super("Checking if LogRotator rules should be applied");
    }

    @Override
    protected void execute(TaskListener listener) throws IOException, InterruptedException {
        
        LogRotatorConfiguration config = GlobalConfiguration.all().get( LogRotatorConfiguration.class );
        
        if( config != null ) {
            // there is config
            if( config.isEnableRotation() && config.getUpdateIntervalHours() > 0 ) {
                // and we should update periodically
                
                long millis_since_last_update = System.currentTimeMillis() - config.getLastRotated();
                double hours_since_last_update = millis_since_last_update / 1000.0 / 60.0 / 60.0;
                
                if( hours_since_last_update >= config.getUpdateIntervalHours() ) {
                    // actually do it
                    LOGGER.log( getNormalLoggingLevel(), "Starting to apply LogRotator rules..." );
                    
                    Jenkins jenkins = Jenkins.getInstanceOrNull();
                    
                    if( null == jenkins ) { return; }
                    
                    for( Item item : jenkins.getAllItems() ) {
                        
                        for( Job<?,?> job : item.getAllJobs() ) {
                                                        
                            BuildDiscarder discarder = job.getBuildDiscarder();
                                                        
                            try {
                            
                                if( discarder instanceof LogRotator ) {
                                    // the job has defined its own LogRotator                                    
                                    switch( config.getPolicyForJobsWithCustomLogRotator() ) {
                                    case NONE:
                                        LOGGER.log( FINER, "Job [" + job.getName() + "] defined its own LogRotator. Policy is to not rotate its logs." );
                                        break;
                                    case CUSTOM:
                                        LOGGER.log( FINER, "Job [" + job.getName() + "] defined its own LogRotator. Policy is to use that to rotate its logs." );
                                        discarder.perform( job );
                                        break;
                                    case GLOBAL:
                                        LOGGER.log( FINER, "Job [" + job.getName() + "] defined its own LogRotator. Policy is to ignore that and use a globally-defined LogRotator." );
                                        applyGlobalLogRotators( job, config.getGlobalLogRotators() );
                                        break;
                                    default:
                                        LOGGER.log(
                                            SEVERE,
                                            "Job [" +
                                            job.getName() +
                                            "] defined its own LogRotator. Unsupported policy found: [" +
                                            config.getPolicyForJobsWithCustomLogRotator() + "]" );
                                        break;
                                    }
                                } else {
                                    // the job has NOT defined its own LogRotator                                    
                                    switch( config.getPolicyForJobsWithoutCustomLogRotator() ) {
                                    case NONE:
                                        LOGGER.log( FINER, "Job [" + job.getName() + "] did not define a LogRotator. Policy is to not rotate its logs." );
                                        break;
                                    case GLOBAL:
                                        LOGGER.log( FINER, "Job [" + job.getName() + "] did not define a LogRotator. Policy is to use a globally-defined LogRotator." );
                                        applyGlobalLogRotators( job, config.getGlobalLogRotators() );
                                        break;
                                    default:
                                        LOGGER.log(
                                            SEVERE,
                                            "Job [" +
                                            job.getName() +
                                            "] did not define a LogRotator. Unsupported policy found: [" +
                                            config.getPolicyForJobsWithoutCustomLogRotator() + "]" );
                                        break;
                                    }
                                }
                            } catch( InterruptedException ie ) {
                                // re-throw InterruptedException always, so we're interruptable.
                                throw ie;
                            } catch( Exception e ) {
                                // any other exception, chomp it and try to rotate the rest of the logs.
                                LOGGER.log(
                                    WARNING,
                                    "Unexpected exception encountered while rotating build logs. Skipping job [" +
                                    job.getFullName() + 
                                    "] & trying the next.",
                                    e );
                            }
                        }
                    }
                    
                    config.setLastRotated( System.currentTimeMillis() );
                }
            }
        }
    }
    
    /*
     * Non-javadoc: will run every minute, but only to check if sufficient time has passed to execute.
     * Since AsyncPeriodicWork's timer is non-configurable, we need to run every minute, and check
     * if there's actually anything to do inside the execute(...) 
     */
    @Override
    public long getRecurrencePeriod() { return MIN; }
    
    /**
     * Apply the global log rotation policies defined in {@link LogRotatorConfiguration} to a job.
     * 
     * @param job      A job whose logs should be rotated
     * @param rotators Log rotation policies.
     * 
     * @throws InterruptedException see {@link LogRotator#perform(Job)}
     * @throws IOException see {@link LogRotator#perform(Job)}
     */
    protected void applyGlobalLogRotators( Job<?,?> job, List<LogRotatorMapping> rotators ) throws IOException, InterruptedException {
        
        for( LogRotatorMapping mapping : rotators ) {
            
            if( job.getFullName().matches( mapping.getJobNameRegex() ) ) {
                
                mapping.getLogRotator().perform( job );
                
                return;
            }
        }
        
        LOGGER.log(
            FINE,
            "Did not find any global LogRotator that would apply to Job [" +
            job.getFullName() +
            "]. Its logs will not be rotated." );
    }
}