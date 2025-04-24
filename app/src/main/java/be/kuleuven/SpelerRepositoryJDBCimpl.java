package be.kuleuven;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SpelerRepositoryJDBCimpl implements SpelerRepository {
  private Connection connection;

  SpelerRepositoryJDBCimpl(Connection connection) {
    this.connection = connection;
  }

  @Override
  public void addSpelerToDb(Speler speler) {
    try{ 
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("INSERT INTO speler (tennisvlaanderenid, naam, punten) VALUES (?, ?, ?);");
      prepared.setInt(1, speler.getTennisvlaanderenId());
      prepared.setString(2, speler.getNaam());
      prepared.setInt(3, speler.getPunten());
      prepared.executeUpdate();

      prepared.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Speler getSpelerByTennisvlaanderenId(int tennisvlaanderenId) {
    Speler gevonden_speler = null;
    try {
      Statement s = (Statement) connection.createStatement();
      String stmt = "SELECT * FROM speler WHERE tennisvlaanderenid = '" + tennisvlaanderenId + "'";
      ResultSet result = s.executeQuery(stmt);

      while (result.next()) {
        int tennisvlaanderenIdFromDB = result.getInt("tennisvlaanderenId");
        String naam = result.getString("naam");
        int punten = result.getInt("punten");

        gevonden_speler = new Speler(tennisvlaanderenIdFromDB, naam, punten);
      }
      if (gevonden_speler == null) {
        throw new InvalidSpelerException(tennisvlaanderenId + "");
      }
      result.close();
      s.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return gevonden_speler;
  }

  @Override
  public List<Speler> getAllSpelers() {
    ArrayList<Speler> resultList = new ArrayList<Speler>();
    try {
      Statement s = (Statement) connection.createStatement();
      String stmt = "SELECT * FROM speler";
      ResultSet result = s.executeQuery(stmt);

      while (result.next()) {
        int tennisvlaanderenId = result.getInt("tennisvlaanderenid");
        String naam = result.getString("naam");
        int punten = result.getInt("punten");

        resultList.add(new Speler(tennisvlaanderenId, naam, punten));
      }

      result.close();
      s.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return resultList;
  }

  @Override
  public void updateSpelerInDb(Speler speler) {
    getSpelerByTennisvlaanderenId(speler.getTennisvlaanderenId());
    try {
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("UPDATE speler SET naam = ?, punten = ? WHERE tennisvlaanderenid = ?;");
      prepared.setInt(3, speler.getTennisvlaanderenId());
      prepared.setString(1, speler.getNaam());
      prepared.setInt(2, speler.getPunten());
      prepared.executeUpdate();

      prepared.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void deleteSpelerInDb(int tennisvlaanderenid) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenid);
    try {
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("DELETE FROM speler WHERE tennisvlaanderenid = ?");
      prepared.setInt(1, tennisvlaanderenid);
      prepared.executeUpdate();

      prepared.close();
      connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getHoogsteRankingVanSpeler(int tennisvlaanderenid) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenid);
    String resultString = null;

    try {
        PreparedStatement prepared = (PreparedStatement) connection
          .prepareStatement("SELECT t.clubnaam, w.finale, w.winnaar " +
                            "FROM wedstrijd w " +
                            "JOIN tornooi t ON w.tornooi = t.id " +
                            "WHERE (w.speler1 = ? OR w.speler2 = ?) " +
                            "ORDER BY w.finale ASC " +
                            "LIMIT 1");
        prepared.setInt(1, tennisvlaanderenid);
        prepared.setInt(2, tennisvlaanderenid);
        ResultSet result = prepared.executeQuery();

        if (result.next()) {
          String clubnaam = result.getString("clubnaam");
          int finale = result.getInt("finale");
          Integer winnaar = result.getObject("winnaar") != null ? result.getInt("winnaar") : null;

          String finaleString;
          if (finale == 1 && winnaar != null && winnaar == tennisvlaanderenid) {
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

          resultString = "Hoogst geplaatst in het tornooi van " + clubnaam + " met plaats in de " + finaleString;
        } else {
          resultString = "Geen resultaten van deze speler";
        }

        result.close();
        prepared.close();
        connection.commit();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return resultString;
  }

  @Override
  public void addSpelerToTornooi(int tornooiId, int tennisvlaanderenId) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenId);
    try {
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("INSERT INTO speler_speelt_tornooi (speler, tornooi) VALUES (?, ?)");
        prepared.setInt(1, tennisvlaanderenId);
        prepared.setInt(2, tornooiId);
        prepared.executeUpdate();

        prepared.close();
        connection.commit();
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeSpelerFromTornooi(int tornooiId, int tennisvlaanderenId) {
    getSpelerByTennisvlaanderenId(tennisvlaanderenId);
    try {
      PreparedStatement prepared = (PreparedStatement) connection
        .prepareStatement("DELETE FROM speler_speelt_tornooi WHERE speler = ? AND tornooi = ?");
        prepared.setInt(1, tennisvlaanderenId);
        prepared.setInt(2, tornooiId);
        prepared.executeUpdate();

        prepared.close();
        connection.commit();
    } catch (Exception e){
      throw new RuntimeException(e);
    }
  }

}
