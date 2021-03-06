/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package oulib.aws.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;
import com.sun.media.jai.codec.TIFFDecodeParam;
import com.sun.media.jai.codec.TIFFEncodeParam;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfo;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoAscii;
import org.apache.commons.imaging.formats.tiff.taginfos.TagInfoByte;
import org.apache.commons.imaging.formats.tiff.write.TiffImageWriterLossy;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputDirectory;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputField;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import oulib.aws.exceptions.InvalidS3CredentialsException;

import oulib.aws.exceptions.NoMatchingTagInfoException;

/**
 *
 * @author Tao Zhao
 */
public class S3Util {
    
    public static final double COMPRESSION_RATE_75_PERCENT_OF_ORIGINAL = 0.87;
    public static final double COMPRESSION_RATE_50_PERCENT_OF_ORIGINAL = 0.71;
    public static final double COMPRESSION_RATE_25_PERCENT_OF_ORIGINAL = 0.50;
    public static final long COMPRESSOIN_TARGET_SIZE_EXTRA_SMALL = 5000000;
    public static final long COMPRESSOIN_TARGET_SIZE__SMALL = 20000000;
    public static final long COMPRESSOIN_TARGET_SIZE_MEDIUM = 60000000;
    
    public static final String S3_SMALL_DERIVATIVE_OUTPUT = "/var/local/librepotools/librepotools-data/s3_small_derivatives";
    public static final String S3_NO_NUMBER_ENDDING_TIFF_PATTERN = "^(.*)(\\d+)(.tiff|.tif)$";
    
    /**
     * Check if an Amazon S3 folder exists
     * 
     * @param folderName : folder name to find
     * @param keyMap : the collection of path to check
     * @return : boolean
     */
    public static boolean folderExitsts(String folderName, Map<String, String> keyMap){
        for (String key : keyMap.keySet()) {
            if(null != key && key.contains("/"+folderName+"/")){
                return true;
            }
        } 
        return false;
    }
    
    /**
     * 
     * @param bucketName : bucket name
     * @param folderName : a unique folder name or partial path in the bucket
     * @param client : s3 client
     * @return : a map of keys with keyset of object keys
     */
    public static Map<String, String> getBucketObjectKeyMap(String bucketName, String folderName, AmazonS3 client){
        final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
        ListObjectsV2Result result;
        Map<String, String> keyMap = new HashMap<>();
            
        do {               
            result = client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {                
                String key = objectSummary.getKey();
                if(key.contains(folderName)){
                    keyMap.put(key, key);
                }
            }            
            req.setContinuationToken(result.getNextContinuationToken());
        } while(result.isTruncated() == true ); 
        
        return keyMap;
    }
    
