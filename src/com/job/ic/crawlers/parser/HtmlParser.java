/**
 * @author Thanaphon Suebchua
 */
package com.job.ic.crawlers.parser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import com.job.ic.crawlers.daos.UrlDAO;
import com.job.ic.crawlers.daos.UrlDb;
import com.job.ic.crawlers.models.CrawlerConfig;
import com.job.ic.crawlers.models.LinksModel;
import com.job.ic.crawlers.models.PageObject;
import com.job.ic.nlp.services.Checker;
import com.job.ic.utils.HttpUtils;

import org.apache.log4j.Logger;

public class HtmlParser {

	private static Logger logger = Logger.getLogger(HtmlParser.class);
	/*
	 * public static String[] blacklistHost = { "5starhotelindia.com",
	 * "about.com", "lasvegashomesandcondos.com",
	 * "europerealestatedirectory.com", "uscondex.com", "dubailandhomes.co.uk",
	 * "panamabeachrealty.com", "airfrance.com", "cpanama.com",
	 * "condosutah.com", "turkish-property-world.com", "property2000.com,",
	 * "ukrealestatedirectory.com", "dubaipropertylistings.com", "condo.com",
	 * "x-rates.com", "internationalrealestatedirectory.com",
	 * "australiarealestatecentral.com", "condosutah.com",
	 * "florida-disney-villa-rentals.com", "villamartin-holiday-villas.co.uk",
	 * "ultra-properties.com", "dubailandhomes.co.uk", "ownerdirect.com",
	 * "holt-realty.com", "startpagina.nl", "penang.ws", "srilanka-hotels.ws",
	 * "visit-mekong.com", "phuket.com", "phiphi.phuket.com", "china-hotels.ws",
	 * "philippines-hotels.ws", "go-seychelles.com", "indonesia-holidays.com",
	 * "japan-hotels.ws", "asiawebdirect.com", "taiwan-hotels.net",
	 * "india-hotel.net", "borneo-hotels.com", "korea-hotels.net",
	 * "krabi-hotels.com", "langkawi-info.com", "bangkok.com",
	 * "khaolak-hotels.com", "singapore-guide.com", "huahin.bangkok.com",
	 * "hong-kong-hotels.ws", "kuala-lumpur.ws", "kosamui.com",
	 * "china-macau.com", "malacca.ws", "thailand-guide.com",
	 * "maldives-resorts.net", "hotels.com", "visit-malaysia.com",
	 * "chiangmai.bangkok.com", "bali-indonesia.com", "omniture.com",
	 * "koh-chang.bangkok.com", "startpagina.nl", "penang.ws",
	 * "srilanka-hotels.ws", "visit-mekong.com", "phuket.com",
	 * "phiphi.phuket.com", "china-hotels.ws", "philippines-hotels.ws",
	 * "go-seychelles.com", "indonesia-holidays.com", "japan-hotels.ws",
	 * "asiawebdirect.com", "taiwan-hotels.net", "india-hotel.net",
	 * "borneo-hotels.com", "korea-hotels.net", "krabi-hotels.com",
	 * "langkawi-info.com", "bangkok.com", "khaolak-hotels.com",
	 * "singapore-guide.com", "huahin.bangkok.com", "hong-kong-hotels.ws",
	 * "kuala-lumpur.ws", "kosamui.com", "china-macau.com", "malacca.ws",
	 * "thailand-guide.com", "maldives-resorts.net", "hotels.com",
	 * "visit-malaysia.com", "chiangmai.bangkok.com", "bali-indonesia.com",
	 * "omniture.com", "koh-chang.bangkok.com", };
	 */

	public static String[] blacklistHost = { "5starhotelindia.com", "about.com", "lasvegashomesandcondos.com", "europerealestatedirectory.com", "uscondex.com", "dubailandhomes.co.uk",
			"panamabeachrealty.com", "airfrance.com", "cpanama.com", "condosutah.com", "turkish-property-world.com", "property2000.com,", "ukrealestatedirectory.com", "dubaipropertylistings.com",
			"condo.com", "x-rates.com", "internationalrealestatedirectory.com", "australiarealestatecentral.com", "condosutah.com", "florida-disney-villa-rentals.com",
			"villamartin-holiday-villas.co.uk", "ultra-properties.com", "dubailandhomes.co.uk", "ownerdirect.com", "holt-realty.com", "startpagina.nl", "penang.ws", "srilanka-hotels.ws",
			"visit-mekong.com", "phuket.com", "phiphi.phuket.com", "china-hotels.ws", "philippines-hotels.ws", "go-seychelles.com", "indonesia-holidays.com", "japan-hotels.ws", "asiawebdirect.com",
			"taiwan-hotels.net", "india-hotel.net", "borneo-hotels.com", "korea-hotels.net", "krabi-hotels.com", "langkawi-info.com", "bangkok.com", "khaolak-hotels.com", "singapore-guide.com",
			"huahin.bangkok.com", "hong-kong-hotels.ws", "kuala-lumpur.ws", "kosamui.com", "china-macau.com", "malacca.ws", "thailand-guide.com", "maldives-resorts.net", "hotels.com",
			"visit-malaysia.com", "chiangmai.bangkok.com", "bali-indonesia.com", "omniture.com", "koh-chang.bangkok.com", "startpagina.nl", "penang.ws", "srilanka-hotels.ws", "visit-mekong.com",
			"phuket.com", "phiphi.phuket.com", "china-hotels.ws", "philippines-hotels.ws", "go-seychelles.com", "indonesia-holidays.com", "japan-hotels.ws", "asiawebdirect.com", "taiwan-hotels.net",
			"india-hotel.net", "borneo-hotels.com", "korea-hotels.net", "krabi-hotels.com", "langkawi-info.com", "bangkok.com", "khaolak-hotels.com", "singapore-guide.com", "huahin.bangkok.com",
			"hong-kong-hotels.ws", "kuala-lumpur.ws", "kosamui.com", "china-macau.com", "malacca.ws", "thailand-guide.com", "maldives-resorts.net", "hotels.com", "visit-malaysia.com",
			"chiangmai.bangkok.com", "bali-indonesia.com", "omniture.com", "koh-chang.bangkok.com",

	};

