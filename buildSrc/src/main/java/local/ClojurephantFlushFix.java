package local;

import dev.clojurephant.plugin.clojure.tasks.ClojureCheck;
import dev.clojurephant.plugin.clojure.tasks.ClojureCheckFlushExecutor;
import org.gradle.api.Project;

/** Wires {@link ClojureCheckFlushExecutor} into clojurephant {@link ClojureCheck} tasks. */
public final class ClojurephantFlushFix {

    private ClojurephantFlushFix() {}

    public static void configure(Project project) {
        project.getPlugins()
                .withId(
                        "dev.clojurephant.clojure",
                        p -> project.afterEvaluate(pr -> pr.getTasks()
                                .withType(ClojureCheck.class)
                                .configureEach(ClojureCheckFlushExecutor::replaceTaskActions)));
    }
}
