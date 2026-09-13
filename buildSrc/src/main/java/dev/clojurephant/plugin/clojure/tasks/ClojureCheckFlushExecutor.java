package dev.clojurephant.plugin.clojure.tasks;

import dev.clojurephant.plugin.common.internal.ClojureException;
import dev.clojurephant.plugin.common.internal.Edn;
import dev.clojurephant.plugin.common.internal.Prepl;
import dev.clojurephant.plugin.common.internal.PreplClient;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.logging.LogLevel;
import org.gradle.process.ExecOperations;
import us.bpsm.edn.Symbol;

/**
 * Replaces clojurephant {@link ClojureCheck}'s task action (flush, load-file, classpath order).
 *
 * <p>PREPL compiler lines must be drained <em>before</em> {@link PreplClient#close()} (close interrupts the
 * reader thread). Each namespace load uses {@code (binding [*warn-on-reflection* true] ...)} so the
 * compiler thread always sees the flag. Lines go to {@link System#out} (not raw {@code Logger.lifecycle})
 * because Gradle can treat arbitrary compiler text as a log format string and fail the build.
 *
 * <p>Gradle maps uncaptured task {@code System.out} to {@link LogLevel#QUIET}; at the default console
 * threshold ({@link LogLevel#LIFECYCLE}) those lines are invisible. This task therefore calls
 * {@code captureStandardOutput(LIFECYCLE)} while the PREPL client runs, and mirrors drained lines to
 * {@code build/reports/check-clojure-prepl.log}.
 */
public final class ClojureCheckFlushExecutor {

    /**
     * Path segment is {@code file:line:col}; use greedy path group so Windows {@code C:\...} / {@code D:}
     * does not break on the drive colon (non-greedy {@code .+?} would match only {@code C}).
     */
    private static final Pattern REFLECTION_WARNING =
            Pattern.compile("Reflection warning, (.*):(\\d+):(\\d+) - ");

    private ClojureCheckFlushExecutor() {}

    public static void replaceTaskActions(ClojureCheck task) {
        task.getActions().clear();
        task.doLast(new Action<Task>() {
            @Override
            public void execute(Task t) {
                check((ClojureCheck) t);
            }
        });
    }

    @SuppressWarnings("deprecation")
    private static ExecOperations execOperationsFor(ClojureCheck task) {
        ProjectInternal p = (ProjectInternal) task.getProject();
        return p.getServices().get(ExecOperations.class);
    }

    private static Path findSourceFileForNamespace(ClojureCheck task, String namespace) {
        String rel = namespace.replace('-', '_').replace('.', '/') + ".clj";
        Path suffix = Paths.get(rel);
        for (File f : task.getSource().getFiles()) {
            Path p = f.toPath();
            if (p.endsWith(suffix)) {
                return p.toAbsolutePath().normalize();
            }
        }
        String relc = namespace.replace('-', '_').replace('.', '/') + ".cljc";
        Path suffc = Paths.get(relc);
        for (File f : task.getSource().getFiles()) {
            Path p = f.toPath();
            if (p.endsWith(suffc)) {
                return p.toAbsolutePath().normalize();
            }
        }
        return null;
    }

    private static boolean warningRefersToProjectSource(ClojureCheck task, String warnPathGroup) {
        if ("NO_SOURCE_PATH".equals(warnPathGroup)) {
            return false;
        }
        Path warned = Paths.get(warnPathGroup).normalize();
        Path warnedAbs = warned.isAbsolute() ? warned.toAbsolutePath().normalize() : null;
        for (File source : task.getSource().getFiles()) {
            Path sp = source.toPath().toAbsolutePath().normalize();
            if (warnedAbs != null) {
                if (spEquals(sp, warnedAbs)) {
                    return true;
                }
            } else if (sp.endsWith(warned)) {
                return true;
            }
        }
        return false;
    }

    private static boolean spEquals(Path a, Path b) {
        try {
            return Files.isSameFile(a, b);
        } catch (IOException e) {
            return a.equals(b);
        }
    }

    private static FileCollection classpathForReflectiveCheck(ClojureCheck task) {
        Path javaMain = task.getProjectLayout()
                .getBuildDirectory()
                .dir("classes/java/main")
                .get()
                .getAsFile()
                .toPath()
                .toAbsolutePath()
                .normalize();
        Path clojureMain = task.getProjectLayout()
                .getBuildDirectory()
                .dir("clojure/main")
                .get()
                .getAsFile()
                .toPath()
                .toAbsolutePath()
                .normalize();
        List<File> head = new ArrayList<>();
        List<File> tail = new ArrayList<>();
        for (File f : task.getClasspath().getFiles()) {
            Path fp = f.toPath().toAbsolutePath().normalize();
            if (fp.startsWith(javaMain) || fp.startsWith(clojureMain)) {
                tail.add(f);
            } else {
                head.add(f);
            }
        }
        List<File> combined = new ArrayList<>(head.size() + tail.size());
        combined.addAll(head);
        combined.addAll(tail);
        return task.getProject().files(combined);
    }

