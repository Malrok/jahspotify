package jahspotify.client;

import java.io.*;
import java.util.*;

import com.google.gson.Gson;
import jahspotify.web.media.*;
import jahspotify.web.queue.*;
import jahspotify.web.system.ServerIdentity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author Johan Lindquist
 */
public class JahSpotifyClient
{
    private String _baseURL;

    public JahSpotifyClient(final String baseURL)
    {
        _baseURL = baseURL;
    }

    public static void main(String[] args) throws Exception
    {
        final Set<ServerIdentity> serverIdentities = ServerBroadcasterClient.discoverServers("224.0.0.1", 9764, 10000, new ServerBroadcasterClient.ServerFoundListener()
        {
            @Override
            public void serverFound(final ServerIdentity serverIdentity)
            {
                System.out.println("serverIdentity = " + serverIdentity);
            }
        });



        JahSpotifyClient jahSpotifyClient = new JahSpotifyClient( "http://" + serverIdentities.iterator().next().getServerWebAddress() +":" + serverIdentities.iterator().next().getWebPort() + "/jahspotify/");

        final QueueConfiguration queueConfiguration = new QueueConfiguration();
        queueConfiguration.setAutoRefill(true);
        jahSpotifyClient.setQueueConfiguration(queueConfiguration);
        // addTrack(Arrays.asList("spotify:track:6UaRii9AH6Zss9xNMEQ2M9", "spotify:track:34q1KaLX8h73xE06xPBmNB", "spotify:track:52JyHLUiugFECIYBWM2qdh"));

        // command line:

        // jahspotify -u <url> --url=<url> queue <-l --list> <-a <track-id> --add=<track-id>> <-d <track-id> --del=<track-id>
        // jahspotify -u <url> --url=<url> player <-s --skip> <-p --pause> <-r --resume>
        // jahspotify -u <url> --url=<url> track <track-id>
        // jahspotify -u <url> --url=<url> album <album-id>
        // jahspotify -u <url> --url=<url> artist <artist-id>
        // jahspotify -u <url> --url=<url> playlist <playlist-id>
        // jahspotify -u <url> --url=<url> image -o <output-file> <image-id>

    }

    private QueueConfiguration getQueueConfiguration() throws IOException
    {
        QueueConfiguration queueConfiguration = new QueueConfiguration();
        queueConfiguration.setRepeatCurrentTrack(false);
        queueConfiguration.setRepeatCurrentQueue(false);

        final String s = getData(_baseURL + "queue/configuration");
        return deserialize(s,QueueConfiguration.class);
    }

    public QueueConfiguration setQueueConfiguration(QueueConfiguration queueConfiguration) throws IOException
    {
        final String s = postData(_baseURL + "queue/jahspotify:queue:default/configuration", queueConfiguration);
        QueueConfiguration newQueueConfiguration = deserialize(s,QueueConfiguration.class);
        return newQueueConfiguration;
    }

    private <T> T  deserialize(final String s, final Class<T> classOfT)
    {
        Gson gson = new Gson();
        return gson.fromJson(s, classOfT);
    }

    public Library readLibrary() throws IOException
    {
        String s = getData(_baseURL  + "library/");
        return deserialize(s, Library.class);
    }

