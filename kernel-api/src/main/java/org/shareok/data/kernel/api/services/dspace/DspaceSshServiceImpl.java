/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.dspace;

import org.shareok.data.config.DataHandler;
import org.shareok.data.dspacemanager.DspaceSshDataUtil;
import org.shareok.data.dspacemanager.DspaceSshHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 *
 * @author Tao Zhao
 */
@Service
public class DspaceSshServiceImpl implements DspaceSshService {
    private DspaceSshHandler handler;

    public DspaceSshHandler getHandler() {
        return handler;
    }

    @Override
    @Autowired
    @Qualifier("dspaceSshHandler")
    public void setHandler(DataHandler handler) {
        this.handler = (DspaceSshHandler)handler;
        if(null == this.handler.getSshExec()){
            this.handler.setSshExec(DspaceSshDataUtil.getSshExecForDspace());
        }
    }
    
    @Override
    public String sshImportData(){
        return handler.importDspace();
    }
    
    @Override
    public String uploadSafDspace(){
        return handler.uploadSafDspace();
    }

    @Override
    public String executeTask(String jobType) {
        switch (jobType) {
            case "ssh-import-dspace":
                return handler.importDspace();
            case "ssh-upload-dspace":
                return handler.uploadSafDspace();
            case "ssh-importloaded-dspace":
                return handler.importUploadedSafDspace();
            default:
                return null;
        }
    }
    
}
