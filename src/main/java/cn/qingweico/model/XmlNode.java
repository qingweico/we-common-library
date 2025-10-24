package cn.qingweico.model;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jodd.util.StringUtil;
import lombok.Getter;
import org.springframework.util.Assert;

import java.lang.reflect.Type;
import java.util.*;

/**
 * @author zqw
 * @date 2025/10/15
 */
public class XmlNode {
    @Getter
    private String name;
    @Getter
    private String text;
    @Getter
    private XmlNode parent;
    private Map<String, List<XmlNode>> children;

    public XmlNode(String name) {
        Assert.hasLength(name, "XmlNode name must not be empty");
        this.name = name;
        children = new LinkedHashMap<>();
    }


    public XmlNode(String name, String text) {
        Assert.hasLength(name, "XmlNode name must not be empty");
        this.name = name;
        this.text = text;

    }


    public boolean isTextParam() {
        return children == null;
    }


    public XmlNode getRoot() {
        XmlNode parent = this.parent;
        if (parent != null) {
            return parent.getParent();
        } else {
            return this;
        }
    }


    public XmlNode setName(String name) {
        if (this.parent != null) {
            XmlNode parent = this.parent;
            parent.remove(this);
            this.name = name;
            parent.appendChild(this);
        } else {
            this.name = name;
        }
        return this;

    }


    public XmlNode setText(String text) {
        this.children = null;
        this.text = text;
        return this;
    }

    public boolean setTextIfAbsent(String text) {
        if (this.children != null && !this.children.isEmpty()
                || StringUtil.isNotEmpty(this.text)) {
            return false;
        }
        setText(text);
        return true;
    }


    public XmlNode getChild(String name) {
        Assert.hasLength(name, "XmlNode name must not be empty");
        if (children == null) {
            return new XmlNode(name, null);
        } else {
            List<XmlNode> list = children.get(name);
            if (list == null || list.isEmpty()) {
                return new XmlNode(name, null);
            } else {
                return list.get(0);
            }
        }
    }

    public String getChildText(String name) {
        return getChild(name).getText();
    }

    public XmlNode setChildText(String name, String text) {
        return getChildOrAppend(name).setText(text);
    }


    public XmlNode getChildOrAppend(String name) {
        XmlNode child = getChild(name);
        if (child.parent == null) {
            return appendChild(child);
        }
        return child;
    }


    public XmlNode[] getChildren(String name) {
        Assert.hasLength(name, "XmlNode name must not be empty");
        if (children == null) {
            return new XmlNode[0];
        } else {
            List<XmlNode> list = children.get(name);
            if (list == null) {
                return new XmlNode[0];
            } else {
                return list.toArray(new XmlNode[0]);
            }
        }
    }

    public int getChildrenCount(String name) {
        return getChildren(name).length;
    }


    public XmlNode[] getAllChildren() {
        List<XmlNode> list = new ArrayList<>();
        if (children != null) {
            for (List<XmlNode> l : children.values()) {
                list.addAll(l);
            }
        }
        return list.toArray(new XmlNode[0]);

    }


    public int getAllChildrenCount() {
        return getAllChildren().length;
    }

    public XmlNode appendTo(XmlNode parent) {
        return parent.appendChild(this);

    }


    public XmlNode appendChild(String name) {
        return appendChild(new XmlNode(name));
    }


    public XmlNode appendChild(String name, String text) {
        return appendChild(new XmlNode(name, text));
    }


    public XmlNode appendChildIfNotEmpty(String name, String text) {
        if (StringUtil.isNotEmpty(text)) {
            return appendChild(new XmlNode(name, text));
        } else {
            return null;
        }
    }

    public boolean appendChildTextIfAbsent(String name, String text) {
        return this.getChildOrAppend(name).setTextIfAbsent(text);
    }


    public XmlNode appendChild(XmlNode param) {
        if (children == null) {
            children = new LinkedHashMap<>();
        }
        List<XmlNode> list = children.computeIfAbsent(param.getName(), k -> new ArrayList<>());
        list.add(param);
        param.parent = this;
        return param;
    }


    public XmlNode remove() {
        if (this.getParent() != null) {
            this.getParent().remove(this);
        }
        return this;
    }


