package dev.foreyy.dimsclient;

import dev.foreyy.dimsclient.chase.Chase;
import dev.foreyy.dimsclient.mine.AutoMine;
import dev.foreyy.dimsclient.teleport.Teleport;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

/**
 * One window: modules up top, how the window looks underneath.
 * Everything is drawn by hand so the palette can swap without touching widget textures.
 */
public final class DimsScreen extends Screen {
	private static final int BASE_WIDTH = 470;
	private static final int BASE_HEIGHT = 292;

	private final Screen parent;
	private final List<Row> rows = new ArrayList<>();
	private final List<Note> notes = new ArrayList<>();
	private Page page = Page.STUN;

	private int panelX;
	private int panelY;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;
	private KeyMapping listening;
	private boolean typingCoords;
	private String coordsDraft = "";
	private float scroll;
	private float scrollTarget;
	private int contentHeight;
	private float tabScroll;
	private float tabScrollTarget;
	private long openedAt;

	public DimsScreen(Screen parent) {
		super(Component.literal("DimSClient"));
		this.parent = parent;
	}

	private DimsConfig config() {
		return DimsClient.CONFIG;
	}

	private Theme theme() {
		return Theme.byId(config().theme);
	}

	private float scale() {
		return config().scale / 100.0F;
	}

	private int panelWidth() {
		return Math.round(BASE_WIDTH * scale());
	}

	private int panelHeight() {
		return Math.round(BASE_HEIGHT * scale());
	}

	@Override
	protected void init() {
		openedAt = System.currentTimeMillis();
		int width = panelWidth();
		int height = panelHeight();
		if (config().panelX < 0) {
			panelX = (this.width - width) / 2;
			panelY = (this.height - height) / 2;
		} else {
			panelX = config().panelX;
			panelY = config().panelY;
		}
		clampPanel();
		layout();
	}

