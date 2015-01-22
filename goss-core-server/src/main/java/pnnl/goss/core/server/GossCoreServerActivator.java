package pnnl.goss.core.server;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pnnl.goss.core.server.internal.BasicDataSourceCreatorImpl;
import pnnl.goss.core.server.internal.GossDataServicesImpl;
import pnnl.goss.core.server.internal.GossRequestHandlerRegistrationImpl;
import pnnl.goss.core.server.internal.GridOpticsServer;

public class GossCoreServerActivator  implements BundleActivator, ManagedService {
    private static final String CONFIG_PID = "pnnl.goss.core.server";
    private static final String DS_CONFIG_PID = "pnnl.goss.datasources";
    private static Logger log = LoggerFactory.getLogger(GossCoreServerActivator.class);

    private GridOpticsServer server;
    private BasicDataSourceCreatorImpl dataSourceCreator;
    private GossDataServicesImpl dataServices;
    private GossRequestHandlerRegistrationImpl registrationService;
    private List<ServiceRegistration<?>> registrations = new ArrayList<>();

    private BundleContext context;
    private Dictionary<String, Object> config = new Hashtable<>();
    private Dictionary<String, Object> datasourcesConfig = new Hashtable<>();


    @Override
    public void updated(Dictionary<String, ?> properties)
            throws ConfigurationException {
        log.debug("Updated configuration");
        if (properties != null){
            Enumeration<String> keys = properties.keys();
            Dictionary<String, Object> updatedprops = null;
            if (properties.get(Constants.SERVICE_PID).equals(CONFIG_PID)){
                updatedprops = config;
            }
            else{
                log.debug("Updating datasources");
                updatedprops = datasourcesConfig;
            }
            while (keys.hasMoreElements()){
                String k = keys.nextElement();
                log.debug("CONFIG: "+k);
                updatedprops.put(k, properties.get(k));
            }
        }

        if(registrations.size() > 0){
            unregisterServices();
        }

        try {
            registerServices();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Registers the basic datasource creator, dataservices, and
     * request handler registration services.  After that this function starts
     * the goss server for processing.
     *
     * @throws Exception
     */
    private void registerServices() throws Exception{
        if (config.size() <= 0){
            throw new InvalidConfigurationException("Must have configured bundle.");
        }

        dataSourceCreator = new BasicDataSourceCreatorImpl();
        dataServices = new GossDataServicesImpl(dataSourceCreator);
        dataServices.update(datasourcesConfig);

        registrationService = new GossRequestHandlerRegistrationImpl(dataServices);

        registrations.add(context.registerService(BasicDataSourceCreator.class,
                dataSourceCreator, new Hashtable<String, Object>()));
        registrations.add(context.registerService(GossDataServices.class,
                dataServices, new Hashtable<String, Object>()));
        registrations.add(context.registerService(GossRequestHandlerRegistrationService.class,
                registrationService, new Hashtable<String, Object>()));

        server = new GridOpticsServer(registrationService,config, false);
    }

    private void unregisterServices(){
        while(registrations.size() > 0){
            ServiceRegistration<?> reg = registrations.remove(0);
            reg.unregister();
        }
        try{
            server.close();
        }
        catch(Exception ex){
            log.error("Shutting down gridoptics server", ex );
        }
        finally{
            server = null;
        }

        try{
            registrationService.shutdown();
        }
        catch(Exception ex){
            log.error("Shutting down registration service", ex );
        }
        finally{
            registrationService = null;

        }
        try{
            dataServices.releaseServices();
        }
        catch(Exception ex){
            log.error("Shutting down dataservices service", ex );
        }
        finally{
            dataServices = null;
        }

        dataSourceCreator = null;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        log.debug("Starting bundle");
        this.context = context;
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, CONFIG_PID);
        context.registerService(ManagedService.class, this, props);

        Hashtable<String, Object> dsProps = new Hashtable<String, Object>();
        dsProps.put(Constants.SERVICE_PID, DS_CONFIG_PID);
        context.registerService(ManagedService.class, this, dsProps);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.debug("Stopping bundle");
        unregisterServices();
        this.context = null;
    }

}