package jahspotify.web;

import java.io.*;
import java.util.*;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.*;

import jahspotify.media.*;
import org.apache.commons.logging.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author Johan Lindquist
 */
@Controller
public class ImageController extends BaseController
{
    private Log _log = LogFactory.getLog(ImageController.class);
    private String _cacheLocation = "/var/lib/jahspotify/web/cache/images/";

    @RequestMapping(value = "/image/{link}", method = RequestMethod.GET)
    public void retrieveImage(@PathVariable(value = "link") String link, final HttpServletResponse httpServletResponse)
    {
        try
        {
            final Link uri = Link.create(link);

            if (uri == null)
            {
                final SimpleStatusResponse simpleStatusResponse = new SimpleStatusResponse();
                simpleStatusResponse.setResponseStatus(ResponseStatus.MISSING_PARAMETER);
                writeResponse(httpServletResponse, simpleStatusResponse);
                return;
            }

            if (!uri.isImageLink())
            {
                final SimpleStatusResponse simpleStatusResponse = new SimpleStatusResponse();
                simpleStatusResponse.setResponseStatus(ResponseStatus.INVALID_PARAMETER);
                writeResponse(httpServletResponse, simpleStatusResponse);
                return;
            }


            Image image = getFromCache(uri);
            if (image == null)
            {
                image = _jahSpotifyService.getJahSpotify().readImage(uri);
            }

            if (image != null)
            {
                byte[] bytes = image.getBytes();

                cacheImage(uri, bytes);

                httpServletResponse.setContentType("image/jpeg");

                // Setup caching parameters
                httpServletResponse.addHeader("Last-Modified", "Tue, 6 Sep 2005 15:10:00 UTC");

                final Calendar utc = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
                utc.add(Calendar.YEAR, 1);

                httpServletResponse.addHeader("Expires", toHttpDate(utc.getTime()));

                httpServletResponse.setContentLength(bytes.length);

                final ServletOutputStream outputStream = httpServletResponse.getOutputStream();
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
            }
            else
            {
                final SimpleStatusResponse simpleStatusResponse = new SimpleStatusResponse();
                simpleStatusResponse.setResponseStatus(ResponseStatus.RESOURCE_NOT_FOUND);
                writeResponse(httpServletResponse, simpleStatusResponse);
                return;
            }

        }
        catch (Exception e)
        {
            _log.error("Error while retrieving image: " + e.getMessage(), e);
            super.writeErrorResponse(httpServletResponse, e);
        }


    }

    private Image getFromCache(final Link uri)
    {
        final String substring = uri.asString().substring(uri.asString().lastIndexOf(":") + 1);
        File f = new File(_cacheLocation + substring);
        if (f.exists())
        {
            _log.debug("Image found in cache: " + uri);

            try
            {
                FileInputStream fileInputStream = new FileInputStream(f);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[16000];
                int len = fileInputStream.read(buffer,0,16000);
                while (len != -1)
                {
                    baos.write(buffer,0,len);
                    len = fileInputStream.read(buffer,0,16000);
                }
                _log.debug("Image size in cache: " + baos.toByteArray().length);

                if (baos.toByteArray().length > 0)
                {
                    return new Image(uri,baos.toByteArray());
                }
                else
                {
                    _log.debug("Image found in cache, but size == 0");
                    f.delete();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        return null;
    }

    private void cacheImage(final Link uri, final byte[] bytes)
    {
        final String substring = uri.asString().substring(uri.asString().lastIndexOf(":") + 1);
        new File(_cacheLocation).mkdirs();
        try
        {
            FileOutputStream fileOutputStream = new FileOutputStream(_cacheLocation + substring);
            fileOutputStream.write(bytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}
