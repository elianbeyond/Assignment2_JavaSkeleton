package Data;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Vector;

import org.postgresql.ds.PGSimpleDataSource;
import Business.Instruction;
import Presentation.IRepositoryProvider;




/**
 * Encapsulates create/read/update/delete operations to PostgreSQL database
 * @author IwanB
 */
public class PostgresRepositoryProvider implements IRepositoryProvider {
	//DB connection parameters - ENTER YOUR LOGIN AND PASSWORD HERE
//	private final String userid = "y22s2c9120_bcao7645";
//	private final String passwd = "cao520159357";
//	private final String myHost = "soit-db-pro-2.ucc.usyd.edu.au";
//
//	private Connection openConnection() throws SQLException
//	{
//		PGSimpleDataSource source = new PGSimpleDataSource();
//		source.setServerName(myHost);
//		source.setDatabaseName(userid);
//		source.setUser(userid);
//		source.setPassword(passwd);
//		Connection conn = source.getConnection();
//
//		return conn;
//	}

	static private  String userid = "postgres";
	static private  String passwd = "cao520159357";
	static private  String myHost = "localhost";

	public static String globalAdmName;

	private Connection openConnection() throws SQLException
	{
		PGSimpleDataSource source = new PGSimpleDataSource();
		source.setServerName(myHost);
		source.setDatabaseName("ass2");
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
					globalAdmName = userName;
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
			System.out.println("function (findInstructionsByAdm) executed successfully.");

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
		Vector <Instruction> vector =new Vector<>();
		String sql = "select  instructionid,amount,frequency,expirydate,customer,administrator,t.code,t.fullname,t.name,investinstruction.notes\n" +
				"from investinstruction,\n" +
				"   (select    concat_ws(' ',firstname,lastname) fullname, login , etf.code, etf.name , investinstruction.notes\n" +
				"    from investinstruction,customer,etf\n" +
				"    where investinstruction.customer = customer.login and etf.code = investinstruction.code )  t\n" +
				"                where investinstruction.customer =  t.login\n" +
				"                      and investinstruction.code = t.code\n" +
				"                      and (t.fullname ilike ? or t.login ilike ? or t.name ilike ? or t.notes ilike ?);\n";

		try {
			PreparedStatement pstmt = openConnection().prepareStatement(sql);
			pstmt.setString(1, "%"+searchString+"%");
			pstmt.setString(2, "%"+searchString+"%");
			pstmt.setString(3, "%"+searchString+"%");
			pstmt.setString(4, "%"+searchString+"%");

			ResultSet rs  = pstmt.executeQuery();

			while(rs.next()){
				Instruction instruction = new Instruction();
				instruction.setAmount(rs.getString("amount"));
				instruction.setFrequency(rs.getString("frequency"));
				instruction.setExpiryDate(rs.getString("expirydate"));
				instruction.setCustomer(rs.getString("fullname"));
				instruction.setEtf(rs.getString("name"));
				instruction.setAdministrator(rs.getString("administrator"));
				instruction.setNotes(rs.getString("notes"));
				vector.add(instruction);
			}
			System.out.println("function (findInstructionsByCriteria) executed successfully.");


			rs.close();
			pstmt.close();

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}

		return vector;
	}



