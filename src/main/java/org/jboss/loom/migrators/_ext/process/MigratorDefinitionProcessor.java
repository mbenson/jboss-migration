package org.jboss.loom.migrators._ext.process;


import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jboss.loom.actions.CliCommandAction;
import org.jboss.loom.actions.CopyFileAction;
import org.jboss.loom.actions.IMigrationAction;
import org.jboss.loom.actions.ManualAction;
import org.jboss.loom.actions.ModuleCreationAction;
import org.jboss.loom.actions.XsltAction;
import org.jboss.loom.conf.Configuration;
import org.jboss.loom.ex.MigrationException;
import org.jboss.loom.migrators._ext.ActionDefs;
import org.jboss.loom.migrators._ext.ActionDefs.CliActionDef;
import org.jboss.loom.migrators._ext.ActionDefs.CopyActionDef;
import org.jboss.loom.migrators._ext.ActionDefs.ManualActionDef;
import org.jboss.loom.migrators._ext.ActionDefs.ModuleActionDef;
import org.jboss.loom.migrators._ext.ActionDefs.XsltActionDef;
import org.jboss.loom.migrators._ext.ContainerOfStackableDefs;
import org.jboss.loom.migrators._ext.DefinitionBasedMigrator;
import org.jboss.loom.migrators._ext.MigratorDefinition;
import org.jboss.loom.spi.IConfigFragment;
import org.jboss.loom.utils.el.JuelCustomResolverEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Processor for migrator definitions from .mig.xml files.
 *  These define:
 * 
 *    * JAXB classes, coded as Groovy files.
 *    * Queries which result in a list of IConfigFragment (typically, backed by those JAXB classes).
 *    * ForEach iterators which iterate over results of queries.
 *      * ForEach's define a variable which is available in EL and sub-contexts.
 * 
 *    * Nested elements which result in actions.
 *      * These actions can depend on each other.
 *      * Actions may contain warnings.
 *      * Some strings may contain EL expressions - e.g. ${varName.bean.path}
 * 
 *  TODO:
 * 
 *  This is the first attempt on implementation. It's a bit too procedural I guess and could be rewritten to something smarter,
 *  perhaps Ant tasks engine could be used as a basis.
 * 
 *  Other thing to improve could be to process the instructions in the order of appearance in the XML file.
 * 
 *  @author Ondrej Zizka, ozizka at redhat.com
 */
public class MigratorDefinitionProcessor {
    private static final Logger log = LoggerFactory.getLogger( MigratorDefinitionProcessor.class );

    
    // Input stuff
    
    /**  The migrator instance whose definition we are processing. */
    final DefinitionBasedMigrator defBasedMig;
    
    
    // Work stuff
    
    /** Stack of nested constructs - forEach, action etc. */
    private final ContextsStack stack = new ContextsStack();
    public ContextsStack getStack() { return stack; }

    /**  Currently built statically, but eventually impls will come from Groovy classes as well.  */
    static final Map<Class<? extends MigratorDefinition.ActionDef>, Class<? extends IActionDefHandler>> handlers;
    static {
        handlers = findActionDefHandlers(); // TBD: This should be done in the MigrationEngine or so.
    }
    private static Map<Class<? extends MigratorDefinition.ActionDef>, Class<? extends IActionDefHandler>> findActionDefHandlers() {
        final HashMap handlers = new HashMap();
        handlers.put( ActionDefs.CliActionDef.class, null);
        handlers.put( ActionDefs.ModuleActionDef.class, null);
        handlers.put( ActionDefs.CopyActionDef.class, null);
        handlers.put( ActionDefs.XsltActionDef.class, null);
        handlers.put( ActionDefs.ManualActionDef.class, null);
        return handlers;
    }
    
    
    // Services
    
    /** Resolver of all ${...} expressions. Based on this processor's current stack. */
    private JuelCustomResolverEvaluator eval = new JuelCustomResolverEvaluator( this.stack );


