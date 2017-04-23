package com.example.mywork;

import com.example.mywork.model.CLArgumentException;
import com.example.mywork.model.CLParam;
import com.example.mywork.model.CLParser;
import com.example.mywork.model.WorkerManager;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for jget.
 *
 */
public class Main {
    private static Map<String, List<String>> filesToDownload = new HashMap<String, List<String>>();
    private static Queue<String> queue = new ConcurrentLinkedQueue<String>();

    public static void main(String args[]) {
        CLParser config = new CLParser(
                new CLParam<Integer>(
                        "threads",
                        "number of working threads (1,2,3,4,...)",
                        "-n", 2,
                        value -> Integer.valueOf(value)
                ),
                new CLParam<Integer>(
                        "limit",
                        "overall speed limit for all working threads in bytes per second (may use suffixes according IEC60027-2 (http://physics.nist.gov/cuu/Units/binary.html))",
                        "-l", 10240,
                        value -> parseSize(value)
                ),
                new CLParam<File>(
                        "output",
                        "output folder",
                        "-o", new File("."),
                        value -> new File(value)
                ),
                new CLParam<File>(
                        "input",
                        "input file name",
                        "-f",
                        value -> new File(value)
                )
        );

        try {
            config.parse(args);
            processFile( (File) config.get("input") );
        } catch (CLArgumentException e) {
            System.out.println(e.getMessage());
            config.printUsage();
            return;
        }

        WorkerManager wm = new WorkerManager(config, queue, filesToDownload);
        long time = wm.run();
        System.out.printf(Locale.ENGLISH, "Transfered %1$d bytes in %2$s %3$d B/s\n", wm.getBytes(), getTimeString(time), 1000 * wm.getBytes() / time);
    }

    /**
     * Parses limit speed.
     * @param value limit speed in textual form
     * @return speed in B/s
     * @throws CLArgumentException
     */
    private static Integer parseSize(String value) throws CLArgumentException {
        Matcher m = Pattern.compile("([0-9]+)((Ki?B|Mi?B)*)").matcher(value);
        if (m.matches()) {
            String num = m.group(1);
            String prefix = m.group(2);
            return Integer.valueOf(num) * getPrefixVal(prefix);
        }
        throw new CLArgumentException("Invalid speed format.");
    }

    /**
     * Gets multiplier for given prefix.
     * @param prefix prefix according IEC60027-2
     * @return multiplier
     */
    private static Integer getPrefixVal(String prefix) {
        if ("KB".equals(prefix)) {
            return 1000;
        } else if ("KiB".equals(prefix)) {
            return 1024;
        } else if ("MB".equals(prefix)) {
            return 1000000;
        } else if ("MiB".equals(prefix)) {
            return 1024 * 1024;
        }
        return 1;
    }

    /**
     * Reads file and makes internal structure for further processing.
     * @param inputFile file with URLs
     * @throws CLArgumentException
     */
    private static void processFile(File inputFile) throws CLArgumentException {
        BufferedReader br = null;
        try {
            InputStreamReader isr = new InputStreamReader(new FileInputStream(inputFile));
            br = new BufferedReader(isr);
            String line = null;
            int counter = 0;
            Map<String, String> fileNames = new HashMap<String,String>();
            while( (line = br.readLine()) != null ){
                ++counter;
                final String[] parts = line.split(" ");
                if( parts.length == 2 ){
                    boolean canAdd = true;
                    if( fileNames.containsKey(parts[1]) ){
                        if( !fileNames.get(parts[1]).equals(parts[0]) ){
                            canAdd = false;
                            System.out.println("Duplicate output file name in line "+counter+". Line is skipped.");
                        }
                    }

                    if( canAdd ){
                        addFile( parts[0], parts[1] );
                        fileNames.put( parts[1], parts[0] );
                    }
                } else {
                    System.out.println("Missing output file name in line "+counter+". Line is skipped.");
                }
            }
        } catch (FileNotFoundException e) {
            throw new CLArgumentException("Input file is not found!");
        } catch (IOException e) {
            throw new CLArgumentException("Can't read input file!");
        }finally{
            if( br != null ){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Adds output filename to download list.
     * @param link URL
     * @param fn file name for local storing
     */
    private static void addFile(String link, String fn) {
        List<String> l = filesToDownload.get(link);
        if( l == null ){
            l = new ArrayList<String>();
            filesToDownload.put(link, l);
            queue.add(link);
        }
        l.add(fn);
    }

    /**
     * Makes textual representation of period.
     * @param ms time period in milliseconds
     * @return textual representation of period
     */
    private static String getTimeString(long ms) {
        long s = ms / 1000;
        if (s != 0) {
            ms %= 1000;
        }
        long m = s / 60;
        if (m != 0) {
            s %= 60;
        }
        long h = m / 60;
        if (h != 0) {
            m %= 60;
        }
        StringBuilder sb = new StringBuilder();
        if (h != 0) {
            sb.append(h).append("h ");
        }
        if (sb.length() > 0 || m != 0) {
            sb.append(m).append("m ");
        }
        sb.append(s);
        if (ms != 0) {
            sb.append(".");
            sb.append(ms);
        }
        sb.append("s");
        return sb.toString();
    }
}
