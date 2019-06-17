package infrastructure;

import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import readingXML.ReadXML;

/**
 * This example shows how to login/logout/authenticate to the server with REST.
 * note that this is a rather "thin" layer over {@link RestConnector} because
 * these operations are *almost* HTML standards.
 */
public class QC_Operations_POC {

	ReadConfigFile readConfigFile = new ReadConfigFile();
	ReadXML readXML = new ReadXML();

	public String LWSSO_Key;
	public static String QCSESSION_Key;
	public String test_Plan_Parent_Folder_Name;
	public String test_Plan_Child_Folder_Name;
	public String test_Plan_hierarchical_Path;
	public List<String> list_Of_Test_Plan_IDs;
	public List<String> list_Of_Test_Lab_IDs;
	public List<String> test_config_id;
	public String test_Lab_Parent_Folder_Name;
	public String parent_Folder_Id;
	public String test_Lab_hierarchical_Path;
	public String test_Lab_Child_Folder_Name;
	public String test_Lab_Test_Set_Name;
	public String child_Folder_Id;
	public String test_Set_Id;
	public String test_Instance_Subtype_Id;
	public String test_Id;
	public ArrayList<String> testcycl_Id = new ArrayList<>();
	public String owner;

	public static void main(String[] args) throws Exception {
		System.out.println("main enters");// New line
		new QC_Operations_POC().allPocOperations(
				"http://" + Constants.HOST + ":" + Constants.PORT + "/qcbin", Constants.DOMAIN, Constants.PROJECT,
				Constants.USERNAME, Constants.PASSWORD);
	}

	public void allPocOperations(final String serverUrl, final String domain, final String project,
			String username, String password) throws Exception {

		RestConnector con = RestConnector.getInstance().init(new HashMap<String, String>(), serverUrl, domain, project);
		System.out.println(con.domain + "," + con.project + "," + con.serverUrl + "," + con.getCookies());

		QC_Operations_POC example = new QC_Operations_POC();

		// if we're authenticated we'll get a null, otherwise a URL where we should
		// login at (we're not logged in, so we'll get a URL).
		String authenticationPoint = example.isAuthenticated();

		Assert.assertTrue("response from isAuthenticated means we're authenticated. that can't be.",
				authenticationPoint != null);

		// now we login to previously returned URL.
		boolean loginResponse = example.login(authenticationPoint, username, password);
		Assert.assertTrue("failed to login.", loginResponse);
		Assert.assertTrue("login did not cause creation of Light Weight Single Sign On(LWSSO) cookie.",
				con.getCookieString().contains("LWSSO_COOKIE_KEY"));

		// proof that we are indeed logged in
		Assert.assertNull("isAuthenticated returned not authenticated after login.", example.isAuthenticated());

		// establishing the session
		example.startSession();
		// getting Test cases and Test folder details from Test Plan
		example.getValues();

		// example.get_folder_id();

		example.creation_Of_Test_Set_Folder();
		example.creation_Of_Test_Set();
		example.creation_Of_Test_Instance();
		example.creation_Of_runs();
		// and now we logout
		// example.logout();

		// And now we can see that we are indeed logged out
		// because isAuthenticated once again returns a url, and not null.
		Assert.assertNotNull("isAuthenticated returned authenticated after logout.", example.isAuthenticated());
	}

	private RestConnector con;

	public QC_Operations_POC() {
		con = RestConnector.getInstance();
		System.out.println(con.domain + "," + con.project + "," + con.serverUrl + "," + con.cookies);
	}

	/**
	 * @param username
	 * @param password
	 * @return true if authenticated at the end of this method.
	 * @throws Exception
	 *
	 *             convenience method used by other examples to do their login
	 */
	public boolean login(String username, String password) throws Exception {

		String authenticationPoint = this.isAuthenticated();
		if (authenticationPoint != null) {
			return this.login(authenticationPoint, username, password);
		}
		return true;
	}

