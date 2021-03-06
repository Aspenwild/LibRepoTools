/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.job;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.shareok.data.redis.job.RedisJob;

/**
 *
 * @author Tao Zhao
 */
public interface RedisJobService extends RepositoryJobService {
    public long startJob(long uid, int jobType, int repoType, int serverId);
    public long startJob(long uid, int jobType, int repoType, int serverId, Date startTime);
    public void updateJob(long jobId, String jobInfoType, String value);
    public void updateJobInfoByJobType(long jobId, String jobType, Map values);
//    public void endJob(long jobId);
//    public String getJobStatus(long jobId);
//    public String getJobLogging(long jobId);    
    public List<RedisJob> getJobListByUser(long uid);
    public List<RedisJob> getJobListByUserEmail(String email);
    public RedisJob findJobByJobId(long jobId);
    public RedisJob createJob(final long uid, final int jobType, final Map<String, String> values);
    public RedisJob saveJob(RedisJob job);
    public Map<String, String> getReportData(RedisJob job);
    public Map<String, String> getJobInfoByAttributes(long jobId, String[] jobAttributes);
    
}
