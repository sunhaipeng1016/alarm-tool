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
 * 卡片布局：固定字段 2 列 grid + 通栏，可选条件展示堆栈/自定义/链路模块。
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

    @Override
    public void send(Map<String, String> values) throws Exception {
        CardMessage cardMessage = getCardMessageTemplate();

        List<Object> elements = cardMessage.getCard().getBody().getElements();

        elements.add(buildTwoColumnSet(
                buildInfoTile("异常类型", "${exception_type}", "purple"),
                buildInfoTile("报警级别", "${alarm_level}", "carmine")));

        elements.add(buildDivider());

        elements.add(buildTwoColumnSet(
                buildInfoTile("发生时间", "${occurrence_time}", "orange"),
                buildInfoTile("主机名称", "${host_name}", "carmine")));

        elements.add(buildDivider());

        elements.add(buildSingleColumnSet(
                buildInfoTile("错误信息", "${error_info}", "carmine")));

        if (values.get("stack_info") != null) {
            elements.add(buildDivider());
            elements.add(buildStackInfoColumnSet());
        }
        if (values.get("message") != null) {
            elements.add(buildDivider());
            elements.add(buildSingleColumnSet(
                    buildInfoTile("自定义消息", "${message}", "carmine")));
        }
        if (values.get("zipkin_url") != null) {
            elements.add(buildDivider());
            elements.add(buildSingleColumnSet(
                    buildInfoTile("链路查看", "${zipkin_url}", "orange")));
        }

        String content = TemplateReplacer.replacePlaceholders(
                objectMapper.writeValueAsString(cardMessage.getCard()), values);

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

    @Override
    public CardMessage getCardMessageTemplate() {
        CardMessage cardMessage = new CardMessage();

        CardMessage.Card card = new CardMessage.Card();

        CardMessage.Header header = new CardMessage.Header();
        header.setTemplate("red");
        header.setTitle(new CardMessage.Title("系统异常报警通知"));
        header.setIcon(new CardMessage.Icon());
        header.getIcon().setToken("alarm_outlined");
        card.setHeader(header);

        card.setBody(new CardMessage.Body());

        cardMessage.setCard(card);
        return cardMessage;
    }

    private CardMessage.Column buildInfoTile(String title, String value, String titleColor) {
        CardMessage.Column column = new CardMessage.Column();

        CardMessage.MarkdownElement titleElement = new CardMessage.MarkdownElement();
        titleElement.setContent("**<font color='" + titleColor + "'>【" + title + "】</font>**");

        CardMessage.MarkdownElement valueElement = new CardMessage.MarkdownElement();
        valueElement.setContent(value);

        column.getElements().add(titleElement);
        column.getElements().add(valueElement);
        return column;
    }

    private CardMessage.MarkdownElement buildDivider() {
        CardMessage.MarkdownElement divider = new CardMessage.MarkdownElement();
        divider.setContent("<hr>");
        return divider;
    }

    private CardMessage.ColumnSet buildTwoColumnSet(CardMessage.Column left, CardMessage.Column right) {
        CardMessage.ColumnSet cs = new CardMessage.ColumnSet();
        cs.getColumns().add(left);
        cs.getColumns().add(right);
        return cs;
    }

    private CardMessage.ColumnSet buildSingleColumnSet(CardMessage.Column col) {
        CardMessage.ColumnSet cs = new CardMessage.ColumnSet();
        cs.getColumns().add(col);
        return cs;
    }

    private CardMessage.ColumnSet buildStackInfoColumnSet() {
        CardMessage.Column column = new CardMessage.Column();

        CardMessage.MarkdownElement titleElement = new CardMessage.MarkdownElement();
        titleElement.setContent("**<font color='orange'>【堆栈信息】</font>**");
        column.getElements().add(titleElement);

        CardMessage.MarkdownElement collapsiblePanel = new CardMessage.MarkdownElement();
        collapsiblePanel.setTag("collapsible_panel");
        collapsiblePanel.setExpanded(false);

        CardMessage.MarkdownElementHeader panelHeader = new CardMessage.MarkdownElementHeader();
        CardMessage.Title panelTitle = new CardMessage.Title();
        panelTitle.setTag("plain_text");
        panelTitle.setContent("堆栈信息");
        panelHeader.setTitle(panelTitle);
        panelHeader.setVerticalAlign("center");
        panelHeader.setIconExpandedAngle(-180);
        panelHeader.setIconPosition("right");

        CardMessage.MarkdownElementIcon panelIcon = new CardMessage.MarkdownElementIcon();
        panelIcon.setTag("standard_icon");
        panelIcon.setToken("down-small-ccm_outlined");
        panelIcon.setSize("16px 16px");
        panelHeader.setIcon(panelIcon);
        collapsiblePanel.setHeader(panelHeader);

        List<CardMessage.MarkdownElementColumn> panelElements = new ArrayList<>();
        CardMessage.MarkdownElementColumn colContent = new CardMessage.MarkdownElementColumn();
        colContent.setTag("markdown");
        colContent.setContent("${stack_info}");
        panelElements.add(colContent);
        collapsiblePanel.setElements(panelElements);

        column.getElements().add(collapsiblePanel);

        CardMessage.ColumnSet cs = new CardMessage.ColumnSet();
        cs.getColumns().add(column);
        return cs;
    }
}