/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shareok.data.kernel.api.services.dspace;

/**
 *
 * @author Tao Zhao
 */
public interface DspacePlosService {
    public String getPlosDsapceLoadingFilesByExcel(String filePath);
    public String getPlosMetadataFilesByExcel(String filePath);
}