    /**
     * Creates an AWS S3 folder
     * 
     * @param bucketName
     * @param folderName
     * @param client 
     */
    public static void createFolder(String bucketName, String folderName, AmazonS3 client){
    	
    	try{
    	
	    	ObjectMetadata metadata = new ObjectMetadata();
	    	metadata.setContentLength(0);
	
	    	InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
	
	    	PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName+"/", emptyContent, metadata);
	
	    	client.putObject(putObjectRequest);
	    	
	    	System.out.println("Sucessfully created the folder of " + folderName + " in the bucket of " + bucketName);
    	}
    	catch(Exception ex){
    		System.out.println("Failed to create the folder of " + folderName + " in the bucket of " + bucketName);
//    		Logger.getLogger(AwsS3Processor.class.getName÷÷()).log(Level.SEVERE, null, ex);
    	}
    	
    }
    
    /**
     * 
     * @param s3client : S3 cient
     * @param name : bucke name
     * @return : S3 bucket
     */
    public static Bucket getS3BucketByName(AmazonS3 s3client, String name){
        Bucket bucket = null;
        for (Iterator<Bucket> it = s3client.listBuckets().iterator(); it.hasNext();) {
            Bucket bu = it.next();
            if(name.equals(bu.getName())){
                return bu;
            }
        }
        return bucket;
    }
    
    public static PutObjectResult generateSmallTiff(AmazonS3 s3client, String sourceBucketName, String sourceKey, String targetBucketName, String targetKey, double compressionRate){
        
        S3Object s3Object= s3client.getObject(new GetObjectRequest(sourceBucketName, sourceKey));
        return generateSmallTiff(s3client, s3Object, targetBucketName, targetKey, compressionRate);
    }
    
    /**
     * Generate a small tiff file from large Tiff S3 bucket object <br>
     * Note: the small tiff file will have the same key path as the original one
     * 
     * @param s3client : S3 client
     * @param s3 : S3 object that con
     * @param targetBucketName : the bucket that stores the small tiff file
     * @param targetKey : key of the object in the target bucket
     * @param compressionRate : compression rate
     * @return : PutObjectResult
     */
    public static PutObjectResult generateSmallTiff(AmazonS3 s3client, S3Object s3, String targetBucketName, String targetKey, double compressionRate){
        
        PutObjectResult result = null;
        ByteArrayOutputStream bos = null;
        ByteArrayOutputStream os = null;
        ByteArrayInputStream is = null;
        S3ObjectInputStream s = null;
        ByteArrayInputStream byteInputStream = null;
        
        try{
            System.setProperty("com.sun.media.jai.disableMediaLib", "true");

            bos = new ByteArrayOutputStream();
            s = s3.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(s);
            byteInputStream = new ByteArrayInputStream(bytes);
            
            TIFFDecodeParam param = new TIFFDecodeParam();
            ImageDecoder dec = ImageCodec.createImageDecoder("TIFF", byteInputStream, param);
            
            RenderedImage image = dec.decodeAsRenderedImage();

            RenderingHints qualityHints = new RenderingHints(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            RenderedOp resizedImage = JAI.create("SubsampleAverage", image, compressionRate, compressionRate, qualityHints);
            
            TIFFEncodeParam params = new com.sun.media.jai.codec.TIFFEncodeParam();

            resizedImage = JAI.create("encode", resizedImage, bos, "TIFF", params);
            
            BufferedImage imagenew = resizedImage.getSourceImage(0).getAsBufferedImage();
            
            os = new ByteArrayOutputStream();
            ImageIO.write(imagenew, "tif", os);
            is = new ByteArrayInputStream(os.toByteArray());
            
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(os.toByteArray().length);
            metadata.setContentType("image/tiff");
            metadata.setLastModified(new Date());
            
            os.close();
            
            imagenew.flush();
            
            result = s3client.putObject(new PutObjectRequest(targetBucketName, targetKey, is, metadata));
        } catch (IOException | AmazonClientException ex) {
            Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally{
        	try{
                    if(bos != null){
                        bos.close();
                    }
                    if(os != null){
                        os.close();
                    }
                    if(is != null){
                        is.close();
                    }
                    if(s != null){
                        s.close();
                    }
                    if(byteInputStream != null){
                        byteInputStream.close();
                    }
        	}
        	catch(IOException ex){
        		Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
        	}
        }
        
        return result;
    }
    
    /**
     * Generate a small tiff file with defined size from large Tiff S3 bucket object <br>
     * Note: the small tiff file will have the same key path as the original one
     * 
     * @param s3client : S3 client
     * @param s3 : S3 object that con
     * @param targetBucketName : the bucket that stores the small tiff file
     * @return : PutObjectResult
     */
    public static PutObjectResult generateSmallTiffWithTargetSize(AmazonS3 s3client, S3Object s3, String targetBucketName, long compressionSize){
    	try{
	    	long objSize = s3.getObjectMetadata().getContentLength();
	    	double compressionRate = Math.sqrt(Double.valueOf(compressionSize)/Double.valueOf(objSize));
//	    	System.out.println("The compressoin rate is "+String.valueOf(compressionRate)+" with original size = "+String.valueOf(objSize)+" and target size = "+String.valueOf(compressionSize));
	    	return generateSmallTiff(s3client, s3, targetBucketName, s3.getKey(), compressionRate);
    	}
    	catch(Exception ex){
    		ex.printStackTrace();
    	}
    	return null;
    }
    
    /**
     * Pull out Tiff metadata from input S3 object and inject into the 
     * content of target S3 Object;<br>
     * Generate the new output S3 object that has the metadata from input object.
     * 
     * @param s3client : S3 client
     * @param sourceBucketName : Input bucket name
     * @param targetBucketName : Output bucket name
     * @param sourceKey : Input object key
     * @param targetKey : Output object key
     * 
     * @return PutObjectResult
     */
    public static PutObjectResult copyS3ObjectTiffMetadata(AmazonS3 s3client, String sourceBucketName, String targetBucketName, String sourceKey, String targetKey){
        S3Object obj1 = s3client.getObject(new GetObjectRequest(sourceBucketName, sourceKey));
        S3Object obj2 = s3client.getObject(new GetObjectRequest(targetBucketName, targetKey));
        return copyS3ObjectTiffMetadata(s3client, obj1, obj2);
    }
    
    /**
     * Pull out Tiff metadata from input S3 object and inject into the 
     * content of target S3 Object;<br>
     * Generate the new output S3 object that has the metadata from input object.
     * 
     * @param s3client : S3 client
     * @param obj1 : input object that provides metadata
     * @param obj2 : target object that receives metadata
     * 
     * @return PutObjectResult
     */
    public static PutObjectResult copyS3ObjectTiffMetadata(AmazonS3 s3client, S3Object obj1, S3Object obj2){
    	
    	PutObjectResult result = null;
    	
        BufferedInputStream bufferedInputStrean = null;
    	ByteArrayOutputStream byteArrayOutputStream = null;
        ByteArrayInputStream byteArrayInputStream = null;
    	ByteArrayInputStream bis = null;
    	S3ObjectInputStream content1 = null;
    	S3ObjectInputStream content2 = null;
        String targetBucketName = obj2.getBucketName();
        String outputKey = obj2.getKey();
    	
    	ImageMetadata metadata1, metadata2;
    	TiffImageMetadata tiffMetadata1, tiffMetadata2;
    	TiffOutputSet output1, output2;
    	
        try {
                content1 = obj1.getObjectContent(); 
                content2 = obj2.getObjectContent(); 
                
                byte[] bytes1 = IOUtils.toByteArray(content1);
                byte[] bytes2 = IOUtils.toByteArray(content2);

                metadata1 = Imaging.getMetadata(bytes1);
                metadata2 = Imaging.getMetadata(bytes2);

                tiffMetadata1 = (TiffImageMetadata)metadata1;
                tiffMetadata2 = (TiffImageMetadata)metadata2;

                output1 = tiffMetadata1.getOutputSet();
                output2 = tiffMetadata2.getOutputSet();

                TiffOutputDirectory rootDir = output2.getOrCreateRootDirectory();
                TiffOutputDirectory exifDir = output2.getOrCreateExifDirectory();
                TiffOutputDirectory gpsDir = output2.getOrCreateGPSDirectory();
                
                if(null != output1.getRootDirectory()){
                    List<TiffOutputField> fs = output1.getRootDirectory().getFields();
                    for(TiffOutputField f1 : fs){
                        if(null == rootDir.findField(f1.tag)
                                // CANNOT create the output image with this tag included!
                                && !"PlanarConfiguration".equals(f1.tagInfo.name)){
                            rootDir.add(f1);
                        }
                    }
                }

                if(null != output1.getExifDirectory()){
                    for(TiffOutputField f2 : output1.getExifDirectory().getFields()){
                        exifDir.removeField(f2.tagInfo);
                        exifDir.add(f2);
                    }
                }

                if(null != output1.getGPSDirectory()){
                    for(TiffOutputField f3 : output1.getGPSDirectory().getFields()){
                        gpsDir.removeField(f3.tagInfo);
                        gpsDir.add(f3);
                    }
                }
                
                byteArrayOutputStream = new ByteArrayOutputStream();
                TiffImageWriterLossy writerLossy = new TiffImageWriterLossy(output2.byteOrder);
                writerLossy.write(byteArrayOutputStream, output2);
                
                byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(byteArrayOutputStream.toByteArray().length);
                metadata.setContentType("image/tiff");
                metadata.setLastModified(new Date());

                result = s3client.putObject(new PutObjectRequest(targetBucketName, outputKey, byteArrayInputStream, metadata));
			
            } catch (ImageReadException | IOException | ImageWriteException ex) {
                    Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
            } finally{
                    try{
                            if(null != content1){
                                    content1.close();
                            }
                            if(null != content2){
                                    content2.close();
                            }
                            if(null != bufferedInputStrean){
                                    bufferedInputStrean.close();
                            }
                            if(null != byteArrayInputStream){
                                byteArrayInputStream.close();
                            }
                            if(null != byteArrayOutputStream){
                                byteArrayOutputStream.close();
                            }
                            if(null != bis){
                                    bis.close();
                            }
                    } catch(IOException ex){
                            Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
                    }			
            }
            return result;
    }
    
    /**
     *  Get exif technical metadata from S3 object
     * 
     * @param s3client
     * @param s3
     * @return : TiffImageMetadata
     */
    public static TiffImageMetadata retrieveExifMetadata(AmazonS3 s3client, S3Object s3){
        TiffImageMetadata tiffMetadata = null;
        try {
            S3ObjectInputStream is = s3.getObjectContent();
            final ImageMetadata metadata = Imaging.getMetadata(is, s3.getKey());
            tiffMetadata = (TiffImageMetadata)metadata;
        } catch (ImageReadException | IOException ex) {
            Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        return tiffMetadata;
    }
    
    /**
     * Add data into tiff exif metadata
     * 
     * @param tiffMetadata : TiffImageMetadata
     * @param data : map of data
     */
    public static void addTiffOutputFieldIntoTiffMetadata(TiffImageMetadata tiffMetadata, Map<TagInfo, Object> data){
        try {
            TiffOutputSet output = tiffMetadata.getOutputSet();
            TiffOutputDirectory exifDirectory = output.getOrCreateExifDirectory();
            for(TagInfo tagInfo : data.keySet()){
                addTiffOutputFieldIntoTiffOutputDirectory(exifDirectory, tagInfo, data.get(tagInfo));
            }
        } catch (ImageWriteException | NoMatchingTagInfoException ex) {
            Logger.getLogger(S3Util.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * 
     * @param exifDirectory : TiffOutputDirectory
     * @param tagInfo : TagInfo defined the field type
     * @param value : value of the new field
     * @throws ImageWriteException
     * @throws NoMatchingTagInfoException 
     */
    public static void addTiffOutputFieldIntoTiffOutputDirectory(TiffOutputDirectory exifDirectory, TagInfo tagInfo, Object value) 
            throws ImageWriteException, NoMatchingTagInfoException{
        
        exifDirectory.removeField(tagInfo);
        
        if(tagInfo instanceof TagInfoAscii){
            exifDirectory.add((TagInfoAscii)tagInfo, String.valueOf(value));
        }
        else if(tagInfo instanceof TagInfoByte){
            final TagInfoByte byteInfo = (TagInfoByte)tagInfo;
//            exifDirectory.add(byteInfo, String.valueOf(value).getBytes());
        }
        /**
         * Implement more taginfo types here....
         */
        else{
            throw new NoMatchingTagInfoException("Cannot find the matching Exif Tag type information!");
        }
    }
    
    public static void generateTifDerivativesByS3Bucket(AmazonS3 s3client, S3BookInfo bookInfo){
    	
    	String sourceBucketName = bookInfo.getBucketSourceName();
        String targetBucketName = bookInfo.getBucketTargetName();
        String bookName = bookInfo.getBookName();
        
        try{
        
            // Every book has a folder in the target bucket:
            Map targetBucketKeyMap = S3Util.getBucketObjectKeyMap(targetBucketName, bookName, s3client);
            if(!S3Util.folderExitsts(bookName, targetBucketKeyMap)){
                S3Util.createFolder(targetBucketName, bookName, s3client);
            }

            final ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(sourceBucketName).withPrefix(bookName + "/data/");
            ListObjectsV2Result result;

            do {               
                result = s3client.listObjectsV2(req);

                for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                    String key = objectSummary.getKey();
                    if(key.contains(".tif") && (key.contains("047") || key.contains("049") || key.contains("054")) && !targetBucketKeyMap.containsKey(key+".tif")){
                        S3Object object = s3client.getObject(new GetObjectRequest(sourceBucketName, key));
                        System.out.println("Start to generate smaller tif image for the object "+key+"\n");
                        S3Util.generateSmallTiffWithTargetSize(s3client, object, targetBucketName, bookInfo.getCompressionSize());
     //                   S3Util.copyS3ObjectTiffMetadata(s3client, object, s3client.getObject(new GetObjectRequest(targetBucketName, key)), targetBucketName, key+".tif");
                        System.out.println("Finished to generate smaller tif image for the object "+key+"\n");
     //                   break;
                    }
                }
                System.out.println("Next Continuation Token : " + result.getNextContinuationToken()+"\n");
                req.setContinuationToken(result.getNextContinuationToken());
            } while(result.isTruncated() == true ); 
	        
        } catch (AmazonServiceException ase) {
           System.out.println("Caught an AmazonServiceException, which means your request made it to Amazon S3, but was rejected with an error response for some reason.\n");
           System.out.println("Error Message:    " + ase.getMessage()+"\n");
           System.out.println("HTTP Status Code: " + ase.getStatusCode()+"\n");
           System.out.println("AWS Error Code:   " + ase.getErrorCode()+"\n");
           System.out.println("Error Type:       " + ase.getErrorType()+"\n");
           System.out.println("Request ID:       " + ase.getRequestId()+"\n");
       } catch (AmazonClientException ace) {
           System.out.println("Caught an AmazonClientException, which means the client encountered an internal error while trying to communicate with S3, \nsuch as not being able to access the network.\n");
           System.out.println("Error Message: " + ace.getMessage()+"\n");
       }
    }
    
    /**
     * 
     * @param s3client : s3 client
     * @param sourceBucket : the bucket as the base to be compared
     * @param srcBucketFolder : a unique folder name or a unique partial path in the bucket
     * @param targetBucket : the bucket to be checked if containing objects in the source bucket
     * @param tgtBucketFolder : a unique folder name or a unique partial path in the bucket
     * @return : list of the objects that are in the source bucket but not the target bucket
     */
    public static List<String> getS3BucketFolderObjDiff(AmazonS3 s3client, String sourceBucket, String srcBucketFolder, String targetBucket, String tgtBucketFolder){
        List<String> diffObjList = new ArrayList<>();
        try{
            Map<String, String> srcMap = getBucketObjectKeyMap(sourceBucket, srcBucketFolder, s3client);
            Map<String, String> tgtMap = getBucketObjectKeyMap(targetBucket, tgtBucketFolder, s3client);
            for(String srcKey : srcMap.keySet()){
                if(srcKey.endsWith(".tif") && null == tgtMap.get(srcKey)){
                    diffObjList.add(srcKey);
                }
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        return diffObjList;
    }
    
    /**
     * Use the default approach to get a AWS S3 client with the default region of east.
     * 
     * @return AmazonS3 : s3 client
     */
    public static AmazonS3 getS3AwsClient(){
        
        AWSCredentials credentials = null;
        try {
            ProfileCredentialsProvider provider = new ProfileCredentialsProvider("default");
            credentials = provider.getCredentials();   
            if(null == credentials){
                throw new InvalidS3CredentialsException("Invalid credentials with default approach!");
            }
        } catch (InvalidS3CredentialsException | AmazonClientException e) {
            throw new AmazonClientException("Cannot load the credentials from the credential profiles file. " +
                    "Please make sure that your credentials file is at the correct " +
                    "location (/Users/zhao0677/.aws/credentials), and is in valid format.", e);
        }
        
        AmazonS3 s3client = new AmazonS3Client(credentials);
        Region usEast = Region.getRegion(Regions.US_EAST_1);
        s3client.setRegion(usEast);
        return s3client;
    }
    
    public static AmazonS3 getS3Client(){
        
        AWSCredentials credentials = null;
            try {
                credentials = new ProfileCredentialsProvider("default").getCredentials();
            } catch (Exception e) {
                throw new AmazonClientException(
                        "Cannot load the credentials from the credential profiles file. " +
                                "Please make sure that your credentials file is at the correct " +
                                "location (/Users/zhao0677/.aws/credentials), and is in valid format.",
                        e);
            }
            
            AmazonS3 s3client = new AmazonS3Client(credentials);
            Region usEast = Region.getRegion(Regions.US_EAST_1);
            s3client.setRegion(usEast);
            return s3client;
    }
}
