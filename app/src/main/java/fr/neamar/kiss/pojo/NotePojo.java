package fr.neamar.kiss.pojo;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class NotePojo extends Pojo {
    public static final String SCHEME = "shortcut://";
    public String content;
    public String[] tags;
    public long timestamp;

    public NotePojo(String id, String content, long timestamp, String[] tags) {
        super(id);
        this.content = content;
        this.normalizedName = StringNormalizer.normalizeWithResult(content, false);
        this.timestamp = timestamp;
        this.tags = tags;
    }

//    public NotePojo(String id, String content, long timestamp) {
//        super(id);
//        this.content = content;
//        this.timestamp = timestamp;
//    }

    public String getContent() {
        return content;
    }


}
