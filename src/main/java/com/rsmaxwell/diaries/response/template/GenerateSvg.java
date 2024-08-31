package com.rsmaxwell.diaries.response.template;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GenerateSvg {

	private static final Logger log = LogManager.getLogger(GenerateSvg.class);

	static private ObjectMapper mapper = new ObjectMapper();

	private String template;

	public GenerateSvg() throws IOException {

		InputStream inputStream = GenerateSvg.class.getResourceAsStream("image.svg");

		StringBuilder resultStringBuilder = new StringBuilder();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = br.readLine()) != null) {
				resultStringBuilder.append(line).append("\n");
			}
		}

		template = resultStringBuilder.toString();

	}

	public String generate(File svgFile, File imageFile) throws Exception {

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

			Path infoPath = Paths.get(svgFile.getParent(), "info.json");
			Files.write(infoPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(info));

			List<Integer> heights = Arrays.asList(50, 100, 150, 200);
			for (int height : heights) {

				Path thumbPath = Paths.get(svgFile.getParent(), String.format("thumb-%d.jpg", height));
				if (!thumbPath.toFile().exists()) {
					generateThumbnail(svgFile, imageFile, thumbPath, info, height);
				}
			}

		} catch (IOException e) {
			System.err.println("Failed to read image dimensions: " + e.getMessage());
		}

		return template;
	}

	private void generateThumbnail(File svgFile, File imageFile, Path thumbPath, ImageInfo info, int height) throws IOException {

		log.info(String.format("generating: '%s'", thumbPath.toString()));

		double ratio = (double) height / (double) info.getHeight();
		BufferedImage img = ImageIO.read(imageFile);
		BufferedImage scaled = scale(img, ratio);
		byte[] thumbBytes = toJpegBytes(scaled);
		Files.write(thumbPath, thumbBytes);
	}

	private BufferedImage scale(BufferedImage source, double ratio) {
		int w = (int) (source.getWidth() * ratio);
		int h = (int) (source.getHeight() * ratio);
		BufferedImage bi = getCompatibleImage(w, h);
		Graphics2D g2d = bi.createGraphics();
		double xScale = (double) w / source.getWidth();
		double yScale = (double) h / source.getHeight();
		AffineTransform at = AffineTransform.getScaleInstance(xScale, yScale);
		g2d.drawRenderedImage(source, at);
		g2d.dispose();
		return bi;
	}

	private BufferedImage getCompatibleImage(int w, int h) {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice gd = ge.getDefaultScreenDevice();
		GraphicsConfiguration gc = gd.getDefaultConfiguration();
		BufferedImage image = gc.createCompatibleImage(w, h);
		return image;
	}

	public static byte[] toJpegBytes(BufferedImage image) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "jpeg", baos);
		return baos.toByteArray();
	}
}
