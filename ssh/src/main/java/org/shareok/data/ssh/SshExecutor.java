/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 * 
 * Some of the code is from : http://blog.csdn.net/Sky123HelloWorld/article/details/41793023
 */
package org.shareok.data.ssh;

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

/**
 *
 * @author Tao Zhao
 */
@Service
public class SshExecutor {

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(SshExecutor.class);

    protected String charset = "UTF-8";
    private SshConnector sshConnector;
    protected JSch jsch;
    protected Session session;
    protected Channel channel;
    protected ChannelSftp chSftp;
    protected String reporter;
    protected boolean connected;

    public String getCharset() {
        return charset;
    }

    public SshConnector getSshConnector() {
        return sshConnector;
    }

    public JSch getJsch() {
        return jsch;
    }

    public Session getSession() {
        return session;
    }

    public Channel getChannel() {
        return channel;
    }

    public ChannelSftp getChSftp() {
        return chSftp;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    @Autowired
    public void setSshConnector(SshConnector sshConnector) {
        this.sshConnector = sshConnector;
    }

    public void setJsch(JSch jsch) {
        this.jsch = jsch;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public void setChSftp(ChannelSftp chSftp) {
        this.chSftp = chSftp;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    
    /**
     * Connect to Server
     *
     * @throws com.jcraft.jsch.JSchException
     */
    public void getConnect() throws JSchException {
        
        if(isConnected()){
            return;
        }
        
        try{
        String userName = sshConnector.getUserName();
        String host = sshConnector.getHost();
        String password = sshConnector.getPassword();
        String rsaKey = sshConnector.getRsaKey();
        int port = sshConnector.getPort();
        int timeout = sshConnector.getTimeout();
        
        jsch = new JSch();
        session = jsch.getSession(userName, host, port);
        addReporter("New Session created.");
        //logger.debug("Session created.");
        if (password != null && !"".equals(password)) {
            session.setPassword(password);
        }
        else{
            jsch.addIdentity(rsaKey);
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(timeout);
        session.connect();
        setConnected(true);
        //System.out.println("Connected successfully to remote Server = \"" + host + "\",as user name = \"" + userName + "\", as port =  \"" + port + "\"");
        addReporter("Session connected.");
        addReporter("Connected successfully to remote Server = " + host + ",as user name = " + userName + ",as port =  " + port);
//        reporter.debug("Session connected.");
//        reporter.debug("Connected successfully to DSpace Server = " + host + ",as user name = " + userName
//                + ",as port =  " + port);
        }
        catch(Exception ex){
            addReporter("Cannot connect to the server!");
            logger.error("Cannot connect to the server!", ex);
        }
    }

    /**
     * Execute commands on remote server
     * 
     * @param commands : String[] commands to be executed. Multiple commands are separated by ';;'
     */
    public void execCmd(String[] commands) {

        BufferedReader reader = null;

        try {
            getConnect();
//            String[]commands = command.split(";;");
            for(String command : commands){
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);

                channel.connect();
                InputStream in = channel.getInputStream();
                reader = new BufferedReader(new InputStreamReader(in,
                        Charset.forName(charset)));
                String buf = null;
                while ((buf = reader.readLine()) != null) {
                    //logger.debug(buf);
                    //System.out.println(buf);
                    addReporter(buf);
                }
            }
        } catch (IOException | JSchException e) {
            addReporter("Cannot execute the commands.");
            logger.error("Cannot execute the commands.", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                addReporter("Cannot close the reader after executing the ssh command.");
                logger.error("Cannot close the reader after executing the ssh command.", e);
            }
            channel.disconnect();
            session.disconnect();
            setConnected(false);
        }
    }
    
    public void execCmd(String command) {
        String[] commands = {command};
        execCmd(commands);
    }

    /**
     * Upload files
     *
     * @param directory : set up in configuration file
     * @param uploadFile : file to be uploaded
     */
    public void upload(String directory, String uploadFile) {
        try {
            getConnect();
            addReporter("Opening Channel.");
            //logger.debug("Opening Channel.");
            channel = session.openChannel("sftp");
            channel.connect();
            chSftp = (ChannelSftp) channel;
            File file = new File(uploadFile);
            long fileSize = file.length();

           // Method 1
//             OutputStream out = chSftp.put(uploadFile, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); // use the OVERWRITE mode
//             byte[] buff = new byte[1024 * 256];
//             int read;
//             if (out != null) {
//              //   reporter.debug("Start to read input stream");
//                InputStream is = new FileInputStream(directory);
//                do {
//                    read = is.read(buff, 0, buff.length);
//                     if (read > 0) {
//                            out.write(buff, 0, read);
//                     }
//                     out.flush();
//                 } while (read >= 0);
//              //   reporter.debug("input stream read done.");
//             }
            FileProgressMonitor monitor = getFileProgressMonitor(fileSize);
            chSftp.put(uploadFile, directory, monitor, ChannelSftp.OVERWRITE); // Method 2
            addReporter(monitor.getLogger());

            // chSftp.put(new FileInputStream(src), dst, new FileProgressMonitor(fileSize), ChannelSftp.OVERWRITE); // Method 3

        } catch (Exception e) {
            addReporter("Cannot upload files to the repository server.");
            logger.error("Cannot upload files to the repository server.", e);
        }finally {
            chSftp.quit();

            if (channel != null) {
                channel.disconnect();
                addReporter("channel disconnect");
                //logger.debug("channel disconnect");
            }
            if (session != null) {
                session.disconnect();
                addReporter("channel disconnect");
                //logger.debug("channel disconnect");
            }
            setConnected(false);
            addReporter("File at  <"+ uploadFile + " has been successfully uploaded to remote server at : \"" + directory + "\".");
        }
    }
    
    public void addReporter(String info){
        reporter = reporter.concat(info + "\n\n");
    }
    
    private FileProgressMonitor getFileProgressMonitor(long fileSize){
        ApplicationContext context = new ClassPathXmlApplicationContext("sshContext.xml");
        FileProgressMonitor monitor = (FileProgressMonitor)context.getBean("fileProgressMonitor");
        monitor.setTransfered(fileSize);
        return monitor;
    }


//    /**
//     * 下载文件
//     *
//     * @param directory 
//     * @param downloadFile 
//     *
//     */
//    public void download(String directory, String downloadFile) {
//        try {
//            getConnect();//建立服务器连接
//            reporter.debug("Opening Channel.");
//            channel = session.openChannel("sftp"); // 打开SFTP通道
//            channel.connect(); // 建立SFTP通道的连接
//            chSftp = (ChannelSftp) channel;
//            SftpATTRS attr = chSftp.stat(downloadFile);
//            long fileSize = attr.getSize();
//            chSftp.get(downloadFile, directory, new FileProgressMonitor(fileSize)); // 
//
//
//            //OutputStream out = new FileOutputStream(directory);
//            //chSftp.get(downloadFile, out, new FileProgressMonitor(fileSize)); // 
//
//            /*
//
//            InputStream is = chSftp.get(downloadFile, new MyProgressMonitor());
//            byte[] buff = new byte[1024 * 2];
//            int read;
//            if (is != null) {
//                reporter.debug("Start to read input stream");
//                do {
//                    read = is.read(buff, 0, buff.length);
//                    if (read > 0) {
//                        out.write(buff, 0, read);
//                    }
//                    out.flush();
//                } while (read >= 0);
//                reporter.debug("input stream read done.");
//            }*/
//
//            reporter.debug("成功下载文件至"+directory);
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            chSftp.quit();
//            if (channel != null) {
//                channel.disconnect();
//                reporter.debug("channel disconnect");
//            }
//            if (session != null) {
//                session.disconnect();
//                reporter.debug("channel disconnect");
//            }
//        }
//    }
//
//    /**
//     * Delete files
//     * @param deleteFile 
//     */
//    public void delete(String deleteFile) {
//
//        try {
//            getConnect();
//            reporter.debug("Opening Channel.");
//            channel = session.openChannel("sftp"); 
//            channel.connect(); 
//            chSftp = (ChannelSftp) channel;
//            chSftp.rm(deleteFile);
//            reporter.debug("成功删除文件"+deleteFile);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//    }

}