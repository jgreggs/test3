/**
 * 
 */
package gate.mod.pr.dbconnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gate.mod.pr.source.Country;
import gate.util.Out;

/**
 * @author Josh
 *
 */
public class MySQLDBConnection {

	private Connection connection = null;
	private Statement statement = null;
	private PreparedStatement preparedStatement = null;
	private NamedParameterStatement namedParameterStatement = null;
	private ResultSet resultSet = null;
	
	private final String host = "localhost";
	private final String database = "ciri_project";
	private final String user = "admin";
	private final String password = "password";
	
	public void getConnection() throws Exception{
		
		//Load MySQL driver
        Class.forName("com.mysql.jdbc.Driver");
        System.out.println("Loaded driver");
        
        //Setup connection to database
        connection = DriverManager.getConnection("jdbc:mysql://" + host + "/" + database, user, password);
        System.out.println("Connected to MySQL");

	}
	
	
	public List<Country> readSourceCountryObjects() throws Exception{
		
		String tableName = ".source_ratings";
		
		List<Country> list = readCountryObjectData(tableName);
		
		return list;
	}
	
	/**
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	private List<Country> readCountryObjectData(String tableName)throws Exception{
		
		Country country = null;
		List<Country> list = new ArrayList<Country>();
		
		String query = "SELECT * FROM " + database + tableName;
		
		try{
			  
			getConnection();
			
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
           
            while(rs.next()){
            	
            	country = new Country();
            	
            	country.setAssn(rs.getInt("assn_rating"));
            	country.setCiri(rs.getInt("ciri_rating"));
    			country.setCountry(rs.getString("ctry_rating"));
    			country.setCountryYear(rs.getString("country_year"));
    			country.setCow(rs.getInt("cow_rating"));
    			country.setDisap(rs.getInt("disap_rating"));
    			country.setDommov(rs.getInt("dommov_rating"));
    			country.setFormov(rs.getInt("formov_rating"));
    			country.setInjud(rs.getInt("injud_rating"));
    			country.setKill(rs.getInt("kill_rating"));
    			country.setNew_empinx(rs.getInt("new_empinx_rating"));
    			country.setNew_relfre(rs.getInt("new_relfre_rating"));
    			country.setOld_empinx(rs.getInt("old_empinx_rating"));
    			country.setOld_move(rs.getInt("old_move_rating"));
    			country.setOld_relfre(rs.getInt("old_relfre_rating"));
    			country.setPhysint(rs.getInt("physint_rating"));
    			country.setPolity(rs.getInt("polity_rating"));
    			country.setPolpris(rs.getInt("polpris_rating"));
    			country.setSpeech(rs.getInt("speech_rating"));
    			country.setTort(rs.getInt("tort_rating"));
    			country.setUnctry(rs.getInt("unctry_rating"));
    			country.setUnreg(rs.getInt("unreg_rating"));
    			country.setUnsubreg(rs.getInt("unsubreg_rating"));
    			country.setWecon(rs.getInt("wecon_rating"));
    			country.setWopol(rs.getInt("wopol_rating"));
    			country.setWorker(rs.getInt("worker_rating"));
    			country.setWosoc(rs.getInt("wosoc_rating"));
    			country.setYear(rs.getInt("year_rating"));
    			
            	list.add(country);
                
            }

            close();
            	
		}catch(Exception e){}
		
		return list;
		
	}
	
	public List<Map<String, Object>> readSourceTable() throws Exception{
		
		String tableName = ".source_ratings";
		
		List<Map<String, Object>> list = readData(tableName);
		
		return list;
		
	}
	
	public List<Map<String, Object>> readAutomatedTable() throws Exception{
		
		String tableName = ".automated_ratings";
		
		List<Map<String, Object>> list = readData(tableName);
		
		return list;
		
	}
		
	public void createSourceTable(Map<String, Object> countryMapping, String countryYearId) throws Exception{
		
		String tableName = ".source_ratings";
		
		createCountry(countryMapping, countryYearId, tableName);
				
	}
	
	public void createAutomatedTable(Map<String, Object> countryMapping, String countryYearId) throws Exception{
		
		String tableName = ".automated_ratings";
		
		createCountry(countryMapping, countryYearId, tableName);
		
	}
	
	private List<Map<String, Object>> readData(String tableName)throws Exception{
		
		Map<String, Object> map = null;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		String query = "SELECT * FROM " + database + tableName;
		
		try{
			  
			getConnection();
			
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsmd = rs.getMetaData();
            int numberOfColumns = rsmd.getColumnCount();
           
            while(rs.next()){
            	
            	map = new TreeMap<>();
            	
            	for(int i = 1; i <= numberOfColumns; i++){

            		String name = rsmd.getColumnName(i);
            		
            		if("country_year".equals(name) || "ctry_rating".equals(name)){
            			
            			map.put(name, rs.getString(i));
            			
            		} else{
            			map.put(name, rs.getInt(i));
            		}
            		
            	}
            	
            	list.add(map);
                
            }

            close();
            	
		}catch(Exception e){}
		
		return list;
		
	}
	
	private void createCountry(Map<String, Object> countryMapping, String countryYearId, String tableName) throws Exception{
		
		getConnection();
		
		StringBuilder strBuilder = new StringBuilder();
		String insertSql = "insert into " + database + tableName + " ";
/*		preparedStatement = connection.prepareStatement("insert into " + database + ".source_ratings (country_year, country, YEAR) "
				+ "values(?, ?, ?)");*/
		
		String columnHeaders = "(country_year,";
		String dataValues = " values(:country_year,";
				
		String previousSeparator = "";
		for (Map.Entry<String, Object> entry : countryMapping.entrySet()) {
		    
			String practice = entry.getKey();		    
		    
			columnHeaders += previousSeparator + practice + "_rating";
		    dataValues += previousSeparator + ":" + practice + "_rating";
		    
		    previousSeparator = ",";
		    
		}
		
		columnHeaders += ")";
		dataValues += ")";
		
		
		String query = strBuilder.append(insertSql).append(columnHeaders).append(dataValues).toString();
		Out.prln(query);
		namedParameterStatement = new NamedParameterStatement(connection, query);
		namedParameterStatement.setString("country_year", countryYearId);
		
		for (Map.Entry<String, Object> entry : countryMapping.entrySet()) {
		    String practice = entry.getKey();
		    Object rating = entry.getValue();
		    
		    practice += "_rating";
		    
		    if(dataValues.contains(practice)){
		    	
		    	if(practice.equalsIgnoreCase("ctry_rating"))
		    		namedParameterStatement.setString(practice, rating.toString());
		    	else
		    		namedParameterStatement.setInt(practice, Integer.parseInt(rating.toString()));
		    }
		    
		}
		
		namedParameterStatement.executeUpdate();
				
		close();
		
	}
	
    // You need to close the resultSet
    public void close() {
    	try {
            if (resultSet != null) {
                    resultSet.close();
            }

            if (statement != null) {
                    statement.close();
            }
            
            if (preparedStatement != null) {
            	preparedStatement.close();
            }
            
            if(namedParameterStatement != null){
            	namedParameterStatement.close();
            }

            if (connection != null) {
                    connection.close();
            }
        } catch (Exception e) {

        }
    }
	
}
