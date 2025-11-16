package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import java.util.List;
import java.util.Objects;

public class TsObserveExpression extends TsExpression {
    private final List<TsPropertyModel> props;

    public TsObserveExpression(List<TsPropertyModel> props) {
        this.props = Objects.requireNonNull(props);
    }

    public List<TsPropertyModel> getProps() {
        return props;
    }

    @Override
    public String format(Settings settings) {
        StringBuilder builder = new StringBuilder();
        builder.append("makeObservable(this, {").append(settings.newline);
        for (TsPropertyModel prop : props) {
            builder.append(settings.indentString)
                    .append(prop.getName()).append(": observable,")
                    .append(settings.newline)
                ;
        }
        builder.append("})");
        return builder.toString();
    }
}
