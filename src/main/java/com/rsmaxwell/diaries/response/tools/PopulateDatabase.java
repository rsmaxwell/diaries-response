package com.rsmaxwell.diaries.response.tools;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;

@SpringBootApplication
public class PopulateDatabase {

	private static final Logger log = LogManager.getLogger(PopulateDatabase.class);

	public static void main(String[] args) {
		SpringApplication.run(PopulateDatabase.class);
	}

	@Bean
	public CommandLineRunner demo(DiaryRepository repository) {
		return (args) -> {

			log.info("Save the list of diaries");
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

			// fetch all diaries
			log.info("Diaries found with findAll():");
			log.info("-------------------------------");
			repository.findAll().forEach(diary -> {
				log.info(diary.toString());
			});
			log.info("");

			// fetch an individual diary by ID
			Diary diary = repository.findById(1L);
			log.info("Diary found with findById(1L):");
			log.info("--------------------------------");
			log.info(diary.toString());
			log.info("");

			// fetch diaries by path
			log.info("Diary found with findByPath('diary-1837'):");
			log.info("--------------------------------------------");
			repository.findByPath("diary-1837").forEach(d -> {
				log.info(d.toString());
			});
			log.info("Success");
		};
	}
}
