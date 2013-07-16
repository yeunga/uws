/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
*  NRC disclaims any warranties,        Le CNRC dénie toute garantie
*  expressed, implied, or               énoncée, implicite ou légale,
*  statutory, of any kind with          de quelque nature que ce
*  respect to the software,             soit, concernant le logiciel,
*  including without limitation         y compris sans restriction
*  any warranty of merchantability      toute garantie de valeur
*  or fitness for a particular          marchande ou de pertinence
*  purpose. NRC shall not be            pour un usage particulier.
*  liable in any event for any          Le CNRC ne pourra en aucun cas
*  damages, whether direct or           être tenu responsable de tout
*  indirect, special or general,        dommage, direct ou indirect,
*  consequential or incidental,         particulier ou général,
*  arising from the use of the          accessoire ou fortuit, résultant
*  software.  Neither the name          de l'utilisation du logiciel. Ni
*  of the National Research             le nom du Conseil National de
*  Council of Canada nor the            Recherches du Canada ni les noms
*  names of its contributors may        de ses  participants ne peuvent
*  be used to endorse or promote        être utilisés pour approuver ou
*  products derived from this           promouvoir les produits dérivés
*  software without specific prior      de ce logiciel sans autorisation
*  written permission.                  préalable et particulière
*                                       par écrit.
*
*  This file is part of the             Ce fichier fait partie du projet
*  OpenCADC project.                    OpenCADC.
*
*  OpenCADC is free software:           OpenCADC est un logiciel libre ;
*  you can redistribute it and/or       vous pouvez le redistribuer ou le
*  modify it under the terms of         modifier suivant les termes de
*  the GNU Affero General Public        la “GNU Affero General Public
*  License as published by the          License” telle que publiée
*  Free Software Foundation,            par la Free Software Foundation
*  either version 3 of the              : soit la version 3 de cette
*  License, or (at your option)         licence, soit (à votre gré)
*  any later version.                   toute version ultérieure.
*
*  OpenCADC is distributed in the       OpenCADC est distribué
*  hope that it will be useful,         dans l’espoir qu’il vous
*  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
*  without even the implied             GARANTIE : sans même la garantie
*  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
*  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
*  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
*  General Public License for           Générale Publique GNU Affero
*  more details.                        pour plus de détails.
*
*  You should have received             Vous devriez avoir reçu une
*  a copy of the GNU Affero             copie de la Licence Générale
*  General Public License along         Publique GNU Affero avec
*  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
*  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
*                                       <http://www.gnu.org/licenses/>.
*
*  $Revision: 5 $
*
************************************************************************
*/

package ca.nrc.cadc.uws.server;

import java.util.Date;

import org.apache.log4j.Logger;

import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;

/**
 *
 * @author pdowler
 */
public abstract class AbstractExecutor  implements JobExecutor
{
    private static final Logger log = Logger.getLogger(AbstractExecutor.class);

    protected JobUpdater jobUpdater;
    protected Class<JobRunner> jobRunnerClass;

    private AbstractExecutor() { }

    protected AbstractExecutor(JobUpdater jobUpdater, Class jobRunnerClass)
    {
        if (jobUpdater == null)
            throw new IllegalArgumentException("jobUpdater cannot be null");
        if (jobRunnerClass == null)
            throw new IllegalArgumentException("jobRunnerClass cannot be null");
        this.jobUpdater = jobUpdater;
        this.jobRunnerClass = jobRunnerClass;
    }

    @Override
    public void terminate()
        throws InterruptedException
    {
        // no-op
    }

    public void setJobUpdater(JobUpdater jobUpdater)
    {
        this.jobUpdater = jobUpdater;
    }

    public void setJobRunnerClass(Class<JobRunner> jobRunnerClass)
    {
        this.jobRunnerClass = jobRunnerClass;
    }

    @Override
    public final void execute(Job job)
        throws JobNotFoundException, JobPersistenceException, JobPhaseException, TransientException
    {
        execute(job, null);
    }

