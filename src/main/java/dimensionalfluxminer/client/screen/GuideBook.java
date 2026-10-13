package dimensionalfluxminer.client.screen;

import dimensionalfluxminer.DimensionalFluxMiner;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * The guide laid out as book pages: the presentation (left) and the contents (right) open the book, then every
 * chapter starts on a left page and flows page after page; long paragraphs continue on the next page and a heading
 * never stays alone at the foot of a page.
 */
final class GuideBook {
    static final int GAP = 5;
    /** The book's own texture: its page illustrations and the core seal decorate the guide. */
    static final ResourceLocation TEXTURE = DimensionalFluxMiner.id("textures/item/guide_book.png");

    record Placed(GuideElement element, int y, int height) {
    }

    /** {@code chapter} is -1 for the front matter and the decorated blank pages. */
    record Page(int chapter, List<Placed> elements) {
    }

    final List<GuideContent.Chapter> chapters;
    final List<Page> pages = new ArrayList<>();
    final int[] chapterStart;

    private final GuideScreen screen;
    private final int width;
    private final int height;
    private List<Placed> current = new ArrayList<>();
    private int y;
    private int chapter = -1;

    private GuideBook(GuideScreen screen, List<GuideContent.Chapter> chapters, int width, int height) {
        this.screen = screen;
        this.chapters = chapters;
        this.width = width;
        this.height = height;
        this.chapterStart = new int[chapters.size()];
    }

    static GuideBook build(GuideScreen screen, List<GuideContent.Chapter> chapters, int width, int height) {
        GuideBook book = new GuideBook(screen, chapters, width, height);
        book.layout();
        return book;
    }

    int spreads() {
        return pages.size() / 2;
    }

    private void layout() {
        for (GuideElement e : List.of(new Emblem(), new CenteredText(Component.translatable("guide.dimensionalfluxminer.cover.title"),
                        GuideElements.HEADING), new CenteredText(Component.translatable("guide.dimensionalfluxminer.cover.subtitle"),
                        GuideElements.MUTED), new GuideElements.Spacer(2),
                new GuideElements.Text(Component.translatable("guide.dimensionalfluxminer.cover.intro")))) {
            add(e, null);
        }
        closePage();
        add(new GuideElements.Heading(Component.translatable("guide.dimensionalfluxminer.index.title")), null);
        add(new Index(this), null);
        add(new GuideElements.Text(Component.translatable("guide.dimensionalfluxminer.index.howto")), null);
        closePage();
        for (int c = 0; c < chapters.size(); c++) {
            chapter = -1;
            if (pages.size() % 2 == 1) {
                decoratedPage();
            }
            chapter = c;
            chapterStart[c] = pages.size();
            GuideContent.Chapter ch = chapters.get(c);
            add(new GuideElements.ChapterTitle(ch.icon(), Component.translatable("guide.dimensionalfluxminer.chapter." + ch.id())), null);
            List<GuideElement> elements = ch.elements();
            for (int i = 0; i < elements.size(); i++) {
                add(elements.get(i), i + 1 < elements.size() ? elements.get(i + 1) : null);
            }
            closePage();
        }
        chapter = -1;
        if (pages.size() % 2 == 1) {
            decoratedPage();
        }
    }

    private void decoratedPage() {
        current.add(new Placed(new Illustration(pages.size() / 2 % 2), 0, height));
        closePage();
    }

    private void add(GuideElement element, GuideElement next) {
        if (element instanceof GuideElements.Text text) {
            addLines(text.lines(screen, width));
            return;
        }
        int h = element.height(screen, width);
        if (h <= 0) {
            return;
        }
        int need = h;
        if (element instanceof GuideElements.Heading && next != null) {
            // Keep a heading with the start of what follows it.
            need += GAP + Math.min(height / 2, firstPart(next));
        }
        if (y > 0 && y + need > height) {
            closePage();
        }
        current.add(new Placed(element, y, h));
        y += h + GAP;
    }

