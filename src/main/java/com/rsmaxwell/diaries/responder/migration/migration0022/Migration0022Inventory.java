package com.rsmaxwell.diaries.responder.migration.migration0022;

import java.io.BufferedWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.rsmaxwell.diaries.responder.config.Config;
import com.rsmaxwell.diaries.responder.config.DbConfig;
import com.rsmaxwell.diaries.responder.config.User;

/**
 * Read-only inventory for change 0022. This program deliberately performs no
 * schema or data mutations.
 */
public final class Migration0022Inventory {

	private static final String QUERY = """
			select f.id,
			       f.version,
			       f.year,
			       f.month,
			       f.day,
			       f.sequence,
			       f.text,
			       md5(f.text) as text_md5,
			       m.id as marquee_id,
			       m.page_id as inferred_page_id,
			       p.id as valid_page_id
			from fragment f
			left join marquee m on m.fragment_id = f.id
			left join page p on p.id = m.page_id
			order by f.id, m.id
			""";

	private static final String[] CSV_HEADER = {
			"fragment_id", "fragment_version", "year", "month", "day",
			"sequence", "inferred_page_id", "marquee_id", "classification",
			"embedded_image_count", "embedded_image_src_values", "text_md5",
			"text_preview", "reason"
	};

	private Migration0022Inventory() {
	}

	public static void main(String[] args) throws Exception {
		Options options = new Options();
		options.addOption(Option.builder("c").longOpt("config").hasArg().required()
				.desc("Responder JSON configuration file").build());
		options.addOption(Option.builder("o").longOpt("output").hasArg().required()
				.desc("New or empty output directory").build());

		CommandLine commandLine = new DefaultParser().parse(options, args);
		Path outputDirectory = Path.of(commandLine.getOptionValue("output")).toAbsolutePath().normalize();
		prepareOutputDirectory(outputDirectory);

		Config config = Config.read(commandLine.getOptionValue("config"));
		DbConfig db = config.getDb();
		User admin = db.getAdmin();
		Class.forName(db.getJdbc().getDriver());

		List<InventoryRecord> records;
		try (Connection connection = DriverManager.getConnection(
				db.getJdbcUrl(db.getDatabase()), admin.getUsername(), admin.getPassword())) {
			connection.setAutoCommit(false);
			connection.setReadOnly(true);
			connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
			records = readInventory(connection);
			connection.rollback();
		}

		writeCsv(outputDirectory.resolve("0022-inventory.csv"), records);
		writeCsv(outputDirectory.resolve("0022-safe-marquee-candidates.csv"), filter(records, Classification.ORDINARY_MARQUEE_CANDIDATE));
		writeCsv(outputDirectory.resolve("0022-legacy-image-candidates.csv"), filter(records, Classification.LEGACY_IMAGE_CANDIDATE));
		writeCsv(outputDirectory.resolve("0022-anomalies.csv"), filter(records, Classification.ORPHAN_OR_INCONSISTENT));
		writeSafeTypeSql(outputDirectory.resolve("005-apply-safe-types.generated.sql"), records);
		writeSummary(outputDirectory.resolve("0022-summary.json"), records);

		System.out.printf("0022 inventory written to %s (%d fragments)%n", outputDirectory, records.size());
	}

	static List<InventoryRecord> readInventory(Connection connection) throws Exception {
		Map<Long, MutableRecord> rows = new LinkedHashMap<>();
		try (PreparedStatement statement = connection.prepareStatement(QUERY);
				ResultSet result = statement.executeQuery()) {
			while (result.next()) {
				long fragmentId = result.getLong("id");
				MutableRecord row = rows.get(fragmentId);
				if (row == null) {
					row = new MutableRecord(
							fragmentId,
							resultLong(result, "version"),
							result.getInt("year"),
							result.getInt("month"),
							result.getInt("day"),
							result.getBigDecimal("sequence"),
							result.getString("text"),
							result.getString("text_md5"));
					rows.put(fragmentId, row);
				}

				Long marqueeId = resultLong(result, "marquee_id");
				if (marqueeId != null) {
					row.marquees.add(new MarqueeReference(
							marqueeId,
							resultLong(result, "inferred_page_id"),
							resultLong(result, "valid_page_id") != null));
				}
			}
		}

		List<InventoryRecord> inventory = new ArrayList<>();
		for (MutableRecord row : rows.values()) {
			inventory.add(row.classify());
		}
		return inventory;
	}

	private static Long resultLong(ResultSet result, String column) {
		try {
			long value = result.getLong(column);
			return result.wasNull() ? null : value;
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to read column " + column, exception);
		}
	}

	private static List<InventoryRecord> filter(List<InventoryRecord> records, Classification classification) {
		return records.stream().filter(record -> record.classification() == classification).toList();
	}

	private static void prepareOutputDirectory(Path outputDirectory) throws Exception {
		Files.createDirectories(outputDirectory);
		try (var children = Files.list(outputDirectory)) {
			if (children.findAny().isPresent()) {
				throw new IllegalArgumentException("Output directory must be empty: " + outputDirectory);
			}
		}
	}