    public MigratorDefinitionProcessor( DefinitionBasedMigrator defBasedMig ) {
        final Variables rootCtx = new RootContext()
        .setVariable("mig", defBasedMig)
        // Shorthands
        .setVariable("conf", defBasedMig.getGlobalConfig()) 
        .setVariable("srcServer", defBasedMig.getGlobalConfig().getSourceServerConf()) 
        .setVariable("destServer", defBasedMig.getGlobalConfig().getTargetServerConf()) 
        .setVariable("migDef", defBasedMig.getDescriptor())
        .setVariable("migDefDir", defBasedMig.getDescriptor().getOrigin().getFile().getParentFile())
        // Others
        .setVariable("workdir", new File("."));
        
        // User variables
        for( Map.Entry<String, String> entry : defBasedMig.getGlobalConfig().getUserVars().entrySet() ){
            rootCtx.setVariable( entry.getKey(), entry.getValue() );
        }

        this.stack.push( (ProcessingStackItem) rootCtx );
        this.defBasedMig = defBasedMig;
    }
    
    

    
    /**
     *  Public method for the root class.
     */
    public List<IMigrationAction> process( MigratorDefinition migDef ) throws MigrationException {
        return this.processChildren( migDef );
    }

    
    /**
     *  Recursively processes the .mig.xml descriptor into Actions.
     *  TODO: Could be better to put the stack manipulation to the beginning and end of the method.
     */
    private List<IMigrationAction> processChildren( ContainerOfStackableDefs defContainer ) throws MigrationException {
        
        List<IMigrationAction> actions = new LinkedList();
        
        // Filter.
        if( defContainer.filter != null ){
            if( ! GroovyScriptUtils.evaluateGroovyExpressionToBool( defContainer.filter, this.stack ) )
                return actions;
        }
        
        // ForEach defs.
        if( defContainer.hasForEachDefs() )
        for( MigratorDefinition.ForEachDef forEachDef : defContainer.getForEachDefs() ) {
            
            DefinitionBasedMigrator.ConfigLoadResult queryResult = this.defBasedMig.getQueryResultByName( forEachDef.queryName ); 
            if( null == queryResult )
                throw new MigrationException("Query '"+forEachDef.queryName+"' not found. "
                        /*+ "Needed at " + XmlUtils.formatLocation(forEachDef.location) */ );
            
            ForEachContext forEachContext = new ForEachContext( forEachDef, this );
            this.getStack().push( forEachContext );
            
            // For each item in query result...
            //for( IConfigFragment configFragment : queryResult.configFragments ) {
            for( IConfigFragment configFragment : forEachContext ) {
                // A variable is set automatically in next().
                
                // Recurse.
                this.processChildren( forEachDef );
            }
            this.getStack().pop();
        }
        
        // Action definitions.
        // TODO: Currently, actions processing is hard-coded. This needs to be brought to meta-data.
        if( defContainer.hasActionDefs() )
        for( MigratorDefinition.ActionDef actionDef : defContainer.getActionDefs() ) {
            IMigrationAction action = createActionFromDef( actionDef );
            
            // Recurse
            this.getStack().push( new ActionContext( action, actionDef.varName ) );
            this.processChildren( actionDef );
            this.getStack().pop();
            
            actions.add( action );
        }
        
        // Warnings
        if( defContainer.warning != null ){
            String warnStr = this.eval.evaluateEL( defContainer.warning ); // EL
            this.getStack().addWarning( warnStr );
        }
            
        return actions;
    }// process();


    
    /**
     *  Creates an action according to the definition, which is a subclass of ActionDef.
     */
    private IMigrationAction createActionFromDef( MigratorDefinition.ActionDef actionDef ) throws MigrationException {
        
        IMigrationAction action;
        
        // Relative paths in mig defs refer to it's originating dir.
        final File defOriginFile = this.defBasedMig.getDescriptor().getOrigin().getFile();
        final File baseDir = defOriginFile.getParentFile(); 
        
        // Switch by subclass.
        /*Class<? extends IActionDefHandler> handlerClass = handlers.get( actionDef.getClass() );
        if( handlerClass == null )
            throw new MigrationException("No action handler defined for " + actionDef.getClass().getName() );
        
        IMigrationAction createAction;
        try {
            createAction = handlerClass.newInstance().setDefBasedMig(this.defBasedMig).createAction( actionDef );
            return createAction;
        } catch( InstantiationException | IllegalAccessException ex ) {
            throw new MigrationException("Failed instantiating " + actionDef.getClass().getName() );
        }*/
        // TBD: Create the built-in handlers.

        
        // TODO: EL
        
        // ManualAction
        if( actionDef instanceof ActionDefs.ManualActionDef ){
            ManualActionDef def = (ManualActionDef) actionDef;
            action = new ManualAction();
            // Warning(s)
            //action.getWarnings().add( def.warning ); // Done through context after EL resolving.
        }

        
        // FileBasedAction
        else if( actionDef instanceof ActionDefs.FileBasedActionDef ){
            
            // Common for FileBasedActionDef
            ActionDefs.FileBasedActionDef def_ = (ActionDefs.FileBasedActionDef) actionDef;
            CopyFileAction.IfExists ifExists = def_.ifExists == null 
                    ? CopyFileAction.IfExists.FAIL
                    : CopyFileAction.IfExists.valueOf( def_.ifExists );
            File dest = new File( baseDir, def_.dest );
            
            // Subtypes
            if( actionDef instanceof ActionDefs.CopyActionDef ){
                CopyActionDef def = (CopyActionDef) actionDef;
                //action = new CopyFileAction( DefinitionBasedMigrator.class, new File(def.pathMask), new File(def.dest), ifExists ); 
                action = new CopyFileAction( DefinitionBasedMigrator.class, def.pathMask, baseDir, dest, ifExists, false ); 
            } 
            else if( actionDef instanceof ActionDefs.XsltActionDef ){
                XsltActionDef def = (XsltActionDef) actionDef;
                File xslt  = new File( baseDir, def.xslt );
                //boolean failIfExists = "true".equals( actionDef.attribs.get("failIfExists") );
                action = new XsltAction( DefinitionBasedMigrator.class, def.pathMask, baseDir, xslt, dest, ifExists, false ); 
            }
            else throw new IllegalStateException("Unexpected subclass: " + actionDef.getClass() );
        }
        
        
        // CliAction
        else if( actionDef instanceof ActionDefs.CliActionDef ){
            CliActionDef def = (CliActionDef) actionDef;

            //ModelNode modelNode = ModelNode.fromString( def.command );
            //action = new CliCommandAction( DefinitionBasedMigrator.class, def.command, modelNode ); 
            action = new CliCommandAction( DefinitionBasedMigrator.class, def.command ); 
        }
        
        
        // ModuleAction
        else if( actionDef instanceof ActionDefs.ModuleActionDef ){
            ModuleActionDef def = (ModuleActionDef) actionDef;

            File jar     = new File( baseDir, def.jarPath );
            
            //String[] deps = parseDeps( actionDef.attribs.get("deps") );
            String[] deps = ( def.deps == null )
                    ? new String[0]
                    : def.deps.toArray( new String[def.deps.size()] );
            
            Configuration.IfExists ifExists = def.ifExists == null 
                    ? Configuration.IfExists.FAIL
                    : Configuration.IfExists.valueOf( def.ifExists );
            action = new ModuleCreationAction( DefinitionBasedMigrator.class, def.name, deps, jar, ifExists );
        }
        
        
        // ...unknown.
        else{
            throw new MigrationException("Unsupported action type '" + actionDef.typeVal 
                    /*+ "' in " + cont.location.getSystemId() */ );
        }
        
        
        return action;
        
    }// createActionFromDef()


    
    /**
     *  Will be used for handlers created out of the code in createActionFromDef().
     */
    public static interface IActionDefHandler {
        /** Just stores the value, instead of a constructor. */
        IActionDefHandler setDefBasedMig( DefinitionBasedMigrator mig );
        
        /** Creates the action based on given definition. */
        IMigrationAction createAction( MigratorDefinition.ActionDef actionDef );
    }

    

}// class
