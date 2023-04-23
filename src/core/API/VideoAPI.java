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

package core.API;

import core.Compute;
import core.Enums.FileExtension;
import core.Enums.Quality;
import core.Feeds;
import core.Fuzz;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class handles all
 * of the API methods directly
 * related to VODs.
 */
public class VideoAPI {
    /**
     * This method gets the list of feeds
     * of a VOD that is still up from the
     * VOD ID.
     * This is NOT to be used for sub-only VODs.
     * @param VODID     Long value representing the VOD ID.
     * @return Feeds    Feeds object holding the list of VOD feeds and their corresponding qualities.
     */
    public static Feeds getVODFeeds(long VODID){
        String[] auth=getVODToken(VODID);  //0: Token; 1: Signature.
        return API.getPlaylist("https://usher.ttvnw.net/vod/"+VODID+".m3u8?sig="+auth[1]+"&token="+auth[0]+"&allow_source=true&player=twitchweb&allow_spectre=true&allow_audio_only=true");
    }

    /**
     * This method retrieves the M3U8 feeds for
     * sub-only VODs by utilising values provided
     * in the public VOD metadata API.
     * @param VODID     Long value representing the VOD ID to retrieve the feeds for.
     * @return Feeds    Feeds object holding all of the feed URLs and their respective qualities.
     */
    public static Feeds getSubVODFeeds(long VODID){
        Feeds feeds = new Feeds();

        // Read client secrets
        String client_id, oauth_token;
        try {
          JSONObject root = new JSONObject(new JSONTokener(
                new FileReader(
                  System.getProperty("user.home") + File.separator
                  + ".TwitchRecover" + File.separator + "secrets.json")));
          client_id = root.getString("client_Id");
          oauth_token = root.getString("oauth_token");
        } catch (Exception e) {
          e.printStackTrace();
          return feeds;
        }

        // Get the JSON response of the VOD
        String response="";
        try {
          CloseableHttpClient httpClient= HttpClients.createDefault();
          HttpGet httpget = new HttpGet("https://api.twitch.tv/helix/videos?id="+VODID);
          httpget.addHeader("User-Agent", "Mozilla/5.0");
          httpget.addHeader("Client-Id", client_id);
          httpget.addHeader("Authorization", oauth_token);
          CloseableHttpResponse httpResponse = httpClient.execute(httpget);
          if(httpResponse.getStatusLine().getStatusCode()==200){
            BufferedReader br=new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line;
            while ((line = br.readLine()) != null) {
              response += line;
            }
            br.close();
          }
          httpResponse.close();
          httpClient.close();
        } catch (Exception e) {
          e.printStackTrace();
          return feeds;
        }

        // Guess VOD url from JSON response
        JSONObject jO = new JSONObject(response);
        String thumbnail_url = jO.getJSONArray("data").getJSONObject(0).getString("thumbnail_url");
        String pattern = "https:\\/\\/static-cdn\\.jtvnw\\.net\\/cf_vods\\/([a-z0-9_]*)\\/[a-z0-9_]*\\/\\/thumb\\/thumb0-%\\{width\\}x%\\{height\\}\\.jpg";
        String domain = "https://" + Compute.singleRegex(pattern, thumbnail_url) + ".cloudfront.net";
        pattern = "https:\\/\\/static-cdn\\.jtvnw\\.net\\/cf_vods\\/[a-z0-9_]*\\/([a-z0-9_]*)\\/\\/thumb\\/thumb0-%\\{width\\}x%\\{height\\}\\.jpg";
        String url = domain + "/" + Compute.singleRegex(pattern, thumbnail_url);

        // Get available bitrates of VOD
        String token = getVODToken(VODID)[0];
        JSONObject jo = new JSONObject(token);
        JSONArray restricted = jo.getJSONObject("chansub").getJSONArray("restricted_bitrates");

        // Store available VOD urls
        for(int i = 0; i < restricted.length(); i++) {
          feeds.addEntry(url + "/" + restricted.get(i).toString() + "/index-dvr" + FileExtension.M3U8.fileExtension,
                         Quality.getQualityV(restricted.get(i).toString()));
        }

        return feeds;
    }

    /**
     * This method retrieves the
     * token and signature values
     * for a VOD.
     * @param VODID     Long value representing the VOD ID to get the token and signature for.
     * @return String[] String array holding the token in the first position and the signature in the second position.
     * String[2]: 0: Token; 1: Signature.
     */
    private static String[] getVODToken(long VODID){
        return API.getToken(String.valueOf(VODID), true);
    }
}