    /** Drains queued PREPL :out / :err lines (non-blocking). */
    private static String escapeEdnString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void drainPreplOutput(
            ClojureCheck task,
            PreplClient client,
            boolean[] projectReflectionWarnings,
            BufferedWriter preplLog,
            long[] preplLineCount)
            throws IOException {
        for (String out : client.pollOutput()) {
            preplLineCount[0]++;
            if (preplLog != null) {
                preplLog.write(out);
                preplLog.newLine();
                preplLog.flush();
            }
            // Do not use Logger.lifecycle(out): Gradle may treat the compiler line as a format string
            // (e.g. braces) and throw. Raw stdout is captured by Gradle for the task.
            System.out.println(out);
            System.out.flush();
            Matcher m = REFLECTION_WARNING.matcher(out);
            if (m.find()) {
                if (warningRefersToProjectSource(task, m.group(1))) {
                    projectReflectionWarnings[0] = true;
                }
            }
        }
    }

    private static void check(ClojureCheck task) {
        task.getLogging().captureStandardOutput(LogLevel.LIFECYCLE);
        task.getLogging().captureStandardError(LogLevel.ERROR);

        task.getFileSystemOperations().delete(spec -> spec.delete(task.getTemporaryDir()));

        Set<String> namespaces = task.getNamespaces().getOrElse(Collections.emptySet());
        task.getLogger().info("Checking {}", String.join(", ", namespaces));

        FileCollection classpath = classpathForReflectiveCheck(task)
                .plus(task.getProjectLayout().files(task.getTemporaryDir()));

        Path preplLogPath = task.getProjectLayout()
                .getBuildDirectory()
                .file("reports/check-clojure-prepl.log")
                .get()
                .getAsFile()
                .toPath();
        long[] preplLineCount = new long[] {0L};

        Prepl prepl = new Prepl(execOperationsFor(task));
        PreplClient preplClient = null;
        boolean failures = false;
        boolean[] projectReflectionWarnings = new boolean[] {false};
        BufferedWriter preplLog = null;

        try {
            Files.createDirectories(preplLogPath.getParent());
            preplLog = Files.newBufferedWriter(
                    preplLogPath,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            preplClient = prepl.start(spec -> {
                spec.setJavaLauncher(task.getJavaLauncher().getOrNull());
                spec.setClasspath(classpath);
                spec.setPort(0);
                spec.forkOptions(fork -> {
                    fork.setJvmArgs(task.getForkOptions().getJvmArgs());
                    fork.setMinHeapSize(task.getForkOptions().getMemoryInitialSize());
                    fork.setMaxHeapSize(task.getForkOptions().getMemoryMaximumSize());
                    fork.setDefaultCharacterEncoding(StandardCharsets.UTF_8.name());
                });
            });
            boolean wantReflection =
                    !ClojureCheck.REFLECTION_SILENT.equals(task.getReflection().getOrNull());
            if (wantReflection) {
                // Root binding still helps tooling; real enforcement is per-load `binding` below.
                preplClient.evalEdn("(set! *warn-on-reflection* true)");
                drainPreplOutput(task, preplClient, projectReflectionWarnings, preplLog, preplLineCount);
            }

            for (String namespace : namespaces) {
                String nsFilePath = namespace.replace('-', '_').replace('.', '/');
                try {
                    Path sourceFile = findSourceFileForNamespace(task, namespace);
                    if (wantReflection) {
                        if (sourceFile != null) {
                            String p = sourceFile.toAbsolutePath().normalize().toString().replace('\\', '/');
                            preplClient.evalEdn(
                                    "(binding [*warn-on-reflection* true] (load-file \"" + escapeEdnString(p) + "\"))");
                        } else {
                            preplClient.evalEdn("(binding [*warn-on-reflection* true] (load \"" + nsFilePath + "\"))");
                        }
                    } else {
                        if (sourceFile != null) {
                            preplClient.evalData(Edn.list(
                                    Symbol.newSymbol("load-file"),
                                    sourceFile.toString().replace('\\', '/')));
                        } else {
                            preplClient.evalData(Edn.list(Symbol.newSymbol("load"), nsFilePath));
                        }
                    }
                    preplClient.evalEdn("(.flush ^java.io.Writer *err*)");
                } catch (ClojureException e) {
                    failures = true;
                    System.err.println(e.getMessage());
                }
                drainPreplOutput(task, preplClient, projectReflectionWarnings, preplLog, preplLineCount);
            }

            for (int i = 0; i < 8; i++) {
                drainPreplOutput(task, preplClient, projectReflectionWarnings, preplLog, preplLineCount);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            try {
                if (preplClient != null) {
                    try {
                        drainPreplOutput(task, preplClient, projectReflectionWarnings, preplLog, preplLineCount);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    preplClient.close();
                }
            } finally {
                if (preplLog != null) {
                    try {
                        preplLog.close();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }

        task.getLogger()
                .lifecycle(
                        "ClojureCheck PREPL :out/:err messages drained: "
                                + preplLineCount[0]
                                + "; mirror log: "
                                + preplLogPath.toAbsolutePath());

        if (ClojureCheck.REFLECTION_FAIL.equals(task.getReflection().getOrNull())
                && projectReflectionWarnings[0]) {
            throw new GradleException("Reflection warnings found. See output above.");
        }

        if (failures) {
            throw new GradleException("Compilation failed. See output above.");
        }

        Path output = task.getInternalOutputFile().get().getAsFile().toPath();
        try {
            Files.write(output, Arrays.asList(Instant.now().toString()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
