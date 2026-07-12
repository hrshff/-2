package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;
import com.github.catvod.bean.Vod;
import com.github.catvod.bean.Result;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 月光影视 Spider
 * 网站: https://www.shipian8.com
 * 模板: MacCMS V10 (stui模板)
 * 适用于 FongMi CatVodSpider 框架
 */
public class YueGuang extends Spider {

    private static final String siteUrl = "https://www.shipian8.com";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.put("Referer", siteUrl + "/");
        return headers;
    }

    /**
     * 替代 Misc.find 的方法
     * 从 text 中提取 start 和 end 之间的字符串
     */
    private String find(String text, String start, String end) {
        if (TextUtils.isEmpty(text)) return "";
        int startIndex = text.indexOf(start);
        if (startIndex == -1) return "";
        startIndex += start.length();
        int endIndex = text.indexOf(end, startIndex);
        if (endIndex == -1) return "";
        return text.substring(startIndex, endIndex);
    }

    @Override
    public void init(Context context) throws Exception {
        super.init(context);
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        String html = OkHttp.string(siteUrl + "/", getHeaders());
        Document doc = Jsoup.parse(html);

        Elements carousel = doc.select(".carousel .col-xs-1 a");
        for (Element item : carousel) {
            String title = item.attr("title");
            String href = item.attr("href");
            String pic = "";
            String style = item.attr("style");
            if (style.contains("url(")) {
                pic = find(style, "url(", ")");
                if (pic.startsWith("//")) pic = "https:" + pic;
            }
            if (!TextUtils.isEmpty(title)) {
                Vod vod = new Vod();
                vod.setVodId(href);
                vod.setVodName(title);
                vod.setVodPic(pic);
                vod.setVodRemarks("推荐");
                list.add(vod);
            }
        }

        Elements items = doc.select(".stui-vodlist > li");
        for (Element item : items) {
            Element thumb = item.selectFirst(".stui-vodlist__thumb");
            if (thumb != null) {
                String title = thumb.attr("title");
                String href = thumb.attr("href");
                String pic = thumb.attr("data-original");
                String status = "";
                Element picText = thumb.selectFirst(".pic-text");
                if (picText != null) status = picText.text().trim();

                if (!TextUtils.isEmpty(title)) {
                    Vod vod = new Vod();
                    vod.setVodId(href);
                    vod.setVodName(title);
                    vod.setVodPic(pic);
                    vod.setVodRemarks(status);
                    list.add(vod);
                }
            }
        }
        return Result.string(list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        List<Vod> list = new ArrayList<>();
        String url;
        if ("1".equals(pg) && (extend == null || extend.isEmpty())) {
            url = siteUrl + "/zwhstp/" + tid + ".html";
        } else {
            url = siteUrl + "/zwhssw/" + tid + "-----------" + pg + ".html";
        }

        String html = OkHttp.string(url, getHeaders());
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".stui-vodlist > li");

        for (Element item : items) {
            Element thumb = item.selectFirst(".stui-vodlist__thumb");
            if (thumb != null) {
                String title = thumb.attr("title");
                String href = thumb.attr("href");
                String pic = thumb.attr("data-original");
                String status = "";
                Element picText = thumb.selectFirst(".pic-text");
                if (picText != null) status = picText.text().trim();

                if (!TextUtils.isEmpty(title)) {
                    Vod vod = new Vod();
                    vod.setVodId(href);
                    vod.setVodName(title);
                    vod.setVodPic(pic);
                    vod.setVodRemarks(status);
                    list.add(vod);
                }
            }
        }
        return Result.string(list);
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        if (ids == null || ids.isEmpty()) return "";
        String id = ids.get(0);
        if (!id.startsWith("http")) id = siteUrl + id;

        String html = OkHttp.string(id, getHeaders());
        Document doc = Jsoup.parse(html);
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));

        Element titleElem = doc.selectFirst("h1.title");
        if (titleElem != null) vod.setVodName(titleElem.text().trim());

        Element picElem = doc.selectFirst(".stui-content__thumb img");
        if (picElem != null) {
            String pic = picElem.attr("data-original");
            if (TextUtils.isEmpty(pic)) pic = picElem.attr("src");
            vod.setVodPic(pic);
        }

        Elements infoItems = doc.select(".stui-content__detail p.data");
        for (Element item : infoItems) {
            String text = item.text();
            if (text.contains("主演")) {
                vod.setVodActor(text.replace("主演：", "").trim());
            } else if (text.contains("导演")) {
                vod.setVodDirector(text.replace("导演：", "").trim());
            } else if (text.contains("类型")) {
                String typeName = text.replace("类型：", "").trim();
                if (typeName.contains(" ")) typeName = typeName.split(" ")[0];
                vod.setTypeName(typeName);
            } else if (text.contains("地区")) {
                String area = text.replace("地区：", "").trim();
                if (area.contains(" ")) area = area.split(" ")[0];
                vod.setVodArea(area);
            } else if (text.contains("年份")) {
                vod.setVodYear(text.replace("年份：", "").trim());
            }
        }

        Element descElem = doc.selectFirst(".detail-sketch");
        if (descElem != null) vod.setVodContent(descElem.text().trim());

        List<String> playFroms = new ArrayList<>();
        List<String> playUrls = new ArrayList<>();
        Elements playlists = doc.select(".playlist");

        for (Element playlist : playlists) {
            Element titleHead = playlist.selectFirst(".stui-pannel__head h3.title");
            String tabName = titleHead != null ? titleHead.text().trim() : "默认线路";
            playFroms.add(tabName);

            Elements playItems = playlist.select(".stui-content__playlist li a");
            List<String> urls = new ArrayList<>();
            for (Element play : playItems) {
                String name = play.text().trim();
                String href = play.attr("href");
                if (!href.startsWith("http")) href = siteUrl + href;
                urls.add(name + "$" + href);
            }
            playUrls.add(String.join("#", urls));
        }

        vod.setVodPlayFrom(String.join("$$$", playFroms));
        vod.setVodPlayUrl(String.join("$$$", playUrls));
        return Result.string(vod);
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String html = OkHttp.string(id, getHeaders());
        String playerJson = find(html, "var player_aaaa=", "</script>");

        if (!TextUtils.isEmpty(playerJson)) {
            JSONObject player = new JSONObject(playerJson);
            String url = player.optString("url", "");
            int encrypt = player.optInt("encrypt", 0);

            if (encrypt == 1) {
                url = java.net.URLDecoder.decode(url, "UTF-8");
            } else if (encrypt == 2) {
                url = java.net.URLDecoder.decode(
                    new String(android.util.Base64.decode(url, android.util.Base64.DEFAULT)),
                    "UTF-8");
            }
            return Result.get().url(url).header(getHeaders()).string();
        }
        return Result.get().url(id).header(getHeaders()).string();
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return searchContent(key, quick, "1");
    }

    @Override
    public String searchContent(String key, boolean quick, String pg) throws Exception {
        List<Vod> list = new ArrayList<>();
        String encodedKey = URLEncoder.encode(key, "UTF-8");
        String url;
        if ("1".equals(pg)) {
            url = siteUrl + "/zwhssc/" + encodedKey + "-------------.html";
        } else {
            url = siteUrl + "/zwhssc/" + encodedKey + "----------" + pg + "---.html";
        }

        String html = OkHttp.string(url, getHeaders());
        Document doc = Jsoup.parse(html);
        Elements items = doc.select(".stui-vodlist__media > li");

        for (Element item : items) {
            Element thumb = item.selectFirst(".v-thumb");
            Element titleElem = item.selectFirst("h3.title a");

            if (thumb != null) {
                String title = thumb.attr("title");
                String href = thumb.attr("href");
                String pic = thumb.attr("data-original");

                if (TextUtils.isEmpty(title) && titleElem != null) {
                    title = titleElem.text().trim();
                }

                String status = "";
                Element picText = thumb.selectFirst(".pic-text");
                if (picText != null) status = picText.text().trim();

                if (!TextUtils.isEmpty(title)) {
                    Vod vod = new Vod();
                    vod.setVodId(href);
                    vod.setVodName(title);
                    vod.setVodPic(pic);
                    vod.setVodRemarks(status);
                    list.add(vod);
                }
            }
        }
        return Result.string(list);
    }
}
