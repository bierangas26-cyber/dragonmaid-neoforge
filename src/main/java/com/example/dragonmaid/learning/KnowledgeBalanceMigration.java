package com.example.dragonmaid.learning;

import com.electronwill.nightconfig.core.CommentedConfig;

/** 1.2: 旧版配置默认值的迁移已经完成（DragonMaidConfig 移除了 knowledgeBalanceRevision），保留空实现兼容调用方。 */
public final class KnowledgeBalanceMigration {
    private KnowledgeBalanceMigration() {
    }

    public static boolean migrate(CommentedConfig data) {
        return false;
    }
}
