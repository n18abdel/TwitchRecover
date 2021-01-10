/*
 * Copyright (c) 2021 Daylam Tayari <daylam@tayari.gg>
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

package TwitchRecover.Core.API;

import TwitchRecover.Core.Compute;
import TwitchRecover.Core.Enums.Quality;
import TwitchRecover.Core.Enums.Timeout;
import TwitchRecover.Core.Feeds;
import TwitchRecover.Core.FileIO;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class handles all of the
 * API calls and retrievals.
 */
public class API {
    /**
     * Method which parses the feeds from a given
     * arraylist which includes all of the lines that
     * were read from the web query and creates and
     * returns a Feeds object which contains all of
     * the feeds and their corresponding qualities.
     * @param response  String arraylist which contains all of the lines read in the web query.
     * @return  Feeds   A Feeds object which contains all of the feed URLs and their corresponding qualities.
     */
    static Feeds parseFeeds(ArrayList<String> response){
        Feeds feeds=new Feeds();
        for(int i=0; i<response.size(); i++){
            if(!response.get(i).startsWith("#")){
                if(response.get(i-2).contains("chunked")){      //For when the line is the source feed.
                    feeds.addEntryPos(response.get(i), Quality.Source, 0);
                    String pattern="#EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID=\"chunked\",NAME=\"([0-9p]*) \\(source\\)\",AUTOSELECT=[\"YES\"||\"NO\"]*,DEFAULT=[\"YES\"||\"NO\"]*";
                    Pattern p=Pattern.compile(pattern);
                    Matcher m=p.matcher(response.get(i-2));
                    if(m.find()){
                        feeds.addEntryPos(response.get(i), Quality.getQualityV(m.group(1)), 1);
                    }
                }
                else{
                    feeds.addEntry(response.get(i), Quality.getQualityV(Compute.singleRegex("#EXT-X-MEDIA:TYPE=VIDEO,GROUP-ID=\"([\\d]*p[36]0)\",NAME=\"([0-9p]*)\",AUTOSELECT=[\"YES\"||\"NO\"]*,DEFAULT=[\"YES\"||\"NO\"]*", response.get(i-2))));
                }
            }
        }
        return feeds;
    }

    /**
     * This method performs a get request of a
     * specific given URL (which has to be a
     * Twitch API URL that is setup in at least
     * V5 on the backend.
     * @param url                   String value representing the URL to perform the get request on.
     * @return ArrayList<String>    String arraylist holding the entire response from the get request, each line representing an entry.
     */
    static ArrayList<String> getReq(String url){
        ArrayList<String> responseContents=new ArrayList<String>();
        try{
            CloseableHttpClient httpClient=HttpClients.createDefault();
            HttpGet httpget=new HttpGet(url);
            httpget.addHeader("User-Agent", "Mozilla/5.0");
            httpget.addHeader("Accept", "application/vnd.twitchtv.v5+json");
            httpget.addHeader("Client-ID", "kimne78kx3ncx6brgo4mv6wki5h1ko");   //Web client client ID (check out my explanation of Twitch's video system for more details).
            CloseableHttpResponse httpResponse=httpClient.execute(httpget);
            if(httpResponse.getStatusLine().getStatusCode()==200){
                BufferedReader br=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
                String line;
                while ((line = br.readLine()) != null) {
                    responseContents.add(line);
                }
                br.close();
            }
        }
        catch(Exception ignored){}
        return responseContents;
    }

    /**
     * This method gets a playlist
     * file and returns the contents
     * of the playlist file.
     * @param url       String value which represents the URL of the playlist file.
     * @return Feeds    Feeds object containing all of the feeds from an M3U8 playlist.
     */
    static Feeds getPlaylist(String url){
        File downloadedFile= null;    //Creates the temp file.
        try {
            URL dURL=new URL(url);
            downloadedFile = File.createTempFile("TwitchRecover-Playlist-", ".m3u8");
            downloadedFile.deleteOnExit();
            FileUtils.copyURLToFile(dURL, downloadedFile, Timeout.CONNECT.time, Timeout.READ.time);
        }
        catch(IOException ignored){}
        return parseFeeds(FileIO.read(downloadedFile.getAbsolutePath()));
    }

    /**
     * This method parses and returns
     * the token and signature values
     * from a given API JSON response.
     * @param response      String arraylist containing the JSON response from the API call.
     * @return String[]     String array containing the token and signature values.
     * String[2]: 0: Token; 1: Signature.
     */
    protected static String[] parseToken(ArrayList<String> response){
        String[] results=new String[2];
        //Parse JSON:
        JSONParser parse=new JSONParser();
        JSONObject jObj=null;
        try{
            jObj=(JSONObject) parse.parse(response.get(0));
        }
        catch(ParseException ignored){}
        String token=jObj.get("token").toString();
        results[1]=jObj.get("sig").toString();
        //Remove back slashes from token:
        results[0]=token.replace("\\", "");
        return results;
    }
}