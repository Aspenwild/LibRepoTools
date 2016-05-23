/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.job;

import org.shareok.data.config.DataHandler;
import org.shareok.data.redis.job.RedisJob;
import org.springframework.web.multipart.MultipartFile;

/**
 *
 * @author Tao Zhao
 */
public interface JobHandler {
    public RedisJob execute(long uid, int jobType, int repoType, DataHandler handler, MultipartFile localFile, String remoteFilePath);
    public RedisJob execute(long uid, String repoType, String jobType, DataHandler handler, MultipartFile localFile, String remoteFilePath);
}