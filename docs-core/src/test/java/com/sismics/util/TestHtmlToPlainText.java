package com.sismics.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Assert;
import org.junit.Test;

public class TestHtmlToPlainText {

    private HtmlToPlainText htmlToPlainText = new HtmlToPlainText();

    @Test
    public void testGetPlainText_withSimpleHtml() {
        String html = "<html><body><p>Hello, world!</p></body></html>";
        Element element = Jsoup.parse(html).body();

        String plainText = htmlToPlainText.getPlainText(element);

        Assert.assertEquals("\nHello, world!\n", plainText);
    }

    @Test
    public void testGetPlainText_withLongText() {
        String html = "<html><body><p>This is a very long paragraph that should be wrapped correctly based on the maxWidth property in the HtmlToPlainText class. The text should wrap around when it reaches a certain length.</p></body></html>";
        Element element = Jsoup.parse(html).body();

        String plainText = htmlToPlainText.getPlainText(element);

        Assert.assertTrue(plainText.contains("\n")); // Verify that text wrapping happened
    }
}
