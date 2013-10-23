package com.dank.festivalapp.wacken2014;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.res.Resources;
import android.util.Log;

import com.dank.festivalapp.lib.Band;
import com.dank.festivalapp.lib.DownloadFilesTask;
import com.dank.festivalapp.lib.FlavorProvider;
import com.dank.festivalapp.lib.News;
import com.dank.festivalapp.lib.ProviderServiceBase;

public class ProviderService extends ProviderServiceBase {

	private static String Url = "http://www.wacken.com/";
	private DownloadFilesTask downloadFile = new DownloadFilesTask();

	public ProviderService() 
	{
		super();
	}

	@Override
	protected String getFestivalName() 
	{
		Resources res = getResources();		
		return res.getString(R.string.app_name);
	}

	/**
	 * download the band index page and parse urls, urls are temporary stored in band description.
	 * @param page
	 */
	@Override
	protected List<Band> getBands() {
		String page = downloadFile.downloadUrl(Url + "de/woa2014/main-bands/billing-2014/");

		Document doc = Jsoup.parse(page, "UTF-8");
		List<Band> bands = new ArrayList<Band>();		

		Element e = doc.getElementsByClass("woa_line_up_list").first();
		Elements es = e.getElementsByClass("three_column_float");

		for (Element n : es )
		{	
			// we got here:
			// bandname 
			// band url
			// band add date
			String name = n.select("a").text();
			String relHref = n.select("a").attr("href"); 
			String added = n.select("span").text().replaceAll("Added:", "").trim();

			Log.w("BANDNAME", name);
			// band description contains detail url
			Band band = new Band(name, relHref);

			try {
				Date date = new SimpleDateFormat("dd.MM.yyyy").parse( added );
				band.setAddDate(date);
			} catch (ParseException exp) {
				exp.printStackTrace();
			}

			bands.add(band);
		}

		return bands;
	}



	/**
	 * method to make some band detail actions, e.g. in case band
	 * details are on a seconds url
	 * @param band
	 * @return
	 */
	protected Band getBandDetailed(Band band)
	{
		Log.w("parseBandInfos", band.getBandname());
		String page = downloadFile.downloadUrl(Url + band.getDescription() );

		Document doc = Jsoup.parse(page, "UTF-8");

		// band description
		Element descElem = doc.getElementsByClass("tx-woalineupmanagement-pi2").first();

		if (descElem != null)
		{
			String desc = doc.getElementsByClass("copyrightline").first().text();
			band.setDescription(desc);
			Log.w("desc", desc);
		}

		// band logo
		Element logoElem =  doc.getElementsByClass("imgtext-table").first().getElementsByAttributeValueMatching("src", "artist_images").first();
		
		// TODO normalize band name for reuse in other data provider
		if (logoElem != null)
		{
			String relLogoUrl = logoElem.attr("src");
			
			String logoFileName = getBandLogo(Url + relLogoUrl, band.getBandname());
			if ( logoFileName != null)
				band.setLogoFile(logoFileName);
		}

		// band foto
		Element fotoElem = doc.getElementsByClass("woa_artist_image_list").select("li").first().select("img").first();

		if (fotoElem != null)
		{
			String relFotoUrl = fotoElem.attr("src");
		
			String fotoFileName =  getBandPicture(Url + relFotoUrl, band.getBandname() );
			if ( fotoFileName != null )
				band.setFotoFile(fotoFileName);
		}

		// flavors
		// no band flavors on the web page itself
		FlavorProvider fp = new FlavorProvider();		
		for (String flavor : fp.getFlavors(band.getBandname()))
			band.addFlavor(flavor);

		// get band url
		// TODO: Web page is really bad to parse


		return band;
	}


	/**
	 * returns a list of all current News for this festival
	 * @return
	 */
	@Override
	protected List<News> getNewsShort() 
	{		
		List<News> newsList = new ArrayList<News>();
		String page = downloadFile.downloadUrl(Url + "de/woa2014/");

		Document doc = Jsoup.parse(page, "UTF-8");

		for (Element n : doc.getElementsByAttributeValueMatching("href", "de/woa2014/main-news/news/ansicht/") )
		{
			String detailsUrl = n.attr("href");
			String subject = n.text();
			
			News news = new News(subject, detailsUrl);	

			newsList.add(news);
			Log.d("getNewsShort", news.getSubject() + " " + news.getMessage() );			
		}

		return newsList;
	}


	/**
	 * returns details to the given news, the used url was temporary stored as message
	 * an another url  
	 * @param news
	 * @return
	 */
	protected News getNewsDetailed(News news)
	{
		String page = downloadFile.downloadUrl(Url + news.getMessage());
		Document doc = Jsoup.parse(page.replaceAll("<br />", "FestivalAppbr2n"), "UTF-8");
		
		Element e = doc.getElementsByClass("header1").first().parent();
		if (e != null)
		{
			// get message		
			news.setMessage(e.getElementsByClass("content").text().replaceAll("FestivalAppbr2n", "\n"));
		
			// get add date
			String date = e.getElementsByClass("copyrightline").first().text();

			try {
				Date d = new SimpleDateFormat("dd.MM.yyyy HH:mm").parse(date);
				if (d.getYear() < 100)
					d.setYear(d.getYear() + 2000);
								
				news.setDate(d);
			} catch (ParseException e1) {			
				e1.printStackTrace();
			}
		}
		
		Log.d("getNewsDetailed", news.getDateAsFormatedString() );
		return news;
	}


	/**
	 * TODO not available
	 */
	@Override
	protected List<BandGigTime> getRunningOrder() {

		// TODO
		List<BandGigTime> allTimesList = new ArrayList<BandGigTime>();
		return allTimesList;	
	}

}
