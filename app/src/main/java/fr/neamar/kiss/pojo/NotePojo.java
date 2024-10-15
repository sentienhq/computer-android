package fr.neamar.kiss.pojo;

import fr.neamar.kiss.normalizer.StringNormalizer;

public class NotePojo extends Pojo {
    public static final String SCHEME = "note://";
    public NotePojoType type;
    public String parentId;
    public String[] childIds;
    public String content;
    public String contentReply;
    public long timestamp;
    public String[] tags;

    public NotePojo(String id, NotePojoType type, String parentId, String[] childIds, String content, String contentReply, long timestamp, String[] tags) {
        super(id);
        this.type = type;
        this.parentId = parentId;
        this.childIds = childIds;
        this.content = content;
        this.contentReply = contentReply;
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
