package com.rsmaxwell.diaries.response.repositoryImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rsmaxwell.diaries.response.repository.CrudRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

public abstract class AbstractCrudRepository<T, ID> implements CrudRepository<T, ID> {

	private static final Logger log = LogManager.getLogger(AbstractCrudRepository.class);

	private EntityManager entityManager;

	public AbstractCrudRepository(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	abstract public String getTable();

	abstract public String getPrimaryKeyField();

	abstract public <S extends T> Object getPrimaryKeyValueAsString(S entity);

	abstract public String convertPrimaryKeyValueToString(ID id);

	abstract public <S extends T> void setPrimaryKeyValue(S entity, Object value);

	abstract public List<String> getFields();

	abstract public <S extends T> List<String> getValues(S entity);

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public long count() {
		Query query = entityManager.createQuery("select count(*)");
		Object object = query.getSingleResult();
		return (long) object;
	}

	public void delete(T entity) {
		String sql = String.format("delete from %s where %s = %s", getTable(), getPrimaryKeyField(), getPrimaryKeyValueAsString(entity));
		Query query = getEntityManager().createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteAll --> count: %d", count));
	}

	public void deleteAll() {
		String sql = String.format("delete from %s", getTable());
		Query query = getEntityManager().createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteAll --> count: %d", count));
	}

	public void deleteAll(Iterable<? extends T> entities) {
		for (T entity : entities) {
			delete(entity);
		}
	}

	public void deleteAllById(Iterable<? extends ID> ids) {
		for (ID id : ids) {
			deleteById(id);
		}
	}

	public void deleteById(ID id) {
		String sql = String.format("delete from %s where %s = %s", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
		Query query = getEntityManager().createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("deleteAll --> count: %d", count));
	}

	public boolean existsById(ID id) {
		String sql = String.format("select exists(select 1 from %s where %s = %s)", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
		Query query = getEntityManager().createNativeQuery(sql);
		int count = query.executeUpdate();
		log.info(String.format("exists --> count: %d", count));
		return (count > 0);
	}

	@SuppressWarnings("unchecked")
	public Iterable<T> findAll() {
		String sql = String.format("select * from %s", getTable());
		Query query = getEntityManager().createNativeQuery(sql);
		List<?> raw = query.getResultList();

		List<T> list = new ArrayList<T>();
		for (Object obj : raw) {
			list.add((T) obj);
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public Iterable<T> findAllById(Iterable<ID> ids) {

		List<T> list = new ArrayList<T>();
		for (ID id : ids) {
			String sql = String.format("select * from %s where %s = %s", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
			Query query = getEntityManager().createNativeQuery(sql);
			List<?> raw = query.getResultList();

			for (Object obj : raw) {
				list.add((T) obj);
			}
		}

		return list;
	}

	@SuppressWarnings("unchecked")
	public Optional<T> findById(ID id) {

		List<T> list = new ArrayList<T>();

		String sql = String.format("select * from %s where %s = %s", getTable(), getPrimaryKeyField(), convertPrimaryKeyValueToString(id));
		Query query = getEntityManager().createNativeQuery(sql);
		List<?> raw = query.getResultList();

		for (Object obj : raw) {
			list.add((T) obj);
		}

		if (list.size() <= 0) {
			return Optional.empty();
		}

		T item = list.get(0);
		return Optional.of(item);
	}

	public <S extends T> S save(S entity) {

		String separator = "";
		StringBuffer fieldsBuffer = new StringBuffer();
		for (String field : getFields()) {
			fieldsBuffer.append(separator);
			fieldsBuffer.append(field);
			separator = ", ";
		}

		separator = "";
		StringBuffer valuesBuffer = new StringBuffer();
		for (String value : getValues(entity)) {
			fieldsBuffer.append(separator);
			valuesBuffer.append("'");
			valuesBuffer.append(value);
			valuesBuffer.append("'");
			separator = ", ";
		}

		String sql = String.format("insert into %s ( %s ) values ( %s ) returning %s", getTable(), fieldsBuffer, valuesBuffer, getPrimaryKeyField());
		Query query = getEntityManager().createNativeQuery(sql);
		Object value = query.getSingleResult();
		log.info(String.format("save --> %s: %s", getPrimaryKeyField(), value.toString()));

		setPrimaryKeyValue(entity, value);

		return entity;
	}

	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {
		List<S> list = new ArrayList<S>();
		for (S entity : entities) {
			list.add(save(entity));
		}
		return list;
	}
}
