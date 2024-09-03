package com.rsmaxwell.diaries.response.repository;

import com.rsmaxwell.diaries.response.dto.PersonDTO;
import com.rsmaxwell.diaries.response.model.Person;

public interface PersonRepository extends CrudRepository<Person, PersonDTO, Long> {

}