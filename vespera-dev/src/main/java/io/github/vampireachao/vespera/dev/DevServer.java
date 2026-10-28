package io.github.vampireachao.vespera.dev;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 开发服务器：一条命令从"一个空仓库"走到"一个能进去说话的服务器"。
 *
 * <p>它是整个工程里唯一会启动服务端的东西，没有第二个 run configuration 需要跟它同步。
 * 这是 mmSupernaturals 那条路子上最值得抄的一点：<b>启动逻辑进版本库</b>，而不是进 IDE 的
 * 设置。谁 clone 下来，谁就有一条能跑的路；丢了 .idea 也丢不掉这条路。
 *
 * <p>它做四件事，顺序固定：
 *
 * <ol>
 *   <li><b>拿服务端。</b>{@code server/paper.jar} 不在，就按 Paper 的 fill API 下最新的 26.3 构建。
 *       下过一次就不再联网；{@code mvn clean} 是唯一会把它删掉的东西。</li>
 *   <li><b>摆好目录。</b>写 {@code eula.txt} 与 {@code server.properties}，把 {@code plugins/}
 *       里少的 jar 补上，把地图资源同步进 {@code server/vespera/}。</li>
 *   <li><b>起服务器。</b>控制台接到这个终端，命令可以直接在这里敲。</li>
 *   <li><b>可选地验一遍。</b>{@code --smoke} 会等到 "Done"、敲一条 {@code /vespera doctor}、
 *       确认插件真的活着，然后自己退出。CI 用这个模式，人不用。</li>
 * </ol>
 *
 * <h2>怎么用</h2>
 * <pre>
 *   ./dev.sh            # 起服务器，进游戏说话
 *   ./dev.sh smoke      # 起、验、退，退出码 0 表示通过
 *   ./dev.sh debug      # 起服务器并挂上 JDWP 5005，插件打断点用
 * </pre>
 *
 * <p>开关（系统属性，或写成环境变量）：{@code vespera.port}、{@code vespera.onlineMode}、
 * {@code vespera.keepState}、{@code vespera.seed}、{@code vespera.paperVersion}、
 * {@code vespera.dir}。
 */
public final class DevServer {

    private static final String FILL_API = "https://fill.papermc.io/v3/projects/paper/versions/";
    private static final String DOWNLOAD_PAGE = "https://papermc.io/downloads/paper";
    private static final Duration STARTUP = Duration.ofMinutes(6);

    private DevServer() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.from(args);
        System.out.println("维斯佩拉 · 开发服务器");
        System.out.println();

        prepare(config);