	public static String[] ibooked = { "hotel-mix.de", "hotelmix.fr", "hotelmix.es", "hotelmix.it", "ibooked.nl", "ibooked.com.br", "ibooked.dk", "www.booked.cz", "www.booked.hu", "booked.com.pl",
			"ibooked.gr", "booked.jp", "ibooked.cn", "booked.kr", "nochi.com", "booked.co.il", "bookeder.com", "albooked.com", "booked.net", "ibooked.", "hotelmix." };

	// /*
	public static String[] blogs = { "ameblo.jp", "geeklog.jp", "yoshimi-tendo.com", "s-tokura.com", "mblg.tv", "abe-natsumi.com", "shinoby.net", "dreamlog.jp", "shimazu-aya-koenkai.com",
			"dekuchin.com", "mfound.jp", "toshiharu-furukawa.jp", "yuki-koyanagi.jp", "mrsc.jp", "crooz.jp", "josei-bigaku.jp", "sososo291.com", "favotter.net", "shinichi-osawa.com", "main.jp",
			"nifty.com", "cocolog-nifty.com", "twinavi.jp", "diet-blog.net", "accessjournal.jp", "google.com", "takadanobuhiko.com", "spark-atv.com", "gg-m.jp", "buzztter.com", "buttobi.net",
			"nakanoyoshie.com", "chafurin.com", "pupu.jp", "abundance-music.com", "honjo.com", "hanamarl.com", "flow.mu", "beamie.jp", "eplus2.jp", "jaxa.jp", "aoyamakaori.com", "e-tabata.net",
			"kizasi.jp", "sozamix.net", "tennis.jp", "fixrecords.com", "mongol800.jp", "ikora.tv", "teacup.com", "takanoteruko.com", "spora.jp", "siyouyo.com", "nitta-yoshihiro.com",
			"about-anti-aging.net", "alphabloggers.com", "go-naminori.com", "kaneko-genjiro.jp", "esakitakashi.com", "340340.net", "blogcity.jp", "weavermusic.jp", "akb48teamogi.jp", "peacedelic.jp",
			"kitashuhei.com", "yoshika.info", "blogherald.com", "kawano-yoshihiro.com", "ameblo.jp", "way-nifty.com", "dreamscometrue.com", "sblo.jp", "chinamisan-blog.com", "chikakofuruya.com",
			"hiromitsu-aoki.com", "akino-kozo.com", "kimbianca.com", "nikki-k.jp", "hanedatakuya.com", "doorblog.jp", "seesaa.jp", "tomoka-t.net", "areablog.jp", "hamada-m-blog.com",
			"harukatomiyuki.net", "ichi-jump.com", "yokosuka-curry.com", "saigenji.com", "ruinagai.com", "zubora-mama.com", "atwiki.jp", "sasanomichiru.net", "movabletype.jp", "country-proud.jp",
			"yukiyo.com", "blogspot.com", "shiboo.jp", "tkomine.com", "timelog.jp", "tweetswind.com", "nomuraeri.com", "mizukinana.jp", "businessblog.jp", "wordpress.com", "simplog.jp", "fc2.com",
			"jugem.jp", "newaudiogram.com", "nishino73.com", "namisuke.com", "earth-king.cc", "haku-music.net", "azumamadoka.com", "hayashi-seiichi.com", "takeshihosomi.com", "twipple.jp",
			"yataro.jp", "dcnblog.jp", "dendou.jp", "nagasawa-hiroaki.jp", "supercell.sc", "hohoko-style.com", "eniblo.com", "players.tv", "y2-66.com", "10chin.net", "gr.jp", "shishido-kavka.com",
			"unscandal.com", "fuyumi-fc.com", "takeru-kobayashi.com", "ieiri.net", "lirionet.jp", "kreva.biz", "elog-ch.net", "mao55.net", "air-nifty.com", "actiblog.com", "kenjirosakiya.com",
			"tblog.jp", "abgo.jp", "tabuseyuta.com", "userlocal.jp", "maruta.be", "web-mono.net", "flames-japan.com", "kaorisasaki.com", "retired.at", "endoh-masaaki.com", "kusanohitoshi.com",
			"meaning666.com", "weibo.com", "takeshi-matsumoto.jp", "harudake.net", "mkx.jp", "koikikukan.com", "fanmo.jp", "livedoor.com", "56-design.com", "kumagai.com", "gakulog.net",
			"authority.jp", "kuruten.jp", "ck2-blogger.jp", "shuntorigoe.com", "amob.jp", "ldblog.jp", "ewok.jp", "junglekouen.com", "txt-nifty.com", "hamanoyuka.net", "madamefigaro.jp",
			"dtiblog.com", "blog-headline.jp", "ozrin.net", "checker-berry.com", "kouzuki-r.com", "goodpic.com", "asablo.jp", "straightener.net", "nojimakenji.com", "jp-tumblr.com", "progoo.com",
			"eponica.net", "sakaiichirou.com", "han.org", "boo-log.com", "mizobatajunpei.com", "eonet.jp", "miyanomamoru-blog.com", "ellegirl.jp", "candypop.jp", "real-cosme.net", "cabrain.net",
			"augusta-mobile.com", "t1ss.net", "flatlabs.net", "krazybee.jp", "miliyah.com", "smalllight.net", "m-drive.net", "fukushima.jp", "kinoshitamariko.com", "kenko.com", "yoshida-ushio.com",
			"thelma.jp", "kobayashi-tomomi.com", "xrea.com", "goyah.net", "keiichimiyako.com", "pakila.jp", "yuhki-nakayama.com", "pillows.jp", "kotono8.com", "andomifuyu.com", "namjai.cc",
			"emirimiyamoto.com", "titan-happy.jp", "warasuto.com", "kaza-hana.jp", "tword.net", "blog-text.jp", "ryuchan.jp", "zerone.jp", "chu.jp", "ideyasuaki.net", "floq.jp", "eigotown.com",
			"takaradakyoko.com", "aspota.jp", "higerock.com", "waxpoetics.jp", "tamurapan.com", "twitis.me", "skuare.net", "kawadajunko.com", "blog.jp", "chikaco.com", "velvet.jp", "tights.jp",
			"superfly-web.com", "exblog.jp", "takanohana.net", "eco-reso.jp", "labola.jp", "dancingdolls.jp", "pecolly.jp", "scope-web.net", "kuru2jam.com", "454545.net", "mami-kawada.jp",
			"hidakatoru.com", "radwimps.jp", "tomozuna-beya.jp", "taiyolab.com", "weblogs.jp", "yozawa-tsubasa.info", "i-lia.com", "mukaiaki.com", "sunnyday.jp", "masahidesakuma.net", "hinasui.com",
			"putimiracle.com", "nezumicky.com", "turtleplanning.com", "chin-don.net", "134r.com", "kokia.com", "obu.to", "redcruise.com", "shumilog.com", "shinobi.jp", "talentblog.jp", "ti-da.net",
			"blogn.org", "game-blog-ranking.com", "starplayers.jp", "sexlife.jp", "tottori-guide.jp", "asahi-net.jp", "sizzle-ohtaka.com", "leon.jp", "madohuchi.com", "artifact-jp.com", "neoteny.com",
			"umiharajunko.com", "yakushiji.info", "akikograce.net", "arrow-arrow.com", "oie-satoshi.com", "mizuguchi.biz", "nomadqueen.com", "soubunshu.com", "kuribo.info", "matsusen.net",
			"shinshu.fm", "casa-b.jp", "chowari.jp", "gtitter.com", "togech.jp", "twdesk.com", "badmintonfaun.jp", "tatsuru.com", "kitadani-hiroshi.com", "naganoblog.jp", "romi-unie.jp",
			"go-to-hawaii.com", "japaho.com", "bakufu.jp", "blogpeople.net", "codomo-inc.jp", "akane-iijima.jp", "soapfun.net", "chip.jp", "kamui-kobayashi.com", "tumblr.com", "amuseblog.jp",
			"kame-on.com", "kentei.cc", "a-thera.com", "luckyikeda.com", "lineblog.me", "jimab.net", "yusukefc.com", "hashtagcloud.net", "unison-s-g.com", "zzhh.jp", "dwnicols.com", "ando-yuko.com",
			"a-utada.com", "hitomi-yaida.com", "serenebach.net", "blogcustomize.com", "sugiyama-style.tv", "doramix.com", "gremz.com", "starblog.jp", "biranger.jp", "makimori.com", "atsuize.jp",
			"gazoo.com", "keinishikori.com", "fieldcorp.jp", "tctc.tv", "tabisuma.jp", "pro-picasso.com", "livedoor.biz", "guts-kaneko.com", "greboo.com", "aliproject.jp", "superbellz.com",
			"pelogoo.com", "noregretlife.com", "tepping.jp", "nagahamahiroyuki.com", "funkist.info", "halcamiho.com", "tonpi.net", "t-kasae.com", "fujioka-mami.com", "choshuriki.com",
			"sportsnavi.com", "hachiojip.net", "livedoor.jp", "diamondblog.jp", "konami.jp", "kakubarhythm.com", "nogizaka46.com", "goseas-surf.com", "natsumifc.com", "jetrun.jp", "x0.com",
			"twpro.jp", "1oven.com", "tochiazuma.jp", "ens-serve.net", "nicovideo.jp", "tsubuyaki-recipe.com", "ma-do.net", "shoe-g.com", "fujitvkidsclub.jp", "mieko.jp", "typepad.com",
			"funkyblog.jp", "obinata-ski.com", "atm-factory.com", "yamashita-yoshiki.jp", "mobloget.jp", "hatenablog.com", "pop-group.net", "yahoofs.jp", "saita.net", "horipro.jp", "liveson.org",
			"boo-bana.com", "d21blog.jp", "afloodofcircle.com", "a-blogcms.jp", "keyakizaka46.com", "canpan.info", "glam.jp", "syurispeiloff.jp", "openers.jp", "dclog.jp", "js-style.com",
			"onoyuki.com", "honeyee.com", "co.uk", "the-journal.jp", "aaequal.com", "robolog.jp", "koga-yuichiro.jp", "ryo-ishikawa.jp", "amon-miyamoto.com", "asks.jp", "rocomotion.jp", "freeex.jp",
			"star-studio.jp", "blog-koukoku.com", "hirosawatadashi.com", "avex.jp", "with2.net", "ajnt.com", "ac.jp", "thestarbems.com", "ishii-midori.jp", "chabeya.com", "tennis365.net",
			"k-shimba.com", "hiromiuehara.com", "rebecca.ac", "taharasoichiro.com", "anisen.tv", "themeparkband.com", "hyo-ken.net", "btown.jp", "webry.info", "life-k.com", "nucleuscms.org",
			"club-willbe.jp", "travelog.jp", "hayashiweek.com", "jp-soft.com", "iemoto.com", "ekitan.com", "groovymedia.biz", "hotei.com", "champdegogo.com", "freegameparts.jp", "thesketchbook-fc.jp",
			"mogi-log.com", "myspace.com", "satokoto.com", "ayako-ishikawa.com", "barks.jp", "takeshimahiroshi.com", "ishikawasayuri.com", "peps.jp", "blog-parts.com", "decoo.jp", "netmania.jp",
			"tanakateruyo.com", "shiga-saku.net", "imaokarie.com", "lisme.jp", "blogmura.com", "mukago.jp", "yaplog.jp", "di-do.net", "mao-asada.jp", "blogram.jp", "kimuramoriyo.com", "youtube.com",
			"syncl.jp", "harakiri-style.com", "alfoo.org", "mariko-shinoda.net", "fukudakohei.info", "hisaaki.net", "fashionjp.net", "ameba.jp", "akane-osawa.com", "blogspot.jp", "seesaa.net",
			"d-gcr.com", "rirelog.com", "quq.jp", "uzo.net", "kazunoblog.com", "slmame.com", "moo.jp", "okadajun.com", "matsuo.tv", "b-shoku.jp", "naomilemon.com", "soya.bz", "laura.jp",
			"monoportal.com", "voiceblog.jp", "stardust-web.net", "yowayowacamera.com", "room66plus.com", "wakisaka.biz", "junkstage.com", "6109.jp", "junna.tv", "togetter.com", "fb-f.jp", "gree.jp",
			"sugizo.com", "shakalabbits.com", "ito.com", "buck-tick.com", "typepad.jp", "pingoo.jp", "yukinoxxx.com", "fukabori.jp", "goo-net.com", "recipe-blog.jp", "trackword.biz", "moonphase.jp",
			"areiraise.com", "netsket.com", "blogzine.jp", "ohse-kohime.jp", "jj-jj.net", "wordpress.org", "chiyo-katsumasa.com", "h-takii.com", "aegis-8.com", "u3music.com", "kumamon-official.jp",
			"tea-nifty.com", "blogwasabi.com", "pe-z.jp", "tvasobi.jp", "yamdas.org", "xn--pss91gi3l.com", "kiramune.jp", "apalog.com", "yamamotokohei.com", "artistblog.jp", "blogranking.net",
			"onukitaeko.jp", "office-augusta.com", "bigcosmic.com", "lt-ac.com", "hobidas.com", "jisin.jp", "kukikodan.com", "politter.com", "enasoku.com", "dreama.jp", "biglobe.ne.jp", "ocn.ne.jp",
			"goo.ne.jp" };
	// */

