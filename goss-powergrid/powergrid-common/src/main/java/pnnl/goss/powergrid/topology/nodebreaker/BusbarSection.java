package pnnl.goss.powergrid.topology.nodebreaker;

import javax.persistence.Column;
import javax.persistence.Entity;
import pnnl.goss.powergrid.topology.IdentifiedObject;
import pnnl.goss.powergrid.topology.NodeBreakerDataType;

@Entity
public class BusbarSection extends IdentifiedObject implements NodeBreakerDataType  {

	private static final long serialVersionUID = 1983837579028685823L;

	@Column
	protected String dataType;

	@Override
	public String getDataType() {
		return dataType;
	}

	@Override
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
}
