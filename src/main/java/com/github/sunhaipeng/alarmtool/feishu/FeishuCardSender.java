package com.github.sunhaipeng.alarmtool.feishu;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.sunhaipeng.alarmtool.config.AlarmConfig;
import com.github.sunhaipeng.alarmtool.core.AlarmSender;
import com.github.sunhaipeng.alarmtool.feishu.model.CardMessage;
import com.lark.oapi.Client;
import com.lark.oapi.service.im.v1.model.CreateMessageReq;
import com.lark.oapi.service.im.v1.model.CreateMessageReqBody;
import com.lark.oapi.service.im.v1.model.CreateMessageResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 飞书卡片消息发送器，基于 Lark SDK 发送交互式卡片消息。
 * 卡片包含标题、异常类型、告警级别、堆栈信息（可折叠）、自定义消息、链路追踪等模块。
 *
 * @author sunhaipeng
 */
public class FeishuCardSender implements AlarmSender {

    private static final Logger logger = LoggerFactory.getLogger(FeishuCardSender.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Client client;
    private final String receiveIdType;
    private final String msgType;
    private final String receiveId;

    public FeishuCardSender(AlarmConfig config) {
        this.client = Client.newBuilder(config.getFeishuAppId(), config.getFeishuAppSecret()).build();
        this.receiveIdType = config.getFeishuReceiveIdType();
        this.msgType = config.getFeishuMsgType();
        this.receiveId = config.getFeishuReceiveId();
    }

    /** 发送卡片消息：获取模板 -> 动态添加模块 -> 占位符替换 -> 调用 SDK 发送 */
    @Override
    public void send(Map<String, String> values) throws Exception {
        CardMessage cardMessage = getCardMessageTemplate();

        if (values.get("stack_info") != null) {
            cardMessage.getCard().getBody().getElements().add(buildStackInfoColumnSet());
        }
        if (values.get("message") != null) {
            cardMessage.getCard().getBody().getElements().add(buildMessageColumnSet());
        }
        if (values.get("zipkin_url") != null) {
            cardMessage.getCard().getBody().getElements().add(buildZipkinUrlColumnSet());
        }

        String content = TemplateReplacer.replacePlaceholders(
                objectMapper.writeValueAsString(cardMessage), values);

        CreateMessageReq req = CreateMessageReq.newBuilder()
                .receiveIdType(receiveIdType)
                .createMessageReqBody(CreateMessageReqBody.newBuilder()
                        .receiveId(receiveId)
                        .msgType(msgType)
                        .content(content)
                        .build())
                .build();

        CreateMessageResp resp = client.im().v1().message().create(req);

        if (!resp.success()) {
            logger.error("Feishu card message send failed. code:{}, msg:{}, reqId:{}",
                    resp.getCode(), resp.getMsg(), resp.getRequestId());
            throw new RuntimeException("Feishu API error: code=" + resp.getCode() + ", msg=" + resp.getMsg());
        }
    }

    /** 返回卡片模板：蓝色标题栏 + 警铃图标 + 空 body */
    @Override
    public CardMessage getCardMessageTemplate() {
        CardMessage cardMessage = new CardMessage();

        CardMessage.Card card = new CardMessage.Card();

        CardMessage.Header header = new CardMessage.Header();
        header.setTitle(new CardMessage.Title("系统异常报警通知"));
        header.setSubtitle(new CardMessage.Title());
        header.setIcon(new CardMessage.Icon());
        header.getIcon().setImgKey("alarm_outlined");
        card.setHeader(header);

        CardMessage.Body body = new CardMessage.Body();
        card.setBody(body);

        cardMessage.setCard(card);
        return cardMessage;
    }

    /** 构建堆栈信息模块：可折叠面板，背景橙色，默认折叠 */
    private CardMessage.ColumnSet buildStackInfoColumnSet() {
        CardMessage.ColumnSet columnSet = new CardMessage.ColumnSet();

        CardMessage.Column column = new CardMessage.Column();
        column.setBackgroundStyle("orange-50");

        List<CardMessage.MarkdownElement> elements = new ArrayList<>();

        CardMessage.MarkdownElement titleElement = new CardMessage.MarkdownElement();
        titleElement.setContent("**<font color='orange'>【堆栈信息】</font>**");
        elements.add(titleElement);

        CardMessage.MarkdownElement collapsiblePanel = new CardMessage.MarkdownElement();
        collapsiblePanel.setTag("collapsible_panel");
        collapsiblePanel.setTextSize(null);
        collapsiblePanel.setExpanded(false);
        collapsiblePanel.setVerticalSpacing("8px");
        collapsiblePanel.setPadding("8px 8px 8px 8px");

        CardMessage.MarkdownElementHeader panelHeader = new CardMessage.MarkdownElementHeader();
        CardMessage.Title panelTitle = new CardMessage.Title();
        panelTitle.setTag("plain_text");
        panelTitle.setContent("堆栈信息");
        panelHeader.setTitle(panelTitle);
        panelHeader.setVerticalAlign("center");
        panelHeader.setIconExpandedAngle(-100);
        panelHeader.setIconPosition("right");

        CardMessage.MarkdownElementIcon panelIcon = new CardMessage.MarkdownElementIcon();
        panelIcon.setTag("standard_icon");
        panelIcon.setToken("down-small-ccm_outlined");
        panelIcon.setSize("");
        panelIcon.setColor("16px 16px");
        panelHeader.setIcon(panelIcon);

        collapsiblePanel.setHeader(panelHeader);

        CardMessage.MarkdownElementBorder border = new CardMessage.MarkdownElementBorder();
        border.setColor("grey");
        border.setCornerRadius("5px");
        collapsiblePanel.setBorder(border);

        List<CardMessage.MarkdownElementColumn> panelElements = new ArrayList<>();
        CardMessage.MarkdownElementColumn columnContent = new CardMessage.MarkdownElementColumn();
        columnContent.setTag("markdown");
        columnContent.setContent("${stack_info}");
        panelElements.add(columnContent);
        collapsiblePanel.setElements(panelElements);

        elements.add(collapsiblePanel);

        CardMessage.MarkdownElement spacer = new CardMessage.MarkdownElement();
        spacer.setTextAlign("left");
        spacer.setContent("");
        elements.add(spacer);

        column.setElements(elements);
        columnSet.getColumns().add(column);

        return columnSet;
    }

    /** 构建自定义消息模块，背景红色 */
    private CardMessage.ColumnSet buildMessageColumnSet() {
        CardMessage.ColumnSet columnSet = new CardMessage.ColumnSet();

        CardMessage.Column column = new CardMessage.Column();
        column.setBackgroundStyle("carmine-50");

        List<CardMessage.MarkdownElement> elements = new ArrayList<>();

        CardMessage.MarkdownElement titleElement = new CardMessage.MarkdownElement();
        titleElement.setContent("**<font color='carmine'>【自定义消息】</font>**");
        elements.add(titleElement);

        CardMessage.MarkdownElement contentElement = new CardMessage.MarkdownElement();
        contentElement.setTextAlign("left");
        contentElement.setContent("${message}");
        elements.add(contentElement);

        column.setElements(elements);
        columnSet.getColumns().add(column);

        return columnSet;
    }

    /** 构建链路追踪模块，背景橙色 */
    private CardMessage.ColumnSet buildZipkinUrlColumnSet() {
        CardMessage.ColumnSet columnSet = new CardMessage.ColumnSet();

        CardMessage.Column column = new CardMessage.Column();
        column.setBackgroundStyle("orange-50");

        List<CardMessage.MarkdownElement> elements = new ArrayList<>();

        CardMessage.MarkdownElement titleElement = new CardMessage.MarkdownElement();
        titleElement.setContent("**<font color='orange'>【链路查看】</font>**");
        elements.add(titleElement);

        CardMessage.MarkdownElement contentElement = new CardMessage.MarkdownElement();
        contentElement.setTextAlign("left");
        contentElement.setContent("${zipkin_url}");
        elements.add(contentElement);

        column.setElements(elements);
        columnSet.getColumns().add(column);

        return columnSet;
    }
}