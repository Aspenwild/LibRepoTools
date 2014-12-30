/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.shareok.data.msofficedata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.formula.udf.IndexedUDFFinder;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 *
 * @author Tao Zhao
 */
public class ExcelHandler implements FileHandler {
      
    private String fileName;
    private FileRouter router;
    private HashMap data;

    public FileRouter getRouter() {
        return router;
    }

    public HashMap getData() {
        return data;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setRouter(FileRouter router) {
        this.router = router;
    }

    public void setData(HashMap data) {
        this.data = data;
    }
    
    private Sheet getWorkbookSheet(String extension, FileInputStream file) throws IOException {
        Sheet sheet = null;
        if("xlsx".equals(extension)){
            XSSFWorkbook workbook = new XSSFWorkbook(file);
            sheet = workbook.getSheetAt(0);
        }
        if("xls".equals(extension)){
            HSSFWorkbook workbook = new HSSFWorkbook(file);
            sheet = workbook.getSheetAt(0);
        }
        return sheet;
    }
    
    private boolean isCellDateFormatted(Cell cell) throws Exception {
        try{
            return DateUtil.isCellDateFormatted(cell);
        }
        catch(Exception ex){
            ex.printStackTrace();
            throw new Exception ("The cell type data formatted cannot be decided!");
        }
    }
    
    /**
     *
     * @throws Exception
     */
    @Override
    public void readData() {
        
        String name = fileName;
        Sheet sheet = null;
        
        try {
            if(null == name || "".equals(name)) {
                throw new Exception ("File name is not specified!"); 
            }
            
            FileInputStream file = new FileInputStream(new File(name));

            String extension = FileUtil.getFileExtension(name);

            String[] excelTypes = router.loadOfficeFileType("excel");
            
            if(null == excelTypes || excelTypes.length == 0){
                throw new Exception("The file types are empty!");
            }

            HashMap<String,String> typeMap = new HashMap<>();
            for(String s : excelTypes){
                typeMap.put(s, s);
            }

            if(typeMap.containsKey(extension)){
                if(extension.equals("xlsx")){
    
                }
            }

            sheet = getWorkbookSheet(extension, file);
            Iterator<Row> rowIterator = sheet.iterator();
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy");
            int rowCount = 0;
            int colCount = 0;
            
            while(rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Iterator<Cell> cellIterator = row.cellIterator();
                while(cellIterator.hasNext()) {

                    Cell cell = cellIterator.next();
                    String key = Integer.toString(rowCount) + "-" + Integer.toString(colCount);
                    switch(cell.getCellType()) {
                        case Cell.CELL_TYPE_BOOLEAN:
                            data.put(key, Boolean.toString(cell.getBooleanCellValue()) + "---bool");
                            break;
                        case Cell.CELL_TYPE_NUMERIC:
                            if(isCellDateFormatted(cell)) {
                                data.put(key, df.format(cell.getDateCellValue()) + "---dat");
                            }
                            else{
                                data.put(key, Double.toString(cell.getNumericCellValue()) + "---num");
                            }
                            break;
                        case Cell.CELL_TYPE_STRING:
                            data.put(key, cell.getStringCellValue() + "---str");
                            break;
                        default:
                            data.put(key, cell.getRichStringCellValue() + "---def");
                            break;
                    }
                    
                    colCount++;
                }
                rowCount++;
                colCount = 0;
            }
            file.close();
        
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException ex) {
            Logger.getLogger(ExcelHandler.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(ExcelHandler.class.getName()).log(Level.SEVERE, null, ex);
        } 
    }
    
    @Override
    public void exportMapDataToXml(HashMap map, String filePath) {
        try{
            Iterator it = map.keySet().iterator();
            while(it.hasNext()){
                String key = (String)it.next();
                Object obj = (Object)map.get(key);
                if(obj instanceof ArrayList){
                    
                }
                else if(obj instanceof String){

                }
                else{
                    System.out.println("Undefined data type");
                }
            }
        }
        catch (Exception ex) {
            Logger.getLogger(ExcelHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
