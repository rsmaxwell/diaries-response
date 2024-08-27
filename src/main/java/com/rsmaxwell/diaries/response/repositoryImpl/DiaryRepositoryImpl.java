package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;

import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.repository.DiaryRepository;

import jakarta.persistence.EntityManager;

public class DiaryRepositoryImpl extends AbstractCrudRepository<Diary, String> implements DiaryRepository {

	public DiaryRepositoryImpl(EntityManager entityManager) {
		super(entityManager);
	}

	public String getTable() {
		return "diary";
	}

	public String getPrimaryKeyField() {
		return "path";
	}

	public <S extends Diary> String getPrimaryKeyValueAsString(S entity) {
		return entity.getPath();
	}

	public String convertPrimaryKeyValueToString(String id) {
		return id;
	}

	public <S extends Diary> void setPrimaryKeyValue(S entity, Object value) {
		entity.setPath((String) value);
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

		if (result.length < 1) {
			throw new RuntimeException(String.format("Unexpected size of results: %d", result.length));
		}

		String path = (String) result[0];

		return new Diary(path);
	}
}