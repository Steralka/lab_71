package labs.lab7.server.managers;

import labs.lab7.common.exceptions.AuthorizationException;
import labs.lab7.common.models.Coordinates;
import labs.lab7.common.models.Discipline;
import labs.lab7.common.models.LabWork;
import labs.lab7.server.dao.LabWorkDao;
import labs.lab7.server.dao.UserDao;
import org.hibernate.InstantiationException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public class DatabaseManager {

    private static final String HIBERNATE_CONFIG = "hibernate.cfg.xml";
    private static final String HASHING_ALGORITHM = "SHA-256";

    private final SessionFactory sessionFactory;

    private MessageDigest md;

    private static DatabaseManager instance;

    private DatabaseManager() throws IllegalStateException {
        sessionFactory = createSessionFactory();
        try {
            md = MessageDigest.getInstance(HASHING_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Отсутствует алгоритм: " + HASHING_ALGORITHM);
        }
    }

    public static DatabaseManager getInstance() throws IllegalStateException {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public long signUpUser(String username, String password) {
        var user = new UserDao();
        user.setUsername(username);
        user.setPassword(generateHash(password));
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.persist(user);
        session.getTransaction().commit();
        session.close();
        return user.getId();
    }

    public long signInUser(String username, String password) throws SQLException, AuthorizationException {
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        var query = session.createQuery("SELECT u FROM users u WHERE u.username = :username", UserDao.class);
        query.setParameter("username", username);
        var result = query.list();

        if (result.isEmpty()) throw new AuthorizationException("Отсутствует пользователь с таким именем");

        var user = result.get(0);
        session.getTransaction().commit();
        session.close();

        if (!user.getPassword().equals(generateHash(password))) throw new AuthorizationException("Неверный пароль");

        return user.getId();
    }

    public long add(LabWork labWork, long userId) {
        var dao = new LabWorkDao();
        dao.setName(labWork.getName());
        dao.setCoordX(labWork.getCoordinates().getX());
        dao.setCoordY(labWork.getCoordinates().getY());
        dao.setCreationDate(labWork.getCreationDate());
        dao.setMinimalPoint(labWork.getMinimalPoint());
        dao.setDifficulty(labWork.getDifficulty());
        if (Objects.nonNull(labWork.getDiscipline())) {
            dao.setDisciplineName(labWork.getDiscipline().getName());
            dao.setDisciplineLectureHours(labWork.getDiscipline().getLectureHours());
            dao.setDisciplinePracticeHours(labWork.getDiscipline().getPracticeHours());
        }
        dao.setAuthorId(userId);
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        session.persist(dao);
        session.getTransaction().commit();
        session.close();
        return labWork.getId();
    }

    public void update(LabWork labWork, long userId) throws AuthorizationException {
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        var dao = session.get(LabWorkDao.class, labWork.getId());
        if (dao.getAuthorId() != userId) throw new AuthorizationException("Нет прав на изменение");

        dao.setName(labWork.getName());
        dao.setCoordX(labWork.getCoordinates().getX());
        dao.setCoordY(labWork.getCoordinates().getY());
        dao.setCreationDate(labWork.getCreationDate());
        dao.setMinimalPoint(labWork.getMinimalPoint());
        dao.setDifficulty(labWork.getDifficulty());
        if (Objects.nonNull(labWork.getDiscipline())) {
            dao.setDisciplineName(labWork.getDiscipline().getName());
            dao.setDisciplineLectureHours(labWork.getDiscipline().getLectureHours());
            dao.setDisciplinePracticeHours(labWork.getDiscipline().getPracticeHours());
        } else {
            dao.setDisciplineName(null);
            dao.setDisciplineLectureHours(null);
            dao.setDisciplinePracticeHours(null);
        }
        session.merge(dao);
        session.getTransaction().commit();
        session.close();
    }

    public void clear(long userId) {
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        var query = session.createQuery("DELETE FROM lab_works WHERE author_id = :author_id", LabWorkDao.class);
        query.setParameter("author_id", userId);
        query.executeUpdate();
        session.getTransaction().commit();
        session.close();
    }

    public int remove(long labWorkId, long userId) {
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();

        var query = session.createQuery("DELETE FROM lab_works WHERE author_id = :author_id AND id = :id", LabWorkDao.class);
        query.setParameter("author_id", userId);
        query.setParameter("id", labWorkId);

        var deletedSize = query.executeUpdate();
        session.getTransaction().commit();
        session.close();
        return deletedSize;
    }

    public List<LabWork> loadLabWorks() {
        var session = sessionFactory.getCurrentSession();
        session.beginTransaction();
        var cq = session.getCriteriaBuilder().createQuery(LabWorkDao.class);
        var rootEntry = cq.from(LabWorkDao.class);
        var all = cq.select(rootEntry);

        var result = session.createQuery(all).getResultList();
        session.getTransaction().commit();
        session.close();
        return result.stream().map(dao -> new LabWork(
                dao.getId(),
                dao.getName(),
                new Coordinates(dao.getCoordX(), dao.getCoordY()),
                dao.getMinimalPoint(),
                dao.getDifficulty(),
                Objects.nonNull(dao.getDisciplineName()) ? new Discipline(
                        dao.getDisciplineName(),
                        dao.getDisciplineLectureHours(),
                        dao.getDisciplinePracticeHours()
                ) : null
        )).toList();
    }

    private SessionFactory createSessionFactory() {
        var registry = new StandardServiceRegistryBuilder().configure(HIBERNATE_CONFIG).build();
        var metadata = new MetadataSources(registry).getMetadataBuilder().build();
        return metadata.getSessionFactoryBuilder().build();
    }

    private String generateHash(String text) {
        byte[] bytes = text.getBytes();
        md.update(bytes);
        byte[] hashBytes = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public void close() {
        sessionFactory.close();
    }
}
