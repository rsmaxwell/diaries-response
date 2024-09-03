package com.rsmaxwell.diaries.response;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DbConfig;
import com.rsmaxwell.diaries.common.config.DiariesConfig;
import com.rsmaxwell.diaries.response.dto.PageDTO;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Page;
import com.rsmaxwell.diaries.response.model.Role;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.repository.PageRepository;
import com.rsmaxwell.diaries.response.repository.RoleRepository;
import com.rsmaxwell.diaries.response.repositoryImpl.DiaryRepositoryImpl;
import com.rsmaxwell.diaries.response.repositoryImpl.PageRepositoryImpl;
import com.rsmaxwell.diaries.response.repositoryImpl.RoleRepositoryImpl;
import com.rsmaxwell.diaries.response.utilities.GetEntityManager;
import com.rsmaxwell.diaries.response.utilities.MyFileUtilities;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class SyncroniseDatabase {

	private static final Logger log = LogManager.getLogger(SyncroniseDatabase.class);

	private DiariesConfig diariesConfig;
	private DiaryRepository diaryRepository;
	private PageRepository pageRepository;
	private RoleRepository roleRepository;

	public SyncroniseDatabase(DiariesConfig diariesConfig, DiaryRepository diaryRepository, PageRepository pageRepository, RoleRepository roleRepository) {
		this.diariesConfig = diariesConfig;
		this.diaryRepository = diaryRepository;
		this.pageRepository = pageRepository;
		this.roleRepository = roleRepository;
	}

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
		DbConfig dbConfig = config.getDb();
		DiariesConfig diariesConfig = config.getDiaries();

		EntityTransaction tx = null;
		// @formatter:off
		try (EntityManagerFactory entityManagerFactory = GetEntityManager.adminFactory(dbConfig); 
			 EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			// @formatter:on

			DiaryRepository diaryRepository = new DiaryRepositoryImpl(entityManager);
			PageRepository pageRepository = new PageRepositoryImpl(entityManager);
			RoleRepository roleRepository = new RoleRepositoryImpl(entityManager);
			SyncroniseDatabase p = new SyncroniseDatabase(diariesConfig, diaryRepository, pageRepository, roleRepository);

			tx = entityManager.getTransaction();
			tx.begin();

			p.synchroniseDiaries();
			p.synchroniseRoles();

			tx.commit();

			log.info("Success");

		} catch (Exception e) {
			log.catching(e);
			if (tx != null) {
				tx.rollback();
			}
			return;
		}
	}

	public void synchroniseDiaries() throws Exception {

		log.info("Refresh the diaries");

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

		// Make sure every database Diary matches an original diary on the file system
		Iterable<Diary> diaries = diaryRepository.findAll();
		for (Diary diary : diaries) {
			String name = diary.getName();
			Path path = Paths.get(original, name);

			log.info(String.format("%s", name));

			if (!path.toFile().exists()) {
				String message = String.format("The database Diary '%s' does not have a corresponding directory", name);
				log.info(message);
				throw new Exception(message);
			}
		}

		// Make sure there is a database Diary for each original diary on the file
		// system
		// Also synchronise the database pages with Pages on the file system
		for (File diarydir : diaryDirs) {
			String name = diarydir.getName();
			Optional<Diary> optional = diaryRepository.findByName(diarydir.getName());

			if (optional.isEmpty()) {
				log.info(String.format("creating database Diary '%s' to correspond with the filesystem directory", name));
				diaryRepository.save(new Diary(name));
			}

			synchronisePages(diariesConfig, diarydir);
		}
	}

	public void synchronisePages(DiariesConfig diariesConfig, File diaryDir) throws Exception {

		log.info("Refresh the pages");

		String original = diariesConfig.getOriginal();
		String diaryName = MyFileUtilities.removeExtension(diaryDir.getName());

		Optional<Diary> optionalDiary = diaryRepository.findByName(diaryName);
		if (optionalDiary.isEmpty()) {
			throw new Exception(String.format("Diary '%s' not found in database"));
		}
		Diary diary = optionalDiary.get();

		// Make sure every database Page matches an original image file
		Iterable<PageDTO> pages = pageRepository.findAllByDiary(diary);
		for (PageDTO page : pages) {
			String pageName = page.getName();
			File imageFile = Paths.get(original, diaryName, String.format("%s.jpg", pageName)).toFile();

			if (!imageFile.exists()) {
				throw new Exception(String.format("The database Page '%s/%s' does not match an original image file", diaryName, pageName));
			}
		}

		// Find the names of the original image files for this diary
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

		// Make sure there is a database Page for each original image file
		for (File imageFile : imageFiles) {

			String pageName = MyFileUtilities.removeExtension(imageFile.getName());
			Optional<PageDTO> optionalPage = pageRepository.findByDiaryAndName(diary, pageName);

			if (optionalPage.isEmpty()) {
				log.info(String.format("creating database Page '%s/%s' to match the filesystem directory", diaryName, pageName));
				pageRepository.save(new Page(diary, pageName));
			}
		}
	}

	public void synchroniseRoles() throws Exception {

		log.info("Refresh the roles");

		List<String> list = new ArrayList<String>();
		list.add("admin");
		list.add("editor");
		list.add("viewer");

		for (String name : list) {
			Optional<Role> optional = roleRepository.findByName(name);

			if (optional.isPresent()) {
				log.info(String.format("Role '%s' already has a database record", name));
			} else {
				log.info(String.format("creating Role '%s' database record", name));
				// diaryRepository.save(new Diary(name));
			}
		}

		Iterable<Role> roles = roleRepository.findAll();
		for (Role role : roles) {
			String name = role.getName();

			if (list.contains(name)) {
				log.info(String.format("The database Role '%s' is correct", name));
			} else {
				String message = String.format("The database Role '%s' sould not be present", name);
				log.info(message);
				throw new Exception(message);
			}
		}
	}
}
