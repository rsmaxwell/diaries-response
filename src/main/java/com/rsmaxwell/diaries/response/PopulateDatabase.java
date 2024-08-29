package com.rsmaxwell.diaries.response;

import java.io.File;
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
import com.rsmaxwell.diaries.common.config.Diaries;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Role;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.repository.RoleRepository;
import com.rsmaxwell.diaries.response.repositoryImpl.DiaryRepositoryImpl;
import com.rsmaxwell.diaries.response.repositoryImpl.RoleRepositoryImpl;
import com.rsmaxwell.diaries.response.utilities.GetEntityManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

public class PopulateDatabase {

	private static final Logger log = LogManager.getLogger(PopulateDatabase.class);

	private DiaryRepository diaryRepository;
	private RoleRepository roleRepository;

	public PopulateDatabase(DiaryRepository diaryRepository, RoleRepository roleRepository) {
		this.diaryRepository = diaryRepository;
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
		Diaries diaries = config.getDiaries();

		EntityTransaction tx = null;
		// @formatter:off
		try (EntityManagerFactory entityManagerFactory = GetEntityManager.adminFactory(dbConfig); 
			 EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			// @formatter:on

			DiaryRepository diaryRepository = new DiaryRepositoryImpl(entityManager);
			RoleRepository roleRepository = new RoleRepositoryImpl(entityManager);
			PopulateDatabase p = new PopulateDatabase(diaryRepository, roleRepository);

			tx = entityManager.getTransaction();
			tx.begin();

			p.populateDiaries(diaries);
			p.populateRoles();

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

	public void populateDiaries(Diaries diariesConfig) throws Exception {

		log.info("Refresh the diaries");

		String root = diariesConfig.getPath();
		File rootDir = new File(root);

		File[] directories = rootDir.listFiles(new FilenameFilter() {

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

		for (File dir : directories) {
			String name = dir.getName();
			Optional<Diary> optional = diaryRepository.findByPath(dir.getName());

			if (optional.isPresent()) {
				log.info(String.format("The filesystem Diary '%s' already has a database record", name));
			} else {
				log.info(String.format("creating database Diary '%s' to correspond with the filesystem directory", name));
				diaryRepository.save(new Diary(name));
			}
		}

		Iterable<Diary> diaries = diaryRepository.findAll();
		for (Diary diary : diaries) {
			String name = diary.getPath();
			Path path = Paths.get(root, name);

			if (path.toFile().exists()) {
				log.info(String.format("The database Diary '%s' already has a corresponding directory", name));
			} else {
				String message = String.format("The database Diary '%s' does not have a corresponding directory", name);
				log.info(message);
				throw new Exception(message);
			}
		}
	}

	public void populateRoles() throws Exception {

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
