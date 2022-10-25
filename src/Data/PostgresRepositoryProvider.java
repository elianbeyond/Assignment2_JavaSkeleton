package Data;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;
import java.util.regex.Pattern;

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
//	public static String globalAdmName;
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



		String sql = "select *,(case when t.expirydate <= CURRENT_DATE then 1 else 0 end)as mark\n" +
				"from (select * from customer join investinstruction ON (investinstruction.customer = customer.login) join Etf\n" +
				"      ON (etf.code = investinstruction.code) where investinstruction.administrator = '"+globalAdmName+"') t\n" +
				"order by mark ASC, t.expirydate ASC, t.firstname DESC, t.lastname DESC;";





		try {
			Statement stmt = openConnection().createStatement();
			ResultSet  rs = stmt.executeQuery(sql);




			while(rs.next()){
				Instruction instruction = new Instruction();
				instruction.setInstructionId(rs.getInt("instructionid"));
				instruction.setAmount(rs.getString("amount"));

				String Freq = rs.getString("frequency");
				if("MTH".equals(Freq)){
					Freq = "Monthly";
				}
				if("FTH".equals(Freq)){
					Freq = "Fortnightly";
				}

				instruction.setFrequency(Freq);

				String expirydate = rs.getString("expirydate");

				//The date format in the database is YMD, which is first parsed to Data and then converted to DMY format
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				SimpleDateFormat newsdf = new SimpleDateFormat("dd-MM-yyyy");
				java.util.Date d = null;
				try {
					d = sdf.parse(expirydate);
				} catch (Exception e) {
					e.printStackTrace();
				}
				java.sql.Date date = new java.sql.Date(d.getTime());
				String newDate = newsdf.format(date);

				instruction.setExpiryDate(newDate);





//				instruction.setCustomer(rs.getString("customer"));
				String fullname = rs.getString("firstname")+" "+rs.getString("lastname");
				instruction.setCustomer(fullname);
				instruction.setAdministrator(rs.getString("administrator"));
				instruction.setNotes(rs.getString("notes"));
				instruction.setEtf(rs.getString("name")) ;


				vector.add(instruction);
			}
			System.out.println("function (findInstructionsByAdm) executed successfully.");
			rs.close();
			stmt.close();
			openConnection().close();

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
		String sql = "select  distinct instructionid,amount,frequency,expirydate,customer,administrator,t.code,t.fullname,t.name,investinstruction.notes\n" +
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

				String Freq = rs.getString("frequency");
				if("MTH".equals(Freq)){
					Freq = "Monthly";
				}
				if("FTH".equals(Freq)){
					Freq = "Fortnightly";
				}
				instruction.setFrequency(Freq);
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
		//check whether amount is a number
		if(!PostgresRepositoryProvider.isNumeric(amount)){
			System.out.println("Please enter a number in amount!");
			return;
		}
		float amount2num= Float.parseFloat(amount);
		String frequency = instruction.getFrequency();
		if(!("Monthly".equals(frequency)||"Fortnightly".equals(frequency))){
			System.out.println("Please enter correct frequency:Monthly/Fortnightly");
			return;
		}
		if("Monthly".equals(frequency)){
			frequency = "MTH";
		}
		if("Fortnightly".equals(frequency)){
			frequency = "FTH";
		}


		Date date = new Date(System.currentTimeMillis());
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.MONTH, 12);
		//Get the current time and add 12 months
		Date expirydate = new Date(c.getTimeInMillis());
		SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyy");




		String login = instruction.getCustomer();
		String administrator = instruction.getAdministrator();
		String etfCode = instruction.getEtf();
		String notes = instruction.getNotes();

        //find the insert index
		String sql1 = "select count(*) from investinstruction";




		try {
			//get investinstruction.investinstructionid
			PreparedStatement pstmt1 = openConnection().prepareStatement(sql1);

			ResultSet rs1  = pstmt1.executeQuery();
			rs1.next();
			int count = rs1.getInt(1);
			int instructionid = count+1;
			rs1.close();
			pstmt1.close();

			String sql2 = "select check_para(?,?);";
			CallableStatement cstmt = openConnection().prepareCall(sql2);
			cstmt.setString(1, login);
			cstmt.setString(2, etfCode);

			ResultSet rs = cstmt.executeQuery();
			rs.next();
			String tem = rs.getString(1);
			System.out.println(tem);

			if(!"2".equals(tem)){
				System.out.println("Please enter the correct customerLogin or etfCode");
				return;
			}



			cstmt.close();

			String sql3 = "select add_instruction(?,?,?,?,?,?,?,?);";
			CallableStatement cstmt2 = openConnection().prepareCall(sql3);

			//strs[0]= customer ,strs[1] = code
			cstmt2.setInt(1, instructionid);
			cstmt2.setFloat(2, amount2num);
			cstmt2.setString(3, frequency);
			cstmt2.setDate(4, expirydate);
			cstmt2.setString(5, login);
			cstmt2.setString(6, etfCode);
			cstmt2.setString(7, globalAdmName);
			cstmt2.setString(8, notes);
			cstmt2.execute();
			cstmt2.close();

			openConnection().close();
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
		//data prepare
		int instructionid = instruction.getInstructionId();
		String amount = instruction.getAmount();

		//check whether amount is num
		if (!PostgresRepositoryProvider.isNumeric(amount)) {
			System.out.println("Please enter a number in amount!");
			return;
		}

		float amount2num = Float.parseFloat(amount);
		String frequency = instruction.getFrequency();

		if(!("Monthly".equals(frequency)||"Fortnightly".equals(frequency))){
			System.out.println("Please enter correct frequency:Monthly/Fortnightly");
			return;
		}
		if("Monthly".equals(frequency)){
			frequency = "MTH";
		}
		if("Fortnightly".equals(frequency)){
			frequency = "FTH";
		}

		String expirydate = instruction.getExpiryDate();

		//check date format

		//parse String expirydate to java.sql.Date date
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		java.util.Date d = null;
		try {
			//Add strong judgment condition, otherwise 2022-02-29 will be wrongly judged as correct format
			sdf.setLenient(false);
			d = sdf.parse(expirydate);
		} catch (Exception e) {
			System.out.println("Please enter correct date format in dd-mm-yyyy");
			return;
		}
		java.sql.Date date = new java.sql.Date(d.getTime());


		String login = instruction.getCustomer();
		String administrator = instruction.getAdministrator();
		String etfCode = instruction.getEtf();
		String notes = instruction.getNotes();


		String sql1 = "select check_para(?,?);";
		try {
			CallableStatement cstmt = openConnection().prepareCall(sql1);


			cstmt.setString(1, login);
			cstmt.setString(2, etfCode);


			ResultSet rs = cstmt.executeQuery();
			rs.next();
			String tem = rs.getString(1);

			if(!"2".equals(tem)){
				System.out.println("Please enter the correct customerLogin or etfCode");
				return;
			}


			cstmt.close();

			String sql2 = "select update_instruction(?,?,?,?,?,?,?,?);";
			CallableStatement cstmt2 = openConnection().prepareCall(sql2);
			cstmt2.setString(1, login);
			cstmt2.setString(2, etfCode);
			cstmt2.setFloat(3,amount2num);
			cstmt2.setString(4,frequency);
			cstmt2.setDate(5,date);
			cstmt2.setString(6,globalAdmName);
			cstmt2.setString(7,notes);
			cstmt2.setInt(8,instructionid);
			cstmt2.execute();
			System.out.println("Update successfully");



			openConnection().close();

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}


	}

	//Use regular expression determines whether the data of String type represents Numeric
	public static boolean isNumeric(String str) {
		if (null == str || "".equals(str)) {
			return false;
		}
		String regx = "[+-]*\\d+\\.?\\d*[Ee]*[+-]*\\d+";
		Pattern pattern = Pattern.compile(regx);
		boolean isNumber = pattern.matcher(str).matches();
		if (isNumber) {
			return isNumber;
		}
		regx = "^[-\\+]?[.\\d]*$";
		pattern = Pattern.compile(regx);
		return pattern.matcher(str).matches();
	}




}