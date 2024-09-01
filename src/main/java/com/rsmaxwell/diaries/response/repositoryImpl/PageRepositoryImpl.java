package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Page;
import com.rsmaxwell.diaries.response.repository.PageRepository;

import jakarta.persistence.EntityManager;

public class PageRepositoryImpl extends AbstractCrudRepository<Page, Long> implements PageRepository {

	public PageRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "diary";
	}

	public String getPrimaryKeyField() {
		return "id";
	}

	public <S extends Page> String getPrimaryKeyValueAsString(S entity) {
		return entity.getId().toString();
	}

	public String convertPrimaryKeyValueToString(Long id) {
		return id.toString();
	}

	public <S extends Page> void setPrimaryKeyValue(S entity, Object value) {
		entity.setId((Long) value);
	}

	public List<String> getFields() {
		List<String> list = new ArrayList<String>();
		list.add("path");
		return list;
	}

	public <S extends Page> List<String> getValues(S entity) {
		List<String> list = new ArrayList<String>();
		list.add(entity.getName());
		return list;
	}

	public Page getObjectFromResult(Object[] result) {

		if (result.length < 2) {
			throw new RuntimeException(String.format("Unexpected size of results: %d", result.length));
		}

		Long id = ((Number) result[0]).longValue();
		Diary diary = (Diary) result[1];
		String name = (String) result[2];

		return new Page(id, diary, name);
	}

	public Optional<Page> findByPath(String path) {
		return findByField("path", path);
	}
}