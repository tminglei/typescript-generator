package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsAssignmentExpression;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsCallExpression;
import cz.habarta.typescript.generator.emitter.TsExpressionStatement;
import cz.habarta.typescript.generator.emitter.TsHelper;
import cz.habarta.typescript.generator.emitter.TsIdentifierReference;
import cz.habarta.typescript.generator.emitter.TsMemberExpression;
import cz.habarta.typescript.generator.emitter.TsMethodModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsModifierFlags;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsReturnStatement;
import cz.habarta.typescript.generator.emitter.TsStatement;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.emitter.TsVariableDeclarationStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HashCodeExtension extends Extension {

    public static final String CFG_EXCLUDE_PROPERTIES = "excludeProperties";
    
    private List<Pattern> excludePatterns = List.of();

    public HashCodeExtension() {
    }

    public HashCodeExtension(List<String> excludePatterns) {
        this.excludePatterns = (excludePatterns != null ? excludePatterns : List.<String>of()).stream()
                .map(d -> Pattern.compile(convertWildcardToRegex(d.trim())))    // 将通配符模式转换为正则表达式
                .collect(Collectors.toList());
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.worksWithPackagesMappedToNamespaces = true;
        return features;
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_EXCLUDE_PROPERTIES)) {
            String excludePropertyConfig = configuration.get(CFG_EXCLUDE_PROPERTIES);
            if (excludePropertyConfig != null) {
                // 解析以逗号分隔的模式列表, 将通配符模式转换为正则表达式
                this.excludePatterns = Arrays.stream(excludePropertyConfig.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(HashCodeExtension::convertWildcardToRegex)
                        .map(Pattern::compile)
                        .collect(Collectors.toList());
            }
        }
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return List.of(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeSymbolResolution, new TsModelTransformer() {
            @Override
            public TsModel transformModel(Context context, TsModel model) {
                return createHashCodeMethods(context.getSymbolTable(), model);
            }
        }));
    }

    private TsModel createHashCodeMethods(SymbolTable symbolTable, TsModel tsModel) {
        // 添加 helper 函数到生成的代码中
        tsModel.getHelpers().add(TsHelper.loadFromResource("/helpers/hashCode.ts"));
        
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (bean.isClass() || bean.isDataClass()) {
                final List<TsMethodModel> methods = new ArrayList<>(bean.getMethods());
                
                // 创建 hashCode 方法
                final TsMethodModel hashCodeMethod = createHashCodeMethod(symbolTable, tsModel, bean);
                methods.add(hashCodeMethod);
                
                beans.add(bean.withMethods(methods));
            } else {
                beans.add(bean);
            }
        }
        return tsModel.withBeans(beans);
    }

    private TsMethodModel createHashCodeMethod(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean) {
        // 过滤掉被排除的属性
        List<TsPropertyModel> includedProperties = bean.getProperties().stream()
                .filter(prop -> this.excludePatterns.stream().noneMatch(p -> p.matcher(prop.getName()).matches()))
                .collect(Collectors.toList());

        // 创建方法体
        final List<TsStatement> body = new ArrayList<>();
        
        // 初始化哈希值
        body.add(new TsVariableDeclarationStatement(
                false, // const
                "hash", 
                TsType.Number,
                new TsCallExpression(
                        new TsIdentifierReference("__initializeHashCode"),
                        new TsStringLiteral(bean.getOrigin().getSimpleName())
                )
        ));
        
        // 对每个属性计算哈希值并组合
        for (TsPropertyModel property : includedProperties) {
            body.add(new TsExpressionStatement(
                    new TsAssignmentExpression(
                            new TsIdentifierReference("hash"),
                            new TsCallExpression(
                                    new TsIdentifierReference("combine"),
                                    new TsIdentifierReference("hash"),
                                    new TsCallExpression(
                                            new TsIdentifierReference("__getHashValue"),
                                            new TsMemberExpression(new TsIdentifierReference("this"), property.getName()),
                                            new TsStringLiteral(property.getName())
                                    )
                            )
                    )
            ));
        }

        // 返回最终的哈希值
        body.add(new TsReturnStatement(new TsIdentifierReference("hash")));

        return new TsMethodModel(
                "hashCode",
                TsModifierFlags.None,
                null, // typeParameters
                new ArrayList<>(), // parameters
                TsType.Number, // return type
                body,
                null
        );
    }

    /**
     * 将通配符模式转换为正则表达式
     */
    private static String convertWildcardToRegex(String wildcard) {
        StringBuilder sb = new StringBuilder();
        sb.append("^");
        for (char c : wildcard.toCharArray()) {
            switch (c) {
                case '*':
                    sb.append(".*");
                    break;
                case '?':
                    sb.append(".");
                    break;
                case '.':
                    sb.append("\\.");
                    break;
                case '^':
                case '$':
                case '+':
                case '[':
                case ']':
                case '(':
                case ')':
                case '{':
                case '}':
                case '|':
                    sb.append("\\" + c);
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        sb.append("$");
        return sb.toString();
    }
}