    public XmlNode removeFirst(String name) {
        if (children != null) {
            List<XmlNode> list = children.get(name);
            if (list != null && !list.isEmpty()) {
                XmlNode p = list.get(0);
                if (p.parent == this) {
                    p.parent = null;
                }

                list.remove(0);
                if (list.isEmpty()) {
                    children.remove(name);
                }
                return p;
            }
        }
        return new XmlNode(name);
    }


    public void remove(String name) {
        for (XmlNode p : getChildren(name)) {
            if (p.parent == this) {
                p.parent = null;
            }
        }
        if (children != null) {
            children.remove(name);
        }
    }


    public XmlNode remove(XmlNode param) {
        if (children != null) {
            String name = param.getName();
            List<XmlNode> list = children.get(param.getName());
            if (list != null && !list.isEmpty()) {
                Iterator<XmlNode> it = list.iterator();
                while (it.hasNext()) {
                    XmlNode xmlNode = it.next();
                    if (xmlNode == param) {
                        it.remove();
                        if (param.parent == this) {
                            param.parent = null;
                        }
                    }
                }
                if (list.isEmpty()) {
                    children.remove(name);
                }
            }
        }
        return param;
    }


    public void removeAll() {
        for (XmlNode p : getAllChildren()) {
            if (p.parent == this) {
                p.parent = null;
            }
        }
        if (children != null) {
            children.clear();
            children = null;
        }
    }

    public static XmlNode parseJson(String root, String json) {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {
        }.getType();
        return fromMap(gson.fromJson(json, type), root);
    }

    @SuppressWarnings("rawtypes,unchecked")
    private static void append(XmlNode node, Object data) {
        if (data instanceof Map) {
            appendMap(node, (Map) data);
        } else if (data instanceof Iterator) {
            appendIterator(node, (Iterator) data);
        } else if (data instanceof Iterable) {
            appendIterator(node, ((Iterable) data).iterator());
        } else {
            appendText(node, data.toString());
        }
    }

    @SuppressWarnings("rawtypes")
    private static void appendIterator(XmlNode node, Iterator data) {
        final XmlNode parentNode = node.getParent();
        boolean isFirst = true;
        Object eleData;
        while (data.hasNext()) {
            eleData = data.next();
            if (isFirst) {
                append(node, eleData);
                isFirst = false;
            } else {
                final XmlNode successor = new XmlNode(node.getName());
                parentNode.appendChild(successor);
                append(successor, eleData);
            }
        }
    }


    private static void appendText(XmlNode node, String text) {
        node.setText(text);
    }


    public static XmlNode fromMap(Map<String, Object> data, String rootName) {
        XmlNode root = new XmlNode(rootName);
        appendMap(root, data);
        return root;
    }


    private static void appendMap(XmlNode root, Map<String, Object> data) {
        if (data == null) {
            return;
        }
        data.forEach((key, value) -> {
            if (null != key) {
                final XmlNode child = root.appendChild(key);
                if (null != value) {
                    append(child, value);
                }
            }
        });

    }

    public Map<String, Object> toMap() {
        return transformSelf(this);
    }

    private Map<String, Object> transformSelf(XmlNode param) {
        return transformSelf(param, new HashMap<>());
    }


    private Map<String, Object> transformSelf(XmlNode param, Map<String, Object> result) {
        if (null == result) {
            result = new HashMap<>();
        }
        final XmlNode[] children = param.getAllChildren();
        XmlNode child;
        for (XmlNode xmlNode : children) {
            child = xmlNode;
            final Object value = result.get(child.getName());
            Object newValue;
            if (child.getAllChildren().length > 0) {
                final Map<String, Object> map = transformSelf(child);
                if (MapUtil.isNotEmpty(map)) {
                    newValue = map;
                } else {
                    newValue = child.getText();
                }
            } else {
                newValue = child.getText();
            }
            if (null != newValue) {
                if (null != value) {
                    if (value instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) value;
                        list.add(newValue);
                    } else {
                        result.put(child.getName(), CollUtil.newArrayList(value, newValue));
                    }
                } else {
                    result.put(child.getName(), newValue);
                }
            }
        }
        return result;
    }
}
