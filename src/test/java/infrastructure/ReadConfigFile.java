package infrastructure;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Properties;

public class ReadConfigFile {

	public Properties prop = new Properties();

	public void read_Config_File() throws Exception {
		FileInputStream ip = new FileInputStream(
				"src\\test\\java\\infrastructure\\config.properties");
		;

		prop.load(ip);

	}

}
