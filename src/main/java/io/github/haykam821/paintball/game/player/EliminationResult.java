package io.github.haykam821.paintball.game.player;

import net.minecraft.network.chat.Component;

public record EliminationResult(Component message, boolean revivable) {}
