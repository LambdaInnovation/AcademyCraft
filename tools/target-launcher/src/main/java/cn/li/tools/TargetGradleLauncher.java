package cn.li.tools;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Single target-build orchestration engine used by all OS frontends. */
public final class TargetGradleLauncher {
    public static void main(String[] args) throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String target = args.length == 0 || args[0].startsWith("-") ? "forge-1.20.1" : args[0];
        int start = args.length == 0 || args[0].startsWith("-") ? 0 : 1;
        TargetModel model = targetModel(root, target);
        int requiredJava = model.gradleJvmVersion;
        String javaHome = Optional.ofNullable(System.getenv("MC_JAVA_HOME_" + requiredJava))
                .orElse(System.getenv("JAVA_HOME"));
        if (javaHome == null || javaHome.isBlank())
            throw new IllegalArgumentException("Set MC_JAVA_HOME_" + requiredJava + " or JAVA_HOME.");
        Path java = Paths.get(javaHome, "bin", isWindows() ? "java.exe" : "java");
        if (!Files.isExecutable(java)) throw new IllegalArgumentException("Java executable not found: " + java);
        Path wrapper = model.wrapper.equals("root") ? root.resolve(isWindows() ? "gradlew.bat" : "gradlew")
                : root.resolve(model.wrapper + (isWindows() && !model.wrapper.endsWith(".bat") ? ".bat" : ""));
        if (!Files.isRegularFile(wrapper)) {
            throw new IllegalArgumentException("Build profile wrapper is missing for target " + target + ": " + wrapper);
        }
        List<String> gradleArgs = gradleArguments(root, target, args, start);
        List<String> command = wrapperCommand(
                wrapper, gradleArgs, isWindows(), System.getenv("ComSpec"));
        ProcessBuilder process = new ProcessBuilder(command).directory(root.toFile()).inheritIO();
        process.environment().put("JAVA_HOME", javaHome);
        int exit = process.start().waitFor();
        System.exit(exit);
    }

    static List<String> gradleArguments(Path root, String target, String[] args, int start) {
        List<String> gradleArgs = new ArrayList<>();
        gradleArgs.add("--gradle-user-home");
        gradleArgs.add(root.resolve("build/gradle-home").resolve(target).toString());
        gradleArgs.add("-PplatformTarget=" + target);
        boolean hasRequestedTask = false;
        for (int i = start; i < args.length; i++) {
            gradleArgs.add(args[i]);
            if (!args[i].startsWith("-")) hasRequestedTask = true;
        }
        if (!hasRequestedTask) gradleArgs.add(":platform:build");
        return gradleArgs;
    }

    static List<String> wrapperCommand(
            Path wrapper,
            List<String> gradleArgs,
            boolean windows,
            String commandProcessor) {
        List<String> command = new ArrayList<>();
        if (windows) {
            command.add(commandProcessor == null || commandProcessor.isBlank()
                    ? "cmd.exe"
                    : commandProcessor);
            command.add("/d");
            command.add("/c");
        }
        command.add(wrapper.toString());
        command.addAll(gradleArgs);
        return command;
    }

    private static TargetModel targetModel(Path root, String target) throws Exception {
        String json = Files.readString(root.resolve("platform-catalog.json"), StandardCharsets.UTF_8);
        return targetModelFromJson(json, target);
    }

    static TargetModel targetModelFromJson(String json, String target) {
        String targets = requiredObjectMember(
                json, "targets", "platform-catalog.json has no targets object");
        String targetBlock = objectMember(targets, target);
        if (targetBlock == null) {
            throw new IllegalArgumentException("Unknown target in platform-catalog.json: " + target);
        }
        targetBlock = requireObject(targetBlock, "Target is not an object: " + target);

        String javaValue = objectMember(targetBlock, "gradleJvmVersion");
        if (javaValue == null) {
            throw new IllegalArgumentException("Target has no gradleJvmVersion: " + target);
        }
        int gradleJvmVersion = parseInt(javaValue, "Invalid gradleJvmVersion for target: " + target);

        String profileValue = objectMember(targetBlock, "buildProfile");
        if (profileValue == null) {
            throw new IllegalArgumentException("Target has no buildProfile: " + target);
        }
        String profile = parseStringValue(profileValue, "Invalid buildProfile for target: " + target);

        String profiles = requiredObjectMember(
                json, "buildProfiles", "platform-catalog.json has no buildProfiles object");
        String profileBlock = objectMember(profiles, profile);
        if (profileBlock == null) {
            throw new IllegalArgumentException("Build profile missing: " + profile);
        }
        profileBlock = requireObject(profileBlock, "Build profile is not an object: " + profile);

        String wrapperValue = objectMember(profileBlock, "wrapper");
        if (wrapperValue == null) {
            throw new IllegalArgumentException("Build profile has no wrapper: " + profile);
        }
        String wrapper = parseStringValue(wrapperValue, "Invalid wrapper for build profile: " + profile);
        return new TargetModel(gradleJvmVersion, wrapper);
    }

    private static String requiredObjectMember(String json, String name, String missingMessage) {
        String value = objectMember(requireObject(json, "Catalog root is not an object"), name);
        if (value == null) throw new IllegalArgumentException(missingMessage);
        return requireObject(value, missingMessage);
    }

    /**
     * Return one direct member value from a JSON object.
     *
     * This structural scan deliberately observes object boundaries. The old
     * global regex could resolve components.forge-1.20.1 before the same key
     * under targets, then report missing target fields.
     */
    private static String objectMember(String objectJson, String wantedName) {
        int index = skipWhitespace(objectJson, 0);
        if (index >= objectJson.length() || objectJson.charAt(index) != '{') {
            throw new IllegalArgumentException("Expected JSON object");
        }
        index++;
        while (true) {
            index = skipWhitespace(objectJson, index);
            if (index >= objectJson.length()) {
                throw new IllegalArgumentException("Unterminated JSON object");
            }
            if (objectJson.charAt(index) == '}') return null;

            ParsedString name = parseJsonString(objectJson, index);
            index = skipWhitespace(objectJson, name.end);
            if (index >= objectJson.length() || objectJson.charAt(index) != ':') {
                throw new IllegalArgumentException("Expected ':' after JSON object member");
            }
            int valueStart = skipWhitespace(objectJson, index + 1);
            int valueEnd = valueEnd(objectJson, valueStart);
            if (wantedName.equals(name.value)) {
                return objectJson.substring(valueStart, valueEnd);
            }

            index = skipWhitespace(objectJson, valueEnd);
            if (index >= objectJson.length()) {
                throw new IllegalArgumentException("Unterminated JSON object");
            }
            char separator = objectJson.charAt(index);
            if (separator == ',') {
                index++;
            } else if (separator == '}') {
                return null;
            } else {
                throw new IllegalArgumentException("Expected ',' or '}' after JSON value");
            }
        }
    }

    private static int valueEnd(String json, int start) {
        if (start >= json.length()) throw new IllegalArgumentException("Missing JSON value");
        char first = json.charAt(start);
        if (first == '"') return parseJsonString(json, start).end;
        if (first == '{' || first == '[') return compositeEnd(json, start);

        int index = start;
        while (index < json.length()) {
            char ch = json.charAt(index);
            if (ch == ',' || ch == '}' || ch == ']' || Character.isWhitespace(ch)) break;
            index++;
        }
        if (index == start) throw new IllegalArgumentException("Missing JSON value");
        return index;
    }

    private static int compositeEnd(String json, int start) {
        Deque<Character> closing = new ArrayDeque<>();
        closing.push(json.charAt(start) == '{' ? '}' : ']');
        int index = start + 1;
        while (index < json.length()) {
            char ch = json.charAt(index);
            if (ch == '"') {
                index = parseJsonString(json, index).end;
                continue;
            }
            if (ch == '{') {
                closing.push('}');
            } else if (ch == '[') {
                closing.push(']');
            } else if (ch == '}' || ch == ']') {
                if (closing.isEmpty() || closing.pop() != ch) {
                    throw new IllegalArgumentException("Mismatched JSON delimiter");
                }
                if (closing.isEmpty()) return index + 1;
            }
            index++;
        }
        throw new IllegalArgumentException("Unterminated JSON composite value");
    }

    private static ParsedString parseJsonString(String json, int start) {
        if (start >= json.length() || json.charAt(start) != '"') {
            throw new IllegalArgumentException("Expected JSON string");
        }
        StringBuilder value = new StringBuilder();
        int index = start + 1;
        while (index < json.length()) {
            char ch = json.charAt(index++);
            if (ch == '"') return new ParsedString(value.toString(), index);
            if (ch != '\\') {
                value.append(ch);
                continue;
            }
            if (index >= json.length()) throw new IllegalArgumentException("Unterminated JSON escape");
            char escaped = json.charAt(index++);
            switch (escaped) {
                case '"', '\\', '/' -> value.append(escaped);
                case 'b' -> value.append('\b');
                case 'f' -> value.append('\f');
                case 'n' -> value.append('\n');
                case 'r' -> value.append('\r');
                case 't' -> value.append('\t');
                case 'u' -> {
                    if (index + 4 > json.length()) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape");
                    }
                    try {
                        value.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Invalid JSON unicode escape", ex);
                    }
                    index += 4;
                }
                default -> throw new IllegalArgumentException("Invalid JSON escape: \\" + escaped);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static String requireObject(String value, String message) {
        String trimmed = value.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '{'
                || trimmed.charAt(trimmed.length() - 1) != '}') {
            throw new IllegalArgumentException(message);
        }
        return trimmed;
    }

    private static String parseStringValue(String value, String message) {
        String trimmed = value.trim();
        try {
            ParsedString parsed = parseJsonString(trimmed, 0);
            if (skipWhitespace(trimmed, parsed.end) != trimmed.length()) {
                throw new IllegalArgumentException(message);
            }
            return parsed.value;
        } catch (IllegalArgumentException ex) {
            if (message.equals(ex.getMessage())) throw ex;
            throw new IllegalArgumentException(message, ex);
        }
    }

    private static int parseInt(String value, String message) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message, ex);
        }
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) index++;
        return index;
    }

    record TargetModel(int gradleJvmVersion, String wrapper) {}
    private record ParsedString(String value, int end) {}

    private static boolean isWindows() { return System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win"); }
}