        int exit = config.smoke ? smoke(config) : run(config);
        System.exit(exit);
    }

    // -- 准备 ---------------------------------------------------------------

    private static void prepare(Config config) throws IOException, InterruptedException {
        Path server = config.server;
        Files.createDirectories(server.resolve("plugins"));
        if (!config.keepState) {
            reset(server);
        }
        ensurePaper(config);
        writeServerProperties(config);
        installPlugins(config);
        installAssets(config);
    }

    /**
     * 把服务器目录恢复成"没跑过"的样子，但留着三样贵东西：服务端 jar、它解出来的
     * 依赖库、以及已经下好的插件 jar。世界默认保留——开发时把地图删掉，等于把昨天的
     * 布置成果一起删掉。
     */
    private static void reset(Path server) throws IOException {
        try (Stream<Path> entries = Files.list(server)) {
            for (Path entry : entries.toList()) {
                String name = entry.getFileName().toString();
                if (name.equals("paper.jar") || name.equals("plugins") || name.equals("libraries")
                        || name.equals("versions") || name.equals("cache") || name.equals("vespera")) {
                    continue;
                }
                delete(entry);
            }
        }
    }

    private static void ensurePaper(Config config) throws IOException, InterruptedException {
        Path jar = config.server.resolve("paper.jar");
        if (Files.isRegularFile(jar)) {
            return;
        }
        String version = config.paperVersion;
        System.out.println("没有服务端 jar，取 Paper " + version + " 的最新构建……");
        String builds = get(FILL_API + version + "/builds");
        String url = firstDownloadUrl(builds);
        if (url == null) {
            throw new IOException("没能从 fill API 里读出下载地址。手动下一个放到 " + jar
                    + "\n  下载页：" + DOWNLOAD_PAGE);
        }
        Path partial = jar.resolveSibling("paper.jar.part");
        HttpResponse<Path> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "vespera-dev")
                        .build(), HttpResponse.BodyHandlers.ofFile(partial));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            throw new IOException("下载失败，HTTP " + response.statusCode() + "：" + url);
        }
        Files.move(partial, jar, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("  已保存到 " + jar + "（" + Files.size(jar) / 1024 / 1024 + " MB）");
    }

    /** fill API 返回的是一个构建数组，第一个是最新的。只取 {@code server:default} 那一条。 */
    static String firstDownloadUrl(String json) {
        int marker = json.indexOf("\"server:default\"");
        if (marker < 0) {
            return null;
        }
        Matcher matcher = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json.substring(marker, Math.min(json.length(), marker + 800)));
        return matcher.find() ? matcher.group(1).replace("\\/", "/") : null;
    }

    private static String get(String url) throws IOException, InterruptedException {
        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "vespera-dev")
                        .build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException(url + " 返回 HTTP " + response.statusCode());
        }
        return response.body();
    }

    /**
     * 写配置。{@code online-mode=true} 是默认值，而且故意不给"方便"的默认关闭：
     * 一个跑在公网上的离线验证服务器，等于把所有人的账号摆在门口。要本地离线测试，
     * 显式加 {@code -Dvespera.onlineMode=false}，并知道自己在做什么。
     */
    private static void writeServerProperties(Config config) throws IOException {
        Path file = config.server.resolve("server.properties");
        Files.writeString(config.server.resolve("eula.txt"),
                "# 起这个服务器等于同意 Minecraft EULA：https://aka.ms/MinecraftEULA\neula=true\n");
        if (Files.isRegularFile(file) && !config.smoke) {
            return;
        }
        Files.writeString(file, String.join("\n",
                "motd=维斯佩拉 · 开发服",
                "server-port=" + config.port,
                "online-mode=" + config.onlineMode,
                "enforce-secure-profile=" + config.onlineMode,
                "white-list=false",
                "spawn-protection=0",
                "max-players=20",
                "view-distance=8",
                "simulation-distance=6",
                "allow-nether=true",
                "enable-command-block=true",
                "level-name=world",
                "level-seed=" + config.seed,
                "level-type=minecraft:normal",
                "gamemode=survival",
                "difficulty=normal") + "\n");
    }

    /** 插件 jar 已经由 Maven 直接产出到 {@code server/plugins/}，这里只补第三方的。 */
    private static void installPlugins(Config config) throws IOException, InterruptedException {
        Path plugins = config.server.resolve("plugins");
        if (!Files.isRegularFile(plugins.resolve("vespera-plugin.jar"))) {
            System.out.println("  警告：server/plugins/vespera-plugin.jar 不存在。");
            System.out.println("        先跑 `mvn package`（./dev.sh 会替你做）。");
        }
        Path manifest = config.project.resolve("plugins.tsv");
        if (!Files.isRegularFile(manifest)) {
            return;
        }
        for (String line : Files.readAllLines(manifest)) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] cells = line.split("\t");
            if (cells.length < 2) {
                continue;
            }
            String name = cells[0].trim();
            String url = cells[1].trim();
            String sha256 = cells.length > 2 ? cells[2].trim() : "";
            Path target = plugins.resolve(name.endsWith(".jar") ? name : name + ".jar");
            if (Files.isRegularFile(target)) {
                continue;
            }
            if (sha256.isEmpty()) {
                System.out.println("  跳过 " + name + "：没有校验和，不自动下载。");
                System.out.println("        手动下载后放到 " + target);
                System.out.println("        来源：" + url);
                continue;
            }
            System.out.println("  下载 " + name + " ……");
            downloadChecked(url, target, sha256);
        }
    }

    private static void downloadChecked(String url, Path target, String sha256)
            throws IOException, InterruptedException {
        Path partial = target.resolveSibling(target.getFileName() + ".part");
        HttpResponse<Path> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
                .send(HttpRequest.newBuilder(URI.create(url))
                        .header("User-Agent", "vespera-dev")
                        .build(), HttpResponse.BodyHandlers.ofFile(partial));
        if (response.statusCode() != 200) {
            Files.deleteIfExists(partial);
            throw new IOException(url + " 返回 HTTP " + response.statusCode());
        }
        String actual = sha256(partial);
        if (!actual.equalsIgnoreCase(sha256)) {
            Files.deleteIfExists(partial);
            throw new IOException(target.getFileName() + " 校验失败：期望 " + sha256 + "，实际 " + actual);
        }
        Files.move(partial, target, StandardCopyOption.REPLACE_EXISTING);
    }

    private static String sha256(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            java.security.MessageDigest digest;
            try {
                digest = java.security.MessageDigest.getInstance("SHA-256");
            } catch (java.security.NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            StringBuilder hex = new StringBuilder();
            for (byte each : digest.digest()) {
                hex.append(String.format("%02x", each));
            }
            return hex.toString();
        }
    }

    /** 地图资源从 {@code vespera-world} 的构建产物同步进来。插件以此目录为世界资产根。 */
    private static void installAssets(Config config) throws IOException {
        Path source = config.project.resolve("vespera-world/target/world-assets");
        if (!Files.isDirectory(source)) {
            return;
        }
        Path target = config.server.resolve("vespera");
        copyTree(source, target);
        System.out.println("  地图资源已同步：" + target);
    }

    private static void copyTree(Path from, Path to) throws IOException {
        try (Stream<Path> paths = Files.walk(from)) {
            for (Path path : paths.toList()) {
                Path destination = to.resolve(from.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.createDirectories(destination.getParent());
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    // -- 运行 ---------------------------------------------------------------

    private static Process launch(Config config, boolean interactive) throws IOException {
        List<String> command = new ArrayList<>(List.of(
                javaBinary(),
                "-Xms2G", "-Xmx2G",
                "-Dfile.encoding=UTF-8",
                "-Duser.language=zh",
                "-Duser.country=CN"));
        if (config.debug) {
            // 插件活在服务端那个 JVM 里，断点必须挂在那一边。
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend="
                    + (config.debugSuspend ? "y" : "n") + ",address=127.0.0.1:" + freePort(config.debugPort));
            if (config.debugSuspend) {
                System.out.println("服务端会在第一行代码前停住，等调试器连 127.0.0.1:" + config.debugPort);
            }
        }
        command.add("-jar");
        command.add("paper.jar");
        command.add("--nogui");

        ProcessBuilder builder = new ProcessBuilder(command)
                .directory(config.server.toFile())
                .redirectErrorStream(true);
        return builder.start();
    }

    /** 人用的模式：控制台接到终端，Ctrl+C 结束（世界会存）。 */
    private static int run(Config config) throws IOException, InterruptedException {
        Process process = launch(config, true);
        Console console = new Console(process, true);
        console.start();
        console.forward(System.in);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (process.isAlive()) {
                console.send("stop");
            }
        }, "vespera-dev-stop"));

        System.out.println();
        System.out.println("服务器起来之后：");
        System.out.println("  进服      localhost:" + config.port + (config.onlineMode ? "（正版验证）" : "（离线验证）"));
        System.out.println("  自检      /vespera doctor");
        System.out.println("  说话      /vespera talk bell-test   或右键出生点旁边那个村民");
        System.out.println("  控制台    在这里直接敲服务器命令，stop 结束");
        System.out.println();

        int exit = process.waitFor();
        System.out.println("服务器已退出（" + exit + "）");
        return exit;
    }

    /**
     * CI 用的模式：起、验、退。验的东西不多，但足以挡住"插件根本没加载"这一类事故：
     * 服务端进了 Done、{@code /vespera doctor} 答了话、剧本被读到了。
     */
    private static int smoke(Config config) throws IOException, InterruptedException {
        Process process = launch(config, false);
        Console console = new Console(process, false);
        console.start();
        try {
            if (!console.await("Done (", STARTUP)) {
                System.out.println("服务端没能在 " + STARTUP.toMinutes() + " 分钟内起来。最后 40 行：");
                console.tail(40).forEach(System.out::println);
                return 1;
            }
            System.out.println("服务端已就绪，敲一条自检……");
            console.send("vespera doctor");
            boolean answered = console.await("维斯佩拉 · 自检", Duration.ofSeconds(30));
            console.tail(40).forEach(System.out::println);
            if (!answered) {
                System.out.println("自检没有回应：插件可能没加载，或者命令没注册。");
                return 1;
            }
            return 0;
        } finally {
            console.send("stop");
            if (!process.waitFor(90, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    // -- 小工具 -------------------------------------------------------------

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", "java").toString();
    }

    /** 调试端口被占时往下找，避免上一次没退干净的会话把这次拖垮。 */
    private static int freePort(int preferred) {
        for (int port = preferred; port < preferred + 20; port++) {
            try (ServerSocket probe = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"))) {
                return port;
            } catch (IOException taken) {
                // 换一个
            }
        }
        return preferred;
    }

    private static void delete(Path root) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    /** 服务端的输出：一边打到终端，一边攒着给断言和报错用。 */
    private static final class Console implements Runnable {

        private final BufferedReader reader;
        private final PrintWriter stdin;
        private final boolean echo;
        private final StringBuilder transcript = new StringBuilder();

        Console(Process process, boolean echo) {
            this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            this.stdin = new PrintWriter(new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)), true);
            this.echo = echo;
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (transcript) {
                        transcript.append(line).append('\n');
                    }
                    if (echo) {
                        System.out.println(line);
                    }
                }
            } catch (IOException e) {
                // 服务器退出了，就这样
            }
        }

        void start() {
            Thread thread = new Thread(this, "vespera-dev-console");
            thread.setDaemon(true);
            thread.start();
        }

        /** 把终端里的输入转给服务器。服务器读的是管道，不是这个终端。 */
        void forward(InputStream terminal) {
            Thread pump = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(terminal, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        send(line);
                    }
                } catch (IOException e) {
                    // 输入断了，服务器还在跑
                }
            }, "vespera-dev-stdin");
            pump.setDaemon(true);
            pump.start();
        }

        void send(String command) {
            synchronized (stdin) {
                stdin.println(command);
            }
        }

        boolean await(String marker, Duration timeout) throws InterruptedException {
            long deadline = System.nanoTime() + timeout.toNanos();
            while (System.nanoTime() < deadline) {
                if (text().contains(marker)) {
                    return true;
                }
                Thread.sleep(300);
            }
            return text().contains(marker);
        }

        String text() {
            synchronized (transcript) {
                return transcript.toString();
            }
        }

        List<String> tail(int lines) {
            String[] all = text().split("\n");
            int from = Math.max(0, all.length - lines);
            return List.of(all).subList(from, all.length);
        }
    }

    /** 所有开关集中在一处，读起来像一份说明书而不是一堆散落的 getProperty。 */
    private record Config(Path project, Path server, int port, boolean onlineMode, boolean keepState,
            boolean smoke, boolean debug, boolean debugSuspend, int debugPort, String seed, String paperVersion) {

        static Config from(String[] args) {
            Path project = Path.of(System.getProperty("vespera.projectDir", System.getProperty("user.dir")))
                    .toAbsolutePath().normalize();
            boolean smoke = args.length > 0 && args[0].equalsIgnoreCase("smoke");
            boolean debug = (args.length > 0 && args[0].equalsIgnoreCase("debug"))
                    || Boolean.getBoolean("vespera.debug");
            return new Config(
                    project,
                    Path.of(System.getProperty("vespera.dir", project.resolve("server").toString())),
                    Integer.getInteger("vespera.port", 25565),
                    Boolean.parseBoolean(System.getProperty("vespera.onlineMode", "true")),
                    // 开发默认保留世界；smoke 默认重建，保证每次验的是同一个世界。
                    Boolean.parseBoolean(System.getProperty("vespera.keepState", Boolean.toString(!smoke))),
                    smoke,
                    debug,
                    Boolean.getBoolean("vespera.debugSuspend"),
                    Integer.getInteger("vespera.debugPort", 5005),
                    System.getProperty("vespera.seed", String.valueOf(System.nanoTime())),
                    System.getProperty("vespera.paperVersion", "26.3"));
        }
    }
}
