package net.fabricmc.tinyremapper.mapping;

import java.util.Arrays;
import java.util.Objects;

public class ClassTinyLine extends TinyLine {
    public final String comment;

    public ClassTinyLine(String obf, String intermediary, String mapped, String comment) {
        super(obf, intermediary, mapped);
        this.comment = comment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassTinyLine)) return false;
        if (!super.equals(o)) return false;
        ClassTinyLine that = (ClassTinyLine) o;
        return Objects.equals(comment, that.comment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), comment);
    }

    @Override
    public String toString() {
        return new LineBuilder()
                .add("CLASS", obf, intermediary, mapped, comment)
                .toString();
    }
}
