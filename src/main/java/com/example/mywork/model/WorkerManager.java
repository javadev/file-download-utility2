package com.example.mywork.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import com.example.mywork.model.CLParser;

/**
 * Manager class for worker threads.
 * @author mda
 *
 */
public class WorkerManager {
    private static final int COPY_BUFF_LEN = 102400;
    final private AtomicLong transferedBytes = new AtomicLong(0);
    final private Semaphore  byteCounter;
    final private CountDownLatch sync;
    final private Integer 	 bandwidthLimit;
    final private Integer 	 threadsNum;
    final private File		 outputFolder;
    final private Queue<String> queue;
    final private Map<String, List<String>> filesToDownload;
    final private FileTransferCallback fileCallback;

    /**
     * Constructor.
     * @param config command line parser
     * @param queue work queue
     * @param filesToDownload files for processing
     */
    public WorkerManager(CLParser config, Queue<String> queue, Map<String, List<String>> filesToDownload, FileTransferCallback fileCallback) {
        this.bandwidthLimit = (Integer) config.get("limit");
        this.threadsNum 	= (int) 	config.get("threads");
        this.outputFolder 	= (File) 	config.get("output");
        this.queue 			= queue;
        this.filesToDownload= filesToDownload;
        this.fileCallback = fileCallback;

        sync = new CountDownLatch( threadsNum );
        byteCounter = new Semaphore( bandwidthLimit );
    }

    /**
     * Runs processing.
     * @return processing time in milliseconds
     */
    public long run() {

        // Daemon thread for time count
        Thread timerThread = new Thread(new Runnable() {
            /**
             * Resets semaphore every second.
             */
            @Override
            public void run() {
                do {
                    int avail = byteCounter.availablePermits();
                    byteCounter.release( bandwidthLimit - avail );
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } while( true );
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();

        long begins = System.currentTimeMillis();

        // Runs working threads
        for( int i=0;  i<threadsNum; ++i ){
            new Thread(new Runnable( ) {
                /**
                 * Downloads files into output directory.
                 */
                @Override
                public void run() {
                    String urlStr = null;
                    while( (urlStr = queue.poll()) != null ){
                        List<String> fileNames = filesToDownload.get(urlStr);
                        String fileName = fileNames.remove(0);

                        File outputFile = new File(outputFolder, fileName);
                        OutputStream os = null;
                        try {
                            os = new FileOutputStream(outputFile);
                        } catch (FileNotFoundException e1) {
                            e1.printStackTrace();
                            break;
                        }

                        fileCallback.print(fileName);
                        HttpURLConnection connection;
                        try {
                            connection = (HttpURLConnection)new URL(urlStr).openConnection();
                            connection.connect();

                            InputStream is = connection.getInputStream();
                            int ch = 0;
                            do {
                                try {
                                    byteCounter.acquire();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                ch = is.read();
                                if( ch == -1 ){
                                    break;
                                }
                                os.write(ch);
                                transferedBytes.incrementAndGet();
                            } while( true );
                            os.flush();
                            is.close();
                            connection.disconnect();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        fileNames.forEach( to -> copyFile(fileName, to) );
                    }
                    sync.countDown();
                }

                /**
                 * Copies file into another file in output directory.
                 * @param from source file
                 * @param to destination file
                 */
                private void copyFile(String from, String to ) {
                    if( !from.equals(to) ){
                        byte[] buff = new byte[COPY_BUFF_LEN];
                        InputStream  is = null;
                        OutputStream os = null;
                        try {
                            is = new FileInputStream(new File(outputFolder, from));
                            os = new FileOutputStream(new File(outputFolder, to));

                            int c;
                            while( (c = is.read(buff, 0, COPY_BUFF_LEN)) != -1){
                                os.write(buff, 0, c);
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            try {
                                os.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }).start();
        }
        try {
            sync.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return System.currentTimeMillis() - begins;
    }

    /**
     * Gets transfered bytes.
     * @return transfered bytes
     */
    public long getBytes() {
        return transferedBytes.get();
    }
}