	/**
	 * @param loginUrl
	 *            to authenticate at
	 * @param username
	 * @param password
	 * @return true on operation success, false otherwise
	 * @throws Exception
	 *
	 *             Logging in to our system is standard http login (basic
	 *             authentication), where one must store the returned cookies for
	 *             further use.
	 */
	public boolean login(String loginUrl, String username, String password) throws Exception {

		// create a string that lookes like:
		// "Basic ((username:password)<as bytes>)<64encoded>"
		byte[] credBytes = (username + ":" + password).getBytes();
		String credEncodedString = "Basic " + Base64Encoder.encode(credBytes);

		Map<String, String> map = new HashMap<String, String>();
		map.put("Authorization", credEncodedString);

		Response response = con.httpGet(loginUrl, null, map);

		System.out.println(response.getStatusCode());

		boolean ret = response.getStatusCode() == HttpURLConnection.HTTP_OK;

		Iterable<String> key = response.getResponseHeaders().get("Set-Cookie");

		LWSSO_Key = key.iterator().next();

		return ret;

	}

	/**
	 * @return true if logout successful
	 * @throws Exception
	 *             close session on server and clean session cookies on client
	 */
	public boolean logout() throws Exception {

		// note the get operation logs us out by setting authentication cookies to:
		// LWSSO_COOKIE_KEY="" via server response header Set-Cookie
		System.out.println("Inside logout method");
		Response response = con.httpGet(con.buildUrl("authentication-point/logout"), null, null);

		return (response.getStatusCode() == HttpURLConnection.HTTP_OK);

	}

	/**
	 * @return null if authenticated.<br>
	 *         a url to authenticate against if not authenticated.
	 * @throws Exception
	 */
	public String isAuthenticated() throws Exception {

		String isAuthenticateUrl = con.buildUrl("rest/is-authenticated");
		String ret;

		Response response = con.httpGet(isAuthenticateUrl, null, null);
		int responseCode = response.getStatusCode();

		// if already authenticated
		if (responseCode == HttpURLConnection.HTTP_OK) {

			ret = null;
		}

		// if not authenticated - get the address where to authenticate
		// via WWW-Authenticate
		else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {

			Iterable<String> authenticationHeader = response.getResponseHeaders().get("WWW-Authenticate");

			for (String str : authenticationHeader) {
				System.out.println(str);
			}

			String newUrl = authenticationHeader.iterator().next().split("=")[1];
			newUrl = newUrl.replace("\"", "");
			newUrl += "/authenticate";

			// String newUrl =
			// "http://10.43.130.123:8080/qcbin/authentication-point/authenticate?login-form-required=y";

			ret = newUrl;
		}

		// Not ok, not unauthorized. An error, such as 404, or 500
		else {

			throw response.getFailure();
		}

		return ret;
	}

	public void startSession() throws Exception {
		String sessionUri = "http://10.43.130.123:8080/qcbin/rest/site-session";
		Map<String, String> headers = new HashMap<>();
		System.out.println(headers);
		headers.put("Cookie", LWSSO_Key);

		Response response = con.httpPost(sessionUri, null, headers);
		System.out.println(response.getStatusCode());
		System.out.println(response.getResponseData());
		System.out.println(response.getResponseHeaders());

		String cookie = response.getResponseHeaders().get("Set-Cookie").toString();
		System.out.println("value of Set-Cookie" + cookie);

		headers = new HashMap<>();
		// System.out.println(headers);
		response = con.httpGet(sessionUri, null, headers);
		System.out.println(response.getStatusCode());
		System.out.println(response.getResponseData());
		System.out.println(response.getResponseHeaders());

	}

