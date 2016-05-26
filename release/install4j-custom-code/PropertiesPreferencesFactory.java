import java.io.File;
import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

/**
 * Factory for Preferences implementation that uses a properties file for storage. Not intended for production!
 * 
 * @author David Dossot
 */
public class PropertiesPreferencesFactory implements PreferencesFactory {
  static {
    // we create the target folder if needed
    new File(PropertiesPreferences.getTargetFolder(true)).mkdirs();
    new File(PropertiesPreferences.getTargetFolder(false)).mkdirs();
  }
  
  public Preferences userRoot() {
    return new PropertiesPreferences("", false);
  }

  public Preferences systemRoot() {
    return new PropertiesPreferences("", true);
  }
}