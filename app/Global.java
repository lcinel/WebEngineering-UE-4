import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import data.JSONDataInserter;
import play.Application;
import play.GlobalSettings;
import play.Logger;
import play.Play;
import play.db.jpa.JPA;
import play.libs.F.Function0;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Global extends GlobalSettings {
	
	@play.db.jpa.Transactional
	public static void insertJSonData() throws IOException {
		File file = new File(Play.application().configuration().getString("questions.filePath"));
		InputSupplier<FileInputStream> inputStreamSupplier = 
				Files.newInputStreamSupplier(file);
		FileInputStream inputStream = inputStreamSupplier.getInput();
		JSONDataInserter.insertData(inputStream);
		Logger.info("Data from json file '" + file.getName() + "' inserted.");
	}
	
	@play.db.jpa.Transactional
    public void onStart(Application app) {
       try {
		JPA.withTransaction(new Function0<Boolean>() {

			@Override
			public Boolean apply() throws Throwable {
				insertJSonData();
				return true;
			}
			   
			});
	} catch (Throwable e) {
		e.printStackTrace();
	}
       
    }

    public void onStop(Application app) {
        Logger.info("Application shutdown...");
    }

}