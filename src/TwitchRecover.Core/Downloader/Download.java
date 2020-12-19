/*
 * Copyright (c) 2020 Daylam Tayari <daylam@tayari.gg>
 *
 * This library is free software. You can redistribute it and/or modify it under the terms of the GNU General Public License version 3 as published by the Free Software Foundation.
 * This program is distributed in the that it will be use, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not see http://www.gnu.org/licenses/ or write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/*
 * @author Daylam Tayari https://github.com/daylamtayari
 * @version 2.0
 * Github project home page: https://github.com/TwitchRecover
 * Twitch Recover repository: https://github.com/TwitchRecover/TwitchRecover
 */

package TwitchRecover.Core.Downloader;

import TwitchRecover.Core.Enums.FileExtensions;
import TwitchRecover.Core.Enums.Timeout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * This class holds all of the downloading methods
 * and handles all of the downloads.
 */
public class Download {
    /**
     * This method downloads a file from a
     * given URL and downloads it at a given
     * file path.
     * @param url
     * @param fp
     * @throws IOException
     */
    public static void download(String url, String fp) throws IOException {
        URL dURL=new URL(url);
        File dFile=new File(fp);
        FileUtils.copyURLToFile(dURL, dFile, Timeout.CONNECT.time, Timeout.READ.time);
    }

    public static void m3u8Download(String url, String fp) throws IOException {
        FileHandler.createTempFolder();
        ArrayList<String> chunks=M3U8Handler.getChunks(url);
        NavigableMap<Integer, File> segmentMap=TSDownload(chunks);
        FileHandler.mergeFile(segmentMap, fp);
    }

    /**
     * This method creates a temporary download
     * from a URL.
     * @param url       URL of the file to be downloaded.
     * @return File     File object of the file that will be downloaded and is returned.
     * @throws IOException
     */
    protected static File tempDownload(String url) throws IOException{
        URL dURL=new URL(url);
        String prefix=FilenameUtils.getBaseName(dURL.getPath());
        if(prefix.length()<10){     //This has to be implemented since the prefix value of the createTempFile method
            prefix="00"+prefix;     //which we use to create a temp file, has to be a minimum of 3 characters long.
        }
        else if(prefix.length()<100){
            prefix="0"+prefix;
        }
        File downloadedFile=File.createTempFile(prefix+"-", "."+FilenameUtils.getExtension(dURL.getPath()), new File(FileHandler.TEMP_FOLDER_PATH+File.separator));    //Creates the temp file.
        downloadedFile.deleteOnExit();  //TODO: The deletion is currently not occuring, to fix.
        FileUtils.copyURLToFile(dURL, downloadedFile, Timeout.CONNECT.time, Timeout.READ.time);
        return downloadedFile;
    }

    /**
     * This method downloads all of the segments
     * of an M3U8 playlist and incorporates them all
     * in a navigable map.
     * @param links                         Arraylist holding all of the links to download.
     * @return NavigableMap<Integer, File>  Navigable map holdding the index and file objects of each TS segment.
     */
    private static NavigableMap<Integer, File> TSDownload(ArrayList<String> links){
        NavigableMap<Integer, File> segmentMap=new TreeMap<>();
        Queue<String> downloadQueue=new ConcurrentLinkedQueue<>();
        for(String link: links){    //Adds all the links to the ressource queue.
            downloadQueue.offer(link);
        }
        int index=0;
        ThreadPoolExecutor downloadTPE=(ThreadPoolExecutor) Executors.newFixedThreadPool(100);
        while(!downloadQueue.isEmpty()){
            index++;
            String item=downloadQueue.poll();
            final int finalIndex=index;
            downloadTPE.execute(new Runnable() {
                @Override
                public void run() {
                    final int threadIndex=finalIndex;
                    final String threadItem=item;
                    try{
                        File tempTS=tempDownload(threadItem);   //TODO: Last chunk is not being downloaded, to fix.
                        segmentMap.put(threadIndex, tempTS);
                    }
                    catch(Exception ignored){}
                }
            });
        }
        downloadTPE.shutdown();
        try{
            downloadTPE.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        }
        catch(Exception ignored){}
        return segmentMap;
    }
}