	public void getValues() throws Exception {

		readConfigFile.read_Config_File();

		test_Plan_Parent_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Parent_Folder_Name");
		test_Plan_Child_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Child_Folder_Name");

		// To get test folder information by using it's name from Test LAB

		// String elementUrl
		// ="http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-set-folders?query={name['Test']}";

		// to get the particular test case from Test LAB
		// String elementUrl =
		// "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-instances?query={cycle-id[16471];test-id[7135]}&page-size=50000";

		// String elementUrl =
		// "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-configs?query={parent-id[16471]}&fields=id,name";

		// To get the test set's information within the project
		// String elementUrl
		// ="http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-sets?query={test-set-folder.name['Test']}";

		// To get the all test cases within the test set
		// String elementUrl =
		// "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-instances?query={cycle-id['23099']}&page-size=50000";

		// To get the run result value from Test Lab
		// String elementUrl
		// ="http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/runs?query={cycle-id['23099']}";

		// To get Parent test folder information by using it's name from Test PLAN
		String elementUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-folders?query={name['"
				+ test_Plan_Parent_Folder_Name + "']}";

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/xml");
		headers.put("Accept", "application/xml");
		Response response = con.httpGet(elementUrl, null, headers);
		System.out.println(response.getStatusCode());

		System.out.println("Printing response" + response.toString());
		System.out.println("This is Response data" + response.getResponseData().toString());

		System.out.println("This is response header" + response.getResponseHeaders().toString());

		System.out.println("****************************************");

		test_Plan_hierarchical_Path = (readXML.read_single_attribute(response,
				"//Entities/Entity/Fields/Field[@Name='hierarchical-path']")).trim();
		test_Plan_hierarchical_Path = test_Plan_hierarchical_Path.concat("*");
		//
		// to get the particular test cases using particular folder name and
		// hierarchical-path of that folder's parent folder from Test PLAN
		String elementUrl1 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/tests?query={test-folder.name['"
				+ test_Plan_Child_Folder_Name + "'];test-folder.hierarchical-path['" + test_Plan_hierarchical_Path
				+ "']}&page-size=50000";

		Map headers1 = new HashMap<>();
		headers1.put("Content-Type", "application/xml");
		headers1.put("Accept", "application/xml");
		Response response1 = con.httpGet(elementUrl1, null, headers1);

		System.out.println(response1.getStatusCode());

		System.out.println("Printing response" + response1.toString());
		System.out.println("This is Response data" + response1.getResponseData().toString());

		System.out.println("This is response header" + response1.getResponseHeaders().toString());
		System.out.println("****************************************");

		list_Of_Test_Plan_IDs = readXML.read_XML_Test_Ids(response1, "//Entities/Entity/Fields/Field[@Name='id']");

	}

	public void creation_Of_Test_Set_Folder() throws Exception {
		readConfigFile.read_Config_File();
		test_Lab_Parent_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Parent_Folder_Name");
		test_Lab_Child_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Child_Folder_Name");

		if (test_Lab_Parent_Folder_Name.equals(get_Test_Lab_Folder_Details())) {
			System.out.println("The Parent folder is already exist");

			if (test_Lab_Child_Folder_Name.equals(get_Test_Lab_Child_Folder_Details())) {
				System.out.println("The child folder is already exist");
			}

			else {

				String elementTestSetFolderUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-set-folders";

				String XMLstring = "<Entity>\r\n" + "<Fields>\r\n" + "<Field Name=\"parent-id\"><Value>"
						+ parent_Folder_Id + "</Value></Field>\r\n" + "<Field Name=\"name\"><Value>"
						+ test_Lab_Child_Folder_Name + "</Value></Field>\r\n" + "</Fields>\r\n" + "</Entity>";

				byte[] data = XMLstring.getBytes();

				Map<String, String> headers = new HashMap<>();
				headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
				headers.put("Content-Type", "application/xml");
				headers.put("Accept", "application/xml");

				Response response = con.httpPost(elementTestSetFolderUrl, data, headers);
				System.out.println(response.getStatusCode());
				System.out.println(response.getResponseData());
				System.out.println(response.getResponseHeaders());

				String cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();
				System.out.println(cookie1);

				child_Folder_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");
			}

		}

		else {

			String elementTestSetFolderUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-set-folders";
			String parent_Id;
			String folder_name;
			String parent_Folder_Id = null;

			List<String> list_Of_Folders = new ArrayList<>();
			list_Of_Folders.add(test_Lab_Parent_Folder_Name);

			list_Of_Folders.add(test_Lab_Child_Folder_Name);

			for (String str : list_Of_Folders) {
				if (str.equals(test_Lab_Parent_Folder_Name)) {
					parent_Id = "0";

				} else {
					parent_Id = parent_Folder_Id;

				}

				folder_name = str;
				String XMLstring = "<Entity>\r\n" + "<Fields>\r\n" + "<Field Name=\"parent-id\"><Value>" + parent_Id
						+ "</Value></Field>\r\n" + "<Field Name=\"name\"><Value>" + folder_name + "</Value></Field>\r\n"
						+ "</Fields>\r\n" + "</Entity>";

				byte[] data = XMLstring.getBytes();

				Map<String, String> headers = new HashMap<>();
				headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
				headers.put("Content-Type", "application/xml");
				headers.put("Accept", "application/xml");

				Response response = con.httpPost(elementTestSetFolderUrl, data, headers);
				System.out.println(response.getStatusCode());
				System.out.println(response.getResponseData());
				System.out.println(response.getResponseHeaders());

				String cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();
				System.out.println(cookie1);

				if (str.equals(test_Lab_Parent_Folder_Name)) {
					parent_Folder_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");
				}

				else {
					child_Folder_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");
				}

			}
		}

	}

	public void creation_Of_Test_Set() throws Exception {

		readConfigFile.read_Config_File();

		test_Lab_Test_Set_Name = readConfigFile.prop.getProperty("test_Plan_Child_Folder_Name");

		if (test_Lab_Test_Set_Name.equals(get_Test_Lab_Test_Set_Details()))

			System.out.println("Test set is already exist");
		else {

			String elementTestSetUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-sets";
			Map<String, String> headers = new HashMap<>();
			headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
			headers.put("Content-Type", "application/xml");
			headers.put("Accept", "application/xml");

			String str = "<Entity>\r\n" + "<Fields>\r\n" + "<Field Name=\"parent-id\"><Value>" + child_Folder_Id
					+ "</Value></Field>\r\n" + "<Field Name=\"name\"><Value>" + test_Lab_Test_Set_Name
					+ "</Value></Field>\r\n"
					+ "<Field Name=\"subtype-id\"><Value>hp.qc.test-set.default</Value></Field>\r\n" + "</Fields>\r\n"
					+ "</Entity>";

			byte[] data = str.getBytes();

			Response response = con.httpPost(elementTestSetUrl, data, headers);
			System.out.println("printing response" + response.toString());
			System.out.println(response.getStatusCode());
			System.out.println(response.getResponseData());
			System.out.println(response.getResponseHeaders());

			test_Set_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");

			String cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();

			System.out.println(cookie1);
		}

	}

	public void creation_Of_Test_Instance() throws Exception {
		String elementTestInstanceUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-instances";
		get_Test_Lab_Test_Instances();
		if (list_Of_Test_Lab_IDs.containsAll(list_Of_Test_Plan_IDs)) {
			System.out.println("Test cases are alredy exist");
		}

		else {
			List<String> missing_Test_Instances = list_Of_Test_Plan_IDs;
			missing_Test_Instances.removeAll(list_Of_Test_Lab_IDs);
			for (String list_Of_IDs : missing_Test_Instances) {
				String str = "<Entity>\r\n" + "<Fields>\r\n" + "<Field Name=\"cycle-id\"><Value>" + test_Set_Id
						+ "</Value></Field> \r\n" +
						// "<Field Name=\"test-config-id\"><Value>"+test_config_Id+"</Value></Field>
						// \r\n"+
						"<Field Name=\"test-id\"><Value>" + list_Of_IDs + "</Value></Field>\r\n" +
						// "<Field Name=\"owner\"><Value>"+owner+"</Value></Field>\r\n" +
						"<Field Name=\"subtype-id\"><Value>" + "hp.qc.test-instance.MANUAL" + "</Value></Field>\r\n"
						+ "</Fields>\r\n" + "</Entity>";
				byte[] data = str.getBytes();
				Map<String, String> headers = new HashMap<>();

				headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
				headers.put("Content-Type", "application/xml");
				headers.put("Accept", "application/xml");

				Response response = con.httpPost(elementTestInstanceUrl, data, headers);
				System.out.println("Printing response" + response.toString());
				System.out.println(response.getStatusCode());
				System.out.println(response.getResponseData());
				System.out.println(response.getResponseHeaders());

				String cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();

				System.out.println(cookie1);

			}
		}

	}

	public void creation_Of_runs() throws Exception {
		try {
			get_Test_Lab_Test_Instances();

			// get_Test_Lab_Test_Runs();

			String elementTestRuns = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/runs";

			LocalDate current_Date = (java.time.LocalDate.now());

			LocalTime current_Time = (java.time.LocalTime.now());

			int count = 0;

			for (String list_Of_IDs : list_Of_Test_Plan_IDs) {

				String temp = testcycl_Id.get(count);
				System.out.println(temp);

				String temp1 = test_config_id.get(count);
				System.out.println(temp1);

				System.out.println(test_Set_Id);
				System.out.println(list_Of_IDs);

				String str = "<Entity>\r\n" + "<Fields>\r\n" + "<Field Name=\"test-config-id\"><Value>" + temp1
						+ "</Value></Field>\r\n" + "<Field Name=\"cycle-id\"><Value>" + test_Set_Id
						+ "</Value></Field>\r\n" + "<Field Name=\"test-id\"><Value>" + list_Of_IDs
						+ "</Value></Field>\r\n" + "<Field Name=\"testcycl-id\"><Value>" + temp + "</Value></Field>\r\n"
						// "<Field Name=\"build-revision\"><Value>1</Value></Field>\r\n" +
						+ "<Field Name=\"name\"><Value>Testing</Value></Field>\r\n" + "<Field Name=\"owner\"><Value>"
						+ Constants.USERNAME + "</Value></Field>\r\n"
						+ "<Field Name=\"status\"><Value>Passed</Value></Field>\r\n"
						+ "<Field Name=\"iters-sum-status\"><Value>Passed</Value></Field>\r\n"
						+ "<Field Name=\"subtype-id\"><Value>hp.qc.run.MANUAL</Value></Field>\r\n"
						// "<Field Name=\"detail\"><Value>QCAutomationTest</Value></Field>\r\n" +
						+ "<Field Name=\"duration\"><Value>0</Value></Field>\r\n"
						+ "<Field Name=\"execution-date\"><Value>" + current_Date + "</Value></Field>\r\n"
						+ "<Field Name=\"execution-time\"><Value>" + current_Time + "</Value></Field>\r\n"
//						+ "<Field Name=\"status\"><Value>Passed</Value></Field>\r\n"
						+ "</Fields>\r\n" + "</Entity>";

				byte[] data = str.getBytes();
				Map<String, String> headers = new HashMap<>();

				headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
				headers.put("Content-Type", "application/xml");
				headers.put("Accept", "application/xml");

				Response response = con.httpPost(elementTestRuns, data, headers);
				System.out.println("printing response" + response.toString());
				System.out.println(response.getStatusCode());
				System.out.println(response.getResponseData());
				System.out.println(response.getResponseHeaders());

				String cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();

				System.out.println(cookie1);

				String id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");

				String elementTestRunsSteps = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/runs/"+id;
				
				str = "<Entity>\r\n" 
						+ "<Fields>\r\n" 
//						+ "<Field Name=\"test-config-id\"><Value>" + temp1+ "</Value></Field>\r\n" 
//						+ "<Field Name=\"cycle-id\"><Value>" + test_Set_Id+ "</Value></Field>\r\n" 
//						+ "<Field Name=\"test-id\"><Value>" + list_Of_IDs+ "</Value></Field>\r\n" 
//						+ "<Field Name=\"testcycl-id\"><Value>" + temp + "</Value></Field>\r\n"
//						+ "<Field Name=\"id\"><Value>" + id + "</Value></Field>\r\n"
//						+ "<Field Name=\"vc-status\"><Value>Passed</Value></Field>\r\n" 
//						+ "<Field Name=\"name\"><Value>Testing</Value></Field>\r\n" + "<Field Name=\"owner\"><Value>"
//						+ Constants.USERNAME + "</Value></Field>\r\n"
//						+ "<Field Name=\"status\"><Value>Failed</Value></Field>\r\n"
						+ "<Field Name=\"iters-sum-status\"><Value>No Run</Value></Field>\r\n"
//						+ "<Field Name=\"subtype-id\"><Value>hp.qc.run.MANUAL</Value></Field>\r\n"
						// "<Field Name=\"detail\"><Value>QCAutomationTest</Value></Field>\r\n" +
//						+ "<Field Name=\"duration\"><Value>0</Value></Field>\r\n"
//						+ "<Field Name=\"execution-date\"><Value>" + current_Date + "</Value></Field>\r\n"
//						+ "<Field Name=\"execution-time\"><Value>" + current_Time + "</Value></Field>\r\n"
						+ "<Field Name=\"status\"><Value>No Run</Value></Field>\r\n" 
						+ "</Fields>\r\n" + "</Entity>";

				byte[] data1 = str.getBytes();
				headers = new HashMap<>();

				headers.put("X-QC-Ignore-Customizable-Required-Fields-Validation", "Y");
				headers.put("Content-Type", "application/xml");
				headers.put("Accept", "application/xml");

//				response = con.httpGet(elementTestRunsSteps, null, headers);
				response = con.httpPut(elementTestRunsSteps, data1, headers);
				System.out.println("printing response" + response.toString());
				System.out.println(response.getStatusCode());
				System.out.println(response.getResponseData());
				System.out.println(response.getResponseHeaders());

				cookie1 = response.getResponseHeaders().get("Set-Cookie").toString();

				System.out.println(cookie1);

				count++;
			}
		} catch (Exception e) {
			System.out.println(e);
		}

	}

	public String get_Test_Lab_Folder_Details() throws Exception {
		String parent_folder_name;

		readConfigFile.read_Config_File();

		test_Plan_Parent_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Parent_Folder_Name");
		test_Plan_Child_Folder_Name = readConfigFile.prop.getProperty("test_Plan_Child_Folder_Name");

		String elementUrl = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-set-folders?query={name['"
				+ test_Plan_Parent_Folder_Name + "']}";

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/xml");
		headers.put("Accept", "application/xml");
		Response response = con.httpGet(elementUrl, null, headers);
		System.out.println(response.getStatusCode());

		System.out.println("Printing response" + response.toString());
		System.out.println("This is Response data" + response.getResponseData().toString());

		System.out.println("This is response header" + response.getResponseHeaders().toString());

		test_Lab_hierarchical_Path = (readXML.read_single_attribute(response,
				"//Entities/Entity/Fields/Field[@Name='hierarchical-path']")).trim();
		test_Lab_hierarchical_Path = test_Lab_hierarchical_Path + "*";

		parent_Folder_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");

		parent_folder_name = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='name']");

		return parent_folder_name;

	}

	public String get_Test_Lab_Child_Folder_Details() throws Exception {
		String child_Folder_Name;

		String elementUrl1 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-set-folders?query={name["
				+ test_Plan_Child_Folder_Name + "];parent-id[" + parent_Folder_Id + "]}&page-size=50000";

		Map headers = new HashMap<>();
		headers.put("Content-Type", "application/xml");
		headers.put("Accept", "application/xml");
		Response response = con.httpGet(elementUrl1, null, headers);

		System.out.println(response.getStatusCode());

		System.out.println("Printing response" + response.toString());
		System.out.println("This is Response data" + response.getResponseData().toString());

		System.out.println("This is response header" + response.getResponseHeaders().toString());

		child_Folder_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");

		child_Folder_Name = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='name']");

		return child_Folder_Name;

	}

	public String get_Test_Lab_Test_Set_Details() throws Exception {
		String test_Set_Name;
		String elementUrl2 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-sets?query={name["
				+ test_Plan_Child_Folder_Name + "];parent-id[" + child_Folder_Id + "]}&page-size=50000";
		;
		Map headers = new HashMap<>();
		headers.put("Content-Type", "application/xml");
		headers.put("Accept", "application/xml");
		Response response = con.httpGet(elementUrl2, null, headers);

		System.out.println(response.getStatusCode());

		System.out.println("Printing response" + response.toString());
		System.out.println("This is Response data" + response.getResponseData().toString());

		System.out.println("This is response header" + response.getResponseHeaders().toString());

		test_Set_Id = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='id']");

		test_Set_Name = readXML.read_single_attribute(response, "//Entity/Fields/Field[@Name='name']");

		return test_Set_Name;
	}

	public void get_Test_Lab_Test_Instances() throws Exception {

		String elementUrl3 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-instances?query={test-set.id["
				+ test_Set_Id + "];test-set.name[" + test_Plan_Child_Folder_Name + "]}";// test-set-folder.id["+test_Set_Id+"]}&page-size=50000";
		Map headers = new HashMap<>();
		headers.put("Content-Type", "application/xml");
		headers.put("Accept", "application/xml");
		Response response = con.httpGet(elementUrl3, null, headers);

		System.out.println(response.getStatusCode());

		System.out.println("Printing response" + response.toString());
		System.out.println("This is Response data" + response.getResponseData().toString());
		System.out.println("This is response header" + response.getResponseHeaders().toString());

		list_Of_Test_Lab_IDs = readXML.read_XML_Test_Ids(response, "//Entities/Entity/Fields/Field[@Name='test-id']");

		testcycl_Id = new ArrayList<>();
		test_config_id = new ArrayList<>();
		for (String str : list_Of_Test_Lab_IDs) {

			String elementUrl4 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/test-instances?query={test-id["
					+ str + "];cycle-id[" + test_Set_Id + "]}&page-size=50000";
			headers = new HashMap<>();
			headers.put("Content-Type", "application/xml");
			headers.put("Accept", "application/xml");
			response = con.httpGet(elementUrl4, null, headers);

			System.out.println(response.getStatusCode());

			System.out.println("Printing response" + response.toString());
			System.out.println("This is Response data" + response.getResponseData().toString());

			System.out.println("This is response header" + response.getResponseHeaders().toString());

			System.out.println(readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='id']"));
			testcycl_Id.add(readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='id']"));

			System.out.println(
					readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='test-config-id']"));
			test_config_id.add(
					readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='test-config-id']"));
		}

		System.out.println(testcycl_Id);

	}

	public void get_Test_Lab_Test_Runs() throws Exception {
		List<String> test_Run_Id = new ArrayList<>();

		for (String str : list_Of_Test_Lab_IDs) {
			int count = 0;
			String testcycle_id = testcycl_Id.get(count);

			String elementUrl5 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/runs?query={test-id["
					+ str + "]}";// testcycl-id["+testcycle_id+"]}&page-size=50000";

			Map headers = new HashMap<>();
			headers.put("Content-Type", "application/xml");
			headers.put("Accept", "application/xml");
			Response response = con.httpGet(elementUrl5, null, headers);

			System.out.println(response.getStatusCode());

			System.out.println("Printing response" + response.toString());
			System.out.println("This is Response data" + response.getResponseData().toString());

			System.out.println("This is response header" + response.getResponseHeaders().toString());

			System.out.println(
					readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='testcycl-id']"));

			test_Run_Id.add(readXML.read_single_attribute(response, "//Entities/Entity/Fields/Field[@Name='id']"));

			elementUrl5 = "http://10.43.130.123:8080/qcbin/rest/domains/DEFAULT/projects/ClearSight/runs/1/run-steps}";

			headers = new HashMap<>();
			headers.put("Content-Type", "application/xml");
			headers.put("Accept", "application/xml");
			response = con.httpGet(elementUrl5, null, headers);

			System.out.println(response.getStatusCode());

			System.out.println("Printing response" + response.toString());
			System.out.println("This is Response data" + response.getResponseData().toString());

			System.out.println("This is response header" + response.getResponseHeaders().toString());
			count++;
		}

		System.out.println(test_Run_Id);
		// return null;

	}

}