	public static HashSet<String> blacklist = new HashSet<>(Arrays.asList(blacklistHost));

	private ArrayList<LinksModel> parseAllLink(Checker checker, PageObject fp) {
		byte[] b = fp.getContent();

		ArrayList<LinksModel> l = new ArrayList<LinksModel>();
		ArrayList<LinksModel> output = new ArrayList<LinksModel>();
		Source sc;
		try {
			sc = new Source(new ByteArrayInputStream(b));
			sc.setLogger(null);

			if (checker != null)
				fp.setPageScore(checker.checkHtmlContent(b));
			else
				fp.setPageScore(0);

			addMetaRedirect(fp, l, sc);
			addFrameLink(fp, l, sc);
			addAnchorLink(fp, l, sc);

			for (LinksModel m : l) {
				String url = m.getLinkUrl();
				String baseuri = fp.getUrl();

				if (url.contains("#")) {
					url = url.substring(0, url.indexOf("#"));
				}

				url = HttpUtils.fixRelativePath(baseuri, url);

				if (url == null || url.trim().length() == 0)
					continue;

				try {
					url = HttpUtils.getStaticUrl(url);
				} catch (Exception e) {

				}

				if (shouldFilter(url)) {
					continue;
				}

				// logger.info(url + "\t" + UrlDb.getUrlDAO().containUrl(url));
				m.setLinkUrl(url);
				output.add(m);
			}
		} catch (IOException e) {
			logger.info(e.getCause() + "\t" + e.getMessage());
		} finally {
			sc = null;
			l.clear();
		}

		return output;
	}

