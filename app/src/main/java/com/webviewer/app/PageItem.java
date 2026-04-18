package com.webviewer.app;

public class PageItem {
    public static final String TYPE_URL = "url";
    public static final String TYPE_HTML = "html";

    public int id;
    public String name;
    public String type;   // "url" or "html"
    public String url;    // for TYPE_URL
    public String html;   // for TYPE_HTML
    public String category; // search, social, tools, media, html

    public PageItem(int id, String name, String type, String url, String html, String category) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.url = url;
        this.html = html;
        this.category = category;
    }

    public char getInitial() {
        return name != null && !name.isEmpty() ? Character.toUpperCase(name.charAt(0)) : 'W';
    }

    public int getIconColor() {
        switch (category != null ? category : "") {
            case "search": return 0xFF534AB7;
            case "social": return 0xFF185FA5;
            case "tools":  return 0xFF0F6E56;
            case "media":  return 0xFF993556;
            case "html":   return 0xFF854F0B;
            default:       return 0xFF3C3489;
        }
    }

    public String getDisplayUrl() {
        if (TYPE_URL.equals(type) && url != null) {
            return url.replaceFirst("^https?://", "").replaceFirst("/.*$", "");
        }
        return "HTML page";
    }
}
