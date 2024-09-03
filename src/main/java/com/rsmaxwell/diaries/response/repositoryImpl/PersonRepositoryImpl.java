package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;

import com.rsmaxwell.diaries.response.dto.PersonDTO;
import com.rsmaxwell.diaries.response.model.Person;
import com.rsmaxwell.diaries.response.repository.PersonRepository;

import jakarta.persistence.EntityManager;

public class PersonRepositoryImpl extends AbstractCrudRepository<Person, PersonDTO, Long> implements PersonRepository {

	public PersonRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "person";
	}

	public <S extends Person> Object getPrimaryKeyValueAsString(S entity) {
		return entity.getId();
	}

	public String convertPrimaryKeyValueToString(Long id) {
		return id.toString();
	}

	public <S extends Person> void setPrimaryKeyValue(S entity, Object value) {
		entity.setId((Long) value);
	}

	public String getPrimaryKeyField() {
		return "id";
	}

	public List<String> getFields() {
		List<String> list = new ArrayList<String>();
		list.add("username");
		list.add("passwordHash");
		list.add("firstName");
		list.add("lastName");
		return list;
	}

	public List<String> getDTOFields() {
		List<String> list = new ArrayList<String>();
		list.add("id");
		list.add("username");
		list.add("firstName");
		list.add("lastName");
		return list;
	}

	public <S extends Person> List<Object> getValues(S entity) {
		List<Object> list = new ArrayList<Object>();
		list.add(entity.getUsername());
		list.add(entity.getPasswordHash());
		list.add(entity.getFirstName());
		list.add(entity.getLastName());
		return list;
	}

	public PersonDTO newDTO(Object[] result) {
		Long id = ((Number) result[0]).longValue();
		String username = (String) result[1];
		String firstName = (String) result[2];
		String lastName = (String) result[3];
		return new PersonDTO(id, username, firstName, lastName);
	}
}