package dcos.metronome.jobrun

import dcos.metronome.JobRunDoesNotExist
import dcos.metronome.model._
import mesosphere.marathon.core.task.Task
import org.joda.time.DateTime

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ Future, Promise }

object JobRunServiceFixture {

  def simpleJobRunService(): JobRunService = new JobRunService {
    val specs = TrieMap.empty[JobRunId, StartedJobRun]

    override def getJobRun(jobRunId: JobRunId): Future[Option[StartedJobRun]] = {
      Future.successful(specs.get(jobRunId))
    }

    override def killJobRun(jobRunId: JobRunId): Future[StartedJobRun] = {
      specs.get(jobRunId) match {
        case Some(value) => Future.successful(value)
        case None        => Future.failed(JobRunDoesNotExist(jobRunId))
      }
    }

    override def activeRuns(jobSpecId: JobId): Future[Iterable[StartedJobRun]] = {
      Future.successful(specs.values.filter(_.jobRun.jobSpec.id == jobSpecId))
    }
    override def listRuns(filter: (JobRun) => Boolean): Future[Iterable[StartedJobRun]] = {
      Future.successful(specs.values.filter(r => filter(r.jobRun)))
    }
    override def startJobRun(jobSpec: JobSpec): Future[StartedJobRun] = {
      val run = JobRun(JobRunId(jobSpec), jobSpec, JobRunStatus.Active, DateTime.now, None, Map.empty[Task.Id, JobRunTask])
      val startedRun = StartedJobRun(run, Promise[JobResult].future)
      specs += run.id -> startedRun
      Future.successful(startedRun)
    }
  }
}
