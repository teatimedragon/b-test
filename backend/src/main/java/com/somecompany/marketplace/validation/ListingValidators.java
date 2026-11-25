package com.somecompany.marketplace.validation;

import static com.somecompany.marketplace.validation.Messages.quote;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The individual validation rules. Each factory returns an independent {@link ListingValidator}.
 *
 * <p>Presence is checked once by {@link #required}; the format rules skip missing or blank values
 * so a missing field yields one error, not several.
 */
public final class ListingValidators {

    public static final Set<String> STATUSES = Set.of("active", "pending", "sold");
    public static final int MAX_LISTING_ID_LENGTH = 150;

    private static final Pattern STATE = Pattern.compile("[A-Za-z]{2}");
    private static final Pattern ISO_DATE_SHAPE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);
    private static final BigDecimal MAX_BEDROOMS = BigDecimal.valueOf(Short.MAX_VALUE);
    private static final BigDecimal MAX_SQFT = BigDecimal.valueOf(Integer.MAX_VALUE);
    private static final BigDecimal LAT_LIMIT = BigDecimal.valueOf(90);
    private static final BigDecimal LNG_LIMIT = BigDecimal.valueOf(180);

    private ListingValidators() {
    }

    // ---- presence and length -------------------------------------------------------------

    /** The field must exist and not be empty or whitespace only. */
    public static ListingValidator required(ListingField field) {
        return listing -> {
            String value = field.valueOf(listing);
            if (value == null) {
                return List.of(ValidationError.of(field, field.column() + " is required but value was null"));
            }
            if (value.isBlank()) {
                return List.of(ValidationError.of(field,
                        field.column() + " " + quote(value) + " must not be empty or blank"));
            }
            return List.of();
        };
    }

    /** The raw value must fit its column. */
    public static ListingValidator maxLength(ListingField field) {
        return listing -> {
            String value = field.valueOf(listing);
            if (value != null && value.length() > field.maxLength()) {
                return List.of(ValidationError.of(field, field.column() + " " + quote(value) + " is "
                        + value.length() + " characters, max is " + field.maxLength()));
            }
            return List.of();
        };
    }

    /** {@code source + ":" + id} must fit the 150-character ListingId column. */
    public static ListingValidator listingIdLength() {
        return listing -> {
            String id = present(listing.id());
            String source = present(listing.source());
            if (id == null || source == null) {
                return List.of();
            }
            String listingId = ListingMapper.listingIdOf(source, id);
            if (listingId.length() > MAX_LISTING_ID_LENGTH) {
                return List.of(new ValidationError(ValidationError.GENERAL, "ListingId " + quote(listingId)
                        + " is " + listingId.length() + " characters, max is " + MAX_LISTING_ID_LENGTH));
            }
            return List.of();
        };
    }

    // ---- text formats --------------------------------------------------------------------

    public static ListingValidator state() {
        return listing -> {
            String value = present(listing.state());
            if (value != null && !STATE.matcher(value).matches()) {
                return List.of(ValidationError.of(ListingField.STATE,
                        "State " + quote(listing.state()) + " must be exactly 2 letters"));
            }
            return List.of();
        };
    }

    public static ListingValidator status() {
        return listing -> {
            String value = present(listing.status());
            if (value != null && !STATUSES.contains(value.toLowerCase(Locale.ROOT))) {
                return List.of(ValidationError.of(ListingField.STATUS,
                        "Status " + quote(listing.status()) + " must be active, pending or sold"));
            }
            return List.of();
        };
    }

    public static ListingValidator listedDate() {
        return listing -> {
            String value = present(listing.listedDate());
            if (value != null && parseDate(value) == null) {
                return List.of(ValidationError.of(ListingField.LISTED_DATE,
                        "ListedDate " + quote(listing.listedDate()) + " must be a valid date in yyyy-MM-dd form"));
            }
            return List.of();
        };
    }

    /** Parses a strict {@code yyyy-MM-dd} date, or returns null. */
    public static LocalDate parseDate(String value) {
        String v = value.trim();
        if (!ISO_DATE_SHAPE.matcher(v).matches()) {
            return null;
        }
        try {
            return LocalDate.parse(v, ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // ---- numbers -------------------------------------------------------------------------

    /** Number {@code > 0}, at most 15 digits before the decimal point and 4 after. */
    public static ListingValidator price() {
        return numeric(ListingField.PRICE, (field, raw, n) -> {
            List<String> problems = new ArrayList<>();
            if (n.signum() <= 0) {
                problems.add("must be greater than 0");
            }
            if (Numbers.integerDigits(n) > 15) {
                problems.add("must have at most 15 digits before the decimal point");
            }
            if (Numbers.fractionDigits(n) > 4) {
                problems.add("must have at most 4 decimal places");
            }
            return problems;
        });
    }

    /** Whole number {@code >= 0} (0 = studio), at most 32767. */
    public static ListingValidator bedrooms() {
        return numeric(ListingField.BEDROOMS, (field, raw, n) -> {
            if (!Numbers.isWhole(n)) {
                return List.of("must be a whole number");
            }
            if (n.signum() < 0) {
                return List.of("must be 0 or greater");
            }
            if (n.compareTo(MAX_BEDROOMS) > 0) {
                return List.of("must be at most " + Short.MAX_VALUE);
            }
            return List.of();
        });
    }

    /** Number {@code > 0} with at most 2 decimal places (and at most 9999.99 to fit Numeric(6,2)). */
    public static ListingValidator bathrooms() {
        return numeric(ListingField.BATHROOMS, (field, raw, n) -> {
            List<String> problems = new ArrayList<>();
            if (n.signum() <= 0) {
                problems.add("must be greater than 0");
            }
            if (Numbers.fractionDigits(n) > 2) {
                problems.add("must have at most 2 decimal places");
            }
            if (Numbers.integerDigits(n) > 4) {
                problems.add("must be less than 10000");
            }
            return problems;
        });
    }

    /** Whole number {@code > 0} that fits an Integer column. */
    public static ListingValidator sqft() {
        return numeric(ListingField.SQFT, (field, raw, n) -> {
            if (!Numbers.isWhole(n)) {
                return List.of("must be a whole number");
            }
            if (n.signum() <= 0) {
                return List.of("must be greater than 0");
            }
            if (n.compareTo(MAX_SQFT) > 0) {
                return List.of("must be at most " + Integer.MAX_VALUE);
            }
            return List.of();
        });
    }

    /** Finite number from -90 to 90 inclusive. Negative values are allowed. */
    public static ListingValidator latitude() {
        return numeric(ListingField.LATITUDE, (field, raw, n) -> n.abs().compareTo(LAT_LIMIT) > 0
                ? List.of("must be between -90 and 90")
                : List.of());
    }

    /** Finite number from -180 to 180 inclusive. Negative values are allowed. */
    public static ListingValidator longitude() {
        return numeric(ListingField.LONGITUDE, (field, raw, n) -> n.abs().compareTo(LNG_LIMIT) > 0
                ? List.of("must be between -180 and 180")
                : List.of());
    }

    @FunctionalInterface
    interface NumberRule {
        List<String> check(ListingField field, String raw, BigDecimal value);
    }

    /** Skips missing/blank values (reported by {@link #required}), then requires a number and applies the rule. */
    static ListingValidator numeric(ListingField field, NumberRule rule) {
        return listing -> {
            String raw = field.valueOf(listing);
            if (present(raw) == null) {
                return List.of();
            }
            var parsed = Numbers.parse(raw);
            if (parsed.isEmpty()) {
                return List.of(ValidationError.of(field, field.column() + " " + quote(raw) + " is not a valid number"));
            }
            return rule.check(field, raw, parsed.get()).stream()
                    .map(problem -> ValidationError.of(field, field.column() + " " + quote(raw) + " " + problem))
                    .toList();
        };
    }

    private static String present(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
