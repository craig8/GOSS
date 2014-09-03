package pnnl.goss.topology.nodebreaker.dao;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import pnnl.goss.powergrid.topology.Substation;
import pnnl.goss.powergrid.topology.nodebreaker.Breaker;
import pnnl.goss.powergrid.topology.nodebreaker.Discrete;
import pnnl.goss.powergrid.topology.nodebreaker.Line;
import pnnl.goss.powergrid.topology.nodebreaker.VoltageLevel;

public class NodeBreakerDao {

	EntityManagerFactory emf;

    EntityManager em;

    public NodeBreakerDao(String persistenceUnitName)
    {
        if (emf == null)
        {
            emf = Persistence.createEntityManagerFactory(persistenceUnitName);
        }
    }
    
    public void persist(Discrete entity){
    	em = emf.createEntityManager();
    	
    	em.persist(entity);
    	if (em != null)
        {
            em.close();
            em = null;
        }
    }
    
    public void persist(VoltageLevel entity){
    	em = emf.createEntityManager();
    	
    	em.persist(entity);
    	if (em != null)
        {
            em.close();
            em = null;
        }
    }
    
    public void persist(Substation substation){
    	em = emf.createEntityManager();
    	
    	em.persist(substation);
    	if (em != null)
        {
            em.close();
            em = null;
        }
    }
    
    public void persist(Breaker breaker){
    	em = emf.createEntityManager();
    	
    	em.persist(breaker);
    	if (em != null)
        {
            em.close();
            em = null;
        }    	
    }
    
    public void persist(Line obj){
    	em = emf.createEntityManager();
    	
    	em.persist(obj);
    	if (em != null)
        {
            em.close();
            em = null;
        }    
    }
    
    public Object get(Class<?> entityClass, String id)
    {
        EntityManager em = emf.createEntityManager();
        Object p = em.find(entityClass, id);
        return p;
    }   
}
