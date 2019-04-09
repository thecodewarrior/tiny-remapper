package net.fabricmc.tinyremapper.mapping;

import java.util.Objects;

public class ParameterTinyLine extends TinyLine {
    public final String owner, methodDescriptor, methodName;

    public ParameterTinyLine(String owner, String methodDescriptor, String methodName, String obf, String intermediary, String mapped) {
        super(obf, intermediary, mapped);
        this.owner = owner;
        this.methodDescriptor = methodDescriptor;
        this.methodName = methodName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterTinyLine)) return false;
        if (!super.equals(o)) return false;
        ParameterTinyLine that = (ParameterTinyLine) o;
        return Objects.equals(owner, that.owner) &&
                Objects.equals(methodDescriptor, that.methodDescriptor) &&
                Objects.equals(methodName, that.methodName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), owner, methodDescriptor, methodName);
    }

    @Override
    public String toString() {
        return new LineBuilder()
                .add("METHOD-PARAM", owner, methodDescriptor, methodName, obf, intermediary, mapped)
                .toString();
    }
}
