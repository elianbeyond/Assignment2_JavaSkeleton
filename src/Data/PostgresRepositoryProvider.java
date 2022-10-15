package Data;

import java.sql.*;
import java.util.Vector;

import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;

import Business.Instruction;
import Presentation.IRepositoryProvider;

/**
 * Encapsulates create/read/update/delete operations to PostgreSQL database
 * @author IwanB
 */
public class PostgresRepositoryProvider implements IRepositoryProvider {
	//DB connection parameters - ENTER YOUR LOGIN AND PASSWORD HERE
    private final String userid = "y22s2c9120_bcao7645";
    private final String passwd = "cao520159357";
    private final String myHost = "soit-db-pro-2.ucc.usyd.edu.au";

	private Connection openConnection() throws SQLException
	{
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setServerName(myHost);
		source.setDatabaseName(userid);
		source.setUser(userid);
		source.setPassword(passwd);
		Connection conn = source.getConnection();
	    
	    return conn;
	}

	/**
	 * Validate administrator login request
	 * @param userName: the user's userName trying to login
	 * @param password: the user's password used to login
	 * @return
	 */
	@Override
	public String checkAdmCredentials(String userName, String password)  {
		String sql = "select * from  administrator where password='"+password+"' and login = '"+userName+"'";

		Statement stmt = null;
		try {
			stmt = openConnection().createStatement();
			ResultSet  rs = stmt.executeQuery(sql);

			while(rs.next()){
//
				String firstname = rs.getString("firstname");
				if(firstname!= null){
					System.out.println("Login succeeded，firstname="+firstname);
				}
				return  firstname;
			}


			rs.close();
			stmt.close();
			openConnection().close();
			System.out.println("Login rejected,program close");
			System.exit(0);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}






		return null;

	}

	/**
	 * Find all associated instructions given an administrator
	 * @param userName: the administrator userName
	 * @return
	 */
	@Override
	public Vector<Instruction> findInstructionsByAdm(String userName) {
		Vector <Instruction> vector =new Vector<>();



		String sql = "select DISTINCT amount,frequency,expiryDate,customer,t.firstname,t.lastname,investinstruction.administrator,name,notes " +
				"from  investinstruction,etf, " +
				"(select login,firstname,lastname,investinstruction.administrator " +
				"from customer,investinstruction where login = investinstruction.customer) t " +
				"where investinstruction.administrator = (select login from administrator where firstname = '"+userName+"')  " +
				"and investinstruction.code = etf.code and t.administrator = investinstruction.administrator " +
				"and investinstruction.customer = t.login ORDER BY expiryDate ASC,t.firstname DESC,t.lastname DESC";





		try {
			Statement stmt = openConnection().createStatement();
			ResultSet  rs = stmt.executeQuery(sql);



			while(rs.next()){
				Instruction instruction = new Instruction();
				instruction.setAmount(rs.getString("amount"));
				instruction.setFrequency(rs.getString("frequency"));
				instruction.setExpiryDate(rs.getString("expirydate"));
//				instruction.setCustomer(rs.getString("customer"));
				String fullname = rs.getString("firstname")+" "+rs.getString("lastname");
				instruction.setCustomer(fullname);
				instruction.setAdministrator(rs.getString("administrator"));
				instruction.setNotes(rs.getString("notes"));
				instruction.setEtf(rs.getString("name")) ;
				vector.add(instruction);
			}



		} catch (SQLException e) {
			throw new RuntimeException(e);
		}










		return  vector;
	}

	/**
	 * Find a list of instructions based on the searchString provided as parameter
	 * @param searchString: see assignment description for search specification
	 * @return
	 */
	@Override
	public Vector<Instruction> findInstructionsByCriteria(String searchString) {

		return new Vector<Instruction>();
	}

	/**
	 * Add a new instruction into the Database
	 * @param instruction: the new instruction to add
	 */
	@Override
	public void addInstruction(Instruction instruction) {



	}

	/**
	 * Update the details of a given instruction
	 * @param instruction: the instruction for which to update details
	 */
	@Override
	public void updateInstruction(Instruction instruction) {


	}
}