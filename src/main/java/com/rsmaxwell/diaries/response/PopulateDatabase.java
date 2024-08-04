package com.rsmaxwell.diaries.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Role;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;
import com.rsmaxwell.diaries.response.repository.RoleRepository;

@SpringBootApplication
public class PopulateDatabase {

	private static final Logger log = LogManager.getLogger(PopulateDatabase.class);

	public static void main(String[] args) {
		SpringApplication.run(PopulateDatabase.class);
	}

	@Bean
	CommandLineRunner populate(DiaryRepository repository) {
		return (args) -> {

			log.info("Refresh the diaries");

			repository.deleteAll();

			repository.save(new Diary("diary-1828-and-1829-and-jan-1830"));
			repository.save(new Diary("diary-1830"));
			repository.save(new Diary("diary-1831"));
			repository.save(new Diary("diary-1832"));
			repository.save(new Diary("diary-1834"));
			repository.save(new Diary("diary-1835"));
			repository.save(new Diary("diary-1836"));
			repository.save(new Diary("diary-1837"));
			repository.save(new Diary("diary-1838"));
			repository.save(new Diary("diary-1839"));
		};
	}

	@Bean
	CommandLineRunner role(RoleRepository repository) {
		return (args) -> {

			log.info("Refresh the roles");

			repository.deleteAll();

			log.info("Save the list of roles");
			repository.save(new Role("admin"));
			repository.save(new Role("editor"));
			repository.save(new Role("viewer"));
		};
	}
}
