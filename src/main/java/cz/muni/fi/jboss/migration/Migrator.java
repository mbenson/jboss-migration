package cz.muni.fi.jboss.migration;

import cz.muni.fi.jboss.migration.ex.*;
import cz.muni.fi.jboss.migration.migrators.connectionFactories.ResAdapterMigrator;
import cz.muni.fi.jboss.migration.migrators.dataSources.DatasourceMigrator;
import cz.muni.fi.jboss.migration.migrators.logging.LoggingMigrator;
import cz.muni.fi.jboss.migration.migrators.security.SecurityMigrator;
import cz.muni.fi.jboss.migration.migrators.server.ServerMigrator;
import cz.muni.fi.jboss.migration.spi.IMigrator;
import cz.muni.fi.jboss.migration.utils.AS7ModuleUtils;
import cz.muni.fi.jboss.migration.utils.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.eclipse.persistence.exceptions.JAXBException;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Migrator is class, which represents all functions of the application.
 *
 * @author Roman Jakubco
 *         Date: 1/24/13
 *         Time: 10:25 AM
 */

public class Migrator {

    private Configuration config;

    private MigrationContext ctx;

    private List<IMigrator> migrators;

    public Migrator(Configuration config, MigrationContext context) {
        this.config = config;
        this.ctx = context;
        this.migrators = createMigrators();
    }

    private List<IMigrator> createMigrators() {
        List<IMigrator> migrators = new LinkedList();
        migrators.add(new DatasourceMigrator(this.config.getGlobal(), this.config.getForMigrator(DatasourceMigrator.class)));
        migrators.add(new ResAdapterMigrator(this.config.getGlobal(), this.config.getForMigrator(ResAdapterMigrator.class)));
        migrators.add(new SecurityMigrator(this.config.getGlobal(), this.config.getForMigrator(SecurityMigrator.class)));
        migrators.add(new LoggingMigrator(this.config.getGlobal(), this.config.getForMigrator(LoggingMigrator.class)));
        migrators.add(new ServerMigrator(this.config.getGlobal(), this.config.getForMigrator(ServerMigrator.class)));

        return migrators;
    }

//    public void init(){
//
//    }

    /**
     * Method which calls method for loading configuration data from AS5 on all migrators.
     *
     * @throws LoadMigrationException
     */
    public void loadAS5Data() throws LoadMigrationException {
        try {
            for (IMigrator mig : this.migrators) {
                mig.loadAS5Data(this.ctx);
            }
        } catch (JAXBException e) {
            throw new LoadMigrationException(e);
        }
    }

    /**
     * Method which calls method for applying migrated configuration on all migrators.
     *
     * @throws ApplyMigrationException if inserting of generated nodes fails.
     */
    public void apply() throws ApplyMigrationException {
        for (IMigrator mig : this.migrators) {
            mig.apply(this.ctx);
        }
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            StreamResult result = new StreamResult(new File(this.config.getGlobal().getStandaloneFilePath()));
            //StreamResult result = new StreamResult(System.out);
            DOMSource source = new DOMSource(this.ctx.getStandaloneDoc());
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }


    }

    /**
     * Method which calls method for generating Dom Nodes on all migrators.
     *
     * @return List containing all generated Nodes
     * @throws MigrationException if migrating of file or generating of nodes fails.
     */
    public List<Node> getDOMElements() throws MigrationException {
        List<Node> elements = new ArrayList<>();
        for (IMigrator mig : this.migrators) {
            elements.addAll(mig.generateDomElements(this.ctx));
        }

        return elements;
    }

    /**
     * Method which calls method for generating Cli scripts on all migrators.
     *
     * @return List containing generated scripts from all migrated subsystems
     * @throws CliScriptException if creation of scripts fail
     */
    public List<String> getCLIScripts() throws CliScriptException {
        List<String> scripts = new ArrayList<>();
        for (IMigrator mig : this.migrators) {
            scripts.addAll(mig.generateCliScripts(this.ctx));
        }

        return scripts;
    }

    /**
     * Method for copying all necessary files for migration from AS5 to their place in AS7 home folder.
     *
     * @throws CopyException if copying of files fails.
     */
    public void copyItems() throws CopyException {
        String targetPath = this.config.getGlobal().getDirAS7();
        File dir = new File(this.config.getGlobal().getDirAS5() + File.separator + this.config.getGlobal().getProfileAS5());

        for (RollbackData rollData : this.ctx.getRollbackDatas()) {
            if (rollData.getName() == null || rollData.getName().isEmpty()) {
                throw new NullPointerException();
            }

            NameFileFilter nff;
            if (rollData.getType().equals("driver")) {
                final String name = rollData.getName();
                nff = new NameFileFilter(name) {
                    @Override
                    public boolean accept(File file) {
                        return file.getName().contains(name) && file.getName().contains("jar");
                    }
                };
            } else {
                nff = new NameFileFilter(rollData.getName());
            }

            List<File> list = (List<File>) FileUtils.listFiles(dir, nff, FileFilterUtils.makeCVSAware(null));

            switch (rollData.getType()) {
                case "driver": {
                    // For now only expecting one jar for driver. Pick the first one.
                    if (list.isEmpty()) {
                        // Special case for freeware jdbc driver jdts.jar
                        if (rollData.getAltName() != null) {
                            final String altName = rollData.getAltName();

                            nff = new NameFileFilter(altName) {
                                @Override
                                public boolean accept(File file) {
                                    return file.getName().contains(altName) && file.getName().contains("jar");
                                }
                            };
                            List<File> altList = (List<File>) FileUtils.listFiles(dir, nff,
                                    FileFilterUtils.makeCVSAware(null));

                            Utils.setRollbackData(rollData, altList, targetPath);
                            break;
                        } else {
                            throw new CopyException("Cannot locate driver jar for driver:" + rollData.getName() + "!");
                        }
                    } else {
                        Utils.setRollbackData(rollData, list, targetPath);
                    }
                }
                break;
                case "log":
                    Utils.setRollbackData(rollData, list, targetPath);
                    break;
                case "security":
                    Utils.setRollbackData(rollData, list, targetPath);
                    break;
                case "resource":
                    Utils.setRollbackData(rollData, list, targetPath);
                    break;
            }
        }

        try {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");

            for (RollbackData cp : this.ctx.getRollbackDatas()) {
                if (cp.getType().equals("driver")) {
                    File directories = new File(cp.getTargetPath() + File.separator);
                    FileUtils.forceMkdir(directories);
                    File module = new File(directories.getAbsolutePath() + File.separator + "module.xml");

                    if (module.createNewFile()) {
                        transformer.transform(new DOMSource(AS7ModuleUtils.createModuleXML(cp)), new StreamResult(module));
                    } else {
                        throw new CopyException("File \"module.xml\" already exists!");
                    }
                }

                FileUtils.copyFileToDirectory(new File(cp.getHomePath()), new File(cp.getTargetPath()));
            }
        } catch (IOException | ParserConfigurationException | TransformerException e) {
            throw new CopyException(e);
        }
    }
}