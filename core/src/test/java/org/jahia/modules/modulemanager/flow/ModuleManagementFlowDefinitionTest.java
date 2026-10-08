package org.jahia.modules.modulemanager.flow;

import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Every {@code flowHandler} expression of every flow definition names a public method of
 * {@link ModuleManagementFlowHandler} with the number of arguments the expression passes.
 * <p>
 * The flow definitions are evaluated at runtime, so the compiler does not read them. This class does, for
 * each {@code flow.xml} under the view directory of the component.
 */
public class ModuleManagementFlowDefinitionTest {

    private static final Path VIEWS = Paths.get("src/main/resources/jnt_serverSettingsManageModules/html");
    private static final Pattern REFERENCE = Pattern.compile("flowHandler\\.(\\w+)(\\()?");

    @Test
    public void everyFlowExpressionNamesAHandlerMethodWithItsArity() throws IOException {
        List<Path> flows;
        try (Stream<Path> files = Files.walk(VIEWS)) {
            flows = files.filter(f -> f.getFileName().toString().equals("flow.xml")).collect(Collectors.toList());
        }
        assertEquals("two flow definitions under " + VIEWS, 2, flows.size());

        List<String> problems = new ArrayList<>();
        int references = 0;
        for (Path flow : flows) {
            String text = new String(Files.readAllBytes(flow));
            Matcher reference = REFERENCE.matcher(text);
            while (reference.find()) {
                references++;
                String name = reference.group(1);
                int line = 1 + (int) text.substring(0, reference.start()).chars().filter(c -> c == '\n').count();
                String where = flow.getParent().getFileName() + ":" + line;
                if (reference.group(2) == null) {
                    if (!hasGetter(name)) {
                        problems.add(where + " reads the property " + name + " that the handler does not expose");
                    }
                } else {
                    int arity = countArguments(text, reference.end());
                    Set<Integer> arities = aritiesOf(name);
                    if (!arities.contains(arity)) {
                        problems.add(where + " calls " + name + " with " + arity + " argument(s), the handler takes " + arities);
                    }
                }
            }
        }
        assertTrue("fewer references than the two flows hold: " + references, references > 40);
        assertTrue(String.join("\n", problems), problems.isEmpty());
    }

    /** The number of top-level arguments between the opening parenthesis at {@code from} and its match. */
    private static int countArguments(String text, int from) {
        int depth = 1;
        int arguments = 0;
        boolean content = false;
        for (int i = from; depth > 0; i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 1) {
                arguments++;
            } else if (!Character.isWhitespace(c)) {
                content = true;
            }
        }
        return content ? arguments + 1 : 0;
    }

    private static Set<Integer> aritiesOf(String name) {
        return Stream.of(ModuleManagementFlowHandler.class.getMethods())
                .filter(m -> m.getName().equals(name))
                .map(Method::getParameterCount)
                .collect(Collectors.toSet());
    }

    private static boolean hasGetter(String property) {
        String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
        return Stream.of(ModuleManagementFlowHandler.class.getMethods())
                .anyMatch(m -> m.getParameterCount() == 0
                        && (m.getName().equals("get" + suffix) || m.getName().equals("is" + suffix)));
    }
}
