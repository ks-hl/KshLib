package dev.kshl.kshlib.kyori;

import dev.kshl.kshlib.misc.Characters;
import dev.kshl.kshlib.misc.Formatter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Paginator<T> {
    private final TextColor primaryColor;
    private final TextColor secondaryColor;
    private final String commandFormat;
    @Nullable
    private final Component header;
    private final BiFunction<Index, T, Component> lineFormatter;

    /**
     * @param header         If provided, is appended at the top of #getPage()
     * @param primaryColor   The color of enabled arrows, page numbers, and entry counts
     * @param secondaryColor The color of everything else, except disabled arrows which are always color code 8
     * @param commandFormat  The command to go to a specific page, containing a '%page%' which will be replaced with the page number.
     * @param lineFormatter  A function that provides the index and result and returns the formatted line.
     */
    public Paginator(@Nullable Component header, TextColor primaryColor, TextColor secondaryColor, String commandFormat, @Nullable BiFunction<Index, T, Component> lineFormatter) {
        this.header = header;

        this.lineFormatter = lineFormatter;
        if (!commandFormat.contains("%page%"))
            throw new IllegalArgumentException("commandFormat does not contain %page%");

        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.commandFormat = commandFormat;
    }

    public Component getPage(List<T> results, int page, int perPage) {
        TextComponent.Builder builder = Component.text();

        if (header != null) {
            builder.append(header);
            builder.appendNewline();
        }

        if (page >= getNumberOfPages(perPage, results.size())) {
            builder.append(Component.text("There are no entries on this page.", primaryColor));
            builder.appendNewline();
        } else {
            formatBody(builder, results, page, perPage);
        }

        addArrows(builder, page, perPage, results.size());

        return builder.build();
    }

    public void formatBody(TextComponent.Builder builder, List<T> results, int page, int perPage) {
        Objects.requireNonNull(lineFormatter, "lineFormatter must be non null to use getPage with default body");

        forEachResult(results, page, perPage, (index, result) -> {
            builder.append(lineFormatter.apply(index, result));
            builder.appendNewline();
        });
    }

    public void forEachResult(List<T> results, int page, int perPage, BiConsumer<Index, T> forEach) {
        for (int i = page * perPage; i < results.size() && i < (page + 1) * perPage; i++) {
            T result = results.get(i);
            Index index = new Index(page, perPage, i, i - page * perPage);

            forEach.accept(index, result);
        }
    }

    private static int getNumberOfPages(int perPage, int numberOfEntries) {
        return (int) Math.ceil((double) numberOfEntries / perPage);
    }

    /**
     * @param builder      The ComponentBuilder to which the arrows will be appended
     * @param page         Which page the paginator is currently on, 0-indexed
     * @param perPage      The number of entries per page
     * @param numOfEntries The total number of entries
     */
    public void addArrows(TextComponent.Builder builder, int page, int perPage, int numOfEntries) {
        int numOfPages = getNumberOfPages(perPage, numOfEntries);

        builder.append(Component.text("(", secondaryColor));
        String leftArrow = String.valueOf(Characters.LEFT_ARROW);
        String rightArrow = String.valueOf(Characters.RIGHT_ARROW);

        Component left1 = Component.text(leftArrow, primaryColor, TextDecoration.BOLD);
        Component right1 = Component.text(rightArrow, primaryColor, TextDecoration.BOLD);

        Component left2 = ComponentHelper.concat(left1, left1);
        Component right2 = ComponentHelper.concat(right1, right1);

        Component leftGray = Component.text(leftArrow + leftArrow + " " + leftArrow, NamedTextColor.DARK_GRAY, TextDecoration.BOLD);
        Component rightGray = Component.text(rightArrow + " " + rightArrow + rightArrow, NamedTextColor.DARK_GRAY, TextDecoration.BOLD);
        if (page > 0 && numOfPages > 0) {
            // FIRST PAGE
            builder.append(left2 //
                    .clickEvent(ClickEvent.runCommand(commandFormat.replace("%page%", "0"))) //
                    .hoverEvent(HoverEvent.showText(Component.text("First Page", secondaryColor))));

            builder.append(Component.space());

            // PREVIOUS PAGE
            builder.append(left1 //
                    .clickEvent(ClickEvent.runCommand(commandFormat.replace("%page%", String.valueOf(page - 1)))) //
                    .hoverEvent(HoverEvent.showText(Component.text("Previous Page", secondaryColor))));
        } else {
            builder.append(leftGray);
        }

        builder.append(Component.space()).append(Component.space());

        if ((page + 1) < numOfPages) {
            // NEXT PAGE
            builder.append(right1 //
                    .clickEvent(ClickEvent.runCommand(commandFormat.replace("%page%", String.valueOf(Math.max(0, page + 1))))) //
                    .hoverEvent(HoverEvent.showText(Component.text("Next Page", secondaryColor))));

            builder.append(Component.space());

            // LAST PAGE
            builder.append(right2 //
                    .clickEvent(ClickEvent.runCommand(commandFormat.replace("%page%", String.valueOf(numOfPages - 1)))) //
                    .hoverEvent(HoverEvent.showText(Component.text("Last Page", secondaryColor))));
        } else {
            builder.append(rightGray);
        }
        builder.append(Component.text(")  Page (", secondaryColor));
        builder.append(Component.text(String.valueOf(page + 1), primaryColor));
        builder.append(Component.text("/", secondaryColor));
        builder.append(Component.text(String.valueOf(numOfPages), primaryColor));
        builder.append(Component.text(") ", secondaryColor));
        builder.append(Component.text(String.valueOf(numOfEntries), primaryColor));
        builder.append(Component.text(" "));
        builder.append(Component.text(Formatter.pluralize(numOfEntries, "entry", "entries"), secondaryColor));
    }

    public record Index(int page, int perPage, int rawIndex, int indexOnPage) {
    }
}
