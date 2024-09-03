package com.rsmaxwell.diaries.response.repository;

import java.util.Optional;

import com.rsmaxwell.diaries.response.dto.PageDTO;
import com.rsmaxwell.diaries.response.model.Diary;
import com.rsmaxwell.diaries.response.model.Page;

public interface PageRepository extends CrudRepository<Page, PageDTO, Long> {

	Iterable<PageDTO> findAllByDiaryId(Long diaryId);

	Iterable<PageDTO> findAllByDiary(Diary diary);

	Optional<PageDTO> findByDiaryAndName(Diary diary, String name);
}