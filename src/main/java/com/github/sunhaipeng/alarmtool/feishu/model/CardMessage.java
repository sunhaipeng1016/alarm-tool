package com.github.sunhaipeng.alarmtool.feishu.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * 飞书卡片消息实体类，对应飞书交互式卡片 JSON 结构。
 * 所有内部类均使用 Jackson 注解序列化，null 字段自动忽略。
 *
 * @author sunhaipeng
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CardMessage {

    @JsonProperty("msg_type")
    private String msgType = "interactive";

    private Card card = new Card();

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    /** 卡片容器 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Card {

        private Header header;
        private Body body;

        public Header getHeader() {
            return header;
        }

        public void setHeader(Header header) {
            this.header = header;
        }

        public Body getBody() {
            return body;
        }

        public void setBody(Body body) {
            this.body = body;
        }
    }

    /** 卡片头部：标题、副标题、图标、颜色主题 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Header {

        private String template = "blue";
        private Title title;
        private Title subtitle;
        private Icon icon;

        public String getTemplate() {
            return template;
        }

        public void setTemplate(String template) {
            this.template = template;
        }

        public Title getTitle() {
            return title;
        }

        public void setTitle(Title title) {
            this.title = title;
        }

        public Title getSubtitle() {
            return subtitle;
        }

        public void setSubtitle(Title subtitle) {
            this.subtitle = subtitle;
        }

        public Icon getIcon() {
            return icon;
        }

        public void setIcon(Icon icon) {
            this.icon = icon;
        }
    }

    /** 纯文本标题 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Title {

        private String tag = "plain_text";
        private String content;

        public Title() {
        }

        public Title(String content) {
            this.content = content;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    /** 飞书内置图标，如 alarm_outlined */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Icon {

        @JsonProperty("img_key")
        private String imgKey;

        public String getImgKey() {
            return imgKey;
        }

        public void setImgKey(String imgKey) {
            this.imgKey = imgKey;
        }
    }

    /** 卡片主体，包含多个元素 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Body {

        private List<Object> elements = new ArrayList<>();

        public List<Object> getElements() {
            return elements;
        }

        public void setElements(List<Object> elements) {
            this.elements = elements;
        }
    }

    /** 多列布局 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ColumnSet {

        private String tag = "column_set";
        private List<Column> columns = new ArrayList<>();

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public List<Column> getColumns() {
            return columns;
        }

        public void setColumns(List<Column> columns) {
            this.columns = columns;
        }
    }

    /** 列组件，可设置背景色 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Column {

        @JsonProperty("background_style")
        private String backgroundStyle;

        private List<MarkdownElement> elements = new ArrayList<>();

        public String getBackgroundStyle() {
            return backgroundStyle;
        }

        public void setBackgroundStyle(String backgroundStyle) {
            this.backgroundStyle = backgroundStyle;
        }

        public List<MarkdownElement> getElements() {
            return elements;
        }

        public void setElements(List<MarkdownElement> elements) {
            this.elements = elements;
        }
    }

    /** Markdown 元素，支持普通文本和可折叠面板 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkdownElement {

        private String tag = "markdown";
        private String content;

        @JsonProperty("text_align")
        private String textAlign;

        @JsonProperty("text_size")
        private String textSize;

        private Boolean expanded;

        @JsonProperty("vertical_spacing")
        private String verticalSpacing;

        private String padding;

        private MarkdownElementHeader header;

        private MarkdownElementBorder border;

        private List<MarkdownElementColumn> elements;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTextAlign() {
            return textAlign;
        }

        public void setTextAlign(String textAlign) {
            this.textAlign = textAlign;
        }

        public String getTextSize() {
            return textSize;
        }

        public void setTextSize(String textSize) {
            this.textSize = textSize;
        }

        public Boolean getExpanded() {
            return expanded;
        }

        public void setExpanded(Boolean expanded) {
            this.expanded = expanded;
        }

        public String getVerticalSpacing() {
            return verticalSpacing;
        }

        public void setVerticalSpacing(String verticalSpacing) {
            this.verticalSpacing = verticalSpacing;
        }

        public String getPadding() {
            return padding;
        }

        public void setPadding(String padding) {
            this.padding = padding;
        }

        public MarkdownElementHeader getHeader() {
            return header;
        }

        public void setHeader(MarkdownElementHeader header) {
            this.header = header;
        }

        public MarkdownElementBorder getBorder() {
            return border;
        }

        public void setBorder(MarkdownElementBorder border) {
            this.border = border;
        }

        public List<MarkdownElementColumn> getElements() {
            return elements;
        }

        public void setElements(List<MarkdownElementColumn> elements) {
            this.elements = elements;
        }
    }

    /** 可折叠面板头部 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkdownElementHeader {

        private Title title;

        @JsonProperty("vertical_align")
        private String verticalAlign;

        @JsonProperty("icon_expanded_angle")
        private Integer iconExpandedAngle;

        @JsonProperty("icon_position")
        private String iconPosition;

        private MarkdownElementIcon icon;

        public Title getTitle() {
            return title;
        }

        public void setTitle(Title title) {
            this.title = title;
        }

        public String getVerticalAlign() {
            return verticalAlign;
        }

        public void setVerticalAlign(String verticalAlign) {
            this.verticalAlign = verticalAlign;
        }

        public Integer getIconExpandedAngle() {
            return iconExpandedAngle;
        }

        public void setIconExpandedAngle(Integer iconExpandedAngle) {
            this.iconExpandedAngle = iconExpandedAngle;
        }

        public String getIconPosition() {
            return iconPosition;
        }

        public void setIconPosition(String iconPosition) {
            this.iconPosition = iconPosition;
        }

        public MarkdownElementIcon getIcon() {
            return icon;
        }

        public void setIcon(MarkdownElementIcon icon) {
            this.icon = icon;
        }
    }

    /** 标准图标 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkdownElementIcon {

        private String tag = "standard_icon";
        private String token;
        private String size;
        private String color;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSize() {
            return size;
        }

        public void setSize(String size) {
            this.size = size;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    /** 边框样式 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkdownElementBorder {

        private String color;

        @JsonProperty("corner_radius")
        private String cornerRadius;

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getCornerRadius() {
            return cornerRadius;
        }

        public void setCornerRadius(String cornerRadius) {
            this.cornerRadius = cornerRadius;
        }
    }

    /** 可折叠面板内的列内容 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MarkdownElementColumn {

        private String tag = "markdown";
        private String content;

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}