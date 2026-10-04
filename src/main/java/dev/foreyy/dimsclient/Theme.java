package dev.foreyy.dimsclient;

import java.util.List;
import net.minecraft.util.ARGB;

/** Palette the whole GUI reads from, so a theme swap recolours every panel at once. */
public enum Theme {
	EMBER("ember", "Уголь", "Ember",
		0x12110F, 0x1C1915, 0x26211B, 0xD7A15E, 0xE7E2D6, 0x9C9688, 0x8FB56A, 0x3C3428),
	NIGHT("night", "Ночь", "Night",
		0x0E1218, 0x171E29, 0x212A38, 0x7EB6E0, 0xE4EAF2, 0x93A0B0, 0x7ECA9A, 0x2E3A4A),
	PINE("pine", "Хвоя", "Pine",
		0x101613, 0x19211C, 0x24302A, 0x7EA860, 0xE6EEE4, 0x97A894, 0xC9D37A, 0x354438),
	BONE("bone", "Кость", "Bone",
		0x161513, 0x221F1B, 0x2E2924, 0xE8D7B8, 0xF4EFE6, 0xA89F90, 0xC9A46A, 0x463E34),
	MATEGLASS("mateglass", "MateGlass", "MateGlass",
		0x0C1016, 0x141A22, 0x1C242E, 0xB7D4F0, 0xF2F6FB, 0xA9B6C6, 0x9FD0C4, 0x6E849C);

	public static final List<Theme> ALL = List.of(values());

	public final String id;
	public final String nameRu;
	public final String nameEn;
	public final int background;
	public final int panel;
	public final int row;
	public final int accent;
	public final int text;
	public final int muted;
	public final int on;
	public final int edge;

	Theme(String id, String nameRu, String nameEn, int background, int panel, int row, int accent, int text, int muted,
			int on, int edge) {
		this.id = id;
		this.nameRu = nameRu;
		this.nameEn = nameEn;
		this.background = ARGB.color(235, background);
		this.panel = ARGB.color(245, panel);
		this.row = ARGB.color(255, row);
		this.accent = ARGB.color(255, accent);
		this.text = ARGB.color(255, text);
		this.muted = ARGB.color(255, muted);
		this.on = ARGB.color(255, on);
		this.edge = ARGB.color(255, edge);
	}

	public int rowHover() {
		return ARGB.scaleRGB(row, 1.28F);
	}

	public int off() {
		return ARGB.color(255, 0xC4685A);
	}

	public String name(boolean russian) {
		return russian ? nameRu : nameEn;
	}

	/** Frosted panel: the world shows through instead of a solid fill. */
	public boolean glass() {
		return this == MATEGLASS;
	}

	public static Theme byId(String id) {
		for (Theme theme : ALL) {
			if (theme.id.equals(id)) {
				return theme;
			}
		}
		return EMBER;
	}
}