	/**
	 * Add a new instruction into the Database
	 * @param instruction: the new instruction to add
	 */
	@Override
	public void addInstruction(Instruction instruction) {

        //data prepare
		String amount = instruction.getAmount();
		float amount2num= Float.parseFloat(amount);
		String frequency = instruction.getFrequency();


		Date date = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.MONTH, 12);
		//Get the current time and add 12 months
		Date expirydate = new Date(c.getTimeInMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy");





		String fullName = instruction.getCustomer();
		String administrator = instruction.getAdministrator();
		String etfName = instruction.getEtf();
		String notes = instruction.getNotes();

        //find the insert index
		String sql1 = "select count(*) from investinstruction";

		//full name ->customer.login -> investinstruction.customer
		String sql2 = "select login from\n" +
				"(select distinct  login,fullname from (select concat_ws(' ',firstname,lastname) fullname, login from customer) t,investinstruction where t.login = investinstruction.customer) t2\n" +
				"where fullname = ?";

		//etf.name ->etf.code
		String sql3 = "select code from etf where name = ?";

		//insert data
		String sql4 = "INSERT INTO investinstruction (instructionid, amount, frequency, expirydate, customer,administrator,code,notes) \n" +
				"VALUES (?,?,?,?,?,?,?,?)";

		try {
			//get investinstruction.investinstructionid
			PreparedStatement pstmt1 = openConnection().prepareStatement(sql1);

			ResultSet rs1  = pstmt1.executeQuery();
			rs1.next();
			int count = rs1.getInt(1);
			int instructionid = count+1;


            //find investinstruction.customer
			PreparedStatement pstmt2 = openConnection().prepareStatement(sql2);
			pstmt2.setString(1, fullName);
			ResultSet rs2  = pstmt2.executeQuery();
			rs2.next();
			String customer = rs2.getString("login");


            //find investinstruction.code
			PreparedStatement pstmt3 = openConnection().prepareStatement(sql3);
			pstmt3.setString(1, etfName);
			ResultSet rs3  = pstmt3.executeQuery();
			rs3.next();
			String code = rs3.getString("code");




            //insert a row of data into table investinstruction
			PreparedStatement pstmt4 = openConnection().prepareStatement(sql4);
			pstmt4.setInt(1,instructionid );
			pstmt4.setFloat(2, amount2num);
			pstmt4.setString(3, frequency);
			pstmt4.setDate(4, expirydate);
			pstmt4.setString(5, customer);
			pstmt4.setString(6, globalAdmName);
			pstmt4.setString(7, code );
			pstmt4.setString(8, notes );
			pstmt4.executeUpdate();


			System.out.println("add successfully, instructionid of new data is :" +instructionid);

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}


	}

	/**
	 * Update the details of a given instruction
	 * @param instruction: the instruction for which to update details
	 */
	@Override
	public void updateInstruction(Instruction instruction) {

//		//data prepare
//		int instructionid = instruction.getInstructionId();
//		String amount = instruction.getAmount();
//		float amount2num= Float.parseFloat(amount);
//		String frequency = instruction.getFrequency();
//
//
//		Date date = new Date(System.currentTimeMillis());
//		Calendar c = Calendar.getInstance();
//		c.setTime(date);
//		c.add(Calendar.MONTH, 12);
//		//Get the current time and add 12 months
//		Date expirydate = new Date(c.getTimeInMillis());
//		SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy");
//
//
//
//
//
//		String fullName = instruction.getCustomer();
//		String administrator = instruction.getAdministrator();
//		String etfName = instruction.getEtf();
//		String notes = instruction.getNotes();
//
//
//
//		//full name ->customer.login -> investinstruction.customer
//		String sql2 = "select login from\n" +
//				"(select distinct  login,fullname from (select concat_ws(' ',firstname,lastname) fullname, login from customer) t,investinstruction where t.login = investinstruction.customer) t2\n" +
//				"where fullname = ?";
//
//		//etf.name ->etf.code
//		String sql3 = "select code from etf where name = ?";
//
//		//insert data
//		String sql4 = "UPDATE investinstruction\n" +
//				"SET amount = ?, frequency = ?,expirydate=?,customer=?,administrator=?,code=?,notes=?\n" +
//				"WHERE instructionid = ?";
//
//		try {
//
//
//
//			//find investinstruction.customer
//			PreparedStatement pstmt2 = openConnection().prepareStatement(sql2);
//			pstmt2.setString(1, fullName);
//			ResultSet rs2  = pstmt2.executeQuery();
//			rs2.next();
//			String customer = rs2.getString("login");
//
//
//			//find investinstruction.code
//			PreparedStatement pstmt3 = openConnection().prepareStatement(sql3);
//			pstmt3.setString(1, etfName);
//			ResultSet rs3  = pstmt3.executeQuery();
//			rs3.next();
//			String code = rs3.getString("code");
//
//
//
//
//			//insert a row of data into table investinstruction
//			PreparedStatement pstmt4 = openConnection().prepareStatement(sql4);
//			pstmt4.setFloat(1, amount2num);
//			pstmt4.setString(2, frequency);
//			pstmt4.setDate(3, expirydate);
//			pstmt4.setString(4, customer);
//			pstmt4.setString(5, globalAdmName);
//			pstmt4.setString(6, code );
//			pstmt4.setString(7, notes );
//			pstmt4.setInt(8, instructionid );
//			pstmt4.executeUpdate();
//
//
//			System.out.println("update successfully, instructionid of changed data is :" +instructionid);
//
//		} catch (SQLException e) {
//			throw new RuntimeException(e);
//		}
//
//
//
//
//
//
	}
}