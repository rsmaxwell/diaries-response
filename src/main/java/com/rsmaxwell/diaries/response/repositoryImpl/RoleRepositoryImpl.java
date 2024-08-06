package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;

import com.rsmaxwell.diaries.response.model.Role;
import com.rsmaxwell.diaries.response.repository.RoleRepository;

import jakarta.persistence.EntityManager;

public class RoleRepositoryImpl extends AbstractCrudRepository<Role, Long> implements RoleRepository {

	public RoleRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "role";
	}

	public String getPrimaryKeyField() {
		return "id";
	}

	public <S extends Role> Object getPrimaryKeyValueAsString(S entity) {
		return entity.getId();
	}

	public String convertPrimaryKeyValueToString(Long id) {
		return id.toString();
	}

	public <S extends Role> void setPrimaryKeyValue(S entity, Object value) {
		entity.setId((Long) value);
	}

	public List<String> getFields() {
		List<String> list = new ArrayList<String>();
		list.add("name");
		return list;
	}

	public <S extends Role> List<String> getValues(S entity) {
		List<String> list = new ArrayList<String>();
		list.add(entity.getName());
		return list;
	}

	public Role getObjectFromResult(Object[] result) {

		if (result.length < 2) {
			throw new RuntimeException(String.format("Unexpected size of results: %d", result.length));
		}

		Long id = ((Number) result[0]).longValue();
		String name = (String) result[1];

		return new Role(id, name);
	}
}