package cn.qingweico.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author zqw
 * @date 2025/7/25
 */
@Data
public class Poem {
    private String quotes;
    private String title;
    private String dynasty;
    private String author;
    private String content;
    private String translate;
    private String preface;
    private String reviews;
    private List<String> tags;
    private Date createTime;
    private Date updateTime;
}
