package com.rsmaxwell.diaries.response.db.model;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface DiaryRepository extends CrudRepository<Diary, Long> {

	List<Diary> findByPath(String path);

	Diary findById(long id);
}