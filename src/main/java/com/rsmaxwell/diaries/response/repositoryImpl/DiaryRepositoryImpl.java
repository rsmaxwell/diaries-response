package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;

import jakarta.persistence.EntityManager;

public class DiaryRepositoryImpl extends AbstractCrudRepository<Diary, Long> implements DiaryRepository {

	public DiaryRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "diary";
	}

	public String getPrimaryKeyField() {
		return "id";
	}

	public <S extends Diary> String getPrimaryKeyValueAsString(S entity) {
		return entity.getId().toString();
	}

	public String convertPrimaryKeyValueToString(Long id) {
		return id.toString();
	}

	public <S extends Diary> void setPrimaryKeyValue(S entity, Object value) {
		entity.setId((Long) value);
	}

	public List<String> getFields() {
		List<String> list = new ArrayList<String>();
		list.add("path");
		return list;
	}

	public <S extends Diary> List<String> getValues(S entity) {
		List<String> list = new ArrayList<String>();
		list.add(entity.getPath());
		return list;
	}

	public Diary getObjectFromResult(Object[] result) {

		if (result.length < 2) {
			throw new RuntimeException(String.format("Unexpected size of results: %d", result.length));
		}

		Long id = ((Number) result[0]).longValue();
		String path = (String) result[1];

		return new Diary(id, path);
	}

	public Optional<Diary> findByPath(String path) {
		return findByField("path", path);
	}
}