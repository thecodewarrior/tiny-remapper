package net.fabricmc.tinyremapper.mapping;

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class TinyLine {
    public final String obf, intermediary, mapped;

    public TinyLine(String obf, String intermediary, String mapped) {
        this.obf = obf;
        this.intermediary = intermediary;
        this.mapped = mapped;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TinyLine)) return false;
        TinyLine tinyLine = (TinyLine) o;
        return Objects.equals(obf, tinyLine.obf) &&
                Objects.equals(intermediary, tinyLine.intermediary) &&
                Objects.equals(mapped, tinyLine.mapped);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(obf);
        result = 31 * result + Objects.hash(intermediary);
        result = 31 * result + Objects.hash(mapped);
        return result;
    }

    private static Pattern escapeSequences = Pattern.compile("\\\\.");
    private static String unescape(char code) {
        switch(code) {
            case 't': return "\t";
            case 'n': return "\n";
            case 'r': return "\r";
            case '\\': return "\\";
        }
        throw new IllegalArgumentException("Unknown escape sequence \\" + code);
    }

    protected static String[] splitLine(String line) {
        String[] words = line.split("\t");
        for (int i = 0; i < words.length; i++) {
            Matcher matcher = escapeSequences.matcher(words[i]);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, unescape(matcher.group().charAt(1)));
            }
            matcher.appendTail(sb);
            words[i] = sb.toString();
        }
        return words;
    }

    protected static class LineBuilder {
        private StringBuilder builder = new StringBuilder();

        public LineBuilder add(String word) {
            builder.append("\t");
            if(word != null) {
                builder.append(word
                        .replace("\t", "\\t")
                        .replace("\n", "\\n")
                        .replace("\r", "\\r")
                        .replace("\\", "\\\\")
                );
            }
            return this;
        }

        public LineBuilder add(String... words) {
            for (String word : words) {
                add(word);
            }
            return this;
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
