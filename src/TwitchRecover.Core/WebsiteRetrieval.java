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

package TwitchRecover.Core;

import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * This class contains the core method for website data
 * recovery and all of its necessary methods to retrieve
 * necessary information from the Twitch analytics websites
 * Twitch recover supports.
 */
public class WebsiteRetrieval{
    //Core methods:
//    public String[] getData(String url){
//
//    }

    /**
     * This method checks if a URL is a stream URL
     * from one of the supported analytics websites.
     * @param url       URL to be checked.
     * @return int      Integer that is either -1 if the URL is invalid or
     * a value that represents which analytics service the stream link is from.
     */
    private int checkURL(String url){
        if(url.contains("twitchtracker.com/") && url.contains("/streams/")){
            return 1;   //Twitch Tracker URL.
        }
        else if(url.contains("streamscharts.com/twitch/channels/") && url.contains("/streams/")){
            return 2;   //Streams Charts URL.
        }
        else if(isSG(url)){
            return 3;   //Sully Gnome URL.
        }
        return -1;
    }
    private String getJSON(String url) throws IOException {
        String json="";
        URL jsonFetch=new URL(url);
        HttpURLConnection httpcon=(HttpURLConnection) jsonFetch.openConnection();
        httpcon.setRequestMethod("GET");
        httpcon.setRequestProperty("User-Agent", "Mozilla/5.0");
        String readLine=null;
        if(httpcon.getResponseCode()==HttpURLConnection.HTTP_OK){
            BufferedReader br=new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            StringBuffer response=new StringBuffer();
            while((readLine=br.readLine())!=null){
                response.append(readLine);
            }
            br.close();
            json=response.toString();
        }
        return json;
    }

    /**
     * This method checks if the inputted URL is a
     * Sully Gnome stream URL.
     * @param url       Inputted URL to be checked.
     * @return boolean  Returns true if the URL is a Sully Gnome stream URL and false otherwise.
     */
    public boolean isSG(String url){
        return url.contains("sullygnome.com/channel/") && url.contains("/stream/");
    }

    //Individual website retrieval:

    //Twitch Tracker retrieval:
//    private String[] getTTData(String url){
//
//    }
    //Stream Charts retrieval:
    private String[] getSCData(String url) throws IOException {
        String[] results=new String[4];     //0: streamer's name; 1: Stream ID; 2: Timestamp; 3: Duration.
        String userID;
        double duration=0.0;
        //Retrieve initial values:
        String pattern="streamscharts\\.com\\/twitch\\/channels\\/([a-zA-Z0-9]*)\\/streams\\/(\\d*)";
        Pattern r=Pattern.compile(pattern);
        Matcher m=r.matcher(url);
        if(m.find()){
            results[0]=m.group(1);
            results[1]=m.group(2);
        }

        //Retrieve user ID:
        String idJSON=getJSON("https://api.twitch.tv/v5/users/?login="+results[2]+"&client_id=ohroxg880bxrq1izlrinohrz3k4vy6");
        JSONObject joID=new JSONObject(idJSON);
        JSONObject users=joID.getJSONObject("users");
        JSONObject user=users.getJSONObject("0");
        userID=user.getString("_id");

        //Retrieve stream values:
        String dataJSON=getJSON("https://alla.streamscharts.com/api/free/streaming/platforms/1/channels/"+userID+"/streams/"+results[2]+"/statuses");
        JSONObject joD=new JSONObject(dataJSON);
        JSONArray items=joD.getJSONArray("items");
        for(int i=0; i<items.length();i++){
            JSONObject item=items.getJSONObject(i);
            if(i==0){
                results[2]=item.getString("stream_created_at");
            }
            duration+=item.getDouble("air_time");
        }

        results[3]=String.valueOf(duration);
        return results;
    }
}