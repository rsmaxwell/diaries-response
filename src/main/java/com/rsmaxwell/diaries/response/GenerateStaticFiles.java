package com.rsmaxwell.diaries.response;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DiariesConfig;
import com.rsmaxwell.diaries.response.template.ImageInfo;
import com.rsmaxwell.diaries.response.utilities.MyFileUtilities;
import com.rsmaxwell.diaries.response.utilities.MyGeneralUtilities;
import com.rsmaxwell.diaries.response.utilities.MyImageUtilities;

public class GenerateStaticFiles {

	private static final Logger log = LogManager.getLogger(GenerateStaticFiles.class);

	private DiariesConfig diariesConfig;
	private String template;

	static private ObjectMapper mapper = new ObjectMapper();

	static Option createOption(String shortName, String longName, String argName, String description, boolean required) {
		return Option.builder(shortName).longOpt(longName).argName(argName).desc(description).hasArg().required(required).build();
	}

	public GenerateStaticFiles(DiariesConfig diariesConfig) throws IOException {
		this.diariesConfig = diariesConfig;
		template = MyGeneralUtilities.readTextResource("image.svg", GenerateStaticFiles.class);
	}

	public static void main(String[] args) throws Exception {

		Option configOption = createOption("c", "config", "Configuration", "Configuration", true);

		// @formatter:off
		Options options = new Options();
		options.addOption(configOption);
		// @formatter:on

		CommandLineParser commandLineParser = new DefaultParser();
		CommandLine commandLine = commandLineParser.parse(options, args);

		String filename = commandLine.getOptionValue("config");
		Config config = Config.read(filename);
		DiariesConfig diariesConfig = config.getDiaries();

		GenerateStaticFiles p = new GenerateStaticFiles(diariesConfig);
		p.populateDiaries();

		log.info("Success");
	}

	public void populateDiaries() throws Exception {

		String original = diariesConfig.getOriginal();
		File originalDir = new File(original);

		File[] diaryDirs = originalDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File f, String name) {
				if (!f.isDirectory()) {
					return false;
				}
				if (!name.startsWith("diary")) {
					return false;
				}
				return true;
			}
		});

		for (File diarydir : diaryDirs) {
			populatePages(diarydir);
		}
	}

	public void populatePages(File diaryDir) throws Exception {

		String diaryName = diaryDir.getName();

		log.info(String.format("Refreshing '%s'", diaryName));

		File[] imageFiles = diaryDir.listFiles(new FileFilter() {

			@Override
			public boolean accept(File f) {

				if (!f.isFile()) {
					return false;
				}

				String name = f.getName();

				if (!name.startsWith("img")) {
					return false;
				}
				if (!name.endsWith(".jpg")) {
					return false;
				}
				return true;
			}
		});

		String original = diariesConfig.getOriginal();
		String working = diariesConfig.getWorking();

		// Make sure there are no extraneous files in the working diary directory.
		// i.e. Check every directory in the working diary directory matches a Page in
		// the original
		File workingDiaryDir = Paths.get(working, diaryName).toFile();
		for (File pageDir : workingDiaryDir.listFiles()) {
			String pageName = pageDir.getName() + ".jpg";
			File imageFile = Paths.get(original, diaryName, pageName).toFile();

			if (!imageFile.exists()) {
				String message = String.format("The Page directory not found: '%s'", imageFile.getAbsolutePath());
				log.info(message);
				throw new Exception(message);
			}

			deleteExtraneousFiles(pageDir);
		}

		// Make sure there is a working page directory for each page in the original
		// And also make sure the working page directory contains an svgFile and a set
		// of thumbs
		for (File imageFile : imageFiles) {
			String pageName = MyFileUtilities.removeExtension(imageFile);
			File workingPageDir = Paths.get(working, diaryName, pageName).toFile();

			if (!workingPageDir.exists()) {
				log.info(String.format("creating the Page directory: '%s'", workingPageDir.getAbsolutePath()));
				workingPageDir.mkdirs();
			}

			Path svgFile = Paths.get(workingPageDir.getAbsolutePath(), "image.svg");
			if (!svgFile.toFile().exists()) {
				generateSvgFile(svgFile, imageFile);
			}

			generateThumbs(workingPageDir, imageFile);
		}
	}

	private void deleteExtraneousFiles(File pageDir) throws Exception {

		List<String> exclude = new ArrayList<String>();
		exclude.add("image.svg");
		exclude.add("info.json");

		List<Integer> heights = Arrays.asList(50, 100, 150, 200);
		for (int height : heights) {
			exclude.add(String.format("thumb-%d.jpg", height));
		}

		File[] files = pageDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				if (exclude.contains(name)) {
					return false;
				}
				return true;
			}
		});

		for (File file : files) {
			log.info(String.format("deleting: '%s'", file.getAbsolutePath()));
			boolean ok = file.delete();
			if (!ok) {
				throw new Exception(String.format("Could not delete file: %s", file.getAbsoluteFile()));
			}
		}
	}

	private void generateSvgFile(Path svgFile, File imageFile) throws Exception {
		log.info(String.format("generating: '%s'", svgFile.toString()));
		Files.write(svgFile, template.getBytes());
	}

	private void generateThumbs(File workingPageDir, File imageFile) throws Exception {

		try (ImageInputStream input = ImageIO.createImageInputStream(imageFile)) {
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new Exception(String.format("No ImageReader found for the file: %s", imageFile.getAbsoluteFile()));
			}

			ImageInfo info = new ImageInfo();
			ImageReader reader = readers.next();
			reader.setInput(input);
			info.setHeight(reader.getHeight(0));
			info.setWidth(reader.getWidth(0));
			reader.dispose();

			Path infoPath = Paths.get(workingPageDir.getAbsolutePath(), "info.json");
			if (!infoPath.toFile().exists()) {
				log.info(String.format("generating: '%s'", infoPath.toString()));
				Files.write(infoPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(info));
			}

			List<Integer> heights = Arrays.asList(50, 100, 150, 200);
			for (int height : heights) {

				Path thumbPath = Paths.get(workingPageDir.getAbsolutePath(), String.format("thumb-%d.jpg", height));
				if (!thumbPath.toFile().exists()) {
					generateThumb(imageFile, thumbPath, info, height);
				}
			}

		} catch (IOException e) {
			System.err.println("Failed to read image dimensions: " + e.getMessage());
		}
	}

	private void generateThumb(File imageFile, Path thumbPath, ImageInfo info, int height) throws IOException {
		log.info(String.format("generating: '%s'", thumbPath.toString()));

		double ratio = (double) height / (double) info.getHeight();
		BufferedImage img = ImageIO.read(imageFile);
		BufferedImage scaled = MyImageUtilities.scale(img, ratio);
		byte[] thumbBytes = MyImageUtilities.toJpegBytes(scaled);
		Files.write(thumbPath, thumbBytes);
	}
}
