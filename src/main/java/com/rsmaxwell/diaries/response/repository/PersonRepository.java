package com.rsmaxwell.diaries.response.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Person;

public interface PersonRepository extends CrudRepository<Person, Long> {

	List<Diary> findByFirstName(String firstName);

	Person findById(long id);
}