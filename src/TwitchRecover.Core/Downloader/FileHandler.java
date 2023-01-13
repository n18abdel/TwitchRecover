/*
 * Copyright (c) 2020, 2021 Daylam Tayari <daylam@tayari.gg>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not see http://www.gnu.org/licenses/ or write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  @author Daylam Tayari daylam@tayari.gg https://github.com/daylamtayari
 *  @version 2.0aH     2.0a Hotfix
 *  Github project home page: https://github.com/TwitchRecover
 *  Twitch Recover repository: https://github.com/TwitchRecover/TwitchRecover
 */

package TwitchRecover.Core.Downloader;

import TwitchRecover.Core.Enums.FileExtension;
import lombok.Cleanup;
import net.bramp.ffmpeg.*;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.IOUtils;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.NavigableMap;

import static java.util.stream.Collectors.joining;

/**
 * This class handles all of the file handling
 * for the Download package.
 */
class FileHandler {
    protected static Path TEMP_FOLDER_PATH;    //Variable which holds the folder path of the temp folder.

    /**
     * This method creates a temp folder where all of the temporary TS
     * files (M3U8 parts) will be saved.
     * @throws IOException
     */
    protected static void createTempFolder() throws IOException {
        TEMP_FOLDER_PATH= Files.createTempDirectory("TwitchRecover-").toAbsolutePath();
        File tempDir=new File(String.valueOf(TEMP_FOLDER_PATH));
        tempDir.deleteOnExit();
    }

    /**
     * This method merges all of the
     * segmented files of the M3U8 playlist
     * into a single file.
     * @param segmentMap    Navigable map holding the index and file objects of all the segment files.
     * @param fp            Final file path of the file.
     */
    protected static String mergeFile(NavigableMap<Integer, File> segmentMap, String fp){
        try {
            File output = new File(fp);
            // Creation of string containing all input file ordered
            String filesStrings = new ArrayList<File>(segmentMap.values())
                    .stream()
                    .map(f -> f.getName())
                    .map(p -> "file " + p + "\n" +
                            "stream\n" +
                            "exact_stream_id 0x102\n" +
                            "stream\n" +
                            "exact_stream_id 0x101\n" +
                            "stream\n" +
                            "exact_stream_id 0x100")
                    .collect(joining(System.getProperty("line.separator")));

            // Saving this in a temp file
            Path listOfFiles = Files.createTempFile(FileHandler.TEMP_FOLDER_PATH, "ffmpeg-list-", ".txt");
            Files.write(listOfFiles, filesStrings.getBytes());

            // Using this txt file as input
            FFmpegBuilder builder = new FFmpegBuilder()
                    .addExtraArgs("-max_streams","10000","-stats")
                    .setInput(listOfFiles.toAbsolutePath().toString())
                    .setFormat("concat")
                    .setVerbosity(FFmpegBuilder.Verbosity.WARNING)
                    .addOutput(output.getAbsolutePath())
                    .setAudioCodec("copy")
                    .setVideoCodec("copy")
                    .done();
            FFmpegExecutor executor = new FFmpegExecutor();
            executor.createJob(builder).run();
            return output.getAbsolutePath();
        } catch (IOException e) {
            System.out.println(e);
        }
        return null;
    }

    /**
     * Method which merges two files together.
     * @param input     File to be merged.
     * @param output    File to be merged into.
     * @throws IOException
     */
    private static void fileMerger(File input, File output) throws IOException {
        @Cleanup OutputStream os= new BufferedOutputStream(new FileOutputStream(output, true));
        @Cleanup InputStream is=new BufferedInputStream(new FileInputStream(input));
        IOUtils.copy(is, os);
        is.close();
        os.close();
    }
}