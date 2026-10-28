package io.github.vampireachao.vespera.plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 内容闸门：哪些线在这个服务器上**存在**。
 *
 * <p>它不是"敏感词过滤器"，也不是"打码"。它决定一段内容是否被生成——关着的线不会出现在
 * 客户端，也不会出现在剧本加载日志里。区别很重要：
 *
 * <ul>
 *   <li>过滤是"有，但你看不见"——玩家会发现一块空白，然后开始猜。</li>
 *   <li>闸门是"没有"——没有空白，没有提示，没有可猜的东西。</li>
 * </ul>
 *
 * <p>默认值全是关。三个理由：
 *
 * <ol>
 *   <li><b>合规优先。</b>出问题的代价远大于少一段剧情的遗憾。默认关，要开必须显式改文件。</li>
 *   <li><b>写错也不漏。</b>闸门名拼错、名字改了、配置没读到——结果都是"不显示"，
 *       而不是"显示了不该显示的"。失败方向必须是安全的那一边。</li>
 *   <li><b>作者一个人玩。</b>只有你会打开它，那就没有"别人等着看"的压力。</li>
 * </ol>
 */
public final class ContentPolicy {

    private final VesperaPlugin plugin;
    private final Map<String, Boolean> gates = new LinkedHashMap<>();

    public ContentPolicy(VesperaPlugin plugin) {
        this.plugin = plugin;
    }

    /** 读 {@code content.yml}。读不到、读坏了、键写错了，结果一律是"全关"。 */
    public void load() {
        gates.clear();
        File file = new File(plugin.getDataFolder(), "content.yml");
        if (!file.isFile()) {
            plugin.getLogger().warning("没有 content.yml，所有内容闸门按关闭处理。");
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = yaml.getConfigurationSection("gates");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            gates.put(key, section.getBoolean(key, false));
        }
    }

    /**
     * 这道闸门现在通不通。
     *
     * <p>{@code null} 表示这段内容不设闸（日常对话、教务、跑腿），应当通行。
     * 未登记的闸门名一律不通——拼错字不该变成"意外开放"。
     */
    public boolean allows(String gate) {
        if (gate == null || gate.isBlank()) {
            return true;
        }
        return gates.getOrDefault(gate, false);
    }

    public Map<String, Boolean> gates() {
        return Map.copyOf(gates);
    }

    /** 给 {@code /vespera doctor} 的一行。开着的会点名，关着的只说数量。 */
    public String describe() {
        if (gates.isEmpty()) {
            return "未配置（全部关闭）";
        }
        List<String> open = gates.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
        if (open.isEmpty()) {
            return gates.size() + " 道，全部关闭";
        }
        return gates.size() + " 道，开启：" + String.join("、", open);
    }
}
