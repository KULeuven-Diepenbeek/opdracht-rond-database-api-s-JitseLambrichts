package be.kuleuven;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class SpelerRepositoryJPAimpl implements SpelerRepository {
  private final EntityManager em;
  public static final String PERSISTANCE_UNIT_NAME = "be.kuleuven.spelerhibernateTest";

  // Constructor
  SpelerRepositoryJPAimpl(EntityManager entityManager) {
    this.em = entityManager;
  }

  @Override
  public void addSpelerToDb(Speler speler) {
    em.getTransaction().begin();
    if (em.find(Speler.class, speler.getTennisvlaanderenId()) != null) {
      em.getTransaction().rollback();
      throw new RuntimeException(" A PRIMARY KEY constraint failed");
    }
    em.persist(speler);
    em.getTransaction().commit();
  }

  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
    Speler speler = em.find(Speler.class, tennisvlaanderenId);
    if (speler == null) {
      throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));
    }
    return speler;
  }

  @Override
  public List<Speler> getAllSpelers() {
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Speler> cq = cb.createQuery(Speler.class);
    Root<Speler> root = cq.from(Speler.class);
    cq.select(root);
    return em.createQuery(cq).getResultList();
  }

  @Override
  public void updateSpelerInDb(Speler speler) {
    em.getTransaction().begin();
    if (em.find(Speler.class, speler.getTennisvlaanderenId()) == null) {
      throw new InvalidSpelerException(String.valueOf(speler.getTennisvlaanderenId()));
    }
    em.merge(speler);
    em.getTransaction().commit();
  }

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenId) {
    em.getTransaction().begin();
    Speler speler = em.find(Speler.class, tennisvlaanderenId);
    if (speler == null) {
      throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));
    }
    em.remove(speler);
    em.getTransaction().commit();
  }

  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenId) {
    Speler speler = em.find(Speler.class, tennisvlaanderenId);
    if (speler == null) {
      throw new InvalidSpelerException(String.valueOf(tennisvlaanderenId));
    }
    List<Object[]> result = em.createQuery(
          "SELECT t.clubnaam, w.finale, w.winnaarId " +
          "FROM Wedstrijd w, Tornooi t  " +
          "WHERE w.tornooiId = t.id " +
          "AND (w.speler1Id = :id OR w.speler2Id = :id) " +
          "ORDER BY w.finale ASC ", Object[].class)
      .setParameter("id", tennisvlaanderenId)
      .setMaxResults(1)
      .getResultList();

    if (result.isEmpty()) {
      return "Geen resultaten van deze speler";
    }

    String clubnaam = (String) result.get(0)[0];
    int finale = (int) result.get(0)[1];
    Integer winnaar = null;
    if (result.get(0)[2] != null) {
      winnaar = (Integer) result.get(0)[2];
    }

    String finaleString;
    if (finale == 1 && winnaar != null && winnaar == tennisvlaanderenId) {
        finaleString = "winst";
    } else if (finale == 1) {
        finaleString = "finale";
    } else if (finale == 2) {
        finaleString = "halve-finale";
    } else if (finale == 4) {
        finaleString = "kwart-finale";
    } else {
        finaleString = "lager dan de kwart-finales";
    }

    return "Hoogst geplaatst in het tornooi van " + clubnaam + " met plaats in de " + finaleString;
  } 

  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
    EntityTransaction tx = em.getTransaction();
    try {
      tx.begin();

      Speler speler = em.find(Speler.class, tennisvlaanderenId);
      Tornooi tornooi = em.find(Tornooi.class, tornooiId);

      speler.getTornooien().add(tornooi);
      em.merge(speler);

      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    EntityTransaction tx = em.getTransaction();

    try {
      tx.begin();

      Speler speler = em.find(Speler.class, tennisvlaanderenId);
      Tornooi tornooi = em.find(Tornooi.class, tornooiId);

      if (speler == null || tornooi == null) {
        throw new IllegalArgumentException("Speler or Tornooi not found");
      }

      speler.getTornooien().remove(tornooi);
      em.merge(speler);

      tx.commit();
    } catch (Exception e) {
      if (tx.isActive())
        tx.rollback();
      throw e;
    } finally {
      em.close();
    }
  }
}