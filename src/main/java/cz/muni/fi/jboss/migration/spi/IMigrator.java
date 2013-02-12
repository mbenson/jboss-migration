package cz.muni.fi.jboss.migration.spi;

import cz.muni.fi.jboss.migration.Configuration;
import cz.muni.fi.jboss.migration.GlobalConfiguration;
import cz.muni.fi.jboss.migration.MigrationContext;
import cz.muni.fi.jboss.migration.ex.ApplyMigrationException;
import cz.muni.fi.jboss.migration.ex.CliScriptException;
import cz.muni.fi.jboss.migration.ex.LoadMigrationException;
import cz.muni.fi.jboss.migration.ex.NodeGenerationException;
import org.w3c.dom.Node;

import java.util.List;

/**
 * Interface for migrators
 *
 * @author Roman Jakubco
 *         Date: 1/24/13
 *         Time: 10:35 AM
 */

public interface IMigrator {
    
    public GlobalConfiguration getGlobalConfig();
    public void setGlobalConfig( GlobalConfiguration conf );

    /**
     * Method for loading all files from AS5 and converting them to objects for migration which are then stored in Mig-
     * rationContext
     *
     * @param ctx context of migration with necessary object and information
     * @throws LoadMigrationException if loading of AS5 configuration fails (missing files / cannot read / wrong content)
     */
    public void loadAS5Data(MigrationContext ctx) throws LoadMigrationException;

    /**
     * Method for inserting migrated data to fresh standalone file.
     *
     * @param ctx context of migration with necessary object and information
     * @throws ApplyMigrationException if inserting of generated nodes fails
     */
    public void apply(MigrationContext ctx) throws ApplyMigrationException;

    /**
     * Method for generating Dom nodes from data stored in MigrationContext. Basically method representing actual migration
     * of the XML configuration files from AS5 to AS7.
     *
     * @param ctx context of migration with necessary object and information
     * @return List of all nodes, which represent migrated configuration of AS5
     * @throws NodeGenerationException if something when wrong with the migration of data and generation of Dom Nodes
     */
    public List<Node> generateDomElements(MigrationContext ctx) throws NodeGenerationException;

    /**
     * Generates CLI scripts from migrated data, which are generated by generateDomElements.
     *
     * @param ctx context of migration with necessary object and information
     * @return List of CLI scripts, which represent migrated configuration of AS5
     * @throws CliScriptException if required attributes for creation of scripts are missing
     */
    public List<String> generateCliScripts(MigrationContext ctx) throws CliScriptException;

    
    /**
     *  Examines a configuration property, typically acquired as console app params.
     * 
     *  @param value May be null, e.g. if the property didn't have '=value' part.
     */
    public int examineConfigProperty(Configuration.ModuleSpecificProperty moduleOption);
    //public int examineConfigProperty(String moduleName, String propName, String value);
    
}
