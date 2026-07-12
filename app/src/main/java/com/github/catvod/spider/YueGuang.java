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
 * 基于实际网页HTML结构分析
 */
public class YueGuang extends Spider {

    private static final String siteUrl = "https://www.shipian8.com";
    private static final String siteName = "月光影视";

    private HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
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

    /**
     * 首页推荐
     * 基于 shipian8.com 实际HTML结构
     */
    @Override
    public String homeContent(boolean filter) throws Exception {
        List<Vod> list = new ArrayList<>();
        String html = OkHttp.string(siteUrl + "/", getHeaders());
        Document doc = Jsoup.parse(html);

        // 1. 轮播图 .carousel .col-xs-1 a
        // 结构: <div class="carousel ..."> <div class="col-xs-1"> <a style="background: url(...)" title="...">
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

        // 2. 影片列表 .stui-vodlist > li
        // 结构: <ul class="stui-vodlist"> <li> <div class="stui-vodlist__box"> <a class="stui-vodlist__thumb" data-original="..." title="..." href="...">
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

    /**
     * 分类列表
     * URL格式基于实际分析:
     * - 第一页: /zwhstp/{cateId}.html
     * - 分页: /zwhssw/{cateId}-----------{page}.html
     */
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

    /**
     * 详情页
     * 基于实际HTML结构:
     * - 标题: h1.title
     * - 封面: .stui-content__thumb img
     * - 信息: .stui-content__detail p.data
     * - 简介: .detail-sketch
     * - 播放列表: .playlist .stui-content__playlist
     */
    @Override
    public String detailContent(List<String> ids) throws Exception {
        if (ids == null || ids.isEmpty()) return "";
        String id = ids.get(0);
        if (!id.startsWith("http")) {
            id = siteUrl + id;
        }

        String html = OkHttp.string(id, getHeaders());
        Document doc = Jsoup.parse(html);
        Vod vod = new Vod();
        vod.setVodId(ids.get(0));

        // 标题 h1.title
        Element titleElem = doc.selectFirst("h1.title");
        if (titleElem != null) {
            vod.setVodName(titleElem.text().trim());
        }

        // 封面 .stui-content__thumb img
        Element picElem = doc.selectFirst(".stui-content__thumb img");
        if (picElem != null) {
            String pic = picElem.attr("data-original");
            if (TextUtils.isEmpty(pic)) pic = picElem.attr("src");
            vod.setVodPic(pic);
        }

        // 信息 .stui-content__detail p.data
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

        // 简介 .detail-sketch
        Element descElem = doc.selectFirst(".detail-sketch");
        if (descElem != null) {
            vod.setVodContent(descElem.text().trim());
        }

        // 播放线路 .playlist
        // 结构: <div class="playlist"> <div class="stui-pannel__head"> <h3 class="title">线路名</h3> </div> <ul class="stui-content__playlist"> <li><a href="...">集数名</a></li> </ul> </div>
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
                if (!href.startsWith("http")) {
                    href = siteUrl + href;
                }
                urls.add(name + "$" + href);
            }
            playUrls.add(String.join("#", urls));
        }

        vod.setVodPlayFrom(String.join("$$$", playFroms));
        vod.setVodPlayUrl(String.join("$$$", playUrls));
        return Result.string(vod);
    }

    /**
     * 播放地址解析
     * 基于 player_aaaa 变量 (MacCMS标准播放器)
     */
    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        String html = OkHttp.string(id, getHeaders());
        String playerJson = find(html, "var player_aaaa=", "</script>");

        if (!TextUtils.isEmpty(playerJson)) {
            JSONObject player = new JSONObject(playerJson);
            String url = player.optString("url", "");
            int encrypt = player.optInt("encrypt", 0);

            // 解密处理
            if (encrypt == 1) {
                url = java.net.URLDecoder.decode(url, "UTF-8");
            } else if (encrypt == 2) {
                url = java.net.URLDecoder.decode(
                    new String(android.util.Base64.decode(url, android.util.Base64.DEFAULT)), 
                    "UTF-8"
                );
            }

            // TVBox标准返回格式
            return Result.get().url(url).header(getHeaders()).string();
        }

        // 兜底：返回原始URL
        return Result.get().url(id).header(getHeaders()).string();
    }

    /**
     * 搜索
     * URL格式基于实际分析:
     * - /zwhssc/{关键词}-------------.html
     * - 分页: /zwhssc/{关键词}----------{page}---.html
     */
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

        // 搜索页使用 .stui-vodlist__media > li 结构
        // 与普通列表不同，搜索页用 .v-thumb 而不是 .stui-vodlist__thumb
        Elements items = doc.select(".stui-vodlist__media > li");
        for (Element item : items) {
            Element thumb = item.selectFirst(".v-thumb");
            Element titleElem = item.selectFirst("h3.title a");

            if (thumb != null) {
                String title = thumb.attr("title");
                String href = thumb.attr("href");
                String pic = thumb.attr("data-original");

                // 如果thumb没有title,尝试从h3获取
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
