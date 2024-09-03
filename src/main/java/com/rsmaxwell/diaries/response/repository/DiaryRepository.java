package com.rsmaxwell.diaries.response.repository;

import java.util.Optional;

import com.rsmaxwell.diaries.response.model.Diary;

public interface DiaryRepository extends CrudRepository<Diary, Diary, Long> {

	Optional<Diary> findByName(String path);

}