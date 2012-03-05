package jahspotify.web;

import java.util.*;
import javax.servlet.http.*;

import jahspotify.media.*;
import jahspotify.media.Album;
import jahspotify.media.Disc;
import jahspotify.media.Link;
import jahspotify.web.media.*;
import org.apache.commons.logging.*;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * @author Johan Lindquist
 */
@Controller
@RequestMapping("/album")
public class AlbumController extends BaseController
{
    private Log _log = LogFactory.getLog(AlbumController.class);

    @RequestMapping(value = "/{link}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public jahspotify.web.media.Album retrieveAlbum(@PathVariable(value = "link") String link)
    {
        try
        {
            final Link albumLink = Link.create(link);
            Album album = _jahSpotifyService.getJahSpotify().readAlbum(albumLink);
            return convertToWebAlbum(album);
        }
        catch (Exception e)
        {
            _log.error("Error while retrieving album: " + e.getMessage(), e);
            throw new JahSpotifyWebException();
        }
    }

    private jahspotify.web.media.Album convertToWebAlbum(final Album album)
    {

        jahspotify.web.media.Album webAlbum = new jahspotify.web.media.Album();

        webAlbum.setId(toWebLink(album.getId()));
        webAlbum.setCover(toWebLink(album.getCover()));
        webAlbum.setArtist(toWebLink(album.getArtist()));
        webAlbum.setDiscs(toWebDiscs(album.getDiscs()));
        webAlbum.setType(jahspotify.web.media.AlbumType.valueOf(album.getType().name()));
        // webAlbum.setRestrictions(toWebRestrictions(album.getRestrictions()));

        BeanUtils.copyProperties(album, webAlbum, new String[]{"id", "restrictions", "artist", "cover", "type", "discs"});
        return webAlbum;
    }

    private List<jahspotify.web.media.Disc> toWebDiscs(final List<Disc> discs)
    {
        List<jahspotify.web.media.Disc> webDiscs = new ArrayList<jahspotify.web.media.Disc>();
        for (Disc disc : discs)
        {
            webDiscs.add(toWebDisc(disc));
        }
        return webDiscs;
    }

    private jahspotify.web.media.Disc toWebDisc(final Disc disc)
    {
        jahspotify.web.media.Disc webDisc = new jahspotify.web.media.Disc();
        webDisc.setTracks(toWebLinks(disc.getTracks()));
        return webDisc;
    }

}
