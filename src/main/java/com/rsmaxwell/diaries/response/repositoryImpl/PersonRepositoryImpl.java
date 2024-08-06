package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;

import com.rsmaxwell.diaries.response.model.Person;
import com.rsmaxwell.diaries.response.repository.PersonRepository;

import jakarta.persistence.EntityManager;

public class PersonRepositoryImpl extends AbstractCrudRepository<Person, Long> implements PersonRepository {

	public PersonRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "person";
	}

	public String getPrimaryKeyField() {
		return "id";
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

	public List<String> getFields() {
		List<String> list = new ArrayList<String>();
		list.add("username");
		list.add("passwordHash");
		list.add("firstName");
		list.add("lastName");
		return list;
	}

	public <S extends Person> List<String> getValues(S entity) {
		List<String> list = new ArrayList<String>();
		list.add(entity.getUsername());
		list.add(entity.getPasswordHash());
		list.add(entity.getFirstName());
		list.add(entity.getLastName());
		return list;
	}

	public Person getObjectFromResult(Object[] result) {

		if (result.length < 5) {
			throw new RuntimeException(String.format("Unexpected size of results: %d", result.length));
		}

		Long id = ((Number) result[0]).longValue();
		String username = (String) result[1];
		String passwordhash = (String) result[2];
		String firstName = (String) result[3];
		String lastName = (String) result[4];

		return new Person(id, username, passwordhash, firstName, lastName);
	}
}