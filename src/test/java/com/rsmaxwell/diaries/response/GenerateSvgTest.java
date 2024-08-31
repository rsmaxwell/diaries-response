package com.rsmaxwell.diaries.response;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.Diaries;
import com.rsmaxwell.diaries.response.template.GenerateSvg;

public class GenerateSvgTest {

	static Option createOption(String shortName, String longName, String argName, String description, boolean required) {
		return Option.builder(shortName).longOpt(longName).argName(argName).desc(description).hasArg().required(required).build();
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
		Diaries diariesConfig = config.getDiaries();
		String original = diariesConfig.getOriginal();
		String working = diariesConfig.getWorking();

		String diaryName = "diary-1828-and-1829-and-jan-1830";
		String pageName = "img2877";

		File imageFile = Paths.get(original, diaryName, pageName + ".jpg").toFile();
		File svgFile = Paths.get(working, diaryName, pageName, "image.svg").toFile();

		GenerateSvg generator = new GenerateSvg();
		String template = generator.generate(svgFile, imageFile);

		System.out.println("template:");
		System.out.println(template);
	}

}
