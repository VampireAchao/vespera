package io.github.vampireachao.vespera.core;

/**
 * 判定条件时能看到的世界状态。
 *
 * <p>只有三样东西：你是哪一族、你身上有没有某个旗标、某个计数到了几。这三样足以表达
 * 设计书里那四层筛选（身份 → 关系 → 状态 → 目击）的全部前提，而多一样都会让剧本
 * 变得没法审。
 *
 * <p>它是个接口，因为"谁来答"在测试里和在服务器上不是同一件事：测试给一张固定的表，
 * 服务器读玩家的持久化数据。
 */
public interface StoryVars {

    /** 种族标识，小写：human / vampire / ghoul / werewolf / demon / priest / witchhunter / angel / enderborn。 */
    String race();

    /** 旗标是否存在。旗标只增不减，删旗标是另一套语义，不在这里。 */
    boolean flag(String name);

    /** 计数。没有这个键时返回 {@code fallback}，而不是 0——"没记过"和"记了 0"要分开。 */
    int num(String name, int fallback);

    /** 一张什么都没记的世界。测试与非玩家语境用它。 */
    static StoryVars empty() {
        return new StoryVars() {
            @Override
            public String race() {
                return "human";
            }

            @Override
            public boolean flag(String name) {
                return false;
            }

            @Override
            public int num(String name, int fallback) {
                return fallback;
            }
        };
    }
}
