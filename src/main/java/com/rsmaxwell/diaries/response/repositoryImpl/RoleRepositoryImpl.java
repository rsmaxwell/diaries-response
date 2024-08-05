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
}