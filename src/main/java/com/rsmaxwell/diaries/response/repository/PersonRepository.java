package com.rsmaxwell.diaries.response.repository;

import org.springframework.data.repository.CrudRepository;

import com.rsmaxwell.diaries.response.model.Person;

public interface PersonRepository extends CrudRepository<Person, Long> {

	Person findByUsername(String username);

	Person findById(long id);
}