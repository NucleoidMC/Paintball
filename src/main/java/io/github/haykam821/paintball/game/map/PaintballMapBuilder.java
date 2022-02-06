package io.github.haykam821.paintball.game.map;

import java.io.IOException;

import io.github.haykam821.paintball.game.PaintballConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.map_templates.MapTemplateSerializer;
import xyz.nucleoid.plasmid.game.GameOpenException;

public class PaintballMapBuilder {
	private final PaintballConfig config;

	public PaintballMapBuilder(PaintballConfig config) {
		this.config = config;
	}

	public PaintballMap create(MinecraftServer server) {
		try {
			MapTemplate template = MapTemplateSerializer.loadFromResource(server, this.config.getMap());
			return new PaintballMap(template);
		} catch (IOException exception) {
			throw new GameOpenException(new TranslatableText("text.paintball.template_load_failed"), exception);
		}
	}
}