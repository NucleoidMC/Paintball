package io.github.haykam821.paintball.game.map;

import java.io.IOException;

import io.github.haykam821.paintball.game.PaintballConfig;
import net.minecraft.text.TranslatableText;
import xyz.nucleoid.plasmid.game.GameOpenException;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.map.template.MapTemplateSerializer;

public class PaintballMapBuilder {
	private final PaintballConfig config;

	public PaintballMapBuilder(PaintballConfig config) {
		this.config = config;
	}

	public PaintballMap create() {
		try {
			MapTemplate template = MapTemplateSerializer.INSTANCE.loadFromResource(this.config.getMap());
			return new PaintballMap(template);
		} catch (IOException exception) {
			throw new GameOpenException(new TranslatableText("text.paintball.template_load_failed"), exception);
		}
	}
}