package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.compat.RecipeViewers;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/** The building blocks of the guide pages, printed in ink on the cream pages of the Dimensional Flux Guide. */
final class GuideElements {
    /** Ink colors on paper, taken from the book's own texture. */
    static final int BODY = 0xFF343442;
    static final int HEADING = 0xFF6B44B5;
    static final int MUTED = 0xFF706A81;
    static final int TIP = 0xFF0D8080;
    static final int WARNING = 0xFFB0402A;
    static final int RULE = 0x606B44B5;
    static final int TINT = 0x226B44B5;
    static final int LINE = 10;

    private GuideElements() {
    }

    /**
     * The translations color their highlights for dark screens (aqua, yellow, green, red); on paper they are
     * printed in the darker inks of the same families (dark purple, dark aqua, dark green, dark red).
     */
    static Component ink(Component text) {
        String raw = text.getString();
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            out.append(c);
            if (c == '§' && i + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(++i));
                out.append(switch (code) {
                    case 'b' -> '5';
                    case 'e' -> '3';
                    case 'a' -> '2';
                    case 'c' -> '4';
                    default -> code;
                });
            }
        }
        return Component.literal(out.toString());
    }

    /** Section title with a thin rule under it. */
    record Heading(Component text) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 15;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            screen.fittedText(g, text, x, y + 2, width, HEADING);
            g.fill(x, y + 12, x + width, y + 13, RULE);
        }
    }

    /** First block of a chapter: its icon and title. */
    record ChapterTitle(Supplier<ItemStack> icon, Component title) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 24;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            g.renderItem(icon.get(), x, y + 1);
            screen.fittedText(g, title, x + 20, y + 5, width - 20, HEADING);
            g.fill(x, y + 20, x + width, y + 21, RULE);
            g.fill(x, y + 22, x + width, y + 23, RULE);
        }
    }

    /** Wrapped paragraph; the pagination may split it into {@link Lines} across pages. */
    static final class Text implements GuideElement {
        private final Component text;
        private int cachedWidth = -1;
        private List<FormattedCharSequence> lines = List.of();

        Text(Component text) {
            this.text = text;
        }

        List<FormattedCharSequence> lines(GuideScreen screen, int width) {
            if (width != cachedWidth) {
                cachedWidth = width;
                lines = screen.font().split(ink(text), width);
            }
            return lines;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return lines(screen, width).size() * LINE;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            new Lines(lines(screen, width)).render(screen, g, x, y, width, mouseX, mouseY);
        }
    }

    /** Some lines of a paragraph (a paragraph split between two pages). */
    record Lines(List<FormattedCharSequence> lines) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return lines.size() * LINE;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            for (int i = 0; i < lines.size(); i++) {
                g.drawString(screen.font(), lines.get(i), x, y + i * LINE, BODY, false);
            }
        }
    }

    /** Boxed note with a colored bar and a title (tip or warning). */
    static final class Note implements GuideElement {
        private final Component title;
        private final Text body;
        private final int accent;

        Note(Component title, Component body, int accent) {
            this.title = title;
            this.body = new Text(body);
            this.accent = accent;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return 15 + body.height(screen, width - 10) + 4;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int h = height(screen, width);
            g.fill(x, y, x + width, y + h, 0x1E000000 | (accent & 0x00FFFFFF));
            g.fill(x, y, x + 2, y + h, accent);
            g.drawString(screen.font(), title, x + 6, y + 4, accent, false);
            body.render(screen, g, x + 6, y + 15, width - 10, mouseX, mouseY);
        }
    }

    /** An item with its name and a description beside it. */
    static final class ItemNote implements GuideElement {
        private final Supplier<ItemStack> stack;
        private final Text description;

        ItemNote(Supplier<ItemStack> stack, Component description) {
            this.stack = stack;
            this.description = new Text(description);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return Math.max(20, 11 + description.height(screen, width - 22)) + 2;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            ItemStack item = stack.get();
            screen.itemSlot(g, item, x, y, mouseX, mouseY);
            screen.fittedText(g, item.getHoverName(), x + 22, y + 1, width - 22, HEADING);
            description.render(screen, g, x + 22, y + 12, width - 22, mouseX, mouseY);
        }
    }

    /** A row of items, then fluids (wrapping), with their tooltips. */
    static final class ItemRow implements GuideElement {
        private final Supplier<List<ItemStack>> items;
        private final List<Supplier<? extends Fluid>> fluids;

        ItemRow(Supplier<List<ItemStack>> items, List<Supplier<? extends Fluid>> fluids) {
            this.items = items;
            this.fluids = fluids;
        }

        private int perRow(int width) {
            return Math.max(1, (width + 2) / 20);
        }

        @Override
        public int height(GuideScreen screen, int width) {
            int count = items.get().size() + fluids.size();
            return ((count + perRow(width) - 1) / perRow(width)) * 20;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            List<ItemStack> list = items.get();
            int per = perRow(width);
            for (int i = 0; i < list.size() + fluids.size(); i++) {
                int sx = x + (i % per) * 20;
                int sy = y + (i / per) * 20;
                if (i < list.size()) {
                    screen.itemSlot(g, list.get(i), sx, sy, mouseX, mouseY);
                } else {
                    screen.fluidSlot(g, fluids.get(i - list.size()).get(), sx, sy, mouseX, mouseY);
                }
            }
        }
    }

    /** A crafting table recipe (shaped or shapeless) read from the recipe manager, tag ingredients cycling. */
    record Crafting(ResourceLocation id) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 67;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            Level level = Minecraft.getInstance().level;
            RecipeHolder<?> holder = level == null ? null : level.getRecipeManager().byKey(id).orElse(null);
            if (holder == null) {
                screen.fittedText(g, Component.translatable("guide.dimensionalfluxminer.recipe_missing"), x, y + 4, width, MUTED);
                return;
            }
            Recipe<?> recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            Component name = result.getCount() > 1
                    ? Component.literal(result.getCount() + "× ").append(result.getHoverName()) : result.getHoverName();
            screen.fittedText(g, name, x, y + 1, width, HEADING);
            int gridY = y + 11;
            List<Ingredient> ingredients = recipe.getIngredients();
            int recipeW = recipe instanceof ShapedRecipe shaped ? shaped.getWidth() : 3;
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int index = row * recipeW + col;
                    ItemStack shown = ItemStack.EMPTY;
                    if (col < recipeW && index < ingredients.size()) {
                        shown = cycle(ingredients.get(index));
                    }
                    screen.itemSlot(g, shown, x + col * 18, gridY + row * 18, mouseX, mouseY);
                }
            }
            int arrowX = x + 3 * 18 + 6;
            g.drawString(screen.font(), "→", arrowX + 2, gridY + 23, HEADING, false);
            screen.itemSlot(g, result, arrowX + 16, gridY + 18, mouseX, mouseY);
            Component kind = Component.translatable(recipe instanceof ShapedRecipe
                    ? "guide.dimensionalfluxminer.recipe_shaped" : "guide.dimensionalfluxminer.recipe_shapeless");
            int kindX = arrowX + 38;
            if (x + width - kindX > 16) {
                screen.fittedText(g, kind, kindX, gridY + 23, x + width - kindX, MUTED);
            }
        }

        private static ItemStack cycle(Ingredient ingredient) {
            ItemStack[] options = ingredient.getItems();
            if (options.length == 0) {
                return ItemStack.EMPTY;
            }
            return options[(int) ((Util.getMillis() / 1000) % options.length)];
        }
    }

    /** Opens machine recipe categories in EMI / JEI; hidden when no viewer is installed. */
    static final class ViewerButton implements GuideElement {
        private final Component label;
        private final ResourceLocation[] categories;

        ViewerButton(Component label, ResourceLocation... categories) {
            this.label = label;
            this.categories = categories;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return RecipeViewers.available() ? 16 : 0;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            if (RecipeViewers.available()) {
                screen.textButton(g, x, y, buttonWidth(screen, width), 15, label, mouseX, mouseY);
            }
        }

        private int buttonWidth(GuideScreen screen, int width) {
            return Math.min(width, screen.font().width(label) + 16);
        }

        @Override
        public boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
            if (!RecipeViewers.available() || mouseX < x || mouseX >= x + buttonWidth(screen, width) || mouseY < y || mouseY >= y + 15) {
                return false;
            }
            screen.playClick();
            RecipeViewers.show(categories);
            return true;
        }
    }

    /** Table with a header row; the cells are recomputed every frame (they follow the server configuration). */
    static final class Table implements GuideElement {
        private static final int ROW = 11;
        private final List<Component> headers;
        private final Supplier<List<List<String>>> rows;
        private final float[] columns;

        /** {@code columns}: relative width of each column. */
        Table(List<Component> headers, float[] columns, Supplier<List<List<String>>> rows) {
            this.headers = headers;
            this.columns = columns;
            this.rows = rows;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return (rows.get().size() + 1) * ROW + 3;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int[] starts = new int[columns.length + 1];
            float total = 0;
            for (float c : columns) {
                total += c;
            }
            float acc = 0;
            for (int i = 0; i < columns.length; i++) {
                starts[i] = x + Math.round(acc / total * width);
                acc += columns[i];
            }
            starts[columns.length] = x + width;
            g.fill(x, y, x + width, y + ROW, 0x336B44B5);
            for (int c = 0; c < headers.size(); c++) {
                screen.fittedText(g, headers.get(c), starts[c] + 2, y + 2, starts[c + 1] - starts[c] - 3, HEADING);
            }
            List<List<String>> list = rows.get();
            for (int r = 0; r < list.size(); r++) {
                int ry = y + (r + 1) * ROW;
                if (r % 2 == 1) {
                    g.fill(x, ry, x + width, ry + ROW, TINT);
                }
                List<String> cells = list.get(r);
                for (int c = 0; c < cells.size() && c < columns.length; c++) {
                    screen.fittedText(g, Component.literal(cells.get(c)), starts[c] + 2, ry + 2, starts[c + 1] - starts[c] - 3,
                            c == 0 ? HEADING : BODY);
                }
            }
            g.fill(x, y + (list.size() + 1) * ROW, x + width, y + (list.size() + 1) * ROW + 1, RULE);
        }
    }

    /** Empty space. */
    record Spacer(int size) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return size;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
        }
    }
}
