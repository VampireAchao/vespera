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

    /** 目录锁。一个世界目录同时只能有一个服务端在写它。 */
    private static final String LOCK_FILE = ".vespera.lock";

    /** 本次运行给服务端 JVM 开的 JDWP 端口。为了在横幅里报出来，只能存在这里。 */
    private static volatile int attachPort = -1;

    /** 真正使用的游戏端口。可能不等于配置值——被占用时会顺延。 */
    private static volatile int listenPort = -1;

    private DevServer() {
    }

    public static void main(String[] args) throws Exception {
        Config config = Config.from(args);
        System.out.println("维斯佩拉 · " + config.profile.label() + "服务器");
        System.out.println("  目录    " + config.server);
        System.out.println("  端口    " + config.port + (config.onlineMode ? "（正版验证）" : "（离线验证）") + "，被占用会自动顺延");
        System.out.println("  形态    " + (config.smoke ? "无头：起、验、退" : "控制台，Ctrl+C 结束"));
        System.out.println();

        prepare(config);

        int exit = config.smoke ? smoke(config) : run(config);
        System.out.println("已退出（" + exit + "），目录锁已释放。");
        System.exit(exit);
    }

    // -- 准备 ---------------------------------------------------------------

    private static void prepare(Config config) throws IOException, InterruptedException {
        Path server = config.server;
        Files.createDirectories(server.resolve("plugins"));
        ensurePort(config);
        acquireLock(config);
        if (!config.keepState) {
            reset(server);
        }
        ensureBuilt(config);
        ensurePaper(config);
        writeServerProperties(config);
        installPlugins(config);
        installAssets(config);
    }

    /**
     * 选一个真的能用的端口。
     *
     * <p>本地同时开着好几个 Minecraft 服务端是常态——另一个仓库的测试服、朋友借你的端口、
     * 上一次没退干净的进程。撞上端口时直接失败，等于让人自己去查 {@code lsof}；
     * 顺延一个端口，并且把占用者名字打出来，才是"一键"该有的样子。
     *
     * <p>顺延只往上找 20 个。找不着就明说，不要假装找到了。
     */
    private static void ensurePort(Config config) {
        listenPort = pickPort(config.port);
        if (listenPort == config.port) {
            return;
        }
        System.out.println("  端口 " + config.port + " 被占用（" + occupant(config.port) + "），改用 " + listenPort + "。");
        System.out.println("        想让服务器用它：先把那个进程停掉；或者加 -Dvespera.port=25xxx 指定。");
    }

    private static int pickPort(int preferred) {
        for (int port = preferred; port < preferred + 20; port++) {
            try (ServerSocket probe = new ServerSocket()) {
                probe.setReuseAddress(true);
                probe.bind(new java.net.InetSocketAddress(port), 1);
                return port;
            } catch (IOException taken) {
                // 下一个
            }
        }
        throw new IllegalStateException("端口 " + preferred + " 到 " + (preferred + 19) + " 全被占用，"
                + "先用 -Dvespera.port=25xxx 换一个。");
    }

    /**
     * 谁占着这个端口。尽力而为：macOS/Linux 有 {@code lsof} 就问它，没有就说不认识。
     *
     * <p>{@code -Fpc} 让它输出机器可读的两行：pid 与命令名。这样比解析人类可读的表格稳。
     */
    private static String occupant(int port) {
        try {
            Process process = new ProcessBuilder("lsof", "-nP", "-iTCP:" + port, "-sTCP:LISTEN", "-Fpc")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            process.waitFor(3, TimeUnit.SECONDS);
            String command = output.lines().filter(line -> line.startsWith("c")).map(line -> line.substring(1))
                    .findFirst().orElse("未知进程");
            String pid = output.lines().filter(line -> line.startsWith("p")).map(line -> line.substring(1))
                    .findFirst().orElse("?");
            return command + " pid " + pid;
        } catch (IOException | InterruptedException e) {
            return "占用者未知";
        }
    }

    /**
     * 一个目录同时只能有一个实例。
     *
     * <p>两套环境用两个目录，本来不会撞；但"忘了自己已经开着一个"是常见事，
     * 而两个服务器共用一个世界目录，结果不是报错，是**世界被写坏**——那种损坏要几小时后
     * 才会以"区块错乱"的形式出现，届时已经没法归因。
     *
     * <p>所以宁可启动失败。锁文件里写着 pid，进程还在就拒跑；进程没了就是上次没退干净，
     * 直接覆盖。
     */
    private static void acquireLock(Config config) throws IOException {
        Path lock = config.server.resolve(LOCK_FILE);
        if (Files.isRegularFile(lock)) {
            String[] parts = Files.readString(lock).trim().split("\\s+");
            long pid = parts.length > 0 ? parseLong(parts[0]) : -1;
            if (pid > 0 && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)) {
                throw new IOException("这个目录已经被另一个实例占着：pid " + pid
                        + (parts.length > 1 ? "，端口 " + parts[1] : "")
                        + "\n  目录：" + config.server
                        + "\n  开发服与测试服要用不同的目录（默认已经是 server/dev 与 server/test），"
                        + "\n  真要复用同一目录，先把那个实例停掉。");
            }
        }
        Files.writeString(lock, ProcessHandle.current().pid() + " " + listenPort + "\n");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(lock);
            } catch (IOException ignored) {
                // 退出路径上没什么可做的
            }
        }, "vespera-dev-lock"));
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
                        || name.equals("versions") || name.equals("cache") || name.equals("vespera")
                        || name.equals(LOCK_FILE)) {
                    continue;
                }
                delete(entry);
            }
        }
    }

    /**
     * 服务端 jar：下载一次，所有环境共用。
     *
     * <p>Paper 的 jar 有 52 MB，而开发服、测试服是两个完全独立的目录。让它们各自下一次，
     * 等于为了"隔离"多付 52 MB 和一次下载时间——而隔离要隔离的是**世界与插件状态**，
     * 不是这个只读的服务端本体。
     *
     * <p>所以缓存放在 {@code server/build/paper.jar}，各环境启动时拷一份过去。
     * 注意：{@code libraries/}、{@code versions/} 仍然是各环境自己的，Paper 会自己解压，
     * 这里不插手——那部分和版本强相关，共用一个反而容易出事。
     */
    private static void ensurePaper(Config config) throws IOException, InterruptedException {
        Path jar = config.server.resolve("paper.jar");
        if (Files.isRegularFile(jar)) {
            return;
        }
        Path cache = config.project.resolve("server/build/paper.jar");
        if (Files.isRegularFile(cache)) {
            System.out.println("  从构建缓存取服务端：" + cache);
            Files.copy(cache, jar, StandardCopyOption.REPLACE_EXISTING);
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
        Path partial = cache.resolveSibling("paper.jar.part");
        Files.createDirectories(cache.getParent());
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
        Files.move(partial, cache, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(cache, jar, StandardCopyOption.REPLACE_EXISTING);
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
        // 端口变了就得重写，否则配置文件里还是那个被占用的号码，服务端起来仍会失败。
        if (Files.isRegularFile(file) && !config.smoke && !config.flat && listenPort == config.port) {
            return;
        }
        String layers = "{\"layers\":[{\"block\":\"minecraft:bedrock\",\"height\":1},"
                + "{\"block\":\"minecraft:stone\",\"height\":3},"
                + "{\"block\":\"minecraft:dirt\",\"height\":1},"
                + "{\"block\":\"minecraft:grass_block\",\"height\":1}],"
                + "\"biome\":\"minecraft:plains\"}";
        Files.writeString(file, String.join("\n",
                "motd=维斯佩拉 · " + config.profile.label() + "服",
                "server-port=" + listenPort,
                "online-mode=" + config.onlineMode,
                "enforce-secure-profile=" + config.onlineMode,
                "white-list=false",
                "spawn-protection=0",
                "max-players=20",
                "view-distance=8",
                "simulation-distance=6",
                "allow-nether=true",
                "enable-command-block=true",
                "level-name=" + (config.flat ? "campus" : "world"),
                "level-seed=" + config.seed,
                "level-type=" + (config.flat ? "minecraft:flat" : "minecraft:normal"),
                "generator-settings=" + (config.flat ? layers : ""),
                "gamemode=survival",
                "difficulty=normal") + "\n");
    }

    /**
     * 把插件装进这个环境。
     *
     * <p>本插件从构建暂存区（{@code server/build/plugins/}）拷过来——不是软链、不是共用文件。
     * 每个环境一份实体拷贝，是为了让"我这边升级插件"永远不会突然改变你正在测的那台服务器。
     */
    private static void installPlugins(Config config) throws IOException, InterruptedException {
        Path plugins = config.server.resolve("plugins");
        Path staged = config.project.resolve("server/build/plugins/vespera-plugin.jar");
        if (Files.isRegularFile(staged)) {
            Files.copy(staged, plugins.resolve("vespera-plugin.jar"), StandardCopyOption.REPLACE_EXISTING);
        } else {
            System.out.println("  警告：构建产物不存在（server/build/plugins/vespera-plugin.jar）。");
            System.out.println("        先跑 `mvn package`。");
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
            // 服务端是子进程，插件活在它里面。断点必须挂在子进程那一侧，
            // 所以这里替它开一个 JDWP 监听端口，让第二个调试器连进来。
            int port = freePort(config.debugPort);
            attachPort = port;
            String suspend = config.debugSuspend ? "y" : "n";
            command.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=" + suspend
                    + ",address=127.0.0.1:" + port);
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
        System.out.println("  进服      localhost:" + listenPort + (config.onlineMode ? "（正版验证）" : "（离线验证）"));
        System.out.println("  自检      /vespera doctor");
        System.out.println("  说话      /vespera talk bell-test   或右键出生点旁边那个村民");
        System.out.println("  控制台    在这里直接敲服务器命令，stop 结束");
        if (attachPort > 0) {
            System.out.println("  插件调试  IDEA → Run → Attach to Process → 选 paper.jar（端口 " + attachPort + "）");
            System.out.println("            或 Debug 一个 Remote JVM Debug：127.0.0.1:" + attachPort);
            if (config.debugSuspend) {
                System.out.println("            服务端正停在第一行等你，没连上之前不会继续");
            } else {
                System.out.println("            现在就能连；但 onEnable 已经跑过了，断点只对之后生效");
            }
        }
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
            // 再验一遍房子：八处探针，确认"校区真的盖出来了"而不是只写了个数字。
            console.send("vespera verify");
            console.await("校区校验", Duration.ofSeconds(30));
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

    /**
     * 这个 JVM 自己是不是被调试器启动的。
     *
     * <p>IDEA 的 Debug 会往 JVM 参数里塞一个 {@code -agentlib:jdwp=...}。认出它，
     * 就能做到"在 IDE 里点 Debug 就直接能打断点"，而不是"点 Debug 之后发现断点不生效、
     * 再去查为什么"——后者是所有调试体验里最浪费时间的一种。
     */
    private static boolean isDebuggingThisJvm() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().stream()
                .anyMatch(argument -> argument.contains("jdwp"));
    }

    /**
     * 插件源码比构建产物新时，自动补一次打包。
     *
     * <p>从 IDE 启动时不会自动跑 Maven（只有 {@code ./dev.sh} 会），所以很容易出现
     * "改了代码、起了服务器、断点不生效"——因为跑的还是上一次的 jar。
     * 与其写一段说明让人记住，不如让它自己补上。{@code -Dvespera.build=false} 可以关掉。
     */
    private static void ensureBuilt(Config config) throws IOException, InterruptedException {
        Path staged = config.project.resolve("server/build/plugins/vespera-plugin.jar");
        long newest = newestSource(config.project);
        boolean stale = !Files.isRegularFile(staged)
                || (newest > 0 && Files.getLastModifiedTime(staged).toMillis() < newest);
        if (!stale) {
            return;
        }
        if (!config.build) {
            System.out.println("  注意：插件 jar 缺失或过期，已按 -Dvespera.build=false 跳过打包。");
            return;
        }
        System.out.println("插件 jar 缺失或过期，先打一次包（mvn package -DskipTests）……");
        int code;
        try {
            code = new ProcessBuilder("mvn", "-B", "-q", "-DskipTests", "package")
                    .directory(config.project.toFile())
                    .inheritIO()
                    .start()
                    .waitFor();
        } catch (IOException e) {
            System.out.println("  没能调用 mvn（" + e.getMessage() + "）。请手动跑一次 `mvn package`。");
            return;
        }
        if (code != 0) {
            throw new IllegalStateException("mvn package 失败（退出码 " + code + "），不启动服务器。");
        }
    }

    /** 插件与核心两边的最新改动时间。core 改了也要重打包——它被 shade 进了插件里。 */
    private static long newestSource(Path project) throws IOException {
        long newest = 0;
        for (String module : List.of("vespera-plugin/src", "vespera-core/src")) {
            Path sources = project.resolve(module);
            if (!Files.isDirectory(sources)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(sources)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    newest = Math.max(newest, Files.getLastModifiedTime(file).toMillis());
                }
            }
        }
        return newest;
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

    /**
     * 两套环境，各自一个目录、一个端口。
     *
     * <p>为什么要分开：开发时改代码要重启、要删世界、要挂调试器；自动验证要每次干净、
     * 要能随时被杀掉。这两件事共用一个目录，就一定会互相打断——最典型的是
     * "我在跑冒烟，你的服务器突然被清空了"。
     *
     * <p>所以：目录分开，端口分开，插件 jar 各拷一份。共用的是构建产物与源码，那本来就该共用。
     */
    enum Profile {
        /** 人用的：留着世界，挂着调试器，慢慢来。 */
        DEV("开发", "dev", 25565),
        /** 验收用的：可随时重建世界，可随时杀掉，不碰开发服任何东西。 */
        TEST("测试", "test", 25599);

        private final String label;
        private final String directory;
        private final int port;

        Profile(String label, String directory, int port) {
            this.label = label;
            this.directory = directory;
            this.port = port;
        }

        String label() {
            return label;
        }

        String directory() {
            return directory;
        }

        int port() {
            return port;
        }
    }

    /** 所有开关集中在一处，读起来像一份说明书而不是一堆散落的 getProperty。 */
    private record Config(Path project, Path server, Profile profile, int port, boolean onlineMode,
            boolean keepState, boolean smoke, boolean flat, boolean debug, boolean debugSuspend, int debugPort,
            String seed, String paperVersion, boolean build) {

        static Config from(String[] args) {
            Path project = Path.of(System.getProperty("vespera.projectDir", System.getProperty("user.dir")))
                    .toAbsolutePath().normalize();
            String first = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";

            // smoke 与 test 都进测试环境；其余进开发环境。
            boolean smoke = first.equals("smoke");
            Profile profile = smoke || first.equals("test") ? Profile.TEST : Profile.DEV;

            // 三种情况都算"要调试"：显式参数、系统属性、以及 IDEA 用 Debug 跑起来
            // （后者自己没传属性，但 JVM 参数里带着 jdwp）。这样在 IDE 里点 Debug 就有断点，
            // 不需要先想起来改配置。
            boolean debug = first.equals("debug")
                    || Boolean.getBoolean("vespera.debug")
                    || (!smoke && isDebuggingThisJvm());

            return new Config(
                    project,
                    Path.of(System.getProperty("vespera.dir",
                            project.resolve("server/" + profile.directory()).toString())),
                    profile,
                    Integer.getInteger("vespera.port", profile.port()),
                    Boolean.parseBoolean(System.getProperty("vespera.onlineMode", "true")),
                    // 固定环境的默认是保留世界；一次性冒烟默认重建，保证每次验的是同一块地。
                    Boolean.parseBoolean(System.getProperty("vespera.keepState", Boolean.toString(!smoke))),
                    smoke,
                    // 平世界：看建筑用的。地形是一块干净的白地，房子才看得清。
                    Boolean.parseBoolean(System.getProperty("vespera.flat", "false")),
                    debug,
                    Boolean.getBoolean("vespera.debugSuspend"),
                    Integer.getInteger("vespera.debugPort", 5005),
                    System.getProperty("vespera.seed", String.valueOf(System.nanoTime())),
                    System.getProperty("vespera.paperVersion", "26.3"),
                    Boolean.parseBoolean(System.getProperty("vespera.build", "true")));
        }
    }
}
