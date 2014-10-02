package pnnl.goss.rdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.hp.hpl.jena.rdf.model.Resource;

/**
 * The <code>EscaTypes</code> maps a mrid string to a EscaType data type.
 * @author d3m614
 *
 */
public class EscaTypes extends HashMap<String, EscaType> {

	private static final long serialVersionUID = -668345956741148741L;

	/**
	 * Retrieve all instances of a specific resource type from the map.
	 * 
	 * @param subject
	 * @return
	 */
	public Collection<EscaType> getByResourceType(Resource subject){
		List<EscaType> collection = new ArrayList<>();
		
		for(EscaType t: values()){
			if (t.getDataType().equals(subject.getLocalName())){
				collection.add(t);
			}
		}
		
		return Collections.unmodifiableList(collection);
	}
}