    /**
     * Execute the specified job. The job is executed synchronously (in the calling thread)
     * if the sync argument is not null. This method transitions the phase from PENDING
     * to QUEUED and, if successful, instantiates and configures the JobRunner and then
     * executes the job.
     * </p><p>
     * Note for subclasses: the actual execution is performed by calling either
     * syncExecute or the asyncExecute method.
     *
     * @param job
     * @param sync
     * @throws JobNotFoundException
     * @throws JobPersistenceException
     * @throws JobPhaseException
     * @throws TransientException
     */
    @Override
    public final void execute(Job job, SyncOutput sync)
        throws JobNotFoundException, JobPersistenceException, JobPhaseException, TransientException
    {
        if (job == null)
            throw new IllegalArgumentException("BUG: Job cannot be null");
        log.debug("execute: " + job.getID() + " sync=" + (sync != null));
        log.debug(job.getID() + ": PENDING -> QUEUED");
        ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.PENDING, ExecutionPhase.QUEUED);
        if (!ExecutionPhase.QUEUED.equals(ep))
        {
            ExecutionPhase actual = jobUpdater.getPhase(job.getID());
            log.debug(job.getID() + ": PENDING -> QUEUED [FAILED] -- was " + actual);
            throw new JobPhaseException("cannot execute job " + job.getID() + " when phase = " + actual);
        }
        job.setExecutionPhase(ep);
        log.debug(job.getID() + ": PENDING -> QUEUED [OK]");

        try
        {
            log.debug(job.getID() + ": creating " + jobRunnerClass.getName());
            final JobRunner jobRunner = jobRunnerClass.newInstance();
            jobRunner.setJobUpdater(jobUpdater);
            jobRunner.setJob(job);
            jobRunner.setSyncOutput(sync);

            if (sync != null)
            {
                executeSync(jobRunner);
            }
            else
            {
                executeAsync(job, jobRunner);
            }
        }
        catch(InstantiationException ex)
        {
            throw new RuntimeException("configuration error: failed to load " + jobRunnerClass.getName(), ex);
        }
        catch(IllegalAccessException ex)
        {
            throw new RuntimeException("configuration error: failed to load " + jobRunnerClass.getName(), ex);
        }
        finally
        {

        }
    }

    /**
     * Execute the job synchronously (in the calling thread).
     *
     * @param jobRunner
     */
    protected void executeSync(JobRunner jobRunner)
    {
        jobRunner.run();
    }

    /**
     * Execute the job asynchronously.
     *
     * @param job
     * @param jobRunner
     */
    protected abstract void executeAsync(Job job, JobRunner jobRunner);

    /**
     * Abort a currently PENDING, QUEUED, or EXECUTING job. This method tries to
     * transition the phase to ABORTED and if successful, it kills the job.
     * </p><p>
     * Note for subclasses: the actual kill is performed by calling the abortJob method.
     *
     * @param job
     * @throws JobNotFoundException
     * @throws JobPersistenceException
     * @throws JobPhaseException
     * @throws TransientException
     */
    @Override
    public void abort(Job job)
        throws JobNotFoundException, JobPersistenceException, JobPhaseException, TransientException
    {
        log.debug("abort: " + job.getID());
        // can plausibly go from PENDING, QUEUED, EXECUTING -> ABORTED
        ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.PENDING, ExecutionPhase.ABORTED, new Date());
        if (!ExecutionPhase.ABORTED.equals(ep))
        {
            ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.ABORTED, new Date());
            if (!ExecutionPhase.ABORTED.equals(ep))
            {
                ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, ExecutionPhase.ABORTED, new Date());
                if (!ExecutionPhase.ABORTED.equals(ep))
                    return; // no phase change - do nothing
            }
        }
        job.setExecutionPhase(ep);
        abortJob(job.getID());
    }

    protected abstract void abortJob(String jobID);
}