    private int firstPart(GuideElement next) {
        if (next instanceof GuideElements.Text text) {
            return Math.min(2, text.lines(screen, width).size()) * GuideElements.LINE;
        }
        return next.height(screen, width);
    }

    private void addLines(List<FormattedCharSequence> lines) {
        int index = 0;
        while (index < lines.size()) {
            int room = (height - y) / GuideElements.LINE;
            int left = lines.size() - index;
            // Never leave a single line of a longer paragraph at the foot of a page.
            if (room <= 0 || (room == 1 && left > 1 && y > 0)) {
                closePage();
                continue;
            }
            int take = Math.min(room, left);
            List<FormattedCharSequence> part = List.copyOf(lines.subList(index, index + take));
            current.add(new Placed(new GuideElements.Lines(part), y, take * GuideElements.LINE));
            y += take * GuideElements.LINE;
            index += take;
            if (index < lines.size()) {
                closePage();
            } else {
                y += GAP;
            }
        }
    }

    private void closePage() {
        if (current.isEmpty()) {
            return;
        }
        pages.add(new Page(chapter, current));
        current = new ArrayList<>();
        y = 0;
    }

    // ------------------------------------------------------------------ front matter and decoration

    /** The core seal of the cover, centered. */
    record Emblem() implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 34;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            g.blit(TEXTURE, x + (width - 32) / 2, y + 1, 32, 32, 32, 64, 16, 16, 128, 128);
        }
    }

    record CenteredText(Component text, int color) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 11;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            screen.fittedText(g, text, x, y + 1, width, color, true);
        }
    }

    /** One of the book's page illustrations covering the whole page that would otherwise stay blank. */
    record Illustration(int variant) implements GuideElement {
        @Override
        public int height(GuideScreen screen, int width) {
            return 0;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            int u = variant == 0 ? 64 : 96;
            g.blit(TEXTURE, x - GuideScreen.PAD_X, y - GuideScreen.PAD_TOP, screen.pageWidth(), screen.pageHeight(), u, 0, 32, 48, 128, 128);
        }
    }

    /** The contents: one line per chapter with its page number; clicking a line turns the book to it. */
    static final class Index implements GuideElement {
        private static final int ROW = 15;
        private final GuideBook book;

        Index(GuideBook book) {
            this.book = book;
        }

        @Override
        public int height(GuideScreen screen, int width) {
            return book.chapters.size() * ROW;
        }

        @Override
        public void render(GuideScreen screen, GuiGraphics g, int x, int y, int width, int mouseX, int mouseY) {
            for (int c = 0; c < book.chapters.size(); c++) {
                int ry = y + c * ROW;
                boolean over = mouseX >= x && mouseX < x + width && mouseY >= ry && mouseY < ry + ROW;
                if (over) {
                    g.fill(x - 2, ry, x + width + 2, ry + ROW, GuideElements.TINT);
                }
                g.pose().pushPose();
                g.pose().translate(x, ry + 1, 0);
                g.pose().scale(0.875F, 0.875F, 1.0F);
                g.renderItem(book.chapters.get(c).icon().get(), 0, 0);
                g.pose().popPose();
                String page = Integer.toString(book.chapterStart[c] + 1);
                int pageW = screen.font().width(page);
                screen.fittedText(g, Component.translatable("guide.dimensionalfluxminer.chapter." + book.chapters.get(c).id()), x + 18,
                        ry + 4, width - 24 - pageW, over ? GuideElements.HEADING : GuideElements.BODY);
                g.drawString(screen.font(), page, x + width - pageW, ry + 4, GuideElements.MUTED, false);
            }
        }

        @Override
        public boolean click(GuideScreen screen, double mouseX, double mouseY, int x, int y, int width, int button) {
            int c = (int) ((mouseY - y) / ROW);
            if (c < 0 || c >= book.chapters.size() || mouseX < x || mouseX >= x + width) {
                return false;
            }
            screen.turnTo(book.chapterStart[c] / 2);
            return true;
        }
    }
}
