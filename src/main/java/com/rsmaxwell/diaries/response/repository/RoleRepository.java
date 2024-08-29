package com.rsmaxwell.diaries.response.repository;

import java.util.Optional;

import com.rsmaxwell.diaries.response.model.Role;

public interface RoleRepository extends CrudRepository<Role, Long> {

	Optional<Role> findByName(String name);

}