	private void layout() {
		this.clearWidgets();
		rows.clear();
		notes.clear();
		float scale = scale();
		int rowHeight = Math.round(22 * scale);
		int gap = Math.round(4 * scale);
		int x = panelX + Math.round(122 * scale);
		int y = panelY + Math.round(58 * scale);
		int rowWidth = panelWidth() - Math.round(136 * scale);

		switch (page) {
			case STUN -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().stunEnabled, value -> config().stunEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Дальность", "Range", DimsConfig.STUN_RANGES,
					() -> config().stunRange, value -> String.format("%.1f", value), value -> config().stunRange = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Интервал", "Interval", DimsConfig.STUN_INTERVALS,
					() -> config().stunIntervalTicks,
					value -> String.format("%.1f %s", value / 20.0F, config().t("сек", "sec")),
					value -> config().stunIntervalTicks = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Бьёт топором в щит, пока прицел на игроке. Топор должен лежать на хотбаре.",
					"Swings an axe into a shield while the crosshair is on a player. Keep an axe on the hotbar."));
			}
				case CHEST -> {
					toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().chestEnabled, value -> config().chestEnabled = value);
					note(x, y + rowHeight + gap + 6, rowWidth, config().t(
						"Правый клик открывает сундук под прицелом, даже если перед ним стена. Дальше, чем игра позволяет дотянуться, не откроется.",
						"Right click opens the chest under the crosshair even when a wall stands in front of it. Past the game's own reach it will not open."));
				}
			case CART -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().cartEnabled, value -> config().cartEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Упреждение", "Lead", DimsConfig.CART_LEADS,
					() -> config().cartLeadTicks, value -> value + config().t(" тиков", " ticks"), value -> config().cartLeadTicks = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Дальность", "Range", DimsConfig.CART_RANGES,
					() -> config().cartRange, value -> ((int) Math.round(value)) + config().t(" блоков", " blocks"),
					value -> config().cartRange = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Оставлять слот", "Keep slot", () -> config().cartKeepSlot, value -> config().cartKeepSlot = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Плавный прицел", "Smooth aim", () -> config().cartSmoothAim, value -> config().cartSmoothAim = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Скорость прицела", "Aim speed", DimsConfig.CART_AIM_SPEEDS,
					() -> config().cartAimSpeed, String::valueOf, value -> config().cartAimSpeed = value);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Режим", "Mode",
					() -> config().cartLegit ? "Legit" : "Unlegit",
					() -> config().cartLegit ? theme().on : theme().accent,
					() -> config().cartLegit = !config().cartLegit);
			}
				case XBOW -> {
					toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().xbowEnabled, value -> config().xbowEnabled = value);
					y += rowHeight + gap;
					bind(x, y, rowWidth, rowHeight, "Кнопка", "Key", DimsClient.XBOW);
					y += rowHeight + gap;
					toggle(x, y, rowWidth, rowHeight, "Огонь", "Fire", () -> config().xbowFire, value -> config().xbowFire = value);
					note(x, y + rowHeight + gap + 6, rowWidth, config().t(
						"По кнопке ставит рельсы в трёх блоках по взгляду, на них TNT-вагонетку и стреляет арбалетом. С огнём сначала поджигает огнивом блок перед рельсами.",
						"On its key it lays rails three blocks along your look, a TNT minecart on them, then fires a crossbow. With fire on, a flint and steel lights the block in front of the rail first."));
				}
			case TOTEM -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().totemEnabled, value -> config().totemEnabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Если во второй руке нет тотема, берёт его из инвентаря.",
					"If the offhand has no totem, it takes one from the inventory."));
			}
			case INVIS -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().seeInvisibleEnabled, value -> config().seeInvisibleEnabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Игроки с невидимостью рисуются как обычно. Сам эффект у них остаётся.",
					"Players with invisibility are drawn as usual. The effect stays on them."));
			}
			case RADAR -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().locatorEnabled, value -> config().locatorEnabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Полоска над хотбаром показывает игроков, даже если локатор на сервере выключен.",
					"The bar above the hotbar shows players even when the server locator is off."));
			}
			case THEME -> {
				for (Theme theme : Theme.ALL) {
					boolean selected = theme == theme();
					choice(x, y, rowWidth, rowHeight, theme.name(config().russian), theme.name(config().russian),
						() -> selected ? config().t("выбрана", "selected") : "",
						() -> selected ? theme.accent : theme().muted,
						() -> config().theme = theme.id);
					y += rowHeight + gap;
				}
			}
			case GLOW -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().glowEnabled, value -> config().glowEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Дальность", "Range", DimsConfig.GLOW_RANGES,
					() -> config().glowRange, value -> value + " " + config().t("блоков", "blocks"), value -> config().glowRange = value);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Вид", "Look",
					() -> switch (config().glowStyle) {
						case 0 -> config().t("контур", "outline");
						case 1 -> config().t("заливка", "fill");
						default -> config().t("контур и заливка", "outline and fill");
					},
					() -> theme().text,
					() -> config().glowStyle = (config().glowStyle + 1) % 3);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Прозрачность заливки", "Fill opacity", DimsConfig.GLOW_ALPHAS,
					() -> config().glowFillAlpha, value -> value * 100 / 255 + "%", value -> config().glowFillAlpha = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Прозрачность контура", "Outline opacity", DimsConfig.GLOW_ALPHAS,
					() -> config().glowLineAlpha, value -> value * 100 / 255 + "%", value -> config().glowLineAlpha = value);
				y += rowHeight + gap;
				y = swatch(x, y, rowWidth, rowHeight, gap, "Сундуки", "Chests", () -> config().glowChests, value -> config().glowChests = value, () -> config().glowChestColor, value -> config().glowChestColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Уголь", "Coal", () -> config().glowCoal, value -> config().glowCoal = value, () -> config().glowCoalColor, value -> config().glowCoalColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Железо", "Iron", () -> config().glowIron, value -> config().glowIron = value, () -> config().glowIronColor, value -> config().glowIronColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Медь", "Copper", () -> config().glowCopper, value -> config().glowCopper = value, () -> config().glowCopperColor, value -> config().glowCopperColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Золото", "Gold", () -> config().glowGold, value -> config().glowGold = value, () -> config().glowGoldColor, value -> config().glowGoldColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Редстоун", "Redstone", () -> config().glowRedstone, value -> config().glowRedstone = value, () -> config().glowRedstoneColor, value -> config().glowRedstoneColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Лазурит", "Lapis", () -> config().glowLapis, value -> config().glowLapis = value, () -> config().glowLapisColor, value -> config().glowLapisColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Алмаз", "Diamond", () -> config().glowDiamond, value -> config().glowDiamond = value, () -> config().glowDiamondColor, value -> config().glowDiamondColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Изумруд", "Emerald", () -> config().glowEmerald, value -> config().glowEmerald = value, () -> config().glowEmeraldColor, value -> config().glowEmeraldColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Кварц", "Quartz", () -> config().glowQuartz, value -> config().glowQuartz = value, () -> config().glowQuartzColor, value -> config().glowQuartzColor = value);
				y = swatch(x, y, rowWidth, rowHeight, gap, "Обломки", "Debris", () -> config().glowDebris, value -> config().glowDebris = value, () -> config().glowDebrisColor, value -> config().glowDebrisColor = value);
				swatch(x, y, rowWidth, rowHeight, gap, "Прочие руды", "Other ores", () -> config().glowOther, value -> config().glowOther = value, () -> config().glowOtherColor, value -> config().glowOtherColor = value);
			}
			case FLY -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().flyEnabled, value -> config().flyEnabled = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Свой прицел", "Own crosshair", () -> config().flyOwnCrosshair, value -> config().flyOwnCrosshair = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Обычный полёт, как в креативе. Двойной прыжок поднимает, шифт опускает. Свой прицел целится от тела, так что блоки ломаются как на земле.",
					"Vanilla flight, like creative. Double jump lifts, sneak drops. Own crosshair aims from the body, so blocks break the way they do on the ground."));
			}
			case WET -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().wetEnabled, value -> config().wetEnabled = value);
				y += rowHeight + gap;
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Каждая открытая грань темнеет и ловит блик, как мокрый камень. Пол, стены и потолок тоже. Мир при этом остаётся сухим.",
					"Every exposed face darkens and catches a glint, like wet stone. Floors, walls and ceilings too. The world stays dry."));
			}
			case LAVA -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().clearLavaEnabled, value -> config().clearLavaEnabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"В лаве камера не заливается оранжевым. Сам урон от лавы остаётся.",
					"The camera stays clear in lava. The lava still burns."));
			}
			case F5 -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().freeF5Enabled, value -> config().freeF5Enabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"В F5 мышь крутит камеру вокруг, взгляд персонажа остаётся на месте.",
					"In F5 the mouse spins the camera around. The character keeps looking where it was."));
			}
			case DARK -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().noDarknessEnabled, value -> config().noDarknessEnabled = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Эффект тьмы не затягивает экран туманом. Сам эффект на игроке остаётся.",
					"Darkness no longer pulls fog over the screen. The effect stays on you."));
			}
			case TP -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().teleportEnabled, value -> config().teleportEnabled = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Сразу", "Blink", () -> config().teleportBlink, value -> config().teleportBlink = value);
				y += rowHeight + gap;
				coords(x, y, rowWidth, rowHeight);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Телепорт", "Teleport",
					() -> "",
					() -> theme().accent,
					() -> {
						if (minecraft.player != null) {
							Teleport.go(minecraft.player);
						}
					});
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Кнопка", "Key", DimsClient.TELEPORT);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Три числа через пробел или запятую: x y z. Сразу переносит одним пакетом после удара булавой в падении по любой цели рядом.",
					"Three numbers, spaces or commas: x y z. Blink moves you there in one packet, after a mace smash on anything nearby while falling."));
			}
			case CAM -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().freeCamEnabled, value -> config().freeCamEnabled = value);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Камера", "Camera", DimsClient.FREE_CAM);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "FreeCamTP", "FreeCamTP", () -> config().freeCamTeleport, value -> config().freeCamTeleport = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Камера летит сквозь всё, персонаж остаётся стоять. FreeCamTP переносит его туда, где камера была в момент выключения, за пару тиков.",
					"The camera flies through everything while you stay put. FreeCamTP puts you where the camera was when it closed, in a couple of ticks."));
			}
			case AIR -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().airEnabled, value -> config().airEnabled = value);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Зависнуть", "Hold", DimsClient.AIR_HOLD);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Упасть", "Drop", DimsClient.AIR_DROP);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Высота", "Height", DimsConfig.AIR_HEIGHTS,
					() -> config().airHeight, value -> value + " " + config().t("блоков", "blocks"),
					value -> config().airHeight = value);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Вверх", "Up", DimsClient.AIR_UP);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Зависаешь в воздухе. Для сервера ты продолжаешь падать, и с этой высоты булава бьёт как с падения. «Упасть» отпускает всю накопленную скорость. «Вверх» телепортирует на выбранную высоту.",
					"You hang in the air. The server still sees you falling, so a mace hits as if you fell from that height. Drop releases everything stored. Up teleports by the chosen height."));
			}
			case ELYTRA -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().elytraEnabled, value -> config().elytraEnabled = value);
				y += rowHeight + gap;
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"На элитрах кружит вокруг игрока на скорости фейерверка и бьёт его. Ракета не нужна. Цель та же, что у KillAura.",
					"On elytra it circles a player at firework speed and hits them. No rocket. Same target as KillAura."));
			}
			case SEARCH -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().searchEnabled, value -> config().searchEnabled = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Деревня", "Village", () -> config().searchVillage, value -> config().searchVillage = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Храм", "Temple", () -> config().searchTemple, value -> config().searchTemple = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Аванпост", "Outpost", () -> config().searchOutpost, value -> config().searchOutpost = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Монумент", "Monument", () -> config().searchMonument, value -> config().searchMonument = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Корабль", "Ship", () -> config().searchShip, value -> config().searchShip = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Портал", "Portal", () -> config().searchPortal, value -> config().searchPortal = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Крепость", "Fortress", () -> config().searchFortress, value -> config().searchFortress = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Бастион", "Bastion", () -> config().searchBastion, value -> config().searchBastion = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Эндер-город", "End city", () -> config().searchEnd, value -> config().searchEnd = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Испытания", "Trial", () -> config().searchTrail, value -> config().searchTrail = value);
			}
			case GOD -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().godEnabled, value -> config().godEnabled = value);
				y += rowHeight + gap;
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Урон не применяется. Смерть всё равно наступает, если сервер доводит здоровье до нуля.",
					"Damage is not applied. You still die if the server drops your health to zero."));
			}
			case SEED -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().seedEnabled, value -> config().seedEnabled = value);
				y += rowHeight + gap;
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Сид появляется над списком модулей, когда рядом загружены структуры. Если мир не ванильный, напишет, что генерация кастомная.",
					"The seed shows above the module list once structures are loaded nearby. A world that is not vanilla says the generation is custom."));
			}
			case NOFALL -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().noFallEnabled, value -> config().noFallEnabled = value);
				y += rowHeight + gap;
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Падение не наносит урон, что бы ты ни делал. На элитрах всё как обычно.",
					"Falling does no damage, whatever you are doing. Elytra stays as it is."));
			}
			case SPEED -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().speedEnabled, value -> config().speedEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Множитель", "Multiplier", DimsConfig.SPEED_MULTIPLIERS,
					() -> config().speedMultiplier, value -> "x" + (value == Math.rint(value) ? String.valueOf((int) Math.rint(value)) : String.valueOf(value)),
					value -> config().speedMultiplier = value);
			}
			case CHASE -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().chaseEnabled, value -> config().chaseEnabled = value);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Режим", "Mode",
					() -> config().chaseLegit ? config().t("Легит", "Legit") : config().t("Анлегит", "Unleashed"),
					() -> theme().text,
					() -> config().chaseLegit = !config().chaseLegit);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Кнопка", "Key", DimsClient.CHASE);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Голые", "Unarmored", () -> config().chaseNaked, value -> config().chaseNaked = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "В броне", "Armored", () -> config().chaseArmored, value -> config().chaseArmored = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Мобы", "Mobs", () -> config().chaseMobs, value -> config().chaseMobs = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Боссы", "Bosses", () -> config().chaseBosses, value -> config().chaseBosses = value);
				y += rowHeight + gap;
					choice(x, y, rowWidth, rowHeight, "AlwaysFriends", "AlwaysFriends",
					() -> config().chaseFriends.isEmpty() ? config().t("нет", "none") : config().chaseFriends,
					() -> theme().on,
					() -> config().chaseFriends = addLooked(config().chaseFriends));
				y += rowHeight + gap;
					choice(x, y, rowWidth, rowHeight, "AlwaysEnemies", "AlwaysEnemies",
					() -> config().chaseEnemies.isEmpty() ? config().t("нет", "none") : config().chaseEnemies,
					() -> theme().off(),
					() -> config().chaseEnemies = addLooked(config().chaseEnemies));
				y += rowHeight + gap;
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Цель", "Target",
					() -> config().chaseName.isEmpty() ? config().t("ближайший", "nearest") : config().chaseName,
					() -> theme().text,
					() -> {
						String looked = Chase.lookedAtName(minecraft);
						config().chaseName = looked.isEmpty() ? "" : looked;
					});
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Скорость анлегита", "Unleashed speed", DimsConfig.SPEED_MULTIPLIERS,
					() -> config().chaseSpeed, value -> "x" + (value == Math.rint(value) ? String.valueOf((int) Math.rint(value)) : String.valueOf(value)),
					value -> config().chaseSpeed = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Скорость легита", "Legit speed", DimsConfig.SPEED_MULTIPLIERS,
					() -> config().chaseLegitSpeed, value -> "x" + (value == Math.rint(value) ? String.valueOf((int) Math.rint(value)) : String.valueOf(value)),
					value -> config().chaseLegitSpeed = value);
					y += rowHeight + gap;
					cycle(x, y, rowWidth, rowHeight, "Дальность", "Range", DimsConfig.CHASE_RANGES,
						() -> config().chaseRange, value -> value + " " + config().t("блоков", "blocks"),
						value -> config().chaseRange = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Анлегит наводится и кружит вокруг цели. Легит идёт следом и бьёт критом. AlwaysFriends не бьются никогда, AlwaysEnemies бьются всегда. Клик по пустому прицелу очищает список.",
					"Unleashed aims and circles the target. Legit walks after them and crits. AlwaysFriends are never hit, AlwaysEnemies always are. Clicking with nobody aimed clears the list."));
			}
			case BARITONE -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().baritoneEnabled, value -> config().baritoneEnabled = value);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Дорога", "Travel",
					() -> config().baritoneElytra ? config().t("элитра", "elytra") : config().t("пешком", "on foot"),
					() -> theme().text,
					() -> config().baritoneElytra = !config().baritoneElytra);
				y += rowHeight + gap;
				coords(x, y, rowWidth, rowHeight);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Идти", "Go",
					() -> "",
					() -> theme().accent,
					() -> {
						if (config().baritoneElytra) {
							dev.foreyy.dimsclient.baritone.Baritone.fly(minecraft);
						} else {
							dev.foreyy.dimsclient.baritone.Baritone.walk(minecraft);
						}
					});
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Стоп", "Stop",
					() -> "",
					() -> theme().off(),
					() -> dev.foreyy.dimsclient.baritone.Baritone.stop(minecraft));
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Идёт к координатам из поля пешком или на элитрах. Когда AutoFarm включён, сам копает выбранный там блок и подбирает выпавшее.",
					"Walks or flies an elytra to the typed coordinates. With AutoFarm on, it mines the block picked there and picks up the drops."));
			}
			case MINE -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().mineEnabled, value -> config().mineEnabled = value);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Блок", "Block",
					() -> config().mineBlock.isEmpty() ? config().t("любая руда", "any ore") : config().mineBlock,
					() -> theme().text,
					() -> AutoMine.pickLookedAt(minecraft));
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Слот", "Slot", List.of(0, 1, 2, 3, 4, 5, 6, 7, 8),
					() -> config().mineSlot, value -> String.valueOf(value + 1), value -> config().mineSlot = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Роет туннель 1x2 по взгляду и забирает руду рядом. Посмотри на блок и нажми «Блок», чтобы искать только его. Пусто — любая руда. Инструмент в выбранном слоте.",
					"Digs a 1x2 tunnel along your look and takes ore beside it. Look at a block and press Block to hunt only that one. Empty means any ore. The tool sits in the chosen slot."));
			}
			case DASH -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().dashEnabled, value -> config().dashEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Пауза", "Cooldown", DimsConfig.DASH_COOLDOWNS,
					() -> config().dashCooldownTicks,
					value -> String.format("%.1f %s", value / 20.0F, config().t("сек", "sec")),
					value -> config().dashCooldownTicks = value);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Кнопка рывка", "Dash key", DimsClient.DASH);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Рывок как Lunge III: по взгляду, без копья и без удара.",
					"A Lunge III dash along your look. No spear, and it does not hit."));
			}
			case HITBOX -> {
				toggle(x, y, rowWidth, rowHeight, "Включить", "Enabled", () -> config().hitboxEnabled, value -> config().hitboxEnabled = value);
				y += rowHeight + gap;
				cycle(x, y, rowWidth, rowHeight, "Размер", "Size", DimsConfig.HITBOX_EXPANDS,
					() -> config().hitboxExpand, value -> String.format("+%.1f", value), value -> config().hitboxExpand = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Показывать в F3+B", "Show in F3+B", () -> config().hitboxVisible, value -> config().hitboxVisible = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Прицел берёт всех шире их хитбокса. В F3+B видно рамку, если показ включён.",
					"The crosshair catches everyone past their hitbox. F3+B draws the frame when showing is on."));
			}
			case WIDGETS -> {
				toggle(x, y, rowWidth, rowHeight, "Панель", "Panel", () -> config().widgetPanel, value -> config().widgetPanel = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "FPS и пинг", "FPS and ping", () -> config().widgetFps, value -> config().widgetFps = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Стрелки игроков", "Player arrows", () -> config().widgetPlayers, value -> config().widgetPlayers = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Время", "Time", () -> config().widgetTime, value -> config().widgetTime = value);
				y += rowHeight + gap;
				toggle(x, y, rowWidth, rowHeight, "Координаты", "Coordinates", () -> config().widgetCoords, value -> config().widgetCoords = value);
				note(x, y + rowHeight + gap + 6, rowWidth, config().t(
					"Таблички тащатся кнопкой оверлея. Стрелки сидят вокруг прицела и показывают координаты игрока.",
					"Cards drag with the overlay key. Arrows sit around the crosshair and show that player's coordinates."));
			}
			case STYLE -> {
				cycle(x, y, rowWidth, rowHeight, "Размер окна", "Window size", List.of(80, 90, 100, 110, 120, 130),
					() -> config().scale, value -> value + "%", value -> {
						config().scale = value;
						rescaleAroundCenter();
					});
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Открыть меню", "Open menu", DimsClient.OPEN);
				bind(x, y, rowWidth, rowHeight, "Скрыть клиент", "Hide client", DimsClient.HIDE);
				y += rowHeight + gap;
				bind(x, y, rowWidth, rowHeight, "Двигать оверлей", "Move overlay", DimsClient.MOVE_HUD);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Язык", "Language",
					() -> config().russian ? "Русский" : "English", () -> theme().accent,
					() -> config().russian = !config().russian);
				y += rowHeight + gap;
				choice(x, y, rowWidth, rowHeight, "Вернуть окно в центр", "Recenter window",
					() -> "", () -> theme().muted, this::centerPanel);
				}
			}
			contentHeight = y - (panelY + Math.round(58 * scale));
			for (Row row : rows) {
				contentHeight = Math.max(contentHeight, row.homeY + row.getHeight() - (panelY + Math.round(58 * scale)));
			}
			if (!notes.isEmpty()) {
				contentHeight += 36;
			}
			clampScroll();
			for (Row row : rows) {
				row.setY(Math.round(row.homeY - scroll));
			}
		}

	/** Keeps the window where it was when the scale slider changes, instead of jumping to a corner. */
	private void rescaleAroundCenter() {
		int centerX = panelX + panelWidth() / 2;
		int centerY = panelY + panelHeight() / 2;
		panelX = centerX - panelWidth() / 2;
		panelY = centerY - panelHeight() / 2;
		clampPanel();
		layout();
	}

	private void centerPanel() {
		config().panelX = -1;
		config().panelY = -1;
		panelX = (this.width - panelWidth()) / 2;
		panelY = (this.height - panelHeight()) / 2;
		layout();
	}

	private int swatch(int x, int y, int width, int height, int gap, String ru, String en,
			BooleanSupplier get, Consumer<Boolean> set, java.util.function.IntSupplier color, java.util.function.IntConsumer apply) {
		toggle(x, y, width - 28, height, ru, en, get, set);
		choice(x + width - 24, y, 24, height, "", "",
			() -> "\u25a0",
			() -> 0xFF000000 | color.getAsInt(),
			() -> apply.accept(DimsConfig.nextGlowColor(color.getAsInt())));
		return y + height + gap;
	}

	private void toggle(int x, int y, int width, int height, String ru, String en, BooleanSupplier get, Consumer<Boolean> set) {
		Toggle row = new Toggle(x, y, width, height, () -> config().t(ru, en), get, set);
		rows.add(row);
		this.addRenderableWidget(row);
	}

	private <T> void cycle(int x, int y, int width, int height, String ru, String en, List<T> values, Supplier<T> current,
			java.util.function.Function<T, String> format, Consumer<T> apply) {
		choice(x, y, width, height, ru, en,
			() -> format.apply(current.get()),
			() -> theme().text,
			() -> {
				int index = values.indexOf(current.get());
				apply.accept(values.get((index + 1) % values.size()));
			});
	}

	private void bind(int x, int y, int width, int height, String ru, String en, KeyMapping mapping) {
		choice(x, y, width, height, ru, en,
			() -> listening == mapping ? "..." : mapping.getTranslatedKeyMessage().getString(),
			() -> listening == mapping ? theme().accent : theme().text,
			() -> listening = mapping);
	}

	private void choice(int x, int y, int width, int height, String ru, String en, Supplier<String> value,
			Supplier<Integer> valueColor, Runnable action) {
		Row row = new Row(x, y, width, height, () -> config().t(ru, en), value, valueColor, action);
		rows.add(row);
		this.addRenderableWidget(row);
	}

	private void note(int x, int y, int width, String text) {
		notes.add(new Note(x, y, width, text));
	}

	private void coords(int x, int y, int width, int height) {
		typingCoords = false;
		coordsDraft = config().teleportCoords;
		choice(x, y, width, height, "Координаты", "Coordinates",
			() -> {
				String shown = typingCoords ? coordsDraft : config().teleportCoords;
				if (shown.isEmpty()) {
					return config().t("нажми и введи", "click and type");
				}
				return shown + (typingCoords ? "_" : "");
			},
			() -> typingCoords ? theme().accent : theme().text,
			() -> {
				typingCoords = true;
				coordsDraft = config().teleportCoords;
			});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		settleScroll();
		Theme theme = theme();
		float open = openAmount();
		float grow = 0.92F + 0.08F * open;
		int fullWidth = panelWidth();
		int fullHeight = panelHeight();
		int width = Math.round(fullWidth * grow);
		int height = Math.round(fullHeight * grow);
		int drawX = panelX + (fullWidth - width) / 2;
		int drawY = panelY + (fullHeight - height) / 2;
		float scale = scale();
		int sidebar = Math.round(110 * scale);
		int shiftX = drawX - panelX;
		int shiftY = drawY - panelY;
		int savedX = panelX;
		int savedY = panelY;
		panelX = drawX;
		panelY = drawY;
		for (Row row : rows) {
			row.setX(row.homeX + shiftX);
			row.setY(Math.round(row.homeY - scroll) + shiftY);
		}
		for (Note note : notes) {
			note.x = note.homeX + shiftX;
		}

		graphics.fill(panelX, panelY, panelX + width, panelY + height, fade(theme.glass() ? glass(theme.background, 150) : theme.background, open));
		graphics.fill(panelX, panelY, panelX + width, panelY + 2, fade(theme.accent, open));
		graphics.fill(panelX, panelY, panelX + sidebar, panelY + height, fade(theme.glass() ? glass(theme.panel, 130) : theme.panel, open));
		graphics.fill(panelX + sidebar, panelY, panelX + sidebar + 1, panelY + height, fade(theme.edge, open));

		int titleY = panelY + Math.round(12 * scale);
		graphics.text(this.font, "DimS", panelX + Math.round(14 * scale), titleY, fade(theme.accent, open), false);
		graphics.text(this.font, "CLIENT", panelX + Math.round(14 * scale) + this.font.width("DimS") + 4, titleY, fade(theme.text, open), false);

		int tabTop = panelY + Math.round(34 * scale);
		int tabBottom = panelY + height - Math.round(8 * scale);
		graphics.enableScissor(panelX, tabTop, panelX + sidebar, tabBottom);
		int tabY = tabTop + Math.round(6 * scale - tabScroll);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.STUN, config().t("МОДУЛИ", "MODULES"), true, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.HITBOX, "Hitboxes", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.DASH, "Dash", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.MINE, "AutoFarm", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.BARITONE, "Baritone", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.CHASE, "KillAura", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.SPEED, "Speed", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.NOFALL, "NoFall", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.SEED, "SeedCracker", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.GOD, "GodMode", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.ELYTRA, "ElytraTarget", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.AIR, "AirStuck", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.CAM, "FreeCam", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.TP, "Teleport", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.FLY, "Fly", false, open);
		tabY += Math.round(10 * scale);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.GLOW, "Render", true, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.GLOW, "XRay", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.SEARCH, "AutoSearch", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.WET, "Wet", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.LAVA, "ClearLava", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.DARK, "NoDarkness", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.F5, "FreeF5", false, open);
		tabY += Math.round(10 * scale);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.STUN, "Misc", true, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.STUN, "AutoStunSlam", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.CART, "AutoCart", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.CHEST, "ChestReach", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.XBOW, "AutoXBow", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.TOTEM, "AutoTotem", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.INVIS, "SeeInvisible", false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.RADAR, "Locator", false, open);
		tabY += Math.round(10 * scale);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.THEME, config().t("ВИД", "LOOK"), true, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.THEME, config().t("Тема", "Theme"), false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.STYLE, config().t("Интерфейс", "Interface"), false, open);
		tabY = tab(graphics, theme, scale, sidebar, tabY, Page.WIDGETS, config().t("Виджеты", "Widgets"), false, open);
		graphics.disableScissor();
		int tabContent = tabY - (tabTop + Math.round(6 * scale - tabScroll));
		int tabMax = Math.max(0, tabContent - (tabBottom - tabTop) + Math.round(6 * scale));
		tabScrollTarget = Mth.clamp(tabScrollTarget, 0.0F, tabMax);
		tabScroll = Mth.clamp(tabScroll, 0.0F, tabMax);
		if (tabMax > 0) {
			int track = tabBottom - tabTop;
			int thumb = Math.max(Math.round(16 * scale), track * track / tabContent);
			int thumbY = tabTop + Math.round((track - thumb) * tabScroll / tabMax);
				graphics.fill(panelX + sidebar - Math.round(4 * scale), thumbY, panelX + sidebar - Math.round(2 * scale), thumbY + thumb, fade(theme.accent, open));
		}

		int contentX = panelX + sidebar + Math.round(14 * scale);
		graphics.text(this.font, page.title(config()), contentX, panelY + Math.round(16 * scale), fade(theme.text, open), false);
		graphics.text(this.font, page.subtitle(config()), contentX, panelY + Math.round(30 * scale), fade(theme.muted, open), false);

		String hint = config().t("ПКМ — закрыть", "RMB — close");
		graphics.text(this.font, hint, panelX + width - Math.round(12 * scale) - this.font.width(hint),
			panelY + height - Math.round(16 * scale), fade(theme.muted, open), false);

		int contentTop = panelY + Math.round(52 * scale);
		int contentBottom = panelY + height - Math.round(26 * scale);
		graphics.enableScissor(panelX + sidebar, contentTop, panelX + width, contentBottom);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		for (Note note : notes) {
			note.y = Math.round(note.homeY - scroll) + shiftY;
			note.draw(graphics, theme, open);
		}
		graphics.disableScissor();
		if (contentHeight > contentBottom - contentTop) {
			int trackHeight = contentBottom - contentTop;
			int thumb = Math.max(Math.round(16 * scale), trackHeight * trackHeight / contentHeight);
			int travel = Math.max(1, contentHeight - trackHeight);
			int thumbY = contentTop + Math.round((trackHeight - thumb) * scroll / travel);
				graphics.fill(panelX + width - Math.round(5 * scale), thumbY, panelX + width - Math.round(2 * scale), thumbY + thumb, fade(theme.accent, open));
		}
		panelX = savedX;
		panelY = savedY;
		for (Row row : rows) {
			row.setX(row.homeX);
			row.setY(Math.round(row.homeY - scroll));
		}
		for (Note note : notes) {
			note.x = note.homeX;
			note.y = Math.round(note.homeY - scroll);
		}
	}

	private float openAmount() {
		float t = (System.currentTimeMillis() - openedAt) / 220.0F;
		t = Math.max(0.0F, Math.min(1.0F, t));
		return 1.0F - (1.0F - t) * (1.0F - t);
	}

	/** Keeps the colour and only eases the alpha, so the window fades in with the grow. */
	/** Keeps the colour and drops the alpha, which is what makes the panel read as glass. */
	private static int glass(int color, int alpha) {
		return net.minecraft.util.ARGB.color(alpha, color);
	}

	/** A setting card. Rounded when the glass look is on, flat otherwise. */
	private void card(GuiGraphicsExtractor graphics, Theme theme, int x, int y, int width, int height, boolean over, float open) {
		int color = over ? theme.rowHover() : theme.row;
		graphics.fill(x, y, x + width, y + height, fade(over ? theme.rowHover() : theme.row, open));
	}

	/** Slides one colour into another, which is what moves the switch between off and on. */
	private static int blend(int from, int to, float amount) {
		return net.minecraft.util.ARGB.color(
			255,
			Math.round(((from >> 16) & 255) + (((to >> 16) & 255) - ((from >> 16) & 255)) * amount),
			Math.round(((from >> 8) & 255) + (((to >> 8) & 255) - ((from >> 8) & 255)) * amount),
			Math.round((from & 255) + ((to & 255) - (from & 255)) * amount));
	}

	private static int fade(int color, float open) {
		int alpha = (color >>> 24) & 255;
		return net.minecraft.util.ARGB.color(Math.round(alpha * open), color);
	}

	private int tab(GuiGraphicsExtractor graphics, Theme theme, float scale, int sidebar, int y, Page page, String label, boolean header, float open) {
		int x = panelX + Math.round(8 * scale);
		int height = Math.round((header ? 14 : 20) * scale);
		int width = sidebar - Math.round(16 * scale);
		if (!header) {
			boolean selected = this.page == page;
			boolean hovered = mouseOver(x, y, width, height);
			if (selected || hovered) {
				int rowColor = selected ? theme.row : theme.rowHover();
				graphics.fill(x, y, x + width, y + height, fade(rowColor, open));
			}
				if (selected) {
					graphics.fill(x, y, x + 2, y + height, fade(theme.accent, open));
				}
			boolean on = switch (page) {
				case STUN -> config().stunEnabled;
				case CART -> config().cartEnabled;
				case CHEST -> config().chestEnabled;
				case XBOW -> config().xbowEnabled;
				case HITBOX -> config().hitboxEnabled;
				case DASH -> config().dashEnabled;
				case GLOW -> config().glowEnabled;
				case MINE -> config().mineEnabled;
				case BARITONE -> config().baritoneEnabled;
				case CHASE -> config().chaseEnabled;
				case SPEED -> config().speedEnabled;
				case NOFALL -> config().noFallEnabled;
				case SEED -> config().seedEnabled;
				case GOD -> config().godEnabled;
				case SEARCH -> config().searchEnabled;
				case ELYTRA -> config().elytraEnabled;
				case AIR -> config().airEnabled;
				case CAM -> config().freeCamEnabled;
				case TP -> config().teleportEnabled;
				case FLY -> config().flyEnabled;
				case WET -> config().wetEnabled;
				case LAVA -> config().clearLavaEnabled;
				case DARK -> config().noDarknessEnabled;
				case F5 -> config().freeF5Enabled;
				case TOTEM -> config().totemEnabled;
				case INVIS -> config().seeInvisibleEnabled;
				case RADAR -> config().locatorEnabled;
				default -> false;
			};
			if (page == Page.STUN || page == Page.CART || page == Page.CHEST || page == Page.XBOW || page == Page.HITBOX || page == Page.DASH || page == Page.GLOW || page == Page.MINE || page == Page.BARITONE || page == Page.CHASE || page == Page.SPEED || page == Page.NOFALL || page == Page.SEED || page == Page.GOD || page == Page.SEARCH || page == Page.ELYTRA || page == Page.AIR || page == Page.CAM || page == Page.TP || page == Page.FLY || page == Page.WET || page == Page.LAVA || page == Page.DARK || page == Page.F5 || page == Page.TOTEM || page == Page.INVIS || page == Page.RADAR) {
					graphics.fill(x + width - Math.round(8 * scale), y + height / 2 - 2, x + width - Math.round(4 * scale),
						y + height / 2 + 2, fade(on ? theme.on : theme.edge, open));
			}
		}
		int color = header ? theme.muted : (this.page == page ? theme.text : theme.muted);
		graphics.text(this.font, label, x + Math.round((header ? 2 : 10) * scale), y + (height - 8) / 2, fade(color, open), false);
		return y + height + Math.round(2 * scale);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (listening != null) {
			applyKey(InputConstants.Type.MOUSE.getOrCreate(event.button()));
			return true;
		}
		float scale = scale();
		int sidebar = Math.round(110 * scale);
		if (event.button() == 0 && mouseOver(panelX, panelY, panelWidth(), Math.round(34 * scale))) {
			dragging = true;
			dragOffsetX = (int) event.x() - panelX;
			dragOffsetY = (int) event.y() - panelY;
			return true;
		}
		if (event.button() == 0) {
			int tabTop = panelY + Math.round(34 * scale);
			int tabBottom = panelY + panelHeight() - Math.round(8 * scale);
			int tabY = tabTop + Math.round(6 * scale - tabScroll);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.STUN, true);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.HITBOX, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.DASH, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.MINE, false);
				tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.BARITONE, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.CHASE, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.SPEED, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.NOFALL, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.SEED, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.GOD, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.ELYTRA, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.AIR, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.CAM, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.TP, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.FLY, false);
			tabY += Math.round(10 * scale);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.GLOW, true);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.GLOW, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.SEARCH, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.WET, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.LAVA, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.DARK, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.F5, false);
			tabY += Math.round(10 * scale);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.STUN, true);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.STUN, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.CART, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.CHEST, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.XBOW, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.TOTEM, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.INVIS, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.RADAR, false);
			tabY += Math.round(10 * scale);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.THEME, true);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.THEME, false);
			tabY = hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.STYLE, false);
			hitTab(event, scale, sidebar, tabY, tabTop, tabBottom, Page.WIDGETS, false);
		}
		if (event.button() == 1 && mouseOver(panelX, panelY, panelWidth(), panelHeight())) {
			onClose();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private int hitTab(MouseButtonEvent event, float scale, int sidebar, int y, int top, int bottom, Page page, boolean header) {
		int height = Math.round((header ? 14 : 20) * scale);
		int visibleTop = Math.max(y, top);
		int visibleBottom = Math.min(y + height, bottom);
		if (!header && visibleBottom > visibleTop && mouseOver(panelX + Math.round(8 * scale), visibleTop, sidebar - Math.round(16 * scale), visibleBottom - visibleTop)) {
			if (this.page != page) {
				click();
				scroll = 0.0F;
				scrollTarget = 0.0F;
				this.page = page;
				layout();
			}
		}
		return y + height + Math.round(2 * scale);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging) {
			panelX = (int) event.x() - dragOffsetX;
			panelY = (int) event.y() - dragOffsetY;
			clampPanel();
			layout();
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		dragging = false;
		return super.mouseReleased(event);
	}

	private void clampPanel() {
		panelX = Mth.clamp(panelX, 0, Math.max(0, this.width - panelWidth()));
		panelY = Mth.clamp(panelY, 0, Math.max(0, this.height - panelHeight()));
	}

	private boolean mouseOver(int x, int y, int width, int height) {
		double mx = Minecraft.getInstance().mouseHandler.getScaledXPos(Minecraft.getInstance().getWindow());
		double my = Minecraft.getInstance().mouseHandler.getScaledYPos(Minecraft.getInstance().getWindow());
		return mx >= x && mx < x + width && my >= y && my < y + height;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		float scale = scale();
		int sidebar = Math.round(110 * scale);
		float step = (float) scrollY * Math.round(18 * scale);
		if (mouseOver(panelX, panelY, sidebar, panelHeight())) {
			tabScrollTarget = Math.max(0.0F, tabScrollTarget - step);
			return true;
		}
		if (mouseOver(panelX, panelY, panelWidth(), panelHeight())) {
			scrollTarget = Mth.clamp(scrollTarget - step, 0.0F, maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - (panelHeight() - Math.round(78 * scale())) + 8);
	}

	private void clampScroll() {
		scrollTarget = Mth.clamp(scrollTarget, 0.0F, maxScroll());
		scroll = Mth.clamp(scroll, 0.0F, maxScroll());
	}

	/** Glides the list toward wherever the wheel pointed, instead of jumping a row at a time. */
	private void settleScroll() {
		clampScroll();
		tabScrollTarget = Math.max(0.0F, tabScrollTarget);
		scroll += (scrollTarget - scroll) * 0.45F;
		tabScroll += (tabScrollTarget - tabScroll) * 0.45F;
		if (Math.abs(scrollTarget - scroll) < 0.4F) {
			scroll = scrollTarget;
		}
		if (Math.abs(tabScrollTarget - tabScroll) < 0.4F) {
			tabScroll = tabScrollTarget;
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (typingCoords) {
			if (event.isEscape()) {
				typingCoords = false;
				return true;
			}
			if (event.key() == InputConstants.KEY_BACKSPACE && !coordsDraft.isEmpty()) {
				coordsDraft = coordsDraft.substring(0, coordsDraft.length() - 1);
			} else if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
				commitCoords();
			} else if (event.key() == InputConstants.KEY_V && event.hasControlDown()) {
				coordsDraft = trimCoords(coordsDraft + minecraft.keyboardHandler.getClipboard());
			}
			return true;
		}
		if (listening != null) {
			applyKey(event.isEscape() ? InputConstants.UNKNOWN : InputConstants.getKey(event));
			return true;
		}
		if (event.isEscape()) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
		if (!typingCoords || !event.isAllowedChatCharacter()) {
			return super.charTyped(event);
		}
		String typed = event.codepointAsString();
		if ("0123456789.,- ".indexOf(typed) >= 0) {
			coordsDraft = trimCoords(coordsDraft + typed);
		}
		return true;
	}

	private void commitCoords() {
		typingCoords = false;
		config().teleportCoords = coordsDraft.trim();
		config().save();
	}

	private static String trimCoords(String text) {
		StringBuilder kept = new StringBuilder();
		for (int i = 0; i < text.length() && kept.length() < 48; i++) {
			char c = text.charAt(i);
			if ("0123456789.,- ".indexOf(c) >= 0) {
				kept.append(c);
			}
		}
		return kept.toString();
	}

	private void applyKey(InputConstants.Key key) {
		listening.setKey(key);
		listening = null;
		KeyMapping.resetMapping();
		minecraft.options.save();
		dev.foreyy.dimsclient.bind.Binds.save();
	}

	@Override
	public void onClose() {
		config().panelX = panelX;
		config().panelY = panelY;
		config().save();
		this.minecraft.setScreenAndShow(parent);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean rowVisible(int y, int height) {
		float scale = scale();
		int top = panelY + Math.round(52 * scale);
		int bottom = panelY + panelHeight() - Math.round(26 * scale);
		return y + height > top && y < bottom;
	}

	/** Adds the looked-at player, or removes them when they are already listed. An empty look clears it. */
	private String addLooked(String list) {
		String name = Chase.lookedAtName(minecraft);
		java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<>();
		for (String part : list.split("[,\\s]+")) {
			if (!part.isEmpty()) {
				names.add(part);
			}
		}
		if (name.isEmpty()) {
			names.clear();
		} else if (!names.removeIf(part -> part.equalsIgnoreCase(name))) {
			names.add(name);
		}
		return String.join(", ", names);
	}

	private static void click() {
		Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F));
	}

	private enum Page {
		STUN, CART, CHEST, XBOW, HITBOX, DASH, GLOW, MINE, BARITONE, CHASE, SPEED, NOFALL, SEED, GOD, SEARCH, ELYTRA, AIR, CAM, TP, FLY, WET, LAVA, DARK, F5, TOTEM, INVIS, RADAR, THEME, STYLE, WIDGETS;

		String title(DimsConfig config) {
			return switch (this) {
				case STUN -> "AutoStunSlam";
				case CART -> "AutoCart";
				case CHEST -> "ChestReach";
				case XBOW -> "AutoXBow";
				case HITBOX -> "Hitboxes";
				case DASH -> "Dash";
				case GLOW -> "XRay";
				case MINE -> "AutoFarm";
				case BARITONE -> "Baritone";
				case CHASE -> "KillAura";
				case SPEED -> "Speed";
				case NOFALL -> "NoFall";
				case SEED -> "SeedCracker";
				case GOD -> "GodMode";
				case SEARCH -> "AutoSearch";
				case ELYTRA -> "ElytraTarget";
				case AIR -> "AirStuck";
				case CAM -> "FreeCam";
				case TP -> "Teleport";
				case FLY -> "Fly";
				case WET -> "Wet";
				case LAVA -> "ClearLava";
				case DARK -> "NoDarkness";
				case F5 -> "FreeF5";
				case TOTEM -> "AutoTotem";
				case INVIS -> "SeeInvisible";
				case RADAR -> "Locator";
				case THEME -> config.t("Тема", "Theme");
				case STYLE -> config.t("Интерфейс", "Interface");
				case WIDGETS -> config.t("Виджеты", "Widgets");
			};
		}

		String subtitle(DimsConfig config) {
			return switch (this) {
				case STUN -> config.t("Топор сбивает щит, пока прицел на цели", "Axe breaks a shield while aimed at the target");
				case CART -> config.t("Рельсы и TNT-вагонетки по своим стрелам", "Rails and TNT minecarts on your arrows");
				case CHEST -> config.t("Сундуки сквозь стены", "Chests through walls");
				case XBOW -> config.t("Рельсы, вагонетка и выстрел по кнопке", "Rails, a cart and a shot on one key");
				case HITBOX -> config.t("Прицел берёт всех шире их хитбокса", "The crosshair catches everyone past their hitbox");
				case DASH -> config.t("Рывок как Lunge III, без копья", "A Lunge III dash, without a spear");
				case GLOW -> config.t("Сундуки и руды сквозь блоки", "Chests and ores through blocks");
				case MINE -> config.t("Роет туннель и собирает руду", "Digs a tunnel and gathers ore");
				case BARITONE -> config.t("Сам идёт к координатам и к блоку AutoFarm", "Walks to coordinates and to the AutoFarm block");
				case CHASE -> config.t("Бежит за игроком и бьёт критами", "Runs after a player and hits with crits");
				case SPEED -> config.t("Ходьба до десяти раз быстрее", "Walking up to ten times faster");
				case NOFALL -> config.t("Падение не наносит урон", "Falling does no damage");
				case SEED -> config.t("Узнаёт сид по структурам", "Reads the seed from structures");
				case GOD -> config.t("Урон не проходит", "Damage does not land");
				case SEARCH -> config.t("Структуры в зоне прорисовки", "Structures inside render distance");
				case ELYTRA -> config.t("Кружит на элитрах и бьёт", "Circles on elytra and hits");
				case AIR -> config.t("Зависаешь, сервер видит падение", "You hang, the server sees a fall");
				case CAM -> config.t("Камера летает, персонаж стоит", "The camera flies, you stay put");
				case FLY -> config.t("Полёт как в креативе", "Flight like creative");
				case TP -> config.t("Идёт к координатам из поля", "Walks to the typed coordinates");
				case WET -> config.t("Мокрые грани", "Wet surfaces");
				case LAVA -> config.t("Камера в лаве остаётся чистой", "The camera stays clear in lava");
				case DARK -> config.t("Тьма не затягивает экран", "Darkness does not cover the screen");
				case F5 -> config.t("Камера F5 крутится вокруг, взгляд на месте", "The F5 camera spins around while the look stays");
				case TOTEM -> config.t("Тотем сам встаёт во вторую руку", "A totem moves into the offhand on its own");
				case INVIS -> config.t("Невидимые игроки остаются видимыми", "Invisible players stay visible");
				case RADAR -> config.t("Полоска локатора есть, даже если сервер её выключил", "The locator bar stays even when the server turns it off");
				case THEME -> config.t("Цвет всего клиента", "Colour of the whole client");
				case STYLE -> config.t("Размер, язык и что видно в игре", "Size, language and what shows in game");
				case WIDGETS -> config.t("Таблички сверху экрана", "Cards along the top of the screen");
			};
		}
	}

	/** A switch. The knob and the colour slide instead of snapping between ON and OFF. */
	private final class Toggle extends Row {
		private final BooleanSupplier get;
		private final Consumer<Boolean> set;
		private float shown = -1.0F;

		private Toggle(int x, int y, int width, int height, Supplier<String> label, BooleanSupplier get, Consumer<Boolean> set) {
			super(x, y, width, height, label, () -> "", () -> 0, () -> set.accept(!get.getAsBoolean()));
			this.get = get;
			this.set = set;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			if (!rowVisible(this.getY(), this.height)) {
				return;
			}
			float target = get.getAsBoolean() ? 1.0F : 0.0F;
			if (shown < 0.0F) {
				shown = target;
			}
			shown += (target - shown) * 0.35F;
			if (Math.abs(target - shown) < 0.01F) {
				shown = target;
			}
			Theme theme = theme();
			float open = openAmount();
			boolean over = this.isHovered();
			card(graphics, theme, this.getX(), this.getY(), this.width, this.height, over, open);
			int textY = this.getY() + (this.height - 8) / 2;
			graphics.text(font, this.label.get(), this.getX() + 12, textY, fade(theme.text, open), false);
			int trackW = 28;
			int trackH = 14;
			int trackX = this.getX() + this.width - 10 - trackW;
			int trackY = this.getY() + (this.height - trackH) / 2;
			graphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, fade(theme.edge, open));
			int knob = trackX + Math.round((trackW - trackH) * shown);
			graphics.fill(knob, trackY, knob + trackH, trackY + trackH, fade(blend(theme.off(), theme.on, shown), open));
		}
	}

	/** A setting line. Drawn here rather than with a vanilla button, which cannot take a theme. */
	private class Row extends AbstractWidget {
		final Supplier<String> label;
		private final Supplier<String> value;
		private final Supplier<Integer> valueColor;
		private final Runnable action;
		final int homeX;
		private final int homeY;

		private Row(int x, int y, int width, int height, Supplier<String> label, Supplier<String> value,
				Supplier<Integer> valueColor, Runnable action) {
			super(x, y, width, height, Component.literal(label.get()));
			this.homeX = x;
			this.homeY = y;
			this.label = label;
			this.value = value;
			this.valueColor = valueColor;
			this.action = action;
		}

		@Override
		protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			if (!rowVisible(this.getY(), this.height)) {
				return;
			}
			Theme theme = theme();
			float open = openAmount();
			boolean over = this.isHovered() && rowVisible(this.getY(), this.height);
			card(graphics, theme, this.getX(), this.getY(), this.width, this.height, over, open);
			int textY = this.getY() + (this.height - 8) / 2;
			graphics.text(font, this.label.get(), this.getX() + 12, textY, fade(theme.text, open), false);
			String shown = this.value.get();
			if (!shown.isEmpty()) {
				graphics.text(font, shown, this.getX() + this.width - 8 - font.width(shown), textY, fade(this.valueColor.get(), open), false);
			}
		}

		@Override
		public void onClick(MouseButtonEvent event, boolean doubleClick) {
			if (event.button() != 0 || !rowVisible(this.getY(), this.height)) {
				return;
			}
			click();
			this.action.run();
		}

		@Override
		protected void updateWidgetNarration(NarrationElementOutput output) {
			output.add(NarratedElementType.TITLE, Component.literal(this.label.get() + " " + this.value.get()));
		}

		@Override
		public NarrationPriority narrationPriority() {
			return this.isHovered() ? NarrationPriority.HOVERED : NarrationPriority.NONE;
		}
	}

	/** Wrapped hint under a short page. Not a widget, so it can never steal a click. */
	private final class Note {
		int x;
		final int homeX;
		final int homeY;
		int y;
		private final int width;
		private final String text;

		private Note(int x, int y, int width, String text) {
			this.homeX = x;
			this.x = x;
			this.homeY = y;
			this.y = y;
			this.width = width;
			this.text = text;
		}

		void draw(GuiGraphicsExtractor graphics, Theme theme, float open) {
			graphics.textWithWordWrap(font, Component.literal(text), x, y, width, fade(theme.muted, open), false);
		}
	}
}
