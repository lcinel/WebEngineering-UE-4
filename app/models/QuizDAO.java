package models;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.Session;

import play.db.jpa.JPA;

/**
 * Provides Data Access methods for JPA
 */
public class QuizDAO implements IQuizDAO {
	public static final QuizDAO INSTANCE = new QuizDAO();

	private QuizDAO() {
	}

	/**
	 * Get a given quiz user based on the id
	 * 
	 * @param id
	 * @return
	 */
	public QuizUser findById(long id) {
		return em().find(QuizUser.class, id);
	}

	public QuizUser findByUserName(String name) {
		if (name != null && !name.isEmpty()) {
			return getByUserName(name);
		} else {
			return null;
		}
	}

	/**
	 * Get a given quiz user based on the name
	 * 
	 * @param name
	 * @return
	 */
	private QuizUser getByUserName(String name) {
		String queryStr = "from QuizUser where userName = :userName";
		TypedQuery<QuizUser> query = em().createQuery(queryStr, QuizUser.class)
				.setParameter("userName", name);
		List<QuizUser> list = query.getResultList();
		if (list.isEmpty()) {
			return null;
		} else {
			return list.get(0);
		}
	}

	/**
	 * Save an entity. Throws an error if an entity with the given id already
	 * exists
	 * 
	 * @param entity
	 * @return
	 */
	@Override
	public void persist(BaseEntity entity) {

		em().persist(entity);
	}

	/**
	 * If no entity with the given id exists in the DB, a new entity is stored.
	 * If there is already an entity with the given id, the entity is updated
	 * 
	 * @param entity
	 * @param <T>
	 * @return
	 */
	@Override
	public <T extends BaseEntity> T merge(T entity) {

		return em().merge(entity);
	}

	/**
	 * Get an entity of the given type using the id
	 * 
	 * @param id
	 * @param entityClazz
	 * @param <T>
	 * @return
	 */
	@Override
	public <T extends BaseEntity> T findEntity(Long id, Class<T> entityClazz) {

		return em().find(entityClazz, id);
	}

	/**
	 * Get a list of all entities of a certain type
	 * 
	 * @param entityClazz
	 * @param <E>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <E extends BaseEntity> List<E> findEntities(Class<E> entityClazz) {

		return ((Session) JPA.em().getDelegate()).createCriteria(entityClazz)
				.list();
	}

	/**
	 * Get the entity manager
	 * 
	 * @return
	 */
	private EntityManager em() {
		return JPA.em();
	}

}