	private static void writeCsv(Path file, List<InventoryRecord> records) throws Exception {
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writer.write(String.join(",", CSV_HEADER));
			writer.newLine();
			for (InventoryRecord record : records) {
				writer.write(record.toCsv());
				writer.newLine();
			}
		}
	}

	private static void writeSummary(Path file, List<InventoryRecord> records) throws Exception {
		Map<String, Object> summary = new LinkedHashMap<>();
		summary.put("totalFragments", records.size());
		for (Classification classification : Classification.values()) {
			summary.put(classification.name(), records.stream().filter(r -> r.classification() == classification).count());
		}
		summary.put("fragmentsWithMultipleImages", records.stream().filter(r -> r.imageSources().size() > 1).count());
		Set<String> uniqueSources = new LinkedHashSet<>();
		records.forEach(record -> uniqueSources.addAll(record.imageSources()));
		uniqueSources.remove("");
		summary.put("uniqueEmbeddedImageSources", uniqueSources.size());

		new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(file.toFile(), summary);
	}

	private static void writeSafeTypeSql(Path file, List<InventoryRecord> records) throws Exception {
		List<InventoryRecord> safe = filter(records, Classification.ORDINARY_MARQUEE_CANDIDATE);
		try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			writer.write("\\set ON_ERROR_STOP on");
			writer.newLine();
			writer.write("BEGIN;");
			writer.newLine();
			writer.write("CREATE TEMP TABLE migration_0022_expected (fragment_id bigint primary key, expected_version bigint, expected_page_id bigint, expected_text_md5 text);");
			writer.newLine();
			if (!safe.isEmpty()) {
				writer.write("INSERT INTO migration_0022_expected VALUES");
				writer.newLine();
				for (int index = 0; index < safe.size(); index++) {
					InventoryRecord record = safe.get(index);
					writer.write(String.format("  (%d, %d, %d, '%s')%s%n",
							record.fragmentId(), record.fragmentVersion(), record.pageId(), record.textMd5(),
							index + 1 == safe.size() ? ";" : ","));
				}
			}
			writer.write("""
					DO $$
					BEGIN
					  IF EXISTS (
					    SELECT 1
					    FROM migration_0022_expected e
					    LEFT JOIN fragment f ON f.id = e.fragment_id
					    WHERE f.id IS NULL
					       OR f.version <> e.expected_version
					       OR f.page_id IS DISTINCT FROM e.expected_page_id
					       OR md5(f.text) IS DISTINCT FROM e.expected_text_md5
					       OR (f.type IS NOT NULL AND f.type <> 'MARQUEE')
					  ) THEN
					    RAISE EXCEPTION '0022 reviewed inventory no longer matches the database';
					  END IF;
					END $$;

					UPDATE fragment f
					SET type = 'MARQUEE'
					FROM migration_0022_expected e
					WHERE f.id = e.fragment_id
					  AND f.type IS NULL;

					COMMIT;
					""");
		}
	}

	private static String csv(Object value) {
		String text = value == null ? "" : value.toString();
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}

	private static String preview(String text) {
		if (text == null) {
			return "";
		}
		String compact = text.replaceAll("\\s+", " ").trim();
		return compact.length() <= 160 ? compact : compact.substring(0, 157) + "...";
	}

	enum Classification {
		ORDINARY_MARQUEE_CANDIDATE,
		LEGACY_IMAGE_CANDIDATE,
		ORPHAN_OR_INCONSISTENT
	}

	record MarqueeReference(long id, Long pageId, boolean validPage) {
	}

	record InventoryRecord(long fragmentId, long fragmentVersion, int year, int month, int day,
			BigDecimal sequence, Long pageId, Long marqueeId, Classification classification,
			List<String> imageSources, String textMd5, String textPreview, String reason) {

		String toCsv() {
			return String.join(",",
					csv(fragmentId), csv(fragmentVersion), csv(year), csv(month), csv(day), csv(sequence),
					csv(pageId), csv(marqueeId), csv(classification), csv(imageSources.size()),
					csv(String.join(" | ", imageSources)), csv(textMd5), csv(textPreview), csv(reason));
		}
	}

	static final class MutableRecord {
		final long id;
		final long version;
		final int year;
		final int month;
		final int day;
		final BigDecimal sequence;
		final String text;
		final String textMd5;
		final List<MarqueeReference> marquees = new ArrayList<>();

		MutableRecord(long id, long version, int year, int month, int day, BigDecimal sequence, String text, String textMd5) {
			this.id = id;
			this.version = version;
			this.year = year;
			this.month = month;
			this.day = day;
			this.sequence = sequence;
			this.text = text;
			this.textMd5 = textMd5;
		}

		InventoryRecord classify() throws Exception {
			List<String> imageSources = LegacyImageDetector.findImageSources(text);
			Classification classification;
			String reason;
			Long pageId = null;
			Long marqueeId = null;

			if (marquees.size() != 1) {
				classification = Classification.ORPHAN_OR_INCONSISTENT;
				reason = marquees.isEmpty() ? "fragment has no marquee" : "fragment has more than one marquee";
			} else {
				MarqueeReference marquee = marquees.get(0);
				pageId = marquee.pageId();
				marqueeId = marquee.id();
				if (pageId == null) {
					classification = Classification.ORPHAN_OR_INCONSISTENT;
					reason = "marquee page_id is null";
				} else if (!marquee.validPage()) {
					classification = Classification.ORPHAN_OR_INCONSISTENT;
					reason = "marquee page_id does not resolve";
				} else if (!imageSources.isEmpty()) {
					classification = Classification.LEGACY_IMAGE_CANDIDATE;
					reason = "fragment HTML contains one or more img elements";
				} else {
					classification = Classification.ORDINARY_MARQUEE_CANDIDATE;
					reason = "one valid marquee/page and no embedded img element";
				}
			}

			return new InventoryRecord(id, version, year, month, day, sequence, pageId, marqueeId,
					classification, List.copyOf(imageSources), textMd5, preview(text), reason);
		}
	}
}
