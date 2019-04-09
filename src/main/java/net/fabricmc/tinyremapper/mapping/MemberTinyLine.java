package net.fabricmc.tinyremapper.mapping;

import java.util.Objects;

public class MemberTinyLine extends TinyLine {
    public final Type type;
    public final String owner, descriptor, comment;

    public MemberTinyLine(String owner, String descriptor, Type type, String obf, String intermediary, String mapped, String comment) {
        super(obf, intermediary, mapped);
        this.type = type;
        this.owner = owner;
        this.descriptor = descriptor;
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberTinyLine)) return false;
        if (!super.equals(o)) return false;
        MemberTinyLine that = (MemberTinyLine) o;
        return Objects.equals(owner, that.owner) &&
                Objects.equals(descriptor, that.descriptor) &&
                Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), owner, descriptor, comment);
    }

    @Override
    public String toString() {
        return new LineBuilder()
                .add(type.keyword, owner, descriptor, obf, intermediary, mapped, comment)
                .toString();
    }

    public enum Type {
        METHOD("METHOD"),
        FIELD("FIELD");

        final String keyword;

        Type(String keyword) {
            this.keyword = keyword;
        }
    }
}
