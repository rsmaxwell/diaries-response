package com.rsmaxwell.diaries.response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.common.config.Config;
import com.rsmaxwell.diaries.common.config.DbConfig;
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

	public static void main(String[] args) throws Exception {

		Config config = Config.read();
		DbConfig dbConfig = config.getDb();

		EntityTransaction tx = null;
		// @formatter:off
		try (EntityManagerFactory entityManagerFactory = GetEntityManager.factory(dbConfig); 
			 EntityManager entityManager = entityManagerFactory.createEntityManager()) {
			// @formatter:on

			DiaryRepository diaryRepository = new DiaryRepositoryImpl(entityManager);
			RoleRepository roleRepository = new RoleRepositoryImpl(entityManager);
			PopulateDatabase p = new PopulateDatabase(diaryRepository, roleRepository);

			tx = entityManager.getTransaction();
			tx.begin();

			p.populateDiaries();
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

	public void populateDiaries() {

		log.info("Refresh the diaries");

		diaryRepository.deleteAll();

		diaryRepository.save(new Diary("diary-1828-and-1829-and-jan-1830"));
		diaryRepository.save(new Diary("diary-1830"));
		diaryRepository.save(new Diary("diary-1831"));
		diaryRepository.save(new Diary("diary-1832"));
		diaryRepository.save(new Diary("diary-1834"));
		diaryRepository.save(new Diary("diary-1835"));
		diaryRepository.save(new Diary("diary-1836"));
		diaryRepository.save(new Diary("diary-1837"));
		diaryRepository.save(new Diary("diary-1838"));
		Diary x = diaryRepository.save(new Diary("diary-1839"));

		diaryRepository.delete(x);
	}

	public void populateRoles() {

		log.info("Refresh the roles");

		roleRepository.deleteAll();

		roleRepository.save(new Role("admin"));
		roleRepository.save(new Role("editor"));
		roleRepository.save(new Role("viewer"));
	}
}
