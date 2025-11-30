package cn.qingweico.convert;

import cn.qingweico.model.HttpRequestEntity;
import cn.qingweico.model.XmlNode;
import cn.qingweico.network.NetworkUtils;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * @author zqw
 * @date 2025/10/15
 */
@Setter
@Getter
public class XmlConvert {

    private boolean prettyMode;

    public XmlConvert(boolean prettyMode) {
        this.prettyMode = prettyMode;
    }


    public String toXml(XmlNode param, String charset) {
        StringWriter sw = new StringWriter();
        XMLWriter writer = createXmlWriter(sw, charset);
        Element root = DocumentHelper.createElement(param.getName());
        try {
            appendChild(root, param);
            writer.startDocument();
            writer.write(root);
            writer.flush();
            writer.endDocument();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            try {
                sw.close();
            } catch (IOException ignored) {
            }
        }
    }

    public String toXml(XmlNode param) {
        return toXml(param, StandardCharsets.UTF_8.name());
    }

    public String toXml(Document doc, String charset) {
        StringWriter sw = new StringWriter();
        XMLWriter writer = createXmlWriter(sw, charset);
        try {
            writer.write(doc);
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
            try {
                sw.close();
            } catch (IOException ignored) {
            }
        }
    }

    private XMLWriter createXmlWriter(StringWriter sw, String charset) {
        if (prettyMode) {
            OutputFormat outputFormat = OutputFormat.createPrettyPrint();
            outputFormat.setEncoding(charset);
            return new XMLWriter(sw, outputFormat);
        } else {
            return new XMLWriter(sw);
        }
    }


    public String toXml(Document doc) {
        return toXml(doc, StandardCharsets.UTF_8.name());
    }


    private void appendChild(Element parent, XmlNode param) {
        for (XmlNode p : param.getAllChildren()) {
            if (p.isTextParam()) {
                Element e = parent.addElement(p.getName());
                if (p.getText() != null) {
                    e.setText(p.getText());
                }
            } else {
                Element e = parent.addElement(p.getName());
                appendChild(e, p);
            }
        }
    }

    public XmlNode fromXml(String xml) {
        SAXReader saxReader = new SAXReader(false);
        try (StringReader sr = new StringReader(xml)) {
            Document doc = saxReader.read(sr);
            Element root = doc.getRootElement();
            XmlNode param = new XmlNode(root.getName());
            readChild(root, param);
            return param;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public String format(String xml) {
        SAXReader saxReader = new SAXReader(false);
        try (StringReader sr = new StringReader(xml)) {
            Document doc = saxReader.read(sr);
            return toXml(doc);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void readChild(Element ele, XmlNode container) {
        Iterator<Element> it = ele.elementIterator();
        while (it.hasNext()) {
            Element element = it.next();
            XmlNode p = container.appendChild(element.getName());
            if (!element.elements().isEmpty()) {
                readChild(element, p);
            } else {
                String txt = element.getText();
                if (txt != null) {
                    p.setText(txt);
                }
            }
        }
    }

    public XmlNode fromMap(Map<String, Object> map, String rootName) {
        return XmlNode.fromMap(map, rootName);
    }


    public Map<String, Object> toMap(XmlNode param) {
        return param.toMap();
    }

    public static void main(String[] args) throws IOException {
        HttpRequestEntity httpRequestEntity = HttpRequestEntity.builder()
                .requestUrl("https://www.google.com/sitemap.xml")
                .proxyHost("127.0.0.1")
                .proxyPort(10808)
                .build();
        InputStream inputStream = NetworkUtils.getInputStreamByUrl(httpRequestEntity);
        String xmlSource = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        XmlConvert xmlConvert = new XmlConvert(true);
        XmlNode xmlNode = xmlConvert.fromXml(xmlSource);
        Map<String, Object> xmlMap = xmlConvert.toMap(xmlNode);
        System.out.println("Xml -> XmlNode -> Map");
        System.out.println(StringConvert.prettyJson(xmlMap));
        System.out.println("Map -> XmlNode -> Xml");
        System.out.println(xmlConvert.toXml(xmlConvert.fromMap(xmlMap, xmlNode.getName())));
    }
}