    public Image readImage(final String uri, boolean streamable) throws Exception
    {
        try
        {
            if (streamable)
            {
                InputStream inputStream = getDataAsStream(_baseURL + "media/" + uri);
                return new Image(uri,inputStream);
            }
            else
            {
                final byte[] dataAsBytes = getDataAsBytes(_baseURL + "media/" + uri);
                return new Image(uri,dataAsBytes);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw e;
        }
    }

    public LibraryEntry readFolder(final String uri, final int levels) throws IOException
    {
        String s = getData(_baseURL  + "media/" + uri + "?levels=" + levels);
        return deserialize(s, LibraryEntry.class);
    }

    public Playlist readPlaylist(final String uri) throws IOException
    {
        String s = getData(_baseURL  + "media/" + uri);
        return deserialize(s,Playlist.class);
    }

    public Track readTrack(final String uri) throws IOException
    {
        String s = getData(_baseURL  + "media/" + uri);
        return deserialize(s,Track.class);
    }

    public void queueTracks(boolean autoPlay, List<String> uris) throws IOException
    {
        QueueTracksRequest queueTracksRequest = new QueueTracksRequest();
        queueTracksRequest.setAutoPlay(autoPlay);
        queueTracksRequest.setURIQueue(uris);
        final String s = postData(_baseURL + "queue/jahspotify:queue:default/add", queueTracksRequest);
    }

    private InputStream getDataAsStream(final String url) throws IOException
    {
        HttpClient httpClient = new DefaultHttpClient();

        final HttpGet httpGet = new HttpGet(url);
        final HttpResponse execute = httpClient.execute(httpGet);

        if (execute.getStatusLine().getStatusCode() == 200)
        {
            return execute.getEntity().getContent();
        }
        return  null;
    }

    private static String getData(final String url) throws IOException
    {
        HttpClient httpClient = new DefaultHttpClient();

        final HttpGet httpGet = new HttpGet(url);
        final HttpResponse execute = httpClient.execute(httpGet);

        if (execute.getStatusLine().getStatusCode() == 200)
        {
            final InputStream content = execute.getEntity().getContent();
            BufferedReader stringReader = new BufferedReader(new InputStreamReader(content));
            StringBuffer sb = new StringBuffer();
            String tmp = stringReader.readLine();
            while (tmp != null)
            {
                sb.append(tmp);
                tmp = stringReader.readLine();
            }
            return sb.toString();
        }
        return  null;
    }

    private static byte[] getDataAsBytes(final String url) throws IOException
    {
        HttpClient httpClient = new DefaultHttpClient();

        final HttpGet httpGet = new HttpGet(url);
        final HttpResponse execute = httpClient.execute(httpGet);

        if (execute.getStatusLine().getStatusCode() == 200)
        {
            final InputStream content = execute.getEntity().getContent();
            // BufferedReader stringReader = new BufferedReader(new InputStreamReader(content));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[10000];

            int len = content.read(buffer,0,10000);
            while (len != -1)
            {
                baos.write(buffer,0,len);
                len = content.read(buffer,0,10000);
            }

            return baos.toByteArray();
        }
        return  null;
    }


    private static String postData(final String url, final Object obj) throws IOException
    {
        Gson gson = new Gson();
        String data = gson.toJson(obj);

        HttpClient httpClient = new DefaultHttpClient();

        final HttpPost httpPost = new HttpPost(url);
        final BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setContentType("application/data");
        basicHttpEntity.setContentType("application/data");
        basicHttpEntity.setContent(new ByteArrayInputStream(data.getBytes()));
        httpPost.setEntity(basicHttpEntity);
        httpPost.addHeader("Accept","application/json");
        final HttpResponse execute = httpClient.execute(httpPost);

        if (execute.getStatusLine().getStatusCode() == 200)
        {
            final InputStream content = execute.getEntity().getContent();
            BufferedReader stringReader = new BufferedReader(new InputStreamReader(content));
            StringBuffer sb = new StringBuffer();
            String tmp = stringReader.readLine();
            while (tmp != null)
            {
                sb.append(tmp);
                tmp = stringReader.readLine();
            }


            return sb.toString();

        }

        return  null;
    }

    public Album readAlbum(final String albumLink) throws IOException
    {
        String s = getData(_baseURL  + "media/" + albumLink);
        return deserialize(s,Album.class);
    }

    public FullTrack readFullTrack(final String id) throws IOException
    {
        String s = getData(_baseURL  + "media/" + id + "?full=true");
        return deserialize(s,FullTrack.class);
    }
}