	public ArrayList<LinksModel> parse(byte[] input, String srcUrl) {
		ArrayList<LinksModel> links = parseAllLink(null, new PageObject(input, srcUrl));
		return links;
	}

	public ArrayList<LinksModel> parse(Checker checker, PageObject fp) {
		ArrayList<LinksModel> links = parseAllLink(checker, fp);
		return links;
	}

	public void parse(Checker checker, PageObject fp, HashMap<String, ArrayList<LinksModel>> destSegments, UrlDAO urlDao) {
		try {

			String url = null;
			ArrayList<LinksModel> l = parseAllLink(checker, fp);

			if (l == null)
				return;

			for (int i = 0; i < l.size(); i++) {
				url = ((LinksModel) l.get(i)).getLinkUrl();

				if (urlDao != null && urlDao.checkAndAddUrl(url, false))
					continue;

				String segment = HttpUtils.getBasePath(url);

				if (segment != null) {
					if (destSegments.containsKey(segment)) {
						destSegments.get(segment).add(l.get(i));
					} else {
						ArrayList<LinksModel> tmp = new ArrayList<>();
						tmp.add(l.get(i));
						destSegments.put(segment, tmp);
					}
				}
			}

			l.clear();
			l = null;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getCause() + ">>" + e.getMessage());
		}
	}

	public void parse(Checker checker, PageObject fp, ArrayList<LinksModel> destLinks, UrlDAO urlDao) {
		try {
			ArrayList<LinksModel> l = parseAllLink(checker, fp);

			if (l != null) {
				for (int i = 0; i < l.size(); i++) {
					String url = ((LinksModel) l.get(i)).getLinkUrl();

					if (urlDao != null && urlDao.checkAndAddUrl(url, false))
						continue;

					destLinks.add(l.get(i));
				}

			}

			l.clear();
			l = null;
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getCause() + ">>" + e.getMessage());
		}
	}

	private void addMetaRedirect(PageObject fp, ArrayList<LinksModel> l, Source sc) {
		List<Element> s = sc.getAllElements(HTMLElementName.META);
		
		String sourceUrl = fp.getUrl();
		Element element;
		try {

			String tmp;
			for (int i = 0; i < s.size(); i++) {
				element = s.get(i);
				if (element == null || element.getAttributeValue("content") == null)
					continue;

				tmp = s.get(i).getAttributeValue("content").toString().toLowerCase();
				if (tmp.contains("url=")) {
					tmp = tmp.substring(tmp.indexOf("url=") + "url=".length());

					if (tmp.contains("\"")) {
						tmp = tmp.substring(0, tmp.indexOf("\""));
					}

					if (tmp.contains("'")) {
						tmp = tmp.substring(0, tmp.indexOf("'"));
					}

					l.add(new LinksModel(sourceUrl, tmp, null, fp.getPageScore()));
				}
			}

		} catch (Exception e) {
		}
		s.clear();
		s = null;
	}

	private void addAnchorLink(PageObject fp, ArrayList<LinksModel> l, Source sc) {
		String sourceUrl = fp.getUrl();
		List<Element> s = sc.getAllElements(HTMLElementName.A);
		Element element;
		String tmp, anchorText = null;
		int k = 0;
		try {
			String page = sc.toString();
			int start = -1, end = -1;
			int orgStart = -1, orgEnd = -1;
			for (int i = 0; i < s.size(); i++) {
				try {
					element = s.get(i);
					if (element == null || element.getAttributeValue("href") == null)
						continue;

					tmp = element.getAttributeValue("href").toString();

					if (tmp.equals("") || tmp == null)
						continue;

					if (tmp.toLowerCase().contains("ymsgr:") || tmp.toLowerCase().contains("callto:") || tmp.toLowerCase().contains("javascript:") || tmp.toLowerCase().contains("mailto:")
							|| tmp.toLowerCase().contains("tel:") || tmp.toLowerCase().contains("skype:"))
						continue;

					orgStart = s.get(i).getBegin();
					orgEnd = s.get(i).getEnd();

					if (orgStart - CrawlerConfig.getConfig().getWindowSize() < 0)
						start = 0;
					else
						start = orgStart - CrawlerConfig.getConfig().getWindowSize();

					if (orgEnd + CrawlerConfig.getConfig().getWindowSize() > page.length())
						end = page.length();
					else
						end = orgEnd + CrawlerConfig.getConfig().getWindowSize();

					String before = page.substring(start, orgStart);

					String after = page.substring(orgEnd, end);

					if ((k = before.indexOf(">")) > -1) {
						start = k + 1;
						before = before.substring(start);
					}

					if ((k = after.lastIndexOf("<")) > 0) {
						end = k;
						after = after.substring(0, end);

					}

					anchorText = before + " -mikelinkstartmike-" + element.getContent() + "-mikelinkendmike-" + after;

					l.add(new LinksModel(sourceUrl, tmp, anchorText, fp.getPageScore()));
				} catch (Exception e) {
					e.printStackTrace();
					logger.error("error parsing url:\t" + sourceUrl);
					logger.error(end + "\t" + orgEnd + "\t" + page.length());
				}

			}

		} catch (Exception e) {
			e.printStackTrace();

		}
		anchorText = null;
		tmp = null;
		s.clear();
		s = null;
	}

	@SuppressWarnings("null")
	private void addFrameLink(PageObject fp, ArrayList<LinksModel> l, Source sc) {
		List<Element> s = sc.getAllElements(HTMLElementName.FRAME);
		Element element;
		String sourceUrl = fp.getUrl();
		try {
			String tmp;
			for (int i = 0; i < s.size(); i++) {
				element = s.get(i);
				if (element == null && element.getAttributeValue("src") == null)
					continue;

				tmp = s.get(i).getAttributeValue("src").toString();
				l.add(new LinksModel(sourceUrl, tmp, null, fp.getPageScore()));
			}

		} catch (Exception e) {
		}
		s.clear();
		s = null;
	}

	// /*
	public static boolean shouldFilter(String s) {

		// if(true)
		// return false;

		if (s == null)
			return true;

		s = s.toLowerCase();
		String host = HttpUtils.getHost(s);
		if (host == null)
			return true;

		if (blacklist.contains(host.toLowerCase().replace("www.", "")))
			return true;

		if (CrawlerConfig.getConfig().getPageModel().contains("tourism") || CrawlerConfig.getConfig().getPageModel().contains("diving")) {
			for (String h : ibooked) {

				if (host.replace("www.", "").toLowerCase().startsWith(h.replace("www.", "")))
					return true;

				if (host.replace("www.", "").toLowerCase().contains("." + h))
					return true;
			}
		}

		// if(s.contains("in-phuket-real"))
		// return true;

		// for(String h: blogs){
		// if (host.replace("www.", "").toLowerCase().contains("." + h))
		// return true;
		// }

		// if(s.contains("hb-nippon.com"))
		// return true;
		

		if(s.contains("javascript:"))
			return true;
		
		if (s.contains("http://www.nicovideo.jp"))
			return true;

		if (s.contains("mailto:") || s.contains("<") || s.contains(">") || s.contains("javascript:") || s.contains("(") || s.contains(")")) {
			return true;
		}

		if (!CrawlerConfig.getConfig().isAllowHttps() && s.startsWith("https://"))
			return true;

		if (s.toLowerCase().contains("hotel-travel-"))
			return true;

		if (s.toLowerCase().contains("hoteltravel."))
			return true;
		
		if (s.toLowerCase().contains("phi-phi.com"))
			return true;
		if (s.toLowerCase().contains("kohlanta-hotels.com"))
			return true;
		
		
		if (s.toLowerCase().contains("thaiwebsites.com"))
			return true;
		
		if (s.toLowerCase().contains("phuket-property-sales.com"))
			return true;
		

		if (s.toLowerCase().contains("in-phuket-realestate.com/"))
			return true;

		if (s.toLowerCase().contains(".booked.net"))
			return true;
		
		if (s.toLowerCase().contains("ibook."))
			return true;
		
		if (s.toLowerCase().contains("hostelbookers."))
			return true;
		
		if (s.toLowerCase().contains(".hotels.com"))
			return true;
		
		
		if (s.toLowerCase().contains("instantthailand."))
			return true;
		
		if (s.toLowerCase().contains("thailandinstant."))
			return true;
		if (s.toLowerCase().contains("hamazo.tv"))
			return true;
		

		if (s.toLowerCase().contains("instant-bookings."))
			return true;

		if (s.toLowerCase().contains("hotelatm"))
			return true;

		if (s.toLowerCase().contains("kohphanganbungalows"))
			return true;
		//
		if (s.toLowerCase().contains("koh-phangan.net"))
			return true;
		//
		if (s.toLowerCase().contains("kohphanganbungalows.de"))
			return true;

		if (s.toLowerCase().contains("kohphanganresort"))
			return true;

		if (s.toLowerCase().contains(".phanganbungalows."))
			return true;

		if (s.contains("bdstanlong.com"))
			return true;

		if (s.contains("tanlonghousing.com"))
			return true;

		if (s.contains("ciputravillas.com"))
			return true;

		if (s.contains("apartmentsvinhomescentralpark.com"))
			return true;
		if (s.contains("vietnam"))
			return true;
		if (s.contains("hanoi"))
			return true;

		if (s.contains(".com.vn"))
			return true;

		if (s.contains(".anantara."))
			return true;
		
		if (s.contains("giaphathousing"))
			return true;

		if (s.contains("@"))
			return true;

		if (s.contains("gulivers.com"))
			return true;

		if (s.contains("apartmentsvinhomes"))
			return true;
		if (s.contains("villasvinhome"))
			return true;

		if (s.contains("philippines"))
			return true;
		if (s.contains("propertyhub.com.sg"))
			return true;

		if (s.contains("asiarealestatedirectory"))
			return true;

		if (s.contains("tanlong"))
			return true;

		// if (CrawlerConfig.getConfig().getPageModel().contains("diving") &&
		// s.toLowerCase().contains("bungalows"))
		// return true;

		if (s.toLowerCase().contains("candicetripp"))
			return true;

		if (s.toLowerCase().contains("phanganinfo"))
			return true;

		if (s.toLowerCase().contains("phanganisland.com"))
			return true;

		if (s.toLowerCase().contains("www.ziggys-bar.com"))
			return true;

		if (CrawlerConfig.getConfig().getPageModel().contains("tourism") || CrawlerConfig.getConfig().getPageModel().contains("diving")) {
			if (s.contains("estate"))
				return true;

			if (s.contains("villasandhomes"))
				return true;
		}

		if (CrawlerConfig.getConfig().getPageModel().contains("diving")) {
			// if(s.contains("1stop"))
			// return true;

			if (s.contains("komodo"))
				return true;

			if (s.contains("cebufundivers.com"))
				return true;
		}

		if (CrawlerConfig.getConfig().getPageModel().contains("estate")) {
			if (s.contains("hotel"))
				return true;

			if (s.contains("resort"))
				return true;

			if (s.contains("miami"))
				return true;
		}

		if (s.contains("ieee.org"))
			return true;

		if (s.contains("acm.org"))
			return true;

		if (s.contains("pinterest"))
			return true;

		if (s.contains("springerlink.com"))
			return true;

		if (s.contains("elsevier.com"))
			return true;

		if (s.contains("http://archive.is/"))
			return true;

		if (s.contains("springer.com"))
			return true;

		if (s.contains("itunes"))
			return true;

		if (s.toLowerCase().contains(".jpg"))
			return true;

		if (s.toLowerCase().contains(".png"))
			return true;

		if (s.toLowerCase().contains(".gif"))
			return true;

		if (s.toLowerCase().contains("wordpress.org"))
			return true;

		if (s.toLowerCase().contains(".sawadee."))
			return true;

		if (s.toLowerCase().contains(".world-estate."))
			return true;

		if (s.toLowerCase().contains(".pdf"))
			return true;

		if (s.toLowerCase().contains(".wmv"))
			return true;

		if (s.toLowerCase().contains("twitter"))
			return true;

		if (s.toLowerCase().contains("twitter.com"))
			return true;

		if (s.toLowerCase().contains("google.com"))
			return true;

		if (s.toLowerCase().contains("google"))
			return true;

		if (s.toLowerCase().contains("www.amazon."))
			return true;

		if (s.toLowerCase().contains("linkpark.hu"))
			return true;

		if (s.toLowerCase().contains("2link.be"))
			return true;

		if (s.toLowerCase().contains("udclick"))
			return true;

		if (s.toLowerCase().contains("linksynergy.com"))
			return true;

		if (s.toLowerCase().contains("tradedoubler.com"))
			return true;

		if (s.toLowerCase().contains("yahoo.com"))
			return true;

		if (s.toLowerCase().contains("yahoo"))
			return true;

		if (s.toLowerCase().contains("youtube.com"))
			return true;

		if (s.toLowerCase().contains("youtube"))
			return true;

		if (s.toLowerCase().contains("flickr.com"))
			return true;

		if (s.toLowerCase().contains("flickr"))
			return true;

		if (s.toLowerCase().contains("ads."))
			return true;

		if (s.toLowerCase().contains("boracay"))
			return true;

		if (s.toLowerCase().contains("youtu.be"))
			return true;

		if (s.toLowerCase().contains("mondinion"))
			return true;

		if (s.toLowerCase().contains("about.com"))
			return true;

		if (s.toLowerCase().contains("doubleclick"))
			return true;

		if (s.toLowerCase().contains("wikipedia"))
			return true;

		if (s.toLowerCase().contains("wikimedia"))
			return true;

		if (s.toLowerCase().contains("kenya"))
			return true;

		if (s.toLowerCase().contains("africa"))
			return true;

		if (s.toLowerCase().contains("tourism-of-india"))
			return true;

		if (s.toLowerCase().contains("zambia"))
			return true;

		if (s.toLowerCase().contains("cyprus"))
			return true;

		if (s.toLowerCase().contains("bali."))
			return true;

		if (s.toLowerCase().contains(".org.uk"))
			return true;

		if (s.toLowerCase().contains("click"))
			return true;

		if (s.toLowerCase().contains("ad."))
			return true;

		if (s.toLowerCase().contains("clk."))
			return true;

		if (s.toLowerCase().contains("ads."))
			return true;

		if (s.toLowerCase().contains("travelfish"))
			return true;

		if (s.toLowerCase().contains("pattayahouseguide"))
			return true;

		if (s.toLowerCase().contains("pattayacondoguide"))
			return true;

		if (s.toLowerCase().contains("newpattayacondos"))
			return true;

		if (s.toLowerCase().contains("neothai"))
			return true;

		if (s.toLowerCase().contains("fotoalbum"))
			return true;

		// if (s.toLowerCase().contains("thaiproperty.net"))
		// return true;

		if (s.toLowerCase().contains("compasspattaya"))
			return true;

		if (s.toLowerCase().contains("directrooms"))
			return true;

		if (s.toLowerCase().contains("http://thai.news-agency.jp"))
			return true;

		if (s.toLowerCase().contains("hotelthailand.com"))
			return true;

		if (s.toLowerCase().contains("mangozeen"))
			return true;

		if (s.toLowerCase().contains("tumblr.com"))
			return true;
		if (s.toLowerCase().contains("oecd"))
			return true;

		if (s.toLowerCase().contains("costablancapropertyportal"))
			return true;
		if (s.toLowerCase().contains("phodir"))
			return true;

		if (s.toLowerCase().contains("bahrain"))
			return true;

		if (s.toLowerCase().contains("qatar"))
			return true;

		if (s.toLowerCase().contains("propertyworld"))
			return true;

		if (s.toLowerCase().contains("newcondosonline"))
			return true;

		if (s.toLowerCase().contains("bulgarian"))
			return true;

		if (s.toLowerCase().contains("argentina"))
			return true;

		if (s.toLowerCase().contains("tobalivillas"))
			return true;

		if (s.toLowerCase().contains("ongpohlin"))
			return true;

		if (s.toLowerCase().contains("pictaero"))
			return true;

		if (s.toLowerCase().contains("bali"))
			return true;

		if (s.toLowerCase().contains("leapfrog-properties.com"))
			return true;

		if (s.toLowerCase().contains("expointernationalrealestate"))
			return true;

		if (s.toLowerCase().contains("lettingcentre"))
			return true;

		if (s.toLowerCase().contains("pictaero"))
			return true;

		if (s.toLowerCase().contains("costablanca"))
			return true;

		if (s.toLowerCase().contains("fx-rate"))
			return true;

		if (s.toLowerCase().contains("caribbean"))
			return true;

		if (s.toLowerCase().contains("hongkonghomes"))
			return true;

		if (s.toLowerCase().contains("asiahomes"))
			return true;

		if (s.toLowerCase().contains("caribbean"))
			return true;

		if (s.toLowerCase().contains("dubai"))
			return true;

		if (s.toLowerCase().contains("www.adventvancouver.com"))
			return true;

		if (s.toLowerCase().contains("holidaycube"))
			return true;

		if (s.toLowerCase().contains("costablanca"))
			return true;

		if (s.toLowerCase().contains("apartments.com.ua"))
			return true;

		if (s.toLowerCase().contains("broll.co.za"))
			return true;

		if (s.toLowerCase().contains("cbre"))
			return true;

		if (s.toLowerCase().contains("europeanproperty"))
			return true;

		if (s.toLowerCase().contains("torontohomemarketers"))
			return true;

		if (s.toLowerCase().contains("ukrain"))
			return true;

		if (s.toLowerCase().contains("elysianfieldsproperties"))
			return true;

		if (s.toLowerCase().contains("huahinpictures"))
			return true;

		if (s.toLowerCase().contains("weebly"))
			return true;

		if (s.toLowerCase().contains("linkedin"))
			return true;

		if (s.toLowerCase().contains("utah"))
			return true;

		if (s.toLowerCase().contains("dubai"))
			return true;

		if (s.toLowerCase().contains("cyprus "))
			return true;

		if (s.toLowerCase().contains("qatar"))
			return true;

		if (s.toLowerCase().contains("croatia"))
			return true;

		if (s.toLowerCase().contains("lasvegas"))
			return true;

		if (s.toLowerCase().contains("bahrain"))
			return true;

		if (s.toLowerCase().contains("barbados"))
			return true;

		if (s.toLowerCase().contains("australia"))
			return true;

		if (s.toLowerCase().contains(".agoda."))
			return true;

		if (s.toLowerCase().contains(".blogspot."))
			return true;

		if (s.toLowerCase().contains("wordpress.com"))
			return true;

		if (s.toLowerCase().contains("r24.org"))
			return true;

		if (s.toLowerCase().contains(".r24."))
			return true;

		if (s.toLowerCase().contains("r24.asia"))
			return true;

		if (s.toLowerCase().contains("coastandcountryfrance"))
			return true;

		if (s.toLowerCase().contains("link.nl"))
			return true;

		if (s.toLowerCase().contains("links.nl"))
			return true;

		if (s.toLowerCase().contains("facebook.com"))
			return true;

		if (s.toLowerCase().contains("instagram"))
			return true;

		if (s.toLowerCase().contains("instagr.am"))
			return true;

		if (s.toLowerCase().contains("twitter.com"))
			return true;

		if (s.toLowerCase().contains(".foursquare."))
			return true;

		if (s.toLowerCase().contains("4sq.com"))
			return true;

		if (s.toLowerCase().contains("bangkokdir.com"))
			return true;
		
		if (s.toLowerCase().contains("chiangmaidir.com"))
			return true;
		
		if (s.toLowerCase().contains("hatyaidir.com"))
			return true;
		
		if (s.toLowerCase().contains("huahindir.com"))
			return true;
		
		if (s.toLowerCase().contains("kanchanaburidir.com"))
			return true;
		
		if (s.toLowerCase().contains("khonkendir.com"))
			return true;
		
		if (s.toLowerCase().contains("kohsamuidir.com"))
			return true;
		
		if (s.toLowerCase().contains("krabidir.com"))
			return true;
		
		if (s.toLowerCase().contains("pattayadir.com"))
			return true;
		
		if (s.toLowerCase().contains("phuketdir.com"))
			return true;
		
		if (s.toLowerCase().contains("thailanddir.com"))
			return true;
		

		if (s.toLowerCase().contains("famitsu.com"))
			return true;
		
		if (s.toLowerCase().contains("dengekionline.com"))
			return true;
		
		if (s.toLowerCase().contains("4gamer.net"))
			return true;
		
		if (s.toLowerCase().contains("rakuten."))
			return true;
		
		
		if (HttpUtils.isIp(s.toLowerCase()))
			return true;

		return false;
	}

	// */
}
